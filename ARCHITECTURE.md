# Arsitektur Aplikasi — AaaaKetahuan

## Pendekatan: Clean Architecture + MVVM

Project ini menggunakan pola **MVVM (Model-View-ViewModel)** yang dikombinasikan dengan prinsip **Clean Architecture** berlapis tiga. Tujuannya agar setiap bagian mudah diuji, tidak saling bergantung secara langsung, dan mudah dikembangkan di masa depan.

```
┌─────────────────────────────────────┐
│           UI LAYER                  │
│  Compose Screens + AutocompleteUI   │
└────────────────┬────────────────────┘
                 │ observes StateFlow
┌────────────────▼────────────────────┐
│        VIEWMODEL LAYER              │
│       TransaksiViewModel            │
└────────────────┬────────────────────┘
                 │ calls suspend fun
┌────────────────▼────────────────────┐
│     DOMAIN / REPOSITORY LAYER       │
│      TransaksiRepository            │
└────────────────┬────────────────────┘
                 │ reads/writes
┌────────────────▼────────────────────┐
│          DATA LAYER                 │
│ ┌──────────────┐   ┌──────────────┐ │
│ │ Local (JSON) │   │ Remote (API) │ │
│ └──────────────┘   └──────────────┘ │
└─────────────────────────────────────┘
```

---

## Layer 1 — UI Layer

**Teknologi:** Jetpack Compose

Berisi semua screen dan komponen visual. UI bersifat **stateless** — tidak menyimpan state sendiri, melainkan mengamati `StateFlow` dari ViewModel dan merender ulang secara reaktif setiap ada perubahan.

### Screen

| Screen | File | Tanggung Jawab |
|---|---|---|
| Dashboard | `DashboardScreen.kt` | Ringkasan saldo, total masuk/keluar, 5 transaksi terkini |
| Input Transaksi | `InputTransaksiScreen.kt` | Form input lengkap dengan autocomplete nama barang |
| Riwayat | `RiwayatScreen.kt` | Daftar semua transaksi dengan filter bulan dan kategori |
| Grafik | `GrafikScreen.kt` | Bar chart pengeluaran per kategori per bulan |
| Export/Import | `ExportImportScreen.kt` | Pilih rentang bulan, generate dan share CSV |

---

## Layer 2 — ViewModel Layer

**Teknologi:** `ViewModel` dari Jetpack Lifecycle + Kotlin `StateFlow`

Satu ViewModel utama: `TransaksiViewModel`. Bertanggung jawab atas:

1. Memegang state UI (daftar transaksi, filter aktif, input form)
2. Mengekspos data ke UI melalui `StateFlow` yang lifecycle-aware
3. Menerima event dari UI dan mendelegasikan ke Repository
4. Menghitung ringkasan (total masuk, total keluar, saldo)

---

## Layer 3 — Repository Layer

**Teknologi:** Kotlin Coroutines (`Dispatchers.IO`)

`TransaksiRepository` kini memiliki tanggung jawab ganda (*dual-write*). Saat `simpanTransaksi()` dipanggil, Repository akan menulis ke file `transaksi.json` lokal (untuk respons UI yang cepat) lalu menembak Google Sheets API di *background* untuk sinkronisasi *real-time*.

### Operasi utama

```kotlin
suspend fun getAllTransaksi(): List<Transaksi>
suspend fun simpanTransaksi(transaksi: Transaksi) // Menyimpan ke Lokal lalu sinkron ke Cloud
suspend fun hapusTransaksi(id: String)
suspend fun editTransaksi(transaksi: Transaksi)
suspend fun getTransaksiByBulan(bulan: Int, tahun: Int): List<Transaksi>
fun getSaran(query: String): List<String>
suspend fun syncPendingTransactions() // Sinkronisasi ulang data dengan isSynced = false
```

---

## Data Layer

Kini memiliki dua sumber data:

### 1. LocalDataSource (Internal Storage)
| File | Format | Isi |
|---|---|---|
| `transaksi.json` | JSON Array | Semua objek `Transaksi` (tambahan field `isSynced: Boolean`) |
| `nama_barang_freq.json` | JSON Object | Map `namaBarang → frekuensi` |

### 2. RemoteDataSource (Google Sheets API)
Berkomunikasi menggunakan `GoogleSheetsHelper.kt` dan kredensial dari `credentials.json` Service Account.

### Model Data

```kotlin
data class Transaksi(
    val id: String = UUID.randomUUID().toString(),
    val tanggal: String,
    val jenis: String,
    val jumlah: Double,
    val namaBarang: String,
    val keterangan: String,
    val kategori: String,
    val bulan: Int,
    val tahun: Int,
    var isSynced: Boolean = false // Penanda status sinkronisasi ke cloud
)
```

---

## Dependency Injection

Menggunakan **Hilt**. `AppModule.kt` menyediakan instance Repository dan GoogleSheetsHelper.

---

## Diagram Alur Data Lengkap

```
[User Input Form]
       |
       | onSubmit(transaksi)
       ▼
[TransaksiViewModel]
       |
       | viewModelScope.launch { repository.simpanTransaksi(transaksi) }
       ▼
[TransaksiRepository]  ──────────────────────────────────────────┐
       |                                                         |
       | 1. Simpan Lokal (Dispatchers.IO)                        | 2. Sync Remote (Background)
       ▼                                                         ▼
[JsonHelper.simpanTransaksi()]                       [GoogleSheetsHelper.appendRow()]
       |                                                         |
       ▼                                                         ▼
[transaksi.json] (isSynced = false/true)               [Google Spreadsheet]
       |
       | ← update UI
       ▼
[UI recompose otomatis]
```
