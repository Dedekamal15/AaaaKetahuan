# Rules & Konvensi Kode — AaaaKetahuan

Dokumen ini adalah kontrak internal project. Semua kontributor (termasuk diri sendiri di masa depan) wajib mengikuti aturan ini untuk menjaga konsistensi dan kualitas kode.

---

## 1. Bahasa & Penamaan

### Bahasa kode
- Semua nama variabel, fungsi, class, dan komentar kode ditulis dalam **Bahasa Inggris**
- UI-facing string (label tombol, pesan error, placeholder) ditulis dalam **Bahasa Indonesia** dan didefinisikan di `strings.xml`
- Jangan hardcode string UI langsung di Composable — selalu gunakan `stringResource()`

### Konvensi penamaan Kotlin

| Tipe | Konvensi | Contoh |
|---|---|---|
| Class / Object | PascalCase | `TransaksiRepository` |
| Fungsi | camelCase | `simpanTransaksi()` |
| Variabel | camelCase | `totalPengeluaran` |
| Konstanta | SCREAMING_SNAKE_CASE | `MAX_SARAN_COUNT` |
| File Composable | PascalCase + suffix `Screen` atau nama komponen | `DashboardScreen.kt`, `AutocompleteTextField.kt` |
| StateFlow privat | prefix `_` | `_transaksiList` |
| StateFlow publik | tanpa prefix | `transaksiList` |

### Penamaan file JSON
- `transaksi.json` — jangan diganti nama, Repository hardcode path ini
- `nama_barang_freq.json` — sama, jangan diganti nama

---

## 2. Arsitektur

### Aturan layer
- **UI tidak boleh** mengakses Repository secara langsung — harus melalui ViewModel
- **ViewModel tidak boleh** mengimport class dari paket `android.content` atau melakukan operasi file — itu tugas Repository
- **Repository tidak boleh** mengimport class Compose atau ViewModel — murni logika data

### StateFlow
- Semua state yang dikonsumsi UI harus diekspos sebagai `StateFlow`, bukan `LiveData` atau raw `MutableState`
- `MutableStateFlow` selalu bersifat `private` di dalam ViewModel
- Gunakan `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultValue)` untuk flow yang di-derive

### Coroutines
- Semua operasi file dijalankan dengan `withContext(Dispatchers.IO)` di dalam Repository
- ViewModel memanggil suspend function Repository dari `viewModelScope.launch`
- Jangan gunakan `GlobalScope` di mana pun

---

## 3. Aturan Fitur Autocomplete

Ini aturan paling kritis karena menyentuh dua file sekaligus (`transaksi.json` dan `nama_barang_freq.json`):

- Frekuensi **hanya bertambah** saat `simpanTransaksi()` berhasil — tidak bertambah saat draft, tidak berkurang saat hapus
- Saran hanya ditampilkan untuk nama barang dengan `frekuensi >= 2`
- Query matching harus **case-insensitive** dan **substring** (bukan prefix-only)
- Jumlah saran yang ditampilkan maksimal **5 item**
- Saran diurutkan berdasarkan frekuensi tertinggi (bukan alfabetis)
- `debounce(150ms)` wajib ada di antara input change dan pemanggilan `getSaran()` di ViewModel
- Dropdown autocomplete tidak boleh muncul jika field kosong

---

## 4. Penanganan Error

### Prinsip umum
- Jangan biarkan crash yang bisa dicegah — terutama dari operasi file
- Setiap operasi baca file harus punya fallback jika file belum ada atau corrupt
- Tampilkan pesan error yang **informatif** ke user, bukan generic "Terjadi kesalahan"

### Pola error handling di Repository

```kotlin
// Pattern yang wajib digunakan untuk semua operasi baca file
private fun bacaTransaksi(): List<Transaksi> {
    return try {
        val file = File(context.filesDir, "transaksi.json")
        if (!file.exists()) return emptyList()
        gson.fromJson(file.readText(), Array<Transaksi>::class.java).toList()
    } catch (e: Exception) {
        // Log error tapi jangan crash
        emptyList()
    }
}
```

### Skenario yang wajib ditangani
- File JSON tidak ada → return list kosong (bukan throw exception)
- File JSON corrupt / tidak bisa di-parse → return list kosong, log warning
- Storage penuh saat menulis → catch `IOException`, tampilkan Snackbar error ke user
- Import CSV dengan format salah → skip baris bermasalah, laporkan jumlah baris yang gagal

---

## 5. Format Data

### Tanggal
- Format penyimpanan: `"yyyy-MM-dd"` (ISO 8601), contoh: `"2026-06-18"`
- Format tampilan di UI: `"dd MMMM yyyy"`, contoh: `"18 Juni 2026"`
- Gunakan `java.time.LocalDate` untuk parsing dan formatting (API 26+)
- Jangan gunakan `java.util.Date` atau `SimpleDateFormat`

### Nominal uang
- Disimpan sebagai `Double` di JSON
- Ditampilkan di UI sebagai Rupiah: `"Rp 45.000"` (titik sebagai pemisah ribuan)
- Format menggunakan `NumberFormat` dengan `Locale("id", "ID")`

```kotlin
// Helper wajib digunakan, jangan format manual
fun Double.toRupiah(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(this)
}
```

### ID Transaksi
- Selalu gunakan `UUID.randomUUID().toString()` — jangan gunakan timestamp sebagai ID

---

## 6. UI / Compose

### Composable
- Setiap Composable harus menerima data sebagai parameter (bukan mengakses ViewModel langsung dari dalam), kecuali Screen-level Composable
- Screen Composable boleh menerima `ViewModel` sebagai parameter, tapi sub-komponen tidak
- Semua Composable wajib memiliki `@Preview` annotation dengan data dummy

### Navigasi
- Jangan gunakan string literal untuk route — definisikan sebagai konstanta di `NavRoute.kt`

```kotlin
// Benar
object NavRoute {
    const val DASHBOARD = "dashboard"
    const val INPUT = "input"
}

// Salah
navController.navigate("dashboard")  // jangan hardcode
```

### Tema
- Jangan hardcode warna di Composable — selalu gunakan `MaterialTheme.colorScheme.*`
- Jangan hardcode ukuran teks — gunakan `MaterialTheme.typography.*`
- Jangan hardcode dimensi yang dipakai berulang — definisikan di `Dimen.kt`

---

## 7. Testing

### Cakupan minimum yang wajib ada

| Komponen | Yang ditest |
|---|---|
| `JsonHelper` | Baca file tidak ada, baca file valid, tulis dan baca ulang |
| `TransaksiRepository` | Simpan transaksi baru, hapus, edit, getSaran dengan berbagai query |
| `TransaksiViewModel` | State update setelah simpan, filter bulan, logika autocomplete |

### Aturan test
- Gunakan fake/stub untuk file I/O di unit test (jangan akses filesystem asli)
- Nama test mengikuti pola: `namaFungsi_kondisiInput_hasilYangDiharapkan`

```kotlin
// Contoh nama test yang benar
fun getSaran_queryMatchDuaItem_returnDuaItem()
fun getSaran_frekuensiSatu_returnEmpty()
fun simpanTransaksi_namaBarangBaru_frekuensiJadiSatu()
```

---

## 8. Git & Commit

### Struktur pesan commit
```
<tipe>: <deskripsi singkat dalam Bahasa Indonesia>

Contoh:
feat: tambah AutocompleteTextField dengan debounce 150ms
fix: perbaiki crash saat transaksi.json corrupt
refactor: pisahkan logika frekuensi ke FrequencyHelper
test: tambah unit test untuk getSaran edge cases
docs: update ARCHITECTURE.md dengan diagram alur autocomplete
```

### Tipe commit
- `feat` — fitur baru
- `fix` — perbaikan bug
- `refactor` — perubahan kode tanpa fitur baru atau bug fix
- `test` — penambahan atau perbaikan test
- `docs` — perubahan dokumentasi saja
- `chore` — update dependency, konfigurasi build, dsb

### Branch
- `main` — selalu dalam kondisi bisa di-build
- `dev` — branch pengembangan aktif
- `feature/<nama-fitur>` — branch untuk fitur spesifik, merge ke `dev` via PR

---

## 9. Dependency

- Jangan tambahkan library baru tanpa mempertimbangkan alternatif bawaan Android/Kotlin terlebih dahulu
- Setiap dependency baru harus dicatat alasannya di `ARCHITECTURE.md`
- Versi dependency dikelola terpusat di `libs.versions.toml` (Version Catalog), bukan ditulis langsung di `build.gradle.kts`
- Jangan gunakan library yang tidak di-maintain aktif (last commit > 2 tahun)

---

## 10. Keamanan & Privasi

- Data transaksi tersimpan di `context.filesDir` — tidak boleh dipindahkan ke `externalStorageDir` tanpa alasan
- Tidak boleh mengirim data transaksi ke network tanpa konfirmasi eksplisit dari user
- Tidak boleh menambahkan analytics atau crash reporting yang mengumpulkan data personal tanpa persetujuan user
- File export CSV hanya dibuat di direktori yang bisa dikontrol user (Downloads atau via Share Sheet)
