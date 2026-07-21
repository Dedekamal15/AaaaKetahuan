package com.example.aaaaketahuan.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aaaaketahuan.forecast.model.BalancePrediction
import com.example.aaaaketahuan.forecast.model.DeficitRisk
import com.example.aaaaketahuan.forecast.model.EndOfMonthPrediction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel untuk modul forecasting on-device.
 *
 * **TERPISAH** dari [com.example.aaaaketahuan.viewmodel.TransaksiViewModel]
 * — jangan menambahkan state atau logic forecasting ke ViewModel lama.
 *
 * State management: [MutableStateFlow] + [asStateFlow], konsisten dengan
 * pola yang sudah ada di project.
 *
 * Seluruh operasi:
 * - Baca data via [ForecastRepository] (yang menangani I/O + CPU sendiri).
 * - Error handling: try/catch dengan [CancellationException] rethrow.
 */
@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val forecastRepository: ForecastRepository
) : ViewModel() {

    // ─── State ────────────────────────────────────────────────────────

    private val _endOfMonthPrediction = MutableStateFlow<EndOfMonthPrediction?>(null)

    /** Prediksi pengeluaran akhir bulan. null sebelum load pertama. */
    val endOfMonthPrediction: StateFlow<EndOfMonthPrediction?> =
        _endOfMonthPrediction.asStateFlow()

    private val _balancePrediction = MutableStateFlow<BalancePrediction?>(null)

    /** Prediksi saldo pada tanggal tertentu. null sebelum dipanggil. */
    val balancePrediction: StateFlow<BalancePrediction?> =
        _balancePrediction.asStateFlow()

    private val _deficitRisk = MutableStateFlow<DeficitRisk?>(null)

    /** Risiko defisit akhir bulan. null sebelum load pertama. */
    val deficitRisk: StateFlow<DeficitRisk?> =
        _deficitRisk.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    /** Indikator loading — true saat salah satu prediksi sedang dihitung. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    /** Pesan error terakhir. Di-clear via [clearError]. */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ─── Fitur 1: Prediksi Pengeluaran Akhir Bulan ────────────────────

    /**
     * Memuat prediksi pengeluaran akhir bulan.
     *
     * Hasil disimpan di [endOfMonthPrediction].
     *
     * Minimum data: 14 hari historis + 7 hari di bulan berjalan.
     * Jika belum mencukupi, state akan berisi [EndOfMonthPrediction.InsufficientData].
     */
    fun loadEndOfMonthPrediction() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = forecastRepository.getEndOfMonthPrediction()
                _endOfMonthPrediction.value = result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat prediksi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Fitur 2: Prediksi Saldo pada Tanggal Tertentu ──────────────

    /**
     * Memuat prediksi saldo pada [targetDate].
     *
     * Hasil disimpan di [balancePrediction].
     *
     * @param targetDate Tanggal target (LocalDate). Harus > hari ini.
     *   Jika <= hari ini, state akan berisi [BalancePrediction.TargetDateInPast].
     */
    fun loadBalancePrediction(targetDate: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = forecastRepository.getBalancePrediction(targetDate)
                _balancePrediction.value = result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat prediksi saldo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Fitur 3: Deteksi Risiko Defisit ────────────────────────────

    /**
     * Memuat deteksi risiko defisit akhir bulan.
     *
     * Hasil disimpan di [deficitRisk].
     *
     * Minimum data sama dengan [loadEndOfMonthPrediction].
     */
    fun loadDeficitRisk() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = forecastRepository.getDeficitRisk()
                _deficitRisk.value = result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat risiko defisit: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Utility ──────────────────────────────────────────────────────

    /**
     * Me-refresh semua prediksi yang bisa di-load tanpa input user:
     * - [loadEndOfMonthPrediction]
     * - [loadDeficitRisk]
     *
     * [loadBalancePrediction] tidak di-refresh otomatis karena
     * membutuhkan parameter [targetDate] dari user.
     */
    fun refreshAll() {
        _endOfMonthPrediction.value = null
        _deficitRisk.value = null
        loadEndOfMonthPrediction()
        loadDeficitRisk()
    }

    /** Menghapus pesan error terakhir. */
    fun clearError() {
        _errorMessage.value = null
    }
}
