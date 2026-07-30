package com.example.aaaaketahuan.ui.pengaturan

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aaaaketahuan.R
import com.example.aaaaketahuan.data.model.KategoriEnum
import com.example.aaaaketahuan.data.model.MetodeBayarEnum
import com.example.aaaaketahuan.data.model.SumberPemasukanEnum
import com.example.aaaaketahuan.ui.theme.ExpenseRed
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel
import com.google.android.gms.common.AccountPicker
import kotlinx.coroutines.launch

@Composable
fun PengaturanScreen(
    viewModel: TransaksiViewModel,
    onBack: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val themeMode by viewModel.themeMode.collectAsState()

    // Spreadsheet state
    var spreadsheetId by remember { mutableStateOf("") }
    var sheetName by remember { mutableStateOf("Sheet1") }
    var isConnected by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var accountEmail by remember { mutableStateOf<String?>(null) }
    var isCreatingSpreadsheet by remember { mutableStateOf(false) }

    // Reminder state
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableStateOf(20) }
    var reminderMinute by remember { mutableStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Category management state
    var showKategoriDialog by remember { mutableStateOf(false) }
    var showMetodeDialog by remember { mutableStateOf(false) }
    var showSumberDialog by remember { mutableStateOf(false) }

    // Collaboration state
    var inviteEmail by remember { mutableStateOf("") }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showInviteDiscoveryDialog by remember { mutableStateOf(false) }

    // Account Picker launcher (pilih akun Google via system dialog)
    val context = LocalContext.current
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val email = result.data!!.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (email != null) {
                viewModel.connectGoogleAccount(email)

                // First try to restore existing spreadsheet for this email
                val hasExisting = viewModel.restoreExistingSpreadsheet(email)
                if (hasExisting) {
                    // Reconnected to existing spreadsheet
                    isCreatingSpreadsheet = false
                    accountEmail = email
                    isConnected = true
                    val config = viewModel.getSpreadsheetConfig()
                    spreadsheetId = config.first
                    sheetName = config.second
                    scope.launch {
                        snackbarHostState.showSnackbar("Spreadsheet berhasil dipulihkan!")
                    }
                    // Check if we should offer to restore data from sheets
                    viewModel.checkAndOfferRestore()
                } else {
                    // Fallback: cari di Drive API terlebih dahulu
                    isCreatingSpreadsheet = true
                    viewModel.restoreExistingSpreadsheetFromDrive(
                        onFound = { id ->
                            isCreatingSpreadsheet = false
                            accountEmail = email
                            isConnected = true
                            spreadsheetId = id
                            scope.launch {
                                snackbarHostState.showSnackbar("Spreadsheet ditemukan di Drive dan dipulihkan!")
                            }
                            viewModel.checkAndOfferRestore()
                        },
                        onNotFound = {
                            // Tidak ditemukan di Drive → buat baru
                            viewModel.createNewSpreadsheet(
                                onSuccess = { id ->
                                    isCreatingSpreadsheet = false
                                    accountEmail = email
                                    isConnected = true
                                    spreadsheetId = id
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
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Gagal: Email tidak ditemukan")
                }
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            // User pressed back or no Google accounts available
            scope.launch {
                snackbarHostState.showSnackbar("Pemilihan akun dibatalkan")
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Gagal memilih akun (${result.resultCode})")
            }
        }
    }

    // Consent screen launcher (untuk menyetujui izin akses Google Sheets)
    val authConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Izin disetujui — cek Drive dulu baru buat baru
            val currentEmail = viewModel.getConnectedAccount()
            isCreatingSpreadsheet = true
            if (currentEmail != null) {
                viewModel.restoreExistingSpreadsheetFromDrive(
                    onFound = { id ->
                        isCreatingSpreadsheet = false
                        accountEmail = currentEmail
                        isConnected = true
                        spreadsheetId = id
                        scope.launch {
                            snackbarHostState.showSnackbar("Spreadsheet ditemukan di Drive!")
                        }
                        viewModel.checkAndOfferRestore()
                    },
                    onNotFound = {
                        viewModel.createNewSpreadsheet(
                            onSuccess = { id ->
                                isCreatingSpreadsheet = false
                                accountEmail = currentEmail
                                isConnected = true
                                spreadsheetId = id
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
            } else {
                viewModel.createNewSpreadsheet(
                    onSuccess = { id ->
                        isCreatingSpreadsheet = false
                        accountEmail = viewModel.getConnectedAccount()
                        isConnected = true
                        spreadsheetId = id
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
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Izin akses Google Sheets ditolak")
            }
        }
    }

    // Notification permission launcher (Android 13+)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            reminderEnabled = true
            viewModel.enableReminder(reminderHour, reminderMinute)
            scope.launch {
                snackbarHostState.showSnackbar("Notifikasi diaktifkan")
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Izin notifikasi ditolak. Aktifkan manual di Pengaturan.")
            }
        }
    }

    // Observe pending auth intent — tampilkan consent screen
    val pendingIntent by viewModel.pendingAuthIntent.collectAsState()
    LaunchedEffect(pendingIntent) {
        val intent = pendingIntent ?: return@LaunchedEffect
        authConsentLauncher.launch(intent)
        viewModel.clearPendingAuthIntent()
    }

    // Observe restore message → show snackbar, then clear
    val restoreMessage by viewModel.restoreMessage.collectAsState()
    LaunchedEffect(restoreMessage) {
        val msg = restoreMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearRestoreMessage()
    }

    // Observe invitation message → show snackbar, then clear
    val invitationMessage by viewModel.invitationMessage.collectAsState()
    LaunchedEffect(invitationMessage) {
        val msg = invitationMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearInvitationMessage()
    }

    // Observe invitation discovery dialog
    val showDiscovery by viewModel.showInviteDiscovery.collectAsState()
    LaunchedEffect(showDiscovery) {
        showInviteDiscoveryDialog = showDiscovery
    }

    // Load current config
    LaunchedEffect(Unit) {
        val config = viewModel.getSpreadsheetConfig()
        spreadsheetId = config.first
        sheetName = config.second
        isConnected = viewModel.isSpreadsheetConnected()
        accountEmail = viewModel.getConnectedAccount()

        reminderEnabled = viewModel.isReminderEnabled()
        reminderHour = viewModel.getReminderHour()
        reminderMinute = viewModel.getReminderMinute()
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {
            // ─── Header with Back Button ───────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali"
                    )
                }
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Spreadsheet & Sync Section ──────────────────────────────
            SettingsSection(
                title = "Spreadsheet & Sync",
                subtitle = "Hubungkan Google Spreadsheet untuk sinkronisasi data."
            ) {
                SettingsCard {
                    // Connection status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isConnected) Icons.Default.Cloud else Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isConnected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isConnected) "Spreadsheet Terhubung"
                                else "Belum Terhubung",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isConnected && accountEmail != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = accountEmail!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else if (!isConnected) {
                                Text(
                                    text = "Hubungkan akun Google untuk sync otomatis",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = AccountPicker.newChooseAccountIntent(
                                    null, null, arrayOf("com.google"),
                                    false, null, null, null, null
                                )
                                accountPickerLauncher.launch(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isCreatingSpreadsheet
                        ) {
                            if (isCreatingSpreadsheet) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    if (isConnected) Icons.Default.CloudSync else Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCreatingSpreadsheet) "Membuat..." else if (isConnected) "Ganti Akun" else "Hubungkan",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                isTestingConnection = true
                                viewModel.testSpreadsheetConnection { success, message ->
                                    isTestingConnection = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (success) "Koneksi berhasil!"
                                            else "Gagal: ${message ?: "Tidak diketahui"}"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = isConnected && !isTestingConnection && !isCreatingSpreadsheet
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Uji Koneksi",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (isConnected) {
                            OutlinedButton(
                                onClick = { showDisconnectDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ExpenseRed
                                )
                            ) {
                                Icon(
                                    Icons.Default.LinkOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Putuskan",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                } // PENTING: Kurung penutup SettingsCard
            } // PENTING: Kurung penutup SettingsSection

            // ─── Kategori & Sumber Section ─────────────────────────────
            SettingsSection(
                title = "Kategori & Sumber",
                subtitle = "Atur kategori, metode bayar, dan sumber pemasukan."
            ) {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.Label,
                        label = "Kategori",
                        value = "${viewModel.getEffectiveKategori().size} kategori",
                        onClick = { showKategoriDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.CreditCard,
                        label = "Metode Pembayaran",
                        value = "${viewModel.getEffectiveMetodeBayar().size} metode",
                        onClick = { showMetodeDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Link,
                        label = "Sumber Pemasukan",
                        value = "${viewModel.getEffectiveSumberPemasukan().size} sumber",
                        showDivider = false,
                        onClick = { showSumberDialog = true }
                    )
                }
            }

            // ─── Kolaborasi Section ─────────────────────────────────────
            SettingsSection(
                title = "Kolaborasi",
                subtitle = "Undang pasangan/keluarga untuk keluar bersama."
            ) {
                SettingsCard {
                    // Invite by email
                    SettingsItem(
                        icon = Icons.Default.Person,
                        label = "Undang via Email",
                        value = if (isConnected) "Kirim undangan" else "Hubungkan dulu",
                        onClick = {
                            if (isConnected) {
                                inviteEmail = ""
                                showInviteDialog = true
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Hubungkan Google Spreadsheet dulu")
                                }
                            }
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    // Check for invitations
                    SettingsItem(
                        icon = Icons.Default.CloudSync,
                        label = "Cek Undangan Masuk",
                        value = "",
                        showDivider = false,
                        onClick = { viewModel.checkForInvitations() }
                    )
                }
            }

            // ─── Tampilan Section ──────────────────────────────────────
            SettingsSection(
                title = "Tampilan",
                subtitle = "Pilih tema terang, gelap, atau ikuti sistem."
            ) {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tema Aplikasi",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = when (themeMode) {
                                "light" -> "Terang"
                                "dark" -> "Gelap"
                                else -> "Sistem"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeButton(
                            label = "Terang",
                            icon = Icons.Default.LightMode,
                            selected = themeMode == "light",
                            onClick = { viewModel.setThemeMode("light") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeButton(
                            label = "Gelap",
                            icon = Icons.Default.DarkMode,
                            selected = themeMode == "dark",
                            onClick = { viewModel.setThemeMode("dark") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeButton(
                            label = "Sistem",
                            icon = Icons.Default.Computer,
                            selected = themeMode == "system",
                            onClick = { viewModel.setThemeMode("system") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ─── Notifikasi Section ────────────────────────────────────
            SettingsSection(
                title = "Notifikasi",
                subtitle = "Pengingat untuk mencatat transaksi harian."
            ) {
                SettingsCard {
                    // Reminder toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (reminderEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reminder Harian",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (reminderEnabled) "Aktif • ${String.format("%02d:%02d", reminderHour, reminderMinute)}"
                                else "Nonaktif",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (reminderEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            reminderEnabled = true
                                            viewModel.enableReminder(reminderHour, reminderMinute)
                                        } else {
                                            notifPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        }
                                    } else {
                                        reminderEnabled = true
                                        viewModel.enableReminder(reminderHour, reminderMinute)
                                    }
                                } else {
                                    reminderEnabled = false
                                    viewModel.disableReminder()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                checkedThumbColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Time picker button (only show when enabled)
                    if (reminderEnabled) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Waktu Pengingat",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format("%02d:%02d", reminderHour, reminderMinute),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // ─── Support Developer Section ─────────────────────────────
            SettingsSection(
                title = "Dukung Pengembang",
                subtitle = "Dukung Pengembang untuk bisa merilis Aplikasi ini ke PlayStore"
            ) {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Favorite,
                        label = "Support Developer",
                        value = "",
                        showDivider = false,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Mengalihkan ke browser...")
                            }
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://saweria.co/Dedekamal")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // ─── Logout Section ─────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ExpenseRed
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AaaaKetahuan v1.0.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // ─── Restore Data from Spreadsheet Dialog ─────────────────────────
    val showRestore by viewModel.showRestoreDialog.collectAsState()
    val restoreCount by viewModel.restoreCount.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()

    if (showRestore) {
        AlertDialog(
            onDismissRequest = { if (!isRestoring) viewModel.dismissRestore() },
            icon = { Icon(Icons.Default.CloudSync, contentDescription = null) },
            title = { Text("Pulihkan Data?") },
            text = {
                Column {
                    Text(
                        text = "Ditemukan $restoreCount transaksi di spreadsheet Anda. " +
                                "Apakah ingin memulihkannya ke aplikasi?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Data dari semua sheet (termasuk periode sebelumnya) akan " +
                                "disalin ke penyimpanan lokal aplikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isRestoring) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Memulihkan data...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmRestore() },
                    enabled = !isRestoring
                ) { Text("Ya, Pulihkan") }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissRestore() },
                    enabled = !isRestoring
                ) { Text("Nanti") }
            }
        )
    }

    // ─── Creating Spreadsheet Dialog ──────────────────────────────────
    if (isCreatingSpreadsheet) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Menyiapkan Spreadsheet") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Membuat spreadsheet di Google Drive Anda...")
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // ─── Time Picker Dialog ────────────────────────────────────────
    if (showTimePicker) {
        var tmpHour by remember { mutableStateOf(reminderHour.toString()) }
        var tmpMinute by remember { mutableStateOf(reminderMinute.toString()) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Atur Waktu Pengingat") },
            text = {
                Column {
                    Text(
                        text = "Atur jam dan menit untuk pengingat harian.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = tmpHour,
                            onValueChange = { tmpHour = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Jam") },
                            placeholder = { Text("20") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.headlineMedium
                        )
                        OutlinedTextField(
                            value = tmpMinute,
                            onValueChange = { tmpMinute = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Menit") },
                            placeholder = { Text("00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Format 24 jam (contoh: 20:00 = jam 8 malam)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = tmpHour.toIntOrNull()?.coerceIn(0, 23) ?: 20
                    val m = tmpMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    reminderHour = h
                    reminderMinute = m
                    viewModel.updateReminderTime(h, m)
                    showTimePicker = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
            }
        )
    }

    // ─── Disconnect Confirmation Dialog ─────────────────────────────
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Putuskan Spreadsheet") },
            text = {
                Text("Data transaksi tetap aman tersimpan secara lokal. " +
                        "Sinkronisasi otomatis ke spreadsheet akan berhenti.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.disconnectSpreadsheet()
                        spreadsheetId = ""
                        sheetName = "Sheet1"
                        accountEmail = null
                        isConnected = false
                        showDisconnectDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Spreadsheet diputuskan")
                        }
                    }
                ) {
                    Text("Putuskan", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // ─── Logout Confirmation Dialog ──────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = {
                Text("Anda akan keluar dari akun Google yang terhubung. " +
                        "Data transaksi tetap aman tersimpan secara lokal.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    }
                ) {
                    Text("Logout", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // ─── Invite User Dialog (User A) ─────────────────────────────
    if (showInviteDialog) {
        val isInviting by viewModel.isInviting.collectAsState()
        val collaborators by viewModel.collaborators.collectAsState()

        // Muat daftar kolaborator saat dialog dibuka
        LaunchedEffect(showInviteDialog) {
            viewModel.loadCollaborators()
        }

        AlertDialog(
            onDismissRequest = { if (!isInviting) showInviteDialog = false },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            title = { Text("Undang Kolaborator") },
            text = {
                Column {
                    Text(
                        text = "Masukkan email Google pasangan/keluarga untuk bergabung " +
                                "ke spreadsheet yang sama.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email") },
                        placeholder = { Text("nama@gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isInviting
                    )
                    if (isInviting) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Memproses...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    // Daftar kolaborator
                    if (collaborators.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Kolaborator saat ini:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        collaborators.forEach { col ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${col.email} (${col.role})",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeCollaborator(col.permissionId) },
                                    enabled = !isInviting,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inviteEmail.isNotBlank()) {
                            viewModel.inviteUser(inviteEmail.trim())
                        }
                    },
                    enabled = inviteEmail.isNotBlank() && !isInviting
                ) { Text("Kirim Undangan") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showInviteDialog = false },
                    enabled = !isInviting
                ) { Text("Tutup") }
            }
        )
    }

    // ─── Invitation Discovery Dialog (User B) ────────────────────
    if (showInviteDiscoveryDialog) {
        val foundSpreadsheets by viewModel.foundSharedSpreadsheets.collectAsState()
        val isChecking by viewModel.isCheckingInvitations.collectAsState()

        AlertDialog(
            onDismissRequest = { if (!isChecking) showInviteDiscoveryDialog = false },
            icon = { Icon(Icons.Default.CloudSync, contentDescription = null) },
            title = { Text("Undangan Ditemukan!") },
            text = {
                Column {
                    if (isChecking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Mencari...")
                        }
                    } else if (foundSpreadsheets.isEmpty()) {
                        Text("Tidak ada undangan aktif saat ini.")
                    } else {
                        Text(
                            text = "Spreadsheet AaaaKetahuan berikut dibagikan ke akun Anda. " +
                                    "Pilih untuk mengganti data lokal Anda.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        foundSpreadsheets.forEach { sheet ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                onClick = { viewModel.acceptInvitation(sheet.spreadsheetId) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            sheet.name,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            sheet.ownerEmail ?: "Tidak diketahui",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.rejectInvitation()
                        showInviteDiscoveryDialog = false
                    },
                    enabled = !isChecking
                ) { Text("Tutup") }
            }
        )
    }

    // ─── Category Management Dialogs ─────────────────────────────
    if (showKategoriDialog) {
        KategoriManageDialog(
            viewModel = viewModel,
            onDismiss = {
                showKategoriDialog = false
            }
        )
    }
    if (showMetodeDialog) {
        SimpleListManageDialog(
            title = "Metode Pembayaran",
            currentItems = viewModel.getEffectiveMetodeBayar(),
            onSave = { viewModel.saveCustomMetodeBayar(it) },
            onDismiss = { showMetodeDialog = false }
        )
    }
    if (showSumberDialog) {
        SimpleListManageDialog(
            title = "Sumber Pemasukan",
            currentItems = viewModel.getEffectiveSumberPemasukan(),
            onSave = { viewModel.saveCustomSumberPemasukan(it) },
            onDismiss = { showSumberDialog = false }
        )
    }
}

// ─── Kategori Management Dialog ──────────────────────────────────
@Composable
private fun KategoriManageDialog(
    viewModel: TransaksiViewModel,
    onDismiss: () -> Unit
) {
    val customItems = remember { viewModel.getCustomKategori().toMutableList() }
    val hiddenItems = remember { viewModel.getHiddenKategori().toMutableList() }
    val enumItems = remember { KategoriEnum.entries.map { it.label } }
    var newItemText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Kategori") },
        text = {
            Column {
                Text(
                    "Centang untuk menampilkan, hapus untuk custom.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Add new item
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        label = { Text("Kategori baru") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val text = newItemText.trim()
                            if (text.isNotEmpty() && text !in enumItems && text !in customItems) {
                                customItems.add(text)
                                newItemText = ""
                            }
                        },
                        enabled = newItemText.isNotBlank()
                    ) { Text("Tambah") }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Divider
                HorizontalDivider()

                // Scrollable list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    // Enum items
                    item {
                        Text(
                            "Default",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(enumItems) { item ->
                        val isHidden = item in hiddenItems
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isHidden) hiddenItems.remove(item)
                                    else hiddenItems.add(item)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isHidden) Icons.Outlined.CheckBoxOutlineBlank
                                else Icons.Filled.CheckBox,
                                contentDescription = null,
                                tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (customItems.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Kustom",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        itemsIndexed(customItems) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckBox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    item,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { customItems.removeAt(index) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.saveCustomKategori(customItems.toList())
                viewModel.saveHiddenKategori(hiddenItems.toList())
                onDismiss()
            }) { Text("Selesai") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

// ─── Simple List Management Dialog ───────────────────────────────
@Composable
private fun SimpleListManageDialog(
    title: String,
    currentItems: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val enumSize = remember {
        when (title) {
            "Metode Pembayaran" -> MetodeBayarEnum.entries.size
            "Sumber Pemasukan" -> SumberPemasukanEnum.entries.size
            else -> 0
        }
    }
    val customItems = remember { currentItems.drop(enumSize).toMutableList() }
    val fixedItems = remember { currentItems.take(enumSize) }
    var newItemText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur $title") },
        text = {
            Column {
                Text(
                    "Tambah atau hapus item kustom.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        label = { Text("Tambah baru") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val text = newItemText.trim()
                            if (text.isNotEmpty() && text !in fixedItems && text !in customItems) {
                                customItems.add(text)
                                newItemText = ""
                            }
                        },
                        enabled = newItemText.isNotBlank()
                    ) { Text("Tambah") }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(fixedItems) { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckBox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (customItems.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Kustom",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        itemsIndexed(customItems) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckBox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(item, modifier = Modifier.weight(1f))
                                TextButton(onClick = { customItems.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, "Hapus", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(fixedItems + customItems)
                onDismiss()
            }) { Text("Selesai") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    value: String,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
    }
    if (showDivider) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun ThemeButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.secondary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}