package com.example.aaaaketahuan.forecast.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.aaaaketahuan.forecast.ForecastViewModel
import com.example.aaaaketahuan.forecast.model.BalancePrediction
import com.example.aaaaketahuan.forecast.model.DeficitRisk
import com.example.aaaaketahuan.forecast.model.EndOfMonthPrediction
import com.example.aaaaketahuan.forecast.model.PredictionConfidence
import com.example.aaaaketahuan.forecast.model.RiskSeverity
import com.example.aaaaketahuan.ui.theme.ExpenseRed
import com.example.aaaaketahuan.ui.theme.IncomeGreen
import com.example.aaaaketahuan.util.toRupiah
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dashboard forecasting — layar utama untuk menampilkan hasil prediksi
 * Tahap A: pengeluaran akhir bulan, prediksi saldo, & risiko defisit.
 *
 * Menggunakan [ForecastViewModel] terpisah (bukan TransaksiViewModel).
 * Semua state di-render sesuai sealed class yang sudah didefinisikan
 * di [com.example.aaaaketahuan.forecast.model].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    viewModel: ForecastViewModel,
    onBack: () -> Unit = {}
) {
    val endPrediction by viewModel.endOfMonthPrediction.collectAsState()
    val balancePrediction by viewModel.balancePrediction.collectAsState()
    val deficitRisk by viewModel.deficitRisk.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ─── Date picker untuk Section 2 ──────────────────────────────────
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTargetDate by remember { mutableStateOf<LocalDate?>(null) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // Snackbar untuk error global
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Load data on first composition
    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {
            // ─── Top Bar ──────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
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
                    text = "Forecasting",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Proyeksikan pengeluaran dan pantau kesehatan keuangan Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════════════════════════════
            // SECTION 1: PREDIKSI PENGELUARAN AKHIR BULAN
            // ═══════════════════════════════════════════════════════════
            SectionHeader(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = "Prediksi Pengeluaran Akhir Bulan",
                subtitle = "Berdasarkan rata-rata pengeluaran 30 hari terakhir"
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
                when (val pred = endPrediction) {
                    null -> LoadingOrIdleContent(isLoading, "Memuat prediksi pengeluaran...")
                    is EndOfMonthPrediction.InsufficientData -> InsufficientContent(
                        message = pred.reason,
                        subMessage = "Tersedia ${pred.daysAvailable} dari ${pred.daysRequired} hari data."
                    )
                    is EndOfMonthPrediction.Predicted -> EndOfMonthPredictedContent(
                        currentTotal = pred.currentTotal,
                        predictedTotal = pred.predictedTotal,
                        confidence = pred.confidence
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════════════════════════════
            // SECTION 2: PREDIKSI SALDO PADA TANGGAL TERTENTU
            // ═══════════════════════════════════════════════════════════
            SectionHeader(
                icon = Icons.Default.Schedule,
                title = "Prediksi Saldo",
                subtitle = "Proyeksikan saldo Anda di tanggal tertentu"
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
                Column(modifier = Modifier.padding(16.dp)) {

                    // Date picker button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedTargetDate != null)
                                selectedTargetDate!!.format(
                                    DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))
                                )
                            else "Pilih tanggal target",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedTargetDate != null)
                                MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(if (selectedTargetDate != null) "Ubah" else "Pilih")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Result area
                    when (val pred = balancePrediction) {
                        null -> {
                            if (selectedTargetDate == null) {
                                IdleContent(
                                    message = "Pilih tanggal target untuk melihat prediksi saldo.",
                                    icon = Icons.Default.CalendarToday
                                )
                            } else if (isLoading) {
                                LoadingOrIdleContent(
                                    isLoading = true,
                                    loadingMessage = "Menghitung prediksi saldo..."
                                )
                            }
                        }
                        is BalancePrediction.InsufficientData -> InsufficientContent(
                            message = pred.reason,
                            subMessage = "Tersedia ${pred.daysAvailable} dari ${pred.daysRequired} hari data."
                        )
                        is BalancePrediction.TargetDateInPast -> InfoContent(
                            icon = Icons.Default.Warning,
                            title = "Tanggal sudah lewat",
                            message = "Pilih tanggal yang akan datang untuk prediksi saldo.",
                            iconTint = MaterialTheme.colorScheme.error
                        )
                        is BalancePrediction.Predicted -> BalancePredictedContent(
                            currentBalance = pred.currentBalance,
                            predictedBalance = pred.predictedBalance,
                            targetDate = pred.targetDate,
                            confidence = pred.confidence
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════════════════════════════
            // SECTION 3: RISIKO DEFISIT
            // ═══════════════════════════════════════════════════════════
            SectionHeader(
                icon = Icons.Default.Warning,
                title = "Risiko Defisit",
                subtitle = "Deteksi kemungkinan saldo negatif akhir bulan"
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (val risk = deficitRisk) {
                null -> {
                    if (isLoading) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            LoadingOrIdleContent(true, "Menganalisis risiko defisit...")
                        }
                    }
                }
                is DeficitRisk.InsufficientData -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        InsufficientContent(
                            message = risk.reason,
                            subMessage = "Data belum mencukupi untuk deteksi risiko."
                        )
                    }
                }
                is DeficitRisk.AlreadyDeficit -> DeficitRiskCard(
                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                    iconColor = MaterialTheme.colorScheme.onErrorContainer,
                    title = "Saldo Sudah Negatif",
                    titleColor = MaterialTheme.colorScheme.onErrorContainer,
                    content = {
                        AmountRow(
                            label = "Saldo saat ini",
                            amount = risk.currentBalance,
                            amountColor = ExpenseRed
                        )
                    }
                )
                is DeficitRisk.AtRisk -> DeficitRiskCard(
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    title = when (risk.level) {
                        RiskSeverity.LOW -> "Risiko Defisit Rendah"
                        RiskSeverity.MODERATE -> "Risiko Defisit Sedang"
                        RiskSeverity.HIGH -> "Risiko Defisit Tinggi"
                    },
                    titleColor = when (risk.level) {
                        RiskSeverity.HIGH -> ExpenseRed
                        else -> MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    content = {
                        AmountRow(
                            label = "Proyeksi saldo akhir",
                            amount = risk.projectedBalance,
                            amountColor = ExpenseRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AmountRow(
                            label = "Perkiraan defisit",
                            amount = -risk.shortfallAmount,
                            amountColor = ExpenseRed
                        )
                    }
                )
                is DeficitRisk.Safe -> DeficitRiskCard(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = "Aman",
                    titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    content = {
                        AmountRow(
                            label = "Proyeksi saldo akhir",
                            amount = risk.projectedBalance,
                            amountColor = IncomeGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AmountRow(
                            label = "Surplus",
                            amount = risk.buffer,
                            amountColor = IncomeGreen
                        )
                    }
                )
            }
        } // End Column

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // ─── Date Picker Dialog ───────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        selectedTargetDate = date
                        viewModel.loadBalancePrediction(date)
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
}

// ═══════════════════════════════════════════════════════════════════════
// SECTION HEADER
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STATE RENDERERS
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SectionCardPadding(content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        content()
    }
}

// ─── Loading / Idle ──────────────────────────────────────────────────

@Composable
private fun LoadingOrIdleContent(
    isLoading: Boolean,
    loadingMessage: String
) {
    SectionCardPadding {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = loadingMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IdleContent(message: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

// ─── Insufficient Data ───────────────────────────────────────────────

@Composable
private fun InsufficientContent(message: String, subMessage: String) {
    SectionCardPadding {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Data Belum Mencukupi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subMessage,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── Info (non-error notice) ─────────────────────────────────────────

@Composable
private fun InfoContent(
    icon: ImageVector,
    title: String,
    message: String,
    iconTint: androidx.compose.ui.graphics.Color
) {
    SectionCardPadding {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SECTION 1: PREDIKSI PENGELUARAN AKHIR BULAN
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EndOfMonthPredictedContent(
    currentTotal: Double,
    predictedTotal: Double,
    confidence: PredictionConfidence
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Confidence badge
        ConfidenceBadge(confidence = confidence)

        Spacer(modifier = Modifier.height(16.dp))

        // Current spending
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Pengeluaran saat ini",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = currentTotal.toRupiah(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Predicted total
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Estimasi akhir bulan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = predictedTotal.toRupiah(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar: current vs predicted
        val progressFraction = if (predictedTotal > 0)
            (currentTotal / predictedTotal).toFloat().coerceIn(0f, 1f) else 0f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress labels
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Terpakai: ${currentTotal.toRupiah()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Sisa: ${(predictedTotal - currentTotal).coerceAtLeast(0.0).toRupiah()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SECTION 2: PREDIKSI SALDO
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun BalancePredictedContent(
    currentBalance: Double,
    predictedBalance: Double,
    targetDate: String,
    confidence: PredictionConfidence
) {
    // Format date for display
    val formattedDate = remember(targetDate) {
        try {
            val date = LocalDate.parse(targetDate)
            date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID")))
        } catch (e: Exception) {
            targetDate
        }
    }

    // Confidence badge
    ConfidenceBadge(confidence = confidence)

    Spacer(modifier = Modifier.height(16.dp))

    // Target date display
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Current balance
    AmountRow(
        label = "Saldo saat ini",
        amount = currentBalance,
        amountColor = if (currentBalance >= 0) IncomeGreen else ExpenseRed
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Predicted balance
    val predColor = when {
        predictedBalance < 0 -> ExpenseRed
        predictedBalance >= currentBalance -> IncomeGreen
        else -> MaterialTheme.colorScheme.onSurface
    }
    AmountRow(
        label = "Prediksi saldo",
        amount = predictedBalance,
        amountColor = predColor,
        amountFontWeight = FontWeight.Bold
    )

    // Difference
    val difference = predictedBalance - currentBalance
    AnimatedVisibility(visible = difference != 0.0) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))
            AmountRow(
                label = if (difference >= 0) "Proyeksi kenaikan" else "Proyeksi penurunan",
                amount = kotlin.math.abs(difference),
                amountColor = if (difference >= 0) IncomeGreen else ExpenseRed,
                amountPrefix = if (difference >= 0) "+ " else "- "
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SECTION 3: RISIKO DEFISIT — CARDS
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DeficitRiskCard(
    backgroundColor: androidx.compose.ui.graphics.Color,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    titleColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (title == "Aman") Icons.Default.CheckCircle
                    else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ConfidenceBadge(confidence: PredictionConfidence) {
    val label = remember(confidence) {
        when (confidence) {
            PredictionConfidence.HIGH -> "Tinggi"
            PredictionConfidence.MEDIUM -> "Sedang"
            PredictionConfidence.LOW -> "Rendah"
        }
    }
    val color = when (confidence) {
        PredictionConfidence.HIGH -> IncomeGreen
        PredictionConfidence.MEDIUM -> MaterialTheme.colorScheme.tertiary
        PredictionConfidence.LOW -> MaterialTheme.colorScheme.error
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Confidence: $label",
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun AmountRow(
    label: String,
    amount: Double,
    amountColor: androidx.compose.ui.graphics.Color,
    amountFontWeight: FontWeight = FontWeight.Normal,
    amountPrefix: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$amountPrefix${amount.toRupiah()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = amountFontWeight,
            color = amountColor
        )
    }
}
