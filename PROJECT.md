# PROJECT.md — AaaaKetahuan

Dokumen ini adalah sumber kebenaran tunggal (_single source of truth_) untuk konteks project: tujuan, keputusan desain, batasan, dan status saat ini.

---

## Gambaran Umum

**AaaaKetahuan** adalah aplikasi Android native untuk mencatat pemasukan dan pengeluaran pribadi yang tersinkronisasi secara *real-time* dengan Google Spreadsheet. Menggabungkan kecepatan penyimpanan lokal (JSON) dengan kemudahan akses data di *cloud* (Google Sheets).

**Bahasa:** Kotlin  
**UI:** Jetpack Compose + Material3  
**Platform target:** Android 8.0 (API 26) ke atas  
**Pola arsitektur:** MVVM + Clean Architecture (3 layer)  
**Status:** ✅ v1.0 selesai — semua fitur inti telah diimplementasi

---

## Tujuan Produk

### Masalah yang dipecahkan
Banyak aplikasi keuangan pribadi memerlukan akun, koneksi internet, atau izin yang berlebihan. Pengguna yang hanya ingin mencatat pengeluaran harian secara sederhana tidak butuh semua itu.

### Target pengguna
Individu yang ingin mencatat keuangan pribadi secara cepat dan privat, tanpa overhead akun cloud.

### Definisi sukses (v1.0)
- ✅ Input transaksi dalam waktu < 15 detik
- ✅ Data tidak pernah hilang akibat bug (error handling solid)
- ✅ Ekspor CSV bisa dibuka dengan benar di Excel dan Google Sheets
- ✅ Data tersinkronisasi ke Google Sheets secara otomatis saat ada koneksi internet
- ✅ Notifikasi reminder harian untuk konsistensi pencatatan

---

## Fitur

### Dalam scope (v1.0)

| Fitur | Prioritas | Status |
|---|---|---|
| Input transaksi keluar | P0 | ✅ Selesai |
| Input pemasukan (halaman khusus) | P0 | ✅ Selesai |
| Nama barang dengan autocomplete | P0 | ✅ Selesai |
| Kategori transaksi (8 kategori) | P0 | ✅ Selesai |
| Metode pembayaran (Cash, Kredit, E-Wallet, Transfer, QRIS) | P0 | ✅ Selesai |
| Dashboard ringkasan bulanan | P0 | ✅ Selesai |
| Sinkronisasi real-time ke Google Sheets | P0 | ✅ Selesai |
| Riwayat transaksi dengan filter | P1 | ✅ Selesai |
| Grafik bar bulanan per kategori | P1 | ✅ Selesai |
| Export CSV (UTF-8 BOM) | P1 | ✅ Selesai |
| Import CSV | P2 | ✅ Selesai |
| Pengaturan (tema, spreadsheet, duplikat, backup) | P1 | ✅ Selesai |
| Notifikasi reminder harian | P2 | ✅ Selesai |
| Format nominal ribuan (Rp 20.000) | P1 | ✅ Selesai |

### Di luar scope (v1.0)

Fitur-fitur berikut secara eksplisit **tidak** dikerjakan di v1.0:

- Multiple akun atau multi-user
- Widget homescreen
- PIN lock / keamanan layar
- Laporan PDF
- Konversi mata uang
- Kategori kustom (masih menggunakan enum tetap)

---

## Keputusan Teknis & Alasannya

### Mengapa OAuth 2.0 (bukan Service Account)?
Versi awal aplikasi menggunakan **Service Account** dengan `credentials.json` di-resource APK. Untuk rilis publik, ini bermasalah: key bisa diekstrak dari APK, semua user nulis ke spreadsheet yang sama, dan setup terlalu rumit untuk user awam.

**OAuth 2.0** dipilih untuk rilis publik karena:
- **Aman** — tidak ada secret di APK, hanya OAuth Client ID yang diverifikasi via SHA-1
- **Setiap user punya spreadsheet sendiri** — dibuat otomatis di Drive mereka saat login
- **Pengalaman mulus** — cukup login dengan akun Google, tidak perlu konfigurasi manual
- **Token auto-refresh** — `GoogleAccountCredential` + `AccountManager` mengelola token
- **User bisa revoke akses** kapan saja dari Google Account Settings

### Mengapa JSON, bukan Room/SQLite?
JSON dipilih untuk v1.0 karena:
- Tidak ada dependency tambahan (Room membutuhkan setup lebih banyak)
- Data dapat diperiksa dan diedit manual jika diperlukan
- Ukuran data transaksi harian pribadi sangat kecil — performa JSON cukup
- Migration path ke Room sudah tersedia jika diperlukan (Repository pattern)

Batasan yang diterima: performa baca seluruh file akan menurun jika transaksi > 2.000 item. Diterima karena use case ini jarang terjadi dalam 1–2 tahun pemakaian.

### Mengapa frekuensi autocomplete disimpan terpisah?
Alternatif yang ditolak: hitung frekuensi dari `transaksi.json` setiap kali user mengetik.

Masalahnya: jika ada 500 transaksi, setiap keystroke akan mem-parse seluruh file hanya untuk menghitung frekuensi. Terlalu mahal untuk operasi yang terjadi di setiap karakter yang diketik.

Solusi yang dipilih: file `nama_barang_freq.json` yang hanya diupdate saat simpan/hapus. O(1) lookup per query.

### Mengapa threshold autocomplete = 2, bukan 1?
Threshold 1 berarti saran muncul setelah **pertama kali** nama barang diinput. Ini belum memberi nilai — user baru saja mengetiknya, tidak perlu saran untuk hal yang baru pertama kali dilakukan.

Threshold 2 berarti saran muncul setelah **kedua kali** — artinya user pernah melakukan transaksi yang sama sebelumnya. Inilah momen di mana autocomplete benar-benar menghemat waktu.

### Mengapa menggunakan AlarmManager untuk notifikasi, bukan WorkManager?
WorkManager lebih cocok untuk tugas yang perlu flex interval, constraint (internet, charging), atau guaranteed execution. Untuk use case reminder harian sederhana (trigger di jam yang sama setiap hari), `AlarmManager.setRepeating()` sudah cukup dan tidak memerlukan dependency tambahan.

### Mengapa format nominal menggunakan extension function, bukan NumberFormat?
`NumberFormat.getCurrencyInstance()` menghasilkan format yang berbeda-beda tergantung locale perangkat (misal: Rp 20.000 vs IDR 20.000.00). Dengan extension function `String.formatNominal()` dan `stripFormatNominal()` di `Helpers.kt`, format konsisten yaitu `"20.000"` tanpa simbol mata uang (prefix "Rp" ditambahkan di UI).

### Mengapa Hilt, bukan manual DI?
Untuk project sekecil ini, manual DI sebenarnya cukup. Hilt dipilih karena:
- Menghilangkan boilerplate Application-scope object
- Mudah di-scale jika project berkembang
- Sudah jadi standar Android modern

### Mengapa MPAndroidChart, bukan Compose Canvas custom?
Menggambar bar chart yang aksesibel dan responsif dari nol di Compose memakan waktu signifikan. MPAndroidChart mature, well-tested, dan mendukung kasus yang dibutuhkan (bar chart per kategori). Tradeoff: library cukup besar (~500KB), tapi tidak ada alternatif Compose-native yang setara fiturnya per 2026.

### Mengapa metodeBayar dan sumber ditambahkan ke model Transaksi?
Awalnya transaksi hanya mencatat jumlah, kategori, dan nama barang. Saat pengguna mulai mencatat pengeluaran secara detail, metode pembayaran menjadi informasi penting untuk rekonsiliasi keuangan. Field `sumber` ditambahkan khusus untuk transaksi pemasukan agar pengguna bisa melacak dari mana uang berasal (gaji, freelance, dll).

---

## Struktur File Dokumentasi

| File | Isi |
|---|---|
| `PROJECT.md` (ini) | Konteks project, keputusan, status |
| `ARCHITECTURE.md` | Arsitektur teknis, layer, alur data |
| `TODO.md` | Daftar tugas terstruktur per fase |
| `RULES.md` | Konvensi kode, aturan wajib, standar commit |
| `README.md` | Pengenalan project, cara menjalankan |

---

## Dependency Project

```toml
# libs.versions.toml

[versions]
kotlin = "1.9.23"
compose-bom = "2024.05.00"
hilt = "2.51"
gson = "2.10.1"
mpandroidchart = "v3.1.0"
navigation-compose = "2.7.7"
lifecycle = "2.7.0"
coroutines = "1.8.0"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }
mpandroidchart = { group = "com.github.PhilJay", name = "MPAndroidChart", version.ref = "mpandroidchart" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
# Google API Client Libraries (ditambahkan manual di build.gradle.kts)
```

> MPAndroidChart diambil dari JitPack. Pastikan `jitpack.io` sudah ditambahkan ke `settings.gradle.kts`:
> ```kotlin
> maven { url = uri("https://jitpack.io") }
> ```

---

## Risiko & Mitigasi

| Risiko | Kemungkinan | Mitigasi |
|---|---|---|
| File JSON corrupt saat app crash di tengah penulisan | Sedang | Tulis ke file temp dulu, rename setelah berhasil (atomic write) |
| Data hilang saat uninstall | Pasti | Tersinkronisasi ke Google Sheets sebagai backup utama |
| Performa lambat saat data > 1000 transaksi | Rendah-Sedang | Monitor dengan Profiler; siapkan migration ke Room |
| MPAndroidChart tidak di-maintain | Rendah | Sudah stabil bertahun-tahun; fork tersedia jika perlu |
| Encoding CSV salah di Excel Windows | Sedang | UTF-8 BOM ✅ |
| Gagal sinkronisasi karena internet mati | Tinggi | `isSynced=false` retry otomatis ✅ |
| Notifikasi tidak muncul di API 33+ | Sedang | Minta izin `POST_NOTIFICATIONS` ✅ |
| Theme mode "system" tidak sinkron | Rendah | Observe `isSystemInDarkTheme()` ✅ |
| OAuth token expired | Rendah | `GoogleAccountCredential` auto-refresh via AccountManager ✅ |
| APK dibongkar (reverse engineering) | Tinggi | OAuth 2.0 aman — tidak ada secret di APK, hanya SHA-1 signature ✅ |

---

## Kontak & Kontribusi

Project ini dikembangkan sebagai aplikasi personal. Untuk perubahan besar, diskusikan terlebih dahulu melalui issue sebelum membuat PR.
