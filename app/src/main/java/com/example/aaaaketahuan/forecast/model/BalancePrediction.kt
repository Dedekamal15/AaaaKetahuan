package com.example.aaaaketahuan.forecast.model

import java.time.LocalDate

/**
 * Hasil prediksi saldo pada tanggal tertentu di masa depan.
 *
 * Perhitungan: saldo saat ini + (rata-rata pemasukan harian × sisa hari)
 *               - (rata-rata pengeluaran harian × sisa hari)
 *
 * Minimum data historis:
 * - Data transaksi mencakup setidaknya 14 hari kalender.
 * - Tanggal target harus berada di masa depan (setelah hari ini).
 */
sealed class BalancePrediction {

    /**
     * Prediksi berhasil dihitung.
     *
     * @property currentBalance Saldo aktual saat ini (masuk - keluar bulan berjalan).
     * @property predictedBalance Perkiraan saldo pada [targetDate].
     * @property targetDate Tanggal target yang diminta (format "yyyy-MM-dd").
     * @property confidence Tingkat kepercayaan. Diturunkan ke LOW jika target >90 hari.
     */
    data class Predicted(
        val currentBalance: Double,
        val predictedBalance: Double,
        val targetDate: String,
        val confidence: PredictionConfidence
    ) : BalancePrediction()

    /**
     * Data historis belum mencukupi.
     *
     * @property reason Penjelasan dalam Bahasa Indonesia.
     * @property daysAvailable Jumlah hari data yang tersedia.
     * @property daysRequired Jumlah hari minimal yang dibutuhkan.
     */
    data class InsufficientData(
        val reason: String,
        val daysAvailable: Int,
        val daysRequired: Int
    ) : BalancePrediction()

    /**
     * Tanggal target sudah lewat (hari ini atau sebelumnya).
     * Prediksi saldo hanya bermakna untuk tanggal di masa depan.
     *
     * @property targetDate Tanggal yang diminta (format "yyyy-MM-dd").
     */
    data class TargetDateInPast(
        val targetDate: String
    ) : BalancePrediction()
}
