# AGENTS.md — AaaaKetahuan

Single-module Android (Kotlin + Jetpack Compose) personal finance tracker synced to Google Sheets via OAuth 2.0.

## Quick Build & Run

```bash
./gradlew assembleDebug          # full build (no tests exist)
./gradlew assembleRelease        # minified via ProGuard
```

- **Requires** Android SDK (`local.properties` with `sdk.dir`).
- **No CI/CD** — no `.github/` workflows.
- **No tests** — neither `src/test/` nor `src/androidTest/` dirs exist.

## Architecture

- **3-layer MVVM**: UI (Compose) → ViewModel (`TransaksiViewModel`) → Repository (`TransaksiRepository`) → Data (JSON + Google Sheets API)
- **Single ViewModel** — `TransaksiViewModel` is shared across all screens via `hiltViewModel()`. State exposed as `StateFlow`.
- **Hilt DI** — `AppModule.kt` provides `GoogleSheetsHelper` and `TransaksiRepository`.
- **No Room/SQLite** — data stored as JSON files in `context.filesDir`:
  - `transaksi.json` (all transactions, with `isSynced` flag)
  - `nama_barang_freq.json` (autocomplete frequency map)

## Key Config

- **Gradle 8.9**, AGP 8.3.2, Kotlin 1.9.23
- **compileSdk / targetSdk 34**, minSdk 26
- **Compose BOM 2024.05.00**, Compose Compiler 1.5.11
- **JitPack required** for MPAndroidChart — already in `settings.gradle.kts`
- Version catalog at `gradle/libs.versions.toml`

## Conventions

- **Code identifiers & comments** = English. **UI strings** = Indonesian.
- **File naming**: PascalCase for classes (`TransaksiRepository.kt`), camelCase for functions.
- **Nominal format**: `Double` stored raw (e.g., `20000.0`); display uses `formatNominal()` → `"20.000"`. See `Helpers.kt`.
- **Date format**: `"yyyy-MM-dd"` as `String`.
- **Transaction IDs**: `UUID.randomUUID().toString()`.

## Source-of-Truth Discrepancies

These are cases where prose docs are stale — trust the code:

| Claim (RULES.md) | Actual (code) | Action |
|---|---|---|
| Autocomplete debounce = 300ms | `debounce(150)` in ViewModel | Use 150ms |
| ViewModel must not import `android.content` | ViewModel imports `android.net.Uri` and `android.content.Intent` | Accept this; don't refactor |

## Google Sheets / OAuth 2.0

- **OAuth 2.0 with GoogleSignIn** — *not* Service Account. Each user authenticates individually.
- OAuth Client ID is verified via SHA-1 signing certificate (no secret in APK).
- **No `credentials.json`** in the source tree — it's gitignored (legacy Service Account artifact).
- Spreadsheet ID + sheet name stored in `SharedPreferences`.
- `GoogleAccountCredential` auto-refreshes tokens via `AccountManager`.
- Setup guide: `SETUP_SPREADSHEET.md`. Must configure OAuth consent screen in Google Cloud Console.

## State Management

```
UI.collectAsState() ← StateFlow ← TransaksiViewModel ← TransaksiRepository
```

Key flows:
- `transaksiList` — filtered by current month/year
- `filterBulan` / `filterTahun` — active filter selection
- `saranNamaBarang` — autocomplete results (debounced at 150ms)
- `totalMasuk`, `totalKeluar`, `saldo` — derived via `.map {}`
- `themeMode` — `"system"` / `"light"` / `"dark"`
- `_pendingAuthIntent` — OAuth re-auth intent from Google Sheets API

## Navigation

- **Bottom nav**: 5 fixed items — Dashboard, Input, Pemasukan, Riwayat, Grafik
- **Additional screens** (no bottom nav): Pengaturan, Export/Import, Edit Transaksi
- Routes defined in `NavRoute` object
- `alwaysShowLabel = true` for all bottom nav items

## Notifications / Reminder

- `AlarmManager.setRepeating()` + `BroadcastReceiver` — not WorkManager
- `NotificationHelper.createChannel()` called in `Application.onCreate()`
- Default reminder: 20:00
- Requires `POST_NOTIFICATIONS` permission on API 33+

## ProGuard (release build)

Keeps these classes from obfuscation (see `proguard-rules.pro`):
- `com.example.aaaaketahuan.data.model.**`
- `com.github.mikephil.charting.**`
- `com.google.api.client.**`, `com.google.api.services.sheets.**`, `com.google.auth.**`

## Project Layout

```
app/src/main/java/com/example/aaaaketahuan/
├── AaaaKetahuanApp.kt          # @HiltAndroidApp
├── MainActivity.kt             # Entry + theme observer
├── data/
│   ├── model/                  # Transaksi, KategoriEnum, MetodeBayarEnum, SumberPemasukanEnum
│   ├── remote/                 # GoogleSheetsHelper.kt
│   └── repository/             # TransaksiRepository.kt (dual-write local + cloud)
├── di/                         # AppModule.kt
├── ui/
│   ├── components/             # AutocompleteTextField, KategoriIcon
│   ├── dashboard/              # DashboardScreen
│   ├── export/                 # ExportImportScreen
│   ├── grafik/                 # GrafikScreen (MPAndroidChart)
│   ├── input/                  # InputTransaksiScreen (also used for edit)
│   ├── navigation/             # NavGraph, NavRoute
│   ├── pemasukan/              # PemasukanScreen
│   ├── pengaturan/             # PengaturanScreen
│   ├── riwayat/                # RiwayatScreen
│   └── theme/                  # Theme.kt
├── util/                       # JsonHelper, Helpers, CsvExporter, CsvImporter, NotificationHelper, ReminderScheduler
└── viewmodel/                  # TransaksiViewModel.kt
```

## Gotchas

- **No search tooling** — lint, ktlint, detekt, spotless are *not configured*. Only Gradle build verification.
- **Autocomplete threshold**: suggestions appear only when `frekuensi >= 2` (not 1).
- **CSV export** uses UTF-8 BOM for Excel compatibility.
- **JSON atomic writes**: file written to temp then renamed (crash-safe).
- **Network resilience**: transactions save locally even when offline (`isSynced=false`). Background retry on `syncPending()`.
- **Chart library is view-based**: MPAndroidChart renders via `AndroidView` composable wrapper (not native Compose).
- **Kategori/metodeBayar/sumber can be customized**: stored as JSON in SharedPreferences via repository.
- **Custom kategori** are allowed in addition to the 8 enum entries, and kategori can be hidden.
