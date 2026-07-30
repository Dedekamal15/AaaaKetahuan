package com.example.aaaaketahuan.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aaaaketahuan.R
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.api.services.sheets.v4.SheetsScopes
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(viewModel: TransaksiViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isCreatingSpreadsheet by remember { mutableStateOf(false) }
    var currentEmail by remember { mutableStateOf<String?>(null) }
    val pendingIntent by viewModel.pendingAuthIntent.collectAsState()

    // Google Sign-In options — request email + Sheets access
    val signInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(SheetsScopes.SPREADSHEETS))
            .build()
    }

    val googleSignInClient = remember { GoogleSignIn.getClient(context, signInOptions) }

    fun handleSpreadsheetSetup(
        email: String,
        snackbarHostState: SnackbarHostState,
        scope: kotlinx.coroutines.CoroutineScope,
        sheetCreated: () -> Unit = {}
    ) {
        // 1. Coba lokal (SharedPreferences)
        val hasExisting = viewModel.restoreExistingSpreadsheet(email)
        if (hasExisting) {
            scope.launch {
                snackbarHostState.showSnackbar("Spreadsheet yang sudah ada ditemukan dan dipulihkan!")
            }
            sheetCreated()
            return
        }

        // 2. Fallback: cari di Drive API
        isCreatingSpreadsheet = true
        viewModel.restoreExistingSpreadsheetFromDrive(
            onFound = { _ ->
                isCreatingSpreadsheet = false
                sheetCreated()
                scope.launch {
                    snackbarHostState.showSnackbar("Spreadsheet ditemukan di Drive dan dipulihkan!")
                }
            },
            onNotFound = {
                // 3. Tidak ditemukan di Drive → buat baru
                viewModel.createNewSpreadsheet(
                    onSuccess = { _ ->
                        isCreatingSpreadsheet = false
                        sheetCreated()
                        scope.launch {
                            snackbarHostState.showSnackbar("Spreadsheet baru berhasil dibuat!")
                        }
                    },
                    onError = { msg ->
                        isCreatingSpreadsheet = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Gagal: $msg")
                        }
                    }
                )
            }
        )
    }

    // Launcher for Google Sign-In intent
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Always try to parse the intent — Google Sign-In status is embedded
        // in the intent data, NOT in the activity resultCode.
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account.email
            if (email != null) {
                currentEmail = email
                viewModel.connectGoogleAccount(email)
                handleSpreadsheetSetup(email, snackbarHostState, scope)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Gagal: Email tidak ditemukan")
                }
            }
        } catch (e: ApiException) {
            scope.launch {
                when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
                        snackbarHostState.showSnackbar("Pemilihan akun dibatalkan")
                    }
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> {
                        snackbarHostState.showSnackbar("Proses masuk masih berlangsung...")
                    }
                    GoogleSignInStatusCodes.SIGN_IN_FAILED,
                    CommonStatusCodes.DEVELOPER_ERROR -> {
                        snackbarHostState.showSnackbar(
                            "Gagal masuk: pastikan OAuth Client ID sudah dikonfigurasi " +
                            "dengan SHA-1 yang benar di Google Cloud Console"
                        )
                    }
                    else -> {
                        snackbarHostState.showSnackbar(
                            "Gagal masuk (${e.statusCode}): ${e.localizedMessage ?: "Terjadi kesalahan"}"
                        )
                    }
                }
            }
        }
    }

    // Consent screen launcher (untuk menyetujui izin akses Google Sheets jika perlu)
    val authConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val email = currentEmail ?: run {
                // Fallback: retry create if email not available
                isCreatingSpreadsheet = true
                viewModel.createNewSpreadsheet(
                    onSuccess = { _ ->
                        isCreatingSpreadsheet = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Spreadsheet berhasil dibuat!")
                        }
                    },
                    onError = { msg ->
                        isCreatingSpreadsheet = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Gagal: $msg")
                        }
                    }
                )
                return@rememberLauncherForActivityResult
            }
            handleSpreadsheetSetup(email, snackbarHostState, scope)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Izin akses Google Sheets ditolak")
            }
        }
    }

    // Silent sign-in: jika akun sudah pernah login sebelumnya, langsung
    // sambungkan tanpa perlu menampilkan dialog pilih akun.
    LaunchedEffect(Unit) {
        if (viewModel.getConnectedAccount() != null) return@LaunchedEffect
        try {
            val silentAccount = withContext(Dispatchers.IO) {
                com.google.android.gms.tasks.Tasks.await(
                    googleSignInClient.silentSignIn()
                )
            }
            val email = silentAccount?.email
            if (email != null) {
                currentEmail = email
                viewModel.connectGoogleAccount(email)
                handleSpreadsheetSetup(email, snackbarHostState, scope)
            }
        } catch (_: Exception) {
            // Silent sign-in gagal — user harus login manual via tombol
        }
    }

    // Observe pending auth intent — tampilkan consent screen
    LaunchedEffect(pendingIntent) {
        val intent = pendingIntent ?: return@LaunchedEffect
        authConsentLauncher.launch(intent)
        viewModel.clearPendingAuthIntent()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App name
            Text(
                text = "AaaaKetahuan",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Catat dan pantau keuangan Anda",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sinkronkan otomatis ke Google Spreadsheet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Google Sign-In button (branded style)
            OutlinedButton(
                onClick = {
                    signInLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isCreatingSpreadsheet,
                border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F1F1F)
                )
            ) {
                if (isCreatingSpreadsheet) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // Google "G" icon
                    Text(
                        text = "G",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4) // Google Blue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Masuk dengan Google",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isCreatingSpreadsheet) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Membuat spreadsheet...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
