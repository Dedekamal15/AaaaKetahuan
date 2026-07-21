package com.example.aaaaketahuan.data.remote

import android.content.Context
import com.example.aaaaketahuan.data.model.Transaksi
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.AddSheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleSheetsHelper(private val context: Context) {

    companion object {
        private const val APPLICATION_NAME = "AaaaKetahuan"
        const val DEFAULT_SPREADSHEET_ID = ""
        const val DEFAULT_SHEET_NAME = "Sheet1"
    }

    /** OAuth credential — set after user logs in with GoogleSignIn */
    var credential: GoogleAccountCredential? = null

    /** Pending consent intent from UserRecoverableAuthIOException, cleared after launch */
    var pendingAuthIntent: android.content.Intent? = null

    /** Dynamic spreadsheet config */
    var spreadsheetId: String = DEFAULT_SPREADSHEET_ID
    var sheetName: String = DEFAULT_SHEET_NAME
    private val range: String get() = "$sheetName!A:I"

    /** Singleton HTTP transport — created once, reused for all API calls */
    private val httpTransport by lazy {
        GoogleNetHttpTransport.newTrustedTransport()
    }

    /**
     * Set the authenticated Google account for Sheets API access.
     * Call this after user successfully logs in via GoogleSignIn.
     */
    fun setAccount(accountEmail: String) {
        credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(SheetsScopes.SPREADSHEETS)
        ).apply {
            selectedAccountName = accountEmail
        }
    }

    fun clearAccount() {
        credential = null
    }

    fun isAuthenticated(): Boolean = credential != null

    /**
     * Updates the spreadsheet target configuration.
     * Falls back to defaults if blank values are provided.
     */
    fun updateConfig(spreadsheetId: String?, sheetName: String?) {
        if (!spreadsheetId.isNullOrBlank()) this.spreadsheetId = spreadsheetId
        if (!sheetName.isNullOrBlank()) this.sheetName = sheetName
    }

    /**
     * Resets spreadsheet config to empty (no configured spreadsheet).
     */
    fun resetConfig() {
        spreadsheetId = DEFAULT_SPREADSHEET_ID
        sheetName = DEFAULT_SHEET_NAME
    }

    private fun getSheetsService(): Sheets? {
        val cred = credential ?: return null
        val jsonFactory = GsonFactory.getDefaultInstance()
        // Reuse the shared transport for connection pooling & resource efficiency
        return Sheets.Builder(httpTransport, jsonFactory, cred)
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    /**
     * Creates a new spreadsheet in the user's Google Drive with the proper headers.
     * Returns the new spreadsheet ID on success.
     */
    suspend fun createNewSpreadsheet(title: String = "AaaaKetahuan"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
                ?: return@withContext Result.failure(Exception("Belum login. Silakan hubungkan akun Google terlebih dahulu."))

            // Create the spreadsheet with title
            val spreadsheet = Spreadsheet()
                .setProperties(SpreadsheetProperties().setTitle(title))

            val result = service.spreadsheets().create(spreadsheet).execute()
            val id = result.spreadsheetId
            spreadsheetId = id

            // Write header row
            val header = ValueRange().setValues(
                listOf(listOf("id", "tanggal", "jenis", "jumlah", "namaBarang",
                    "keterangan", "kategori", "bulan", "tahun", "metodeBayar", "sumber"))
            )
            service.spreadsheets().values()
                .append(id, "$sheetName!A:K", header)
                .setValueInputOption("USER_ENTERED")
                .execute()

            Result.success(id)
        } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
            pendingAuthIntent = e.intent
            Result.failure(Exception("PERLU_IZIN"))
        } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException) {
            val msg = e.localizedMessage
                ?: e.cause?.localizedMessage
                ?: "Autentikasi ditolak oleh Google. Buat OAuth Client ID Android di Google Cloud Console dengan package name & SHA-1 yang benar."
            Result.failure(Exception("$msg"))
        } catch (e: com.google.android.gms.auth.GoogleAuthException) {
            val msg = e.localizedMessage
                ?: "Autentikasi ditolak oleh Google. Buat OAuth Client ID Android di Google Cloud Console dengan package name & SHA-1 yang benar."
            Result.failure(Exception("$msg"))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Jaringan bermasalah: ${e.localizedMessage ?: "cek koneksi internet"}"))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal (${e.javaClass.simpleName})"))
        }
    }

    /**
     * Creates a new sheet tab within the existing spreadsheet.
     * The new sheet is given a descriptive name (e.g., "Juli 2026").
     * After creation, [sheetName] is updated so future appends go to the new sheet.
     *
     * @param newSheetName Title for the new sheet tab (e.g., "Agustus 2026").
     * @return Result.success if the sheet was created, Result.failure otherwise.
     */
    suspend fun createNewSheetTab(newSheetName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
                ?: return@withContext Result.failure(Exception("Belum login."))

            val addSheet = AddSheetRequest()
                .setProperties(SheetProperties().setTitle(newSheetName))

            val request = Request().setAddSheet(addSheet)
            val batchRequest = BatchUpdateSpreadsheetRequest()
                .setRequests(listOf(request))

            service.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute()

            // Write header row to the new sheet
            val header = ValueRange().setValues(
                listOf(listOf("id", "tanggal", "jenis", "jumlah", "namaBarang",
                    "keterangan", "kategori", "bulan", "tahun", "metodeBayar", "sumber"))
            )
            service.spreadsheets().values()
                .append(spreadsheetId, "$newSheetName!A:K", header)
                .setValueInputOption("USER_ENTERED")
                .execute()

            // Update current sheet name so future writes go here
            sheetName = newSheetName

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membuat sheet baru: ${e.localizedMessage ?: e.javaClass.simpleName}"))
        }
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
        transaksi.tahun.toString(),
        transaksi.metodeBayar,
        transaksi.sumber
    )

    /**
     * Appends a single transaction row to the spreadsheet.
     * Column order: ID, Tanggal, Jenis, Jumlah, NamaBarang, Keterangan, Kategori, Bulan, Tahun, MetodeBayar, Sumber
     */
    suspend fun appendRow(transaksi: Transaksi): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
                ?: return@withContext Result.failure(Exception("Not authenticated"))
            val values = listOf(buildRow(transaksi))
            val body = ValueRange().setValues(values)

            service.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal simpan ke spreadsheet: ${e.localizedMessage ?: e.javaClass.simpleName}"))
        }
    }

    /**
     * Appends multiple transaction rows to the spreadsheet in a single batch.
     */
    suspend fun appendRows(transaksiList: List<Transaksi>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
                ?: return@withContext Result.failure(Exception("Not authenticated"))
            val values: List<List<Any>> = transaksiList.map { buildRow(it) }
            val body = ValueRange().setValues(values)

            service.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal sinkronisasi batch: ${e.localizedMessage ?: e.javaClass.simpleName}"))
        }
    }

    /**
     * Returns the names of all sheet tabs in the configured spreadsheet.
     * Used when restoring data — we read every tab to collect all transactions.
     */
    suspend fun getAllSheetTabs(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
                ?: return@withContext Result.failure(Exception("Belum login."))
            val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
            val titles = spreadsheet.sheets.map { it.properties.title }
            Result.success(titles)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membaca daftar sheet: ${e.localizedMessage ?: e.javaClass.simpleName}"))
        }
    }

    /**
     * Reads all data rows from a specific sheet tab (excluding the header row).
     * Each row is returned as a List<Any> in column order A–K:
     * [id, tanggal, jenis, jumlah, namaBarang, keterangan, kategori, bulan, tahun, metodeBayar, sumber]
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun readAllRows(sheetName: String): Result<List<List<Any>>> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
                ?: return@withContext Result.failure(Exception("Belum login."))
            val response = service.spreadsheets().values()
                .get(spreadsheetId, "$sheetName!A:K")
                .execute()
            val rawValues = response.values
            val allRows: List<List<Any>> = if (rawValues != null) {
                rawValues as List<List<Any>>
            } else {
                emptyList()
            }
            // Skip header row (first row)
            val dataRows: List<List<Any>> = if (allRows.size > 1) allRows.drop(1) else emptyList()
            Result.success(dataRows)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membaca data sheet: ${e.localizedMessage ?: e.javaClass.simpleName}"))
        }
    }

    /**
     * Checks if the configured spreadsheet is accessible.
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
                ?: return@withContext Result.failure(Exception("Not authenticated"))
            service.spreadsheets().get(spreadsheetId).execute()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal uji koneksi: ${e.localizedMessage ?: e.javaClass.simpleName}"))
        }
    }
}
