# Rules & Konvensi Kode — AaaaKetahuan

Dokumen ini adalah kontrak internal project. Semua kontributor (termasuk diri sendiri di masa depan) wajib mengikuti aturan ini untuk menjaga konsistensi dan kualitas kode.

---

## 1. Bahasa & Penamaan
- Semua nama variabel, fungsi, class, dan komentar kode ditulis dalam **Bahasa Inggris**
- UI-facing string (label tombol, pesan error, placeholder) ditulis dalam **Bahasa Indonesia** dan didefinisikan di `strings.xml`

---

## 2. Arsitektur
- **UI tidak boleh** mengakses Repository secara langsung — harus melalui ViewModel
- **ViewModel tidak boleh** mengimport class dari paket `android.content` atau file
- Semua state diekspos sebagai `StateFlow`

---

## 3. Aturan Fitur Autocomplete
- Frekuensi **hanya bertambah** saat `simpanTransaksi()` berhasil
- Saran hanya ditampilkan untuk `frekuensi >= 2`
- Query matching case-insensitive, maksimal 5 item.

---

## 4. Penanganan Error

### Skenario yang wajib ditangani
- File JSON tidak ada → return list kosong (bukan throw exception)
- File JSON corrupt / tidak bisa di-parse → return list kosong, log warning
- Storage penuh saat menulis → catch `IOException`, tampilkan Snackbar error ke user
- **Network Error:** Jika HP tidak ada koneksi internet (`ACCESS_NETWORK_STATE` bernilai false), aplikasi harus tetap menyimpan data ke `transaksi.json` secara lokal, lalu memberikan penanda (`isSynced = false`) agar bisa dikirim ke Google Sheets nanti saat internet kembali menyala.

---

## 5. Format Data
- Tanggal disimpan sebagai: `"yyyy-MM-dd"`
- Nominal sebagai `Double`, diformat ke UI menggunakan `.toRupiah()`
- Gunakan `UUID.randomUUID().toString()` untuk ID.

---

## 10. Keamanan & Privasi

- **JANGAN PERNAH** melakukan *commit/push* file `credentials.json` (milik Service Account) ke repositori Git publik. Pastikan file tersebut dimasukkan ke dalam `.gitignore` untuk mencegah kebocoran akses Google Cloud.
- Data transaksi tersimpan di `context.filesDir`.
- Tidak boleh mengirim data transaksi ke cloud/network KECUALI ke endpoint Google Sheets yang telah diatur Service Account-nya.
