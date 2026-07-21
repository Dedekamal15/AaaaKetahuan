# TODO — AaaaKetahuan

Status: `[x]` selesai | `[-]` in progress | `[ ]` belum | `[~]` ditunda

---

## ✅ Fase 1 — Fondasi Project (SELESAI)

### Setup & Konfigurasi
- [x] Buat project Android baru
- [x] Set `minSdk = 26`, konfigurasi build
- [x] Izin INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS di AndroidManifest
- [x] Tambah dependency (Compose, Hilt, Gson, MPAndroidChart, Navigation, Google API)
- [x] Setup Hilt
- [x] Setup tema aplikasi (light/dark/system)

### Data Layer (Lokal)
- [x] Data class `Transaksi.kt` (dengan metodeBayar, sumber, isSynced)
- [x] `JsonHelper.kt` — baca/tulis `transaksi.json`
- [x] `nama_barang_freq.json` — frekuensi untuk autocomplete

---

## ✅ Fase 2 — ViewModel & Repository (SELESAI)

- [x] `TransaksiViewModel.kt` dengan Hilt injection
- [x] Expose StateFlow ke UI
- [x] `TransaksiRepository.kt` — dual-write lokal + cloud
- [x] Konfigurasi spreadsheet (SharedPreferences + mutable)
- [x] Konfigurasi tema (SharedPreferences)
- [x] Konfigurasi reminder (SharedPreferences + AlarmManager)

---

## ✅ Fase 2.5 — Integrasi Google Sheets (SELESAI)

- [x] Project Google Cloud + Service Account + credentials.json
- [x] credentials.json di `res/raw/`, diabaikan git
- [x] `GoogleSheetsHelper.kt` — auth + appendRow + readSheet
- [x] Sync-retry untuk data isSynced=false
- [x] UI pengaturan: hubungkan/putuskan spreadsheet + test koneksi

---

## ✅ Fase 3 — UI Screens (SELESAI)

- [x] Navigasi bottom bar (5 item) + route definitions
- [x] DashboardScreen — ringkasan saldo, total, transaksi terkini
- [x] InputTransaksiScreen — form pengeluaran (autocomplete, kategori, metode bayar)
- [x] PemasukanScreen — form pemasukan (sumber pemasukan)
- [x] RiwayatScreen — daftar + filter bulan/kategori + edit
- [x] GrafikScreen — bar chart per kategori
- [x] ExportImportScreen — pilih bulan, export/import CSV
- [x] PengaturanScreen — tema, spreadsheet, duplikat, backup, notifikasi

---

## ✅ Fase 4 — Utilitas (SELESAI)

- [x] `CsvExporter.kt` — export CSV dengan UTF-8 BOM
- [x] `Helpers.kt` — formatNominal(), stripFormatNominal()
- [x] `NotificationHelper.kt` — channel + builder
- [x] `ReminderScheduler.kt` — AlarmManager schedule/cancel
- [x] `ReminderReceiver.kt` — BroadcastReceiver

---

## ✅ Fase 5 — Polish & Testing (SELESAI)

- [x] Testing offline: data tetap tersimpan lokal saat tanpa internet
- [x] JSON corrupt handling: return list kosong, log warning
- [x] CSV encoding: BOM untuk Excel Windows
- [x] Theme mode: system/light/dark via SharedPreferences + StateFlow
- [x] SnackbarHost positioned as overlay
- [x] Bottom nav all labels always visible
- [x] Nominal format: pemisah ribuan titik

---

## 📋 Backlog — Fase Selanjutnya

Fitur yang bisa dikerjakan setelah v1.0 stabil:

| Item | Prioritas | Catatan |
|---|---|---|
| Migration ke Room/SQLite | Medium | Perlu jika data > 2000 transaksi |
| Edit kategori transaksi | Rendah | Saat ini pakai enum tetap |
| Halaman Pemasukan di bottom nav | Medium | Saat ini diakses via Dashboard |
| Fitur duplikat transaksi | Rendah | UI sudah ada di Pengaturan, logic belum |
| Backup/restore lokal | Rendah | UI sudah ada di Pengaturan, logic belum |
| Widget homescreen | Rendah | |
| PIN lock aplikasi | Rendah | |
| Laporan PDF | Rendah | |
| Konversi mata uang | Rendah | |
