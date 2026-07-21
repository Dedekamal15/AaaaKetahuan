package com.example.aaaaketahuan.forecast.model

/**
 * Tingkat kepercayaan terhadap hasil prediksi, ditentukan oleh jumlah
 * data historis yang tersedia saat perhitungan.
 *
 * - [HIGH]:  >= 30 hari data (satu bulan penuh atau lebih).
 * - [MEDIUM]: 22–29 hari data.
 * - [LOW]:    14–21 hari data (memenuhi syarat minimal, namun varians tinggi).
 */
enum class PredictionConfidence {
    LOW,
    MEDIUM,
    HIGH
}
