package com.tana.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tana.R
import com.tana.data.model.SavingsGoalEntity
import com.tana.data.model.TransactionEntity
import com.tana.data.model.TransactionType
import com.tana.ui.components.AnimatedAmountText
import com.tana.ui.components.CategoryAvatar
import com.tana.ui.components.FinancialDisciplineGauge
import com.tana.ui.components.MonochromeCard
import com.tana.ui.components.SectionHeader
import com.tana.ui.components.TransactionTypeBadge
import com.tana.ui.components.WeeklyExpenseSavingsChart
import com.tana.ui.components.animatedSpecularGlow
import com.tana.ui.components.getCategoryIcon
import com.tana.ui.theme.AccentExpense
import com.tana.ui.theme.AccentIncome
import com.tana.ui.theme.AccentSavings
import com.tana.ui.theme.BrandPrimary
import com.tana.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun DashboardScreen(
    transactions: List<TransactionEntity>,
    savingsGoals: List<SavingsGoalEntity>,
    selectedSavingsGoalId: String = "ALL",
    onSelectSavingsGoal: (String) -> Unit = {},
    selectedChartMode: String = "CRISP_LINE",
    onChartModeChanged: ((String) -> Unit)? = null,
    onOpenAddTransaction: (TransactionType) -> Unit,
    onOpenWithdrawSavings: ((goalId: Long?) -> Unit)? = null,
    onNavigateToTransactions: () -> Unit,
    onNavigateToSavings: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onQuickDepositGoal: (goalId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val totalSavings = remember(savingsGoals, transactions) {
        if (savingsGoals.isNotEmpty()) {
            val goalSavings = savingsGoals.sumOf { it.currentAmount }
            val orphanSavings = transactions.filter { it.type == TransactionType.SAVINGS && it.goalId == null }.sumOf { it.amount }
            (goalSavings + orphanSavings).coerceAtLeast(0.0)
        } else {
            transactions.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amount }.coerceAtLeast(0.0)
        }
    }
    val totalTargetSavings = remember(savingsGoals) {
        savingsGoals.sumOf { it.targetAmount }
    }

    // Identify active selected goal
    val selectedGoal = remember(savingsGoals, selectedSavingsGoalId) {
        if (selectedSavingsGoalId == "ALL") null
        else savingsGoals.find { it.id.toString() == selectedSavingsGoalId }
    }

    val displayedSavingsAmount = remember(selectedGoal, totalSavings) {
        selectedGoal?.currentAmount ?: totalSavings
    }

    val displayedTargetAmount = remember(selectedGoal, totalTargetSavings) {
        selectedGoal?.targetAmount ?: totalTargetSavings
    }

    val displayedProgress = remember(displayedSavingsAmount, displayedTargetAmount) {
        if (displayedTargetAmount > 0) (displayedSavingsAmount / displayedTargetAmount).toFloat().coerceIn(0f, 1f)
        else 0f
    }

    val remainingNeeded = remember(displayedSavingsAmount, displayedTargetAmount) {
        (displayedTargetAmount - displayedSavingsAmount).coerceAtLeast(0.0)
    }

    var isDropdownOpen by remember { mutableStateOf(false) }

    val disciplineScore = remember(totalIncome, totalExpense, totalSavings) {
        if (totalIncome <= 0) 50
        else {
            val savingsRate = (totalSavings / totalIncome) * 100
            val expenseRate = (totalExpense / totalIncome) * 100
            val base = 50 + (savingsRate * 1.2) - (if (expenseRate > 70) (expenseRate - 70) * 1.5 else 0.0)
            base.toInt().coerceIn(10, 99)
        }
    }

    val savingsRatio = remember(totalIncome, totalSavings) {
        if (totalIncome > 0) (totalSavings / totalIncome).toFloat().coerceIn(0f, 1f) else 0.2f
    }

    val activeGoals = remember(savingsGoals) { savingsGoals.filter { !it.isCompleted }.take(3) }
    val recentTransactions = remember(transactions) { transactions.take(5) }
    val dateFormat = remember { SimpleDateFormat("d MMM, HH:mm", Locale("id", "ID")) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // Modern Neobank Executive Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(BrandPrimary, Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wallet,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "FINSAVE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AccentIncome)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Smart Wealth & Savings Tracker",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                        .testTag("dashboard_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Pengaturan",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bespoke Atmospheric Pure Savings Overview Card with Artistic Hero Imagery & Goal Dropdown
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0x668B5CF6),
                                    Color(0x226366F1),
                                    Color(0x10FFFFFF)
                                )
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
            ) {
                // Background artistic asset
                Image(
                    painter = painterResource(id = R.drawable.img_dashboard_hero),
                    contentDescription = "Atmospheric Hero Art",
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(26.dp)),
                    contentScale = ContentScale.Crop
                )

                // Scrim Frosted Overlay for pristine readability
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xD9161335),
                                    Color(0xF00B0A22),
                                    Color(0xFA07061A)
                                )
                            )
                        )
                        .animatedSpecularGlow(
                            glowColor = Color(0xFF818CF8),
                            glowAlpha = 0.35f,
                            durationMillis = 3600
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header row with Dropdown selector
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
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "TOTAL SALDO TABUNGAN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.6.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color(0xFFB2AEE0)
                            )
                        }

                        // Dropdown Selector Pill
                        Box {
                            Surface(
                                color = Color(0xFF201D42).copy(alpha = 0.9f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(0.8.dp, Color(0xFF818CF8).copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clickable { isDropdownOpen = true }
                                    .testTag("savings_dropdown_selector")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Savings,
                                        contentDescription = null,
                                        tint = AccentSavings,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (selectedGoal != null) selectedGoal.title.take(14) else "Semua Target",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.4.sp
                                        ),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Pilih Tabungan",
                                        tint = Color(0xFFB2AEE0),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Material 3 Dropdown Menu for choosing savings target
                            DropdownMenu(
                                expanded = isDropdownOpen,
                                onDismissRequest = { isDropdownOpen = false },
                                modifier = Modifier
                                    .background(Color(0xFF14122C))
                                    .border(BorderStroke(1.dp, Color(0xFF322F52)), RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Semua Tabungan (Akumulasi)",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (selectedSavingsGoalId == "ALL") FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (selectedSavingsGoalId == "ALL") AccentSavings else Color.White
                                            )
                                            if (selectedSavingsGoalId == "ALL") {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = AccentSavings,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSelectSavingsGoal("ALL")
                                        isDropdownOpen = false
                                    }
                                )

                                savingsGoals.forEach { goal ->
                                    val isCurrent = selectedSavingsGoalId == goal.id.toString()
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = goal.title,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                                        ),
                                                        color = if (isCurrent) AccentSavings else Color.White
                                                    )
                                                    Text(
                                                        text = FinanceViewModel.formatRupiah(goal.currentAmount),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                                        color = Color(0xFFB2AEE0)
                                                    )
                                                }
                                                if (isCurrent) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = AccentSavings,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onSelectSavingsGoal(goal.id.toString())
                                            isDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Large Animated Amount of Selected Savings
                    AnimatedAmountText(
                        targetAmount = displayedSavingsAmount,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        testTag = "savings_balance_amount"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress bar & achievement pill
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedGoal != null) "Target: ${FinanceViewModel.formatCompact(displayedTargetAmount)}" else "Total Target: ${FinanceViewModel.formatCompact(displayedTargetAmount)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color(0xFFB2AEE0)
                            )
                            Text(
                                text = "${(displayedProgress * 100).toInt()}% Tercapai",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = AccentSavings
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { displayedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = AccentSavings,
                            trackColor = Color(0xFF29264A),
                            strokeCap = StrokeCap.Round
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Pure Savings Metric Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Target Dana
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF181638).copy(alpha = 0.85f))
                                .border(BorderStroke(0.5.dp, Color(0xFF322F52)), RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Target",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFFB2AEE0)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FinanceViewModel.formatCompact(displayedTargetAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                color = Color.White
                            )
                        }

                        // Sisa Kebutuhan
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF181638).copy(alpha = 0.85f))
                                .border(BorderStroke(0.5.dp, Color(0xFF322F52)), RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDownward,
                                    contentDescription = null,
                                    tint = AccentExpense,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sisa Butuh",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFFB2AEE0)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FinanceViewModel.formatCompact(remainingNeeded),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                color = Color.White
                            )
                        }

                        // Rencana Setoran
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF181638).copy(alpha = 0.85f))
                                .border(BorderStroke(0.5.dp, Color(0xFF322F52)), RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = AccentSavings,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Rencana",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFFB2AEE0)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val planAmt = selectedGoal?.fillPlanAmount ?: if (savingsGoals.isNotEmpty()) savingsGoals.sumOf { it.fillPlanAmount } else 0.0
                            Text(
                                text = if (planAmt > 0) FinanceViewModel.formatCompact(planAmt) else "Mandiri",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 4 Modern Savings Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Setor Tabungan
                QuickActionButton(
                    icon = Icons.Filled.Savings,
                    label = "Setor",
                    tint = AccentSavings,
                    modifier = Modifier.weight(1f),
                    testTag = "quick_add_savings_button",
                    onClick = {
                        val targetGoalId = selectedGoal?.id
                        if (targetGoalId != null) {
                            onQuickDepositGoal(targetGoalId)
                        } else {
                            onOpenAddTransaction(TransactionType.SAVINGS)
                        }
                    }
                )

                // Tarik Tabungan
                QuickActionButton(
                    icon = Icons.Filled.Remove,
                    label = "Tarik",
                    tint = AccentExpense,
                    modifier = Modifier.weight(1f),
                    testTag = "quick_withdraw_savings_button",
                    onClick = {
                        if (onOpenWithdrawSavings != null) {
                            onOpenWithdrawSavings(selectedGoal?.id)
                        } else {
                            onNavigateToSavings()
                        }
                    }
                )

                // Target Baru
                QuickActionButton(
                    icon = Icons.Filled.Flag,
                    label = "Target",
                    tint = BrandPrimary,
                    modifier = Modifier.weight(1f),
                    testTag = "quick_view_savings_button",
                    onClick = onNavigateToSavings
                )

                // Catat Arus Kas (Transaksi)
                QuickActionButton(
                    icon = Icons.Filled.CreditCard,
                    label = "Catat",
                    tint = AccentIncome,
                    modifier = Modifier.weight(1f),
                    testTag = "quick_add_transaction_button",
                    onClick = { onOpenAddTransaction(TransactionType.EXPENSE) }
                )
            }
        }

        // Financial Discipline Score Indicator
        item {
            FinancialDisciplineGauge(
                savingsRatio = savingsRatio,
                disciplineScore = disciplineScore
            )
        }

        // Visual Animated Bar, Crisp Line, Smooth Bezier, Cumulative Area, Predictive Saving Chart
        item {
            WeeklyExpenseSavingsChart(
                transactions = transactions,
                savingsGoals = savingsGoals,
                selectedSavingsGoalId = selectedSavingsGoalId,
                selectedChartMode = selectedChartMode,
                onChartModeChanged = onChartModeChanged
            )
        }

        // Active Savings Goals Section
        item {
            SectionHeader(
                title = "Target Tabungan Aktif",
                actionLabel = "Kelola Target",
                onActionClick = onNavigateToSavings
            )
        }

        if (activeGoals.isEmpty()) {
            item {
                MonochromeCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    onClick = onNavigateToSavings
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Savings,
                                contentDescription = null,
                                tint = AccentSavings,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Target Tabungan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Buat target tabungan impian untuk melatih disiplin finansial.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(activeGoals) { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                MonochromeCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    onClick = { onQuickDepositGoal(goal.id) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${FinanceViewModel.formatRupiah(goal.currentAmount)} / ${FinanceViewModel.formatRupiah(goal.targetAmount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = AccentSavings.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(0.5.dp, AccentSavings.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = AccentSavings,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Glowing Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(AccentSavings, Color(0xFF38BDF8))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions Section
        item {
            SectionHeader(
                title = "Transaksi Terkini",
                actionLabel = "Lihat Semua",
                onActionClick = onNavigateToTransactions
            )
        }

        if (recentTransactions.isEmpty()) {
            item {
                MonochromeCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Belum ada transaksi tercatat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        } else {
            items(recentTransactions) { tx ->
                MonochromeCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    cornerRadius = 16.dp,
                    contentPadding = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CategoryAvatar(
                                category = tx.category,
                                type = tx.type,
                                size = 42.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tx.category,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (tx.note.isNotBlank()) "${dateFormat.format(Date(tx.timestamp))} • ${tx.note}" else dateFormat.format(Date(tx.timestamp)),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val isWithdrawal = tx.type == TransactionType.SAVINGS && tx.amount < 0
                            val sign = when {
                                tx.type == TransactionType.EXPENSE || isWithdrawal -> "-"
                                else -> "+"
                            }
                            val color = when {
                                tx.type == TransactionType.EXPENSE || isWithdrawal -> AccentExpense
                                tx.type == TransactionType.INCOME -> AccentIncome
                                else -> AccentSavings
                            }
                            Text(
                                text = "$sign${FinanceViewModel.formatRupiah(kotlin.math.abs(tx.amount))}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 14.5.sp),
                                color = color
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            TransactionTypeBadge(type = tx.type)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp)) // Padding for bottom floating nav bar
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, com.tana.ui.theme.GlassGradientBorder),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

