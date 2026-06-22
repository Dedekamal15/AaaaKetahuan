# PROJECT.md — AaaaKetahuan

Dokumen ini adalah sumber kebenaran tunggal (_single source of truth_) untuk konteks project: tujuan, keputusan desain, batasan, dan status saat ini.

---

## Gambaran Umum

**AaaaKetahuan** adalah aplikasi Android native untuk mencatat pemasukan dan pengeluaran pribadi yang tersinkronisasi secara *real-time* dengan Google Spreadsheet. Menggabungkan kecepatan penyimpanan lokal (JSON) dengan kemudahan akses data di *cloud* (Google Sheets).

**Bahasa:** Kotlin  
**UI:** Jetpack Compose  
**Platform target:** Android 8.0 (API 26) ke atas  
**Pola arsitektur:** MVVM + Clean Architecture (3 layer)  
**Status:** Fase perencanaan — siap implementasi

---

## Tujuan Produk

### Masalah yang dipecahkan
Banyak aplikasi keuangan pribadi memerlukan akun, koneksi internet, atau izin yang berlebihan. Pengguna yang hanya ingin mencatat pengeluaran harian secara sederhana tidak butuh semua itu.

### Target pengguna
Individu yang ingin mencatat keuangan pribadi secara cepat dan privat, tanpa overhead akun cloud.

### Definisi sukses (v1.0)
- Input transaksi dalam waktu < 15 detik
- Data tidak pernah hilang akibat bug (error handling solid)
- Ekspor CSV bisa dibuka dengan benar di Excel dan Google Sheets
- Data tersinkronisasi ke Google Sheets secara otomatis saat ada koneksi internet

---

## Fitur

### Dalam scope (v1.0)

| Fitur | Prioritas | Status |
|---|---|---|
| Input transaksi masuk/keluar | P0 | Belum |
| Nama barang dengan autocomplete | P0 | Belum |
| Kategori transaksi (8 kategori) | P0 | Belum |
| Dashboard ringkasan bulanan | P0 | Belum |
| Sinkronisasi real-time ke Google Sheets | P0 | Belum |
| Riwayat transaksi dengan filter | P1 | Belum |
| Grafik bar bulanan per kategori | P1 | Belum |
| Export CSV | P1 | Belum |
| Import CSV | P2 | Belum |

### Di luar scope (v1.0)

Fitur-fitur berikut secara eksplisit **tidak akan** dikerjakan di v1.0:

- Multiple akun atau multi-user
- Notifikasi/reminder
- Widget homescreen
- PIN lock
- Laporan PDF
- Konversi mata uang

Alasan: menjaga scope agar v1.0 bisa selesai dan stabil. Fitur-fitur ini masuk Backlog di `TODO.md`.

---

## Keputusan Teknis & Alasannya

### Mengapa Google Sheets API (Service Account)?
Google Sheets dipilih sebagai *backend/database* gratis agar pengguna dapat dengan mudah melihat, membagikan, atau mengedit data keuangan mereka melalui PC. Penggunaan Service Account memungkinkan aplikasi mengirim data di *background* tanpa mengharuskan pengguna *login* via OAuth2 setiap kali membuka aplikasi.

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

### Mengapa Hilt, bukan manual DI?

Untuk project sekecil ini, manual DI sebenarnya cukup. Hilt dipilih karena:
- Menghilangkan boilerplate Application-scope object
- Mudah di-scale jika project berkembang
- Sudah jadi standar Android modern

### Mengapa MPAndroidChart, bukan Compose Canvas custom?

Menggambar bar chart yang aksesibel dan responsif dari nol di Compose memakan waktu signifikan. MPAndroidChart mature, well-tested, dan mendukung kasus yang dibutuhkan (bar chart per kategori). Tradeoff: library cukup besar (~500KB), tapi tidak ada alternatif Compose-native yang setara fiturnya per 2026.

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
# Google API dependencies will be added di sini (Fase 2.5)
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
| Data hilang saat uninstall | Pasti | Dokumentasikan di README; tersinkronisasi ke Google Sheets sebagai backup utama |
| Performa lambat saat data > 1000 transaksi | Rendah-Sedang | Monitor dengan Profiler; siapkan migration ke Room |
| MPAndroidChart tidak di-maintain | Rendah | Sudah stabil bertahun-tahun; fork tersedia jika perlu |
| Encoding CSV salah di Excel Windows | Sedang | Tambahkan UTF-8 BOM saat export |
| Gagal sinkronisasi karena internet mati | Tinggi | Flag `isSynced=false` pada data lokal, retry otomatis saat internet kembali |

---

## Kontak & Kontribusi

Project ini dikembangkan sebagai aplikasi personal. Untuk perubahan besar, diskusikan terlebih dahulu melalui issue sebelum membuat PR.
