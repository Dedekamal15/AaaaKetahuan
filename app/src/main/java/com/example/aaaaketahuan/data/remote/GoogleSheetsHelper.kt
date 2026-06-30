package com.example.aaaaketahuan.data.remote

import android.content.Context
import com.example.aaaaketahuan.R
import com.example.aaaaketahuan.data.model.Transaksi
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

class GoogleSheetsHelper(private val context: Context) {

    companion object {
        private const val APPLICATION_NAME = "AaaaKetahuan"
        const val DEFAULT_SPREADSHEET_ID = "13aeG7h75xREgcmgs1QUOj6gXfjkvvFVmtlfyny9r3sk"
        const val DEFAULT_SHEET_NAME = "Sheet1"
    }

    /** Dynamic spreadsheet config — can be updated via updateConfig() */
    var spreadsheetId: String = DEFAULT_SPREADSHEET_ID
    var sheetName: String = DEFAULT_SHEET_NAME
    private val range: String get() = "$sheetName!A:I"

    /**
     * Updates the spreadsheet target configuration.
     * Falls back to defaults if blank values are provided.
     */
    fun updateConfig(spreadsheetId: String?, sheetName: String?) {
        if (!spreadsheetId.isNullOrBlank()) this.spreadsheetId = spreadsheetId
        if (!sheetName.isNullOrBlank()) this.sheetName = sheetName
    }

    /**
     * Resets spreadsheet config to default.
     */
    fun resetConfig() {
        spreadsheetId = DEFAULT_SPREADSHEET_ID
        sheetName = DEFAULT_SHEET_NAME
    }

    private fun getCredentials(): GoogleCredentials {
        val inputStream = context.resources.openRawResource(R.raw.credentials)
        return GoogleCredentials.fromStream(inputStream)
            .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS))
    }

    private fun getSheetsService(): Sheets {
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        val credentials = getCredentials()
        val requestInitializer = HttpCredentialsAdapter(credentials)
        return Sheets.Builder(transport, jsonFactory, requestInitializer)
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    private fun buildRow(transaksi: Transaksi): List<Any> = listOf(
        transaksi.id,
        transaksi.tanggal,
        transaksi.jenis,
        transaksi.jumlah.toString(),
        transaksi.namaBarang,
        transaksi.keterangan,
        transaksi.kategori,
        transaksi.bulan.toString(),
        transaksi.tahun.toString()
    )

    /**
     * Appends a single transaction row to the spreadsheet.
     * Column order: ID, Tanggal, Jenis, Jumlah, NamaBarang, Keterangan, Kategori, Bulan, Tahun
     */
    suspend fun appendRow(transaksi: Transaksi): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
            val values = listOf(buildRow(transaksi))
            val body = ValueRange().setValues(values)

            service.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Appends multiple transaction rows to the spreadsheet in a single batch.
     */
    suspend fun appendRows(transaksiList: List<Transaksi>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
            val values: List<List<Any>> = transaksiList.map { buildRow(it) }
            val body = ValueRange().setValues(values)

            service.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if the configured spreadsheet is accessible.
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
            service.spreadsheets().get(spreadsheetId).execute()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
