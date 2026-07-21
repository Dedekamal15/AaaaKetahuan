package com.example.aaaaketahuan.ui.riwayat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aaaaketahuan.R
import com.example.aaaaketahuan.data.model.KategoriEnum
import com.example.aaaaketahuan.ui.components.TransaksiCard
import com.example.aaaaketahuan.ui.theme.ExpenseRed
import com.example.aaaaketahuan.util.toRupiah
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** Pre-computed Indonesian month names — computed once to avoid recomposition overhead. */
private val BULAN_NAMES: List<String> by lazy {
    (1..12).map { Month.of(it).getDisplayName(TextStyle.FULL, Locale("id", "ID")) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RiwayatScreen(
    viewModel: TransaksiViewModel,
    onEditTransaksi: (String) -> Unit = {}
) {
    val transaksiList by viewModel.transaksiList.collectAsState()
    val filterBulan by viewModel.filterBulan.collectAsState()
    val filterTahun by viewModel.filterTahun.collectAsState()

    // UI State
    var showMonthPicker by remember { mutableStateOf(false) }
    var selectedKategoriFilter by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterJenis by remember { mutableStateOf("semua") }       // "semua", "masuk", "keluar"
    var filterMetodeBayar by remember { mutableStateOf<Set<String>>(emptySet()) } // empty = all
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Derived state: only recalculated when dependencies change
    val monthLabel = remember(filterBulan, filterTahun) {
        "${BULAN_NAMES[filterBulan - 1]} $filterTahun"
    }

    // Apply all filters — recalculated only when transaksiList or any filter changes
    val filteredList = remember(transaksiList, selectedKategoriFilter, filterJenis, filterMetodeBayar, searchQuery) {
        transaksiList
            .filter { t ->
                (selectedKategoriFilter == null || t.kategori == selectedKategoriFilter) &&
                (filterJenis == "semua" || t.jenis == filterJenis) &&
                (filterMetodeBayar.isEmpty() || t.metodeBayar in filterMetodeBayar) &&
                (searchQuery.isBlank() ||
                 t.namaBarang.contains(searchQuery, ignoreCase = true) ||
                 t.keterangan.contains(searchQuery, ignoreCase = true) ||
                 t.kategori.contains(searchQuery, ignoreCase = true))
            }
            .sortedByDescending { it.tanggal }
    }

    // Group transactions by date
    val groupedByDate = remember(filteredList) {
        filteredList.groupBy { it.tanggal }
    }

    // Totals for selected month (pre-filter)
    val totals = remember(transaksiList, filterBulan, filterTahun) {
        val transaksiForTotals = transaksiList.filter { it.bulan == filterBulan && it.tahun == filterTahun }
        val pemasukan = transaksiForTotals.filter { it.jenis == "masuk" }.sumOf { it.jumlah }
        val pengeluaran = transaksiForTotals.filter { it.jenis == "keluar" }.sumOf { it.jumlah }
        Triple(pemasukan, pengeluaran, pemasukan - pengeluaran)
    }
    val totalPemasukan = totals.first
    val totalPengeluaran = totals.second
    val netBalance = totals.third

    // Count active filters
    val activeFilterCount = remember(filterJenis, filterMetodeBayar) {
        listOfNotNull(
            if (filterJenis != "semua") 1 else null,
            if (filterMetodeBayar.isNotEmpty()) 1 else null
        ).sum()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Top AppBar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Cari"
                        )
                    }
                }
            }

            // Search Bar
            if (showSearchBar) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = { Text("Cari transaksi...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Filters
            item {
                Column {
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
                                BULAN_NAMES.forEachIndexed { index, name ->
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
                        // Filter button — now clickable
                        Row(
                            modifier = Modifier.clickable { showFilterSheet = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (activeFilterCount > 0)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (activeFilterCount > 0) "Filter ($activeFilterCount)"
                                else "Filter",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (activeFilterCount > 0)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category chips — horizontally scrollable Row (8 items, no lazy overhead)
                    val effectiveKategori = remember { viewModel.getEffectiveKategori() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
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
                        effectiveKategori.forEach { label ->
                            FilterChip(
                                selected = selectedKategoriFilter == label,
                                onClick = {
                                    selectedKategoriFilter = if (selectedKategoriFilter == label) null
                                    else label
                                },
                                label = {
                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Summary card for selected month
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = totalPemasukan.toRupiah(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pemasukan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = netBalance.toRupiah(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netBalance < 0) ExpenseRed else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (netBalance < 0) "Defisit" else "Sisa",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = totalPengeluaran.toRupiah(),
                                style = MaterialTheme.typography.titleMedium,
                                color = ExpenseRed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pengeluaran",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Transaction List or Empty State
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
                                Box(
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

    // ─── Filter Bottom Sheet ────────────────────────────────────────
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Filter Transaksi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Jenis Transaksi ──
                Text(
                    text = "Jenis Transaksi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("semua" to "Semua", "masuk" to "Masuk", "keluar" to "Keluar").forEach { (value, label) ->
                        FilterChip(
                            selected = filterJenis == value,
                            onClick = {
                                filterJenis = value
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Metode Bayar ──
                Text(
                    text = "Metode Bayar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val metodeList = listOf("Cash", "Kredit", "E-Wallet", "Transfer", "QRIS")
                    metodeList.forEach { metode ->
                        FilterChip(
                            selected = metode in filterMetodeBayar,
                            onClick = {
                                filterMetodeBayar = if (metode in filterMetodeBayar) {
                                    filterMetodeBayar - metode
                                } else {
                                    filterMetodeBayar + metode
                                }
                            },
                            label = { Text(metode) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // ── Reset & Tutup ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = {
                            filterJenis = "semua"
                            filterMetodeBayar = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Filter")
                    }
                    Button(
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Tutup")
                    }
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
