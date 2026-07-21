# Google OAuth Verification Guide — AaaaKetahuan

## 📋 Sebelum Submit Verifikasi

### 1. Upload Privacy Policy & Terms of Service ke Hosting

File sudah disediakan:
- `privacy-policy.html`
- `terms-of-service.html`

Upload ke hosting Gratis:
- [GitHub Pages](https://pages.github.com/) — buat repo publik, push file, akses via `https://username.github.io/repo/privacy-policy.html`
- [Netlify](https://www.netlify.com/) — drag & drop folder
- [Vercel](https://vercel.com/) — drag & drop folder

> Setelah diupload, copy URL-nya. Contoh: `https://username.github.io/aaaaketahuan/privacy-policy.html`

### 2. Pastikan di OAuth Consent Screen

- [x] Scope `.../auth/spreadsheets` sudah ditambahkan
- [x] Privacy Policy URL sudah diisi
- [x] Terms of Service URL sudah diisi
- [x] Email support sudah diisi

### 3. Siapkan Video Demo

Rekam layar HP selama **±1-2 menit** — tidak perlu diedit, cukup satu take.
Gunakan [Screen Recorder bawaan HP](https://support.google.com/android/answer/9075928).

---

## 🎬 Video Demo — Script & Adegan

### Durasi: ± 90 detik

| Waktu | Adegan | Aksi di Layar | Narasi (Bahasa Indonesia) |
|-------|--------|---------------|---------------------------|
| **0:00-0:10** | Buka aplikasi | Tap ikon AaaaKetahuan | *"Ini adalah AaaaKetahuan, aplikasi pencatatan keuangan pribadi. Saya akan mendemonstrasikan bagaimana aplikasi ini menggunakan Google Sheets API."* |
| **0:10-0:20** | Menuju Pengaturan | Tap ikon ⚙️ di pojok kanan atas Dashboard | *"Pertama, buka menu Pengaturan."* |
| **0:20-0:35** | Klik Hubungkan | Scroll ke bagian "Spreadsheet & Sync", tap tombol **Hubungkan** | *"Di bagian Spreadsheet & Sync, tap tombol Hubungkan untuk memulai."* |
| **0:35-0:45** | Pilih akun Google | Sistem menampilkan dialog pilih akun Google → pilih akun | *"Pilih akun Google yang ingin digunakan."* |
| **0:45-0:55** | Consent screen muncul | Halaman izin Google muncul: "AaaaKetahuan ingin melihat, mengedit, membuat, dan menghapus spreadsheet Anda" → tap **Izinkan** | *"Google akan menampilkan halaman izin. Aplikasi meminta akses untuk membuat spreadsheet baru di Drive pengguna."* |
| **0:55-1:05** | Spreadsheet terbuat | Muncul snackbar "Spreadsheet berhasil dibuat!" Status berubah jadi "Spreadsheet Terhubung" dengan email terdaftar | *"Setelah diizinkan, aplikasi otomatis membuat spreadsheet baru di Google Drive pengguna. Status berubah menjadi terhubung."* |
| **1:05-1:20** | Input transaksi | Kembali ke Dashboard → tap ikon ➕ (Input) → isi transaksi (contoh: Makan Siang Rp25.000, kategori Makanan) → tap Simpan | *"Sekarang saya akan menambahkan transaksi contoh. Isi nominal, kategori, lalu simpan."* |
| **1:20-1:30** | Verifikasi data di Google Sheets | Buka Google Sheets app atau browser → buka spreadsheet "AaaaKetahuan" → tunjukkan data transaksi sudah ada di sheet | *"Data transaksi otomatis tersinkronisasi ke Google Spreadsheet. Kita bisa melihatnya langsung di Google Sheets."* |
| **1:30-1:40** | Putuskan koneksi | Kembali ke Pengaturan → tap **Putuskan** → konfirmasi dialog | *"Pengguna bisa memutuskan koneksi kapan saja dari menu Pengaturan."* |

> **Catatan penting untuk video:**
> - Pastikan **akun Google penguji** (yang dipilih di video) sudah didaftarkan sebagai **Test User** di OAuth Consent Screen
> - Boleh pakai **Bahasa Indonesia** atau **English** — terserah yang lebih nyaman
> - Fokus ke alur: **Hubungkan → Izin → Spreadsheet terbuat → Data tersync**
> - Tidak perlu diedit, cukup record sekali jalan

---

## 📝 Isian Form Verifikasi Google

| Field | Isi |
|-------|-----|
| **Application name** | AaaaKetahuan |
| **Homepage URL** | (opsional, bisa dikosongkan atau link GitHub) |
| **Privacy Policy URL** | `https://[hosting-mu]/privacy-policy.html` |
| **Terms of Service URL** | `https://[hosting-mu]/terms-of-service.html` |
| **Authorized domains** | `[hosting-mu].com` (domain tempat privacy policy dihosting) |
| **Scope** | `.../auth/spreadsheets` (Google Sheets API) |
| **Short description** | *AaaaKetahuan is a personal finance tracking app for Android that helps users record income and expenses, view reports, and optionally sync data to Google Sheets for backup.* |
| **Long description** | *AaaaKetahuan allows users to track their daily financial transactions with features including: transaction recording, expense/report charts, transaction history with monthly filters, CSV export/import for backup, and optional Google Sheets synchronization for cloud backup.* |
| **Video URL** | Upload video ke Google Drive / YouTube Unlisted, paste link-nya |
| **How your app uses data** | *The app only accesses Google Sheets to create a new spreadsheet in the user's own Google Drive (with user's explicit permission) and sync transaction data to that spreadsheet. The app does NOT read, modify, or access any other spreadsheets or Google services.* |
| **Is your app using Google Sheets API to read user data for training AI/ML?** | No |

---

## 🚀 Step-by-Step Submit

1. Buka [Google Cloud Console → OAuth Consent Screen](https://console.cloud.google.com/apis/credentials/consent)
2. Ubah mode dari **Testing** → **In Production**
3. Isi semua field yang diminta (Privacy Policy URL, Terms URL, dll)
4. Upload video demo
5. Klik **Submit for Verification**
6. Tunggu email dari Google (biasanya 2-7 hari kerja)

### ℹ️ Tips dari Developer Lain

> **Scope `spreadsheets` termasuk "Sensitive" bukan "Restricted"** — proses verifikasinya lebih cepat daripada Restricted scope (biasanya 2-5 hari).
>
> Pastikan di video kamu **hanya menunjukkan operasi yang relevan dengan scope yang diminta** (membuat spreadsheet, mengisi data). Jangan ada aksi lain yang tidak relevan.
>
> Jika Google menolak pertama kali, biasanya mereka kasih catatan perbaikan. Baca komentarnya, perbaiki, submit ulang.
