package com.example.aaaaketahuan.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.example.aaaaketahuan.data.model.Transaksi
import com.example.aaaaketahuan.data.remote.GoogleSheetsHelper
import com.example.aaaaketahuan.util.CsvExporter
import com.example.aaaaketahuan.util.CsvImporter
import com.example.aaaaketahuan.util.JsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class TransaksiRepository @Inject constructor(
    private val context: Context,
    private val sheetsHelper: GoogleSheetsHelper
) {
    private val transaksiFile: File
        get() = File(context.filesDir, "transaksi.json")

    private val freqFile: File
        get() = File(context.filesDir, "nama_barang_freq.json")

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun getAllTransaksi(): List<Transaksi> = withContext(Dispatchers.IO) {
        JsonHelper.bacaTransaksi(transaksiFile)
    }

    /**
     * Dual-write: save locally first, then sync to Google Sheets in background.
     * If sync fails or no internet, marks isSynced = false for later retry.
     */
    suspend fun simpanTransaksi(transaksi: Transaksi) = withContext(Dispatchers.IO) {
        // Determine sync status based on network availability
        val hasNetwork = isNetworkAvailable()
        transaksi.isSynced = false // Will be updated after sync attempt

        // Save locally first
        val list = JsonHelper.bacaTransaksi(transaksiFile).toMutableList()
        list.add(transaksi)
        JsonHelper.simpanTransaksi(transaksiFile, list)

        // Update frequency
        val freq = JsonHelper.bacaFrekuensi(freqFile)
        freq[transaksi.namaBarang] = (freq[transaksi.namaBarang] ?: 0) + 1
        JsonHelper.simpanFrekuensi(freqFile, freq)

        // Attempt sync to Google Sheets if network is available
        if (hasNetwork) {
            val result = sheetsHelper.appendRow(transaksi)
            if (result.isSuccess) {
                transaksi.isSynced = true
                // Update local file with synced status
                val updatedList = JsonHelper.bacaTransaksi(transaksiFile).toMutableList()
                val index = updatedList.indexOfFirst { it.id == transaksi.id }
                if (index != -1) {
                    updatedList[index] = transaksi
                    JsonHelper.simpanTransaksi(transaksiFile, updatedList)
                }
            }
        }
    }

    suspend fun hapusTransaksi(id: String) = withContext(Dispatchers.IO) {
        val list = JsonHelper.bacaTransaksi(transaksiFile).toMutableList()
        list.removeAll { it.id == id }
        JsonHelper.simpanTransaksi(transaksiFile, list)
        // Frequency is NOT decremented — intentional UX decision
    }

    suspend fun editTransaksi(transaksi: Transaksi) = withContext(Dispatchers.IO) {
        val list = JsonHelper.bacaTransaksi(transaksiFile).toMutableList()
        val index = list.indexOfFirst { it.id == transaksi.id }
        if (index != -1) {
            val old = list[index]
            list[index] = transaksi
            JsonHelper.simpanTransaksi(transaksiFile, list)

            // Update frequency if namaBarang changed
            if (old.namaBarang != transaksi.namaBarang) {
                val freq = JsonHelper.bacaFrekuensi(freqFile)
                freq[transaksi.namaBarang] = (freq[transaksi.namaBarang] ?: 0) + 1
                JsonHelper.simpanFrekuensi(freqFile, freq)
            }
        }
    }

    suspend fun getTransaksiByBulan(bulan: Int, tahun: Int): List<Transaksi> =
        withContext(Dispatchers.IO) {
            val list = JsonHelper.bacaTransaksi(transaksiFile)
            list.filter { it.bulan == bulan && it.tahun == tahun }
        }

    fun getSaran(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val freq = JsonHelper.bacaFrekuensi(freqFile)
        return freq
            .filter { it.value >= 2 }
            .filter { it.key.lowercase().contains(query.lowercase()) }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    /**
     * Sync all pending transactions (isSynced = false) to Google Sheets.
     * Returns the number of successfully synced transactions.
     */
    suspend fun syncPendingTransactions(): Int = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext 0

        val allTransaksi = JsonHelper.bacaTransaksi(transaksiFile)
        val unsynced = allTransaksi.filter { !it.isSynced }

        if (unsynced.isEmpty()) return@withContext 0

        var syncedCount = 0
        val result = sheetsHelper.appendRows(unsynced)

        if (result.isSuccess) {
            syncedCount = unsynced.size
            // Mark all as synced
            val updatedList = allTransaksi.toMutableList()
            unsynced.forEach { unsyncedItem ->
                val index = updatedList.indexOfFirst { it.id == unsyncedItem.id }
                if (index != -1) {
                    updatedList[index] = updatedList[index].copy(isSynced = true)
                }
            }
            JsonHelper.simpanTransaksi(transaksiFile, updatedList)
        }

        syncedCount
    }

    suspend fun exportCsv(bulan: Int?, tahun: Int?): File = withContext(Dispatchers.IO) {
        val list = if (bulan != null && tahun != null) {
            getTransaksiByBulan(bulan, tahun)
        } else {
            getAllTransaksi()
        }
        CsvExporter.export(list, context)
    }

    suspend fun importCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream")
        val result = CsvImporter.import(inputStream)
        val list = JsonHelper.bacaTransaksi(transaksiFile).toMutableList()
        list.addAll(result.transaksiList)
        JsonHelper.simpanTransaksi(transaksiFile, list)

        // Update frequencies for all imported items
        val freq = JsonHelper.bacaFrekuensi(freqFile)
        result.transaksiList.forEach { t ->
            freq[t.namaBarang] = (freq[t.namaBarang] ?: 0) + 1
        }
        JsonHelper.simpanFrekuensi(freqFile, freq)

        result.successCount
    }
}
