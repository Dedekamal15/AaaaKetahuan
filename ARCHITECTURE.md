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
│  transaksi.json + nama_barang_freq  │
│         (Internal Storage)          │
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

### Komponen Reusable

**`AutocompleteTextField`** — komponen kunci untuk fitur saran nama barang:

```
User mengetik
    ↓
onValueChange → ViewModel.onNamaBarangChange(query)
    ↓ debounce 150ms
    ↓
repository.getSaran(query) — filter freq >= 2
    ↓
StateFlow<List<String>> → UI render dropdown
    ↓
User pilih item → field terisi, dropdown tutup
```

Aturan tampil dropdown:
- Hanya muncul jika `query.isNotBlank()` DAN ada hasil saran
- Saran diurutkan dari yang paling sering digunakan
- Maksimal 5 item saran ditampilkan
- Matching bersifat case-insensitive dan substring (bukan prefix)

### Navigasi

Menggunakan **Jetpack Navigation Compose** dengan Bottom Navigation Bar untuk empat destinasi utama (Dashboard, Input, Riwayat, Grafik) dan satu screen Export yang dapat diakses dari Dashboard atau menu overflow.

---

## Layer 2 — ViewModel Layer

**Teknologi:** `ViewModel` dari Jetpack Lifecycle + Kotlin `StateFlow`

Satu ViewModel utama: `TransaksiViewModel`. Bertanggung jawab atas:

1. Memegang state UI (daftar transaksi, filter aktif, input form)
2. Mengekspos data ke UI melalui `StateFlow` yang lifecycle-aware
3. Menerima event dari UI dan mendelegasikan ke Repository
4. Menghitung ringkasan (total masuk, total keluar, saldo)

```kotlin
// Contoh state yang diekspos ke UI
val transaksiList: StateFlow<List<Transaksi>>
val totalMasuk: StateFlow<Double>
val totalKeluar: StateFlow<Double>
val saranNamaBarang: StateFlow<List<String>>
val filterBulan: StateFlow<Int>
val filterTahun: StateFlow<Int>
```

**Alur autocomplete di ViewModel:**

```kotlin
val saranNamaBarang: StateFlow<List<String>> = _namaBarangInput
    .debounce(150)
    .map { query -> repository.getSaran(query) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

`debounce(150)` memastikan `getSaran()` tidak dipanggil di setiap keystroke — hanya setelah user berhenti mengetik selama 150ms.

---

## Layer 3 — Repository Layer

**Teknologi:** Kotlin Coroutines (`Dispatchers.IO`)

`TransaksiRepository` adalah satu-satunya titik akses ke penyimpanan data. Semua operasi file dijalankan di `Dispatchers.IO` agar tidak memblokir main thread.

### Operasi utama

```kotlin
suspend fun getAllTransaksi(): List<Transaksi>
suspend fun simpanTransaksi(transaksi: Transaksi)
suspend fun hapusTransaksi(id: String)
suspend fun editTransaksi(transaksi: Transaksi)
suspend fun getTransaksiByBulan(bulan: Int, tahun: Int): List<Transaksi>
fun getSaran(query: String): List<String>
suspend fun exportCsv(bulan: Int?, tahun: Int?): File
suspend fun importCsv(uri: Uri): Int  // returns jumlah baris berhasil diimpor
```

### Logika frekuensi autocomplete

Saat `simpanTransaksi()` dipanggil:

1. Simpan transaksi ke `transaksi.json`
2. Baca `nama_barang_freq.json`
3. Increment counter untuk `transaksi.namaBarang`
4. Tulis ulang `nama_barang_freq.json`

Saat `hapusTransaksi()` dipanggil, frekuensi **tidak** dikurangi — riwayat pencarian tetap tersedia meski transaksinya dihapus. Ini keputusan UX yang disengaja.

---

## Data Layer

### File Storage

Semua file disimpan di `context.filesDir` (internal storage, privat per-app, tidak butuh permission).

| File | Format | Isi |
|---|---|---|
| `transaksi.json` | JSON Array | Semua objek `Transaksi` |
| `nama_barang_freq.json` | JSON Object | Map `namaBarang → frekuensi` |

### Model Data

```kotlin
data class Transaksi(
    val id: String = UUID.randomUUID().toString(),
    val tanggal: String,          // format: "yyyy-MM-dd"
    val jenis: String,            // "masuk" atau "keluar"
    val jumlah: Double,
    val namaBarang: String,       // free text, sumber data autocomplete
    val keterangan: String,       // catatan tambahan opsional
    val kategori: String,         // dari KategoriEnum
    val bulan: Int,               // 1–12
    val tahun: Int
)

enum class KategoriEnum(val label: String) {
    MAKANAN("Makanan"),
    TRANSPORTASI("Transportasi"),
    KESEHATAN("Kesehatan"),
    PENDIDIKAN("Pendidikan"),
    TAGIHAN("Tagihan"),
    HIBURAN("Hiburan"),
    TABUNGAN("Tabungan"),
    LAINNYA("Lainnya")
}
```

### Serialisasi

Menggunakan **Gson** untuk konversi `List<Transaksi>` ↔ JSON string. Operasi baca/tulis dibantu `JsonHelper.kt`:

```kotlin
object JsonHelper {
    fun bacaTransaksi(file: File): List<Transaksi>
    fun simpanTransaksi(file: File, list: List<Transaksi>)
    fun bacaFrekuensi(file: File): MutableMap<String, Int>
    fun simpanFrekuensi(file: File, map: Map<String, Int>)
}
```

### Format CSV Export

```
id,tanggal,jenis,jumlah,namaBarang,keterangan,kategori,bulan,tahun
550e8400,...,2026-06-18,keluar,45000.0,Makan siang,Warung bu Tini,Makanan,6,2026
```

---

## Dependency Injection

Menggunakan **Hilt**. `AppModule.kt` menyediakan:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideTransaksiRepository(@ApplicationContext context: Context): TransaksiRepository {
        return TransaksiRepository(context)
    }
}
```

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
[TransaksiRepository]  ──────────────────────────────────┐
       |                                                   |
       | withContext(Dispatchers.IO)                       |
       ▼                                                   ▼
[JsonHelper.simpanTransaksi()]            [JsonHelper.simpanFrekuensi()]
       |                                                   |
       ▼                                                   ▼
[transaksi.json]                         [nama_barang_freq.json]
       |
       | ← getAllTransaksi() setelah simpan
       ▼
[ViewModel update StateFlow]
       |
       ▼
[UI recompose otomatis]
```

---

## Pertimbangan Masa Depan

Jika suatu saat data mulai besar (ribuan transaksi) dan performa JSON mulai terasa lambat, migration path yang direkomendasikan adalah **Room Database** (SQLite). Repository pattern yang sudah ada memudahkan migrasi — hanya perlu mengganti implementasi Repository tanpa mengubah ViewModel atau UI sama sekali.
