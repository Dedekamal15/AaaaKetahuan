package com.example.aaaaketahuan.ui.input

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.aaaaketahuan.R
import com.example.aaaaketahuan.data.model.KategoriEnum
import com.example.aaaaketahuan.data.model.MetodeBayarEnum
import com.example.aaaaketahuan.ui.components.AutocompleteTextField
import com.example.aaaaketahuan.ui.components.getKategoriIcon
import com.example.aaaaketahuan.ui.theme.ExpenseRed
import com.example.aaaaketahuan.ui.theme.IncomeGreen
import com.example.aaaaketahuan.util.formatNominal
import com.example.aaaaketahuan.util.stripFormatNominal
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputTransaksiScreen(
    viewModel: TransaksiViewModel,
    editTransaksiId: String? = null
) {
    val saranList by viewModel.saranNamaBarang.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var jenis by remember { mutableStateOf("keluar") }
    var nominal by remember { mutableStateOf("") }
    var namaBarang by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }
    // Determine a safe default kategori (first visible one, fallback "Makanan")
    val defaultKategoriLabel = remember {
        val hidden = viewModel.getHiddenKategori().toSet()
        KategoriEnum.entries.firstOrNull { it.label !in hidden }?.label
            ?: viewModel.getCustomKategori().firstOrNull()
            ?: KategoriEnum.MAKANAN.label
    }
    var selectedKategori by remember { mutableStateOf(defaultKategoriLabel) }
    var selectedMetodeBayar by remember { mutableStateOf("Cash") }
    var tanggal by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // Load existing transaksi if editing — search across ALL data, not just current filter
    LaunchedEffect(editTransaksiId) {
        if (!editTransaksiId.isNullOrBlank()) {
            viewModel.getTransaksiById(editTransaksiId) { transaksi ->
                if (transaksi != null) {
                    jenis = transaksi.jenis
                    nominal = transaksi.jumlah.toLong().toString()
                    namaBarang = transaksi.namaBarang
                    keterangan = transaksi.keterangan
                    selectedKategori = transaksi.kategori
                    selectedMetodeBayar = transaksi.metodeBayar.ifBlank { "Cash" }
                    tanggal = transaksi.tanggal
                }
            }
        }
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
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "AaaaKetahuan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Input Transaksi",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Catat pengeluaran harian Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Transaction Type Indicator (dynamic: shows "Pemasukan" or "Pengeluaran")
        val isExpense = jenis == "keluar"
        val typeColor = if (isExpense) ExpenseRed else IncomeGreen
        val typeIcon = if (isExpense) Icons.Default.ArrowCircleUp else Icons.Default.ArrowCircleDown
        val typeLabel = if (isExpense) "Pengeluaran" else "Pemasukan"
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = typeColor.copy(alpha = 0.12f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    typeIcon,
                    contentDescription = null,
                    tint = typeColor
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = typeColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Nominal
                Text(
                    text = "Nominal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = nominal.formatNominal(),
                    onValueChange = { newValue ->
                        nominal = newValue.stripFormatNominal()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = {
                        Text(
                            text = "Rp",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.headlineMedium,
                    singleLine = true,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Nama Barang with Autocomplete
                AutocompleteTextField(
                    value = namaBarang,
                    onValueChange = { newValue ->
                        namaBarang = newValue
                        viewModel.onNamaBarangChange(newValue)
                    },
                    suggestions = saranList,
                    onSuggestionClick = { suggestion ->
                        namaBarang = suggestion
                        viewModel.clearSaran()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Kategori Grid
                Text(
                    text = "Kategori",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    // Compute kategori list once — changes rarely (only when hidden/custom changes)
                    val allVisibleLabels = remember {
                        val hiddenSet = viewModel.getHiddenKategori().toSet()
                        val visibleEnumLabels = KategoriEnum.entries
                            .filter { it.label !in hiddenSet }
                            .map { it.label }
                        val customLabels = viewModel.getCustomKategori()
                        (visibleEnumLabels + customLabels)
                            .ifEmpty { KategoriEnum.entries.map { it.label } }
                    }
                    val chunked = allVisibleLabels.chunked(4)
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { label ->
                                val isSelected = selectedKategori == label
                                // Static colors per item — no animation overhead
                                val chipBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                val chipContent = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                val chipBorder = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.outlineVariant

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipBg)
                                        .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                        .clickable { selectedKategori = label }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = getKategoriIcon(label),
                                            contentDescription = null,
                                            tint = chipContent
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = chipContent
                                        )
                                    }
                                }
                            }
                            // Fill empty space for uneven rows
                            repeat(4 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Metode Pembayaran Grid
                Text(
                    text = "Metode Pembayaran",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    val effectiveMetode = remember { viewModel.getEffectiveMetodeBayar() }
                    effectiveMetode.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { metode ->
                                val isSelected = selectedMetodeBayar == metode
                                // Static colors — no animation allocation per item
                                val chipBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                val chipContent = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                val chipBorder = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.outlineVariant

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipBg)
                                        .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                        .clickable { selectedMetodeBayar = metode }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = getMetodeIcon(metode),
                                            contentDescription = null,
                                            tint = chipContent
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = metode,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = chipContent
                                        )
                                    }
                                }
                            }
                            // Fill empty space for uneven rows
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Picker
                Text(
                    text = "Tanggal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val formattedDate = remember(tanggal) {
                    LocalDate.parse(tanggal).format(
                        DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale("id", "ID"))
                    )
                }
                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowCircleDown,
                            contentDescription = "Pilih tanggal",
                            modifier = Modifier.clickable { showDatePicker = true }
                        )
                    },
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Keterangan
                Text(
                    text = "Keterangan (Opsional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Tambahkan catatan singkat...") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        val jumlah = nominal.toDoubleOrNull() ?: 0.0
                        if (jumlah > 0 && namaBarang.isNotBlank()) {
                            viewModel.onSubmitTransaksi(
                                jenis = jenis,
                                jumlah = jumlah,
                                namaBarang = namaBarang,
                                keterangan = keterangan,
                                kategori = selectedKategori,
                                tanggal = tanggal,
                                metodeBayar = selectedMetodeBayar
                            )
                            // Reset form
                            nominal = ""
                            namaBarang = ""
                            keterangan = ""
                            viewModel.clearSaran()
                            scope.launch {
                                snackbarHostState.showSnackbar("Transaksi berhasil disimpan!")
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Nominal dan nama barang wajib diisi!")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simpan Transaksi",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Data disimpan secara lokal di perangkat Anda.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        tanggal = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

        // Snackbar Host positioned as overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    } // End Box
}

fun getMetodeIcon(metode: String): ImageVector {
    return when (metode.lowercase()) {
        "cash" -> Icons.Default.Payments
        "kredit" -> Icons.Default.CreditCard
        "e-wallet" -> Icons.Default.AccountBalanceWallet
        "transfer" -> Icons.Default.AccountBalance
        "qris" -> Icons.Default.QrCode2
        else -> Icons.Default.Payments
    }
}

