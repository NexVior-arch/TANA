package com.tana.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.tana.R
import com.tana.data.model.SavingsGoalEntity
import com.tana.data.model.TransactionEntity
import com.tana.ui.components.AnimatedAmountText
import com.tana.ui.components.GradientButton
import com.tana.ui.components.MonochromeCard
import com.tana.ui.components.SavingsGoalDetailDialog
import com.tana.ui.components.SavingsGoalProjectionLineChart
import com.tana.ui.components.SectionHeader
import com.tana.ui.components.ThousandsSeparatorVisualTransformation
import com.tana.ui.components.animatedSpecularGlow
import com.tana.ui.theme.AccentExpense
import com.tana.ui.theme.AccentIncome
import com.tana.ui.theme.AccentSavings
import com.tana.ui.theme.BrandPrimaryBright
import com.tana.ui.theme.CyanButtonGradient
import com.tana.ui.theme.RoseButtonGradient
import com.tana.ui.theme.PrimaryButtonGradient
import com.tana.ui.theme.DarkButtonGradient
import com.tana.ui.theme.DisabledButtonGradient
import com.tana.ui.viewmodel.FinanceViewModel

@Composable
fun SavingsScreen(
    savingsGoals: List<SavingsGoalEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    onAddGoal: (
        title: String,
        targetAmount: Double,
        initialAmount: Double,
        fillPlanAmount: Double,
        fillPlanFrequency: String,
        activeDays: Set<Int>,
        reminderSettings: String,
        note: String
    ) -> Unit,
    onDepositGoal: (goalId: Long, amount: Double, note: String) -> Unit,
    onWithdrawGoal: (goalId: Long, amount: Double, note: String) -> Unit = { _, _, _ -> },
    onDeleteGoal: (Long) -> Unit,
    onUpdateGoal: (SavingsGoalEntity) -> Unit = {},
    onUpdateSavingsTransaction: (TransactionEntity, Double, String, Long) -> Unit = { _, _, _, _ -> },
    onDeleteSavingsTransaction: (Long, Long?) -> Unit = { _, _ -> },
    errorMessage: String? = null,
    onErrorShown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
            onErrorShown()
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var depositGoalTargetId by remember { mutableStateOf<Long?>(null) }
    val depositGoalTarget = remember(savingsGoals, depositGoalTargetId) {
        if (depositGoalTargetId != null) savingsGoals.find { it.id == depositGoalTargetId } else null
    }
    var depositIsSetorMode by remember { mutableStateOf(true) }

    var selectedGoalId by remember { mutableStateOf<Long?>(null) }
    val selectedGoalForDetail = remember(savingsGoals, selectedGoalId) {
        if (selectedGoalId != null) savingsGoals.find { it.id == selectedGoalId } else null
    }

    var goalToDeleteId by remember { mutableStateOf<Long?>(null) }
    val goalToDelete = remember(savingsGoals, goalToDeleteId) {
        if (goalToDeleteId != null) savingsGoals.find { it.id == goalToDeleteId } else null
    }

    var pendingCelebrationGoalTitle by remember { mutableStateOf<String?>(null) }
    var pendingCelebrationGoalId by remember { mutableStateOf<Long?>(null) }
    var celebrationGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    LaunchedEffect(savingsGoals, pendingCelebrationGoalTitle, pendingCelebrationGoalId) {
        if (pendingCelebrationGoalTitle != null) {
            val found = savingsGoals.find { it.title.equals(pendingCelebrationGoalTitle, ignoreCase = true) }
            if (found != null && found.currentAmount >= found.targetAmount && found.targetAmount > 0) {
                celebrationGoal = found
                pendingCelebrationGoalTitle = null
            }
        }
        if (pendingCelebrationGoalId != null) {
            val found = savingsGoals.find { it.id == pendingCelebrationGoalId }
            if (found != null && found.currentAmount >= found.targetAmount && found.targetAmount > 0) {
                celebrationGoal = found
                pendingCelebrationGoalId = null
            }
        }
    }

    val activeGoals = remember(savingsGoals) { savingsGoals.filter { !it.isCompleted } }
    val completedGoals = remember(savingsGoals) { savingsGoals.filter { it.isCompleted } }

    val totalSavedInGoals = remember(savingsGoals) { savingsGoals.sumOf { it.currentAmount } }
    val totalTargetGoals = remember(savingsGoals) { savingsGoals.sumOf { it.targetAmount } }
    val overallProgress = remember(totalSavedInGoals, totalTargetGoals) {
        if (totalTargetGoals > 0) (totalSavedInGoals / totalTargetGoals).toFloat().coerceIn(0f, 1f) else 0f
    }

    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")) }

    // Create Goal Dialog
    if (showCreateDialog) {
        AddGoalDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { title, target, init, planAmt, planFreq, activeDays, reminder, note ->
                onAddGoal(title, target, init, planAmt, planFreq, activeDays, reminder, note)
                showCreateDialog = false
                if (init >= target && target > 0) {
                    pendingCelebrationGoalTitle = title
                }
            }
        )
    }

    // Goal Completion Celebration Dialog
    if (celebrationGoal != null) {
        val completedGoal = celebrationGoal!!
        Dialog(
            onDismissRequest = { celebrationGoal = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { celebrationGoal = null },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AccentIncome.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = AccentIncome,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "🎉 SELAMAT! TARGET TERCAPAI!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = completedGoal.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentSavings,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Selamat! Anda telah berhasil mengumpulkan ${FinanceViewModel.formatRupiah(completedGoal.targetAmount)}! Target tabungan Anda telah 100% tuntas terisi.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { celebrationGoal = null },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Tutup", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val currentG = celebrationGoal
                                    celebrationGoal = null
                                    if (currentG != null) {
                                        selectedGoalId = currentG.id
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f).height(48.dp)
                            ) {
                                Text("Lihat Detail", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Deposit / Withdraw Goal Dialog
    if (depositGoalTarget != null) {
        val goal = depositGoalTarget!!
        DepositToGoalDialog(
            goal = goal,
            transactions = transactions,
            initialIsSetor = depositIsSetorMode,
            onDismiss = { depositGoalTargetId = null },
            onDeposit = { amt, note ->
                val prevAmt = goal.currentAmount
                val newAmt = prevAmt + amt
                onDepositGoal(goal.id, amt, note)
                depositGoalTargetId = null
                if (newAmt >= goal.targetAmount && goal.targetAmount > 0) {
                    pendingCelebrationGoalId = goal.id
                }
            },
            onWithdraw = { amt, note ->
                onWithdrawGoal(goal.id, amt, note)
                depositGoalTargetId = null
            }
        )
    }

    // Detail Goal Dialog (Grafik & History)
    if (selectedGoalForDetail != null) {
        val goal = selectedGoalForDetail!!
        SavingsGoalDetailDialog(
            goal = goal,
            transactions = transactions,
            onDismiss = { selectedGoalId = null },
            onOpenDeposit = {
                depositGoalTargetId = goal.id
                depositIsSetorMode = true
            },
            onOpenWithdraw = {
                depositGoalTargetId = goal.id
                depositIsSetorMode = false
            },
            onDeleteGoal = {
                goalToDeleteId = goal.id
                selectedGoalId = null
            },
            onUpdateGoal = { updated ->
                onUpdateGoal(updated)
            },
            onUpdateSavingsTransaction = onUpdateSavingsTransaction,
            onDeleteSavingsTransaction = onDeleteSavingsTransaction
        )
    }

    // Delete Confirmation Dialog
    if (goalToDelete != null) {
        val goal = goalToDelete!!
        Dialog(
            onDismissRequest = { goalToDeleteId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { goalToDeleteId = null },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(AccentExpense.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = AccentExpense,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Hapus Target Tabungan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Apakah Anda yakin ingin menghapus target \"${goal.title}\"?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { goalToDeleteId = null },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Batal", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    onDeleteGoal(goal.id)
                                    goalToDeleteId = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentExpense),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("confirm_delete_goal_button")
                            ) {
                                Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TARGET TABUNGAN",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Rencana otomatis & proyeksi waktu selesai",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("create_new_goal_button")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Target Baru", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Overall Savings Summary Glass Card with Artistic Asset
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0x668B5CF6),
                                    Color(0x336366F1),
                                    Color(0x10FFFFFF)
                                )
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                // Background artistic asset
                Image(
                    painter = painterResource(id = R.drawable.img_savings_art),
                    contentDescription = "Savings Vault Art",
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )

                // Atmospheric Scrim
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xEA16143A),
                                    Color(0xF50E0C28),
                                    Color(0xFC08071C)
                                )
                            )
                        )
                        .animatedSpecularGlow(
                            glowColor = Color(0xFF818CF8),
                            glowAlpha = 0.28f,
                            durationMillis = 4000
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL DANA TABUNGAN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.4.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFB2AEE0)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedAmountText(
                                targetAmount = totalSavedInGoals,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentSavings
                            )
                        }

                        Surface(
                            color = AccentSavings.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.5.dp, AccentSavings.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${(overallProgress * 100).toInt()}% Target",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = AccentSavings,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target Akumulasi: ${FinanceViewModel.formatRupiah(totalTargetGoals)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFFB2AEE0)
                        )
                        Text(
                            text = "${activeGoals.size} Aktif • ${completedGoals.size} Selesai",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // High contrast progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1C1A38))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(overallProgress)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(AccentSavings, Color(0xFF818CF8))
                                    )
                                )
                        )
                    }
                }
            }
        }

        // Active Goals Section
        item {
            SectionHeader(title = "Target Berjalan (${activeGoals.size})")
        }

        if (activeGoals.isEmpty()) {
            item {
                MonochromeCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
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
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Target Tabungan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tekan 'Target Baru' di atas untuk memulai disiplin menabung otomatis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(activeGoals, key = { it.id }) { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                val activeDaysSet = remember(goal.activeDays) { FinanceViewModel.parseActiveDays(goal.activeDays) }

                MonochromeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedGoalId = goal.id },
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GoalProgressRing(
                                    progress = progress,
                                    size = 44.dp,
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = goal.title,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    if (goal.note.isNotBlank()) {
                                        Text(
                                            text = goal.note,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { goalToDeleteId = goal.id },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "TERKUMPUL",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FinanceViewModel.formatRupiah(goal.currentAmount),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = AccentSavings
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "TARGET: ${FinanceViewModel.formatRupiah(goal.targetAmount)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Kurang ${FinanceViewModel.formatRupiah(remainingAmount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // PROJECTION GRAPH & TIMELINE FORECAST
                        SavingsGoalProjectionLineChart(
                            currentAmount = goal.currentAmount,
                            targetAmount = goal.targetAmount,
                            fillPlanAmount = goal.fillPlanAmount,
                            fillPlanFrequency = goal.fillPlanFrequency,
                            activeDays = activeDaysSet,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action row with Setor & Tarik Tabungan buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    depositGoalTargetId = goal.id
                                    depositIsSetorMode = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("deposit_goal_${goal.id}")
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "+ Setor", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = {
                                    depositGoalTargetId = goal.id
                                    depositIsSetorMode = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("withdraw_goal_${goal.id}")
                            ) {
                                Icon(imageVector = Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "- Tarik", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        // Completed Goals Section
        if (completedGoals.isNotEmpty()) {
            item {
                SectionHeader(title = "Target Selesai (${completedGoals.size})")
            }

            items(completedGoals, key = { it.id }) { goal ->
                val activeDaysSet = remember(goal.activeDays) { FinanceViewModel.parseActiveDays(goal.activeDays) }

                MonochromeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedGoalId = goal.id },
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Congratulations Badge & Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = AccentIncome.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, AccentIncome.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = AccentIncome,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "🎉 Target 100% Tercapai!",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = AccentIncome
                                    )
                                }
                            }

                            IconButton(
                                onClick = { goalToDeleteId = goal.id },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TOTAL DANA TERKUMPUL",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FinanceViewModel.formatRupiah(goal.currentAmount),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                    color = AccentIncome,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TARGET: ${FinanceViewModel.formatRupiah(goal.targetAmount)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Projection Chart
                        SavingsGoalProjectionLineChart(
                            currentAmount = goal.currentAmount,
                            targetAmount = goal.targetAmount,
                            fillPlanAmount = goal.fillPlanAmount,
                            fillPlanFrequency = goal.fillPlanFrequency,
                            activeDays = activeDaysSet,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Cek Histori Only (Tercapai)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { selectedGoalId = goal.id },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.AutoGraph, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Cek Histori", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}@Composable
private fun GoalProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 4.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "goal_ring_progress"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(AccentSavings, BrandPrimaryBright, AccentSavings)),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
            )
        }
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        targetAmount: Double,
        initialAmount: Double,
        fillPlanAmount: Double,
        fillPlanFrequency: String,
        activeDays: Set<Int>,
        reminderSettings: String,
        note: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var initialText by remember { mutableStateOf("") }
    var planAmountText by remember { mutableStateOf("10000") }
    var planFrequency by remember { mutableStateOf("DAILY") } // DAILY, WEEKLY, MONTHLY
    var activeDays by remember { mutableStateOf(setOf(2, 3, 4, 5, 6, 7, 1)) } // Mon..Sun default
    var enableReminder by remember { mutableStateOf(true) }
    var reminderHour by remember { mutableStateOf("12") }
    var reminderMinute by remember { mutableStateOf("00") }
    var note by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showCurrencyMenu by remember { mutableStateOf(false) }
    var selectedCurrencyName by remember { mutableStateOf("Indonesia Rupiah ( Rp )") }

    var showCalculator by remember { mutableStateOf(false) }
    var calcDaysText by remember { mutableStateOf("30") }
    var showTimePicker by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val targetNum = targetText.toDoubleOrNull() ?: 0.0
    val initialNum = initialText.toDoubleOrNull() ?: 0.0
    val planAmtNum = planAmountText.toDoubleOrNull() ?: 10000.0

    // Dynamic Live Schedule Calculation
    val schedule = remember(targetNum, initialNum, planAmtNum, planFrequency, activeDays) {
        FinanceViewModel.calculateGoalSchedule(
            targetAmount = targetNum,
            currentAmount = initialNum,
            fillPlanAmount = planAmtNum,
            fillPlanFrequency = planFrequency,
            activeDays = activeDays
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = {
                                val reminderStr = if (enableReminder) "${reminderHour.padStart(2, '0')}:${reminderMinute.padStart(2, '0')}" else "OFF"
                                val imgStr = selectedImageUri?.toString() ?: ""
                                val finalNote = if (imgStr.isNotEmpty()) {
                                    if (note.isBlank()) "IMG_URI:$imgStr" else "$note\nIMG_URI:$imgStr"
                                } else note

                                onSave(
                                    title.trim(),
                                    targetNum,
                                    initialNum,
                                    planAmtNum,
                                    planFrequency,
                                    activeDays,
                                    reminderStr,
                                    finalNote.trim()
                                )
                            },
                            enabled = title.isNotBlank() && targetNum > 0 && planAmtNum > 0,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("save_goal_button")
                        ) {
                            Text(
                                text = "Simpan",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Field: Nama Tabungan
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Tabungan") },
                    placeholder = { Text("Misal: Beli Laptop, Liburan, Dana Darurat") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("goal_title_input")
                )

                // Field: Target Tabungan
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) targetText = it },
                    label = { Text("Target Tabungan") },
                    placeholder = { Text("1000000") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("goal_target_input")
                )

                // Field: Mata Uang Dropdown Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCurrencyName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mata Uang") },
                        leadingIcon = {
                            Text(
                                text = "🇮🇩",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showCurrencyMenu = true }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Pilih Mata Uang")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCurrencyMenu = true }
                    )

                    DropdownMenu(
                        expanded = showCurrencyMenu,
                        onDismissRequest = { showCurrencyMenu = false }
                    ) {
                        listOf(
                            "Indonesia Rupiah ( Rp )",
                            "US Dollar ( $ )",
                            "Euro ( € )",
                            "Japanese Yen ( ¥ )",
                            "Malaysian Ringgit ( RM )",
                            "Singapore Dollar ( S$ )"
                        ).forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr) },
                                onClick = {
                                    selectedCurrencyName = curr
                                    showCurrencyMenu = false
                                }
                            )
                        }
                    }
                }

                // Field: Saldo Awal (Opsional)
                OutlinedTextField(
                    value = initialText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) initialText = it },
                    label = { Text("Saldo Awal (Opsional)") },
                    placeholder = { Text("0") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("goal_initial_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Section: Rencana Pengisian
                Text(
                    text = "Rencana Pengisian",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Segmented Control: [ Harian | Mingguan | Bulanan ]
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val frequencies = listOf(
                            "DAILY" to "Harian",
                            "WEEKLY" to "Mingguan",
                            "MONTHLY" to "Bulanan"
                        )
                        frequencies.forEach { (freqKey, label) ->
                            val isSelected = planFrequency == freqKey
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { planFrequency = freqKey }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }

                // Nominal Pengisian Input with Calendar Icon Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = planAmountText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) planAmountText = it },
                        label = { Text("Nominal Pengisian") },
                        placeholder = { Text("10000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_plan_amount_input")
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { showCalculator = !showCalculator }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Kalkulator Target",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Interactive Target Calculator Link
                TextButton(
                    onClick = { showCalculator = !showCalculator },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Masih bingung mengisi rencana pengisian?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Hitung Dengan Kalkulator Target",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Calculator Panel (Expandable)
                if (showCalculator) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "KALKULATOR TARGET TABUNGAN",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "Pilih durasi target yang Anda inginkan:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val presetDays = listOf(30 to "1 Bulan", 60 to "2 Bulan", 90 to "3 Bulan", 180 to "6 Bulan", 365 to "1 Tahun")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(presetDays) { (days, label) ->
                                    val isSel = calcDaysText == days.toString()
                                    Surface(
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { calcDaysText = days.toString() }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            val numDays = calcDaysText.toDoubleOrNull() ?: 30.0
                            val neededPerDay = if (targetNum > 0) ((targetNum - initialNum).coerceAtLeast(0.0) / numDays) else 0.0
                            val roundedNeeded = (Math.ceil(neededPerDay / 1000.0) * 1000.0)

                            if (targetNum > 0) {
                                Text(
                                    text = "Rekomendasi Setoran: ${FinanceViewModel.formatRupiah(roundedNeeded)} / hari",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AccentSavings
                                )
                                Button(
                                    onClick = {
                                        planAmountText = roundedNeeded.toLong().toString()
                                        planFrequency = "DAILY"
                                        showCalculator = false
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Gunakan Nominal Ini", style = MaterialTheme.typography.labelMedium)
                                }
                            } else {
                                Text(
                                    text = "Isi Target Tabungan terlebih dahulu untuk menghitung.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section: Notifikasi
                Text(
                    text = "Notifikasi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Clock & Toggle Switch Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showTimePicker = !showTimePicker }
                            ) {
                                val timeFormatted = "${reminderHour.padStart(2, '0')}:${reminderMinute.padStart(2, '0')}"
                                Text(
                                    text = timeFormatted,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 36.sp
                                    ),
                                    color = if (enableReminder) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Jam",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Switch(
                                checked = enableReminder,
                                onCheckedChange = { enableReminder = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        if (showTimePicker) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = reminderHour,
                                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) reminderHour = it },
                                    label = { Text("Jam") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(":", style = MaterialTheme.typography.titleLarge)
                                OutlinedTextField(
                                    value = reminderMinute,
                                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) reminderMinute = it },
                                    label = { Text("Menit") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Days Chips Selector
                        val dayList = listOf(
                            1 to "Minggu",
                            2 to "Senin",
                            3 to "Selasa",
                            4 to "Rabu",
                            5 to "Kamis",
                            6 to "Jumat",
                            7 to "Sabtu"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dayList) { (dayCal, dayLabel) ->
                                val isActive = activeDays.contains(dayCal)
                                Surface(
                                    color = if (isActive && enableReminder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (isActive && enableReminder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.clickable {
                                        if (enableReminder) {
                                            if (isActive) {
                                                if (activeDays.size > 1) activeDays = activeDays - dayCal
                                            } else {
                                                activeDays = activeDays + dayCal
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = dayLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isActive && enableReminder) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isActive && enableReminder) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // DYNAMIC ESTIMATION PREVIEW CARD
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AccentSavings.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Timeline,
                                contentDescription = null,
                                tint = AccentSavings,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ESTIMASI OTOMATIS APK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AccentSavings
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (targetNum > 0 && planAmtNum > 0) {
                            val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
                            val targetDateFormatted = dateFormat.format(Date(schedule.projectedCompletionMillis))
                            Text(
                                text = "Selesai dalam ${schedule.totalCalendarDays} hari ($targetDateFormatted)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ritme: ${schedule.timesCount}x pengisian rutin",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Masukkan target dan nominal setoran untuk kalkulasi otomatis.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Catatan / Motivasi Input Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan / Motivasi (Opsional)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("goal_note_input")
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DepositToGoalDialog(
    goal: SavingsGoalEntity,
    transactions: List<TransactionEntity>,
    initialIsSetor: Boolean = true,
    onDismiss: () -> Unit,
    onDeposit: (amount: Double, note: String) -> Unit,
    onWithdraw: (amount: Double, note: String) -> Unit
) {
    val isGoalAchieved = goal.currentAmount >= goal.targetAmount && goal.targetAmount > 0
    var isSetorMode by remember { mutableStateOf(if (isGoalAchieved) false else initialIsSetor) }
    val focusRequester = remember { FocusRequester() }
    var amountTfv by remember { mutableStateOf(TextFieldValue("")) }
    var note by remember(isSetorMode) {
        mutableStateOf("")
    }
    // Prevents double-tap / double-submit from firing two deposits or withdrawals
    // for a single user action (e.g. finger bounce, UI lag before dialog closes).
    var isSubmitting by remember { mutableStateOf(false) }

    val goalTransactions = remember(transactions, goal.id) {
        transactions.filter { 
            it.goalId == goal.id || 
            (it.category.contains(goal.title, ignoreCase = true) && it.type == com.tana.data.model.TransactionType.SAVINGS) 
        }.sortedByDescending { it.timestamp }
    }

    // Dynamic quick chips: user's past deposited amounts for this goal, all savings transactions, accumulated with plan amount
    val presetChips = remember(goalTransactions, transactions, goal.fillPlanAmount) {
        val historicalGoal = goalTransactions.map { kotlin.math.abs(it.amount).toLong() }.filter { it > 0 }
        val historicalAll = transactions.filter { it.type == com.tana.data.model.TransactionType.SAVINGS && it.amount != 0.0 }
            .map { kotlin.math.abs(it.amount).toLong() }
            .filter { it > 0 }
        val combined = mutableListOf<Long>()
        if (goal.fillPlanAmount > 0) {
            combined.add(goal.fillPlanAmount.toLong())
        }
        combined.addAll(historicalGoal)
        combined.addAll(historicalAll)
        if (combined.isNotEmpty()) {
            combined.distinct().sorted()
        } else {
            listOf(20000L, 50000L, 100000L, 200000L)
        }
    }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy • HH:mm", Locale("id", "ID")) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSetorMode) "SETOR KE TARGET" else "TARIK DARI TARGET",
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 1.4.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isSetorMode) AccentSavings else AccentExpense
                        )
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_deposit_dialog")) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(4.dp)
                ) {
                    val isAchieved = goal.currentAmount >= goal.targetAmount && goal.targetAmount > 0
                    
                    Surface(
                        color = if (isSetorMode) (if (isAchieved) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !isAchieved) { isSetorMode = true }
                    ) {
                        Text(
                            text = if (isAchieved) "Target Tercapai" else "Setor (+)",
                            fontWeight = FontWeight.Bold,
                            color = if (isAchieved) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f) else (if (isSetorMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        color = if (!isSetorMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSetorMode = false }
                    ) {
                        Text(
                            text = "Tarik (-)",
                            fontWeight = FontWeight.Bold,
                            color = if (!isSetorMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Summary info
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Terkumpul", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FinanceViewModel.formatRupiah(goal.currentAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Target Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FinanceViewModel.formatRupiah(goal.targetAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                OutlinedTextField(
                    value = amountTfv,
                    onValueChange = { tfv ->
                        if (tfv.text.all { c -> c.isDigit() }) {
                            amountTfv = tfv
                        }
                    },
                    label = { Text(if (isSetorMode) "Nominal Setoran (+)" else "Nominal Penarikan (-)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("deposit_amount_input")
                )

                // Quick history/plan chips: show user's saved amounts or 1 plan amount (scrollable)
                if (presetChips.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetChips) { chipAmt ->
                            val isSelected = amountTfv.text == chipAmt.toString()
                            Surface(
                                color = if (isSelected) AccentSavings.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 0.5.dp,
                                    if (isSelected) AccentSavings else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.clickable {
                                    val newText = chipAmt.toString()
                                    amountTfv = TextFieldValue(
                                        text = newText,
                                        selection = TextRange(newText.length)
                                    )
                                    try {
                                        focusRequester.requestFocus()
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Text(
                                    text = FinanceViewModel.formatRupiah(chipAmt.toDouble()),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) AccentSavings else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isSetorMode) "Catatan Setoran (Opsional)" else "Catatan Penarikan (Opsional)") },
                    placeholder = { Text("Keterangan catatan tabungan...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("deposit_note_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                val amountNum = amountTfv.text.toDoubleOrNull() ?: 0.0
                // No balance-cap validation on withdraw: this is a savings target
                // tracker, not a bank ATM. The user can record a withdrawal larger
                // than the current tracked balance if that's what actually happened.
                val isEnabled = amountNum > 0 && !isSubmitting

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("BATAL", style = MaterialTheme.typography.labelLarge)
                    }

                    GradientButton(
                        onClick = {
                            if (!isSubmitting) {
                                isSubmitting = true
                                if (isSetorMode) {
                                    onDeposit(amountNum, note.trim())
                                } else {
                                    onWithdraw(amountNum, note.trim())
                                }
                                onDismiss()
                            }
                        },
                        enabled = isEnabled,
                        gradient = if (isSetorMode) CyanButtonGradient else RoseButtonGradient,
                        disabledGradient = DisabledButtonGradient,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(2f).height(48.dp).testTag("confirm_deposit_button")
                    ) {
                        Text(
                            text = if (isSetorMode) "SETOR SEKARANG" else "TARIK SEKARANG",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // TRANSACTION HISTORY FOR THIS GOAL
                if (goalTransactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "HISTORI SETOR & TARIK TARGET INI (${goalTransactions.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        goalTransactions.take(5).forEach { tx ->
                            val isDeposit = tx.amount >= 0
                            val badgeColor = if (isDeposit) com.tana.ui.theme.AccentIncome else AccentExpense

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = badgeColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (tx.note.isNotBlank()) tx.note else if (isDeposit) "Setoran Tabungan" else "Penarikan Tabungan",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = dateFormat.format(Date(tx.timestamp)),
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${if (isDeposit) "+" else ""}${FinanceViewModel.formatRupiah(tx.amount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                                        color = badgeColor,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        
                        if (goalTransactions.size > 5) {
                            Text(
                                text = "Menampilkan 5 transaksi terakhir",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}
