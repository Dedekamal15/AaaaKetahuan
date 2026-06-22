package com.example.aaaaketahuan.ui.pengaturan

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aaaaketahuan.ui.theme.ExpenseRed
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel

@Composable
fun PengaturanScreen(
    viewModel: TransaksiViewModel,
    onBack: () -> Unit = {}
) {
    var selectedTheme by remember { mutableStateOf("terang") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AaaaKetahuan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Kategori & Sumber
        SettingsSection(
            title = "Kategori & Sumber",
            subtitle = "Disinkron juga ke konfigurasi Sheet kamu."
        ) {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Label,
                    label = "Kategori",
                    value = "10 kategori"
                )
                SettingsItem(
                    icon = Icons.Default.CreditCard,
                    label = "Metode Pembayaran",
                    value = "5 metode"
                )
                SettingsItem(
                    icon = Icons.Default.Link,
                    label = "Sumber Pemasukan",
                    value = "2 sumber",
                    showDivider = false
                )
            }
        }

        // Section: Spreadsheet & Sync
        SettingsSection(
            title = "Spreadsheet & Sync",
            subtitle = "Setiap catatan langsung disimpan ke Sheet kamu via OAuth."
        ) {
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Spreadsheet Terhubung",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AaaaKetahuan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = { /* disconnect */ }) {
                        Text(
                            text = "Putuskan",
                            color = ExpenseRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section: Tampilan
        SettingsSection(
            title = "Tampilan",
            subtitle = "Mode terang atau gelap."
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
                        text = "Tema",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
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
                        selected = selectedTheme == "terang",
                        onClick = { selectedTheme = "terang" },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeButton(
                        label = "Gelap",
                        icon = Icons.Default.DarkMode,
                        selected = selectedTheme == "gelap",
                        onClick = { selectedTheme = "gelap" },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeButton(
                        label = "Sistem",
                        icon = Icons.Default.Computer,
                        selected = selectedTheme == "sistem",
                        onClick = { selectedTheme = "sistem" },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Section: Otomasi
        SettingsSection(
            title = "Otomasi",
            subtitle = "Kelola data dan transaksi otomatis."
        ) {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Repeat,
                    label = "Transaksi Berulang",
                    value = "0 aktif",
                    showDivider = false
                )
            }
        }

        // Section: Notifikasi
        SettingsSection(
            title = "Notifikasi",
            subtitle = "Pengingat catat harian."
        ) {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    label = "Reminder Harian",
                    value = "Nonaktif",
                    showDivider = false
                )
            }
        }

        // Section: Bantuan
        SettingsSection(
            title = "Bantuan",
            subtitle = "Butuh bantuan? Hubungi kami."
        ) {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Help,
                    label = "Pusat Bantuan",
                    value = "",
                    showDivider = false
                )
            }
        }
    }
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun ThemeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.secondary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
