package com.tana.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tana.data.model.TransactionEntity
import com.tana.ui.theme.AccentExpense
import com.tana.ui.theme.AccentSavings
import com.tana.ui.theme.PrimaryButtonGradient
import com.tana.ui.theme.SoftBlueGradient
import com.tana.ui.theme.DisabledButtonGradient
import java.text.DecimalFormat

@Composable
fun CatatTabunganDialog(
    dialogTitle: String = "Catat Tabungan",
    initialIsSetor: Boolean = true,
    initialAmount: Double = 0.0,
    initialNote: String = "",
    defaultPlanAmount: Double = 0.0,
    historyTransactions: List<TransactionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (isSetor: Boolean, amount: Double, note: String) -> Unit
) {
    var isSetorMode by remember { mutableStateOf(initialIsSetor) }
    val initStr = if (initialAmount > 0) initialAmount.toLong().toString() else ""
    val focusRequester = remember { FocusRequester() }
    var amountTfv by remember {
        mutableStateOf(TextFieldValue(text = initStr, selection = TextRange(initStr.length)))
    }
    var noteText by remember {
        mutableStateOf(if (initialNote.startsWith("Setor") || initialNote.startsWith("Tarik")) "" else initialNote)
    }

    // Aggregate unique amounts for quick history chips:
    // When user saves (nabung), newly deposited amounts are collected from history and appear here,
    // scrollable horizontally ("tar di geser")
    val presetChips = remember(historyTransactions, defaultPlanAmount) {
        val historical = historyTransactions.map { kotlin.math.abs(it.amount).toLong() }
            .filter { it > 0 }
        val combined = mutableListOf<Long>()
        if (defaultPlanAmount > 0) {
            combined.add(defaultPlanAmount.toLong())
        }
        combined.addAll(historical)
        if (combined.isNotEmpty()) {
            combined.distinct().sorted()
        } else {
            listOf(20000L, 50000L, 100000L, 200000L)
        }
    }

    val decFormatter = remember { DecimalFormat("#,###") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title (Screenshot 1: "Catat Tabungan")
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color.White
                )

                // Segmented Toggle Bar: [ + Tambah | - Kurangi ]
                Surface(
                    color = Color(0xFF16191E),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // + Tambah
                        Surface(
                            color = if (isSetorMode) Color(0xFF383F4D) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp),
                            border = if (isSetorMode) BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { isSetorMode = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isSetorMode) Color.White else Color(0xFF9C9587),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tambah",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isSetorMode) Color.White else Color(0xFF9C9587)
                                )
                            }
                        }

                        // - Kurangi
                        Surface(
                            color = if (!isSetorMode) Color(0xFF383F4D) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp),
                            border = if (!isSetorMode) BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { isSetorMode = false }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = if (!isSetorMode) Color.White else Color(0xFF9C9587),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kurangi",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = if (!isSetorMode) Color.White else Color(0xFF9C9587)
                                )
                            }
                        }
                    }
                }

                // Nominal Field
                OutlinedTextField(
                    value = amountTfv,
                    onValueChange = { tfv ->
                        if (tfv.text.all { c -> c.isDigit() }) {
                            amountTfv = tfv
                        }
                    },
                    label = { Text("Nominal", color = Color(0xFFA39D8F)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = Color(0xFFA39D8F),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (amountTfv.text.isNotEmpty()) {
                            IconButton(onClick = { amountTfv = TextFieldValue("") }) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Hapus",
                                    tint = Color(0xFFA39D8F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFC6A15B),
                        unfocusedBorderColor = Color(0xFF524D42),
                        focusedContainerColor = Color(0xFF181B20),
                        unfocusedContainerColor = Color(0xFF181B20),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("catat_tabungan_nominal_input")
                )

                // Quick Amount History Chips Row (Scrollable horizontally)
                if (presetChips.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presetChips.forEach { chipAmt ->
                            val label = try {
                                decFormatter.format(chipAmt).replace(",", ".")
                            } catch (e: Exception) {
                                chipAmt.toString()
                            }
                            val isSelected = amountTfv.text == chipAmt.toString()
                            Surface(
                                color = if (isSelected) Color(0xFF38445A) else Color(0xFF2A2F3A),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFFC6A15B) else Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val newText = chipAmt.toString()
                                        // Position cursor at the very end of line
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
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = if (isSelected) Color(0xFFD4B876) else Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Keterangan Field
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Keterangan", color = Color(0xFFA39D8F)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = null,
                            tint = Color(0xFFA39D8F),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFC6A15B),
                        unfocusedBorderColor = Color(0xFF524D42),
                        focusedContainerColor = Color(0xFF181B20),
                        unfocusedContainerColor = Color(0xFF181B20),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catat_tabungan_note_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons: Batal & Simpan
                val parsedAmt = amountTfv.text.toDoubleOrNull() ?: 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("catat_tabungan_batal_btn")
                    ) {
                        Text(
                            text = "Batal",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF9C9587)
                        )
                    }

                    GradientButton(
                        onClick = {
                            if (parsedAmt > 0) {
                                onSave(isSetorMode, parsedAmt, noteText)
                            }
                        },
                        enabled = parsedAmt > 0,
                        gradient = SoftBlueGradient,
                        disabledGradient = DisabledButtonGradient,
                        shape = RoundedCornerShape(20.dp),
                        contentColor = Color(0xFF0F172A),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp, vertical = 11.dp),
                        modifier = Modifier.testTag("catat_tabungan_simpan_btn")
                    ) {
                        Text(
                            text = "Simpan",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
