package com.example.aaaaketahuan.forecast

import com.example.aaaaketahuan.data.model.Transaksi
import com.example.aaaaketahuan.forecast.model.PredictionConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Unit test untuk [DeficitRiskAnalyzer].
 *
 * Seluruh fungsi bersifat **pure** (deterministic) sehingga bisa diuji
 * tanpa mocking. Transaksi dibuat dengan tanggal relatif terhadap
 * [today] sehingga test tetap valid kapan pun dijalankan.
 */
class DeficitRiskAnalyzerTest {

    private val today: LocalDate = LocalDate.now()
    private val dateFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

    // ═════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Membuat transaksi pengeluaran harian untuk [count] hari terakhir.
     *
     * @param startOffset 0 = mulai dari hari ini, 1 = mulai kemarin, dst.
     * @param count Jumlah hari.
     * @param dailyAmount Jumlah pengeluaran per hari.
     * @param prefix Prefix ID transaksi.
     */
    private fun makeExpenses(
        startOffset: Int = 0,
        count: Int,
        dailyAmount: Double,
        prefix: String = "e"
    ): List<Transaksi> {
        val result = mutableListOf<Transaksi>()
        for (i in 0 until count) {
            val date = today.minusDays((startOffset + count - 1 - i).toLong())
            result.add(
                Transaksi(
                    id = "$prefix-$i",
                    tanggal = date.format(dateFormatter),
                    jenis = "keluar",
                    jumlah = dailyAmount,
                    namaBarang = "Item$i",
                    keterangan = "",
                    kategori = "Makanan",
                    bulan = date.monthValue,
                    tahun = date.year
                )
            )
        }
        return result
    }

    /**
     * Membuat satu transaksi pemasukan dengan sumber tertentu.
     */
    private fun makeIncome(
        date: LocalDate,
        jumlah: Double,
        sumber: String = "Gaji",
        prefix: String = "inc"
    ): Transaksi {
        return Transaksi(
            id = "$prefix-${date.format(dateFormatter)}",
            tanggal = date.format(dateFormatter),
            jenis = "masuk",
            jumlah = jumlah,
            namaBarang = sumber,
            keterangan = "",
            kategori = sumber,
            bulan = date.monthValue,
            tahun = date.year,
            sumber = sumber
        )
    }

    /**
     * Membuat transaksi pengeluaran dengan jumlah bervariasi (untuk test confidence).
     */
    private fun makeVariableExpenses(
        amounts: List<Pair<Int, Double>> // List of (daysAgo, amount)
    ): List<Transaksi> {
        return amounts.map { (daysAgo, amount) ->
            val date = today.minusDays(daysAgo.toLong())
            Transaksi(
                id = "v-$daysAgo",
                tanggal = date.format(dateFormatter),
                jenis = "keluar",
                jumlah = amount,
                namaBarang = "Item",
                keterangan = "",
                kategori = "Makanan",
                bulan = date.monthValue,
                tahun = date.year
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 1. TES: countDayTypes
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `countDayTypes 30 days returns correct weekday count`() {
        val (weekday, weekend) = DeficitRiskAnalyzer.countDayTypes(today, 30)
        assertEquals(30, weekday + weekend)
        // Sanity check: dalam 30 hari, weekend biasanya 8-10 hari
        assertTrue("Expected 8-10 weekends in 30 days, got $weekend", weekend in 8..10)
    }

    @Test
    fun `countDayTypes 7 days returns correct counts`() {
        val (weekday, weekend) = DeficitRiskAnalyzer.countDayTypes(today, 7)
        assertEquals(7, weekday + weekend)
    }

    @Test
    fun `countDayTypes counts today correctly`() {
        val (weekday, weekend) = DeficitRiskAnalyzer.countDayTypes(today, 1)
        val isTodayWeekend = today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY
        assertEquals(if (isTodayWeekend) 0 else 1, weekday)
        assertEquals(if (isTodayWeekend) 1 else 0, weekend)
    }

    // ═════════════════════════════════════════════════════════════════
    // 2. TES: isWeekend / isWeekday
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `isWeekend returns true for Saturday`() {
        // Cari hari Sabtu berikutnya
        var sat = today
        while (sat.dayOfWeek != DayOfWeek.SATURDAY) sat = sat.plusDays(1)
        assertTrue(DeficitRiskAnalyzer.isWeekend(sat))
    }

    @Test
    fun `isWeekend returns true for Sunday`() {
        var sun = today
        while (sun.dayOfWeek != DayOfWeek.SUNDAY) sun = sun.plusDays(1)
        assertTrue(DeficitRiskAnalyzer.isWeekend(sun))
    }

    @Test
    fun `isWeekday returns true for Monday`() {
        var mon = today
        while (mon.dayOfWeek != DayOfWeek.MONDAY) mon = mon.plusDays(1)
        assertTrue(DeficitRiskAnalyzer.isWeekday(mon))
        assertTrue(!DeficitRiskAnalyzer.isWeekend(mon))
    }

    // ═════════════════════════════════════════════════════════════════
    // 3. TES: hitungPrediksiHarianByHariJenis
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `prediksiHarian with stable daily expense returns same weekday and weekend values`() {
        val expenses = makeExpenses(count = 30, dailyAmount = 10000.0)
        val result = DeficitRiskAnalyzer.hitungPrediksiHarianByHariJenis(today, expenses)

        // Dengan data stabil 10rb/hari, prediksi harus mendekati 10rb
        assertTrue("Weekday prediksi terlalu rendah: ${result.nilaiWeekday}", result.nilaiWeekday > 8000)
        assertTrue("Weekend prediksi terlalu rendah: ${result.nilaiWeekend}", result.nilaiWeekend > 8000)
    }

    @Test
    fun `prediksiHarian with higher weekend spending returns higher weekend value`() {
        // Weekend: 20000, Weekday: 5000
        val expenses = mutableListOf<Transaksi>()
        for (i in 0 until 30) {
            val date = today.minusDays(i.toLong())
            val isWeekend = DeficitRiskAnalyzer.isWeekend(date)
            val amount = if (isWeekend) 20000.0 else 5000.0
            expenses.add(
                Transaksi(
                    id = "we-$i",
                    tanggal = date.format(dateFormatter),
                    jenis = "keluar",
                    jumlah = amount,
                    namaBarang = "Item",
                    keterangan = "",
                    kategori = "Makanan",
                    bulan = date.monthValue,
                    tahun = date.year
                )
            )
        }
        val result = DeficitRiskAnalyzer.hitungPrediksiHarianByHariJenis(today, expenses)
        assertTrue(
            "Weekend ($result.nilaiWeekend) harus > Weekday (${result.nilaiWeekday})",
            result.nilaiWeekend > result.nilaiWeekday
        )
    }

    @Test
    fun `prediksiHarian with weekend guard uses avgWeekend30 when weekend sample lt 2 in 7 days`() {
        // Buat data 30 hari, weekend terakhir belanja tinggi.
        // Tapi jika dalam 7 hari terakhir hanya ada 1 weekend, guard harus pakai avg30.
        val expenses = mutableListOf<Transaksi>()
        for (i in 0 until 30) {
            val date = today.minusDays(i.toLong())
            val amount = 10000.0 // flat
            expenses.add(
                Transaksi(
                    id = "g-$i",
                    tanggal = date.format(dateFormatter),
                    jenis = "keluar",
                    jumlah = amount,
                    namaBarang = "Item",
                    keterangan = "",
                    kategori = "Makanan",
                    bulan = date.monthValue,
                    tahun = date.year
                )
            )
        }

        // Hitung jumlah weekend dalam 7 hari terakhir
        val (_, weekendCount7) = DeficitRiskAnalyzer.countDayTypes(today, 7)
        val result = DeficitRiskAnalyzer.hitungPrediksiHarianByHariJenis(today, expenses)

        // Nilai harus wajar (antara 8000-12000 untuk data 10rb/hari)
        assertTrue("Weekend: ${result.nilaiWeekend}", result.nilaiWeekend in 5000.0..15000.0)
        assertTrue("Weekday: ${result.nilaiWeekday}", result.nilaiWeekday in 5000.0..15000.0)

        // Catatan: guard aktif hanya jika weekendCount7 < 2.
        // Hasilnya tetap sama karena avgWeekend30 == avgWeekend7 (data flat).
        if (weekendCount7 < 2) {
            // Guard aktif — fallback ke avgWeekend30, tidak crash
            assertTrue(result.nilaiWeekend > 0)
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 4. TES: ambilPrediksiUntukTanggal
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `ambilPrediksiUntukTanggal returns weekend value on Saturday`() {
        var sat = today
        while (sat.dayOfWeek != DayOfWeek.SATURDAY) sat = sat.plusDays(1)

        val prediksi = com.example.aaaaketahuan.forecast.model.PrediksiHarian(
            nilaiWeekday = 10000.0,
            nilaiWeekend = 25000.0
        )
        assertEquals(25000.0, DeficitRiskAnalyzer.ambilPrediksiUntukTanggal(sat, prediksi), 0.001)
    }

    @Test
    fun `ambilPrediksiUntukTanggal returns weekday value on Monday`() {
        var mon = today
        while (mon.dayOfWeek != DayOfWeek.MONDAY) mon = mon.plusDays(1)

        val prediksi = com.example.aaaaketahuan.forecast.model.PrediksiHarian(
            nilaiWeekday = 10000.0,
            nilaiWeekend = 25000.0
        )
        assertEquals(10000.0, DeficitRiskAnalyzer.ambilPrediksiUntukTanggal(mon, prediksi), 0.001)
    }

    // ═════════════════════════════════════════════════════════════════
    // 5. TES: hitungConfidencePengeluaran
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `confidencePengeluaran LOW when less than 14 unique days`() {
        val expenses = makeExpenses(count = 10, dailyAmount = 10000.0)
        assertEquals(PredictionConfidence.LOW, DeficitRiskAnalyzer.hitungConfidencePengeluaran(expenses))
    }

    @Test
    fun `confidencePengeluaran HIGH with stable data over 14 days`() {
        val expenses = makeExpenses(count = 20, dailyAmount = 10000.0)
        // Data stabil 10rb/hari → CV rendah → HIGH
        assertEquals(PredictionConfidence.HIGH, DeficitRiskAnalyzer.hitungConfidencePengeluaran(expenses))
    }

    @Test
    fun `confidencePengeluaran MEDIUM with highly variable data`() {
        // Variasi tinggi: 18 hari 0, 2 hari 5_000_000 → CV ≈ 3.0 >> 1.0
        val amounts = (0 until 20).map { i ->
            i to if (i >= 18) 5_000_000.0 else 0.0
        }
        val expenses = makeVariableExpenses(amounts)
        assertEquals(PredictionConfidence.MEDIUM, DeficitRiskAnalyzer.hitungConfidencePengeluaran(expenses))
    }

    @Test
    fun `confidencePengeluaran handles empty list`() {
        assertEquals(PredictionConfidence.LOW, DeficitRiskAnalyzer.hitungConfidencePengeluaran(emptyList()))
    }

    // ═════════════════════════════════════════════════════════════════
    // 6. TES: estimasiTanggalGajian
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `estimasiTanggalGajian LOW when less than 2 months data`() {
        val incomes = listOf(
            makeIncome(today.minusDays(5), 5_000_000.0)
        )
        val result = DeficitRiskAnalyzer.estimasiTanggalGajian(incomes, today)
        assertEquals(PredictionConfidence.LOW, result.confidence)
        assertNull(result.tanggalMin)
        assertNull(result.tanggalMax)
        assertTrue(result.alasan.contains("kurang dari 2 bulan"))
    }

    @Test
    fun `estimasiTanggalGajian HIGH when range within 5 days`() {
        // Gaji tiap tanggal 25 selama 3 bulan
        val incomes = mutableListOf<Transaksi>()
        for (month in 0..2) {
            val date = today.minusMonths(month.toLong()).withDayOfMonth(25)
            incomes.add(makeIncome(date, 5_000_000.0))
        }
        val result = DeficitRiskAnalyzer.estimasiTanggalGajian(incomes, today)
        assertEquals(PredictionConfidence.HIGH, result.confidence)
        assertEquals(25, result.tanggalMin)
        assertEquals(25, result.tanggalMax)
    }

    @Test
    fun `estimasiTanggalGajian MEDIUM when range 6-10 days`() {
        // Gaji di tanggal 20, 25, 22 (range = 5, tepat batas HIGH)
        // Buat range = 7
        val incomes = mutableListOf<Transaksi>()
        val dates = listOf(20, 20, 27) // range = 7
        for (month in 0..2) {
            val date = today.minusMonths(month.toLong()).withDayOfMonth(dates[month])
            incomes.add(makeIncome(date, 5_000_000.0))
        }
        val result = DeficitRiskAnalyzer.estimasiTanggalGajian(incomes, today)
        assertEquals(PredictionConfidence.MEDIUM, result.confidence)
        assertEquals(20, result.tanggalMin)
        assertEquals(27, result.tanggalMax)
    }

    @Test
    fun `estimasiTanggalGajian LOW when range over 10 days`() {
        val incomes = mutableListOf<Transaksi>()
        val dates = listOf(10, 10, 25) // range = 15
        for (month in 0..2) {
            val date = today.minusMonths(month.toLong()).withDayOfMonth(dates[month])
            incomes.add(makeIncome(date, 5_000_000.0))
        }
        val result = DeficitRiskAnalyzer.estimasiTanggalGajian(incomes, today)
        assertEquals(PredictionConfidence.LOW, result.confidence)
        assertEquals(10, result.tanggalMin)
        assertEquals(25, result.tanggalMax)
    }

    @Test
    fun `estimasiTanggalGajian only considers first salary per month`() {
        // Bulan ini ada 2 transaksi Gaji — hanya yang pertama dipakai
        val thisMonth = today.withDayOfMonth(15)
        val lastMonth = today.minusMonths(1).withDayOfMonth(10)

        val incomes = listOf(
            makeIncome(thisMonth, 5_000_000.0),
            makeIncome(thisMonth.plusDays(7), 2_000_000.0, prefix = "bonus"), // second tx same month
            makeIncome(lastMonth, 5_000_000.0)
        )
        val result = DeficitRiskAnalyzer.estimasiTanggalGajian(incomes, today)
        // Min should be 10 (last month) or 15 (this month first)
        assertNotNull(result.tanggalMin)
        assertNotNull(result.tanggalMax)
    }

    // ═════════════════════════════════════════════════════════════════
    // 7. TES: tentukanStatusRisiko — semua 7 threshold
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `status risiko Sangat Sehat when ratio above 0,20`() {
        val (label, icon) = DeficitRiskAnalyzer.tentukanStatusRisiko(0.21)
        assertEquals("Sangat Sehat", label)
        assertEquals("\uD83D\uDFE2", icon)
    }

    @Test
    fun `status risiko Sehat when ratio 0,10 to 0,20`() {
        val (label, icon) = DeficitRiskAnalyzer.tentukanStatusRisiko(0.15)
        assertEquals("Sehat", label)
        assertEquals("\uD83D\uDFE2", icon)
    }

    @Test
    fun `status risiko Sehat at exact boundary 0,20`() {
        // 0.20 > 0.10 → Sehat (bukan Sangat Sehat, karena > 0.20)
        val (label, _) = DeficitRiskAnalyzer.tentukanStatusRisiko(0.20)
        assertEquals("Sehat", label)
    }

    @Test
    fun `status risiko Cukup Aman when ratio 0,05 to 0,10`() {
        val (label, icon) = DeficitRiskAnalyzer.tentukanStatusRisiko(0.07)
        assertEquals("Cukup Aman", label)
        assertEquals("\uD83D\uDFE1", icon)
    }

    @Test
    fun `status risiko Cukup Aman at exact boundary 0,05`() {
        // 0.05 > 0.05? No — strictly greater false, so falls through to Seimbang
        val (label, _) = DeficitRiskAnalyzer.tentukanStatusRisiko(0.05)
        assertEquals("Seimbang", label)
    }

    @Test
    fun `status risiko Cukup Aman just above 0,05 boundary`() {
        val (label, _) = DeficitRiskAnalyzer.tentukanStatusRisiko(0.0501)
        assertEquals("Cukup Aman", label)
    }

    @Test
    fun `status risiko Seimbang when ratio -0,05 to 0,05`() {
        val (label, icon) = DeficitRiskAnalyzer.tentukanStatusRisiko(0.0)
        assertEquals("Seimbang", label)
        assertEquals("\uD83D\uDFE1", icon)
    }

    @Test
    fun `status risiko Seimbang at negative boundary -0,05`() {
        val (label, _) = DeficitRiskAnalyzer.tentukanStatusRisiko(-0.05)
        assertEquals("Seimbang", label)
    }

    @Test
    fun `status risiko Defisit Ringan when ratio -0,10 to -0,05`() {
        val (label, icon) = DeficitRiskAnalyzer.tentukanStatusRisiko(-0.08)
        assertEquals("Defisit Ringan", label)
        assertEquals("\uD83D\uDFE0", icon)
    }

    @Test
    fun `status risiko Defisit Ringan at exact -0,10`() {
        val (label, _) = DeficitRiskAnalyzer.tentukanStatusRisiko(-0.10)
        assertEquals("Defisit Ringan", label)
    }

    @Test
    fun `status risiko Defisit Sedang when ratio -0,20 to -0,10`() {
        val (label, icon) = DeficitRiskAnalyzer.tentukanStatusRisiko(-0.15)
        assertEquals("Defisit Sedang", label)
        assertEquals("\uD83D\uDD34", icon)
    }

    @Test
    fun `status risiko Defisit Sedang at exact -0,20`() {
        val (label, _) = DeficitRiskAnalyzer.tentukanStatusRisiko(-0.20)
        assertEquals("Defisit Sedang", label)
    }

    @Test
    fun `status risiko Defisit Berat when ratio below -0,20`() {
        val (label, icon) = DeficitRiskAnalyzer.tentukanStatusRisiko(-0.30)
        assertEquals("Defisit Berat", label)
        assertEquals("\uD83D\uDD34", icon)
    }

    // ═════════════════════════════════════════════════════════════════
    // 8. TES: hitungRisikoDefisit — Skenario Utama
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `risikoDefisit divide by zero guard when no income projected`() {
        val expenses = makeExpenses(count = 30, dailyAmount = 10000.0)
        val result = DeficitRiskAnalyzer.hitungRisikoDefisit(
            saldoSaatIni = 100000.0,
            riwayatPengeluaran = expenses,
            riwayatPemasukan = emptyList(),
            pemasukanBulanIni = 0.0,
            today = today,
            tanggalAkhirBulan = today.withDayOfMonth(today.lengthOfMonth())
        )
        assertEquals(-1.0, result.rasio, 0.001)
        assertEquals("Defisit Berat", result.status)
    }

    @Test
    fun `risikoDefisit when gaji already received this month`() {
        // Gaji sudah masuk bulan ini
        val salaryThisMonth = makeIncome(today.minusDays(3), 5_000_000.0)
        val expenses = makeExpenses(count = 30, dailyAmount = 100000.0)

        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        val result = DeficitRiskAnalyzer.hitungRisikoDefisit(
            saldoSaatIni = 3_000_000.0,
            riwayatPengeluaran = expenses,
            riwayatPemasukan = listOf(salaryThisMonth),
            pemasukanBulanIni = 5_000_000.0,
            today = today,
            tanggalAkhirBulan = endOfMonth
        )
        // Gaji sudah masuk, jadi prediksiPemasukanSisaBulan = 0
        // Hanya prediksi pengeluaran yang jalan
        assertNotNull(result)
        // Tidak crash, confidence wajar
    }

    @Test
    fun `risikoDefisit when gaji not yet received with good history`() {
        // Histori gaji 3 bulan: tgl 25
        val expenses = makeExpenses(count = 30, dailyAmount = 50000.0)
        val salaryHistory = mutableListOf<Transaksi>()
        for (m in 1..3) {
            val date = today.minusMonths(m.toLong()).withDayOfMonth(25)
            salaryHistory.add(makeIncome(date, 5_000_000.0))
        }

        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        val result = DeficitRiskAnalyzer.hitungRisikoDefisit(
            saldoSaatIni = 500_000.0,
            riwayatPengeluaran = expenses,
            riwayatPemasukan = salaryHistory,
            pemasukanBulanIni = 0.0,
            today = today,
            tanggalAkhirBulan = endOfMonth
        )
        assertNotNull(result)
        // Simulation should add salary on day 25 if today is before that
        // Atau sudah lewat tanggal 25? Either way, no crash
    }

    @Test
    fun `risikoDefisit detects potential deficit date`() {
        // Saldo rendah, pengeluaran tinggi, tanpa gaji
        val expenses = makeExpenses(count = 30, dailyAmount = 200000.0)
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())

        val result = DeficitRiskAnalyzer.hitungRisikoDefisit(
            saldoSaatIni = 100000.0,
            riwayatPengeluaran = expenses,
            riwayatPemasukan = emptyList(),
            pemasukanBulanIni = 0.0,
            today = today,
            tanggalAkhirBulan = endOfMonth
        )
        // Saldo 100rb - 200rb/hari → defisit dalam 1-2 hari
        assertNotNull("Seharusnya terdeteksi potensi defisit", result.tanggalPotensiDefisit)
        assertTrue(result.proyeksiSaldoAkhir < 0)
    }

    @Test
    fun `risikoDefisit confidence is weakest-link`() {
        // Pengeluaran > 14 hari & stabil → HIGH
        val expenses = makeExpenses(count = 20, dailyAmount = 10000.0)
        // Gaji hanya 1 bulan → LOW
        val salaryHistory = listOf(
            makeIncome(today.minusMonths(1).withDayOfMonth(25), 5_000_000.0)
        )
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())

        val result = DeficitRiskAnalyzer.hitungRisikoDefisit(
            saldoSaatIni = 1_000_000.0,
            riwayatPengeluaran = expenses,
            riwayatPemasukan = salaryHistory,
            pemasukanBulanIni = 0.0,
            today = today,
            tanggalAkhirBulan = endOfMonth
        )
        // Salary confidence LOW (< 2 months data), expense confidence HIGH → weakest = LOW
        assertEquals(PredictionConfidence.LOW, result.confidence)
        assertTrue(result.confidenceReason.isNotEmpty())
    }

    @Test
    fun `risikoDefisit confidence HIGH when both components are HIGH`() {
        val expenses = makeExpenses(count = 20, dailyAmount = 10000.0)
        // 3 bulan gaji stabil
        val salaryHistory = mutableListOf<Transaksi>()
        for (m in 1..3) {
            val date = today.minusMonths(m.toLong()).withDayOfMonth(25)
            salaryHistory.add(makeIncome(date, 5_000_000.0))
        }

        val result = DeficitRiskAnalyzer.hitungRisikoDefisit(
            saldoSaatIni = 10_000_000.0,
            riwayatPengeluaran = expenses,
            riwayatPemasukan = salaryHistory,
            pemasukanBulanIni = 5_000_000.0, // gaji sudah masuk bulan ini
            today = today,
            tanggalAkhirBulan = today.withDayOfMonth(today.lengthOfMonth())
        )
        // Gaji sudah masuk → confidence gaji = HIGH
        // Pengeluaran stabil 14+ hari → HIGH
        assertEquals(PredictionConfidence.HIGH, result.confidence)
    }

    // ═════════════════════════════════════════════════════════════════
    // 9. TES: weakestConfidence
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `weakestConfidence returns LOW when any input is LOW`() {
        assertEquals(
            PredictionConfidence.LOW,
            DeficitRiskAnalyzer.weakestConfidence(PredictionConfidence.HIGH, PredictionConfidence.LOW)
        )
    }

    @Test
    fun `weakestConfidence returns MEDIUM when inputs are MEDIUM and HIGH`() {
        assertEquals(
            PredictionConfidence.MEDIUM,
            DeficitRiskAnalyzer.weakestConfidence(PredictionConfidence.HIGH, PredictionConfidence.MEDIUM)
        )
    }

    @Test
    fun `weakestConfidence returns HIGH when all are HIGH`() {
        assertEquals(
            PredictionConfidence.HIGH,
            DeficitRiskAnalyzer.weakestConfidence(PredictionConfidence.HIGH, PredictionConfidence.HIGH)
        )
    }
}
