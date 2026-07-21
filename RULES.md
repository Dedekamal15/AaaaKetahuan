# Rules & Konvensi Kode — AaaaKetahuan

Dokumen ini adalah kontrak internal project. Semua kontributor (termasuk diri sendiri di masa depan) wajib mengikuti aturan ini untuk menjaga konsistensi dan kualitas kode.

---

## 1. Bahasa & Penamaan
- Semua nama variabel, fungsi, class, dan komentar kode ditulis dalam **Bahasa Inggris**
- UI-facing string (label tombol, pesan error, placeholder) ditulis dalam **Bahasa Indonesia**
- Nama file: PascalCase untuk class (`TransaksiRepository.kt`), camelCase untuk fungsi (`simpanTransaksi()`)

---

## 2. Arsitektur
- **UI tidak boleh** mengakses Repository secara langsung — harus melalui ViewModel
- **ViewModel tidak boleh** mengimport class dari paket `android.content` atau `java.io.File`
  - Semua operasi yang butuh `Context` / `SharedPreferences` → delegasikan ke Repository
  - Repository mendapat `Context` via Hilt injection (`@ApplicationContext`)
- Semua state diekspos sebagai `StateFlow` (kecuali state lokal ephemeral di UI)
- ViewModel cukup satu (`TransaksiViewModel`) untuk semua screen

---

## 3. Aturan Fitur Autocomplete
- Frekuensi **hanya bertambah** saat `simpanTransaksi()` berhasil
- Saran hanya ditampilkan untuk `frekuensi >= 2`
- Query matching case-insensitive, maksimal 5 item
- Flow autocomplete di-debounce via `debounce(300)` di `init` ViewModel untuk hindari recompose berlebihan

---

## 4. Penanganan Error

### Skenario yang wajib ditangani
- File JSON tidak ada → return list kosong (bukan throw exception)
- File JSON corrupt / tidak bisa di-parse → return list kosong, log warning
- Storage penuh saat menulis → catch `IOException`, tampilkan Snackbar error ke user
- **Network Error:** Jika HP tidak ada koneksi internet, aplikasi tetap menyimpan data ke file lokal, lalu memberi penanda `isSynced = false` untuk dikirim nanti
- **Spreadsheet tidak terhubung:** Simpan lokal tetap jalan, tampilkan indikasi di UI

---

## 5. Format Data
- Tanggal disimpan sebagai `"yyyy-MM-dd"`
- Nominal sebagai `Double`, disimpan tanpa format (contoh: `20000.0`)
- Format nominal di UI menggunakan `String.formatNominal()`:
  - Input: `"20000"` → Output: `"20.000"`
  - Untuk逆向 (strip): `stripFormatNominal("20.000")` → `"20000"`
- Gunakan `UUID.randomUUID().toString()` untuk ID

---

## 6. Aturan Spreadsheet (OAuth 2.0)
- Autentikasi menggunakan **OAuth 2.0** (GoogleSignIn) — bukan Service Account
- Setiap pengguna login dengan akun Google-nya sendiri → spreadsheet dibuat otomatis di Drive pengguna
- Spreadsheet ID dan nama Sheet disimpan di SharedPreferences setelah OAuth sukses
- `GoogleAccountCredential` mengelola token OAuth secara otomatis via `AccountManager`
- Tidak boleh ada `credentials.json` di project — gunakan OAuth Client ID di Google Cloud Console

---

## 7. Aturan Notifikasi / Reminder
- Gunakan `AlarmManager.setRepeating()` untuk jadwal harian (bukan WorkManager)
- `ReminderReceiver` adalah `BroadcastReceiver` yang menampilkan notifikasi via `NotificationHelper`
- `NotificationHelper.createChannel()` dipanggil sekali di `Application.onCreate()`
- Izin `POST_NOTIFICATIONS` (API 33+) ditangani via request di runtime
- Default reminder: jam 20:00

---

## 8. Aturan Theme
- Theme mode disimpan sebagai `String` di SharedPreferences: `"system"` | `"light"` | `"dark"`
- ViewModel expose `themeMode: StateFlow<String>`
- `MainActivity` observe themeMode → apply ke `AaaaKetahuanTheme(darkTheme = ...)`
- Default: `"system"` (ikuti setting sistem)

---

## 9. Navigasi
- Bottom Navigation Bar: 5 item tetap (Dashboard, Input, Pemasukan, Riwayat, Grafik)
- Gunakan `alwaysShowLabel = true` agar label semua item selalu terlihat
- `NavRoute` object mendefinisikan semua route string
- Halaman non-bottom-nav (Pengaturan, Export/Import) diakses via tombol di screen lain

---

## 10. Keamanan & Privasi (OAuth 2.0)

- Autentikasi menggunakan OAuth 2.0 — pengguna login dengan **akun Google mereka sendiri**
- Token OAuth dikelola oleh `GoogleAccountCredential` + `AccountManager` Android — aman dan auto-refresh
- Pengguna bisa mencabut izin kapan saja dari **Google Account Settings**
- Tidak ada secret/key yang disimpan di APK — aman dari reverse engineering
- Data transaksi tersimpan di `context.filesDir`
- Tidak boleh mengirim data transaksi ke endpoint selain Google Sheets endpoint
- Semua operasi network via HTTPS

---

## 11. Format Nominal (Rupiah)

### Tampilan di UI
- Nominal ditampilkan dengan prefix `"Rp "` dan pemisah ribuan titik
- Contoh: `Rp 20.000`, `Rp 1.500.000`, `Rp 0`
- Gunakan `String.formatNominal()` untuk format display
- Gunakan `String.stripFormatNominal()` untuk mengembalikan ke angka mentah

### Penyimpanan
- Simpan sebagai `Double` di `Transaksi.jumlah`
- Jangan simpan dengan format mata uang — simpan nilai mentah

---

## 12. Model Transaksi

```kotlin
data class Transaksi(
    val id: String,              // UUID.randomUUID().toString()
    val tanggal: String,         // "yyyy-MM-dd"
    val jenis: String,           // "masuk" | "keluar"
    val jumlah: Double,
    val namaBarang: String,
    val keterangan: String,
    val kategori: String,
    val bulan: Int,
    val tahun: Int,
    val metodeBayar: String,     // "Cash" | "Kredit" | "E-Wallet" | "Transfer" | "QRIS"
    val sumber: String,          // "Gaji" | "Lainnya" (hanya untuk pemasukan)
    var isSynced: Boolean        // status sinkronisasi cloud
)
```
