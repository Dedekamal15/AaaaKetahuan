package com.example.aaaaketahuan.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aaaaketahuan.data.model.Transaksi
import com.example.aaaaketahuan.data.repository.TransaksiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
        loadTransaksi()
        syncPending()
    }

    fun loadTransaksi() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allTransaksi = repository.getAllTransaksi()
                _transaksiList.value = allTransaksi.filter {
                    it.bulan == _filterBulan.value && it.tahun == _filterTahun.value
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onNamaBarangChange(input: String) {
        _namaBarangInput.value = input
        viewModelScope.launch {
            _namaBarangInput
                .debounce(150)
                .map { query -> repository.getSaran(query) }
                .collect { saran -> _saranNamaBarang.value = saran }
        }
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
                _errorMessage.value = "Gagal import CSV: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun getTransaksiByKategoriForMonth(bulan: Int, tahun: Int): Map<String, Double> {
        return _transaksiList.value
            .filter { it.bulan == bulan && it.tahun == tahun && it.jenis == "keluar" }
            .groupBy { it.kategori }
            .mapValues { (_, list) -> list.sumOf { it.jumlah } }
    }

    /**
     * Retry syncing all unsynced transactions to Google Sheets.
     */
    fun syncPending() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val count = repository.syncPendingTransactions()
                if (count > 0) {
                    loadTransaksi()
                }
                updatePendingCount()
            } catch (e: Exception) {
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
}
