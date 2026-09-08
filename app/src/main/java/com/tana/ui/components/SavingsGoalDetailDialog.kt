package com.tana.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.tana.data.model.SavingsGoalEntity
import com.tana.data.model.TransactionEntity
import com.tana.ui.theme.AccentExpense
import com.tana.ui.theme.AccentIncome
import com.tana.ui.theme.AccentSavings
import com.tana.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@Composable
fun SavingsGoalDetailDialog(
    goal: SavingsGoalEntity,
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit,
    onOpenDeposit: () -> Unit,
    onOpenWithdraw: () -> Unit,
    onDeleteGoal: () -> Unit,
    onUpdateGoal: (SavingsGoalEntity) -> Unit = {},
    onUpdateSavingsTransaction: (TransactionEntity, Double, String, Long) -> Unit = { _, _, _, _ -> },
    onDeleteSavingsTransaction: (Long, Long?) -> Unit = { _, _ -> }
) {
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var selectedTransactionForAction by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val goalTransactions = remember(transactions, goal.id) {
        transactions.filter { it.goalId == goal.id || (it.category.contains(goal.title, ignoreCase = true) && it.type == com.tana.data.model.TransactionType.SAVINGS) }
            .sortedByDescending { it.timestamp }
    }

    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
    
    val fullDateFormat = remember { SimpleDateFormat("d MMM yyyy • HH:mm", Locale("id", "ID")) }
    val shortDateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale("id", "ID")) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val createdDateStr = remember(goal.createdAt) {
        shortDateFormat.format(Date(if (goal.createdAt > 0) goal.createdAt else System.currentTimeMillis()))
    }

    // Calculate completion estimate
    val estimationText = remember(goal.currentAmount, goal.targetAmount, goal.fillPlanAmount, goal.fillPlanFrequency, goal.activeDays) {
        if (goal.isCompleted || progress >= 1f) {
            "Target Tercapai 🎉"
        } else if (goal.fillPlanAmount > 0) {
            val schedule = FinanceViewModel.calculateGoalSchedule(
                targetAmount = goal.targetAmount,
                currentAmount = goal.currentAmount,
                fillPlanAmount = goal.fillPlanAmount,
                fillPlanFrequency = goal.fillPlanFrequency,
                activeDays = FinanceViewModel.parseActiveDays(goal.activeDays)
            )
            val fullDateFmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            val targetDateStr = fullDateFmt.format(Date(schedule.projectedCompletionMillis))
            "${schedule.totalCalendarDays} Hari Lagi ($targetDateStr)"
        } else {
            "Atur Nominal Rutin"
        }
    }

    val frequencyLabel = remember(goal.fillPlanFrequency) {
        when (goal.fillPlanFrequency.uppercase()) {
            "DAILY", "PERHARI" -> "Perhari"
            "WEEKLY", "PERMINGGU" -> "Per Minggu"
            "MONTHLY", "PERBULAN" -> "Per Bulan"
            "YEARLY", "PERTAHUN" -> "Per Tahun"
            else -> "Per Hari"
        }
    }

    if (showEditGoalDialog) {
        EditGoalDialog(
            goal = goal,
            onDismiss = { showEditGoalDialog = false },
            onSave = { updatedGoal ->
                onUpdateGoal(updatedGoal)
                showEditGoalDialog = false
            }
        )
    }

    // History Item Action Popup (Ubah & Hapus - matches Screenshot 2)
    if (selectedTransactionForAction != null) {
        val tx = selectedTransactionForAction!!
        val isDeposit = tx.amount >= 0
        val txAmountFormatted = "${if (isDeposit) "+ " else "- "}${FinanceViewModel.formatRupiah(kotlin.math.abs(tx.amount))}"
        val txDateFormatted = fullDateFormat.format(Date(tx.timestamp))
        val dayNumber = remember(tx.timestamp, goal.createdAt) {
            val baseTime = if (goal.createdAt > 0) goal.createdAt else tx.timestamp
            val diffDays = ((tx.timestamp - baseTime) / 86400000L).toInt()
            if (diffDays >= 0) "Day ${diffDays + 1}" else "Day 1"
        }

        Dialog(
            onDismissRequest = { selectedTransactionForAction = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { selectedTransactionForAction = null },
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
                        // Close icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { selectedTransactionForAction = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large nominal text
                        Text(
                            text = txAmountFormatted,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp
                            ),
                            color = if (isDeposit) AccentIncome else AccentExpense
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Date & Day counter
                        Text(
                            text = txDateFormatted,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = dayNumber,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (tx.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "\"${tx.note}\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons: [ Ubah ] [ Hapus ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val toEdit = selectedTransactionForAction
                                    selectedTransactionForAction = null
                                    transactionToEdit = toEdit
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("action_edit_transaction_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ubah", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val toDel = selectedTransactionForAction
                                    selectedTransactionForAction = null
                                    transactionToDelete = toDel
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AccentExpense),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AccentExpense
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("action_delete_transaction_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentExpense)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hapus", fontWeight = FontWeight.Bold, color = AccentExpense)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Transaction Dialog
    if (transactionToEdit != null) {
        val tx = transactionToEdit!!
        CatatTabunganDialog(
            dialogTitle = "Edit Tabungan",
            initialIsSetor = tx.amount >= 0,
            initialAmount = kotlin.math.abs(tx.amount),
            initialNote = tx.note,
            historyTransactions = goalTransactions,
            onDismiss = { transactionToEdit = null },
            onSave = { isSetor, amount, note ->
                val finalAmt = if (isSetor) amount else -amount
                onUpdateSavingsTransaction(tx, finalAmt, note, tx.timestamp)
                transactionToEdit = null
            }
        )
    }

    // Delete Transaction Confirmation Dialog
    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        Dialog(
            onDismissRequest = { transactionToDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { transactionToDelete = null },
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
                            text = "Hapus Riwayat Tabungan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Apakah Anda yakin ingin menghapus catatan transaksi ${FinanceViewModel.formatRupiah(tx.amount)}? Saldo target akan disesuaikan secara otomatis.",
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
                                onClick = { transactionToDelete = null },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Batal", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    onDeleteSavingsTransaction(tx.id, goal.id)
                                    transactionToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentExpense),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Header Navigation: < Title | ✏️ 🗑️
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showEditGoalDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Catatan & Target",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDeleteGoal) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Target",
                                tint = AccentExpense
                            )
                        }
                    }
                }

                // Completion Celebration Banner
                if (goal.isCompleted || progress >= 1f) {
                    Surface(
                        color = AccentIncome.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AccentIncome.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = AccentIncome,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "🎉 TARGET TABUNGAN TERCAPAI 100%!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 14.sp),
                                    color = AccentIncome
                                )
                                Text(
                                    text = "Selamat! Anda telah berhasil mengumpulkan ${FinanceViewModel.formatRupiah(goal.targetAmount)} untuk ${goal.title}.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Main Goal Card (matches Screenshot 2)
                MonochromeCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Image banner if available
                        if (!goal.imageUri.isNullOrBlank()) {
                            AsyncImage(
                                model = goal.imageUri,
                                contentDescription = goal.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Target amount row with circular percentage pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = FinanceViewModel.formatRupiah(goal.targetAmount),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                color = if (goal.isCompleted || progress >= 1f) AccentIncome.copy(alpha = 0.15f) else AccentSavings.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (goal.isCompleted || progress >= 1f) AccentIncome.copy(alpha = 0.4f) else AccentSavings.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    ),
                                    color = if (goal.isCompleted || progress >= 1f) AccentIncome else AccentSavings,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Periodic plan nominal: e.g. "Rp5.000 Perhari"
                        if (goal.fillPlanAmount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${FinanceViewModel.formatRupiah(goal.fillPlanAmount)} $frequencyLabel",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                ),
                                color = AccentSavings
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tanggal Dibuat & Estimasi Selesai (Row)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tanggal Dibuat",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = createdDateStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1.3f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Estimasi Selesai",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = estimationText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    color = if (goal.isCompleted || progress >= 1f) AccentIncome else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(AccentSavings, AccentSavings.copy(alpha = 0.8f))
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Terkumpul: ${FinanceViewModel.formatRupiah(goal.currentAmount)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (remainingAmount > 0) "Sisa: ${FinanceViewModel.formatRupiah(remainingAmount)}" else "Lunas",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quick Action Buttons (Setor & Tarik)
                if (goal.isCompleted || progress >= 1f) {
                    Button(
                        onClick = {
                            scope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("detail_history_only_button")
                    ) {
                        Icon(imageVector = Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Histori Tabungan", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenDeposit,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("detail_deposit_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Setor (+)", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenWithdraw,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("detail_withdraw_button")
                        ) {
                            Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tarik (-)", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // SECTION: GRAFIK PERKEMBANGAN TABUNGAN
                SectionHeader(title = "GRAFIK PERKEMBANGAN TABUNGAN")

                MonochromeCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Histori Pertumbuhan Saldo",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        GoalGrowthChart(
                            transactions = goalTransactions,
                            currentAmount = goal.currentAmount,
                            targetAmount = goal.targetAmount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }

                // SECTION: RIWAYAT TRANSAKSI TABUNGAN (Matches Screenshot 2)
                SectionHeader(title = "RIWAYAT SETOR & TARIK (${goalTransactions.size})")

                if (goalTransactions.isEmpty()) {
                    MonochromeCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada riwayat transaksi setor/tarik untuk target ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    goalTransactions.forEachIndexed { index, tx ->
                        val isDeposit = tx.amount >= 0
                        val badgeColor = if (isDeposit) AccentIncome else AccentExpense
                        val dayNumber = remember(tx.timestamp, goal.createdAt) {
                            val baseTime = if (goal.createdAt > 0) goal.createdAt else tx.timestamp
                            val diffDays = ((tx.timestamp - baseTime) / 86400000L).toInt()
                            if (diffDays >= 0) "Day ${diffDays + 1}" else "Day 1"
                        }

                        MonochromeCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTransactionForAction = tx
                                },
                            backgroundColor = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = badgeColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fullDateFormat.format(Date(tx.timestamp)),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$dayNumber • ${if (tx.note.isNotBlank()) tx.note else if (isDeposit) "Setoran Tabungan" else "Penarikan Tabungan"}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Text(
                                    text = "${if (isDeposit) "+ " else "- "}${FinanceViewModel.formatRupiah(kotlin.math.abs(tx.amount))}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = badgeColor,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onSave: (newAmount: Double, newNote: String, newTimestamp: Long) -> Unit
) {
    val isDeposit = transaction.amount >= 0
    var amountStr by remember { mutableStateOf(kotlin.math.abs(transaction.amount).toLong().toString()) }
    var note by remember { mutableStateOf(transaction.note) }
    val badgeColor = if (isDeposit) AccentIncome else AccentExpense
    val parsedAmount = amountStr.toDoubleOrNull()
    val isAmountValid = parsedAmount != null && parsedAmount > 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
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
                    // Header Row: Icon + Title & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isDeposit) "Ubah Setoran Tabungan" else "Ubah Penarikan Tabungan",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Fields with Emerald / Obsidian styling
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) amountStr = it },
                            label = { Text("Nominal (Rp)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ThousandsSeparatorVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = badgeColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = badgeColor,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = badgeColor,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!isAmountValid) {
                            Text(
                                text = "Nominal harus lebih besar dari 0",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentExpense
                            )
                        }
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Catatan / Keterangan") },
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons: [ Batal ] [ Simpan Perubahan ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
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
                                if (parsedAmount != null) {
                                    val rawAmt = parsedAmount
                                    val finalAmt = if (isDeposit) rawAmt else -rawAmt
                                    onSave(finalAmt, note.trim(), transaction.timestamp)
                                }
                            },
                            enabled = isAmountValid,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = badgeColor,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalDialog(
    goal: SavingsGoalEntity,
    onDismiss: () -> Unit,
    onSave: (SavingsGoalEntity) -> Unit
) {
    var title by remember { mutableStateOf(goal.title) }
    var targetStr by remember { mutableStateOf(goal.targetAmount.toLong().toString()) }
    var planAmountStr by remember { mutableStateOf(if (goal.fillPlanAmount > 0) goal.fillPlanAmount.toLong().toString() else "") }
    var selectedFrequency by remember { mutableStateOf(goal.fillPlanFrequency.ifBlank { "DAILY" }) }
    var note by remember { mutableStateOf(goal.note) }

    val frequencies = listOf(
        "DAILY" to "Perhari (Harian)",
        "WEEKLY" to "Per Minggu (Mingguan)",
        "MONTHLY" to "Per Bulan (Bulanan)",
        "YEARLY" to "Per Tahun (Tahunan)"
    )
    var freqExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
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
                    // Header Row: Title & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentSavings.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = AccentSavings,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Edit Target Tabungan",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Nama Target") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentSavings,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = AccentSavings,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = AccentSavings,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = targetStr,
                            onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) targetStr = it },
                            label = { Text("Target Nominal (Rp)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ThousandsSeparatorVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentSavings,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = AccentSavings,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = AccentSavings,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = planAmountStr,
                            onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) planAmountStr = it },
                            label = { Text("Nominal Nabung Rutin (Rp)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ThousandsSeparatorVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Frequency Dropdown
                        ExposedDropdownMenuBox(
                            expanded = freqExpanded,
                            onExpandedChange = { freqExpanded = !freqExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = frequencies.find { it.first.equals(selectedFrequency, ignoreCase = true) }?.second ?: "Perhari (Harian)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Frekuensi Nabung") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = freqExpanded,
                                onDismissRequest = { freqExpanded = false }
                            ) {
                                frequencies.forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedFrequency = code
                                            freqExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Catatan Target") },
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
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
                                val targetAmt = targetStr.toDoubleOrNull() ?: goal.targetAmount
                                val planAmt = planAmountStr.toDoubleOrNull() ?: 0.0
                                val updated = goal.copy(
                                    title = title.trim().ifBlank { goal.title },
                                    targetAmount = targetAmt,
                                    fillPlanAmount = planAmt,
                                    fillPlanFrequency = selectedFrequency,
                                    note = note.trim(),
                                    isCompleted = goal.currentAmount >= targetAmt && targetAmt > 0
                                )
                                onSave(updated)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalGrowthChart(
    transactions: List<TransactionEntity>,
    currentAmount: Double,
    targetAmount: Double,
    modifier: Modifier = Modifier
) {
    val points = remember(transactions, currentAmount) {
        if (transactions.isEmpty()) {
            listOf(0.0, currentAmount)
        } else {
            val sorted = transactions.sortedBy { it.timestamp }
            var running = 0.0
            val list = mutableListOf(0.0)
            sorted.forEach { tx ->
                running += tx.amount
                list.add(running.coerceAtLeast(0.0))
            }
            if (list.last() != currentAmount) {
                list.add(currentAmount)
            }
            list
        }
    }

    val maxVal = remember(points, targetAmount) {
        maxOf(targetAmount, points.maxOrNull() ?: 1.0, 1.0)
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (width > 0 && height > 0 && points.size >= 2) {
            val stepX = width / (points.size - 1)
            val path = Path()
            val fillPath = Path()

            fillPath.moveTo(0f, height)

            points.forEachIndexed { i, valAmt ->
                val x = i * stepX
                val ratio = (valAmt / maxVal).toFloat().coerceIn(0f, 1f)
                val y = height - (ratio * (height - 20.dp.toPx())) - 10.dp.toPx()

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevVal = points[i - 1]
                    val prevRatio = (prevVal / maxVal).toFloat().coerceIn(0f, 1f)
                    val prevY = height - (prevRatio * (height - 20.dp.toPx())) - 10.dp.toPx()

                    val controlX1 = prevX + (stepX / 2f)
                    val controlY1 = prevY
                    val controlX2 = prevX + (stepX / 2f)
                    val controlY2 = y

                    path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                }
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(AccentSavings.copy(alpha = 0.3f), AccentSavings.copy(alpha = 0.02f))
                )
            )

            drawPath(
                path = path,
                color = AccentSavings,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
