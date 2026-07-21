# Arsitektur Aplikasi — AaaaKetahuan

## Pendekatan: Clean Architecture + MVVM

Project ini menggunakan pola **MVVM (Model-View-ViewModel)** yang dikombinasikan dengan prinsip **Clean Architecture** berlapis tiga. Tujuannya agar setiap bagian mudah diuji, tidak saling bergantung secara langsung, dan mudah dikembangkan di masa depan.

```
┌─────────────────────────────────────┐
│           UI LAYER                  │
│  Compose Screens + Components       │
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

**Teknologi:** Jetpack Compose + Material3

Berisi semua screen dan komponen visual. UI bersifat **stateless** — tidak menyimpan state sendiri, melainkan mengamati `StateFlow` dari ViewModel dan merender ulang secara reaktif setiap ada perubahan.

### Screen

| Screen | File | Tanggung Jawab |
|---|---|---|
| Dashboard | `DashboardScreen.kt` | Ringkasan saldo, total masuk/keluar, 5 transaksi terkini, navigasi cepat |
| Input Transaksi | `InputTransaksiScreen.kt` | Form input pengeluaran dengan autocomplete nama barang, kategori, metode bayar |
| Pemasukan | `PemasukanScreen.kt` | Form input pemasukan dengan sumber pemasukan (gaji, freelance, dll) |
| Riwayat | `RiwayatScreen.kt` | Daftar semua transaksi dengan filter bulan, kategori, dan edit |
| Grafik | `GrafikScreen.kt` | Bar chart pengeluaran per kategori per bulan |
| Export/Import | `ExportImportScreen.kt` | Pilih rentang bulan, generate dan share CSV |
| Pengaturan | `PengaturanScreen.kt` | Tema, spreadsheet config, duplikat, backup/restore, notifikasi |

### Navigation

Navigasi menggunakan `NavigationBar` (bottom navigation) dengan 5 item:
- Dashboard, Input, Pemasukan, Riwayat, Grafik

Screen lain (Pengaturan, Export/Import) diakses dari Dashboard atau halaman lain melalui tombol navigasi sekunder.

---

## Layer 2 — ViewModel Layer

**Teknologi:** `ViewModel` dari Jetpack Lifecycle + Kotlin `StateFlow`

Satu ViewModel utama: `TransaksiViewModel`. Bertanggung jawab atas:

1. Memegang state UI (daftar transaksi, filter aktif, input form)
2. Mengekspos data ke UI melalui `StateFlow` yang lifecycle-aware
3. Menerima event dari UI dan mendelegasikan ke Repository
4. Menghitung ringkasan (total masuk, total keluar, saldo)
5. Konfigurasi spreadsheet, tema, dan reminder

**Aturan penting:** ViewModel **tidak boleh** mengakses `android.content` secara langsung. Semua operasi yang memerlukan `Context` atau `SharedPreferences` didelegasikan ke Repository.

---

## Layer 3 — Repository Layer

**Teknologi:** Kotlin Coroutines (`Dispatchers.IO`)

`TransaksiRepository` kini memiliki tanggung jawab ganda (*dual-write*). Saat `simpanTransaksi()` dipanggil, Repository akan menulis ke file `transaksi.json` lokal (untuk respons UI yang cepat) lalu menembak Google Sheets API di *background* untuk sinkronisasi *real-time*.

### Operasi utama

```kotlin
suspend fun getAllTransaksi(): List<Transaksi>
suspend fun simpanTransaksi(transaksi: Transaksi)
suspend fun hapusTransaksi(id: String)
suspend fun editTransaksi(transaksi: Transaksi)
suspend fun getTransaksiByBulan(bulan: Int, tahun: Int): List<Transaksi>
fun getSaran(query: String): List<String>
suspend fun syncPendingTransactions()

// Spreadsheet config (mutable, via SharedPreferences)
fun getSpreadsheetConfig(): Pair<String, String>
fun connectSpreadsheet(id: String, sheetName: String)
fun disconnectSpreadsheet()
fun isSpreadsheetConnected(): Boolean
suspend fun testConnection(): Boolean

// Theme config
fun getThemeMode(): String
fun setThemeMode(mode: String)

// Reminder config
fun isReminderEnabled(): Boolean
fun enableReminder(hour: Int, minute: Int)
fun disableReminder()
fun updateReminderTime(hour: Int, minute: Int)
```

---

## Data Layer

Memiliki dua sumber data:

### 1. LocalDataSource (Internal Storage)
| File | Format | Isi |
|---|---|---|
| `transaksi.json` | JSON Array | Semua objek `Transaksi` (tambahan field `isSynced: Boolean`) |
| `nama_barang_freq.json` | JSON Object | Map `namaBarang → frekuensi` |

### 2. RemoteDataSource (Google Sheets API — OAuth 2.0)
Berkomunikasi menggunakan `GoogleSheetsHelper.kt` dengan autentikasi **OAuth 2.0** via `GoogleSignIn`. Setiap pengguna login dengan akun Google mereka sendiri — spreadsheet dibuat otomatis di Drive pengguna. `GoogleAccountCredential` mengelola token OAuth secara transparan (auto-refresh via `AccountManager`).

**Tidak ada rahasia/secret di APK** — hanya OAuth Client ID yang diverifikasi via SHA-1 signing certificate.

### Model Data

```kotlin
data class Transaksi(
    val id: String = UUID.randomUUID().toString(),
    val tanggal: String,           // "yyyy-MM-dd"
    val jenis: String,             // "masuk" | "keluar"
    val jumlah: Double,
    val namaBarang: String,
    val keterangan: String,
    val kategori: String,
    val bulan: Int,
    val tahun: Int,
    val metodeBayar: String = "",  // "Cash", "Kredit", "E-Wallet", "Transfer", "QRIS"
    val sumber: String = "",       // "Gaji", "Lainnya" (for income only)
    var isSynced: Boolean = false  // Status sinkronisasi ke cloud
)
```

---

## Dependency Injection

Menggunakan **Hilt**. `AppModule.kt` menyediakan instance Repository dan GoogleSheetsHelper.

```kotlin
@HiltAndroidApp
class AaaaKetahuanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this) // Init notification channel
    }
}
```

---

## Notifikasi Reminder

Menggunakan `AlarmManager.setRepeating()` + `BroadcastReceiver` (bukan WorkManager):

| Komponen | File | Peran |
|---|---|---|
| NotificationHelper | `NotificationHelper.kt` | Buat channel + tampilkan notifikasi |
| ReminderScheduler | `ReminderScheduler.kt` | Schedule/cancel alarm via AlarmManager |
| ReminderReceiver | `ReminderReceiver.kt` | BroadcastReceiver: terima alarm → tampilkan notif |
| AaaaKetahuanApp | `AaaaKetahuanApp.kt` | Init notification channel di onCreate() |
| TransaksiViewModel | `TransaksiViewModel.kt` | Fungsi enable/disable/update reminder |

Alasan memilih `AlarmManager` ketimbang `WorkManager`: use case sederhana (trigger harian di jam yang sama), tidak perlu flex interval atau constraint.

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
