package com.example.aaaaketahuan.forecast.model

/**
 * Hasil deteksi risiko defisit (saldo negatif) pada akhir bulan.
 *
 * Dihitung dengan membandingkan proyeksi saldo akhir bulan terhadap
 * rata-rata pengeluaran bulanan. Jika proyeksi saldo negatif, tingkat
 * keparahan ditentukan oleh rasio defisit terhadap pengeluaran bulanan.
 *
 * Minimum data historis sama dengan [EndOfMonthPrediction]:
 * - 14 hari data historis
 * - 7 hari data di bulan berjalan
 */
sealed class DeficitRisk {

    /**
     * Aman: proyeksi saldo akhir bulan positif.
     *
     * @property projectedBalance Saldo yang diproyeksikan.
     * @property buffer Selisih saldo terhadap nol (surplus).
     */
    data class Safe(
        val projectedBalance: Double,
        val buffer: Double
    ) : DeficitRisk()

    /**
     * Berisiko defisit: proyeksi saldo akhir bulan negatif.
     *
     * @property level Tingkat keparahan (RENDAH/SEDANG/TINGGI).
     * @property projectedBalance Saldo yang diproyeksikan (negatif).
     * @property shortfallAmount Jumlah defisit dalam rupiah (nilai absolut).
     */
    data class AtRisk(
        val level: RiskSeverity,
        val projectedBalance: Double,
        val shortfallAmount: Double
    ) : DeficitRisk()

    /**
     * Sudah defisit: saldo saat ini sudah negatif.
     *
     * @property currentBalance Saldo aktual saat ini (negatif).
     */
    data class AlreadyDeficit(
        val currentBalance: Double
    ) : DeficitRisk()

    /**
     * Data belum mencukupi untuk deteksi risiko.
     *
     * @property reason Penjelasan dalam Bahasa Indonesia.
     */
    data class InsufficientData(
        val reason: String
    ) : DeficitRisk()
}

/**
 * Tingkat keparahan risiko defisit.
 *
 * - [LOW]:     defisit < 20% dari rata-rata pengeluaran bulanan.
 * - [MODERATE]: defisit 20–49% dari rata-rata pengeluaran bulanan.
 * - [HIGH]:    defisit >= 50% dari rata-rata pengeluaran bulanan.
 */
enum class RiskSeverity {
    LOW,
    MODERATE,
    HIGH
}
