# AaaaKetahuan

Aplikasi Android untuk mencatat pemasukan dan pengeluaran bulanan secara lokal yang tersinkronisasi secara real-time ke Google Spreadsheet.

## Fitur Utama

- **Input pengeluaran** — catat pengeluaran harian dengan nama barang, nominal, kategori, dan metode pembayaran
- **Input pemasukan** — halaman khusus untuk mencatat pemasukan (gaji, freelance, dll.) dengan sumber pemasukan
- **Autocomplete nama barang** — saran muncul otomatis setelah nama yang sama diinput ≥ 2 kali
- **Format nominal ribuan** — nominal diformat dengan pemisah ribuan titik (contoh: Rp 20.000)
- **Sinkronisasi real-time ke Google Spreadsheet** — sebagai cloud backup dan akses lintas perangkat
- **Dashboard bulanan** — saldo, total masuk, total keluar, dan daftar transaksi terkini
- **Riwayat transaksi** — dengan filter berdasarkan bulan, kategori, dan edit transaksi
- **Grafik bar bulanan** — per kategori pengeluaran
- **Export dan import data** — dalam format CSV (UTF-8 BOM untuk kompatibilitas Excel)
- **Pengaturan lengkap** — tema (system/light/dark), hubungkan/putuskan spreadsheet, duplikat transaksi, backup/restore
- **Notifikasi reminder harian** — pengingat untuk mencatat transaksi setiap hari (toggle ON/OFF + atur jam)

## Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose + Material3 |
| State management | ViewModel + StateFlow |
| Async | Kotlin Coroutines |
| Serialisasi JSON | Gson |
| Network/Cloud | Google Sheets API (OAuth 2.0 + GoogleSignIn) |
| Grafik | MPAndroidChart |
| Navigasi | Jetpack Navigation Compose |
| Dependency Injection | Hilt |
| Notifikasi | AlarmManager + BroadcastReceiver |

## Struktur Project

```
app/src/main/java/com/example/aaaaketahuan/
├── data/
│   ├── model/
│   │   └── Transaksi.kt          # Data class dengan field metodeBayar & sumber
│   ├── remote/
│   │   └── GoogleSheetsHelper.kt  # Spreadsheet API (mutable config)
│   └── repository/
│       └── TransaksiRepository.kt # Dual-write lokal + cloud
├── di/
│   └── AppModule.kt               # Hilt DI
├── ui/
│   ├── components/
│   │   ├── AutocompleteTextField.kt
│   │   └── KategoriIcon.kt
│   ├── dashboard/
│   │   └── DashboardScreen.kt
│   ├── input/
│   │   └── InputTransaksiScreen.kt
│   ├── pemasukan/
│   │   └── PemasukanScreen.kt
│   ├── riwayat/
│   │   └── RiwayatScreen.kt
│   ├── grafik/
│   │   └── GrafikScreen.kt
│   ├── export/
│   │   └── ExportImportScreen.kt
│   ├── pengaturan/
│   │   └── PengaturanScreen.kt
│   ├── navigation/
│   │   └── NavGraph.kt            # Bottom nav + route definitions
│   └── theme/
│       └── Theme.kt               # Tema dengan light/dark mode
├── util/
│   ├── Helpers.kt                 # formatNominal(), stripFormatNominal()
│   ├── CsvExporter.kt             # Export CSV dengan BOM
│   ├── NotificationHelper.kt      # Notification channel & builder
│   ├── ReminderScheduler.kt       # AlarmManager scheduling
│   └── ReminderReceiver.kt        # BroadcastReceiver untuk alarm
├── AaaaKetahuanApp.kt             # @HiltAndroidApp + init notification channel
└── MainActivity.kt                # Entry point + theme observer
```

> **Sinkronisasi menggunakan OAuth 2.0** — setiap pengguna login dengan akun Google-nya sendiri.
> Spreadsheet dibuat otomatis di Google Drive pengguna. Tidak perlu konfigurasi manual.

## Cara Menjalankan

1. Clone atau buka project di Android Studio (minimum Arctic Fox)
2. Pastikan Kotlin versi 1.9+ dan Compose BOM terbaru terpasang
3. Ikuti panduan di `SETUP_SPREADSHEET.md` untuk konfigurasi Google Cloud Console (OAuth 2.0)
4. Sync Gradle
5. Jalankan di emulator atau perangkat fisik (min API 26 / Android 8.0)

## Persyaratan Sistem

- Android 8.0 (API 26) ke atas
- Memerlukan izin `android.permission.INTERNET` untuk mengirim data ke Google Sheets
- Memerlukan izin `android.permission.ACCESS_NETWORK_STATE` untuk mengecek status koneksi internet sebelum sinkronisasi
- Izin `POST_NOTIFICATIONS` (API 33+) untuk reminder harian
