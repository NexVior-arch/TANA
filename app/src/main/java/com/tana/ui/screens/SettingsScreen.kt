package com.tana.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tana.data.api.OpenRouterClient
import com.tana.ui.components.MonochromeCard
import com.tana.ui.components.SectionHeader
import com.tana.ui.theme.AccentExpense
import com.tana.ui.theme.AccentSavings
import com.tana.ui.theme.DarkGlassBackground
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

import com.tana.ui.components.GradientButton
import com.tana.ui.theme.PrimaryButtonGradient
import com.tana.ui.theme.RoseButtonGradient
import com.tana.ui.theme.CyanButtonGradient
import com.tana.ui.theme.SoftBlueGradient
import com.tana.ui.theme.DarkButtonGradient

@Composable
fun SettingsScreen(
    openRouterApiKey: String,
    openRouterModel: String,
    isReminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    reminderCustomMessage: String,
    currencyCode: String = "IDR",
    currencySymbol: String = "Rp",
    isAppLockEnabled: Boolean = false,
    appLockPin: String = "",
    isBiometricEnabled: Boolean = true,
    telegramBotToken: String = "",
    telegramChatId: String = "",
    maxAllowedFailedAttempts: Int = 5,
    telegramTestStatus: String? = null,
    aiTestResult: String? = null,
    isAiTestLoading: Boolean = false,
    onSaveApiKey: (String) -> Unit,
    onSelectModel: (String) -> Unit = {},
    onTestOpenRouter: (apiKey: String, model: String) -> Unit = { _, _ -> },
    onClearTestResult: () -> Unit = {},
    onUpdateReminder: (enabled: Boolean, hour: Int, minute: Int, customMessage: String) -> Unit,
    onTriggerTestNotification: () -> Unit,
    onUpdateSecuritySettings: (lockEnabled: Boolean, pin: String, biometricEnabled: Boolean, telegramToken: String, telegramChatId: String, maxAttempts: Int) -> Unit = { _, _, _, _, _, _ -> },
    onTestTelegramConnection: (botToken: String, chatId: String) -> Unit = { _, _ -> },
    onSetCurrency: (code: String, symbol: String) -> Unit = { _, _ -> },
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var apiKeyInput by remember(openRouterApiKey) { mutableStateOf(openRouterApiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showCustomCurrencyDialog by remember { mutableStateOf(false) }
    var customCurrencyInput by remember { mutableStateOf("") }
    var customReminderText by remember(reminderCustomMessage) { mutableStateOf(reminderCustomMessage) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    // Security Settings State
    var lockEnabledState by remember(isAppLockEnabled) { mutableStateOf(isAppLockEnabled) }
    var pinInputState by remember(appLockPin) { mutableStateOf(appLockPin) }
    var isPinVisible by remember { mutableStateOf(false) }
    var biometricEnabledState by remember(isBiometricEnabled) { mutableStateOf(isBiometricEnabled) }
    var maxAttemptsState by remember(maxAllowedFailedAttempts) { mutableStateOf(maxAllowedFailedAttempts) }
    var telegramTokenInput by remember(telegramBotToken) { mutableStateOf(telegramBotToken) }
    var isTelegramTokenVisible by remember { mutableStateOf(false) }
    var telegramChatIdInput by remember(telegramChatId) { mutableStateOf(telegramChatId) }

    var selectedCategoryTab by remember { mutableStateOf("Semua") }
    var isTestingConnection by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Time Picker
    val timePickerDialog = remember(reminderHour, reminderMinute) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onUpdateReminder(isReminderEnabled, hourOfDay, minute, customReminderText)
                Toast.makeText(context, "Pengingat diatur ke %02d:%02d".format(hourOfDay, minute), Toast.LENGTH_SHORT).show()
            },
            reminderHour,
            reminderMinute,
            true
        )
    }

    if (showCustomCurrencyDialog) {
        Dialog(
            onDismissRequest = { showCustomCurrencyDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showCustomCurrencyDialog = false },
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
                        Text(
                            text = "Simbol Mata Uang Kustom",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Masukkan simbol atau kode mata uang (misal: Rp, $, €, £, ¥, RM, S$, ฿, ₱):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = customCurrencyInput,
                            onValueChange = { customCurrencyInput = it },
                            placeholder = { Text("Simbol / Kode") },
                            singleLine = true,
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
                            modifier = Modifier.fillMaxWidth().testTag("custom_currency_input")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCustomCurrencyDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Batal", fontWeight = FontWeight.SemiBold)
                            }

                            GradientButton(
                                onClick = {
                                    if (customCurrencyInput.isNotBlank()) {
                                        onSetCurrency(customCurrencyInput.trim().uppercase(), customCurrencyInput.trim())
                                        showCustomCurrencyDialog = false
                                        Toast.makeText(context, "Mata uang diubah ke ${customCurrencyInput.trim()}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                gradient = PrimaryButtonGradient,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("save_custom_currency_button")
                            ) {
                                Text("Simpan", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        Dialog(
            onDismissRequest = { showClearConfirmation = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showClearConfirmation = false },
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
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = AccentExpense,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Hapus Seluruh Data",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Tindakan ini akan mengosongkan riwayat seluruh transaksi dan chat AI. Apakah Anda yakin?",
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
                                onClick = { showClearConfirmation = false },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Batal", fontWeight = FontWeight.SemiBold)
                            }

                            GradientButton(
                                onClick = {
                                    onClearAllData()
                                    showClearConfirmation = false
                                    Toast.makeText(context, "Data berhasil dikosongkan", Toast.LENGTH_SHORT).show()
                                },
                                gradient = RoseButtonGradient,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("confirm_clear_all_button")
                            ) {
                                Text("Hapus Semua", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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

            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    Text(
                        text = "PENGATURAN",
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Pengingat Disiplin, OpenRouter AI, & Konfigurasi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Chips Row
            val categories = listOf("Semua", "Keamanan", "OpenRouter AI", "Pengingat", "Mata Uang & Data")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategoryTab == category
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        modifier = Modifier.clickable { selectedCategoryTab = category }
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.5.sp
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Section: Keamanan & Kunci Aplikasi (PIN, Biometrik & Telegram Intruder Trap)
        if (selectedCategoryTab == "Semua" || selectedCategoryTab == "Keamanan") {
            item {
                SectionHeader(title = "Keamanan & Kunci Aplikasi")
            }

            item {
                MonochromeCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Kategori 1: Proteksi PIN (4 Angka Wajib)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Filled.Pin,
                                        contentDescription = null,
                                        tint = if (lockEnabledState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Kunci Aplikasi dengan PIN (4 Digit)",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (lockEnabledState) "Aktif • Memerlukan 4 digit PIN saat buka aplikasi" else "Nonaktif • Aplikasi langsung terbuka tanpa PIN",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = lockEnabledState,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked && pinInputState.length != 4) {
                                            Toast.makeText(context, "Silakan isi 4 digit PIN di bawah terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                        }
                                        lockEnabledState = isChecked
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }

                            // Kolom Pengisian PIN 4 Digit
                            Text(
                                text = "KOLOM PIN KEAMANAN (4 ANGKA)",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = pinInputState,
                                onValueChange = { input ->
                                    val cleanInput = input.filter { it.isDigit() }.take(4)
                                    pinInputState = cleanInput
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Masukkan 4 digit PIN (misal: 1234)", style = MaterialTheme.typography.bodyMedium) },
                                singleLine = true,
                                visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                        Icon(
                                            imageVector = if (isPinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = "Lihat PIN",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Text(
                                text = if (pinInputState.length == 4) "✓ Format PIN valid (4 Digit)" else "⚠️ Wajib terisi tepat 4 digit angka (${pinInputState.length}/4)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                color = if (pinInputState.length == 4) AccentSavings else MaterialTheme.colorScheme.error
                            )
                        }

                        // Kategori 2: Toggle Buka dengan Sidik Jari (Biometrik) - Terpisah
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Filled.Fingerprint,
                                        contentDescription = null,
                                        tint = if (biometricEnabledState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Buka dengan Sidik Jari (Biometrik)",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (biometricEnabledState) "Aktif • Bisa buka cepat via sensor sidik jari HP" else "Nonaktif • Hanya bisa buka menggunakan PIN",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = biometricEnabledState,
                                    onCheckedChange = { biometricEnabledState = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }

                        // Kategori 3: Pengaturan Batas Kesalahan & Jebakan Kamera Penyusup
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = AccentExpense,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Jebakan Kamera & Notifikasi Telegram",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Kamera depan otomatis menjepret penyusup & mengirim foto ke Telegram jika salah PIN",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Pengaturan Batas Kesalahan PIN (3x atau 5x)
                            Text(
                                text = "BATAS MAKSIMAL KESALAHAN PIN SEBELUM JEBAKAN AKTIF",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(3, 5).forEach { attemptOption ->
                                    val isSelected = maxAttemptsState == attemptOption
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { maxAttemptsState = attemptOption },
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = "$attemptOption Kali Salah",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Kolom Token Bot Telegram
                            Text(
                                text = "KOLOM TELEGRAM BOT TOKEN",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = telegramTokenInput,
                                onValueChange = { telegramTokenInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Contoh: 123456789:ABCdefGHI...", style = MaterialTheme.typography.bodyMedium) },
                                singleLine = true,
                                visualTransformation = if (isTelegramTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isTelegramTokenVisible = !isTelegramTokenVisible }) {
                                        Icon(
                                            imageVector = if (isTelegramTokenVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = "Toggle Token",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            // Kolom Telegram Chat ID
                            Text(
                                text = "KOLOM TELEGRAM CHAT ID",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = telegramChatIdInput,
                                onValueChange = { telegramChatIdInput = it.trim() },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("ID Chat / User ID Telegram kamu (misal: 12345678)", style = MaterialTheme.typography.bodyMedium) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            // Tombol Uji Koneksi Telegram
                            GradientButton(
                                onClick = {
                                    if (telegramTokenInput.isBlank() || telegramChatIdInput.isBlank()) {
                                        Toast.makeText(context, "Isi Bot Token dan Chat ID dulu!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onTestTelegramConnection(telegramTokenInput.trim(), telegramChatIdInput.trim())
                                    }
                                },
                                gradient = CyanButtonGradient,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Uji Kirim Telegram", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }

                            if (telegramTestStatus != null) {
                                Text(
                                    text = telegramTestStatus,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = if (telegramTestStatus.contains("Gagal", ignoreCase = true) || telegramTestStatus.contains("Error", ignoreCase = true)) MaterialTheme.colorScheme.error else AccentSavings
                                )
                            }
                        }

                        // Tombol Simpan Pengaturan Keamanan
                        GradientButton(
                            onClick = {
                                if (lockEnabledState && pinInputState.length != 4) {
                                    Toast.makeText(context, "PIN wajib terdiri dari tepat 4 digit angka!", Toast.LENGTH_SHORT).show()
                                } else {
                                    onUpdateSecuritySettings(
                                        lockEnabledState,
                                        pinInputState.trim(),
                                        biometricEnabledState,
                                        telegramTokenInput.trim(),
                                        telegramChatIdInput.trim(),
                                        maxAttemptsState
                                    )
                                    Toast.makeText(context, "Pengaturan keamanan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan Pengaturan Keamanan", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Section: Daily Reminder Notifications
        if (selectedCategoryTab == "Semua" || selectedCategoryTab == "Pengingat") {
            item {
                SectionHeader(title = "Notifikasi Pengingat Harian")
            }

        item {
            MonochromeCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = if (isReminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Disiplin Finansial Harian",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Notifikasi rutin untuk mencatat pengeluaran & target tabungan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateReminder(enabled, reminderHour, reminderMinute, customReminderText)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("toggle_reminder_switch")
                        )
                    }

                    if (isReminderEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Time Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { timePickerDialog.show() }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("reminder_time_picker_button"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Waktu Pengingat",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "%02d:%02d WIB".format(reminderHour, reminderMinute),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Motivational Reminder Text
                        Text(
                            text = "PESAN PENGINGAT KUSTOM (OPSIONAL)",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customReminderText,
                            onValueChange = {
                                customReminderText = it
                                onUpdateReminder(isReminderEnabled, reminderHour, reminderMinute, it)
                            },
                            placeholder = { Text("Biarkan kosong untuk quote motivasi otomatis...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_reminder_text_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Test Notification Trigger Button
                        Button(
                            onClick = {
                                onTriggerTestNotification()
                                Toast.makeText(context, "Notifikasi uji coba dikirim ke bilah status!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("test_notification_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kirim Notifikasi Uji Coba Sekarang",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }

        // Section: OpenRouter AI Setup
        if (selectedCategoryTab == "Semua" || selectedCategoryTab == "OpenRouter AI") {
            item {
                SectionHeader(title = "OpenRouter AI Configuration")
            }

        item {
            MonochromeCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OpenRouter API Key",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (openRouterApiKey.isNotBlank()) {
                            Surface(
                                color = AccentSavings.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "TERSIMPAN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AccentSavings,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Kunci API OpenRouter Anda disimpan secara lokal di perangkat dan digunakan untuk menghubungkan model AI keuangan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("sk-or-v1-...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        singleLine = true,
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle Key",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("openrouter_api_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradientButton(
                            onClick = {
                                onSaveApiKey(apiKeyInput.trim())
                                Toast.makeText(context, "API Key OpenRouter berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("save_openrouter_key_button")
                        ) {
                            Text("Simpan Kunci API", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        if (openRouterApiKey.isNotBlank()) {
                            Button(
                                onClick = {
                                    apiKeyInput = ""
                                    onSaveApiKey("")
                                    Toast.makeText(context, "API Key dihapus (kembali ke analisis lokal)", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("Hapus", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fixed Model Information
                    Text(
                        text = "MODEL AI AKTIF (SISTEM)",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Model Bawaan Sistem",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = AccentSavings.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Terkunci & Gratis",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = AccentSavings,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "dots-studio/dots-3-note-preview:free",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Model ini diatur otomatis oleh sistem dan dioptimalkan untuk asisten keuangan & tabungan TANA.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradientButton(
                            onClick = {
                                if (openRouterApiKey.isBlank()) {
                                    Toast.makeText(context, "Silakan simpan API Key terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                } else {
                                    onTestOpenRouter(openRouterApiKey, openRouterModel)
                                }
                            },
                            enabled = !isAiTestLoading,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.5f).height(38.dp).testTag("test_ai_model_button")
                        ) {
                            if (isAiTestLoading) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menguji...", style = MaterialTheme.typography.labelSmall)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Uji Model AI", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        if (aiTestResult != null) {
                            OutlinedButton(
                                onClick = onClearTestResult,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Bersihkan", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (aiTestResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Hasil Uji Model AI:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = aiTestResult ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Connection Tester button: "Tes Koneksi"
                    Button(
                        onClick = {
                            scope.launch {
                                isTestingConnection = true
                                try {
                                    val response = com.tana.data.api.OpenRouterClient.service.getModels()
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "🚀 Koneksi Sukses! TANA AI siap bertempur.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "🥀 Koneksi Gagal (Kode: ${response.code()}). Periksa API Key Anda.", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "😰 Gagal terhubung ke OpenRouter: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isTestingConnection = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        enabled = !isTestingConnection
                    ) {
                        if (isTestingConnection) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tes Koneksi", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
        }

        // Section: Currency Selection
        if (selectedCategoryTab == "Semua" || selectedCategoryTab == "Mata Uang & Data") {
            item {
                SectionHeader(title = "Mata Uang Aplikasi")
            }

        item {
            MonochromeCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SIMBOL & KODE MATA UANG",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val currencyList = listOf(
                        "IDR" to "Rp",
                        "USD" to "$",
                        "EUR" to "€",
                        "JPY" to "¥",
                        "MYR" to "RM",
                        "SGD" to "S$"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(currencyList) { (code, symbol) ->
                            val isSelected = currencySymbol == symbol
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.clickable {
                                    onSetCurrency(code, symbol)
                                    Toast.makeText(context, "Mata uang diubah ke $symbol ($code)", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "$symbol ($code)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.clickable { showCustomCurrencyDialog = true }
                            ) {
                                Text(
                                    text = "+ Kustom",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Data & Backup
        item {
            SectionHeader(title = "Pengelolaan Data")
        }

        item {
            MonochromeCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showClearConfirmation = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = AccentExpense
                        ),
                        border = BorderStroke(1.dp, AccentExpense.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("clear_all_data_button")
                    ) {
                        Text("Kosongkan Seluruh Data", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
        }

        // About Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FINMONOCHROME v1.0",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Disiplin Finansial Minimalis • Tanpa Emoji • Bebas Distraksi",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
}
