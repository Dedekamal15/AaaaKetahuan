package com.example.aaaaketahuan.data.remote

import com.google.api.services.drive.model.Permission
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper untuk Google Drive Permissions API — mengelola undangan
 * kolaborasi spreadsheet antar user.
 *
 * Membutuhkan OAuth scope:
 * - [com.google.api.services.drive.DriveScopes.DRIVE_FILE] (sharing)
 * - [com.google.api.services.drive.DriveScopes.DRIVE_METADATA_READONLY] (discovery)
 *
 * Seluruh operasi I/O dijalankan di [Dispatchers.IO].
 */
@Singleton
class DriveSharingHelper @Inject constructor() {

    companion object {
        private const val APP_NAME = "AaaaKetahuan"
    }

    // ─── User A: Mengundang User B ────────────────────────────────

    /**
     * Membagikan spreadsheet kepada user lain melalui Google Drive
     * Permissions API.
     *
     * Google Drive secara otomatis mengirimkan email notifikasi ke
     * [userEmail] bahwa spreadsheet telah dibagikan.
     *
     * @param driveService Instance Drive API yang sudah terautentikasi.
     * @param spreadsheetId ID spreadsheet yang akan dibagikan.
     * @param userEmail Alamat email user yang diundang (User B).
     * @return [Result.success] jika berhasil, [Result.failure] jika gagal.
     */
    suspend fun shareSpreadsheet(
        driveService: com.google.api.services.drive.Drive,
        spreadsheetId: String,
        userEmail: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val permission = Permission().apply {
                type = "user"
                role = "writer" // bisa baca & tulis
                emailAddress = userEmail
                // Notifikasi email dikirim otomatis oleh Google Drive
            }
            driveService.permissions().create(spreadsheetId, permission)
                .setFields("id")
                .execute()
            Result.success(Unit)
        } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
            // Scope baru butuh consent — diteruskan ke ViewModel untuk ditampilkan
            Result.failure(DriveAuthException(e.intent))
        } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException) {
            Result.failure(Exception("Izin akses Drive ditolak. Periksa OAuth Client ID di Google Cloud Console."))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Gagal mengundang: ${e.localizedMessage ?: "cek koneksi internet"}"))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal mengundang (${e.javaClass.simpleName})"))
        }
    }

    /**
     * Menghapus akses user lain dari spreadsheet (jika diperlukan).
     */
    suspend fun removePermission(
        driveService: com.google.api.services.drive.Drive,
        spreadsheetId: String,
        permissionId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            driveService.permissions().delete(spreadsheetId, permissionId).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menghapus akses: ${e.localizedMessage}"))
        }
    }

    // ─── User B: Menemukan spreadsheet yang di-share ──────────────

    /**
     * Informasi ringkas tentang spreadsheet yang ditemukan.
     *
     * @property spreadsheetId ID spreadsheet.
     * @property name Nama spreadsheet.
     * @property ownerEmail Email pemilik spreadsheet (User A).
     */
    data class SharedSpreadsheetInfo(
        val spreadsheetId: String,
        val name: String,
        val ownerEmail: String?
    )

    /**
     * Mencari spreadsheet yang dibagikan ke user saat ini.
     *
     * Query: file bertipe spreadsheet yang di-share ke user dan
     * namanya mengandung "AaaaKetahuan" (filter kasar milik app ini).
     *
     * @param driveService Instance Drive API yang sudah terautentikasi.
     * @return Daftar spreadsheet yang ditemukan.
     */
    suspend fun findSharedSpreadsheets(
        driveService: com.google.api.services.drive.Drive
    ): Result<List<SharedSpreadsheetInfo>> = withContext(Dispatchers.IO) {
        try {
            val response = driveService.files().list()
                .setQ(
                    "sharedWithMe = true and " +
                        "mimeType = 'application/vnd.google-apps.spreadsheet' and " +
                        "name contains 'AaaaKetahuan'"
                )
                .setFields("files(id, name, owners(emailAddress))")
                .setPageSize(10)
                .execute()

            val files = response.files ?: emptyList()
            val result = files.map { file ->
                SharedSpreadsheetInfo(
                    spreadsheetId = file.id,
                    name = file.name ?: "Spreadsheet",
                    ownerEmail = file.owners?.firstOrNull()?.emailAddress
                )
            }
            Result.success(result)
        } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
            Result.failure(DriveAuthException(e.intent))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Gagal mencari spreadsheet: ${e.localizedMessage ?: "cek koneksi"}"))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal mencari spreadsheet"))
        }
    }

    /**
     * Mencari spreadsheet "AaaaKetahuan" milik user sendiri (bukan sharedWithMe).
     *
     * Query: file milik user, bertipe spreadsheet, dengan nama persis "AaaaKetahuan".
     * Fallback untuk [TransaksiRepository.restoreSpreadsheetForEmail] ketika
     * data lokal (SharedPreferences) hilang.
     *
     * @param driveService Instance Drive API yang sudah terautentikasi.
     * @return Spreadsheet pertama yang ditemukan, atau null.
     */
    suspend fun findOwnSpreadsheet(
        driveService: com.google.api.services.drive.Drive
    ): Result<SharedSpreadsheetInfo?> = withContext(Dispatchers.IO) {
        try {
            val response = driveService.files().list()
                .setQ(
                    "'me' in owners and " +
                        "mimeType = 'application/vnd.google-apps.spreadsheet' and " +
                        "name = 'AaaaKetahuan'"
                )
                .setFields("files(id, name, owners(emailAddress))")
                .setPageSize(1)
                .execute()

            val file = response.files?.firstOrNull()
            val info = file?.let {
                SharedSpreadsheetInfo(
                    spreadsheetId = it.id,
                    name = it.name ?: "AaaaKetahuan",
                    ownerEmail = it.owners?.firstOrNull()?.emailAddress
                )
            }
            Result.success(info)
        } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
            Result.failure(DriveAuthException(e.intent))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Gagal mencari spreadsheet: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal mencari spreadsheet"))
        }
    }

    /**
     * Mendapatkan daftar permission (user yang memiliki akses) ke spreadsheet.
     * Digunakan untuk menampilkan daftar kolaborator saat ini.
     */
    suspend fun listCollaborators(
        driveService: com.google.api.services.drive.Drive,
        spreadsheetId: String
    ): Result<List<CollaboratorInfo>> = withContext(Dispatchers.IO) {
        try {
            val response = driveService.permissions().list(spreadsheetId)
                .setFields("permissions(id, emailAddress, role, type)")
                .execute()

            val permissions = response.permissions ?: emptyList()
            val collaborators = permissions
                .filter { it.type == "user" }
                .map { perm ->
                    CollaboratorInfo(
                        permissionId = perm.id,
                        email = perm.emailAddress ?: "(unknown)",
                        role = perm.role ?: "reader"
                    )
                }
            Result.success(collaborators)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal memuat daftar kolaborator: ${e.localizedMessage}"))
        }
    }

    /**
     * Informasi seorang kolaborator.
     */
    data class CollaboratorInfo(
        val permissionId: String,
        val email: String,
        val role: String
    )
}

/**
 * Exception khusus untuk kasus OAuth yang butuh consent ulang.
 * ViewModel akan menangkap ini dan meluncurkan [intent] consent screen.
 */
class DriveAuthException(val intent: android.content.Intent) : Exception("PERLU_IZIN_DRIVE")
