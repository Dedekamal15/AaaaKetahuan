package com.example.aaaaketahuan.ui.riwayat

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aaaaketahuan.data.model.KategoriEnum
import com.example.aaaaketahuan.ui.components.TransaksiCard
import com.example.aaaaketahuan.ui.theme.ExpenseRed
import com.example.aaaaketahuan.util.toRupiah
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    viewModel: TransaksiViewModel,
    onEditTransaksi: (String) -> Unit = {}
) {
    val transaksiList by viewModel.transaksiList.collectAsState()
    val filterBulan by viewModel.filterBulan.collectAsState()
    val filterTahun by viewModel.filterTahun.collectAsState()

    var showMonthPicker by remember { mutableStateOf(false) }
    var selectedKategoriFilter by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    val bulanNames = (1..12).map {
        java.time.Month.of(it).getDisplayName(TextStyle.FULL, Locale("id", "ID"))
    }
    val monthLabel = "${bulanNames[filterBulan - 1]} $filterTahun"

    val filteredList = if (selectedKategoriFilter != null) {
        transaksiList.filter { it.kategori == selectedKategoriFilter }
    } else {
        transaksiList
    }.sortedByDescending { it.tanggal }

    // Group transactions by date
    val groupedByDate = filteredList.groupBy { it.tanggal }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top AppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {}) {
                Icon(Icons.Default.Search, contentDescription = "Cari")
            }
        }

        // Filters
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Card(
                        modifier = Modifier.clickable { showMonthPicker = true },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = monthLabel, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showMonthPicker,
                        onDismissRequest = { showMonthPicker = false }
                    ) {
                        bulanNames.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text("$name $filterTahun") },
                                onClick = {
                                    viewModel.onFilterBulanChange(index + 1, filterTahun)
                                    showMonthPicker = false
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Filter",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedKategoriFilter == null,
                    onClick = { selectedKategoriFilter = null },
                    label = { Text("Semua", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                KategoriEnum.entries.take(5).forEach { kategori ->
                    FilterChip(
                        selected = selectedKategoriFilter == kategori.label,
                        onClick = {
                            selectedKategoriFilter = if (selectedKategoriFilter == kategori.label) null
                            else kategori.label
                        },
                        label = {
                            Text(kategori.label, style = MaterialTheme.typography.labelMedium)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Transaction List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp)
        ) {
            if (groupedByDate.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Belum ada transaksi",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Coba pilih periode lain atau tambahkan catatan baru.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                groupedByDate.forEach { (date, transactions) ->
                    item {
                        val dailyTotal = transactions.sumOf {
                            if (it.jenis == "keluar") it.jumlah else -it.jumlah
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (dailyTotal > 0) "-${dailyTotal.toRupiah()}"
                                else "+${(-dailyTotal).toRupiah()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (dailyTotal > 0) ExpenseRed
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(transactions, key = { it.id }) { transaksi ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    showDeleteDialog = transaksi.id
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(ExpenseRed, RoundedCornerShape(12.dp))
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.onError
                                    )
                                }
                            }
                        ) {
                            TransaksiCard(
                                transaksi = transaksi,
                                onClick = { onEditTransaksi(transaksi.id) }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Hapus Transaksi") },
            text = { Text("Apakah Anda yakin ingin menghapus transaksi ini?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onHapusTransaksi(id)
                    showDeleteDialog = null
                }) {
                    Text("Hapus", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
