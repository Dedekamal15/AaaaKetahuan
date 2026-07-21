package com.example.aaaaketahuan.ui.grafik

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.aaaaketahuan.R
import com.example.aaaaketahuan.ui.theme.ExpenseRed
import com.example.aaaaketahuan.util.toRupiah
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun GrafikScreen(
    viewModel: TransaksiViewModel,
    onNavigateToRiwayat: () -> Unit = {}
) {
    val transaksiList by viewModel.transaksiList.collectAsState()
    val filterBulan by viewModel.filterBulan.collectAsState()
    val filterTahun by viewModel.filterTahun.collectAsState()
    var showMoreMenu by remember { mutableStateOf(false) }

    val bulanNames = (1..12).map {
        java.time.Month.of(it).getDisplayName(TextStyle.FULL, Locale("id", "ID"))
    }

    // Get expense by category for current month
    val kategoriData = transaksiList
        .filter { it.bulan == filterBulan && it.tahun == filterTahun && it.jenis == "keluar" }
        .groupBy { it.kategori }
        .mapValues { (_, list) -> list.sumOf { it.jumlah } }

    val totalPengeluaran = kategoriData.values.sum()
    val kategoriTerboros = kategoriData.maxByOrNull { it.value }?.key ?: "-"

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // Top AppBar
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            IconButton(onClick = onNavigateToRiwayat) {
                Icon(Icons.Default.Search, contentDescription = "Cari")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Header & Month Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Analisis Grafik",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Laporan pengeluaran bulanan Anda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Month Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (filterBulan > 1) viewModel.onFilterBulanChange(filterBulan - 1, filterTahun)
                else viewModel.onFilterBulanChange(12, filterTahun - 1)
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan sebelumnya")
            }
            Text(
                text = "${bulanNames[filterBulan - 1]} $filterTahun",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            IconButton(onClick = {
                if (filterBulan < 12) viewModel.onFilterBulanChange(filterBulan + 1, filterTahun)
                else viewModel.onFilterBulanChange(1, filterTahun + 1)
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Bulan selanjutnya")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bar Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pengeluaran per Kategori",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Lihat Riwayat") },
                                onClick = {
                                    showMoreMenu = false
                                    onNavigateToRiwayat()
                                }
                            )
                        }
                    }
                }

                if (kategoriData.isNotEmpty()) {
                    val categoryColors = listOf(
                        Color(0xFFFF8A65), Color(0xFF4DB6AC), Color(0xFF9575CD),
                        Color(0xFFF06292), Color(0xFFDCE775), Color(0xFF64B5F6),
                        Color(0xFFFFB74D), Color(0xFFAED581)
                    )

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        factory = { context ->
                            BarChart(context).apply {
                                description.isEnabled = false
                                legend.isEnabled = false
                                setFitBars(true)
                                setDrawGridBackground(false)
                                setDrawBorders(false)
                                setNoDataText("Belum ada data")

                                xAxis.apply {
                                    position = XAxis.XAxisPosition.BOTTOM
                                    setDrawGridLines(false)
                                    granularity = 1f
                                    textColor = android.graphics.Color.parseColor("#40493d")
                                    textSize = 10f
                                }

                                axisLeft.apply {
                                    setDrawGridLines(true)
                                    gridColor = android.graphics.Color.parseColor("#20C4C7C5")
                                    textColor = android.graphics.Color.parseColor("#40493d")
                                    textSize = 10f
                                    axisMinimum = 0f
                                }

                                axisRight.isEnabled = false
                            }
                        },
                        update = { chart ->
                            val entries = kategoriData.values.mapIndexed { index, value ->
                                BarEntry(index.toFloat(), value.toFloat())
                            }
                            val labels = kategoriData.keys.toList()

                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

                            val dataSet = BarDataSet(entries, "Pengeluaran").apply {
                                colors = categoryColors.take(entries.size).map {
                                    android.graphics.Color.argb(
                                        (it.alpha * 255).toInt(),
                                        (it.red * 255).toInt(),
                                        (it.green * 255).toInt(),
                                        (it.blue * 255).toInt()
                                    )
                                }
                                setDrawValues(false)
                            }

                            chart.data = BarData(dataSet).apply {
                                barWidth = 0.6f
                            }
                            chart.invalidate()
                            chart.animateY(1000)
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada data pengeluaran",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Total Pengeluaran",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = totalPengeluaran.toRupiah(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Stats rows
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kategori Terboros",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = kategoriTerboros,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detail Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                Text(
                    text = "Rincian Per Kategori",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "KATEGORI",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }

                // Table Rows
                val categoryColors = listOf(
                    Color(0xFFFF8A65), Color(0xFF4DB6AC), Color(0xFF9575CD),
                    Color(0xFFF06292), Color(0xFFDCE775), Color(0xFF64B5F6),
                    Color(0xFFFFB74D), Color(0xFFAED581)
                )
                kategoriData.entries.sortedByDescending { it.value }
                    .forEachIndexed { index, (kategori, nominal) ->
                        val percent = if (totalPengeluaran > 0)
                            (nominal / totalPengeluaran * 100) else 0.0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(2f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(categoryColors[index % categoryColors.size])
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = kategori,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Text(
                                text = nominal.toRupiah(),
                                style = MaterialTheme.typography.titleMedium,
                                color = ExpenseRed,
                                modifier = Modifier.weight(1.5f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "${String.format("%.1f", percent)}%",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    }

                if (kategoriData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
