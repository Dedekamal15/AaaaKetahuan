package com.example.aaaaketahuan.ui.navigation

object NavRoute {
    const val DASHBOARD = "dashboard"
    const val INPUT = "input"
    const val PEMASUKAN = "pemasukan"
    const val RIWAYAT = "riwayat"
    const val GRAFIK = "grafik"
    const val PENGATURAN = "pengaturan"
    const val EXPORT_IMPORT = "export_import"
    const val EDIT_TRANSAKSI = "edit_transaksi/{transaksiId}"

    fun editTransaksi(transaksiId: String): String = "edit_transaksi/$transaksiId"
}
