package com.tana.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.tana.data.model.SavingsGoalEntity
import com.tana.data.model.TransactionEntity
import com.tana.data.model.TransactionType
import com.tana.ui.theme.AccentExpense
import com.tana.ui.theme.AccentSavings
import com.tana.ui.theme.GlassGradientBorder
import com.tana.ui.theme.PrimaryButtonGradient
import com.tana.ui.theme.CyanButtonGradient
import com.tana.ui.theme.RoseButtonGradient
import com.tana.ui.theme.DisabledButtonGradient
import com.tana.ui.viewmodel.FinanceViewModel

enum class NabungMode {
    SETOR,
    TARIK,
    BUAT_BARU
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NabungActionSheet(
    activeSavingsGoals: List<SavingsGoalEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onDepositGoal: (goalId: Long, amount: Double, note: String) -> Unit,
    onWithdrawGoal: (goalId: Long, amount: Double, note: String) -> Unit,
    onCreateNewGoal: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var mode by remember { mutableStateOf(NabungMode.SETOR) }
    var selectedGoal by remember(activeSavingsGoals) { mutableStateOf(activeSavingsGoals.firstOrNull()) }
    val focusRequester = remember { FocusRequester() }
    var amountTfv by remember { mutableStateOf(TextFieldValue("")) }
    var noteText by remember { mutableStateOf("") }
    var showGoalDropdown by remember { mutableStateOf(false) }

    val amountNum = amountTfv.text.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
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
                            .background(AccentSavings.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, AccentSavings.copy(alpha = 0.3f)), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = AccentSavings,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CATAT NABUNG",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.8.sp,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Setor, tarik, atau buat target baru",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Mode Selector Toggle: [ Setor (+) | Tarik (-) | + Goal Baru ]
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GlassGradientBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        NabungMode.SETOR to "Setor (+)",
                        NabungMode.TARIK to "Tarik (-)",
                        NabungMode.BUAT_BARU to "+ Target"
                    )

                    modes.forEach { (m, label) ->
                        val isSelected = mode == m
                        val activeColor = MaterialTheme.colorScheme.surface
                        val textColor = if (isSelected) {
                            if (m == NabungMode.SETOR) AccentSavings else if (m == NabungMode.TARIK) AccentExpense else MaterialTheme.colorScheme.primary
                        } else MaterialTheme.colorScheme.onSurfaceVariant

                        Surface(
                            color = if (isSelected) activeColor else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) BorderStroke(1.dp, GlassGradientBorder) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    mode = m
                                    if (m == NabungMode.BUAT_BARU) {
                                        onDismiss()
                                        onCreateNewGoal()
                                    }
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = textColor,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            if (mode != NabungMode.BUAT_BARU) {
                // Goal Selector Dropdown
                Text(
                    text = "PILIH TARGET TABUNGAN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (activeSavingsGoals.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GlassGradientBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Belum Ada Target Tabungan Aktif",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            GradientButton(
                                onClick = {
                                    onDismiss()
                                    onCreateNewGoal()
                                },
                                gradient = CyanButtonGradient,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text("+ Buat Target Tabungan Baru", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedGoal?.title ?: "Pilih Target Tabungan",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Savings, contentDescription = null, tint = AccentSavings)
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (mode == NabungMode.SETOR) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (mode == NabungMode.SETOR) AccentSavings else AccentExpense
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGoalDropdown = true }
                        )

                        DropdownMenu(
                            expanded = showGoalDropdown,
                            onDismissRequest = { showGoalDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            activeSavingsGoals.forEach { g ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(g.title, fontWeight = FontWeight.Bold)
                                            Text(
                                                FinanceViewModel.formatRupiah(g.currentAmount),
                                                color = AccentSavings,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedGoal = g
                                        showGoalDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Amount Input
                    Text(
                        text = if (mode == NabungMode.SETOR) "NOMINAL SETORAN (RP)" else "NOMINAL PENARIKAN (RP)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = amountTfv,
                        onValueChange = { tfv ->
                            if (tfv.text.all { c -> c.isDigit() }) {
                                amountTfv = tfv
                            }
                        },
                        placeholder = { Text("100000", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)) },
                        textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, fontSize = 28.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("nabung_amount_input")
                    )

                    // Quick History / Preset Chips Row (Collect all historical saved amounts + plan amount so chips grow as user saves)
                    val presets = remember(selectedGoal, transactions) {
                        val historical = transactions
                            .filter { it.type == TransactionType.SAVINGS && it.amount != 0.0 }
                            .map { kotlin.math.abs(it.amount).toLong() }
                            .filter { it > 0 }
                        val combined = mutableListOf<Long>()
                        selectedGoal?.fillPlanAmount?.let { planAmt ->
                            if (planAmt > 0) combined.add(planAmt.toLong())
                        }
                        combined.addAll(historical)
                        if (combined.isNotEmpty()) {
                            combined.distinct().sorted()
                        } else {
                            listOf(20000L, 50000L, 100000L, 200000L)
                        }
                    }
                    if (presets.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val decFmt = remember { java.text.DecimalFormat("#,###") }
                            presets.forEach { chipAmt ->
                                val chipLabel = try { decFmt.format(chipAmt).replace(",", ".") } catch (e: Exception) { chipAmt.toString() }
                                val isSelected = amountTfv.text == chipAmt.toString()
                                Surface(
                                    color = if (isSelected) AccentSavings.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (isSelected) BorderStroke(1.5.dp, AccentSavings) else BorderStroke(1.dp, GlassGradientBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
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
                                        text = chipLabel,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = if (isSelected) AccentSavings else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Note Input
                    Text(
                        text = "CATATAN (OPSIONAL)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text(if (mode == NabungMode.SETOR) "Misal: Setor gajian, hasil bonus" else "Misal: Ambil untuk keperluan mendesak", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Submit Gradient Button
                    val isActionEnabled = selectedGoal != null && amountNum > 0
                    GradientButton(
                        onClick = {
                            val goal = selectedGoal
                            if (goal != null && amountNum > 0) {
                                if (mode == NabungMode.SETOR) {
                                    onDepositGoal(goal.id, amountNum, noteText.trim())
                                } else {
                                    onWithdrawGoal(goal.id, amountNum, noteText.trim())
                                }
                                onDismiss()
                            }
                        },
                        enabled = isActionEnabled,
                        gradient = if (mode == NabungMode.SETOR) CyanButtonGradient else RoseButtonGradient,
                        disabledGradient = DisabledButtonGradient,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("submit_nabung_button")
                    ) {
                        Icon(
                            imageVector = if (mode == NabungMode.SETOR) Icons.Default.Add else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (mode == NabungMode.SETOR) "SIMPAN SETORAN TABUNGAN" else "SIMPAN PENARIKAN TABUNGAN",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
