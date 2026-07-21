package com.example.aaaaketahuan.ui.pemasukan

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import com.example.aaaaketahuan.data.model.MetodeBayarEnum
import com.example.aaaaketahuan.data.model.SumberPemasukanEnum
import java.time.Month
import com.example.aaaaketahuan.util.formatNominal
import com.example.aaaaketahuan.util.stripFormatNominal
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PemasukanScreen(
    viewModel: TransaksiViewModel,
    onBack: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var nominal by remember { mutableStateOf("") }
    var catatan by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var selectedSumber by remember { mutableStateOf("Gaji") }
    var selectedMetode by remember { mutableStateOf("Cash") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Periode Baru Dialog State
    var showPeriodDialog by remember { mutableStateOf(false) }
    var pendingPeriod by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                text = "Catat Pemasukan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Nominal
        Text(
            text = "NOMINAL",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Rp",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = nominal.formatNominal(),
                    onValueChange = { newValue ->
                        nominal = newValue.stripFormatNominal()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0", style = MaterialTheme.typography.headlineLarge) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.headlineLarge,
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Catatan (Opsional)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(12.dp)
                    .padding(horizontal = 1.dp)
                    .then(
                        Modifier.padding(start = 0.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CATATAN (OPSIONAL)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = catatan,
            onValueChange = { catatan = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Contoh: Gaji April") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tanggal
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TANGGAL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(Modifier.padding(0.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = LocalDate.parse(tanggal).format(
                        DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale("id", "ID"))
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showDatePicker = true }) {
                    Text("Pilih")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sumber
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SUMBER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val effectiveSumber = viewModel.getEffectiveSumberPemasukan()
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            effectiveSumber.forEach { sumber ->
                FilterChip(
                    selected = selectedSumber == sumber,
                    onClick = { selectedSumber = sumber },
                    label = { Text(sumber) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        labelColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metode
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "METODE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val effectiveMetode = viewModel.getEffectiveMetodeBayar()
        // Split into groups of 3 for layout
        effectiveMetode.chunked(3).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { metode ->
                    FilterChip(
                        selected = selectedMetode == metode,
                        onClick = { selectedMetode = metode },
                        label = { Text(metode) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            labelColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
                // Fill empty space for uneven rows
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Simpan Button
        Button(
            onClick = {
                val jumlah = nominal.toDoubleOrNull() ?: 0.0
                if (jumlah > 0) {
                    viewModel.onSubmitTransaksi(
                        jenis = "masuk",
                        jumlah = jumlah,
                        namaBarang = catatan.ifBlank { selectedSumber },
                        keterangan = catatan,
                        kategori = selectedSumber,
                        tanggal = tanggal,
                        metodeBayar = selectedMetode,
                        sumber = selectedSumber
                    )
                    // Reset form
                    nominal = ""
                    catatan = ""

                    // Check if this pemasukan is in a new month → offer to start new period
                    val transDate = LocalDate.parse(tanggal)
                    val (lastBulan, lastTahun) = viewModel.getLastPeriodStart()
                    if (transDate.monthValue != lastBulan || transDate.year != lastTahun) {
                        pendingPeriod = Pair(transDate.monthValue, transDate.year)
                        showPeriodDialog = true
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Pemasukan berhasil disimpan!")
                        }
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Nominal wajib diisi!")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Simpan",
                style = MaterialTheme.typography.titleMedium
            )
        }
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

    // ─── "Mulai Periode Baru?" Dialog ─────────────────────────────────
    var isCreatingSheet by remember { mutableStateOf(false) }

    if (showPeriodDialog && pendingPeriod != null) {
        val (pBulan, pTahun) = pendingPeriod!!
        val monthName = Month.of(pBulan).getDisplayName(java.time.format.TextStyle.FULL, Locale("id", "ID"))
        AlertDialog(
            onDismissRequest = {
                if (!isCreatingSheet) {
                    showPeriodDialog = false
                    pendingPeriod = null
                }
            },
            icon = {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Mulai Periode Bulan Baru?")
            },
            text = {
                Column {
                    Text(
                        text = "Apakah Anda ingin memulai periode baru untuk $monthName $pTahun?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Sheet \"$monthName $pTahun\" akan dibuat di spreadsheet Anda\n" +
                                "• Dashboard akan menampilkan periode baru\n" +
                                "• Riwayat bulan sebelumnya tetap bisa dilihat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isCreatingSheet) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Membuat sheet baru...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isCreatingSheet = true
                        viewModel.startNewPeriod(pBulan, pTahun) { success ->
                            isCreatingSheet = false
                            showPeriodDialog = false
                            pendingPeriod = null
                            scope.launch {
                                if (success) {
                                    snackbarHostState.showSnackbar(
                                        "Periode baru $monthName $pTahun dimulai! Sheet baru telah dibuat."
                                    )
                                } else {
                                    snackbarHostState.showSnackbar(
                                        "Periode baru $monthName $pTahun dimulai (tidak terhubung ke spreadsheet)."
                                    )
                                }
                            }
                        }
                    },
                    enabled = !isCreatingSheet
                ) {
                    Text("Ya, Mulai")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPeriodDialog = false
                        pendingPeriod = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Periode tidak berubah.")
                        }
                    },
                    enabled = !isCreatingSheet
                ) {
                    Text("Nanti")
                }
            }
        )
    }

    // Snackbar Host
    SnackbarHost(hostState = snackbarHostState)
}
