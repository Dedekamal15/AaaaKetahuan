package com.example.aaaaketahuan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.aaaaketahuan.data.model.Transaksi
import com.example.aaaaketahuan.ui.theme.ExpenseRed
import com.example.aaaaketahuan.util.toDisplayDate
import com.example.aaaaketahuan.util.toRupiah

@Composable
fun TransaksiCard(
    transaksi: Transaksi,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isMasuk = transaksi.jenis == "masuk"
    val amountColor = if (isMasuk) MaterialTheme.colorScheme.primary else ExpenseRed
    val amountPrefix = if (isMasuk) "+ " else "- "
    val iconBgColor = if (isMasuk) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val iconColor = if (isMasuk) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getKategoriIcon(transaksi.kategori),
                    contentDescription = null,
                    tint = iconColor
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaksi.namaBarang,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaksi.kategori,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Text(
                        text = transaksi.tanggal.toDisplayDate(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "$amountPrefix${transaksi.jumlah.toRupiah()}",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor
            )
        }
    }
}

fun getKategoriIcon(kategori: String): ImageVector {
    return when (kategori.lowercase()) {
        "makanan" -> Icons.Default.Restaurant
        "transportasi" -> Icons.Default.DirectionsCar
        "kesehatan" -> Icons.Default.MedicalServices
        "pendidikan" -> Icons.Default.School
        "tagihan" -> Icons.Default.Receipt
        "hiburan" -> Icons.Default.Movie
        "tabungan" -> Icons.Default.Savings
        else -> Icons.Default.MoreHoriz
    }
}
