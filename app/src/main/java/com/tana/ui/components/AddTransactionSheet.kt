package com.tana.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tana.data.model.SavingsGoalEntity
import com.tana.data.model.TransactionType
import com.tana.ui.theme.AccentExpense
import com.tana.ui.theme.AccentIncome
import com.tana.ui.theme.AccentSavings
import com.tana.ui.theme.GlassGradientBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    activeSavingsGoals: List<SavingsGoalEntity>,
    onDismiss: () -> Unit,
    onSave: (type: TransactionType, amount: Double, category: String, note: String, goalId: Long?) -> Unit,
    initialType: TransactionType = TransactionType.EXPENSE,
    preselectedGoalId: Long? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember { mutableStateOf(if (preselectedGoalId != null) TransactionType.SAVINGS else initialType) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var customCategoryText by remember { mutableStateOf("") }
    var selectedGoalId by remember { mutableStateOf(preselectedGoalId) }

    val expenseCategories = listOf(
        "Makanan & Minuman", "Transportasi", "Belanja Bulanan",
        "Tagihan & Utilitas", "Hiburan", "Kesehatan", "Edukasi", "Lainnya"
    )
    val incomeCategories = listOf(
        "Gaji Pokok", "Freelance & Proyek", "Bonus & Tunjangan",
        "Dividen & Investasi", "Hadiah / Hibah", "Penjualan Barang", "Lainnya"
    )

    var selectedCategory by remember(selectedType) {
        mutableStateOf(
            when (selectedType) {
                TransactionType.EXPENSE -> expenseCategories.first()
                TransactionType.INCOME -> incomeCategories.first()
                TransactionType.SAVINGS -> "Nabung Rutin"
            }
        )
    }

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
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CATAT TRANSAKSI",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.8.sp,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Input data keuangan secara presisi",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .testTag("close_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Type Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(BorderStroke(1.dp, GlassGradientBorder), RoundedCornerShape(16.dp))
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TypeSegmentButton(
                    title = "Pengeluaran",
                    isSelected = selectedType == TransactionType.EXPENSE,
                    color = AccentExpense,
                    modifier = Modifier.weight(1f),
                    testTag = "type_expense_tab"
                ) {
                    selectedType = TransactionType.EXPENSE
                    selectedCategory = expenseCategories.first()
                }

                TypeSegmentButton(
                    title = "Pemasukan",
                    isSelected = selectedType == TransactionType.INCOME,
                    color = AccentIncome,
                    modifier = Modifier.weight(1f),
                    testTag = "type_income_tab"
                ) {
                    selectedType = TransactionType.INCOME
                    selectedCategory = incomeCategories.first()
                }

                TypeSegmentButton(
                    title = "Nabung",
                    isSelected = selectedType == TransactionType.SAVINGS,
                    color = AccentSavings,
                    modifier = Modifier.weight(1f),
                    testTag = "type_savings_tab"
                ) {
                    selectedType = TransactionType.SAVINGS
                    selectedCategory = "Nabung Rutin"
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nominal Input Field
            Text(
                text = "NOMINAL TRANSAKSI (RP)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    fontSize = 10.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        amountText = input
                    }
                },
                placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)) },
                textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, fontSize = 28.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            )

            // If SAVINGS is selected, show Target Goal Selector
            AnimatedVisibility(visible = selectedType == TransactionType.SAVINGS) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text(
                        text = "ALOKASI TARGET TABUNGAN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeSavingsGoals.isEmpty()) {
                        Text(
                            text = "Belum ada target tabungan aktif. Transaksi akan dicatat sebagai tabungan umum.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                GoalChip(
                                    title = "Tabungan Bebas",
                                    isSelected = selectedGoalId == null,
                                    onClick = { selectedGoalId = null }
                                )
                            }
                            items(activeSavingsGoals) { goal ->
                                GoalChip(
                                    title = goal.title,
                                    isSelected = selectedGoalId == goal.id,
                                    onClick = { selectedGoalId = goal.id }
                                )
                            }
                        }
                    }
                }
            }

            // Categories Selector
            AnimatedVisibility(visible = selectedType != TransactionType.SAVINGS) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text(
                        text = "KATEGORI TRANSAKSI",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val categories = if (selectedType == TransactionType.EXPENSE) expenseCategories else incomeCategories
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            CategoryPill(
                                label = cat,
                                isSelected = selectedCategory == cat,
                                type = selectedType,
                                onClick = { selectedCategory = cat }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Catatan Field
            Text(
                text = "CATATAN / KETERANGAN",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("Contoh: Makan siang, bayar wifi, dll", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            val amountNum = amountText.toDoubleOrNull() ?: 0.0
            val isEnabled = amountNum > 0

            Button(
                onClick = {
                    val finalCategory = if (selectedType == TransactionType.SAVINGS && selectedGoalId != null) {
                        val g = activeSavingsGoals.find { it.id == selectedGoalId }
                        "Tabungan: ${g?.title ?: "Target"}"
                    } else selectedCategory

                    onSave(selectedType, amountNum, finalCategory, noteText.trim(), selectedGoalId)
                    onDismiss()
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIMPAN TRANSAKSI",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TypeSegmentButton(
    title: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
    val border = if (isSelected) BorderStroke(1.dp, GlassGradientBorder) else null

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = border,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.5.sp
                ),
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryPill(
    label: String,
    isSelected: Boolean,
    type: TransactionType,
    onClick: () -> Unit
) {
    val icon = getCategoryIcon(label, type)
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                color = fg
            )
        }
    }
}

@Composable
private fun GoalChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Savings,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                color = fg,
                maxLines = 1
            )
        }
    }
}
