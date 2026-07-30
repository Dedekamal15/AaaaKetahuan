package com.example.aaaaketahuan.forecast.model

/**
 * Prediksi pengeluaran harian yang dibedakan berdasarkan jenis hari.
 *
 * @property nilaiWeekday Rata-rata prediksi pengeluaran untuk hari kerja (Senin–Jumat).
 * @property nilaiWeekend Rata-rata prediksi pengeluaran untuk akhir pekan (Sabtu–Minggu).
 */
data class PrediksiHarian(
    val nilaiWeekday: Double,
    val nilaiWeekend: Double
)
