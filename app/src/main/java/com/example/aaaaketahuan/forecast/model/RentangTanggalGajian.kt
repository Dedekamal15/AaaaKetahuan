package com.example.aaaaketahuan.forecast.model

/**
 * Rentang tanggal prediksi gajian berdasarkan histori.
 *
 * @property tanggalMin Hari-ke-berapa dalam bulan untuk kejadian gaji paling awal (nullable jika data < 2 bulan).
 * @property tanggalMax Hari-ke-berapa dalam bulan untuk kejadian gaji paling akhir.
 * @property confidence Tingkat kepercayaan prediksi:
 *   - HIGH:  rentang ≤ 5 hari
 *   - MEDIUM: rentang ≤ 10 hari
 *   - LOW:   rentang > 10 hari, atau histori < 2 bulan.
 * @property alasan Penjelasan jika confidence rendah (atau string kosong jika normal).
 */
data class RentangTanggalGajian(
    val tanggalMin: Int?,
    val tanggalMax: Int?,
    val confidence: PredictionConfidence,
    val alasan: String
)
