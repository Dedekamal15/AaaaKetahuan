# TODO — AaaaKetahuan

Status: `[ ]` belum | `[x]` selesai | `[-]` in progress | `[~]` ditunda

---

## Fase 1 — Fondasi Project

### Setup & Konfigurasi
- [ ] Buat project Android baru di Android Studio (Empty Compose Activity)
- [ ] Set `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`
- [ ] Tambahkan semua dependency ke `build.gradle.kts` (Compose BOM, Hilt, Gson, MPAndroidChart, Navigation Compose)
- [ ] Setup Hilt: tambahkan plugin `com.google.dagger.hilt.android` dan anotasi `@HiltAndroidApp` ke `Application` class
- [ ] Buat package structure sesuai ARCHITECTURE.md
- [ ] Setup tema aplikasi di `Theme.kt` (warna, tipografi, shape — minimalis putih/abu)

### Data Layer
- [ ] Buat data class `Transaksi.kt` dengan semua field termasuk `namaBarang`
- [ ] Buat `KategoriEnum.kt` dengan 8 kategori
- [ ] Buat `JsonHelper.kt` — fungsi baca/tulis `transaksi.json` dan `nama_barang_freq.json`
- [ ] Buat `TransaksiRepository.kt` dengan semua operasi CRUD
- [ ] Implementasi logika `simpanTransaksi()` — update frekuensi setelah simpan
- [ ] Implementasi `getSaran(query)` — filter `freq >= 2`, case-insensitive, max 5 hasil
- [ ] Implementasi `getTransaksiByBulan(bulan, tahun)`
- [ ] Buat `AppModule.kt` untuk Hilt provider Repository
- [ ] Unit test `JsonHelper` (baca/tulis file)
- [ ] Unit test `TransaksiRepository` (CRUD + logika frekuensi)

---

## Fase 2 — ViewModel

- [ ] Buat `TransaksiViewModel.kt` dengan inject `TransaksiRepository` via Hilt
- [ ] Expose `StateFlow<List<Transaksi>>` untuk daftar transaksi
- [ ] Expose `StateFlow<Double>` untuk `totalMasuk`, `totalKeluar`, `saldo`
- [ ] Expose `StateFlow<Int>` untuk `filterBulan` dan `filterTahun`
- [ ] Implementasi `MutableStateFlow` untuk state form input
- [ ] Implementasi `saranNamaBarang` dengan `debounce(150)` dan `map { getSaran() }`
- [ ] Fungsi `onNamaBarangChange(input: String)` untuk update input dan trigger saran
- [ ] Fungsi `onSubmitTransaksi()` — validasi + simpan + reset form
- [ ] Fungsi `onHapusTransaksi(id: String)`
- [ ] Fungsi `onEditTransaksi(transaksi: Transaksi)`
- [ ] Fungsi `onFilterBulanChange(bulan: Int, tahun: Int)`
- [ ] Unit test ViewModel (mock Repository)

---

## Fase 3 — UI Screens

### Navigasi
- [ ] Buat `NavGraph.kt` dengan Bottom Navigation Bar (Dashboard, Input, Riwayat, Grafik)
- [ ] Tambahkan route untuk `ExportImportScreen`
- [ ] Setup `MainActivity.kt` sebagai host navigasi

### Komponen Shared
- [ ] Buat `AutocompleteTextField.kt` — `ExposedDropdownMenuBox` dengan StateFlow saran
- [ ] Buat `TransaksiCard.kt` — card item untuk list riwayat
- [ ] Buat `RingkasanCard.kt` — card metrik (total masuk, keluar, saldo)
- [ ] Buat `KategoriChip.kt` — chip selector untuk pilih kategori

### DashboardScreen
- [ ] Layout: 3 metrik card di atas (masuk, keluar, saldo)
- [ ] Selector bulan/tahun (dropdown atau chip horizontal)
- [ ] LazyColumn 5 transaksi terkini
- [ ] Tombol "Lihat semua" → navigasi ke Riwayat
- [ ] Tampilan empty state jika belum ada transaksi

### InputTransaksiScreen
- [ ] Field: Jenis (toggle masuk/keluar)
- [ ] Field: Nominal (keyboard numerik, format Rupiah)
- [ ] Field: Nama Barang (`AutocompleteTextField`) ← fitur autocomplete
- [ ] Field: Keterangan (opsional, free text)
- [ ] Field: Kategori (dropdown dari `KategoriEnum`)
- [ ] Field: Tanggal (DatePicker, default hari ini)
- [ ] Tombol Simpan dengan validasi (nominal > 0, namaBarang tidak kosong)
- [ ] Snackbar konfirmasi setelah berhasil simpan
- [ ] Mode edit: pre-fill field dari data transaksi yang dipilih

### RiwayatScreen
- [ ] LazyColumn semua transaksi
- [ ] Filter bulan/tahun (dropdown atau chip)
- [ ] Filter kategori (multi-select chip)
- [ ] Swipe-to-delete dengan konfirmasi dialog
- [ ] Tap item → navigasi ke mode edit di InputTransaksiScreen
- [ ] Tampilan empty state dengan CTA

### GrafikScreen
- [ ] Selector bulan/tahun
- [ ] Bar chart pengeluaran per kategori (MPAndroidChart `BarChart`)
- [ ] Label total pengeluaran bulan itu di atas chart
- [ ] Tabel ringkasan di bawah chart (kategori + nominal + persentase)

### ExportImportScreen
- [ ] Selector rentang: bulan tertentu atau semua data
- [ ] Tombol Export CSV → generate file → Android Share Sheet
- [ ] Tombol Import CSV → file picker → proses import → tampilkan hasil (berhasil/gagal per baris)
- [ ] Preview jumlah transaksi yang akan diexport

---

## Fase 4 — Utilitas

- [ ] Buat `CsvExporter.kt` — konversi `List<Transaksi>` ke string CSV
- [ ] Buat `CsvImporter.kt` — parse CSV ke `List<Transaksi>`, handle error per baris
- [ ] Tambahkan format Rupiah helper: `Double.toRupiah()` → `"Rp 45.000"`
- [ ] Tambahkan helper tanggal: parse `"yyyy-MM-dd"` ↔ `LocalDate`
- [ ] Tambahkan validasi form (nominal, nama barang wajib isi)

---

## Fase 5 — Polish & Testing

- [ ] Tambahkan animasi transisi antar screen (Compose `AnimatedNavHost`)
- [ ] Pastikan semua screen support dark mode
- [ ] Test di berbagai ukuran layar (5", 6.5", tablet)
- [ ] Test skenario: file JSON corrupt → fallback ke list kosong, jangan crash
- [ ] Test skenario: storage penuh → tampilkan error yang informatif
- [ ] Test export CSV: buka di Excel/Google Sheets, pastikan encoding benar (UTF-8 BOM)
- [ ] Test import CSV: file tidak valid, kolom kurang, format salah
- [ ] Review performa: profil memory saat list transaksi > 500 item
- [ ] Tambahkan ProGuard rules untuk Gson (jaga field `Transaksi` dari obfuscation)

---

## Backlog (belum diprioritaskan)

- [ ] Widget homescreen: tampilkan saldo hari ini
- [ ] Backup otomatis ke Google Drive
- [ ] Notifikasi pengingat input harian (jam bisa dikonfigurasi)
- [ ] Fitur pencarian transaksi by keyword
- [ ] Multiple currency support
- [ ] PIN lock aplikasi
- [ ] Migrasi ke Room Database jika data > 2000 transaksi
- [ ] Tema warna kustom

---

## Catatan Rilis

### v1.0.0 (Target)
Fase 1–4 selesai. Semua fitur inti berfungsi.

### v1.1.0
Fase 5 (polish + testing) selesai. Siap distribusi.
