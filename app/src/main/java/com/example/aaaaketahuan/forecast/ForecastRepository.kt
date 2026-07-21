package com.example.aaaaketahuan.forecast

import com.example.aaaaketahuan.data.repository.TransaksiRepository
import com.example.aaaaketahuan.forecast.model.BalancePrediction
import com.example.aaaaketahuan.forecast.model.DeficitRisk
import com.example.aaaaketahuan.forecast.model.EndOfMonthPrediction
import com.example.aaaaketahuan.forecast.model.PredictionConfidence
import com.example.aaaaketahuan.forecast.model.RiskSeverity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.min

/**
 * Repository untuk komputasi forecasting on-device.
 *
 * Mengambil data mentah dari [TransaksiRepository], melakukan agregasi
 * harian, dan menyimpannya dalam **in-memory cache** yang di-invalidate
 * saat timestamp file transaksi.json berubah.
 *
 * Seluruh komputasi CPU-bound dijalankan di [Dispatchers.Default].
 * I/O (baca file JSON) tetap ditangani oleh [TransaksiRepository] di
 * [Dispatchers.IO] masing-masing.
 *
 * @property transaksiRepository Sumber data transaksi — tidak ada akses
 *   langsung ke file JSON dari repository ini.
 */
class ForecastRepository @Inject constructor(
    private val transaksiRepository: TransaksiRepository
) {

    // ─── In-Memory Cache ─────────────────────────────────────────────

    /**
     * Agregasi harian yang sudah diproses: total pemasukan & pengeluaran
     * per tanggal. Di-rebuild jika timestamp file berubah.
     */
    private var cachedDailyTotals: List<DailySummary>? = null

    /** Timestamp [transaksiRepository.getTransaksiFileTimestamp] terakhir. */
    private var lastFileTimestamp: Long = 0L

    /**
     * Agregasi harian — satu entri per tanggal.
     *
     * @property date Tanggal transaksi.
     * @property totalKeluar Total pengeluaran pada tanggal tersebut.
     * @property totalMasuk Total pemasukan pada tanggal tersebut.
     */
    private data class DailySummary(
        val date: LocalDate,
        val totalKeluar: Double,
        val totalMasuk: Double
    )

    /**
     * Memastikan cache valid dengan memeriksa timestamp file.
     * Jika file berubah sejak cache terakhir dibangun, lakukan rebuild.
     *
     * Jika rebuild gagal dan cache lama masih tersedia, cache lama
     * tetap dipakai (stale data lebih baik daripada tidak ada data).
     *
     * @throws Exception jika cache null dan rebuild gagal.
     */
    private suspend fun ensureCacheValid() {
        try {
            val timestamp = transaksiRepository.getTransaksiFileTimestamp()
            if (cachedDailyTotals == null || timestamp != lastFileTimestamp) {
                rebuildCache()
                lastFileTimestamp = timestamp
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Jika cache lama masih ada, biarkan tetap dipakai (stale)
            if (cachedDailyTotals == null) throw e
        }
    }

    /**
     * Rebuild cache: baca semua transaksi dari repository, kelompokkan
     * per tanggal, lalu hitung total pemasukan & pengeluaran harian.
     *
     * Method ini memanggil [TransaksiRepository.getAllTransaksi] yang
     * menangani I/O-nya sendiri di [Dispatchers.IO].
     */
    private suspend fun rebuildCache() {
        val all = transaksiRepository.getAllTransaksi()
        val grouped = all.groupBy { it.tanggal }
        val summaries = grouped.map { (dateStr, list) ->
            DailySummary(
                date = LocalDate.parse(dateStr),
                totalKeluar = list.filter { it.jenis == "keluar" }.sumOf { it.jumlah },
                totalMasuk = list.filter { it.jenis == "masuk" }.sumOf { it.jumlah }
            )
        }.sortedBy { it.date }
        cachedDailyTotals = summaries
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    /**
     * Menghitung rata-rata pengeluaran & pemasukan harian berdasarkan
     * data [n] hari kalender terakhir (maksimal 30).
     *
     * Hari tanpa transaksi dihitung sebagai 0, yang benar untuk SMA
     * (tidak perlu membagi hanya dengan hari yang memiliki transaksi).
     *
     * @param today Tanggal referensi "hari ini".
     * @return Triple<Int, Double, Double> → (jumlah_hari, avgDailyKeluar, avgDailyMasuk).
     *   Jumlah hari = 0 jika cache kosong.
     */
    private fun getAvailableDailyAverages(today: LocalDate): Triple<Int, Double, Double> {
        val cache = cachedDailyTotals ?: return Triple(0, 0.0, 0.0)
        if (cache.isEmpty()) return Triple(0, 0.0, 0.0)

        val firstDate = cache.first().date
        val availableDays = ChronoUnit.DAYS.between(firstDate, today).toInt() + 1
        val n = minOf(30, availableDays)

        val cutoff = today.minusDays(n.toLong() - 1)
        val dailyMap = cache.associateBy { it.date }

        var totalKeluar = 0.0
        var totalMasuk = 0.0

        for (i in 0 until n) {
            val date = cutoff.plusDays(i.toLong())
            val summary = dailyMap[date]
            totalKeluar += summary?.totalKeluar ?: 0.0
            totalMasuk += summary?.totalMasuk ?: 0.0
        }

        return Triple(n, totalKeluar / n, totalMasuk / n)
    }

    /**
     * Menentukan [PredictionConfidence] berdasarkan jumlah hari data
     * yang tersedia dalam perhitungan SMA.
     *
     * - HIGH:  >= 30 hari
     * - MEDIUM: 22–29 hari
     * - LOW:    14–21 hari
     */
    private fun confidenceFromDays(days: Int): PredictionConfidence = when {
        days >= 30 -> PredictionConfidence.HIGH
        days >= 22 -> PredictionConfidence.MEDIUM
        else -> PredictionConfidence.LOW
    }

    // ─── Fitur 1: Prediksi Pengeluaran Akhir Bulan ────────────────────

    /**
     * Menghitung prediksi total pengeluaran akhir bulan ini.
     *
     * Algoritma: **Simple Moving Average (SMA)** — rata-rata pengeluaran
     * harian selama 30 hari kalender terakhir (atau data tersedia),
     * dikalikan dengan sisa hari di bulan berjalan, lalu ditambahkan
     * ke realisasi pengeluaran bulan ini.
     *
     * **Minimum data:**
     * - Histori transaksi mencakup >= 14 hari kalender.
     * - Sudah melewati >= 7 hari di bulan berjalan.
     *
     * **Output:**
     * - [EndOfMonthPrediction.Predicted] jika data mencukupi.
     * - [EndOfMonthPrediction.InsufficientData] jika data belum cukup.
     *
     * @throws Exception jika cache gagal dibangun dan tidak ada cache lama.
     */
    suspend fun getEndOfMonthPrediction(): EndOfMonthPrediction = withContext(Dispatchers.Default) {
        ensureCacheValid()
        val cache = cachedDailyTotals
            ?: return@withContext EndOfMonthPrediction.InsufficientData(
                "Tidak ada data transaksi.", 0, 14
            )
        if (cache.isEmpty()) {
            return@withContext EndOfMonthPrediction.InsufficientData(
                "Belum ada transaksi yang tercatat.", 0, 14
            )
        }

        val today = LocalDate.now()
        val dataSpan = ChronoUnit.DAYS.between(cache.first().date, today).toInt() + 1

        if (dataSpan < 14) {
            return@withContext EndOfMonthPrediction.InsufficientData(
                "Data historis belum mencukupi. Dibutuhkan minimal 14 hari, " +
                    "saat ini baru $dataSpan hari.",
                dataSpan, 14
            )
        }

        val daysInCurrentMonth = today.dayOfMonth
        if (daysInCurrentMonth < 7) {
            return@withContext EndOfMonthPrediction.InsufficientData(
                "Data bulan ini belum mencukupi. Minimal 7 hari di bulan berjalan " +
                    "untuk prediksi yang bermakna, saat ini baru hari ke-$daysInCurrentMonth.",
                daysInCurrentMonth, 7
            )
        }

        val (days, avgDailyKeluar, _) = getAvailableDailyAverages(today)
        val daysInMonth = today.lengthOfMonth()
        val remainingDays = daysInMonth - today.dayOfMonth

        val currentMonthKeluar = cache
            .filter { it.date.month == today.month && it.date.year == today.year }
            .sumOf { it.totalKeluar }

        val predictedRemaining = avgDailyKeluar * remainingDays
        val predictedTotal = currentMonthKeluar + predictedRemaining

        EndOfMonthPrediction.Predicted(
            currentTotal = currentMonthKeluar,
            predictedTotal = predictedTotal,
            confidence = confidenceFromDays(days)
        )
    }

    // ─── Fitur 2: Prediksi Saldo pada Tanggal Tertentu ──────────────

    /**
     * Menghitung prediksi saldo pada [targetDate].
     *
     * Algoritma:
     * ```
     * prediksiSaldo = saldoSaatIni + (rataPemasukanHarian × sisaHari)
     *                  - (rataPengeluaranHarian × sisaHari)
     * ```
     *
     * **Minimum data:** sama dengan [getEndOfMonthPrediction].
     *
     * **Batasan:**
     * - [targetDate] harus > hari ini. Jika <= hari ini → [BalancePrediction.TargetDateInPast].
     * - Jika [targetDate] > 90 hari dari sekarang → confidence diturunkan ke LOW.
     *
     * @param targetDate Tanggal target prediksi (LocalDate).
     * @return [BalancePrediction.Predicted], [BalancePrediction.InsufficientData],
     *   atau [BalancePrediction.TargetDateInPast].
     *
     * @throws Exception jika cache gagal dibangun.
     */
    suspend fun getBalancePrediction(targetDate: LocalDate): BalancePrediction =
        withContext(Dispatchers.Default) {
            val today = LocalDate.now()

            if (!targetDate.isAfter(today)) {
                return@withContext BalancePrediction.TargetDateInPast(targetDate.toString())
            }

            ensureCacheValid()
            val cache = cachedDailyTotals
                ?: return@withContext BalancePrediction.InsufficientData(
                    "Tidak ada data transaksi.", 0, 14
                )
            if (cache.isEmpty()) {
                return@withContext BalancePrediction.InsufficientData(
                    "Belum ada transaksi yang tercatat.", 0, 14
                )
            }

            val dataSpan = ChronoUnit.DAYS.between(cache.first().date, today).toInt() + 1
            if (dataSpan < 14) {
                return@withContext BalancePrediction.InsufficientData(
                    "Data historis belum mencukupi. Dibutuhkan minimal 14 hari, " +
                        "saat ini baru $dataSpan hari.",
                    dataSpan, 14
                )
            }

            val (days, avgDailyKeluar, avgDailyMasuk) = getAvailableDailyAverages(today)

            // Saldo aktual bulan berjalan
            val currentMonthData = cache.filter {
                it.date.month == today.month && it.date.year == today.year
            }
            val currentBalance = currentMonthData.sumOf { it.totalMasuk } -
                currentMonthData.sumOf { it.totalKeluar }

            val daysUntilTarget = ChronoUnit.DAYS.between(today, targetDate).toInt()

            val predictedIncome = avgDailyMasuk * daysUntilTarget
            val predictedExpense = avgDailyKeluar * daysUntilTarget
            val predictedBalance = currentBalance + predictedIncome - predictedExpense

            // Target lebih dari 90 hari: confidence turun
            val confidence = if (daysUntilTarget > 90) {
                PredictionConfidence.LOW
            } else {
                confidenceFromDays(days)
            }

            BalancePrediction.Predicted(
                currentBalance = currentBalance,
                predictedBalance = predictedBalance,
                targetDate = targetDate.toString(),
                confidence = confidence
            )
        }

    // ─── Fitur 3: Deteksi Risiko Defisit ────────────────────────────

    /**
     * Mendeteksi risiko saldo negatif (defisit) pada akhir bulan ini.
     *
     * Menggunakan [getBalancePrediction] dengan target = akhir bulan,
     * lalu mengklasifikasikan tingkat risiko berdasarkan rasio defisit
     * terhadap rata-rata pengeluaran bulanan.
     *
     * **Klasifikasi:**
     * - [DeficitRisk.AlreadyDeficit]: saldo saat ini sudah negatif.
     * - [DeficitRisk.AtRisk]: proyeksi saldo akhir bulan negatif.
     *     - LOW:     defisit < 20% dari rata-rata pengeluaran bulanan.
     *     - MODERATE: defisit 20–49%.
     *     - HIGH:    defisit >= 50%.
     * - [DeficitRisk.Safe]: proyeksi saldo akhir bulan >= 0.
     * - [DeficitRisk.InsufficientData]: data belum mencukupi.
     *
     * @throws Exception jika cache gagal dibangun.
     */
    suspend fun getDeficitRisk(): DeficitRisk = withContext(Dispatchers.Default) {
        val today = LocalDate.now()
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())

        val balancePrediction = getBalancePrediction(endOfMonth)
        when (balancePrediction) {
            is BalancePrediction.InsufficientData -> {
                DeficitRisk.InsufficientData(balancePrediction.reason)
            }
            is BalancePrediction.TargetDateInPast -> {
                // Tidak mungkin terjadi karena target = akhir bulan > today
                DeficitRisk.InsufficientData("Gagal memproyeksikan saldo akhir bulan.")
            }
            is BalancePrediction.Predicted -> {
                val currentBalance = balancePrediction.currentBalance
                val projectedBalance = balancePrediction.predictedBalance

                when {
                    currentBalance <= 0 -> DeficitRisk.AlreadyDeficit(currentBalance)

                    projectedBalance < 0 -> {
                        val shortfallAmount = -projectedBalance
                        val (_, avgDailyKeluar, _) = getAvailableDailyAverages(today)
                        val avgMonthlySpending = avgDailyKeluar * today.lengthOfMonth()

                        val severity = if (avgMonthlySpending > 0) {
                            val ratio = shortfallAmount / avgMonthlySpending
                            when {
                                ratio < 0.2 -> RiskSeverity.LOW
                                ratio < 0.5 -> RiskSeverity.MODERATE
                                else -> RiskSeverity.HIGH
                            }
                        } else {
                            // Tidak ada data pengeluaran — fallback ke MODERATE
                            RiskSeverity.MODERATE
                        }

                        DeficitRisk.AtRisk(severity, projectedBalance, shortfallAmount)
                    }

                    else -> DeficitRisk.Safe(
                        projectedBalance = projectedBalance,
                        buffer = projectedBalance
                    )
                }
            }
        }
    }
}
