package com.tana.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tana.data.model.SavingsGoalEntity
import com.tana.data.model.TransactionEntity
import com.tana.data.model.TransactionType
import com.tana.ui.theme.AccentExpense
import com.tana.ui.theme.AccentIncome
import com.tana.ui.theme.AccentPurple
import com.tana.ui.theme.AccentSavings
import com.tana.ui.theme.BrandPrimaryBright
import com.tana.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class DayChartData(
    val dateLabel: String,
    val dayName: String,
    val dateMillis: Long,
    val expenseAmount: Double,
    val savingsAmount: Double,
    val incomeAmount: Double
)

@Composable
fun WeeklyExpenseSavingsChart(
    transactions: List<TransactionEntity>,
    savingsGoals: List<SavingsGoalEntity> = emptyList(),
    selectedSavingsGoalId: String? = "ALL",
    modifier: Modifier = Modifier,
    selectedChartMode: String = "CRISP_LINE",
    onChartModeChanged: ((String) -> Unit)? = null
) {
    var selectedDaysRange by remember { mutableStateOf(7) } // 7 or 14 days
    val currentMode = selectedChartMode

    val predictiveResult = remember(transactions, savingsGoals, selectedSavingsGoalId) {
        FinanceViewModel.calculatePredictiveSaving(transactions, savingsGoals, selectedSavingsGoalId)
    }

    val chartData = remember(transactions, selectedDaysRange) {
        val list = mutableListOf<DayChartData>()
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
        val dateFormat = SimpleDateFormat("d MMM", Locale("id", "ID"))

        for (i in (selectedDaysRange - 1) downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 86399999L

            val dayTx = transactions.filter { it.timestamp in startOfDay..endOfDay }
            val expense = dayTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val savings = dayTx.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amount }
            val income = dayTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

            list.add(
                DayChartData(
                    dateLabel = dateFormat.format(Date(startOfDay)),
                    dayName = dayFormat.format(Date(startOfDay)),
                    dateMillis = startOfDay,
                    expenseAmount = expense,
                    savingsAmount = savings,
                    incomeAmount = income
                )
            )
        }
        list
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxVal = remember(chartData, currentMode, predictiveResult) {
        if (currentMode == "PREDICTIVE_SAVINGS") {
            val peak = max(predictiveResult.targetAmount * 1.12, (predictiveResult.points.maxOfOrNull { it.amount } ?: 1000.0) * 1.1)
            if (peak <= 0.0) 100000.0 else peak
        } else if (currentMode == "CUMULATIVE_AREA") {
            var running = 0.0
            var peak = 1.0
            chartData.forEach {
                val netDay = if (it.incomeAmount > 0) (it.incomeAmount - it.expenseAmount) else (it.savingsAmount - it.expenseAmount)
                running += netDay
                if (running > peak) peak = running
            }
            if (peak <= 0.0) 100000.0 else peak * 1.2
        } else {
            val peak = chartData.maxOfOrNull { max(it.expenseAmount, it.savingsAmount) } ?: 1.0
            if (peak <= 0.0) 100000.0 else peak * 1.25
        }
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(chartData, currentMode, predictiveResult) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
    }

    MonochromeCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row with Title and Range / Status Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (currentMode == "PREDICTIVE_SAVINGS") "PREDIKSI PROYEKSI TABUNGAN" else "ANALISIS ARUS KAS & GRAFIK",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (currentMode == "PREDICTIVE_SAVINGS") "Proyeksi cerdas dari riwayat transaksi & ritme tabungan" else "Opsi grafik tersimpan otomatis",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (currentMode == "PREDICTIVE_SAVINGS") {
                    val progressPct = if (predictiveResult.targetAmount > 0) {
                        ((predictiveResult.currentAmount / predictiveResult.targetAmount) * 100).toInt().coerceIn(0, 100)
                    } else 0
                    Surface(
                        color = BrandPrimaryBright.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, BrandPrimaryBright.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = BrandPrimaryBright,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$progressPct% Tercapai",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = BrandPrimaryBright
                            )
                        }
                    }
                } else {
                    // Days Toggle (7H vs 14H)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.clickable {
                            selectedDaysRange = if (selectedDaysRange == 7) 14 else 7
                            selectedIndex = null
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rentang: ${selectedDaysRange} Hari",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Type Selector Tabs (Garis | Kurva | Batang | Akumulasi | Prediksi)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf(
                    Triple("CRISP_LINE", "Garis", Icons.Filled.TrendingDown),
                    Triple("SMOOTH_CURVE", "Kurva", Icons.Filled.AutoGraph),
                    Triple("DAILY_BAR", "Batang", Icons.Filled.BarChart),
                    Triple("CUMULATIVE_AREA", "Akumulasi", Icons.Filled.Savings),
                    Triple("PREDICTIVE_SAVINGS", "Prediksi", Icons.Filled.AutoAwesome)
                )

                modes.forEach { (modeKey, label, icon) ->
                    val isSelected = currentMode == modeKey
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedIndex = null
                                onChartModeChanged?.invoke(modeKey)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 1.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (currentMode == "PREDICTIVE_SAVINGS" && savingsGoals.isEmpty()) {
                // No real data to project from - show an honest empty state instead of
                // rendering a chart built on a fabricated target/rate (previously this mode
                // silently defaulted to a fake Rp 10.000.000 target with no basis in the
                // user's actual data, which is misleading for a real savings app).
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum Ada Target Tabungan",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Buat target tabungan dulu supaya prediksi bisa dihitung dari data asli, bukan perkiraan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            // Legend indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentMode == "PREDICTIVE_SAVINGS") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentSavings)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Histori",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BrandPrimaryBright)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Proyeksi Masa Depan",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                            color = BrandPrimaryBright
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .background(Color(0xFF9C9587))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Target (${FinanceViewModel.formatCompact(predictiveResult.targetAmount)})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF9C9587)
                        )
                    }
                } else if (currentMode == "CUMULATIVE_AREA") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AccentIncome, AccentSavings)))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Pertumbuhan Saldo Bersih",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(AccentSavings, Color(0xFFD4B876)))
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Tabungan",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(AccentExpense, Color(0xFF8C4438)))
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Pengeluaran",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Tooltip Card
            if (currentMode == "PREDICTIVE_SAVINGS") {
                AnimatedVisibility(
                    visible = selectedIndex != null && selectedIndex in predictiveResult.points.indices,
                    enter = fadeIn(spring()),
                    exit = fadeOut(spring())
                ) {
                    val pt = selectedIndex?.let { predictiveResult.points.getOrNull(it) }
                    if (pt != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (pt.isProjected) BrandPrimaryBright.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (pt.milestoneLabel != null) "${pt.dateLabel} • ${pt.milestoneLabel}" else pt.dateLabel,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                        color = if (pt.isProjected) BrandPrimaryBright else MaterialTheme.colorScheme.onSurface
                                    )
                                    val dist = (predictiveResult.targetAmount - pt.amount).coerceAtLeast(0.0)
                                    Text(
                                        text = if (dist <= 0) "Target Tercapai Penuh! 🎉" else "Sisa ke target: ${FinanceViewModel.formatCompact(dist)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = if (dist <= 0) AccentSavings else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = FinanceViewModel.formatRupiah(pt.amount),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = if (pt.isProjected) BrandPrimaryBright else AccentSavings
                                    )
                                    Text(
                                        text = if (pt.isProjected) "Estimasi Saldo" else "Saldo Riwayat",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = selectedIndex != null && selectedIndex in chartData.indices,
                    enter = fadeIn(spring()),
                    exit = fadeOut(spring())
                ) {
                    val item = selectedIndex?.let { chartData.getOrNull(it) }
                    if (item != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${item.dayName}, ${item.dateLabel}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val netDay = if (item.incomeAmount > 0) item.incomeAmount - item.expenseAmount else item.savingsAmount - item.expenseAmount
                                    Text(
                                        text = if (netDay >= 0) "Surplus: +${FinanceViewModel.formatCompact(netDay)}"
                                        else "Defisit: ${FinanceViewModel.formatCompact(netDay)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = if (netDay >= 0) AccentSavings else AccentExpense
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "+${FinanceViewModel.formatCompact(item.savingsAmount)}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                            color = AccentSavings
                                        )
                                        Text(
                                            text = "Tabungan",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "-${FinanceViewModel.formatCompact(item.expenseAmount)}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                            color = AccentExpense
                                        )
                                        Text(
                                            text = "Pengeluaran",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Modern Canvas Chart
            val gridColor = MaterialTheme.colorScheme.outlineVariant
            val isDark = MaterialTheme.colorScheme.surface.let { it.red < 0.5f }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            ) {
                val canvasSurfaceColor = MaterialTheme.colorScheme.surface
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(chartData, currentMode, predictiveResult) {
                            detectTapGestures { offset ->
                                if (currentMode == "PREDICTIVE_SAVINGS") {
                                    val count = predictiveResult.points.size
                                    if (count > 0) {
                                        val slotWidth = size.width / count
                                        val index = (offset.x / slotWidth).toInt().coerceIn(0, count - 1)
                                        selectedIndex = if (selectedIndex == index) null else index
                                    }
                                } else {
                                    val slotWidth = size.width / chartData.size
                                    val index = (offset.x / slotWidth).toInt().coerceIn(0, chartData.size - 1)
                                    selectedIndex = if (selectedIndex == index) null else index
                                }
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 24.dp.toPx()

                    // Draw horizontal glowing grid lines
                    val gridLevels = listOf(0.25f, 0.5f, 0.75f, 1f)
                    for (level in gridLevels) {
                        val y = canvasHeight * (1f - level)
                        drawLine(
                            color = gridColor.copy(alpha = 0.35f),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (currentMode == "PREDICTIVE_SAVINGS") {
                        val points = predictiveResult.points
                        val count = points.size
                        if (count >= 2) {
                            val slotWidth = canvasWidth / count
                            val targetAmount = predictiveResult.targetAmount.coerceAtLeast(1000.0)

                            // Selection highlight column
                            selectedIndex?.let { sel ->
                                if (sel in points.indices) {
                                    drawRoundRect(
                                        color = if (isDark) Color(0xFF2A2A38).copy(alpha = 0.7f) else Color(0xFFE8E8EE).copy(alpha = 0.7f),
                                        topLeft = Offset(sel * slotWidth + 2.dp.toPx(), 0f),
                                        size = Size(slotWidth - 4.dp.toPx(), canvasHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                                    )
                                }
                            }

                            // 1. Draw Target Goal Milestone Guideline (Horizontal Gold Line)
                            val targetRatio = ((targetAmount / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)
                            val targetY = canvasHeight - (canvasHeight * targetRatio)
                            drawLine(
                                color = Color(0xFF9C9587).copy(alpha = 0.7f),
                                start = Offset(0f, targetY),
                                end = Offset(canvasWidth, targetY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )

                            // Calculate point coordinates
                            val pointCoords = points.mapIndexed { i, pt ->
                                val x = i * slotWidth + slotWidth / 2
                                val ratio = ((pt.amount / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)
                                Offset(x, canvasHeight - (canvasHeight * ratio))
                            }

                            // Find Today index
                            val todayIndex = points.indexOfFirst { !it.isProjected && it.isMilestone }.takeIf { it >= 0 } ?: (points.indexOfLast { !it.isProjected }.takeIf { it >= 0 } ?: 0)

                            // 2. Draw Historical Solid Line and Area (from start up to Today)
                            val histCoords = pointCoords.take(todayIndex + 1)
                            if (histCoords.size >= 2) {
                                val pathHist = Path().apply {
                                    moveTo(histCoords.first().x, histCoords.first().y)
                                    for (i in 1 until histCoords.size) {
                                        lineTo(histCoords[i].x, histCoords[i].y)
                                    }
                                }
                                val fillHist = Path().apply {
                                    addPath(pathHist)
                                    lineTo(histCoords.last().x, canvasHeight)
                                    lineTo(histCoords.first().x, canvasHeight)
                                    close()
                                }
                                drawPath(
                                    path = fillHist,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(AccentSavings.copy(alpha = 0.35f), Color.Transparent),
                                        startY = 0f,
                                        endY = canvasHeight
                                    )
                                )
                                drawPath(
                                    path = pathHist,
                                    color = AccentSavings,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }

                            // 3. Draw Future Projected Dashed Line and Glow Area (from Today onward)
                            val projCoords = pointCoords.drop(todayIndex)
                            if (projCoords.size >= 2) {
                                val pathProj = Path().apply {
                                    moveTo(projCoords.first().x, projCoords.first().y)
                                    for (i in 1 until projCoords.size) {
                                        lineTo(projCoords[i].x, projCoords[i].y)
                                    }
                                }
                                val fillProj = Path().apply {
                                    addPath(pathProj)
                                    lineTo(projCoords.last().x, canvasHeight)
                                    lineTo(projCoords.first().x, canvasHeight)
                                    close()
                                }
                                drawPath(
                                    path = fillProj,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(BrandPrimaryBright.copy(alpha = 0.22f), Color.Transparent),
                                        startY = 0f,
                                        endY = canvasHeight
                                    )
                                )
                                drawPath(
                                    path = pathProj,
                                    color = BrandPrimaryBright,
                                    style = Stroke(
                                        width = 2.5.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                                    )
                                )
                            }

                            // 4. Vertical Today Divider
                            val todayPt = pointCoords.getOrNull(todayIndex)
                            if (todayPt != null) {
                                drawLine(
                                    color = AccentSavings.copy(alpha = 0.5f),
                                    start = Offset(todayPt.x, 0f),
                                    end = Offset(todayPt.x, canvasHeight),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                            }

                            // 5. Draw Point Nodes
                            pointCoords.forEachIndexed { i, pt ->
                                val isSelected = selectedIndex == i
                                val isToday = i == todayIndex
                                val isTargetMilestone = i == count - 1
                                val isProjected = points[i].isProjected

                                when {
                                    isTargetMilestone -> {
                                        // Glowing Target Achievement Star Node
                                        drawCircle(
                                            color = Color(0xFF9C9587),
                                            radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                                            center = pt
                                        )
                                    }
                                    isToday -> {
                                        // Glowing Today Pulse Node
                                        drawCircle(
                                            color = AccentSavings.copy(alpha = 0.3f),
                                            radius = 11.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = AccentSavings,
                                            radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 2.dp.toPx(),
                                            center = pt
                                        )
                                    }
                                    isProjected -> {
                                        // Future Projection Diamonds
                                        drawCircle(
                                            color = BrandPrimaryBright,
                                            radius = if (isSelected) 5.5.dp.toPx() else 3.5.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = canvasSurfaceColor,
                                            radius = if (isSelected) 3.dp.toPx() else 1.8.dp.toPx(),
                                            center = pt
                                        )
                                    }
                                    else -> {
                                        // Past Historical Nodes
                                        drawCircle(
                                            color = AccentSavings,
                                            radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = canvasSurfaceColor,
                                            radius = if (isSelected) 2.5.dp.toPx() else 1.5.dp.toPx(),
                                            center = pt
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val slotWidth = canvasWidth / chartData.size

                        // Selection highlight column
                        selectedIndex?.let { sel ->
                            if (sel in chartData.indices) {
                                drawRoundRect(
                                    color = if (isDark) Color(0xFF2A2A38).copy(alpha = 0.7f) else Color(0xFFE8E8EE).copy(alpha = 0.7f),
                                    topLeft = Offset(sel * slotWidth + 2.dp.toPx(), 0f),
                                    size = Size(slotWidth - 4.dp.toPx(), canvasHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                                )
                            }
                        }

                        when (currentMode) {
                            "DAILY_BAR" -> {
                                val barWidth = slotWidth * (if (selectedDaysRange > 7) 0.22f else 0.28f)
                                val groupGap = slotWidth * 0.06f

                                chartData.forEachIndexed { i, day ->
                                    val slotCenter = i * slotWidth + slotWidth / 2
                                    val savingsX = slotCenter - barWidth - (groupGap / 2)
                                    val expenseX = slotCenter + (groupGap / 2)

                                    val savingsHeightRatio = ((day.savingsAmount / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)
                                    val expenseHeightRatio = ((day.expenseAmount / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)

                                    val savingsH = canvasHeight * savingsHeightRatio
                                    val expenseH = canvasHeight * expenseHeightRatio

                                    // Savings Bar
                                    if (savingsH > 0) {
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(AccentSavings, AccentSavings.copy(alpha = 0.6f)),
                                                startY = canvasHeight - savingsH,
                                                endY = canvasHeight
                                            ),
                                            topLeft = Offset(savingsX, canvasHeight - savingsH),
                                            size = Size(barWidth, savingsH),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                        )
                                    } else {
                                        drawCircle(
                                            color = AccentSavings.copy(alpha = 0.2f),
                                            radius = 2.dp.toPx(),
                                            center = Offset(savingsX + barWidth / 2, canvasHeight - 3.dp.toPx())
                                        )
                                    }

                                    // Expense Bar
                                    if (expenseH > 0) {
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(AccentExpense, AccentExpense.copy(alpha = 0.6f)),
                                                startY = canvasHeight - expenseH,
                                                endY = canvasHeight
                                            ),
                                            topLeft = Offset(expenseX, canvasHeight - expenseH),
                                            size = Size(barWidth, expenseH),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                        )
                                    } else {
                                        drawCircle(
                                            color = AccentExpense.copy(alpha = 0.2f),
                                            radius = 2.dp.toPx(),
                                            center = Offset(expenseX + barWidth / 2, canvasHeight - 3.dp.toPx())
                                        )
                                    }
                                }
                            }

                            "CRISP_LINE" -> {
                                val pointsSavings = chartData.mapIndexed { i, day ->
                                    val x = i * slotWidth + slotWidth / 2
                                    val sRatio = ((day.savingsAmount / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)
                                    Offset(x, canvasHeight - (canvasHeight * sRatio))
                                }
                                val pointsExpense = chartData.mapIndexed { i, day ->
                                    val x = i * slotWidth + slotWidth / 2
                                    val eRatio = ((day.expenseAmount / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)
                                    Offset(x, canvasHeight - (canvasHeight * eRatio))
                                }

                                // Expense Line
                                if (pointsExpense.size >= 2) {
                                    val pathExpense = Path().apply {
                                        moveTo(pointsExpense.first().x, pointsExpense.first().y)
                                        for (i in 1 until pointsExpense.size) {
                                            lineTo(pointsExpense[i].x, pointsExpense[i].y)
                                        }
                                    }
                                    val fillExpense = Path().apply {
                                        addPath(pathExpense)
                                        lineTo(pointsExpense.last().x, canvasHeight)
                                        lineTo(pointsExpense.first().x, canvasHeight)
                                        close()
                                    }
                                    drawPath(
                                        path = fillExpense,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(AccentExpense.copy(alpha = 0.22f), Color.Transparent),
                                            startY = 0f,
                                            endY = canvasHeight
                                        )
                                    )
                                    drawPath(
                                        path = pathExpense,
                                        color = AccentExpense,
                                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }

                                // Savings Line
                                if (pointsSavings.size >= 2) {
                                    val pathSavings = Path().apply {
                                        moveTo(pointsSavings.first().x, pointsSavings.first().y)
                                        for (i in 1 until pointsSavings.size) {
                                            lineTo(pointsSavings[i].x, pointsSavings[i].y)
                                        }
                                    }
                                    val fillSavings = Path().apply {
                                        addPath(pathSavings)
                                        lineTo(pointsSavings.last().x, canvasHeight)
                                        lineTo(pointsSavings.first().x, canvasHeight)
                                        close()
                                    }
                                    drawPath(
                                        path = fillSavings,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(AccentSavings.copy(alpha = 0.3f), Color.Transparent),
                                            startY = 0f,
                                            endY = canvasHeight
                                        )
                                    )
                                    drawPath(
                                        path = pathSavings,
                                        color = AccentSavings,
                                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }

                                // Data Point Rings
                                pointsExpense.forEachIndexed { i, pt ->
                                    val isSelected = selectedIndex == i
                                    drawCircle(
                                        color = AccentExpense,
                                        radius = if (isSelected) 5.5.dp.toPx() else 3.5.dp.toPx(),
                                        center = pt
                                    )
                                    drawCircle(
                                        color = canvasSurfaceColor,
                                        radius = if (isSelected) 3.dp.toPx() else 1.5.dp.toPx(),
                                        center = pt
                                    )
                                }
                                pointsSavings.forEachIndexed { i, pt ->
                                    val isSelected = selectedIndex == i
                                    drawCircle(
                                        color = AccentSavings,
                                        radius = if (isSelected) 5.5.dp.toPx() else 3.5.dp.toPx(),
                                        center = pt
                                    )
                                    drawCircle(
                                        color = canvasSurfaceColor,
                                        radius = if (isSelected) 3.dp.toPx() else 1.5.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }

                            "CUMULATIVE_AREA" -> {
                                var cumVal = 0.0
                                val pointsCum = chartData.mapIndexed { i, day ->
                                    val netDay = if (day.incomeAmount > 0) (day.incomeAmount - day.expenseAmount) else (day.savingsAmount - day.expenseAmount)
                                    cumVal += netDay
                                    val safeCum = cumVal.coerceAtLeast(0.0)
                                    val x = i * slotWidth + slotWidth / 2
                                    val ratio = ((safeCum / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)
                                    Offset(x, canvasHeight - (canvasHeight * ratio))
                                }

                                if (pointsCum.size >= 2) {
                                    val pathCum = Path().apply {
                                        moveTo(pointsCum.first().x, pointsCum.first().y)
                                        for (i in 0 until pointsCum.size - 1) {
                                            val p0 = pointsCum[i]
                                            val p1 = pointsCum[i + 1]
                                            val cx = (p0.x + p1.x) / 2
                                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                        }
                                    }
                                    val fillCum = Path().apply {
                                        addPath(pathCum)
                                        lineTo(pointsCum.last().x, canvasHeight)
                                        lineTo(pointsCum.first().x, canvasHeight)
                                        close()
                                    }
                                    drawPath(
                                        path = fillCum,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(AccentIncome.copy(alpha = 0.4f), Color.Transparent),
                                            startY = 0f,
                                            endY = canvasHeight
                                        )
                                    )
                                    drawPath(
                                        path = pathCum,
                                        brush = Brush.horizontalGradient(listOf(AccentIncome, AccentSavings)),
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )

                                    pointsCum.forEachIndexed { i, pt ->
                                        val isSelected = selectedIndex == i
                                        drawCircle(
                                            color = AccentIncome,
                                            radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx(),
                                            center = pt
                                        )
                                    }
                                }
                            }

                            else -> {
                                // SMOOTH BEZIER CURVE
                                val pointsSavings = mutableListOf<Offset>()
                                val pointsExpense = mutableListOf<Offset>()

                                chartData.forEachIndexed { i, day ->
                                    val x = i * slotWidth + slotWidth / 2
                                    val sRatio = ((day.savingsAmount / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)
                                    val eRatio = ((day.expenseAmount / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)
                                    pointsSavings.add(Offset(x, canvasHeight - (canvasHeight * sRatio)))
                                    pointsExpense.add(Offset(x, canvasHeight - (canvasHeight * eRatio)))
                                }

                                // Expense Curve & Area
                                if (pointsExpense.size >= 2) {
                                    val pathExpense = Path().apply {
                                        moveTo(pointsExpense.first().x, pointsExpense.first().y)
                                        for (i in 0 until pointsExpense.size - 1) {
                                            val p0 = pointsExpense[i]
                                            val p1 = pointsExpense[i + 1]
                                            val cx = (p0.x + p1.x) / 2
                                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                        }
                                    }
                                    val fillExpense = Path().apply {
                                        addPath(pathExpense)
                                        lineTo(pointsExpense.last().x, canvasHeight)
                                        lineTo(pointsExpense.first().x, canvasHeight)
                                        close()
                                    }
                                    drawPath(
                                        path = fillExpense,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(AccentExpense.copy(alpha = 0.25f), Color.Transparent),
                                            startY = 0f,
                                            endY = canvasHeight
                                        )
                                    )
                                    drawPath(
                                        path = pathExpense,
                                        color = AccentExpense,
                                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }

                                // Savings Curve & Area
                                if (pointsSavings.size >= 2) {
                                    val pathSavings = Path().apply {
                                        moveTo(pointsSavings.first().x, pointsSavings.first().y)
                                        for (i in 0 until pointsSavings.size - 1) {
                                            val p0 = pointsSavings[i]
                                            val p1 = pointsSavings[i + 1]
                                            val cx = (p0.x + p1.x) / 2
                                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                        }
                                    }
                                    val fillSavings = Path().apply {
                                        addPath(pathSavings)
                                        lineTo(pointsSavings.last().x, canvasHeight)
                                        lineTo(pointsSavings.first().x, canvasHeight)
                                        close()
                                    }
                                    drawPath(
                                        path = fillSavings,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(AccentSavings.copy(alpha = 0.35f), Color.Transparent),
                                            startY = 0f,
                                            endY = canvasHeight
                                        )
                                    )
                                    drawPath(
                                        path = pathSavings,
                                        color = AccentSavings,
                                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }

                                // Data points
                                pointsSavings.forEachIndexed { i, pt ->
                                    val isSelected = selectedIndex == i
                                    drawCircle(
                                        color = AccentSavings,
                                        radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                                        center = pt
                                    )
                                }
                                pointsExpense.forEachIndexed { i, pt ->
                                    val isSelected = selectedIndex == i
                                    drawCircle(
                                        color = AccentExpense,
                                        radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Day / Milestone Labels Row
                if (currentMode == "PREDICTIVE_SAVINGS") {
                    val points = predictiveResult.points
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        points.forEachIndexed { index, pt ->
                            val isSelected = selectedIndex == index
                            Text(
                                text = pt.dateLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected || pt.isMilestone) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) BrandPrimaryBright else if (pt.isProjected) BrandPrimaryBright.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedIndex = if (selectedIndex == index) null else index
                                    }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        chartData.forEachIndexed { index, day ->
                            val isSelected = selectedIndex == index
                            Text(
                                text = if (selectedDaysRange > 7 && index % 2 != 0) "" else day.dayName.take(3),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedIndex = if (selectedIndex == index) null else index
                                    }
                            )
                        }
                    }
                }
            }

            // Dedicated Predictive Saving Intelligence & Velocity Panel
            if (currentMode == "PREDICTIVE_SAVINGS") {
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, BrandPrimaryBright.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Timeline,
                                    contentDescription = null,
                                    tint = BrandPrimaryBright,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TARGET: ${predictiveResult.goalTitle.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.1.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                color = AccentSavings.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${predictiveResult.consistencyLabel} (${predictiveResult.consistencyScore}%)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AccentSavings,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3 Key Projection Metric Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Kecepatan Menabung
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Speed,
                                        contentDescription = null,
                                        tint = AccentSavings,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ritme Nabung",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+${FinanceViewModel.formatCompact(predictiveResult.avgDailySavings)}/hr",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                    color = AccentSavings
                                )
                                Text(
                                    text = "${FinanceViewModel.formatCompact(predictiveResult.avgWeeklySavings)}/mgg",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Sisa Kebutuhan
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Flag,
                                        contentDescription = null,
                                        tint = Color(0xFF9C9587),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sisa Butuh",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FinanceViewModel.formatCompact(predictiveResult.remainingAmount),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "dari ${FinanceViewModel.formatCompact(predictiveResult.targetAmount)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Estimasi Selesai
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = BrandPrimaryBright,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Estimasi Tuntas",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (predictiveResult.projectedDaysRemaining == 0) "Selesai 🎉" else "~${predictiveResult.projectedDaysRemaining} Hari",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                    color = BrandPrimaryBright
                                )
                                Text(
                                    text = "(${predictiveResult.projectedCompletionDateFormatted})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }

                        if (predictiveResult.acceleratedDaysSaved > 0 && predictiveResult.remainingAmount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BrandPrimaryBright.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = BrandPrimaryBright,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tips Percepat: Tambah +${FinanceViewModel.formatCompact(predictiveResult.acceleratedDailyAmount)}/hari → Selesai ${predictiveResult.acceleratedDaysSaved} hari lebih awal!",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = BrandPrimaryBright
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CategorySlice(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@Composable
fun CategoryBreakdownRingChart(
    transactions: List<TransactionEntity>,
    type: TransactionType = TransactionType.EXPENSE,
    modifier: Modifier = Modifier
) {
    val filteredTx = remember(transactions, type) {
        transactions.filter { it.type == type }
    }

    val totalAmount = remember(filteredTx) {
        filteredTx.sumOf { it.amount }
    }

    val slices = remember(filteredTx, totalAmount) {
        if (totalAmount <= 0) emptyList()
        else {
            val palette = listOf(
                BrandPrimaryBright,
                Color(0xFFB08D6B), // Muted terracotta-tan
                Color(0xFF8A8579), // Warm taupe-gray
                Color(0xFFA3956B), // Muted olive-gold
                Color(0xFF7A9187), // Muted sage
                Color(0xFF9C7F8A)  // Muted mauve
            )
            val grouped = filteredTx.groupBy { it.category }
                .mapValues { it.value.sumOf { t -> t.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)

            val topTotal = grouped.sumOf { it.second }
            val otherTotal = totalAmount - topTotal

            val list = mutableListOf<CategorySlice>()
            grouped.forEachIndexed { i, (cat, amt) ->
                list.add(
                    CategorySlice(
                        category = cat,
                        amount = amt,
                        percentage = (amt / totalAmount).toFloat(),
                        color = palette[i % palette.size]
                    )
                )
            }
            if (otherTotal > 0) {
                list.add(
                    CategorySlice(
                        category = "Lainnya",
                        amount = otherTotal,
                        percentage = (otherTotal / totalAmount).toFloat(),
                        color = Color(0xFF8C8578)
                    )
                )
            }
            list
        }
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }

    MonochromeCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISTRIBUSI ${if (type == TransactionType.EXPENSE) "PENGELUARAN" else "PEMASUKAN"}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${slices.size} Kategori",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (slices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada transaksi ${if (type == TransactionType.EXPENSE) "pengeluaran" else "pemasukan"}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern Donut Chart Canvas with Rounded Arc Strokes
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val arcSize = size.width - strokeWidth
                            var startAngle = -90f

                            // Base track
                            drawArc(
                                color = Color(0xFF2E2E3E).copy(alpha = 0.4f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            slices.forEach { slice ->
                                val sweep = slice.percentage * 360f * animProgress.value
                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle,
                                    sweepAngle = (sweep - 2f).coerceAtLeast(0f),
                                    useCenter = false,
                                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                startAngle += sweep
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinanceViewModel.formatCompact(totalAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Breakdown List with mini progress tracks
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slices.forEach { slice ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(slice.color)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = slice.category,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = "${(slice.percentage * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = slice.color
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                // Mini Progress Track
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(slice.percentage * animProgress.value)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(slice.color)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialDisciplineGauge(
    savingsRatio: Float,
    disciplineScore: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (disciplineScore / 100f).coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "discipline_gauge"
    )

    val gaugeColor = when {
        disciplineScore >= 80 -> AccentIncome
        disciplineScore >= 60 -> AccentSavings
        disciplineScore >= 40 -> Color(0xFFC4914F)
        else -> AccentExpense
    }

    MonochromeCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(gaugeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INDEKS KESEHATAN KEUANGAN",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        disciplineScore >= 80 -> "Master Saver • Sangat Konsisten"
                        disciplineScore >= 60 -> "Finansial Stabil & Terkendali"
                        disciplineScore >= 40 -> "Waspada • Tingkatkan Tabungan"
                        else -> "Perhatian Khusus • Bocor Halus"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rasio simpanan tabungan ${(savingsRatio * 100).toInt()}% dari seluruh arus kas masuk.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Modern Radial Meter
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                val outlineColor = MaterialTheme.colorScheme.outlineVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    val arcSize = size.width - stroke

                    // Background Track
                    drawArc(
                        color = outlineColor.copy(alpha = 0.4f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    // Active Score Arc with Gradient
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(AccentExpense, Color(0xFFC4914F), AccentSavings, AccentIncome)
                        ),
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$disciplineScore",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsGoalProjectionLineChart(
    currentAmount: Double,
    targetAmount: Double,
    fillPlanAmount: Double,
    fillPlanFrequency: String,
    activeDays: Set<Int>,
    modifier: Modifier = Modifier
) {
    val schedule = remember(currentAmount, targetAmount, fillPlanAmount, fillPlanFrequency, activeDays) {
        FinanceViewModel.calculateGoalSchedule(
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            fillPlanAmount = fillPlanAmount,
            fillPlanFrequency = fillPlanFrequency,
            activeDays = activeDays
        )
    }

    val progressPercent = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toInt().coerceIn(0, 100) else 0

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(currentAmount, targetAmount) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentSavings)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PROYEKSI TARGET & ESTIMASI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = AccentSavings.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "$progressPercent% Tercapai",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = AccentSavings,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mini Step Line Graph with glowing milestones
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val gridColor = MaterialTheme.colorScheme.outlineVariant

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height - 12.dp.toPx()

                // Baseline and Target guides
                drawLine(
                    color = gridColor.copy(alpha = 0.4f),
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = AccentSavings.copy(alpha = 0.5f),
                    start = Offset(0f, 0f),
                    end = Offset(w, 0f),
                    strokeWidth = 1.dp.toPx()
                )

                val ratioNow = if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                val effectiveRatio = ratioNow * animProgress.value

                val currentX = w * effectiveRatio
                val currentY = h - (h * effectiveRatio)

                // Fill area under current progress
                val currentPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(currentX, currentY)
                    lineTo(currentX, h)
                    close()
                }
                drawPath(
                    path = currentPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(AccentSavings.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = h
                    )
                )

                // Solid Line for current progress
                drawLine(
                    color = AccentSavings,
                    start = Offset(0f, h),
                    end = Offset(currentX, currentY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Projected Dashed Line from Current to Target 100%
                if (effectiveRatio < 1f) {
                    drawLine(
                        color = BrandPrimaryBright.copy(alpha = 0.7f),
                        start = Offset(currentX, currentY),
                        end = Offset(w, 0f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Node marker at current position
                drawCircle(
                    color = AccentSavings,
                    radius = 5.dp.toPx(),
                    center = Offset(currentX, currentY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(currentX, currentY)
                )

                // Target finish flag node
                drawCircle(
                    color = BrandPrimaryBright,
                    radius = 4.dp.toPx(),
                    center = Offset(w - 2.dp.toPx(), 0f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Schedule summary text
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Timeline,
                    contentDescription = null,
                    tint = AccentSavings,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = schedule.summaryText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
