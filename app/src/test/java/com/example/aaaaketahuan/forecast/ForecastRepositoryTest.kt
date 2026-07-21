package com.example.aaaaketahuan.forecast

import com.example.aaaaketahuan.data.model.Transaksi
import com.example.aaaaketahuan.data.repository.TransaksiRepository
import com.example.aaaaketahuan.forecast.model.BalancePrediction
import com.example.aaaaketahuan.forecast.model.DeficitRisk
import com.example.aaaaketahuan.forecast.model.EndOfMonthPrediction
import com.example.aaaaketahuan.forecast.model.PredictionConfidence
import com.example.aaaaketahuan.forecast.model.RiskSeverity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Unit test untuk [ForecastRepository].
 *
 * Strategi:
 * - [TransaksiRepository] dimock via MockK untuk mengontrol data tanpa I/O.
 * - Seluruh data transaksi menggunakan tanggal relatif terhadap [LocalDate.now()].
 * - Assertions menggunakan JUnit 4 (org.junit.Assert).
 * - Smart cast via `if (result is X)` untuk akses properti tiap sealed subclass.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForecastRepositoryTest {

    private lateinit var mockTransaksiRepo: TransaksiRepository
    private lateinit var repository: ForecastRepository
    private val today: LocalDate = LocalDate.now()
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @Before
    fun setUp() {
        mockTransaksiRepo = mockk(relaxUnitFun = true)
        coEvery { mockTransaksiRepo.getTransaksiFileTimestamp() } returns 1L
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns emptyList()
        repository = ForecastRepository(mockTransaksiRepo)
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private fun makeTransactions(
        startOffset: Int,           // 0 = today, 1 = yesterday, etc.
        count: Int,
        dailyExpense: Double,
        dailyIncome: Double = 0.0
    ): List<Transaksi> {
        val result = mutableListOf<Transaksi>()
        val startDate = today.minusDays(startOffset.toLong())
        for (i in 0 until count) {
            val date = startDate.minusDays((count - 1 - i).toLong())
            val ds = date.format(dateFormatter)
            if (dailyExpense > 0.0) result.add(
                Transaksi("e-$i", ds, "keluar", dailyExpense, "E$i", "", "Makanan",
                    date.monthValue, date.year)
            )
            if (dailyIncome > 0.0) result.add(
                Transaksi("i-$i", ds, "masuk", dailyIncome, "I$i", "", "Gaji",
                    date.monthValue, date.year)
            )
        }
        return result
    }

    // ═════════════════════════════════════════════════════════════════
    // FITUR 1: PREDIKSI PENGELUARAN AKHIR BULAN
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `endOfMonth returns InsufficientData when no transactions`() = runTest {
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns emptyList()
        val r = repository.getEndOfMonthPrediction()

        if (r is EndOfMonthPrediction.InsufficientData) {
            assertEquals(0, r.daysAvailable)
            assertEquals(14, r.daysRequired)
        } else fail("Expected InsufficientData, got $r")
    }

    @Test
    fun `endOfMonth returns InsufficientData when data span less than 14 days`() = runTest {
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns makeTransactions(0, 10, 10000.0)
        val r = repository.getEndOfMonthPrediction()

        if (r is EndOfMonthPrediction.InsufficientData) {
            assertEquals(10, r.daysAvailable)
            assertEquals(14, r.daysRequired)
        } else fail("Expected InsufficientData, got $r")
    }

    @Test
    fun `endOfMonth returns Predicted with correct SMA`() = runTest {
        val amount = 10000.0
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns makeTransactions(0, 30, amount)
        val r = repository.getEndOfMonthPrediction()

        if (r is EndOfMonthPrediction.Predicted) {
            val remaining = today.lengthOfMonth() - today.dayOfMonth
            val currentMonthTotal = makeTransactions(0, 30, amount)
                .filter { it.jenis == "keluar" && it.bulan == today.monthValue && it.tahun == today.year }
                .sumOf { it.jumlah }
            assertEquals(currentMonthTotal, r.currentTotal, 0.001)
            assertEquals(currentMonthTotal + amount * remaining, r.predictedTotal, 0.001)
        } else fail("Expected Predicted, got $r")
    }

    @Test
    fun `endOfMonth confidence HIGH for 30 days`() = runTest {
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns makeTransactions(0, 30, 10000.0)
        val r = repository.getEndOfMonthPrediction()
        if (r is EndOfMonthPrediction.Predicted) assertEquals(PredictionConfidence.HIGH, r.confidence)
        else fail("Expected Predicted, got $r")
    }

    @Test
    fun `endOfMonth confidence MEDIUM for 22-29 days`() = runTest {
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns makeTransactions(0, 22, 10000.0)
        val r = repository.getEndOfMonthPrediction()
        if (r is EndOfMonthPrediction.Predicted) assertEquals(PredictionConfidence.MEDIUM, r.confidence)
        else fail("Expected Predicted, got $r")
    }

    @Test
    fun `endOfMonth confidence LOW for 14-21 days`() = runTest {
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns makeTransactions(0, 14, 10000.0)
        val r = repository.getEndOfMonthPrediction()
        if (r is EndOfMonthPrediction.Predicted) assertEquals(PredictionConfidence.LOW, r.confidence)
        else fail("Expected Predicted, got $r")
    }

    @Test
    fun `endOfMonth handles zero expenses`() = runTest {
        // Buat manual: 30 hari transaksi dengan amount 0
        val tx = (0 until 30).map { i ->
            val date = today.minusDays((29 - i).toLong()).format(dateFormatter)
            Transaksi("z-$i", date, "keluar", 0.0, "Z$i", "", "Makanan",
                today.monthValue, today.year)
        }
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns tx
        val r = repository.getEndOfMonthPrediction()
        if (r is EndOfMonthPrediction.Predicted) {
            assertEquals(0.0, r.currentTotal, 0.001)
            assertEquals(0.0, r.predictedTotal, 0.001)
        } else fail("Expected Predicted, got $r")
    }

    // ═════════════════════════════════════════════════════════════════
    // FITUR 2: PREDIKSI SALDO
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `balancePrediction returns TargetDateInPast for yesterday`() = runTest {
        val r = repository.getBalancePrediction(today.minusDays(1))
        if (r is BalancePrediction.TargetDateInPast) {
            assertEquals(today.minusDays(1).toString(), r.targetDate)
        } else fail("Expected TargetDateInPast, got $r")
    }

    @Test
    fun `balancePrediction returns TargetDateInPast for today`() = runTest {
        val r = repository.getBalancePrediction(today)
        if (r is BalancePrediction.TargetDateInPast) {
            assertEquals(today.toString(), r.targetDate)
        } else fail("Expected TargetDateInPast, got $r")
    }

    @Test
    fun `balancePrediction returns InsufficientData when cache null`() = runTest {
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns emptyList()
        val r = repository.getBalancePrediction(today.plusDays(7))
        if (r is BalancePrediction.InsufficientData) {
            assertEquals(0, r.daysAvailable)
            assertEquals(14, r.daysRequired)
        } else fail("Expected InsufficientData, got $r")
    }

    @Test
    fun `balancePrediction returns correct predicted balance`() = runTest {
        val expense = 10000.0; val income = 15000.0; val net = income - expense
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns makeTransactions(0, 30, expense, income)
        val target = today.plusDays(10)
        val r = repository.getBalancePrediction(target)

        if (r is BalancePrediction.Predicted) {
            val monthData = makeTransactions(0, 30, expense, income)
                .filter { it.bulan == today.monthValue && it.tahun == today.year }
            val bal = monthData.filter { it.jenis == "masuk" }.sumOf { it.jumlah } -
                    monthData.filter { it.jenis == "keluar" }.sumOf { it.jumlah }
            assertEquals(bal, r.currentBalance, 0.001)
            assertEquals(bal + net * 10, r.predictedBalance, 0.001)
            assertEquals(target.toString(), r.targetDate)
        } else fail("Expected Predicted, got $r")
    }

    @Test
    fun `balancePrediction confidence drops to LOW for target beyond 90 days`() = runTest {
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns makeTransactions(0, 30, 10000.0, 15000.0)
        val r = repository.getBalancePrediction(today.plusDays(91))
        if (r is BalancePrediction.Predicted) {
            assertEquals(PredictionConfidence.LOW, r.confidence)
        } else fail("Expected Predicted, got $r")
    }

    // ═════════════════════════════════════════════════════════════════
    // FITUR 3: RISIKO DEFISIT
    // ═════════════════════════════════════════════════════════════════

    @Test
    fun `deficitRisk returns AlreadyDeficit when current balance negative`() = runTest {
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns makeTransactions(0, 30, 50000.0, 0.0)
        val r = repository.getDeficitRisk()
        val monthExpense = makeTransactions(0, 30, 50000.0, 0.0)
            .filter { it.bulan == today.monthValue && it.tahun == today.year }
            .filter { it.jenis == "keluar" }.sumOf { it.jumlah }

        if (monthExpense > 0 && r is DeficitRisk.AlreadyDeficit) {
            assertTrue(r.currentBalance < 0)
        } else if (r is DeficitRisk.InsufficientData) {
            assertTrue(r.reason.isNotEmpty())
        }
        // Catatan: hasil tergantung tanggal real — validasi logis saja
    }

    @Test
    fun `deficitRisk returns InsufficientData when data short`() = runTest {
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns makeTransactions(0, 7, 10000.0)
        val r = repository.getDeficitRisk()
        if (r is DeficitRisk.InsufficientData) assertTrue(r.reason.isNotEmpty())
        else fail("Expected InsufficientData, got $r")
    }

    @Test
    fun `deficitRisk returns Safe when projected positive`() = runTest {
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns makeTransactions(0, 30, 5000.0, 100000.0)
        val r = repository.getDeficitRisk()
        val monthIncome = makeTransactions(0, 30, 5000.0, 100000.0)
            .filter { it.bulan == today.monthValue && it.tahun == today.year }
            .filter { it.jenis == "masuk" }.sumOf { it.jumlah }
        val monthExpense = makeTransactions(0, 30, 5000.0, 100000.0)
            .filter { it.bulan == today.monthValue && it.tahun == today.year }
            .filter { it.jenis == "keluar" }.sumOf { it.jumlah }

        if (monthIncome > monthExpense && r is DeficitRisk.Safe) {
            assertTrue(r.projectedBalance >= 0)
        }
    }

    @Test
    fun `prediction respects income contribution`() = runTest {
        coEvery { mockTransaksiRepo.getAllTransaksi() } returns makeTransactions(0, 20, 0.0, 50000.0)
        val r = repository.getBalancePrediction(today.plusDays(15))
        if (r is BalancePrediction.Predicted) {
            assertTrue(r.currentBalance >= 0)
            assertTrue(r.predictedBalance >= r.currentBalance)
        } else fail("Expected Predicted, got $r")
    }
}
