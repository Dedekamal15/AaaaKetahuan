# TODO — AaaaKetahuan

Status: `[ ]` belum | `[x]` selesai | `[-]` in progress | `[~]` ditunda

---

## Fase 1 — Fondasi Project

### Setup & Konfigurasi
- [ ] Buat project Android baru di Android Studio (Empty Compose Activity)
- [ ] Set `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`
- [ ] Masukkan `INTERNET` dan `ACCESS_NETWORK_STATE` ke `AndroidManifest.xml`
- [ ] Tambahkan dependency ke `build.gradle.kts` (termasuk Google API Client Library untuk Sheets)
- [ ] Setup Hilt: tambahkan plugin dan konfigurasi app
- [ ] Setup tema aplikasi di `Theme.kt`

### Data Layer (Lokal)
- [ ] Buat data class `Transaksi.kt` dengan semua field (tambahkan `isSynced: Boolean`)
- [ ] Buat `JsonHelper.kt` — fungsi baca/tulis `transaksi.json`

---

## Fase 2 — ViewModel
- [ ] Buat `TransaksiViewModel.kt` dengan inject `TransaksiRepository` via Hilt
- [ ] Expose `StateFlow` untuk UI dan fungsi event handling

---

## Fase 2.5 — Integrasi Google Sheets (Baru)
- [ ] Buat project di Google Cloud Console dan aktifkan Google Sheets API.
- [ ] Buat Service Account dan unduh file kredensial JSON.
- [ ] Letakkan file JSON ke folder `res/raw/` dan tambahkan ke `.gitignore`.
- [ ] Buat `GoogleSheetsHelper.kt` untuk mengatur autentikasi dan request `appendRow` ke URL Spreadsheet.
- [ ] Update `TransaksiRepository` untuk memanggil `GoogleSheetsHelper` di background setiap kali ada data baru.
- [ ] Implementasi mekanisme sync-retry untuk data yang `isSynced = false`.

---

## Fase 3 — UI Screens
- [ ] Navigasi & Komponen Shared
- [ ] DashboardScreen
- [ ] InputTransaksiScreen
- [ ] RiwayatScreen
- [ ] GrafikScreen
- [ ] ExportImportScreen

---

## Fase 4 — Utilitas
- [ ] Buat helper CsvExporter, format Rupiah, tanggal.

---

## Fase 5 — Polish & Testing
- [ ] Testing skenario offline (koneksi putus) dan sinkronisasi lanjutan.
- [ ] Testing JSON corrupt, storage penuh.

