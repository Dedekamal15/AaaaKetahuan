package com.example.aaaaketahuan.forecast.model

import java.time.LocalDate

/**
 * Hasil analisis risiko defisit — menggabungkan prediksi pengeluaran
 * (berbasis pola weekday/weekend) dengan prediksi pemasukan (gaji).
 *
 * @property status Label risiko dalam Bahasa Indonesia (misal "Sehat", "Defisit Ringan", dll).
 * @property icon Emoji indikator visual (🟢, 🟡, 🟠, 🔴).
 * @property rasio Rasio proyeksi saldo akhir terhadap total pemasukan proyeksi bulan ini.
 *   - Positif → surplus; Negatif → defisit.
 *   - -1.0 jika tidak ada pemasukan sama sekali (divide-by-zero guard).
 * @property proyeksiSaldoAkhir Hasil simulasi saldo berjalan pada akhir bulan.
 * @property tanggalPotensiDefisit Tanggal pertama kali saldo diproyeksikan negatif (null jika aman).
 * @property confidence Tingkat kepercayaan gabungan (weakest-link antara pengeluaran & pemasukan).
 * @property confidenceReason Penjelasan mengapa confidence rendah (string kosong jika HIGH/MEDIUM).
 */
data class RisikoDefisitResult(
    val status: String,
    val icon: String,
    val rasio: Double,
    val proyeksiSaldoAkhir: Double,
    val tanggalPotensiDefisit: LocalDate?,
    val confidence: PredictionConfidence,
    val confidenceReason: String
)
