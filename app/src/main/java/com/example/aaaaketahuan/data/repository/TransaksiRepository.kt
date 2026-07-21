package com.example.aaaaketahuan.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.example.aaaaketahuan.data.model.Transaksi
import com.example.aaaaketahuan.data.remote.GoogleSheetsHelper
import com.example.aaaaketahuan.util.CsvExporter
import com.example.aaaaketahuan.util.CsvImporter
import com.example.aaaaketahuan.util.JsonHelper
import com.example.aaaaketahuan.util.NotificationHelper
import com.example.aaaaketahuan.util.ReminderScheduler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class TransaksiRepository @Inject constructor(
    private val context: Context,
    private val sheetsHelper: GoogleSheetsHelper
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences("aaaaketahuan_config", Context.MODE_PRIVATE)

    private val transaksiFile: File
        get() = File(context.filesDir, "transaksi.json")

    private val freqFile: File
        get() = File(context.filesDir, "nama_barang_freq.json")

    private var configLoaded = false

    /** Mutex serializing all read-modify-write on [transaksiFile] for thread safety */
    private val transaksiFileMutex = Mutex()

    /** Mutex serializing all read-modify-write on [freqFile] */
    private val freqFileMutex = Mutex()

    private fun ensureConfigLoaded() {
        if (!configLoaded) {
            loadSpreadsheetConfigToHelper()
            configLoaded = true
        }
    }

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
     * Returns the last modification timestamp of [transaksiFile].
     *
     * Digunakan oleh [com.example.aaaaketahuan.forecast.ForecastRepository]
     * untuk mendeteksi perubahan data tanpa harus membaca ulang seluruh file —
     * jika timestamp tidak berubah, cache agregasi harian masih valid.
     *
     * @return Millis since epoch, atau 0L jika file belum pernah dibuat.
     */
    suspend fun getTransaksiFileTimestamp(): Long = withContext(Dispatchers.IO) {
        transaksiFile.lastModified()
    }

    /**
     * Fast check for pending sync count — reads file and counts unsynced without filter.
     */
    suspend fun getPendingSyncCount(): Int = withContext(Dispatchers.IO) {
        val all = JsonHelper.bacaTransaksi(transaksiFile)
        all.count { !it.isSynced }
    }

    /**
     * Dual-write: save locally first, then sync to Google Sheets in background.
     * If sync fails or no internet, marks isSynced = false for later retry.
     */
    suspend fun simpanTransaksi(transaksi: Transaksi) = withContext(Dispatchers.IO) {
        ensureConfigLoaded()
        // Determine sync status based on network availability
        val hasNetwork = isNetworkAvailable()
        val transaksiToSave = transaksi.copy(isSynced = false)

        transaksiFileMutex.withLock {
            // Save locally first
            val list = JsonHelper.bacaTransaksi(transaksiFile).toMutableList()
            list.add(transaksiToSave)
            JsonHelper.simpanTransaksi(transaksiFile, list)
        }

        // Update frequency
        freqFileMutex.withLock {
            val freq = JsonHelper.bacaFrekuensi(freqFile)
            freq[transaksiToSave.namaBarang] = (freq[transaksiToSave.namaBarang] ?: 0) + 1
            JsonHelper.simpanFrekuensi(freqFile, freq)
        }

        // Attempt sync to Google Sheets if network is available
        if (hasNetwork) {
            val result = sheetsHelper.appendRow(transaksiToSave)
            if (result.isSuccess) {
                transaksiFileMutex.withLock {
                    val updatedList = JsonHelper.bacaTransaksi(transaksiFile).toMutableList()
                    val index = updatedList.indexOfFirst { it.id == transaksiToSave.id }
                    if (index != -1) {
                        updatedList[index] = updatedList[index].copy(isSynced = true)
                        JsonHelper.simpanTransaksi(transaksiFile, updatedList)
                    }
                }
            }
        }
    }

    suspend fun hapusTransaksi(id: String) = withContext(Dispatchers.IO) {
        transaksiFileMutex.withLock {
            val list = JsonHelper.bacaTransaksi(transaksiFile).toMutableList()
            list.removeAll { it.id == id }
            JsonHelper.simpanTransaksi(transaksiFile, list)
        }
        // Frequency is NOT decremented — intentional UX decision
    }

    suspend fun editTransaksi(transaksi: Transaksi) = withContext(Dispatchers.IO) {
        transaksiFileMutex.withLock {
            val list = JsonHelper.bacaTransaksi(transaksiFile).toMutableList()
            val index = list.indexOfFirst { it.id == transaksi.id }
            if (index != -1) {
                val old = list[index]
                // Mark as unsynced so it gets re-synced
                list[index] = transaksi.copy(isSynced = false)
                JsonHelper.simpanTransaksi(transaksiFile, list)

                // Update frequency if namaBarang changed
                if (old.namaBarang != transaksi.namaBarang) {
                    freqFileMutex.withLock {
                        val freq = JsonHelper.bacaFrekuensi(freqFile)
                        freq[transaksi.namaBarang] = (freq[transaksi.namaBarang] ?: 0) + 1
                        JsonHelper.simpanFrekuensi(freqFile, freq)
                    }
                }
            }
        }
    }

    suspend fun getTransaksiByBulan(bulan: Int, tahun: Int): List<Transaksi> =
        withContext(Dispatchers.IO) {
            val list = JsonHelper.bacaTransaksi(transaksiFile)
            list.filter { it.bulan == bulan && it.tahun == tahun }
        }

    suspend fun getSaran(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val freq = JsonHelper.bacaFrekuensi(freqFile)
        freq
            .filter { it.value >= 2 }
            .filter { it.key.lowercase().contains(query.lowercase()) }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    /**
     * Returns true if local transaksi.json exists and has data.
     * Used to detect fresh install vs existing user.
     */
    fun hasLocalData(): Boolean {
        return transaksiFile.exists() && transaksiFile.length() > 0
    }

    /**
     * Reads ALL data from every sheet tab in the connected spreadsheet,
     * parses each row into a [Transaksi], deduplicates by ID, and saves
     * everything to local JSON. Also rebuilds the frequency map.
     *
     * @return Result with the number of transactions restored.
     */
    suspend fun restoreAllFromSheet(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            ensureConfigLoaded()
            val tabsResult = sheetsHelper.getAllSheetTabs()
            if (tabsResult.isFailure) {
                return@withContext Result.failure(tabsResult.exceptionOrNull()!!)
            }

            val sheetTabs = tabsResult.getOrThrow()
            val allTransaksi = linkedMapOf<String, Transaksi>() // dedup by ID, preserve order

            for (sheetTab in sheetTabs) {
                val rowsResult = sheetsHelper.readAllRows(sheetTab)
                if (rowsResult.isSuccess) {
                    val rows = rowsResult.getOrThrow()
                    for (row in rows) {
                        try {
                            val t = parseRowListToTransaksi(row)
                            // Only add if ID not already seen
                            if (t.id !in allTransaksi) {
                                allTransaksi[t.id] = t
                            }
                        } catch (_: Exception) {
                            // Skip malformed rows silently
                        }
                    }
                }
            }

            val restoredList = allTransaksi.values.toList()

            // Save to local JSON atomically
            transaksiFileMutex.withLock {
                JsonHelper.simpanTransaksi(transaksiFile, restoredList)
            }

            // Rebuild frequency map
            freqFileMutex.withLock {
                val freq = mutableMapOf<String, Int>()
                restoredList.forEach { t ->
                    freq[t.namaBarang] = (freq[t.namaBarang] ?: 0) + 1
                }
                JsonHelper.simpanFrekuensi(freqFile, freq)
            }

            Result.success(restoredList.size)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal restore data: ${e.localizedMessage ?: e.javaClass.simpleName}"))
        }
    }

    /**
     * Parses a row from Google Sheets API (List<Any>) into a [Transaksi].
     * Column order (A–K): id, tanggal, jenis, jumlah, namaBarang, keterangan,
     *                     kategori, bulan, tahun, metodeBayar, sumber
     */
    private fun parseRowListToTransaksi(row: List<Any>): Transaksi {
        return Transaksi(
            id = row.getOrElse(0) { "" }.toString().ifBlank { UUID.randomUUID().toString() },
            tanggal = row.getOrElse(1) { "" }.toString(),
            jenis = row.getOrElse(2) { "" }.toString(),
            jumlah = row.getOrElse(3) { "0" }.toString().toDoubleOrNull() ?: 0.0,
            namaBarang = row.getOrElse(4) { "" }.toString(),
            keterangan = row.getOrElse(5) { "" }.toString(),
            kategori = row.getOrElse(6) { "" }.toString(),
            bulan = row.getOrElse(7) { "0" }.toString().toIntOrNull() ?: 1,
            tahun = row.getOrElse(8) { "0" }.toString().toIntOrNull() ?: 2024,
            metodeBayar = row.getOrElse(9) { "" }.toString(),
            sumber = row.getOrElse(10) { "" }.toString(),
            isSynced = true  // Data from Google Sheets considered already synced
        )
    }

    /**
     * Sync all pending transactions (isSynced = false) to Google Sheets.
     * Returns the number of successfully synced transactions.
     */
    suspend fun syncPendingTransactions(): Int = withContext(Dispatchers.IO) {
        ensureConfigLoaded()
        if (!isNetworkAvailable()) return@withContext 0

        val unsynced = transaksiFileMutex.withLock {
            val all = JsonHelper.bacaTransaksi(transaksiFile)
            all.filter { !it.isSynced }
        }

        if (unsynced.isEmpty()) return@withContext 0

        var syncedCount = 0
        val result = sheetsHelper.appendRows(unsynced)

        if (result.isSuccess) {
            syncedCount = unsynced.size
            transaksiFileMutex.withLock {
                val updatedList = JsonHelper.bacaTransaksi(transaksiFile).toMutableList()
                unsynced.forEach { unsyncedItem ->
                    val index = updatedList.indexOfFirst { it.id == unsyncedItem.id }
                    if (index != -1) {
                        updatedList[index] = updatedList[index].copy(isSynced = true)
                    }
                }
                JsonHelper.simpanTransaksi(transaksiFile, updatedList)
            }
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
        val result = context.contentResolver.openInputStream(uri)?.use { stream ->
            CsvImporter.import(stream)
        } ?: throw IllegalArgumentException("Cannot open input stream")

        transaksiFileMutex.withLock {
            val list = JsonHelper.bacaTransaksi(transaksiFile).toMutableList()
            list.addAll(result.transaksiList)
            JsonHelper.simpanTransaksi(transaksiFile, list)
        }

        // Update frequencies for all imported items
        freqFileMutex.withLock {
            val freq = JsonHelper.bacaFrekuensi(freqFile)
            result.transaksiList.forEach { t ->
                freq[t.namaBarang] = (freq[t.namaBarang] ?: 0) + 1
            }
            JsonHelper.simpanFrekuensi(freqFile, freq)
        }

        result.successCount
    }

    // ─── Spreadsheet Config ──────────────────────────────────────────

    fun getSpreadsheetId(): String {
        return prefs.getString(KEY_SPREADSHEET_ID, sheetsHelper.spreadsheetId) ?: sheetsHelper.spreadsheetId
    }

    fun getSheetName(): String {
        return prefs.getString(KEY_SHEET_NAME, sheetsHelper.sheetName) ?: sheetsHelper.sheetName
    }

    fun isSpreadsheetConnected(): Boolean {
        return getSpreadsheetId().isNotBlank() && getConnectedAccount() != null
    }

    fun getConnectedAccount(): String? {
        return prefs.getString(KEY_ACCOUNT_EMAIL, null)
    }

    @SuppressLint("CommitPrefEdits")
    fun connectGoogleAccount(accountEmail: String) {
        prefs.edit()
            .putString(KEY_ACCOUNT_EMAIL, accountEmail)
            .apply()
        sheetsHelper.setAccount(accountEmail)
    }

    /**
     * Save spreadsheet ID + sheet name after successful OAuth connection.
     * Called after the app creates or the user selects a spreadsheet.
     * Also persists the mapping email → spreadsheet so the same user
     * can reuse it on next login instead of creating a new one.
     */
    @SuppressLint("CommitPrefEdits")
    fun saveSpreadsheetConfig(spreadsheetId: String, sheetName: String = "Sheet1") {
        prefs.edit()
            .putString(KEY_SPREADSHEET_ID, spreadsheetId)
            .putString(KEY_SHEET_NAME, sheetName)
            .apply()
        sheetsHelper.updateConfig(spreadsheetId, sheetName)
        // Persist mapping so same email reuses this spreadsheet
        val email = getConnectedAccount()
        if (email != null) {
            saveSpreadsheetForEmail(email, spreadsheetId, sheetName)
        }
    }

    /**
     * Checks if the given email already has a saved spreadsheet config.
     * If found, restores it to [saveSpreadsheetConfig] so the app reuses it.
     * @return true if a previous spreadsheet was restored, false otherwise.
     */
    fun restoreSpreadsheetForEmail(email: String): Boolean {
        val savedInfo = getSavedSpreadsheetForEmail(email) ?: return false
        val (savedId, savedName) = savedInfo
        if (savedId.isBlank()) return false
        saveSpreadsheetConfig(savedId, savedName)
        return true
    }

    /**
     * Returns the names of all sheet tabs in the connected spreadsheet.
     * Delegates to GoogleSheetsHelper.
     */
    suspend fun getAllSheetTabs(): Result<List<String>> {
        ensureConfigLoaded()
        return sheetsHelper.getAllSheetTabs()
    }

    /**
     * Returns the number of data rows (excluding header) in the given sheet tab.
     */
    suspend fun readAllRowsFromSheet(sheetName: String): Result<Int> {
        ensureConfigLoaded()
        val result = sheetsHelper.readAllRows(sheetName)
        return result.map { it.size }
    }

    /**
     * Returns the saved spreadsheet info for a given email,
     * or null if none was saved yet.
     */
    private fun getSavedSpreadsheetForEmail(email: String): Pair<String, String>? {
        val map = loadEmailSpreadsheetMap()
        val json = map[email] ?: return null
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            val data: Map<String, String> = gson.fromJson(json, type)
            val id = data["spreadsheetId"] ?: return null
            val name = data["sheetName"] ?: "Sheet1"
            Pair(id, name)
        } catch (e: Exception) { null }
    }

    private fun saveSpreadsheetForEmail(email: String, spreadsheetId: String, sheetName: String) {
        val map = loadEmailSpreadsheetMap().toMutableMap()
        val infoJson = gson.toJson(mapOf(
            "spreadsheetId" to spreadsheetId,
            "sheetName" to sheetName
        ))
        map[email] = infoJson
        saveEmailSpreadsheetMap(map)
    }

    private fun loadEmailSpreadsheetMap(): Map<String, String> {
        val json = prefs.getString(KEY_EMAIL_SPREADSHEET_MAP, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { emptyMap() }
    }

    private fun saveEmailSpreadsheetMap(map: Map<String, String>) {
        prefs.edit().putString(KEY_EMAIL_SPREADSHEET_MAP, gson.toJson(map)).apply()
    }

    @SuppressLint("CommitPrefEdits")
    fun disconnectSpreadsheet() {
        prefs.edit()
            .remove(KEY_SPREADSHEET_ID)
            .remove(KEY_SHEET_NAME)
            .remove(KEY_ACCOUNT_EMAIL)
            .apply()
        sheetsHelper.resetConfig()
        sheetsHelper.clearAccount()
    }

    /**
     * Logout: clears all account credentials, spreadsheet config, and OAuth tokens.
     * After calling this, [getConnectedAccount] returns null and the app
     * will show the AuthScreen on next recomposition.
     */
    @SuppressLint("CommitPrefEdits")
    fun logout() {
        prefs.edit()
            .remove(KEY_SPREADSHEET_ID)
            .remove(KEY_SHEET_NAME)
            .remove(KEY_ACCOUNT_EMAIL)
            .remove(KEY_USER_DISPLAY_NAME)
            .apply()
        sheetsHelper.resetConfig()
        sheetsHelper.clearAccount()
    }

    fun loadSpreadsheetConfigToHelper() {
        val id = getSpreadsheetId()
        val name = getSheetName()
        sheetsHelper.updateConfig(id, name)
        // Restore OAuth credential if account was previously connected
        val email = getConnectedAccount()
        if (email != null) {
            sheetsHelper.setAccount(email)
        }
    }

    /**
     * Creates a new spreadsheet in the user's Google Drive.
     * Must be called AFTER [connectGoogleAccount].
     */
    suspend fun createNewSpreadsheet(title: String = "AaaaKetahuan"): Result<String> {
        ensureConfigLoaded()
        return sheetsHelper.createNewSpreadsheet(title)
    }

    /** Returns and clears the pending auth intent from a UserRecoverableAuthIOException */
    fun consumePendingAuthIntent(): android.content.Intent? {
        val intent = sheetsHelper.pendingAuthIntent
        sheetsHelper.pendingAuthIntent = null
        return intent
    }

    suspend fun testSpreadsheetConnection(): Result<Boolean> {
        loadSpreadsheetConfigToHelper()
        if (!sheetsHelper.isAuthenticated()) {
            return Result.failure(Exception("Belum terhubung ke akun Google"))
        }
        return sheetsHelper.testConnection()
    }

    // ─── Theme Config ──────────────────────────────────────────────

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "system") ?: "system"
    }

    @SuppressLint("CommitPrefEdits")
    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    // ─── Daily Reminder Config ────────────────────────────────────

    fun isReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMINDER_ENABLED, false)
    }

    fun getReminderHour(): Int {
        return prefs.getInt(KEY_REMINDER_HOUR, 20) // default 20:00
    }

    fun getReminderMinute(): Int {
        return prefs.getInt(KEY_REMINDER_MINUTE, 0)
    }

    fun getReminderTimeFormatted(): String {
        val hour = getReminderHour()
        val minute = getReminderMinute()
        return String.format("%02d:%02d", hour, minute)
    }

    @SuppressLint("CommitPrefEdits")
    fun enableReminder(hour: Int, minute: Int) {
        prefs.edit()
            .putBoolean(KEY_REMINDER_ENABLED, true)
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
        ReminderScheduler.schedule(context, hour, minute)
    }

    @SuppressLint("CommitPrefEdits")
    fun disableReminder() {
        prefs.edit()
            .putBoolean(KEY_REMINDER_ENABLED, false)
            .apply()
        ReminderScheduler.cancel(context)
    }

    @SuppressLint("CommitPrefEdits")
    fun updateReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
        if (isReminderEnabled()) {
            ReminderScheduler.reschedule(context, hour, minute)
        }
    }

    /** Call this once at app startup to re-schedule active reminders */
    fun refreshReminderAlarm() {
        if (isReminderEnabled()) {
            val hour = getReminderHour()
            val minute = getReminderMinute()
            ReminderScheduler.schedule(context, hour, minute)
        }
    }

    fun getUserDisplayName(): String? {
        return prefs.getString(KEY_USER_DISPLAY_NAME, null)
    }

    @SuppressLint("CommitPrefEdits")
    fun saveUserDisplayName(name: String) {
        prefs.edit().putString(KEY_USER_DISPLAY_NAME, name).apply()
    }

    // ─── Period Management ───────────────────────────────────────

    /**
     * Returns the last recorded period start (bulan, tahun).
     * Defaults to the current month/year if never set.
     */
    fun getLastPeriodStart(): Pair<Int, Int> {
        val bulan = prefs.getInt(KEY_LAST_PERIOD_BULAN, 0)
        val tahun = prefs.getInt(KEY_LAST_PERIOD_TAHUN, 0)
        return if (bulan == 0 || tahun == 0) {
            val now = java.time.LocalDate.now()
            Pair(now.monthValue, now.year)
        } else {
            Pair(bulan, tahun)
        }
    }

    /**
     * Records a new period start (bulan, tahun).
     * Called when user confirms "Mulai periode bulan baru".
     */
    @SuppressLint("CommitPrefEdits")
    fun setLastPeriodStart(bulan: Int, tahun: Int) {
        prefs.edit()
            .putInt(KEY_LAST_PERIOD_BULAN, bulan)
            .putInt(KEY_LAST_PERIOD_TAHUN, tahun)
            .apply()
    }

    /**
     * Creates a new sheet tab in the connected spreadsheet for the new period.
     * Sheet name format: "MMMM yyyy" in Indonesian locale (e.g., "Juli 2026").
     * Also updates the stored sheet name so future syncs write to the new sheet.
     * Persists the new sheet name to SharedPreferences so it survives app restarts.
     */
    suspend fun createNewPeriodSheet(bulan: Int, tahun: Int): Result<Unit> {
        val sheetName = formatPeriodSheetName(bulan, tahun)
        val result = sheetsHelper.createNewSheetTab(sheetName)
        if (result.isSuccess) {
            saveSpreadsheetConfig(getSpreadsheetId(), sheetName)
        }
        return result
    }

    /**
     * Returns the sheet name that corresponds to the given period.
     * Used when switching between periods in the spreadsheet.
     */
    fun formatPeriodSheetName(bulan: Int, tahun: Int): String {
        val monthName = java.time.Month.of(bulan)
            .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("id", "ID"))
        return "$monthName $tahun"
    }

    companion object {
        private const val KEY_SPREADSHEET_ID = "spreadsheet_id"
        private const val KEY_SHEET_NAME = "sheet_name"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_CUSTOM_KATEGORI = "custom_kategori"
        private const val KEY_CUSTOM_METODE = "custom_metode"
        private const val KEY_CUSTOM_SUMBER = "custom_sumber"
        private const val KEY_HIDDEN_KATEGORI = "hidden_kategori"
        private const val KEY_USER_DISPLAY_NAME = "user_display_name"
        private const val KEY_EMAIL_SPREADSHEET_MAP = "email_spreadsheet_map"
        private const val KEY_LAST_PERIOD_BULAN = "last_period_bulan"
        private const val KEY_LAST_PERIOD_TAHUN = "last_period_tahun"

        private val gson = Gson()
    }

    private fun loadStringList(key: String): List<String> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { emptyList() }
    }

    private fun saveStringList(key: String, list: List<String>) {
        prefs.edit().putString(key, gson.toJson(list)).apply()
    }

    fun getCustomKategori(): List<String> = loadStringList(KEY_CUSTOM_KATEGORI)
    fun saveCustomKategori(list: List<String>) = saveStringList(KEY_CUSTOM_KATEGORI, list)

    fun getCustomMetodeBayar(): List<String> = loadStringList(KEY_CUSTOM_METODE)
    fun saveCustomMetodeBayar(list: List<String>) = saveStringList(KEY_CUSTOM_METODE, list)

    fun getCustomSumberPemasukan(): List<String> = loadStringList(KEY_CUSTOM_SUMBER)
    fun saveCustomSumberPemasukan(list: List<String>) = saveStringList(KEY_CUSTOM_SUMBER, list)

    fun getHiddenKategori(): List<String> = loadStringList(KEY_HIDDEN_KATEGORI)
    fun saveHiddenKategori(list: List<String>) = saveStringList(KEY_HIDDEN_KATEGORI, list)
}
