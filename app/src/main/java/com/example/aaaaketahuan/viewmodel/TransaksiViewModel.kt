package com.example.aaaaketahuan.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aaaaketahuan.data.model.KategoriEnum
import com.example.aaaaketahuan.data.model.MetodeBayarEnum
import com.example.aaaaketahuan.data.model.SumberPemasukanEnum
import com.example.aaaaketahuan.data.model.Transaksi
import com.example.aaaaketahuan.data.remote.DriveSharingHelper
import com.example.aaaaketahuan.data.repository.TransaksiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TransaksiViewModel @Inject constructor(
    private val repository: TransaksiRepository
) : ViewModel() {

    private val _transaksiList = MutableStateFlow<List<Transaksi>>(emptyList())
    val transaksiList: StateFlow<List<Transaksi>> = _transaksiList.asStateFlow()

    private val _filterBulan = MutableStateFlow(LocalDate.now().monthValue)
    val filterBulan: StateFlow<Int> = _filterBulan.asStateFlow()

    private val _filterTahun = MutableStateFlow(LocalDate.now().year)
    val filterTahun: StateFlow<Int> = _filterTahun.asStateFlow()

    private val _namaBarangInput = MutableStateFlow("")
    private val _saranNamaBarang = MutableStateFlow<List<String>>(emptyList())
    val saranNamaBarang: StateFlow<List<String>> = _saranNamaBarang.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _pendingAuthIntent = MutableStateFlow<android.content.Intent?>(null)
    val pendingAuthIntent: StateFlow<android.content.Intent?> = _pendingAuthIntent.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userDisplayName = MutableStateFlow<String?>(null)
    val userDisplayName: StateFlow<String?> = _userDisplayName.asStateFlow()

    fun clearPendingAuthIntent() {
        _pendingAuthIntent.value = null
    }

    val totalMasuk: StateFlow<Double> = _transaksiList
        .map { list -> list.filter { it.jenis == "masuk" }.sumOf { it.jumlah } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalKeluar: StateFlow<Double> = _transaksiList
        .map { list -> list.filter { it.jenis == "keluar" }.sumOf { it.jumlah } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val saldo: StateFlow<Double> = _transaksiList
        .map { list ->
            list.filter { it.jenis == "masuk" }.sumOf { it.jumlah } -
                list.filter { it.jenis == "keluar" }.sumOf { it.jumlah }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        _themeMode.value = repository.getThemeMode()
        _userEmail.value = repository.getConnectedAccount()
        _userDisplayName.value = repository.getUserDisplayName()
        loadTransaksi()
        syncPending()
        setupAutocompleteSearch()
    }

    /**
     * Sets up autocomplete search with debounce once in init,
     * avoiding flow leaks from repeated launch calls.
     */
    @OptIn(FlowPreview::class)
    private fun setupAutocompleteSearch() {
        viewModelScope.launch {
            _namaBarangInput
                .debounce(150)
                .flowOn(Dispatchers.IO)
                .map { query -> repository.getSaran(query) }
                .collect { saran -> _saranNamaBarang.value = saran }
        }
    }

    fun loadTransaksi() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _transaksiList.value = repository.getTransaksiByBulan(
                    _filterBulan.value, _filterTahun.value
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _errorMessage.value = "Gagal memuat data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onNamaBarangChange(input: String) {
        _namaBarangInput.value = input
    }

    fun clearSaran() {
        _saranNamaBarang.value = emptyList()
    }

    fun onSubmitTransaksi(
        jenis: String,
        jumlah: Double,
        namaBarang: String,
        keterangan: String,
        kategori: String,
        tanggal: String,
        metodeBayar: String = "",
        sumber: String = ""
    ) {
        viewModelScope.launch {
            try {
                val date = LocalDate.parse(tanggal)
                val transaksi = Transaksi(
                    tanggal = tanggal,
                    jenis = jenis,
                    jumlah = jumlah,
                    namaBarang = namaBarang,
                    keterangan = keterangan,
                    kategori = kategori,
                    bulan = date.monthValue,
                    tahun = date.year,
                    metodeBayar = metodeBayar,
                    sumber = sumber
                )
                repository.simpanTransaksi(transaksi)
                loadTransaksi()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _errorMessage.value = "Gagal menyimpan transaksi: ${e.message}"
            }
        }
    }

    fun onEditTransaksi(transaksi: Transaksi) {
        viewModelScope.launch {
            try {
                repository.editTransaksi(transaksi)
                loadTransaksi()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _errorMessage.value = "Gagal mengedit transaksi: ${e.message}"
            }
        }
    }

    fun onHapusTransaksi(id: String) {
        viewModelScope.launch {
            try {
                repository.hapusTransaksi(id)
                loadTransaksi()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _errorMessage.value = "Gagal menghapus transaksi: ${e.message}"
            }
        }
    }

    fun onFilterBulanChange(bulan: Int, tahun: Int) {
        _filterBulan.value = bulan
        _filterTahun.value = tahun
        loadTransaksi()
    }

    fun exportCsv(bulan: Int? = null, tahun: Int? = null, onSuccess: (File) -> Unit) {
        viewModelScope.launch {
            try {
                val file = repository.exportCsv(bulan, tahun)
                onSuccess(file)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _errorMessage.value = "Gagal export CSV: ${e.message}"
            }
        }
    }

    fun importCsv(uri: Uri, onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val count = repository.importCsv(uri)
                loadTransaksi()
                onSuccess(count)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _errorMessage.value = "Gagal import CSV: ${e.message}"
            }
        }
    }

    fun setThemeMode(mode: String) {
        repository.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ─── Daily Reminder ───────────────────────────────────────────────

    fun isReminderEnabled(): Boolean = repository.isReminderEnabled()

    fun getReminderHour(): Int = repository.getReminderHour()

    fun getReminderMinute(): Int = repository.getReminderMinute()

    fun getReminderTimeFormatted(): String = repository.getReminderTimeFormatted()

    fun enableReminder(hour: Int, minute: Int) {
        repository.enableReminder(hour, minute)
    }

    fun disableReminder() {
        repository.disableReminder()
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        repository.updateReminderTime(hour, minute)
    }

    /**
     * Finds a transaction across ALL data (not just current month filter)
     * for editing purposes.
     */
    fun getTransaksiById(id: String, onResult: (Transaksi?) -> Unit) {
        viewModelScope.launch {
            val all = repository.getAllTransaksi()
            onResult(all.find { it.id == id })
        }
    }

    fun getTransaksiByKategoriForMonth(bulan: Int, tahun: Int): Map<String, Double> {
        return _transaksiList.value
            .filter { it.bulan == bulan && it.tahun == tahun && it.jenis == "keluar" }
            .groupBy { it.kategori }
            .mapValues { (_, list) -> list.sumOf { it.jumlah } }
    }

    // ─── Spreadsheet Config (OAuth 2.0) ────────────────────────────

    fun getSpreadsheetConfig(): Pair<String, String> {
        return Pair(repository.getSpreadsheetId(), repository.getSheetName())
    }

    fun getConnectedAccount(): String? {
        return repository.getConnectedAccount()
    }

    fun isSpreadsheetConnected(): Boolean {
        return repository.isSpreadsheetConnected()
    }

    /**
     * Called after user successfully logs in via GoogleSignIn.
     * Sets the OAuth credential and saves the account email.
     */
    fun connectGoogleAccount(accountEmail: String) {
        repository.connectGoogleAccount(accountEmail)
        _userEmail.value = accountEmail
    }

    /**
     * Checks if the given email already has a spreadsheet saved from a previous login.
     * If found, restores the config so the app reuses the same spreadsheet
     * instead of creating a new one.
     * @return true if an existing spreadsheet was restored, false otherwise.
     */
    fun restoreExistingSpreadsheet(email: String): Boolean {
        return repository.restoreSpreadsheetForEmail(email)
    }

    /**
     * Fallback: cari spreadsheet milik user sendiri via Drive API
     * ketika [restoreExistingSpreadsheet] gagal (data lokal hilang).
     *
     * @param onFound dipanggil dengan spreadsheet ID jika ditemukan.
     * @param onNotFound dipanggil jika tidak ada spreadsheet.
     */
    fun restoreExistingSpreadsheetFromDrive(
        onFound: (String) -> Unit,
        onNotFound: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val result = repository.findOwnSpreadsheetFromDrive()
                result.fold(
                    onSuccess = { spreadsheetId ->
                        if (spreadsheetId != null) {
                            repository.saveSpreadsheetConfig(spreadsheetId)
                            onFound(spreadsheetId)
                        } else {
                            onNotFound()
                        }
                    },
                    onFailure = { onNotFound() }
                )
            } catch (_: Exception) {
                onNotFound()
            }
        }
    }

    /**
     * Creates a new spreadsheet in the user's Google Drive.
     * Must be called AFTER [connectGoogleAccount].
     */
    fun createNewSpreadsheet(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.createNewSpreadsheet()
                result.fold(
                    onSuccess = { spreadsheetId ->
                        repository.saveSpreadsheetConfig(spreadsheetId)
                        onSuccess(spreadsheetId)
                    },
                    onFailure = { error ->
                        if (error.message == "PERLU_IZIN") {
                            val intent = repository.consumePendingAuthIntent()
                            if (intent != null) {
                                _pendingAuthIntent.value = intent
                            } else {
                                onError("Perlu izin akses Google Sheets")
                            }
                        } else {
                            onError(error.message ?: "Gagal membuat spreadsheet")
                        }
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onError(e.message ?: "Gagal membuat spreadsheet")
            }
        }
    }

    fun disconnectSpreadsheet() {
        repository.disconnectSpreadsheet()
    }

    /**
     * Logout: clears all credentials and spreadsheet config,
     * then sets [userEmail] to null so MainActivity renders AuthScreen.
     */
    fun logout() {
        repository.logout()
        _userEmail.value = null
        _userDisplayName.value = null
    }

    /**
     * Saves onboarding data: user display name, hidden categories, and custom categories.
     * Called when user completes the onboarding screen.
     */
    fun saveOnboardingData(
        name: String,
        hiddenCategories: List<String>,
        customCategories: List<String>
    ) {
        repository.saveUserDisplayName(name)
        repository.saveHiddenKategori(hiddenCategories)
        repository.saveCustomKategori(customCategories)
        _userDisplayName.value = name
    }

    fun testSpreadsheetConnection(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.testSpreadsheetConnection()
                if (result.isSuccess) {
                    onResult(true, null)
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "Gagal terhubung")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onResult(false, e.message ?: "Gagal terhubung")
            }
        }
    }

    /**
     * Retry syncing all unsynced transactions to Google Sheets.
     * Skips network call early if no pending transactions exist.
     */
    fun syncPending() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // Early exit: check if anything pending without full file read
                val pending = repository.getPendingSyncCount()
                if (pending == 0) return@launch

                val count = repository.syncPendingTransactions()
                if (count > 0) {
                    loadTransaksi()
                }
                updatePendingCount()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _errorMessage.value = "Gagal sinkronisasi: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun updatePendingCount() {
        val all = repository.getAllTransaksi()
        _pendingSyncCount.value = all.count { !it.isSynced }
    }

    // ─── Category Management ─────────────────────────────────

    /** Returns merged list: enum entries + custom entries, minus hidden ones */
    fun getEffectiveKategori(): List<String> {
        val hidden = repository.getHiddenKategori().toSet()
        val fromEnum = KategoriEnum.entries.map { it.label }.filter { it !in hidden }
        val custom = repository.getCustomKategori()
        return fromEnum + custom
    }

    fun getCustomKategori(): List<String> = repository.getCustomKategori()
    fun saveCustomKategori(list: List<String>) = repository.saveCustomKategori(list)
    fun getHiddenKategori(): List<String> = repository.getHiddenKategori()
    fun saveHiddenKategori(list: List<String>) = repository.saveHiddenKategori(list)

    fun getEffectiveMetodeBayar(): List<String> {
        val fromEnum = MetodeBayarEnum.entries.map { it.label }
        return fromEnum + repository.getCustomMetodeBayar()
    }

    fun getCustomMetodeBayar(): List<String> = repository.getCustomMetodeBayar()
    fun saveCustomMetodeBayar(list: List<String>) = repository.saveCustomMetodeBayar(list)

    fun getEffectiveSumberPemasukan(): List<String> {
        val fromEnum = SumberPemasukanEnum.entries.map { it.label }
        return fromEnum + repository.getCustomSumberPemasukan()
    }

    fun getCustomSumberPemasukan(): List<String> = repository.getCustomSumberPemasukan()
    fun saveCustomSumberPemasukan(list: List<String>) = repository.saveCustomSumberPemasukan(list)

    // ─── Period Management ─────────────────────────────────────────────

    /** Returns the last recorded period month/year. Defaults to current month. */
    fun getLastPeriodStart(): Pair<Int, Int> = repository.getLastPeriodStart()

    /**
     * Starts a new period for the given month/year:
     * 1. Records the period in SharedPreferences
     * 2. Resets the filter to the new period
     * 3. Creates a new sheet tab in the spreadsheet
     *
     * Called from PemasukanScreen when user confirms "Mulai periode bulan baru?"
     */
    fun startNewPeriod(bulan: Int, tahun: Int, onSheetCreated: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            // 1. Record period
            repository.setLastPeriodStart(bulan, tahun)

            // 2. Reset filter to new period
            _filterBulan.value = bulan
            _filterTahun.value = tahun
            loadTransaksi()

            // 3. Create new sheet tab in spreadsheet (if connected)
            val spreadsheetId = repository.getSpreadsheetId()
            if (spreadsheetId.isNotBlank()) {
                val result = repository.createNewPeriodSheet(bulan, tahun)
                onSheetCreated(result.isSuccess)
            } else {
                onSheetCreated(false)
            }
        }
    }

    // ─── Restore from Google Sheets ─────────────────────────────────────

    private val _showRestoreDialog = MutableStateFlow(false)
    val showRestoreDialog: StateFlow<Boolean> = _showRestoreDialog.asStateFlow()

    private val _restoreCount = MutableStateFlow(0)
    val restoreCount: StateFlow<Int> = _restoreCount.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage.asStateFlow()

    /**
     * Called after spreadsheet is connected (either restored from email or newly created).
     * If this is a fresh install (no local data) and a spreadsheet is connected,
     * reads available data from the spreadsheet and offers to restore it.
     */
    fun checkAndOfferRestore() {
        viewModelScope.launch {
            if (repository.hasLocalData()) return@launch
            val spreadsheetId = repository.getSpreadsheetId()
            if (spreadsheetId.isBlank()) return@launch

            // Count how many transactions are in the spreadsheet
            val count = countSheetTransactions()
            if (count > 0) {
                _restoreCount.value = count
                _showRestoreDialog.value = true
            }
        }
    }

    /**
     * Counts total data rows across all sheet tabs (without loading everything into memory).
     */
    private suspend fun countSheetTransactions(): Int {
        val tabsResult = repository.getAllSheetTabs()
        if (tabsResult.isFailure) return 0
        val tabs = tabsResult.getOrThrow()
        var total = 0
        for (tab in tabs) {
            val rowsResult = repository.readAllRowsFromSheet(tab)
            if (rowsResult.isSuccess) {
                total += rowsResult.getOrThrow()
            }
        }
        return total
    }

    /**
     * User confirms restore — pulls all data from Google Sheets into local storage.
     */
    fun confirmRestore(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isRestoring.value = true
            _restoreMessage.value = null
            try {
                val result = repository.restoreAllFromSheet()
                result.fold(
                    onSuccess = { count ->
                        _showRestoreDialog.value = false
                        loadTransaksi()
                        _restoreMessage.value = "Berhasil memulihkan $count transaksi dari spreadsheet!"
                    },
                    onFailure = { error ->
                        _restoreMessage.value = "Gagal: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _restoreMessage.value = "Gagal: ${e.message}"
            } finally {
                _isRestoring.value = false
                onComplete()
            }
        }
    }

    /** User dismisses the restore dialog */
    fun dismissRestore() {
        _showRestoreDialog.value = false
        _restoreCount.value = 0
        _restoreMessage.value = null
    }

    /** Clear just the message (after snackbar has been shown) */
    fun clearRestoreMessage() {
        _restoreMessage.value = null
    }

    // ═══════════════════════════════════════════════════════════════
    // KOLABORASI — UNDANG USER
    // ═══════════════════════════════════════════════════════════════

    private val _collaborators = MutableStateFlow<List<DriveSharingHelper.CollaboratorInfo>>(emptyList())
    val collaborators: StateFlow<List<DriveSharingHelper.CollaboratorInfo>> = _collaborators.asStateFlow()

    private val _isInviting = MutableStateFlow(false)
    val isInviting: StateFlow<Boolean> = _isInviting.asStateFlow()

    private val _invitationMessage = MutableStateFlow<String?>(null)
    val invitationMessage: StateFlow<String?> = _invitationMessage.asStateFlow()

    // ——— Invitation discovery (User B) ———

    private val _showInviteDiscovery = MutableStateFlow(false)
    val showInviteDiscovery: StateFlow<Boolean> = _showInviteDiscovery.asStateFlow()

    private val _foundSharedSpreadsheets = MutableStateFlow<List<DriveSharingHelper.SharedSpreadsheetInfo>>(emptyList())
    val foundSharedSpreadsheets: StateFlow<List<DriveSharingHelper.SharedSpreadsheetInfo>> =
        _foundSharedSpreadsheets.asStateFlow()

    private val _isCheckingInvitations = MutableStateFlow(false)
    val isCheckingInvitations: StateFlow<Boolean> = _isCheckingInvitations.asStateFlow()

    /**
     * User A: Kirim undangan kolaborasi via email.
     */
    fun inviteUser(email: String) {
        viewModelScope.launch {
            _isInviting.value = true
            _invitationMessage.value = null
            try {
                val result = repository.inviteUser(email)
                result.fold(
                    onSuccess = {
                        _invitationMessage.value = "Undangan terkirim ke $email"
                    },
                    onFailure = { error ->
                        _invitationMessage.value = "Gagal: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _invitationMessage.value = "Gagal: ${e.message}"
            } finally {
                _isInviting.value = false
            }
        }
    }

    /**
     * User A: Muat daftar kolaborator saat ini.
     */
    fun loadCollaborators() {
        viewModelScope.launch {
            try {
                val result = repository.getCollaborators()
                result.fold(
                    onSuccess = { _collaborators.value = it },
                    onFailure = { /* silent */ }
                )
            } catch (_: Exception) { }
        }
    }

    /**
     * User B: Cek undangan yang masuk.
     */
    fun checkForInvitations() {
        viewModelScope.launch {
            _isCheckingInvitations.value = true
            _invitationMessage.value = null
            try {
                val result = repository.checkForInvitations()
                result.fold(
                    onSuccess = { list ->
                        _foundSharedSpreadsheets.value = list
                        _showInviteDiscovery.value = list.isNotEmpty()
                    },
                    onFailure = { error ->
                        _invitationMessage.value = "Gagal: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _invitationMessage.value = "Gagal: ${e.message}"
            } finally {
                _isCheckingInvitations.value = false
            }
        }
    }

    /**
     * User B: Terima undangan — replace data lokal dengan spreadsheet bersama.
     */
    fun acceptInvitation(spreadsheetId: String) {
        viewModelScope.launch {
            _isInviting.value = true
            _invitationMessage.value = null
            try {
                val result = repository.acceptInvitation(spreadsheetId)
                result.fold(
                    onSuccess = { count ->
                        _invitationMessage.value = "Berhasil! $count transaksi dipulihkan."
                        _showInviteDiscovery.value = false
                        _userEmail.value = repository.getConnectedAccount()
                        loadTransaksi()
                    },
                    onFailure = { error ->
                        _invitationMessage.value = "Gagal: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _invitationMessage.value = "Gagal: ${e.message}"
            } finally {
                _isInviting.value = false
            }
        }
    }

    /** User B: Tolak undangan. */
    fun rejectInvitation() {
        repository.rejectInvitation()
        _showInviteDiscovery.value = false
        _foundSharedSpreadsheets.value = emptyList()
        _invitationMessage.value = "Undangan ditolak."
    }

    /** Dismiss invitation message (after snackbar). */
    fun clearInvitationMessage() {
        _invitationMessage.value = null
    }

    /**
     * User A: Hapus akses kolaborator.
     * @param permissionId ID permission dari [DriveSharingHelper.CollaboratorInfo.permissionId].
     */
    fun removeCollaborator(permissionId: String) {
        viewModelScope.launch {
            _isInviting.value = true
            _invitationMessage.value = null
            try {
                val result = repository.removeCollaborator(permissionId)
                result.fold(
                    onSuccess = {
                        _invitationMessage.value = "Kolaborator berhasil dihapus"
                        // Refresh daftar kolaborator
                        loadCollaborators()
                    },
                    onFailure = { error ->
                        _invitationMessage.value = "Gagal: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _invitationMessage.value = "Gagal: ${e.message}"
            } finally {
                _isInviting.value = false
            }
        }
    }
}
