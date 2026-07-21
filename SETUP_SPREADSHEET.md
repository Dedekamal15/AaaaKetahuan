# Setup Google Spreadsheet — AaaaKetahuan (OAuth 2.0)

Panduan untuk menyiapkan Google Sheets API + login pengguna (OAuth 2.0).

> **Untuk pengguna aplikasi:** Cukup buka Pengaturan → Hubungkan → pilih akun Google → spreadsheet auto-terbuat.
> Panduan di bawah ini hanya untuk **developer/publisher** yang ingin build dari source.

---

## Prasyarat

- Akun Google (untuk Google Cloud Console)
- Project Android ini sudah siap di-build

---

## Bagian 1: Setup Google Cloud Console (SEKALI SAJA)

Ini adalah **satu kali setup** oleh developer aplikasi. Setelah selesai, semua pengguna cukup login dengan akun Google mereka — tanpa perlu setup apa pun.

### Langkah 1: Buat Project

1. Buka [Google Cloud Console](https://console.cloud.google.com/)
2. Klik dropdown project → **New Project**
3. Isi **Project name:** `AaaaKetahuan`
4. Klik **Create**

### Langkah 2: Aktifkan Google Sheets API

1. Buka menu ☰ → **APIs & Services** → **Library**
2. Cari **"Google Sheets API"** → klik → **Enable**

### Langkah 3: Konfigurasi OAuth Consent Screen

1. Buka menu ☰ → **APIs & Services** → **OAuth consent screen**
2. Pilih **User Type:** **External** (karena aplikasi publik)
3. Klik **Create**
4. Isi:
   - **App name:** `AaaaKetahuan`
   - **User support email:** pilih email kamu
   - **Developer contact info:** email kamu
5. Klik **Save and Continue**
6. **Scopes:** klik **Add or Remove Scopes**
   - Cari `.../auth/spreadsheets`
   - Centang **`.../auth/spreadsheets`** (Google Sheets API)
   - Klik **Update** → **Save and Continue**
7. **Test users:** (lewati dulu, bisa diisi nanti)
8. Klik **Save and Continue**

> **Catatan:** Karena scope `spreadsheets` tergolong **sensitive**, Google akan memverifikasi aplikasi jika pengguna > 100. Untuk testing, 100 user pertama tidak perlu verifikasi.

### Langkah 4: Buat OAuth 2.0 Client ID (Android)

1. Buka menu ☰ → **APIs & Services** → **Credentials**
2. Klik **+ Create Credentials** → **OAuth client ID**
3. Pilih **Application type:** **Android**
4. Isi:
   - **Name:** `AaaaKetahuan Android`
   - **Package name:** `com.example.aaaaketahuan`
   - **SHA-1 certificate fingerprint:** (lihat cara di bawah)
5. Klik **Create**

#### Cara Mendapatkan SHA-1 Fingerprint

Buka terminal di folder project, jalankan:

```bash
cd android
./gradlew signingReport
```

Cari baris **SHA1** di bagian `variant: debug`:
```
Variant: debug
Config: debug
Store: .../.android/debug.keystore
SHA1:  BB:0D:AC:74:... (28 karakter)
```

Copy SHA-1 tersebut ke form Google Cloud Console.

Untuk **release build**, kamu perlu menggunakan keystore production:
```bash
keytool -list -v -keystore path/to/keystore.jks -alias your-alias
```

### Langkah 5: Catat Client ID

Setelah OAuth client dibuat, catat **Client ID** (bukan Client Secret). Contoh:
```
1234567890-abc123def456.apps.googleusercontent.com
```

**Tidak perlu dimasukkan ke kode** — GoogleSignIn otomatis mendeteksinya dari `google-services.json` atau dari Android Package Name + SHA-1.

---

## Bagian 2: Untuk Pengguna Aplikasi

Ini yang dilakukan **pengguna akhir**:

1. Buka aplikasi AaaaKetahuan
2. Buka **Pengaturan** (dari Dashboard → ikon ⚙️)
3. Di bagian **Spreadsheet & Sync**, tap **Hubungkan**
4. Pilih akun Google kamu
5. Setuju izin akses Google Sheets
6. Tunggu beberapa detik — spreadsheet baru akan auto-terbuat di Google Drive kamu
7. Selesai! Semua transaksi akan sync otomatis

> **Spreadsheet dibuat otomatis** di Google Drive kamu dengan judul "AaaaKetahuan".
> Kamu bisa buka, edit, atau bagikan spreadsheet tersebut kapan saja.

---

## Troubleshooting

| Masalah | Penyebab | Solusi |
|---------|----------|--------|
| `GoogleSignIn` tidak muncul | Tidak ada akun Google di HP | Tambah akun Google di Settings HP |
| Login berhasil tapi sheet tidak terbuat | Tidak ada koneksi internet | Cek koneksi, coba lagi |
| `403` error setelah login | OAuth consent screen scope kurang | Pastikan `.../auth/spreadsheets` sudah ditambahkan di scopes |
| `10_000` error | SHA-1 salah | Cek SHA-1 dengan `signingReport`, update di Cloud Console |
| Login tapi tidak sync | Spreadsheet ID kosong | Coba putuskan lalu hubungkan lagi |
| "Access blocked" | Aplikasi belum diverifikasi Google | Klik **Continue** (untuk < 100 user) atau ajukan verifikasi |

---

## Migrasi dari Service Account (Versi Lama)

Jika sebelumnya pakai `credentials.json` (Service Account):

1. Aplikasi baru tetap bisa baca spreadsheet lama jika user **share spreadsheet** ke akun Google pribadinya
2. Tapi lebih praktis: hubungkan dengan akun baru, spreadsheet baru auto-terbuat
3. Data lokal tetap aman — hanya sinkronisasi yang pindah ke spreadsheet baru
4. File `credentials.json` bisa dihapus dari project (sudah tidak dipakai)

---

## Arsitektur OAuth 2.0

```
┌─────────────────────────────────────────────┐
│              HP Pengguna                     │
│                                              │
│  PengaturanScreen                            │
│       │                                      │
│       │ tap "Hubungkan"                      │
│       ▼                                      │
│  GoogleSignIn (pilih akun)                   │
│       │                                      │
│       │ onSuccess → account.email            │
│       ▼                                      │
│  GoogleAccountCredential                     │
│  (mengelola token otomatis)                  │
│       │                                      │
│       ▼                                      │
│  Sheets API → create spreadsheet             │
│           di Drive pengguna                  │
│       │                                      │
│       ▼                                      │
│  Spreadsheet ID disimpan di                  │
│  SharedPreferences lokal                     │
└─────────────────────────────────────────────┘
```

- Tidak ada `credentials.json` di APK
- Setiap user punya spreadsheet **milik sendiri** di Drive-nya
- Token dikelola oleh `GoogleAccountCredential` + `AccountManager` — auto-refresh
- Izin bisa dicabut kapan saja dari **Google Account Settings** pengguna
