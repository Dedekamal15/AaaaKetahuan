package com.example.aaaaketahuan.data.model

import java.util.UUID

data class Transaksi(
    val id: String = UUID.randomUUID().toString(),
    val tanggal: String,          // format: "yyyy-MM-dd"
    val jenis: String,            // "masuk" or "keluar"
    val jumlah: Double,
    val namaBarang: String,
    val keterangan: String,
    val kategori: String,
    val bulan: Int,               // 1-12
    val tahun: Int,
    val metodeBayar: String = "", // "Cash", "Kredit", "E-Wallet", "Transfer", "QRIS"
    val sumber: String = "",      // "Gaji", "Lainnya" (for income only)
    var isSynced: Boolean = false // sync status to Google Sheets
)
