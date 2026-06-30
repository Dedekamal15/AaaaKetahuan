package com.example.aaaaketahuan.ui.pengaturan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aaaaketahuan.data.model.KategoriEnum
import com.example.aaaaketahuan.data.model.MetodeBayarEnum
import com.example.aaaaketahuan.data.model.SumberPemasukanEnum
import com.example.aaaaketahuan.ui.theme.ExpenseRed
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel
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
    var showConnectDialog by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }

    // Load current config
    LaunchedEffect(Unit) {
        val config = viewModel.getSpreadsheetConfig()
        spreadsheetId = config.first
        sheetName = config.second
        isConnected = viewModel.isSpreadsheetConnected()
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
                        Icons.Default.ArrowBack,
                        contentDescription = "Kembali"
                    )
                }
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
                            if (isConnected) {
                                Text(
                                    text = spreadsheetId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = "Hubungkan untuk sync otomatis",
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
                            onClick = { showConnectDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                if (isConnected) Icons.Default.CloudSync else Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isConnected) "Ganti" else "Hubungkan")
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
                            enabled = isConnected && !isTestingConnection
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
                            Text("Uji Koneksi")
                        }

                        if (isConnected) {
                            OutlinedButton(
                                onClick = { showDisconnectDialog = true },
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
                                Text("Putuskan")
                            }
                        }
                    }
                }
            }

            // ─── Kategori & Sumber Section ─────────────────────────────
            SettingsSection(
                title = "Kategori & Sumber",
                subtitle = "Atur kategori, metode bayar, dan sumber pemasukan."
            ) {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Label,
                        label = "Kategori",
                        value = "${KategoriEnum.entries.size} kategori",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Fitur ini akan segera hadir")
                            }
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.CreditCard,
                        label = "Metode Pembayaran",
                        value = "${MetodeBayarEnum.entries.size} metode",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Fitur ini akan segera hadir")
                            }
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.Link,
                        label = "Sumber Pemasukan",
                        value = "${SumberPemasukanEnum.entries.size} sumber",
                        showDivider = false,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Fitur ini akan segera hadir")
                            }
                        }
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

            // ─── Otomasi Section ───────────────────────────────────────
            SettingsSection(
                title = "Otomasi",
                subtitle = "Kelola data dan transaksi otomatis."
            ) {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Repeat,
                        label = "Transaksi Berulang",
                        value = "0 aktif",
                        showDivider = false,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Fitur ini akan segera hadir")
                            }
                        }
                    )
                }
            }

            // ─── Notifikasi Section ────────────────────────────────────
            SettingsSection(
                title = "Notifikasi",
                subtitle = "Pengingat catat harian."
            ) {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        label = "Reminder Harian",
                        value = "Nonaktif",
                        showDivider = false,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Fitur ini akan segera hadir")
                            }
                        }
                    )
                }
            }

            // ─── Bantuan Section ───────────────────────────────────────
            SettingsSection(
                title = "Bantuan",
                subtitle = "Pelajari cara pakai atau hubungi pengembang."
            ) {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Help,
                        label = "Pusat Bantuan",
                        value = "",
                        showDivider = false,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Fitur ini akan segera hadir")
                            }
                        }
                    )
                }
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

    // ─── Connect Dialog ───────────────────────────────────────────────
    if (showConnectDialog) {
        var inputId by remember { mutableStateOf(spreadsheetId.ifBlank { "" }) }
        var inputSheet by remember { mutableStateOf(sheetName) }

        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = {
                Text(
                    text = if (isConnected) "Ganti Spreadsheet" else "Hubungkan Spreadsheet"
                )
            },
            text = {
                Column {
                    Text(
                        text = "Masukkan ID Spreadsheet dari URL Google Sheets Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cari ID pada URL: sheets.google.com/spreadsheets/d/**ID_ADA_DISINI**/edit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputId,
                        onValueChange = { inputId = it },
                        label = { Text("Spreadsheet ID") },
                        placeholder = { Text("13aeG7h75xREgc...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputSheet,
                        onValueChange = { inputSheet = it },
                        label = { Text("Nama Sheet") },
                        placeholder = { Text("Sheet1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputId.isNotBlank()) {
                            viewModel.connectSpreadsheet(inputId.trim(), inputSheet.trim().ifBlank { "Sheet1" })
                            spreadsheetId = inputId.trim()
                            sheetName = inputSheet.trim().ifBlank { "Sheet1" }
                            isConnected = true
                            showConnectDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Spreadsheet berhasil dihubungkan!")
                            }
                        }
                    },
                    enabled = inputId.isNotBlank()
                ) {
                    Text("Hubungkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) {
                    Text("Batal")
                }
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
}

// ─── Reusable Components ──────────────────────────────────────────────

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
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
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
