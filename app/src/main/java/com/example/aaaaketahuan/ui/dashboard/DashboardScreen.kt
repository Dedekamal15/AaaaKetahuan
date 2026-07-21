package com.example.aaaaketahuan.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aaaaketahuan.R
import com.example.aaaaketahuan.ui.components.TransaksiCard
import com.example.aaaaketahuan.ui.theme.ExpenseRed
import com.example.aaaaketahuan.util.toRupiah
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Indonesian month names displayed in the month picker and labels.
 * Pre-computed once at class-load time to avoid recomputation on every recomposition.
 */
private val BULAN_NAMES: List<String> by lazy {
    (1..12).map { Month.of(it).getDisplayName(TextStyle.FULL, Locale("id", "ID")) }
}

@Composable
fun DashboardScreen(
    viewModel: TransaksiViewModel,
    onNavigateToRiwayat: () -> Unit = {},
    onNavigateToGrafik: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToInput: () -> Unit = {},
    onNavigateToPengaturan: () -> Unit = {},
    onNavigateToForecast: () -> Unit = {}
) {
    val transaksiList by viewModel.transaksiList.collectAsState()
    val totalMasuk by viewModel.totalMasuk.collectAsState()
    val totalKeluar by viewModel.totalKeluar.collectAsState()
    val saldo by viewModel.saldo.collectAsState()
    val filterBulan by viewModel.filterBulan.collectAsState()
    val filterTahun by viewModel.filterTahun.collectAsState()
    val userDisplayName by viewModel.userDisplayName.collectAsState()

    // Derived state: only recalculated when transaksiList reference changes
    val recentTransaksi = remember(transaksiList) {
        transaksiList.sortedByDescending { it.tanggal }.take(5)
    }

    // Derived state: only recalculated when month/year filter changes
    val monthLabel = remember(filterBulan, filterTahun) {
        "${BULAN_NAMES[filterBulan - 1]} $filterTahun"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item { DashboardHeader(onNavigateToPengaturan, onNavigateToExport) }

            item {
                GreetingSection(
                    userDisplayName = userDisplayName,
                    filterBulan = filterBulan,
                    filterTahun = filterTahun,
                    onBulanChange = { bulan -> viewModel.onFilterBulanChange(bulan, filterTahun) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                MetricsSection(
                    saldo = saldo,
                    totalMasuk = totalMasuk,
                    totalKeluar = totalKeluar,
                    monthLabel = monthLabel
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item { RecentTransactionsHeader(onNavigateToRiwayat) }

            if (recentTransaksi.isEmpty()) {
                item { EmptyTransactionState() }
            } else {
                items(recentTransaksi, key = { it.id }) { transaksi ->
                    TransaksiCard(transaksi = transaksi)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                AnalyticsTeaser(onNavigateToGrafik)
                Spacer(modifier = Modifier.height(12.dp))
                ForecastingTeaser(onNavigateToForecast)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        FloatingActionButton(
            onClick = onNavigateToInput,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 88.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Tambah Transaksi",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ─── Extracted Section Composables ──────────────────────────────────────────

@Composable
private fun DashboardHeader(
    onNavigateToPengaturan: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
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
        Row {
            IconButton(onClick = onNavigateToExport) {
                Icon(Icons.Default.Share, contentDescription = "Export / Import")
            }
            IconButton(onClick = onNavigateToPengaturan) {
                Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
            }
        }
    }
}

@Composable
private fun GreetingSection(
    userDisplayName: String?,
    filterBulan: Int,
    filterTahun: Int,
    onBulanChange: (Int) -> Unit
) {
    val displayName = userDisplayName ?: "Pengguna"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Halo, $displayName",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Berikut ringkasan keuangan Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MonthSelector(
            filterBulan = filterBulan,
            filterTahun = filterTahun,
            onBulanChange = onBulanChange
        )
    }
}

@Composable
private fun MonthSelector(
    filterBulan: Int,
    filterTahun: Int,
    onBulanChange: (Int) -> Unit
) {
    var showMonthPicker by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier.clickable { showMonthPicker = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${BULAN_NAMES[filterBulan - 1]} $filterTahun",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(
            expanded = showMonthPicker,
            onDismissRequest = { showMonthPicker = false }
        ) {
            BULAN_NAMES.forEachIndexed { index, name ->
                DropdownMenuItem(
                    text = { Text("$name $filterTahun") },
                    onClick = {
                        onBulanChange(index + 1)
                        showMonthPicker = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MetricsSection(
    saldo: Double,
    totalMasuk: Double,
    totalKeluar: Double,
    monthLabel: String
) {
    BalanceCard(saldo = saldo, monthLabel = monthLabel)
    Spacer(modifier = Modifier.height(12.dp))
    MiniMetricsRow(totalMasuk = totalMasuk, totalKeluar = totalKeluar)
}

@Composable
private fun BalanceCard(saldo: Double, monthLabel: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Total Saldo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = saldo.toRupiah(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Periode $monthLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
        }
    }
    }
}

}

@Composable
private fun MiniMetricsRow(totalMasuk: Double, totalKeluar: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ArrowDownward,
            label = "Total Masuk",
            amount = totalMasuk,
            contentColor = MaterialTheme.colorScheme.primary
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ArrowUpward,
            label = "Total Keluar",
            amount = totalKeluar,
            contentColor = ExpenseRed
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    amount: Double,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount.toRupiah(),
                style = MaterialTheme.typography.titleLarge,
                color = contentColor
            )
        }
    }
}

@Composable
private fun RecentTransactionsHeader(onNavigateToRiwayat: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "5 Transaksi Terkini",
            style = MaterialTheme.typography.titleLarge
        )
        TextButton(onClick = onNavigateToRiwayat) {
            Text(
                text = "Lihat Semua",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun EmptyTransactionState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(64.dp).alpha(0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Belum ada transaksi",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Mulai catat pemasukan atau pengeluaran Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ForecastingTeaser(onNavigateToForecast: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onNavigateToForecast),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "Lihat Prediksi Keuangan",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Proyeksikan pengeluaran dan saldo Anda dengan prediksi cerdas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsTeaser(onNavigateToGrafik: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onNavigateToGrafik),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "Lihat Analisis Bulanan",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Pahami kemana uang Anda pergi dengan grafik interaktif.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}
