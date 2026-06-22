package com.example.aaaaketahuan.ui.pemasukan

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.aaaaketahuan.data.model.MetodeBayarEnum
import com.example.aaaaketahuan.data.model.SumberPemasukanEnum
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
    viewModel: TransaksiViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var nominal by remember { mutableStateOf("") }
    var catatan by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var selectedSumber by remember { mutableStateOf(SumberPemasukanEnum.GAJI) }
    var selectedMetode by remember { mutableStateOf(MetodeBayarEnum.CASH) }
    var showDatePicker by remember { mutableStateOf(false) }

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
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* navigate back */ }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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
                verticalAlignment = androidx.compose.ui.Alignment.Bottom
            ) {
                Text(
                    text = "Rp",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = nominal,
                    onValueChange = { newValue ->
                        nominal = newValue.filter { it.isDigit() }
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
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
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
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SUMBER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SumberPemasukanEnum.entries.forEach { sumber ->
                FilterChip(
                    selected = selectedSumber == sumber,
                    onClick = { selectedSumber = sumber },
                    label = { Text(sumber.label) },
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
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "METODE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetodeBayarEnum.entries.take(3).forEach { metode ->
                FilterChip(
                    selected = selectedMetode == metode,
                    onClick = { selectedMetode = metode },
                    label = { Text(metode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        labelColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetodeBayarEnum.entries.drop(3).forEach { metode ->
                FilterChip(
                    selected = selectedMetode == metode,
                    onClick = { selectedMetode = metode },
                    label = { Text(metode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        labelColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
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
                        namaBarang = catatan.ifBlank { selectedSumber.label },
                        keterangan = catatan,
                        kategori = selectedSumber.label,
                        tanggal = tanggal,
                        metodeBayar = selectedMetode.label,
                        sumber = selectedSumber.label
                    )
                    // Reset form
                    nominal = ""
                    catatan = ""
                    scope.launch {
                        snackbarHostState.showSnackbar("Pemasukan berhasil disimpan!")
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

    // Snackbar Host
    SnackbarHost(hostState = snackbarHostState)
}
