package com.example.aaaaketahuan.forecast

import com.example.aaaaketahuan.data.model.Transaksi
import com.example.aaaaketahuan.forecast.model.PrediksiHarian
import com.example.aaaaketahuan.forecast.model.PredictionConfidence
import com.example.aaaaketahuan.forecast.model.RentangTanggalGajian
import com.example.aaaaketahuan.forecast.model.RisikoDefisitResult
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.sqrt

/**
 * Pure-function analyzer untuk prediksi keuangan dan risiko defisit.
 *
 * Seluruh fungsi bersifat **deterministic**: input yang sama → output yang sama.
 * Tidak ada side-effect, tidak ada akses I/O, tidak ada dependency external.
 *
 * Pola ini memudahkan unit testing — setiap fungsi bisa diuji secara terisolasi
 * tanpa mocking. Gunakan [ForecastRepository.getAdvancedDeficitRisk] untuk
 * integrasi dengan data nyata dari repository.
 */
object DeficitRiskAnalyzer {

    // ═════════════════════════════════════════════════════════════════
    // 1. PREDIKSI PENGELUARAN HARIAN (berdasarkan jenis hari)
    // ═════════════════════════════════════════════════════════════════

    /**
     * Menghitung prediksi pengeluaran harian yang dibedakan antara
     * **weekday** (Senin–Jumat) dan **weekend** (Sabtu–Minggu).
     *
     * Algoritma:
     * 1. Pisahkan pengeluaran 7 hari & 30 hari terakhir ke dalam kelompok
     *    weekday dan weekend.
     * 2. Hitung rata-rata harian tiap kelompok di tiap window:
     *    `avgWeekday7`, `avgWeekday30`, `avgWeekend7`, `avgWeekend30`.
     * 3. **Guard weekend**: jika jumlah hari weekend dalam 7 hari terakhir < 2,
     *    gunakan `avgWeekend30` sebagai pengganti `avgWeekend7` (hindari bias
     *    sample kecil).
     * 4. Gabungkan dengan bobot tetap **60:40** (7 hari : 30 hari):
     *    `prediksi = (avg7 * 0.6) + (avg30 * 0.4)`
     *
     * @param today Tanggal referensi "hari ini".
     * @param riwayatPengeluaran Seluruh transaksi pengeluaran (akan difilter otomatis).
     * @return [PrediksiHarian] berisi rata-rata prediksi weekday & weekend.
     */
    fun hitungPrediksiHarianByHariJenis(
        today: LocalDate,
        riwayatPengeluaran: List<Transaksi>
    ): PrediksiHarian {
        val data30Hari = filterKeluarInRange(riwayatPengeluaran, today.minusDays(29), today)
        val data7Hari = filterKeluarInRange(riwayatPengeluaran, today.minusDays(6), today)

        val (weekdayCount30, weekendCount30) = countDayTypes(today, 30)
        val (weekdayCount7, weekendCount7) = countDayTypes(today, 7)

        val avgWeekday30 = computeAvgByDayType(data30Hari, dayFilter = ::isWeekday, dayCount = weekdayCount30)
        val avgWeekend30 = computeAvgByDayType(data30Hari, dayFilter = ::isWeekend, dayCount = weekendCount30)

        val avgWeekday7 = computeAvgByDayType(data7Hari, dayFilter = ::isWeekday, dayCount = weekdayCount7)

        // Guard: jika sampel weekend di 7 hari terlalu sedikit, fallback ke rata-rata 30 hari.
        // Ini mencegah bias dari hari libur yang kebetulan jatuh di akhir pekan saat sample sedikit.
        val avgWeekend7 = if (weekendCount7 < 2) {
            avgWeekend30
        } else {
            computeAvgByDayType(data7Hari, dayFilter = ::isWeekend, dayCount = weekendCount7)
        }

        // Bobot tetap 60:40 (7 hari : 30 hari), diterapkan terpisah per jenis hari.
        val prediksiWeekday = (avgWeekday7 * 0.6) + (avgWeekday30 * 0.4)
        val prediksiWeekend = (avgWeekend7 * 0.6) + (avgWeekend30 * 0.4)

        return PrediksiHarian(prediksiWeekday, prediksiWeekend)
    }

    /**
     * Mengambil nilai prediksi untuk tanggal tertentu berdasarkan jenis harinya.
     *
     * @param tanggal Tanggal target.
     * @param prediksi Objek [PrediksiHarian] yang berisi rata-rata weekday & weekend.
     * @return `nilaiWeekend` jika [tanggal] jatuh pada Sabtu atau Minggu,
     *         selain itu `nilaiWeekday`.
     */
    fun ambilPrediksiUntukTanggal(tanggal: LocalDate, prediksi: PrediksiHarian): Double {
        return if (isWeekend(tanggal)) prediksi.nilaiWeekend else prediksi.nilaiWeekday
    }

    // ═════════════════════════════════════════════════════════════════
    // 3. CONFIDENCE PREDIKSI PENGELUARAN
    // ═════════════════════════════════════════════════════════════════

    /**
     * Menentukan tingkat kepercayaan prediksi pengeluaran berdasarkan
     * jumlah hari data & fluktuasi nominal.
     *
     * - **LOW**:  jumlah hari unik dengan data < 14 hari.
     * - **MEDIUM**: data >= 14 hari **dan** koefisien variasi (CV) > 1.0
     *   (fluktuasi tinggi — standar deviasi melebihi rata-rata).
     * - **HIGH**: data >= 14 hari **dan** CV <= 1.0.
     *
     * **Koefisien variasi (CV)** = stdDev / mean.
     * CV > 1.0 berarti standar deviasi > rata-rata, indikasi volatilitas tinggi.
     * Threshold ini dipilih karena sederhana, intuitif, dan umum dipakai
     * dalam statistik deskriptif untuk mengukur dispersi relatif.
     *
     * @param riwayatPengeluaran Seluruh transaksi pengeluaran.
     * @return [PredictionConfidence] — LOW, MEDIUM, atau HIGH.
     */
    fun hitungConfidencePengeluaran(riwayatPengeluaran: List<Transaksi>): PredictionConfidence {
        val uniqueDates = riwayatPengeluaran.map { it.tanggal }.distinct().count()
        // Minimum 14 hari data unik
        if (uniqueDates < 14) return PredictionConfidence.LOW

        // Hitung koefisien variasi untuk deteksi fluktuasi
        val amounts = riwayatPengeluaran.map { it.jumlah }
        if (amounts.size < 2) return PredictionConfidence.MEDIUM

        val mean = amounts.average()
        // Jika rata-rata 0 atau negatif (seharusnya tidak terjadi untuk pengeluaran),
        // tidak ada fluktuasi yang bermakna → HIGH
        if (mean <= 0) return PredictionConfidence.HIGH

        val variance = amounts.sumOf { (it - mean) * (it - mean) } / amounts.size
        val stdDev = sqrt(variance)
        val cv = stdDev / mean

        // CV > 1.0 dianggap fluktuasi tinggi → MEDIUM
        return if (cv > 1.0) PredictionConfidence.MEDIUM else PredictionConfidence.HIGH
    }

    // ═════════════════════════════════════════════════════════════════
    // 4. ESTIMASI TANGGAL GAJIAN
    // ═════════════════════════════════════════════════════════════════

    /**
     * Memperkirakan rentang tanggal gajian berdasarkan histori pemasukan
     * kategori "Gaji" dalam 3 bulan terakhir.
     *
     * Algoritma:
     * 1. Filter transaksi dengan `sumber == "Gaji"`.
     * 2. Untuk 3 bulan terakhir (termasuk bulan berjalan), ambil hari-ke-berapa
     *    dari transaksi gaji pertama tiap bulan.
     * 3. Jika data < 2 bulan → confidence LOW, rentang `null`, alasan terisi.
     * 4. Jika ≥ 2 bulan → hitung `tanggalMin` dan `tanggalMax` dari kumpulan tanggal.
     * 5. Confidence:
     *    - HIGH:  rentang ≤ 5 hari
     *    - MEDIUM: rentang ≤ 10 hari
     *    - LOW:   rentang > 10 hari
     *
     * @param riwayatPemasukan Seluruh transaksi pemasukan.
     * @param today Tanggal referensi "hari ini".
     * @return [RentangTanggalGajian] dengan rentang dan confidence.
     */
    fun estimasiTanggalGajian(
        riwayatPemasukan: List<Transaksi>,
        today: LocalDate
    ): RentangTanggalGajian {
        // Filter transaksi gaji
        val salaryTx = riwayatPemasukan
            .filter { it.sumber == "Gaji" && it.jenis == "masuk" }

        // Ambil hari-ke-berapa dari gaji pertama tiap bulan dalam 3 bulan terakhir
        val dayOfMonthPerMonth = mutableListOf<Int>()

        for (month in 0..2) {
            val targetDate = today.minusMonths(month.toLong())
            val monthTx = salaryTx.filter {
                it.bulan == targetDate.monthValue && it.tahun == targetDate.year
            }
            if (monthTx.isNotEmpty()) {
                // Ambil transaksi gaji paling awal di bulan tersebut
                monthTx.minByOrNull { it.tanggal }?.let { firstTx ->
                    try {
                        val date = LocalDate.parse(firstTx.tanggal)
                        dayOfMonthPerMonth.add(date.dayOfMonth)
                    } catch (_: Exception) {
                        // Skip tanggal yang tidak bisa di-parse
                    }
                }
            }
        }

        // Minimum 2 bulan data
        if (dayOfMonthPerMonth.size < 2) {
            return RentangTanggalGajian(
                tanggalMin = null,
                tanggalMax = null,
                confidence = PredictionConfidence.LOW,
                alasan = "Histori gajian kurang dari 2 bulan"
            )
        }

        val tanggalMin = dayOfMonthPerMonth.min()
        val tanggalMax = dayOfMonthPerMonth.max()
        val rentang = tanggalMax - tanggalMin

        val confidence = when {
            rentang <= 5 -> PredictionConfidence.HIGH
            rentang <= 10 -> PredictionConfidence.MEDIUM
            else -> PredictionConfidence.LOW
        }

        return RentangTanggalGajian(tanggalMin, tanggalMax, confidence, "")
    }

    // ═════════════════════════════════════════════════════════════════
    // 5. STATUS RISIKO (7 tingkat, berbasis rasio)
    // ═════════════════════════════════════════════════════════════════

    /**
     * Menentukan label status dan icon berdasarkan rasio keuangan.
     *
     * Rasio = proyeksi saldo akhir bulan / total proyeksi pemasukan.
     *
     * | Rentang Rasio      | Status          | Icon |
     * |--------------------|-----------------|------|
     * | > 0.20             | Sangat Sehat    | 🟢   |
     * | 0.10 – 0.20        | Sehat           | 🟢   |
     * | 0.05 – 0.10        | Cukup Aman      | 🟡   |
     * | -0.05 – 0.05       | Seimbang        | 🟡   |
     * | -0.10 – -0.05      | Defisit Ringan  | 🟠   |
     * | -0.20 – -0.10      | Defisit Sedang  | 🔴   |
     * | < -0.20            | Defisit Berat   | 🔴   |
     *
     * @param rasio Rasio keuangan (bisa positif, nol, atau negatif).
     * @return Pair(status, icon).
     */
    fun tentukanStatusRisiko(rasio: Double): Pair<String, String> {
        return when {
            rasio > 0.20 -> "Sangat Sehat" to "\uD83D\uDFE2"
            rasio > 0.10 -> "Sehat" to "\uD83D\uDFE2"
            rasio > 0.05 -> "Cukup Aman" to "\uD83D\uDFE1"
            rasio >= -0.05 -> "Seimbang" to "\uD83D\uDFE1"
            rasio >= -0.10 -> "Defisit Ringan" to "\uD83D\uDFE0"
            rasio >= -0.20 -> "Defisit Sedang" to "\uD83D\uDD34"
            else -> "Defisit Berat" to "\uD83D\uDD34"
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 6. FUNGSI UTAMA — RISIKO DEFISIT LENGKAP
    // ═════════════════════════════════════════════════════════════════

    /**
     * Fungsi utama untuk menghitung risiko defisit akhir bulan.
     *
     * Alur lengkap:
     * 1. Hitung prediksi pengeluaran harian (weekday vs weekend) dari histori.
     * 2. Cek status gaji bulan berjalan (sudah masuk / belum).
     * 3. Jika belum gajian, estimasi rentang tanggal & nominal gaji.
     * 4. Simulasi harian dari hari ini sampai akhir bulan:
     *    - Kurangi saldo dengan prediksi harian sesuai jenis hari.
     *    - Jika tanggal simulasi berada dalam rentang prediksi gaji (dan
     *      gaji belum masuk) → tambahkan nominal gaji ke saldo.
     *    - Catat tanggal pertama kali saldo < 0 sebagai potensi defisit.
     * 5. Hitung rasio = proyeksiSaldoAkhir / totalPemasukanProyeksi.
     *    Guard: jika penyebut 0 → rasio = -1 (otomatis Defisit Berat).
     * 6. Tentukan status risiko berdasarkan rasio.
     * 7. Confidence akhir = weakest-link antara confidence pengeluaran & gaji.
     *
     * @param saldoSaatIni Saldo aktual saat ini.
     * @param riwayatPengeluaran Seluruh transaksi pengeluaran.
     * @param riwayatPemasukan Seluruh transaksi pemasukan.
     * @param pemasukanBulanIni Total pemasukan yang sudah tercatat bulan berjalan.
     * @param today Tanggal hari ini.
     * @param tanggalAkhirBulan Tanggal terakhir bulan berjalan.
     * @return [RisikoDefisitResult] berisi status, proyeksi, dan metadata.
     */
    fun hitungRisikoDefisit(
        saldoSaatIni: Double,
        riwayatPengeluaran: List<Transaksi>,
        riwayatPemasukan: List<Transaksi>,
        pemasukanBulanIni: Double,
        today: LocalDate,
        tanggalAkhirBulan: LocalDate
    ): RisikoDefisitResult {
        // 1. Prediksi pengeluaran per jenis hari
        val prediksiHarian = hitungPrediksiHarianByHariJenis(today, riwayatPengeluaran)
        val confidencePengeluaran = hitungConfidencePengeluaran(riwayatPengeluaran)

        // 2. Cek status gaji bulan ini
        val sudahGajian = riwayatPemasukan.any {
            it.sumber == "Gaji" &&
                it.jenis == "masuk" &&
                it.bulan == today.monthValue &&
                it.tahun == today.year
        }

        // 3. Prediksi sisa pemasukan
        val rentangGaji: RentangTanggalGajian
        val nominalPrediksiGaji: Double
        val prediksiPemasukanSisaBulan: Double

        if (sudahGajian) {
            nominalPrediksiGaji = 0.0
            rentangGaji = RentangTanggalGajian(
                tanggalMin = null,
                tanggalMax = null,
                confidence = PredictionConfidence.HIGH,
                alasan = "Gaji sudah masuk bulan ini"
            )
            prediksiPemasukanSisaBulan = 0.0
        } else {
            // Rata-rata nominal gaji 3 bulan terakhir
            val salaryTx = riwayatPemasukan.filter { it.sumber == "Gaji" && it.jenis == "masuk" }
            val threeMonthsAgo = today.minusMonths(3)
            val recentSalary = salaryTx.filter {
                try {
                    val date = LocalDate.parse(it.tanggal)
                    !date.isBefore(threeMonthsAgo) && !date.isAfter(today)
                } catch (_: Exception) {
                    false
                }
            }
            nominalPrediksiGaji = if (recentSalary.isNotEmpty()) {
                recentSalary.sumOf { it.jumlah } / recentSalary.size
            } else {
                0.0
            }

            rentangGaji = estimasiTanggalGajian(riwayatPemasukan, today)
            prediksiPemasukanSisaBulan = nominalPrediksiGaji
        }

        // 4. Simulasi harian
        var saldoBerjalan = saldoSaatIni
        var tanggalPotensiDefisit: LocalDate? = null
        var tanggalIterasi = today
        var gajiSudahDitambahkan = sudahGajian // Flag untuk memastikan gaji hanya ditambahkan sekali

        while (!tanggalIterasi.isAfter(tanggalAkhirBulan)) {
            val prediksiHariIni = ambilPrediksiUntukTanggal(tanggalIterasi, prediksiHarian)
            saldoBerjalan -= prediksiHariIni

            // Tambah gaji jika dalam rentang (hanya sekali — pada tanggalMin)
            if (!gajiSudahDitambahkan &&
                rentangGaji.tanggalMin != null &&
                rentangGaji.tanggalMax != null &&
                tanggalIterasi.dayOfMonth == rentangGaji.tanggalMin
            ) {
                saldoBerjalan += nominalPrediksiGaji
                gajiSudahDitambahkan = true
            }

            // Catat tanggal pertama kali defisit
            if (saldoBerjalan < 0 && tanggalPotensiDefisit == null) {
                tanggalPotensiDefisit = tanggalIterasi
            }

            tanggalIterasi = tanggalIterasi.plusDays(1)
        }

        val proyeksiSaldoAkhir = saldoBerjalan

        // 5. Hitung rasio
        val totalPemasukanProyeksi = pemasukanBulanIni + prediksiPemasukanSisaBulan
        val rasio = if (totalPemasukanProyeksi > 0) {
            proyeksiSaldoAkhir / totalPemasukanProyeksi
        } else {
            -1.0
        }

        val (statusLabel, statusIcon) = tentukanStatusRisiko(rasio)

        // 6. Confidence gabungan (weakest-link, bukan rata-rata)
        val confidencePemasukan = if (!sudahGajian) rentangGaji.confidence else PredictionConfidence.HIGH
        val confidenceAkhir = weakestConfidence(confidencePengeluaran, confidencePemasukan)

        val alasanConfidence = when {
            confidenceAkhir != PredictionConfidence.LOW -> ""
            confidencePemasukan == PredictionConfidence.LOW ->
                "Pola gajian belum konsisten (histori < 2 bulan atau tanggal bervariasi)"
            else -> "Riwayat pengeluaran belum cukup / fluktuatif"
        }

        return RisikoDefisitResult(
            status = statusLabel,
            icon = statusIcon,
            rasio = rasio,
            proyeksiSaldoAkhir = proyeksiSaldoAkhir,
            tanggalPotensiDefisit = tanggalPotensiDefisit,
            confidence = confidenceAkhir,
            confidenceReason = alasanConfidence
        )
    }

    // ═════════════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Memfilter transaksi pengeluaran (`jenis == "keluar"`) dalam rentang tanggal.
     */
    private fun filterKeluarInRange(
        transaksi: List<Transaksi>,
        from: LocalDate,
        to: LocalDate
    ): List<Transaksi> {
        return transaksi.filter { t ->
            t.jenis == "keluar" && run {
                try {
                    val date = LocalDate.parse(t.tanggal)
                    !date.isBefore(from) && !date.isAfter(to)
                } catch (_: Exception) {
                    false
                }
            }
        }
    }

    /**
     * Menghitung jumlah hari weekday dan weekend dalam [days] hari terakhir
     * sebelum [today] (inklusif).
     *
     * @return Pair(weekdayCount, weekendCount).
     */
    internal fun countDayTypes(today: LocalDate, days: Int): Pair<Int, Int> {
        var weekday = 0
        var weekend = 0
        for (i in 0 until days) {
            val date = today.minusDays(i.toLong())
            if (isWeekend(date)) weekend++ else weekday++
        }
        return weekday to weekend
    }

    /** Apakah [date] jatuh pada hari Sabtu atau Minggu? */
    internal fun isWeekend(date: LocalDate): Boolean {
        val day = date.dayOfWeek
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
    }

    /** Apakah [date] jatuh pada hari Senin–Jumat? */
    internal fun isWeekday(date: LocalDate): Boolean = !isWeekend(date)

    /**
     * Menghitung rata-rata jumlah transaksi per hari untuk tipe hari tertentu.
     *
     * @param data Data transaksi yang sudah difilter rentang tanggal.
     * @param dayFilter Predikat untuk memilih hari (::isWeekday atau ::isWeekend).
     * @param dayCount Jumlah hari dalam periode untuk tipe hari tersebut (pembagi).
     * @return Rata-rata pengeluaran per hari, atau 0 jika [dayCount] ≤ 0.
     */
    private fun computeAvgByDayType(
        data: List<Transaksi>,
        dayFilter: (LocalDate) -> Boolean,
        dayCount: Int
    ): Double {
        if (dayCount <= 0) return 0.0
        val filtered = data.filter {
            try {
                dayFilter(LocalDate.parse(it.tanggal))
            } catch (_: Exception) {
                false
            }
        }
        return if (filtered.isEmpty()) 0.0 else filtered.sumOf { it.jumlah } / dayCount
    }

    /**
     * Mengambil confidence **paling rendah** dari beberapa input.
     *
     * Urutan: LOW (terendah) < MEDIUM < HIGH (tertinggi).
     * Fungsi ini mengimplementasikan prinsip **weakest-link**: tingkat
     * kepercayaan keseluruhan adalah yang paling rendah dari seluruh komponen.
     */
    internal fun weakestConfidence(vararg confidences: PredictionConfidence): PredictionConfidence {
        val order = mapOf(
            PredictionConfidence.LOW to 0,
            PredictionConfidence.MEDIUM to 1,
            PredictionConfidence.HIGH to 2
        )
        return confidences.minByOrNull { order[it] ?: 0 } ?: PredictionConfidence.LOW
    }
}
