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
        // Spreadsheet ID from URL: https://docs.google.com/spreadsheets/d/13aeG7h75xREgcmgs1QUOj6gXfjkvvFVmtlfyny9r3sk
        private const val SPREADSHEET_ID = "13aeG7h75xREgcmgs1QUOj6gXfjkvvFVmtlfyny9r3sk"
        private const val SHEET_NAME = "Sheet1"
        private const val RANGE = "$SHEET_NAME!A:I"
        private const val APPLICATION_NAME = "AaaaKetahuan"
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

    /**
     * Appends a single transaction row to the spreadsheet.
     * Column order: ID, Tanggal, Jenis, Jumlah, NamaBarang, Keterangan, Kategori, Bulan, Tahun
     */
    suspend fun appendRow(transaksi: Transaksi): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
            val row = listOf<Any>(
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
            val values = listOf(row)
            val body = ValueRange().setValues(values)

            service.spreadsheets().values()
                .append(SPREADSHEET_ID, RANGE, body)
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
            val values: List<List<Any>> = transaksiList.map { transaksi ->
                listOf(
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
            }
            val body = ValueRange().setValues(values)

            service.spreadsheets().values()
                .append(SPREADSHEET_ID, RANGE, body)
                .setValueInputOption("USER_ENTERED")
                .execute()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if the spreadsheet is accessible.
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
            service.spreadsheets().get(SPREADSHEET_ID).execute()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
