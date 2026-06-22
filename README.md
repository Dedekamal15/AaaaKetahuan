# AaaaKetahuan

Aplikasi Android untuk mencatat pemasukan dan pengeluaran bulanan secara lokal yang tersinkronisasi secara real-time ke Google Spreadsheet.

## Fitur Utama

- Input transaksi masuk dan keluar dengan nama barang, nominal, dan kategori
- Autocomplete nama barang — saran muncul otomatis setelah nama yang sama diinput ≥ 2 kali
- **Sinkronisasi real-time ke Google Spreadsheet sebagai cloud backup dan akses lintas perangkat**
- Dashboard bulanan: saldo, total masuk, total keluar, dan daftar transaksi terkini
- Riwayat transaksi dengan filter berdasarkan bulan dan kategori
- Grafik bar bulanan per kategori pengeluaran
- Export dan import data dalam format CSV

## Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose |
| State management | ViewModel + StateFlow |
| Async | Kotlin Coroutines |
| Serialisasi JSON | Gson |
| Network/Cloud | Google Sheets API (Google API Client) |
| Grafik | MPAndroidChart |
| Navigasi | Jetpack Navigation Compose |
| Dependency Injection | Hilt |

## Struktur Project

```
app/src/main/java/com/example/aaaaketahuan/
├── data/
│   ├── model/
│   │   └── Transaksi.kt
│   ├── remote/
│   │   └── GoogleSheetsHelper.kt
│   └── repository/
│       └── TransaksiRepository.kt
├── di/
│   └── AppModule.kt
├── ui/
...
└── MainActivity.kt
```

*Catatan: Kredensial API (Service Account) ditempatkan di `app/src/main/res/raw/credentials.json` dan **wajib** diabaikan dalam `.gitignore`.*

## Cara Menjalankan

1. Clone atau buka project di Android Studio (minimum Arctic Fox)
2. Pastikan Kotlin versi 1.9+ dan Compose BOM terbaru terpasang
3. Siapkan `credentials.json` dari Google Cloud Service Account dan letakkan di `res/raw/`
4. Sync Gradle
5. Jalankan di emulator atau perangkat fisik (min API 26 / Android 8.0)

## Persyaratan Sistem

- Android 8.0 (API 26) ke atas
- Memerlukan izin `android.permission.INTERNET` untuk mengirim data ke Google Sheets.
- Memerlukan izin `android.permission.ACCESS_NETWORK_STATE` untuk mengecek status koneksi internet sebelum sinkronisasi.
