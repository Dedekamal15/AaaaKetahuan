package com.example.aaaaketahuan.forecast.model

/**
 * Hasil prediksi pengeluaran akhir bulan.
 *
 * Dihitung dengan Simple Moving Average (SMA) dari pengeluaran harian
 * selama 30 hari kalender terakhir (atau data tersedia, minimal 14 hari).
 *
 * Minimum data historis:
 * - Data transaksi mencakup setidaknya 14 hari kalender.
 * - Setidaknya 7 hari data telah berlalu di bulan berjalan.
 */
sealed class EndOfMonthPrediction {

    /**
     * Prediksi berhasil dihitung.
     *
     * @property currentTotal Total pengeluaran bulan ini (realisasi).
     * @property monthlyIncome Total pemasukan bulan ini (anggaran).
     * @property predictedTotal Perkiraan total pengeluaran akhir bulan.
     * @property confidence Tingkat kepercayaan berdasarkan jumlah data historis.
     */
    data class Predicted(
        val currentTotal: Double,
        val monthlyIncome: Double,
        val predictedTotal: Double,
        val confidence: PredictionConfidence
    ) : EndOfMonthPrediction()

    /**
     * Data historis belum mencukupi untuk menghasilkan prediksi yang bermakna.
     *
     * @property reason Penjelasan dalam Bahasa Indonesia (untuk ditampilkan ke user).
     * @property daysAvailable Jumlah hari data yang tersedia saat ini.
     * @property daysRequired Jumlah hari minimal yang dibutuhkan.
     */
    data class InsufficientData(
        val reason: String,
        val daysAvailable: Int,
        val daysRequired: Int
    ) : EndOfMonthPrediction()
}
