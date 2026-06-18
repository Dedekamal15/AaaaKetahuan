# AaaaKetahuan

Aplikasi Android untuk mencatat pemasukan dan pengeluaran bulanan secara lokal, tanpa database eksternal.

## Fitur Utama

- Input transaksi masuk dan keluar dengan nama barang, nominal, dan kategori
- Autocomplete nama barang — saran muncul otomatis setelah nama yang sama diinput ≥ 2 kali
- Dashboard bulanan: saldo, total masuk, total keluar, dan daftar transaksi terkini
- Riwayat transaksi dengan filter berdasarkan bulan dan kategori
- Grafik bar bulanan per kategori pengeluaran
- Export dan import data dalam format CSV
- Semua data tersimpan lokal di internal storage perangkat (tidak butuh internet)

## Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose |
| State management | ViewModel + StateFlow |
| Async | Kotlin Coroutines |
| Serialisasi JSON | Gson |
| Grafik | MPAndroidChart |
| Navigasi | Jetpack Navigation Compose |
| Dependency Injection | Hilt |

## Struktur Data

Data disimpan dalam dua file JSON di internal storage app (`context.filesDir`):

**`transaksi.json`** — daftar semua transaksi

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "tanggal": "2026-06-18",
    "jenis": "keluar",
    "jumlah": 45000.0,
    "namaBarang": "Makan siang",
    "keterangan": "Warung bu Tini",
    "kategori": "Makanan",
    "bulan": 6,
    "tahun": 2026
  }
]
```

**`nama_barang_freq.json`** — frekuensi kemunculan nama barang untuk autocomplete

```json
{
  "Makan siang": 5,
  "Bensin": 3,
  "Aqua": 2
}
```

## Struktur Project

```
app/src/main/java/com/example/aaaaketahuan/
├── data/
│   ├── model/
│   │   └── Transaksi.kt
│   └── repository/
│       └── TransaksiRepository.kt
├── di/
│   └── AppModule.kt
├── ui/
│   ├── dashboard/
│   │   └── DashboardScreen.kt
│   ├── input/
│   │   ├── InputTransaksiScreen.kt
│   │   └── AutocompleteTextField.kt
│   ├── riwayat/
│   │   └── RiwayatScreen.kt
│   ├── grafik/
│   │   └── GrafikScreen.kt
│   ├── export/
│   │   └── ExportImportScreen.kt
│   └── theme/
│       └── Theme.kt
├── util/
│   ├── JsonHelper.kt
│   └── CsvExporter.kt
├── viewmodel/
│   └── TransaksiViewModel.kt
└── MainActivity.kt
```

## Cara Menjalankan

1. Clone atau buka project di Android Studio (minimum Arctic Fox)
2. Pastikan Kotlin versi 1.9+ dan Compose BOM terbaru terpasang
3. Sync Gradle
4. Jalankan di emulator atau perangkat fisik (min API 26 / Android 8.0)

## Cara Export Data

Buka menu **Export / Import** → pilih rentang bulan → tap **Export CSV**. File akan tersimpan di folder Downloads atau dapat langsung dibagikan via Share Sheet.

## Persyaratan Sistem

- Android 8.0 (API 26) ke atas
- Tidak memerlukan izin internet
- Memerlukan izin `WRITE_EXTERNAL_STORAGE` hanya untuk export CSV ke Downloads (API < 29)
