package com.tana.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tana.data.api.OpenRouterClient
import com.tana.data.database.AppDatabase
import com.tana.data.model.AgentRoutineEntity
import com.tana.data.model.AiMessageEntity
import com.tana.data.model.SavingsGoalEntity
import com.tana.data.model.TransactionEntity
import com.tana.data.model.TransactionType
import com.tana.data.repository.FinanceRepository
import com.tana.reminder.DailyReminderReceiver
import com.tana.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val FIXED_AI_MODEL = "dots-studio/dots-3-note-preview:free"

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    val transactions: StateFlow<List<TransactionEntity>>
    val savingsGoals: StateFlow<List<SavingsGoalEntity>>
    val aiMessages: StateFlow<List<AiMessageEntity>>
    val agentRoutines: StateFlow<List<AgentRoutineEntity>>

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()
    private var currentAiJob: kotlinx.coroutines.Job? = null

    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage: StateFlow<String?> = _aiErrorMessage.asStateFlow()

    // Surfaces the result of the last deposit/withdraw/edit/delete operation so the
    // UI can show a real error (e.g. goal deleted, invalid amount) instead of
    // assuming success. Note: withdrawals/edits are intentionally allowed to push
    // the balance negative (this is a savings target tracker, not a bank ATM), so
    // InsufficientBalance is never returned by those paths today - it's kept in
    // SavingsOpResult in case a future caller needs a hard cap.
    private val _savingsErrorMessage = MutableStateFlow<String?>(null)
    val savingsErrorMessage: StateFlow<String?> = _savingsErrorMessage.asStateFlow()

    fun clearSavingsErrorMessage() {
        _savingsErrorMessage.value = null
    }

    private fun describeSavingsFailure(result: com.tana.data.repository.SavingsOpResult): String? {
        return when (result) {
            is com.tana.data.repository.SavingsOpResult.Success -> null
            is com.tana.data.repository.SavingsOpResult.GoalNotFound ->
                "Target tabungan tidak ditemukan. Mungkin sudah dihapus."
            is com.tana.data.repository.SavingsOpResult.InvalidAmount ->
                result.reason
            is com.tana.data.repository.SavingsOpResult.InsufficientBalance ->
                "Saldo tidak cukup. Tersedia ${FinanceViewModel.formatRupiah(result.available)}, diminta ${FinanceViewModel.formatRupiah(result.requested)}."
        }
    }

    private val _openRouterApiKey = MutableStateFlow("")
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    private val _openRouterModel = MutableStateFlow(FIXED_AI_MODEL)
    val openRouterModel: StateFlow<String> = _openRouterModel.asStateFlow()

    private val _selectedChartMode = MutableStateFlow("CRISP_LINE")
    val selectedChartMode: StateFlow<String> = _selectedChartMode.asStateFlow()

    private val _currencySymbol = MutableStateFlow("Rp")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _currencyCode = MutableStateFlow("IDR")
    val currencyCode: StateFlow<String> = _currencyCode.asStateFlow()

    private val _dashboardSelectedGoalId = MutableStateFlow("ALL")
    val dashboardSelectedGoalId: StateFlow<String> = _dashboardSelectedGoalId.asStateFlow()

    fun setDashboardSelectedGoalId(id: String) {
        viewModelScope.launch {
            _dashboardSelectedGoalId.value = id
            repository.setPreference("dashboard_selected_goal_id", id)
            com.tana.widget.WidgetUpdateHelper.setSelectedGoalId(getApplication(), id)
        }
    }

    private val _isReminderEnabled = MutableStateFlow(true)
    val isReminderEnabled: StateFlow<Boolean> = _isReminderEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(20)
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(0)
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    private val _reminderCustomMessage = MutableStateFlow("")
    val reminderCustomMessage: StateFlow<String> = _reminderCustomMessage.asStateFlow()

    // --- Security & App Lock States ---
    private val _isAppLockEnabled = MutableStateFlow(false)
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _appLockPin = MutableStateFlow("")
    val appLockPin: StateFlow<String> = _appLockPin.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(true)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _telegramBotToken = MutableStateFlow("")
    val telegramBotToken: StateFlow<String> = _telegramBotToken.asStateFlow()

    private val _telegramChatId = MutableStateFlow("")
    val telegramChatId: StateFlow<String> = _telegramChatId.asStateFlow()

    private val _failedUnlockAttempts = MutableStateFlow(0)
    val failedUnlockAttempts: StateFlow<Int> = _failedUnlockAttempts.asStateFlow()

    private val _maxAllowedFailedAttempts = MutableStateFlow(5)
    val maxAllowedFailedAttempts: StateFlow<Int> = _maxAllowedFailedAttempts.asStateFlow()

    private val _telegramTestStatus = MutableStateFlow<String?>(null)
    val telegramTestStatus: StateFlow<String?> = _telegramTestStatus.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = FinanceRepository(db)

        transactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        savingsGoals = repository.allSavingsGoals.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        aiMessages = repository.aiMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        agentRoutines = repository.allAgentRoutines.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        loadPreferences()
        DailyReminderReceiver.createNotificationChannel(application)
        com.tana.reminder.AgentAutonomousScheduler.createAgentNotificationChannel(application)
        com.tana.reminder.AgentAutonomousScheduler.executePendingRoutines(application)
        checkAndSeedSampleData()

        // Continuous background ticker loop for real-time interval agent routines
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val routines = agentRoutines.value
                    if (routines.any { it.isEnabled && it.isApproved }) {
                        com.tana.reminder.AgentAutonomousScheduler.executePendingRoutines(getApplication())
                    }
                } catch (_: Exception) {}
                delay(3000)
            }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            repository.getPreference("openrouter_api_key").collectLatest { key ->
                _openRouterApiKey.value = key ?: ""
            }
        }
        viewModelScope.launch {
            repository.getPreference("openrouter_model").collectLatest { _ ->
                _openRouterModel.value = FIXED_AI_MODEL
            }
        }
        viewModelScope.launch {
            repository.getPreference("reminder_enabled").collectLatest { enabled ->
                _isReminderEnabled.value = enabled?.toBooleanStrictOrNull() ?: true
            }
        }
        viewModelScope.launch {
            repository.getPreference("reminder_hour").collectLatest { h ->
                _reminderHour.value = h?.toIntOrNull() ?: 20
            }
        }
        viewModelScope.launch {
            repository.getPreference("reminder_minute").collectLatest { m ->
                _reminderMinute.value = m?.toIntOrNull() ?: 0
            }
        }
        viewModelScope.launch {
            repository.getPreference("reminder_custom_msg").collectLatest { msg ->
                _reminderCustomMessage.value = msg ?: ""
            }
        }
        viewModelScope.launch {
            repository.getPreference("selected_chart_mode").collectLatest { mode ->
                if (!mode.isNullOrBlank()) {
                    _selectedChartMode.value = mode
                }
            }
        }
        viewModelScope.launch {
            repository.getPreference("currency_symbol").collectLatest { sym ->
                val activeSym = sym ?: "Rp"
                _currencySymbol.value = activeSym
                activeCurrencySymbol = activeSym
            }
        }
        viewModelScope.launch {
            repository.getPreference("currency_code").collectLatest { code ->
                _currencyCode.value = code ?: "IDR"
            }
        }
        viewModelScope.launch {
            repository.getPreference("security_app_lock_enabled").collectLatest { lockEnabled ->
                val isLocked = lockEnabled?.toBooleanStrictOrNull() ?: false
                _isAppLockEnabled.value = isLocked
                if (!isLocked) {
                    _isAppUnlocked.value = true
                }
            }
        }
        viewModelScope.launch {
            repository.getPreference("security_app_lock_pin").collectLatest { pin ->
                _appLockPin.value = pin ?: ""
            }
        }
        viewModelScope.launch {
            repository.getPreference("security_biometric_enabled").collectLatest { bio ->
                _isBiometricEnabled.value = bio?.toBooleanStrictOrNull() ?: true
            }
        }
        viewModelScope.launch {
            repository.getPreference("security_telegram_bot_token").collectLatest { token ->
                _telegramBotToken.value = token ?: ""
            }
        }
        viewModelScope.launch {
            repository.getPreference("security_telegram_chat_id").collectLatest { chatId ->
                _telegramChatId.value = chatId ?: ""
            }
        }
        viewModelScope.launch {
            repository.getPreference("security_max_failed_attempts").collectLatest { maxStr ->
                _maxAllowedFailedAttempts.value = maxStr?.toIntOrNull() ?: 5
            }
        }
        viewModelScope.launch {
            repository.getPreference("dashboard_selected_goal_id").collectLatest { id ->
                _dashboardSelectedGoalId.value = id ?: "ALL"
            }
        }
    }

    fun setCurrency(code: String, symbol: String) {
        _currencyCode.value = code
        _currencySymbol.value = symbol
        activeCurrencySymbol = symbol
        viewModelScope.launch {
            repository.setPreference("currency_code", code)
            repository.setPreference("currency_symbol", symbol)
        }
    }

    fun setSelectedChartMode(mode: String) {
        _selectedChartMode.value = mode
        viewModelScope.launch {
            repository.setPreference("selected_chart_mode", mode)
        }
    }

    private fun checkAndSeedSampleData() {
        viewModelScope.launch {
            val existing = repository.getPreferenceDirect("data_seeded")
            if (existing == null) {
                // Initialize clean greeting without fake demo transactions
                repository.insertAiMessage(
                    role = "assistant",
                    content = "Halo. Saya TANA AI Financial Advisor. Saya siap membantu Anda menganalisis kebiasaan finansial secara presisi berbasis fakta, membaca rincian struk belanja/invoice, dan mencatat transaksi atau setoran tabungan secara instan.\n\nAnda dapat meminta saya mencatat pengeluaran, menyetor tabungan, atau membuat target baru kapan saja."
                )
                repository.setPreference("data_seeded", "true")
            }
        }
    }

    fun seedInitialData() {
        // Kept for manual simulation if needed, but not automatically loaded
        viewModelScope.launch {
            repository.insertAiMessage(
                role = "assistant",
                content = "Database telah diinisialisasi untuk pencatatan keuangan riil Anda."
            )
        }
    }

    // --- Actions ---
    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis(),
        goalId: Long? = null
    ) {
        viewModelScope.launch {
            if (type == TransactionType.SAVINGS && goalId != null) {
                repository.depositToGoal(goalId, amount, note.ifBlank { category })
            } else {
                repository.insertTransaction(
                    TransactionEntity(
                        type = type,
                        amount = amount,
                        category = category,
                        note = note,
                        timestamp = timestamp,
                        goalId = goalId
                    )
                )
            }
            com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun addSavingsGoal(
        title: String,
        targetAmount: Double,
        initialAmount: Double = 0.0,
        fillPlanFrequency: String = "DAILY",
        fillPlanAmount: Double = 0.0,
        activeDays: String = "1,2,3,4,5,6,7",
        reminderEnabled: Boolean = true,
        reminderHour: Int = 12,
        reminderMinute: Int = 0,
        imageUri: String? = null,
        currency: String = "IDR",
        note: String = ""
    ) {
        viewModelScope.launch {
            // Auto calculate projected target date based on daily/weekly plan
            val daysSet = parseActiveDays(activeDays)
            val schedule = calculateGoalSchedule(
                targetAmount = targetAmount,
                currentAmount = initialAmount,
                fillPlanAmount = fillPlanAmount,
                fillPlanFrequency = fillPlanFrequency,
                activeDays = daysSet
            )

            val goalId = repository.insertSavingsGoal(
                SavingsGoalEntity(
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = initialAmount,
                    targetDateMillis = schedule.projectedCompletionMillis,
                    fillPlanFrequency = fillPlanFrequency,
                    fillPlanAmount = fillPlanAmount,
                    activeDays = activeDays,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    imageUri = imageUri,
                    currency = currency,
                    note = note,
                    isCompleted = initialAmount >= targetAmount && targetAmount > 0
                )
            )
            if (initialAmount > 0) {
                repository.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.SAVINGS,
                        amount = initialAmount,
                        category = "Tabungan: $title",
                        note = "Saldo awal target tabungan",
                        timestamp = System.currentTimeMillis(),
                        goalId = goalId
                    )
                )
            }
            com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun depositToGoal(goalId: Long, amount: Double, note: String = "") {
        viewModelScope.launch {
            val result = repository.depositToGoal(goalId, amount, note)
            describeSavingsFailure(result)?.let { _savingsErrorMessage.value = it }
            com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun withdrawFromGoal(goalId: Long, amount: Double, note: String = "") {
        viewModelScope.launch {
            val result = repository.withdrawFromGoal(goalId, amount, note)
            describeSavingsFailure(result)?.let { _savingsErrorMessage.value = it }
            com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun depositToGoalByName(goalName: String, amount: Double, note: String = "") {
        viewModelScope.launch {
            val goal = repository.findGoalByName(goalName)
            if (goal != null) {
                val result = repository.depositToGoal(goal.id, amount, note)
                describeSavingsFailure(result)?.let { _savingsErrorMessage.value = it }
            } else {
                // If goal doesn't exist yet, automatically create it and deposit
                val goalId = repository.insertSavingsGoal(
                    SavingsGoalEntity(
                        title = goalName.trim().ifBlank { "Tabungan Mandiri" },
                        targetAmount = amount * 10,
                        currentAmount = amount,
                        targetDateMillis = System.currentTimeMillis() + (60 * 86400000L),
                        fillPlanFrequency = "DAILY",
                        fillPlanAmount = amount,
                        activeDays = "1,2,3,4,5,6,7",
                        reminderEnabled = true,
                        reminderHour = 20,
                        reminderMinute = 0,
                        note = "Dibuat otomatis dari interaksi AI"
                    )
                )
                repository.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.SAVINGS,
                        amount = amount,
                        category = "Tabungan: ${goalName.trim().ifBlank { "Tabungan Mandiri" }}",
                        note = note,
                        timestamp = System.currentTimeMillis(),
                        goalId = goalId
                    )
                )
            }
            com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun withdrawFromGoalByName(goalName: String, amount: Double, note: String = "") {
        viewModelScope.launch {
            val goal = repository.findGoalByName(goalName)
            if (goal != null) {
                val result = repository.withdrawFromGoal(goal.id, amount, note)
                describeSavingsFailure(result)?.let { _savingsErrorMessage.value = it }
                com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
            }
        }
    }

    fun deleteSavingsGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(id)
            com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun deleteSavingsGoalByName(goalName: String) {
        viewModelScope.launch {
            val goal = repository.findGoalByName(goalName)
            if (goal != null) {
                repository.deleteSavingsGoal(goal.id)
                com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
            }
        }
    }

    fun updateGoalByName(goalName: String, newTarget: Double, newPlan: Double) {
        viewModelScope.launch {
            val goal = repository.findGoalByName(goalName)
            if (goal != null) {
                val updatedTarget = if (newTarget > 0) newTarget else goal.targetAmount
                val updatedPlan = if (newPlan > 0) newPlan else goal.fillPlanAmount
                repository.updateSavingsGoalMetadata(
                    goalId = goal.id,
                    title = goal.title,
                    targetAmount = updatedTarget,
                    fillPlanAmount = updatedPlan,
                    fillPlanFrequency = goal.fillPlanFrequency,
                    note = goal.note
                )
                com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
            }
        }
    }

    fun fixLastSavingsTransactionByName(goalName: String, newAmount: Double, newNote: String) {
        viewModelScope.launch {
            val goal = repository.findGoalByName(goalName)
            if (goal != null) {
                val transactions = repository.allTransactions.firstOrNull() ?: emptyList()
                val lastGoalTx = transactions.filter { it.goalId == goal.id || it.category.contains(goal.title, ignoreCase = true) }
                    .maxByOrNull { it.timestamp }
                val result = if (lastGoalTx != null) {
                    val finalAmount = if (lastGoalTx.amount < 0) -kotlin.math.abs(newAmount) else kotlin.math.abs(newAmount)
                    repository.updateSavingsTransaction(lastGoalTx, finalAmount, newNote.ifBlank { lastGoalTx.note }, lastGoalTx.timestamp)
                } else {
                    repository.depositToGoal(goal.id, newAmount, newNote)
                }
                describeSavingsFailure(result)?.let { _savingsErrorMessage.value = it }
                com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
            }
        }
    }

    /**
     * Used by the Edit Goal dialog. Only ever touches metadata (title, target,
     * plan, note) - currentAmount is intentionally never accepted here, so an
     * in-flight deposit (manual or from the autonomous AI agent) can never be
     * silently overwritten by a stale snapshot from an open edit dialog.
     */
    fun updateSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.updateSavingsGoalMetadata(
                goalId = goal.id,
                title = goal.title,
                targetAmount = goal.targetAmount,
                fillPlanAmount = goal.fillPlanAmount,
                fillPlanFrequency = goal.fillPlanFrequency,
                note = goal.note
            )
            com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun updateSavingsTransaction(
        transaction: TransactionEntity,
        newAmount: Double,
        newNote: String,
        newTimestamp: Long
    ) {
        viewModelScope.launch {
            val result = repository.updateSavingsTransaction(transaction, newAmount, newNote, newTimestamp)
            describeSavingsFailure(result)?.let { _savingsErrorMessage.value = it }
            com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun deleteSavingsTransaction(transactionId: Long, goalId: Long?) {
        viewModelScope.launch {
            val result = repository.deleteSavingsTransaction(transactionId, goalId)
            describeSavingsFailure(result)?.let { _savingsErrorMessage.value = it }
            com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun setOpenRouterApiKey(key: String) {
        viewModelScope.launch {
            repository.setPreference("openrouter_api_key", key.trim())
            _openRouterApiKey.value = key.trim()
        }
    }

    fun setOpenRouterModel(model: String) {
        viewModelScope.launch {
            repository.setPreference("openrouter_model", FIXED_AI_MODEL)
            _openRouterModel.value = FIXED_AI_MODEL
        }
    }

    private val _aiTestResult = MutableStateFlow<String?>(null)
    val aiTestResult: StateFlow<String?> = _aiTestResult.asStateFlow()

    private val _isAiTestLoading = MutableStateFlow(false)
    val isAiTestLoading: StateFlow<Boolean> = _isAiTestLoading.asStateFlow()

    fun testOpenRouterConnection(apiKey: String, modelName: String = FIXED_AI_MODEL) {
        viewModelScope.launch {
            _isAiTestLoading.value = true
            _aiTestResult.value = ""
            
            val testQuery = "Halo! Tolong respon dengan sapaan singkat dan beritahu bahwa model $FIXED_AI_MODEL dan API Key OpenRouter Anda berfungsi dengan sangat baik! Maksimal 2 kalimat."
            
            val streamBuffer = StringBuilder()
            val result = repository.requestAiFinancialAdviceStream(
                context = getApplication(),
                apiKey = apiKey.trim(),
                modelName = FIXED_AI_MODEL,
                userQuery = testQuery,
                mediaAttachmentName = null,
                mediaType = null,
                attachedImageUri = null,
                chatHistory = emptyList(),
                onChunk = { token ->
                    streamBuffer.append(token)
                    _aiTestResult.value = streamBuffer.toString()
                }
            )
            
            result.onSuccess { reply ->
                _aiTestResult.value = reply
            }.onFailure { err ->
                if (_aiTestResult.value.isNullOrBlank()) {
                    _aiTestResult.value = "Gagal: ${err.message ?: "Kesalahan tidak diketahui"}"
                }
            }
            _isAiTestLoading.value = false
        }
    }

    fun clearAiTestResult() {
        _aiTestResult.value = null
    }

    // --- Autonomous Agent Routine Operations ---
    fun createAgentRoutine(
        type: com.tana.data.model.AgentRoutineType,
        title: String,
        description: String = "",
        targetGoalId: Long? = null,
        targetGoalName: String? = null,
        amount: Double = 0.0,
        activeDaysCsv: String = "2,3,4,5,6,7",
        executionHour: Int = 8,
        executionMinute: Int = 0,
        customPromptOrQuote: String = "",
        isApproved: Boolean = true
    ) {
        viewModelScope.launch {
            var intervalSec = 0
            when {
                activeDaysCsv.startsWith("SEC_") -> intervalSec = activeDaysCsv.replace("SEC_", "").toIntOrNull() ?: 1
                activeDaysCsv.startsWith("MIN_") -> intervalSec = (activeDaysCsv.replace("MIN_", "").toIntOrNull() ?: 1) * 60
                activeDaysCsv.startsWith("HOUR_") -> intervalSec = (activeDaysCsv.replace("HOUR_", "").toIntOrNull() ?: 1) * 3600
                activeDaysCsv.contains("detik", ignoreCase = true) || description.contains("detik", ignoreCase = true) -> {
                    val match = Regex("""(\d+)\s*detik""", RegexOption.IGNORE_CASE).find("$activeDaysCsv $description")
                    intervalSec = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
                }
            }

            val routine = com.tana.data.model.AgentRoutineEntity(
                type = type,
                title = title.trim().ifBlank { "Tugas Otomatis AI" },
                description = description,
                targetGoalId = targetGoalId,
                targetGoalName = targetGoalName?.trim(),
                amount = amount,
                activeDaysCsv = activeDaysCsv,
                executionHour = executionHour,
                executionMinute = executionMinute,
                intervalSeconds = intervalSec,
                customPromptOrQuote = customPromptOrQuote,
                isEnabled = isApproved,
                isApproved = isApproved
            )
            repository.insertAgentRoutine(routine)
            com.tana.reminder.AgentAutonomousScheduler.executePendingRoutines(getApplication())
        }
    }

    fun setAgentRoutineEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setAgentRoutineEnabled(id, enabled)
            if (enabled) {
                com.tana.reminder.AgentAutonomousScheduler.executePendingRoutines(getApplication())
            }
        }
    }

    fun toggleAgentRoutine(id: Long, enabled: Boolean) = setAgentRoutineEnabled(id, enabled)

    fun updateAiMessageContent(id: Long, content: String) {
        viewModelScope.launch {
            repository.updateAiMessageContent(id, content)
        }
    }

    fun approveAndEnableRoutine(id: Long, approved: Boolean) {
        viewModelScope.launch {
            repository.setAgentRoutineApproved(id, approved)
            if (approved) {
                com.tana.reminder.AgentAutonomousScheduler.executePendingRoutines(getApplication())
            }
        }
    }

    fun deleteAgentRoutine(id: Long) {
        viewModelScope.launch {
            repository.deleteAgentRoutine(id)
        }
    }

    fun stopAgentRoutinesByQuery(query: String) {
        viewModelScope.launch {
            if (query.trim().uppercase() == "ALL" || query.trim().uppercase() == "SEMUA") {
                val all = repository.allAgentRoutines.firstOrNull() ?: emptyList()
                for (r in all) {
                    repository.setAgentRoutineEnabled(r.id, false)
                }
            } else {
                repository.disableRoutinesByQuery(query)
            }
        }
    }

    fun executePendingAgentRoutinesNow() {
        com.tana.reminder.AgentAutonomousScheduler.executePendingRoutines(getApplication())
    }

    // --- Security & PIN Lock Operations ---
    fun setAppUnlocked(unlocked: Boolean) {
        _isAppUnlocked.value = unlocked
        if (unlocked) {
            _failedUnlockAttempts.value = 0
        }
    }

    fun recordFailedUnlockAttempt() {
        val next = _failedUnlockAttempts.value + 1
        _failedUnlockAttempts.value = next

        val limit = _maxAllowedFailedAttempts.value
        // If reached limit attempts (3x or 5x), capture photo and trigger Telegram Alert
        if (next >= limit) {
            triggerIntruderTrapCapture(next)
        }
    }

    private fun triggerIntruderTrapCapture(attemptCount: Int) {
        val token = _telegramBotToken.value.trim()
        val chatId = _telegramChatId.value.trim()
        val context = getApplication<Application>()

        viewModelScope.launch {
            try {
                // Silently capture front photo
                val photoBytes = com.tana.security.SilentCameraCaptureManager.captureFrontPhoto(context)
                
                // If Telegram is configured, send alert
                if (token.isNotBlank() && chatId.isNotBlank()) {
                    com.tana.security.TelegramAlertService.sendIntruderAlertWithPhoto(
                        botToken = token,
                        chatId = chatId,
                        photoBytes = photoBytes,
                        failedAttempts = attemptCount
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Error in intruder trap capture", e)
            }
        }
    }

    fun updateSecuritySettings(
        lockEnabled: Boolean,
        pin: String,
        biometricEnabled: Boolean,
        telegramToken: String,
        telegramChatId: String,
        maxAttempts: Int = 5
    ) {
        viewModelScope.launch {
            repository.setPreference("security_app_lock_enabled", lockEnabled.toString())
            repository.setPreference("security_app_lock_pin", pin.trim())
            repository.setPreference("security_biometric_enabled", biometricEnabled.toString())
            repository.setPreference("security_telegram_bot_token", telegramToken.trim())
            repository.setPreference("security_telegram_chat_id", telegramChatId.trim())
            repository.setPreference("security_max_failed_attempts", maxAttempts.toString())

            _isAppLockEnabled.value = lockEnabled
            _appLockPin.value = pin.trim()
            _isBiometricEnabled.value = biometricEnabled
            _telegramBotToken.value = telegramToken.trim()
            _telegramChatId.value = telegramChatId.trim()
            _maxAllowedFailedAttempts.value = maxAttempts
            
            if (!lockEnabled) {
                _isAppUnlocked.value = true
                _failedUnlockAttempts.value = 0
            }
        }
    }

    fun testTelegramBotConnection(botToken: String, chatId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _telegramTestStatus.value = "Menguji koneksi ke Telegram..."
            val result = com.tana.security.TelegramAlertService.testConnection(botToken, chatId)
            result.onSuccess { msg ->
                _telegramTestStatus.value = msg
                onResult(true, msg)
            }.onFailure { err ->
                val errMsg = err.localizedMessage ?: "Koneksi gagal"
                _telegramTestStatus.value = if (errMsg.startsWith("Gagal") || errMsg.startsWith("Akses")) errMsg else "Gagal: $errMsg"
                onResult(false, errMsg)
            }
        }
    }

    fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int, customMessage: String) {
        viewModelScope.launch {
            repository.setPreference("reminder_enabled", enabled.toString())
            repository.setPreference("reminder_hour", hour.toString())
            repository.setPreference("reminder_minute", minute.toString())
            repository.setPreference("reminder_custom_msg", customMessage.trim())

            _isReminderEnabled.value = enabled
            _reminderHour.value = hour
            _reminderMinute.value = minute
            _reminderCustomMessage.value = customMessage.trim()

            val context = getApplication<Application>()
            if (enabled) {
                ReminderScheduler.scheduleDailyReminder(context, hour, minute)
            } else {
                ReminderScheduler.cancelReminder(context)
            }
        }
    }

    fun triggerTestNotification() {
        val context = getApplication<Application>()
        val msg = _reminderCustomMessage.value.ifBlank { null }
        ReminderScheduler.triggerTestNotification(context, msg)
    }

    fun sendAiPrompt(
        userPrompt: String,
        mediaAttachmentName: String? = null,
        mediaType: String? = null,
        attachedImageUri: String = ""
    ) {
        if (userPrompt.isBlank() && mediaAttachmentName == null) return
        if (_isAiLoading.value) return // Anti-spam: Ignore if already generating

        val currentKey = _openRouterApiKey.value.trim()
        val currentModel = _openRouterModel.value
        val effectivePrompt = if (userPrompt.isBlank() && mediaAttachmentName != null) {
            "Tolong analisis struktur rincian data/biaya dari file $mediaAttachmentName ini dan berikan rekomendasi rencana target tabungan yang tepat."
        } else {
            userPrompt.trim()
        }

        currentAiJob?.cancel()
        currentAiJob = viewModelScope.launch {
            val baseMsg = if (mediaAttachmentName != null) {
                "[Lampiran: $mediaAttachmentName]\n$effectivePrompt"
            } else {
                effectivePrompt
            }
            val userMsg = if (attachedImageUri.isNotEmpty()) {
                "$baseMsg\n[ATTACHMENT_URI: $attachedImageUri]"
            } else {
                baseMsg
            }
            repository.insertAiMessage(role = "user", content = userMsg)
            _isAiLoading.value = true
            _aiErrorMessage.value = null

            try {
                if (currentKey.isBlank()) {
                    // Smart heuristic offline response
                    val localSummary = repository.generateFinancialContextSummary()
                    val offlineAdvice = generateOfflineAdvice(effectivePrompt, localSummary, mediaAttachmentName, mediaType)
                    repository.insertAiMessage(
                        role = "assistant",
                        content = offlineAdvice
                    )
                } else {
                    val currentHistory = aiMessages.value
                    val assistantMsgId = repository.insertAiMessage(role = "assistant", content = "▋")
                    val streamedResponse = StringBuilder()
                    var lastUpdateTime = 0L

                    val effectiveModel = FIXED_AI_MODEL
                    val result = repository.requestAiFinancialAdviceStream(
                        context = getApplication(),
                        apiKey = currentKey,
                        modelName = FIXED_AI_MODEL,
                        userQuery = effectivePrompt,
                        mediaAttachmentName = mediaAttachmentName,
                        mediaType = mediaType,
                        attachedImageUri = attachedImageUri.ifBlank { null },
                        chatHistory = currentHistory,
                        onChunk = { token ->
                            streamedResponse.append(token)
                            val now = System.currentTimeMillis()
                            if (now - lastUpdateTime > 80L || token.contains("\n") || token.contains(" ")) {
                                lastUpdateTime = now
                                repository.updateAiMessageContent(assistantMsgId, streamedResponse.toString() + " ▋")
                            }
                        }
                    )

                    result.onSuccess { reply ->
                        repository.updateAiMessageContent(assistantMsgId, reply)
                    }.onFailure { err ->
                        if (err is kotlinx.coroutines.CancellationException) throw err
                        if (streamedResponse.isNotEmpty()) {
                            repository.updateAiMessageContent(assistantMsgId, streamedResponse.toString())
                        } else {
                            val localSummary = repository.generateFinancialContextSummary()
                            val offlineAdvice = generateOfflineAdvice(effectivePrompt, localSummary, mediaAttachmentName, mediaType)
                            val fullReply = "$offlineAdvice\n\n*(Catatan AI [$effectiveModel]: OpenRouter [${err.message?.take(60)}...], beralih ke Engine Lokal)*"
                            repository.updateAiMessageContent(assistantMsgId, fullReply)
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellation exception caused by user stopping generation
                return@launch
            } catch (e: Exception) {
                val localSummary = repository.generateFinancialContextSummary()
                val offlineAdvice = generateOfflineAdvice(effectivePrompt, localSummary, mediaAttachmentName, mediaType)
                repository.insertAiMessage(role = "assistant", content = offlineAdvice)
            } finally {
                _isAiLoading.value = false
                currentAiJob = null
            }
        }
    }

    fun stopAiGeneration() {
        if (_isAiLoading.value || currentAiJob?.isActive == true) {
            currentAiJob?.cancel()
            currentAiJob = null
            _isAiLoading.value = false
            viewModelScope.launch {
                repository.insertAiMessage(
                    role = "assistant",
                    content = "*(Respon dihentikan oleh pengguna)*"
                )
            }
        }
    }

    private fun generateOfflineAdvice(
        prompt: String,
        summary: String,
        mediaName: String? = null,
        mediaType: String? = null
    ): String {
        val lowerPrompt = prompt.lowercase()

        // 0a. Detect Explicit Stop Agent Routine Request
        // e.g. "stop setor otomatis", "matikan nabung otomatis", "stop agen", "hentikan motivasi"
        if (lowerPrompt.contains("stop") || lowerPrompt.contains("matikan") || lowerPrompt.contains("hentikan")) {
            if (lowerPrompt.contains("setor") || lowerPrompt.contains("nabung") || lowerPrompt.contains("agen") || lowerPrompt.contains("motivasi") || lowerPrompt.contains("otomatis") || lowerPrompt.contains("semua")) {
                val targetName = when {
                    lowerPrompt.contains("motivasi") -> "Motivasi"
                    lowerPrompt.contains("semua") || lowerPrompt.contains("agen") -> "ALL"
                    else -> {
                        val targetRegex = Regex("""(?:stop|matikan|hentikan)\s+(?:setor(?:kan)?|nabung)?\s*(?:otomatis)?\s*(?:ke|untuk)?\s*([A-Za-z0-9\s]+)""")
                        val match = targetRegex.find(lowerPrompt)
                        match?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() } ?: "ALL"
                    }
                }

                return """
                    Instruksi Penghentian AI Agent Terdeteksi:
                    - Target Operasi: ${if (targetName == "ALL") "Semua Rutinitas Otomatis" else targetName}
                    
                    AI Agent akan menonaktifkan jadwal eksekusi otomatis sesuai permintaan Anda. Anda dapat mengaktifkannya kembali kapan saja.
                    
                    [STOP_AGENT_ROUTINE: $targetName]
                    
                    Tekan tombol 'Konfirmasi Hentikan' di bawah untuk mengeksekusi penghentian rutinitas ini.
                """.trimIndent()
            }
        }

        // 0a1. Detect Delete Goal Intent
        if ((lowerPrompt.contains("hapus") || lowerPrompt.contains("delete")) && (lowerPrompt.contains("target") || lowerPrompt.contains("tabungan") || lowerPrompt.contains("rencana"))) {
            val goalNameRegex = Regex("""(?:hapus|delete)\s*(?:target|tabungan)?\s*([A-Za-z0-9\s]+)""")
            val matchName = goalNameRegex.find(prompt)
            val goalTitle = matchName?.groupValues?.get(1)?.trim() ?: "Target Tabungan Utama"
            return """
                Permintaan Penghapusan Target Tabungan Terdeteksi:
                - Target yang akan dihapus: $goalTitle
                
                [HAPUS_NABUNG: $goalTitle]
                
                Tekan tombol 'Hapus Target' di bawah untuk menghapus target tabungan ini dari database.
            """.trimIndent()
        }

        // 0a2. Detect Update Goal Intent
        if ((lowerPrompt.contains("ubah") || lowerPrompt.contains("ganti") || lowerPrompt.contains("edit")) && (lowerPrompt.contains("target") || lowerPrompt.contains("tabungan"))) {
            val amtRegex = Regex("""([0-9]+(?:[.,][0-9]+)?\s*(?:jt|juta|rb|ribu|k)|[0-9.,]+)""", RegexOption.IGNORE_CASE)
            val matchAmt = amtRegex.find(lowerPrompt)
            val targetAmt = if (matchAmt != null) {
                val raw = matchAmt.groupValues[1].replace(".", "").replace(",", "").lowercase().trim()
                when {
                    raw.endsWith("jt") || raw.endsWith("juta") -> (raw.replace("jt", "").replace("juta", "").trim().toDoubleOrNull() ?: 1.0) * 1_000_000
                    raw.endsWith("rb") || raw.endsWith("k") || raw.endsWith("ribu") -> (raw.replace("rb", "").replace("k", "").replace("ribu", "").trim().toDoubleOrNull() ?: 1.0) * 1_000
                    else -> raw.toDoubleOrNull() ?: 5_000_000.0
                }
            } else 5_000_000.0

            val goalNameRegex = Regex("""(?:ubah|ganti|edit)\s*(?:target|tabungan)?\s*([A-Za-z0-9\s]+)""")
            val matchName = goalNameRegex.find(prompt)
            val goalTitle = matchName?.groupValues?.get(1)?.trim()?.replace(Regex("""sebesar.*|sejumlah.*|[0-9]+.*""", RegexOption.IGNORE_CASE), "")?.trim()
                ?: "Target Tabungan Utama"

            val dailyPlan = (targetAmt / 100).coerceAtLeast(10000.0)

            return """
                Permintaan Perubahan Parameter Target Tabungan Terdeteksi:
                - Nama Target: $goalTitle
                - Target Dana Baru: ${formatRupiah(targetAmt)}
                - Setoran Rutin Baru: ${formatRupiah(dailyPlan)}
                
                [UPDATE_GOAL: $goalTitle | ${targetAmt.toLong()} | ${dailyPlan.toLong()}]
                
                Tekan tombol 'Simpan Perubahan Target' di bawah untuk memperbarui database.
            """.trimIndent()
        }

        // 0a3. Detect Fix Transaction Intent
        if (lowerPrompt.contains("perbaiki") || lowerPrompt.contains("salah ketik") || lowerPrompt.contains("koreksi")) {
            val amtRegex = Regex("""([0-9]+(?:[.,][0-9]+)?\s*(?:jt|juta|rb|ribu|k)|[0-9.,]+)""", RegexOption.IGNORE_CASE)
            val matchAmt = amtRegex.find(lowerPrompt)
            val parsedAmt = if (matchAmt != null) {
                val raw = matchAmt.groupValues[1].replace(".", "").replace(",", "").lowercase().trim()
                when {
                    raw.endsWith("jt") || raw.endsWith("juta") -> (raw.replace("jt", "").replace("juta", "").trim().toDoubleOrNull() ?: 1.0) * 1_000_000
                    raw.endsWith("rb") || raw.endsWith("k") || raw.endsWith("ribu") -> (raw.replace("rb", "").replace("k", "").replace("ribu", "").trim().toDoubleOrNull() ?: 1.0) * 1_000
                    else -> raw.toDoubleOrNull() ?: 50000.0
                }
            } else 50000.0

            val goalNameRegex = Regex("""(?:perbaiki|koreksi|salah ketik)\s*(?:transaksi|tabungan|nominal)?\s*([A-Za-z0-9\s]+)""")
            val matchName = goalNameRegex.find(prompt)
            val goalTitle = matchName?.groupValues?.get(1)?.trim()?.replace(Regex("""sebesar.*|sejumlah.*|[0-9]+.*""", RegexOption.IGNORE_CASE), "")?.trim()
                ?: "Target Tabungan Utama"

            return """
                Permintaan Perbaikan Transaksi Tabungan Terdeteksi:
                - Target Tabungan: $goalTitle
                - Nominal Perbaikan: ${formatRupiah(parsedAmt)}
                
                [UBAH_TRANSAKSI_NABUNG: $goalTitle | ${parsedAmt.toLong()} | Koreksi nominal via AI Agent]
                
                Tekan tombol 'Perbaiki Transaksi' di bawah untuk mengeksekusi koreksi nominal di database.
            """.trimIndent()
        }

        val hasDepositKeywords = lowerPrompt.contains("setor") || lowerPrompt.contains("nabung") || lowerPrompt.contains("simpan")
        val hasWithdrawKeywords = lowerPrompt.contains("tarik") || lowerPrompt.contains("ambil") || lowerPrompt.contains("kurangi")

        // 0b. Detect Withdraw / Reduce Savings Request
        // e.g. "tarik 50000 dari dana darurat", "ambil tabungan 100k"
        if (hasWithdrawKeywords && (lowerPrompt.contains("tabungan") || lowerPrompt.contains("dana") || lowerPrompt.contains("uang") || lowerPrompt.contains("rp") || lowerPrompt.contains("rb") || lowerPrompt.contains("k"))) {
            val withdrawRegex = Regex("""([0-9]+(?:[.,][0-9]+)?\s*(?:jt|juta|rb|ribu|k)|[0-9.,]+)""", RegexOption.IGNORE_CASE)
            val matchAmt = withdrawRegex.find(lowerPrompt)
            val parsedAmt = if (matchAmt != null) {
                val raw = matchAmt.groupValues[1].replace(".", "").replace(",", "").lowercase().trim()
                when {
                    raw.endsWith("jt") || raw.endsWith("juta") -> (raw.replace("jt", "").replace("juta", "").trim().toDoubleOrNull() ?: 1.0) * 1_000_000
                    raw.endsWith("rb") || raw.endsWith("k") || raw.endsWith("ribu") -> (raw.replace("rb", "").replace("k", "").replace("ribu", "").trim().toDoubleOrNull() ?: 1.0) * 1_000
                    else -> raw.toDoubleOrNull() ?: 20000.0
                }
            } else 20000.0

            val targetRegex = Regex("""(?:dari|untuk|ke)\s+([A-Za-z0-9\s]+)""")
            val matchTarget = targetRegex.find(prompt)
            val goalName = matchTarget?.groupValues?.get(1)?.trim()?.replace(Regex("""sebesar.*|sejumlah.*|sebesar.*""", RegexOption.IGNORE_CASE), "")?.trim()
                ?: "Target Tabungan Utama"

            val formattedAmt = formatRupiah(parsedAmt)
            return """
                Permintaan Penarikan Dana Tabungan Terdeteksi:
                - Target Sumber: $goalName
                - Nominal: $formattedAmt
                
                Kalkulasi Finansial:
                Penarikan dana sebesar $formattedAmt akan memotong akumulasi saldo target $goalName di database.
                
                [TARIK_NABUNG: $goalName | ${parsedAmt.toLong()} | Penarikan dana tabungan via AI TANA]
                
                Tekan tombol 'Jalankan' di bawah untuk mengeksekusi pengurangan saldo di database.
            """.trimIndent()
        }

        // 0c. Detect Recurring Auto-Deposit / Routine request
        // e.g. "nabung 10k per detik", "nabung 10k tiap detik", "setor 50rb setiap hari"
        val isRecurringKeywords = lowerPrompt.contains("per detik") || lowerPrompt.contains("tiap detik") || lowerPrompt.contains("setiap detik") ||
                lowerPrompt.contains("per menit") || lowerPrompt.contains("tiap menit") || lowerPrompt.contains("setiap menit") ||
                lowerPrompt.contains("per jam") || lowerPrompt.contains("tiap jam") || lowerPrompt.contains("setiap jam") ||
                lowerPrompt.contains("setiap hari") || lowerPrompt.contains("tiap hari") || lowerPrompt.contains("per hari") ||
                lowerPrompt.contains("otomatis") || lowerPrompt.contains("rutin") || lowerPrompt.contains("kecuali") ||
                (lowerPrompt.contains("detik") && hasDepositKeywords) || (lowerPrompt.contains("menit") && hasDepositKeywords)

        if (isRecurringKeywords && hasDepositKeywords) {
            val amtRegex = Regex("""([0-9]+(?:[.,][0-9]+)?\s*(?:jt|juta|rb|ribu|k)|[0-9.,]+)""", RegexOption.IGNORE_CASE)
            val matchAmt = amtRegex.find(lowerPrompt)
            val parsedAmt = if (matchAmt != null) {
                val raw = matchAmt.groupValues[1].replace(".", "").replace(",", "").lowercase().trim()
                when {
                    raw.endsWith("jt") || raw.endsWith("juta") -> (raw.replace("jt", "").replace("juta", "").trim().toDoubleOrNull() ?: 1.0) * 1_000_000
                    raw.endsWith("rb") || raw.endsWith("k") || raw.endsWith("ribu") -> (raw.replace("rb", "").replace("k", "").replace("ribu", "").trim().toDoubleOrNull() ?: 1.0) * 1_000
                    else -> raw.toDoubleOrNull() ?: 10000.0
                }
            } else 10000.0

            // Determine interval / schedule
            val (scheduleCode, scheduleLabel, freqUnit) = when {
                lowerPrompt.contains("detik") -> {
                    val secMatch = Regex("""(\d+)\s*detik""").find(lowerPrompt)
                    val secVal = secMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    Triple("SEC_$secVal", "Setiap $secVal Detik Realtime", "/ detik")
                }
                lowerPrompt.contains("menit") -> {
                    val minMatch = Regex("""(\d+)\s*menit""").find(lowerPrompt)
                    val minVal = minMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    Triple("MIN_$minVal", "Setiap $minVal Menit Realtime", "/ menit")
                }
                lowerPrompt.contains("jam") -> {
                    val hrMatch = Regex("""(\d+)\s*jam""").find(lowerPrompt)
                    val hrVal = hrMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    Triple("HOUR_$hrVal", "Setiap $hrVal Jam", "/ jam")
                }
                lowerPrompt.contains("kecuali minggu") -> Triple("2,3,4,5,6,7", "Setiap Hari (Kecuali Minggu)", "/ hari")
                lowerPrompt.contains("senin sampai jumat") || lowerPrompt.contains("hari kerja") -> Triple("2,3,4,5,6", "Senin - Jumat", "/ hari")
                else -> Triple("1,2,3,4,5,6,7", "Setiap Hari", "/ hari")
            }

            val targetRegex = Regex("""(?:ke|untuk|di)\s+([A-Za-z0-9\s]+)""")
            val matchTarget = targetRegex.find(prompt)
            val goalName = matchTarget?.groupValues?.get(1)?.trim()?.replace(Regex("""sebesar.*|sejumlah.*|setiap.*|tiap.*|per.*|kecuali.*""", RegexOption.IGNORE_CASE), "")?.trim()
                ?: "Target Tabungan Utama"

            val formattedAmt = formatRupiah(parsedAmt)

            return """
                Permintaan AI Agent Otomatis Mandiri Terdeteksi:
                - Tindakan: Setoran Rutin Otomatis Mandiri
                - Target Tujuan: $goalName
                - Nominal: $formattedAmt $freqUnit
                - Jadwal Operasi: $scheduleLabel
                
                Kompilasi Izin & Protokol Eksekusi:
                Setelah Anda memberikan izin konfirmasi di bawah, AI Agent TANA akan menyetorkan $formattedAmt secara mandiri dan otomatis ke target $goalName $scheduleLabel.
                
                [PROPOSE_AGENT_ROUTINE: AUTO_DEPOSIT | $goalName | ${parsedAmt.toLong()} | $scheduleCode | Setoran otomatis $formattedAmt ke $goalName ($scheduleLabel)]
                
                Tekan tombol 'Izinkan & Aktifkan Agen Otomatis' di bawah untuk memberikan otorisasi.
            """.trimIndent()
        }

        // 0d. Detect Create New Savings Goal Request
        // e.g. "buat target tabungan beli laptop 8jt", "target baru mobil 50jt", "bikin target dana darurat 10jt"
        if ((lowerPrompt.contains("target") || lowerPrompt.contains("rencana") || lowerPrompt.contains("tujuan")) && (lowerPrompt.contains("buat") || lowerPrompt.contains("bikin") || lowerPrompt.contains("tambah") || lowerPrompt.contains("baru"))) {
            val amtRegex = Regex("""([0-9]+(?:[.,][0-9]+)?\s*(?:jt|juta|rb|ribu|k)|[0-9.,]+)""", RegexOption.IGNORE_CASE)
            val matchAmt = amtRegex.find(lowerPrompt)
            val targetAmt = if (matchAmt != null) {
                val raw = matchAmt.groupValues[1].replace(".", "").replace(",", "").lowercase().trim()
                when {
                    raw.endsWith("jt") || raw.endsWith("juta") -> (raw.replace("jt", "").replace("juta", "").trim().toDoubleOrNull() ?: 5.0) * 1_000_000
                    raw.endsWith("rb") || raw.endsWith("k") || raw.endsWith("ribu") -> (raw.replace("rb", "").replace("k", "").replace("ribu", "").trim().toDoubleOrNull() ?: 5.0) * 1_000
                    else -> raw.toDoubleOrNull() ?: 5_000_000.0
                }
            } else 5_000_000.0

            val goalNameRegex = Regex("""(?:target|buat|bikin|tujuan)\s*(?:baru)?\s*(?:tabungan)?\s*([A-Za-z0-9\s]+)""")
            val matchName = goalNameRegex.find(prompt)
            val goalTitle = matchName?.groupValues?.get(1)?.trim()?.replace(Regex("""sebesar.*|sejumlah.*|[0-9]+.*""", RegexOption.IGNORE_CASE), "")?.trim()
                ?: "Target Finansial Impian"

            val dailyPlan = (targetAmt / 100).coerceAtLeast(10000.0)

            return """
                Rencana Pembuatan Target Tabungan Baru:
                - Nama Target: $goalTitle
                - Target Dana: ${formatRupiah(targetAmt)}
                - Rekomendasi Setoran Harian: ${formatRupiah(dailyPlan)} / hari
                
                [TARGET_NABUNG: $goalTitle | ${targetAmt.toLong()} | ${dailyPlan.toLong()} | DAILY]
                
                Tekan tombol 'Simpan Target Tabungan' di bawah untuk menyimpannya ke daftar target Anda.
            """.trimIndent()
        }

        // 1. Detect direct deposit intention in prompt (ONE-TIME deposit)
        // e.g. "nabung 50000", "setor 100000 ke dana darurat", "setor lagi", "tambah uang ke tabungan 50rb"
        val depositRegex = Regex("""(?:nabung|setor|simpan|tambah(?:kan)?(?:\s+uang)?)\s*(?:sebesar|sejumlah)?\s*([0-9]+(?:[.,][0-9]+)?\s*(?:jt|juta|rb|ribu|k)|[0-9.,]+)?""", RegexOption.IGNORE_CASE)
        val depositMatch = depositRegex.find(lowerPrompt)
        if (hasDepositKeywords) {
            val rawAmt = depositMatch?.groupValues?.getOrNull(1)?.replace(".", "")?.replace(",", "")?.trim()
            val parsedAmt = when {
                rawAmt.isNullOrBlank() -> 50000.0
                rawAmt.endsWith("jt") || rawAmt.endsWith("juta") -> (rawAmt.replace("jt", "").replace("juta", "").trim().toDoubleOrNull() ?: 1.0) * 1_000_000
                rawAmt.endsWith("rb") || rawAmt.endsWith("k") || rawAmt.endsWith("ribu") -> (rawAmt.replace("rb", "").replace("k", "").replace("ribu", "").trim().toDoubleOrNull() ?: 1.0) * 1_000
                else -> rawAmt.toDoubleOrNull() ?: 50000.0
            }

            // Extract goal name if specified (e.g. "ke Dana Darurat" or "untuk Beli Laptop")
            val targetNameRegex = Regex("""(?:ke|untuk|di)\s+([A-Za-z0-9\s]+)""")
            val targetMatch = targetNameRegex.find(prompt)
            val extractedGoalName = targetMatch?.groupValues?.get(1)?.trim()?.replace(Regex("""sebesar.*|sejumlah.*|lagi.*""", RegexOption.IGNORE_CASE), "")?.trim()
                ?: "Target Tabungan Utama"

            val formattedAmt = formatRupiah(parsedAmt)
            return """
                Permintaan Setoran Tabungan Baru Terdeteksi:
                - Nominal: $formattedAmt
                - Target Tujuan: $extractedGoalName
                
                Kalkulasi Finansial:
                Usulan aksi setoran baru telah disiapkan. Penambahan dana $formattedAmt ini akan dialokasikan dan dicatat ke dalam database tabungan Anda setelah Anda menyetujuinya.
                
                [SETOR_NABUNG: $extractedGoalName | ${parsedAmt.toLong()} | Setoran tabungan via AI TANA]
                
                Tekan tombol 'Jalankan' di bawah untuk mengeksekusi dan memperbarui saldo tabungan Anda di database.
            """.trimIndent()
        }

        // 2. Detect expense/income recording in prompt
        // e.g. "catat pengeluaran 45000 makan siang", "beli bensin 50rb"
        val expenseRegex = Regex("""(?:catat\s+pengeluaran|bayar|beli|biaya|makan|pengeluaran)\s*(?:sebesar)?\s*([0-9]+(?:[.,][0-9]+)?\s*(?:jt|juta|rb|ribu|k)|[0-9.,]+)""", RegexOption.IGNORE_CASE)
        val expenseMatch = expenseRegex.find(lowerPrompt)
        if (expenseMatch != null) {
            val rawAmt = expenseMatch.groupValues[1].replace(".", "").replace(",", "").trim()
            val parsedAmt = when {
                rawAmt.endsWith("jt") -> (rawAmt.replace("jt", "").trim().toDoubleOrNull() ?: 1.0) * 1_000_000
                rawAmt.endsWith("rb") || rawAmt.endsWith("k") -> (rawAmt.replace("rb", "").replace("k", "").trim().toDoubleOrNull() ?: 1.0) * 1_000
                else -> rawAmt.toDoubleOrNull() ?: 25000.0
            }
            val formattedAmt = formatRupiah(parsedAmt)
            val category = when {
                lowerPrompt.contains("makan") || lowerPrompt.contains("kopi") || lowerPrompt.contains("resto") -> "Makanan & Minuman"
                lowerPrompt.contains("bensin") || lowerPrompt.contains("parkir") || lowerPrompt.contains("ojek") -> "Transportasi"
                lowerPrompt.contains("belanja") || lowerPrompt.contains("supermarket") -> "Belanja Kebutuhan"
                lowerPrompt.contains("listrik") || lowerPrompt.contains("air") || lowerPrompt.contains("pulsa") -> "Tagihan & Utilitas"
                else -> "Pengeluaran Harian"
            }
            return """
                Pencatatan Transaksi Finansial Terdeteksi:
                - Jenis: Pengeluaran (EXPENSE)
                - Nominal: $formattedAmt
                - Kategori: $category
                - Keterangan: ${prompt.take(60)}
                
                [CATAT_TRANSAKSI: EXPENSE | ${parsedAmt.toLong()} | $category | ${prompt.take(60)}]
                
                Tekan tombol 'Simpan ke Pembukuan' di bawah untuk langsung menyimpannya ke database.
            """.trimIndent()
        }

        // 3. Greetings and Conversational Queries
        if (lowerPrompt.contains("halo") || lowerPrompt.contains("hai") || lowerPrompt.contains("pagi") || lowerPrompt.contains("siang") || lowerPrompt.contains("malam") || lowerPrompt.contains("assalamualaikum") || lowerPrompt.contains("kamu siapa")) {
            return """
                Halo! Saya TANA AI Financial Advisor. Saya siap membantu Anda menganalisa dan mengelola keuangan Anda secara cerdas:
                
                • Setor Dana Cepat: Ketik "Setor 50.000 ke Dana Darurat"
                • Tarik Dana: Ketik "Tarik 20.000 dari Tabungan"
                • Buat Target Baru: Ketik "Buat target tabungan Beli Laptop 8jt"
                • Agen Otomatis Mandiri: Ketik "Nabung 10rb tiap hari kecuali Minggu"
                • Catat Pengeluaran: Ketik "Catat pengeluaran 35.000 makan siang"
                
                Ada yang ingin Anda rencanakan atau konsultasikan hari ini?
            """.trimIndent()
        }

        if (lowerPrompt.contains("terima kasih") || lowerPrompt.contains("makasih") || lowerPrompt.contains("thanks")) {
            return "Sama-sama! Selalu jaga konsistensi menabung dan disiplin finansial Anda. Beritahu saya jika ada transaksi atau target tabungan lain yang ingin Anda catat atau jalankan."
        }

        if (mediaName != null) {
            // Extract numbers if user typed them in prompt e.g. "Catat pengeluaran 18500"
            val promptAmountRegex = Regex("""(?:catat|bayar|beli|sebesar|sejumlah)?\s*([0-9]+(?:[.,][0-9]+)?\s*(?:jt|juta|rb|ribu|k)|[0-9.,]+)""", RegexOption.IGNORE_CASE)
            val amountMatch = promptAmountRegex.find(lowerPrompt)
            val extractedAmt = if (amountMatch != null) {
                val rawAmt = amountMatch.groupValues[1].replace(".", "").replace(",", "").trim()
                when {
                    rawAmt.endsWith("jt") -> (rawAmt.replace("jt", "").trim().toDoubleOrNull() ?: 1.0) * 1_000_000
                    rawAmt.endsWith("rb") || rawAmt.endsWith("k") -> (rawAmt.replace("rb", "").replace("k", "").trim().toDoubleOrNull() ?: 1.0) * 1_000
                    else -> rawAmt.toDoubleOrNull() ?: 0.0
                }
            } else 0.0

            if (extractedAmt > 0) {
                val formattedStr = formatRupiah(extractedAmt)
                return """
                    Lampiran Berhasil Diterima: $mediaName
                    
                    Rincian Pencatatan Transaksi dari Lampiran:
                    - Total Nominal: $formattedStr
                    - Kategori: Belanja / Pengeluaran
                    - Keterangan: Transaksi dari $mediaName
                    
                    [CATAT_TRANSAKSI: EXPENSE | ${extractedAmt.toLong()} | Belanja | Transaksi dari $mediaName]
                    
                    Tekan tombol 'Simpan ke Pembukuan' di bawah untuk mengeksekusi pencatatan.
                """.trimIndent()
            }

            return """
                Lampiran Berhasil Diterima: $mediaName (${mediaType ?: "Dokumen/Struk"})
                
                Analisis Vision AI Multimodal:
                • Untuk pemindaian otomatis rincian barang & total harga langsung dari gambar/struk secara presisi, pastikan OpenRouter API Key dan model Vision (seperti google/gemini-2.0-flash-001 atau openai/gpt-4o-mini) sudah terkonfigurasi di Pengaturan AI.
                
                • Atau sebutkan nominal dari struk ini secara manual (contoh: "Catat pengeluaran 18.500 dari struk ini") agar saya bisa langsung memprosesnya ke pencatatan keuangan.
            """.trimIndent()
        }

        return """
            Ringkasan Keuangan Faktual Berdasarkan Database:
            $summary
            
            Prinsip Disiplin Pengelolaan Uang:
            1. Pisahkan Tabungan di Awal: Saat menerima pemasukan, prioritaskan alokasi minimal 20% langsung ke target tabungan sebelum dialokasikan ke pos pengeluaran lain.
            2. Real-Time Tracking: Pastikan setiap pengeluaran sekecil apapun dicatat agar kalkulasi arus kas selalu akurat.
            3. Fokus pada Target Aktif: Konsistensi setoran harian/mingguan lebih menentukan keberhasilan mencapai tujuan tabungan dibanding nominal besar yang tidak teratur.
            
            Fitur Interaksi AI Aktif:
            • Anda dapat mengetik "Nabung 50.000 ke Dana Darurat" dan saya akan menyiapkan setoran ke database.
            • Anda dapat mengetik "Catat pengeluaran 35.000 makan siang" untuk membukukan transaksi.
            • Hubungkan OpenRouter API Key di Pengaturan untuk analisis kustom multi-model (Gemini, Llama, Nemotron).
        """.trimIndent()
    }

    fun clearAiHistory() {
        viewModelScope.launch {
            repository.clearAiHistory()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.setPreference("data_seeded", "true")
            com.tana.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    companion object {
        data class GoalScheduleEstimate(
            val timesCount: Int,
            val totalCalendarDays: Int,
            val projectedCompletionMillis: Long,
            val summaryText: String
        )

        fun parseActiveDays(daysStr: String): Set<Int> {
            if (daysStr.isBlank()) return setOf(1, 2, 3, 4, 5, 6, 7)
            return daysStr.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..7 }
                .toSet()
                .ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
        }

        fun calculateGoalSchedule(
            targetAmount: Double,
            currentAmount: Double,
            fillPlanAmount: Double,
            fillPlanFrequency: String,
            activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)
        ): GoalScheduleEstimate {
            val remaining = (targetAmount - currentAmount).coerceAtLeast(0.0)
            if (remaining <= 0.0) {
                return GoalScheduleEstimate(
                    timesCount = 0,
                    totalCalendarDays = 0,
                    projectedCompletionMillis = System.currentTimeMillis(),
                    summaryText = "Target Tercapai 100%"
                )
            }

            val plan = if (fillPlanAmount > 0) fillPlanAmount else (targetAmount * 0.05).coerceAtLeast(10000.0)
            val timesNeeded = kotlin.math.ceil(remaining / plan).toInt().coerceAtLeast(1)

            val validActiveDays = if (activeDays.isEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else activeDays

            val fullDateFmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            when (fillPlanFrequency) {
                "DAILY" -> {
                    val calendar = Calendar.getInstance()
                    var savedTimes = 0
                    var elapsedCalendarDays = 0
                    while (savedTimes < timesNeeded && elapsedCalendarDays < 3650) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        elapsedCalendarDays++
                        val dow = calendar.get(Calendar.DAY_OF_WEEK)
                        if (validActiveDays.contains(dow)) {
                            savedTimes++
                        }
                    }
                    val restDaysPerWeek = 7 - validActiveDays.size
                    val detailExtra = if (restDaysPerWeek > 0) " (${restDaysPerWeek}h libur/mgg)" else ""
                    val dateFormatted = fullDateFmt.format(Date(calendar.timeInMillis))
                    return GoalScheduleEstimate(
                        timesCount = timesNeeded,
                        totalCalendarDays = elapsedCalendarDays,
                        projectedCompletionMillis = calendar.timeInMillis,
                        summaryText = "Estimasi selesai $elapsedCalendarDays hari ($dateFormatted)$detailExtra"
                    )
                }
                "WEEKLY" -> {
                    val calendarDays = timesNeeded * 7
                    val completionMillis = System.currentTimeMillis() + (calendarDays * 86400000L)
                    val dateFormatted = fullDateFmt.format(Date(completionMillis))
                    return GoalScheduleEstimate(
                        timesCount = timesNeeded,
                        totalCalendarDays = calendarDays,
                        projectedCompletionMillis = completionMillis,
                        summaryText = "Estimasi selesai $calendarDays hari ($dateFormatted)"
                    )
                }
                "MONTHLY" -> {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MONTH, timesNeeded)
                    val daysApprox = timesNeeded * 30
                    val dateFormatted = fullDateFmt.format(Date(cal.timeInMillis))
                    return GoalScheduleEstimate(
                        timesCount = timesNeeded,
                        totalCalendarDays = daysApprox,
                        projectedCompletionMillis = cal.timeInMillis,
                        summaryText = "Estimasi selesai $daysApprox hari ($dateFormatted)"
                    )
                }
                else -> {
                    val calendarDays = timesNeeded
                    val completionMillis = System.currentTimeMillis() + (timesNeeded * 86400000L)
                    val dateFormatted = fullDateFmt.format(Date(completionMillis))
                    return GoalScheduleEstimate(
                        timesCount = timesNeeded,
                        totalCalendarDays = calendarDays,
                        projectedCompletionMillis = completionMillis,
                        summaryText = "Estimasi selesai $calendarDays hari ($dateFormatted)"
                    )
                }
            }
        }

        var activeCurrencySymbol: String = "Rp"

        fun formatRupiah(amount: Double): String {
            val absAmount = kotlin.math.abs(amount)
            val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
            formatter.maximumFractionDigits = 0
            val formattedNum = formatter.format(absAmount)
            val prefix = if (amount < 0) "-" else ""
            return if (activeCurrencySymbol.isBlank()) "$prefix$formattedNum" else "$prefix$activeCurrencySymbol $formattedNum"
        }

        fun formatCompact(amount: Double): String {
            val formatted = when {
                amount >= 1_000_000_000 -> String.format(Locale.US, "%.1fM", amount / 1_000_000_000)
                amount >= 1_000_000 -> String.format(Locale.US, "%.1fJt", amount / 1_000_000)
                amount >= 1_000 -> String.format(Locale.US, "%.0fRb", amount / 1_000)
                else -> String.format(Locale.US, "%.0f", amount)
            }
            return if (activeCurrencySymbol.isBlank()) formatted else "$activeCurrencySymbol $formatted"
        }

        fun calculatePredictiveSaving(
            transactions: List<TransactionEntity>,
            savingsGoals: List<SavingsGoalEntity>,
            selectedGoalId: String? = "ALL"
        ): PredictiveSavingResult {
            val targetGoal = if (selectedGoalId != null && selectedGoalId != "ALL") {
                savingsGoals.find { it.id.toString() == selectedGoalId }
            } else null

            val goalTitle = targetGoal?.title ?: if (savingsGoals.isEmpty()) "Semua Tabungan" else "Semua Target Tabungan"
            val targetAmount = targetGoal?.targetAmount ?: (if (savingsGoals.isNotEmpty()) savingsGoals.sumOf { it.targetAmount } else 10_000_000.0)
            val currentAmount = targetGoal?.currentAmount ?: (if (savingsGoals.isNotEmpty()) savingsGoals.sumOf { it.currentAmount } else 0.0)
            val remainingAmount = (targetAmount - currentAmount).coerceAtLeast(0.0)

            val relevantTxs = transactions.filter {
                it.type == TransactionType.SAVINGS && (targetGoal == null || it.goalId == targetGoal.id || it.goalId == null)
            }.sortedBy { it.timestamp }

            val now = System.currentTimeMillis()
            val totalSavingsFromTxs = relevantTxs.sumOf { it.amount }
            val txCount = relevantTxs.size

            // Calculate historical time span and daily velocity
            val earliestTimestamp = relevantTxs.firstOrNull()?.timestamp ?: (now - 14 * 86400000L)
            val elapsedDays = ((now - earliestTimestamp) / 86400000L).coerceAtLeast(1).toDouble()

            // Determine average daily savings rate from actual history or fallback to goal plan
            val avgDailySavings = if (totalSavingsFromTxs > 0 && txCount > 0) {
                val calculatedRate = totalSavingsFromTxs / elapsedDays
                val recentTxs = relevantTxs.filter { it.timestamp >= now - 30 * 86400000L }
                if (recentTxs.isNotEmpty() && elapsedDays > 30) {
                    val recentDays = ((now - recentTxs.first().timestamp) / 86400000L).coerceAtLeast(1).toDouble()
                    val recentRate = recentTxs.sumOf { it.amount } / recentDays
                    (calculatedRate * 0.35 + recentRate * 0.65).coerceAtLeast(1000.0)
                } else {
                    calculatedRate.coerceAtLeast(1000.0)
                }
            } else {
                val planAmt = targetGoal?.fillPlanAmount ?: if (savingsGoals.isNotEmpty()) savingsGoals.sumOf { it.fillPlanAmount } else 0.0
                val planFreq = targetGoal?.fillPlanFrequency ?: "DAILY"
                if (planAmt > 0) {
                    when (planFreq) {
                        "DAILY" -> planAmt
                        "WEEKLY" -> planAmt / 7.0
                        "MONTHLY" -> planAmt / 30.0
                        else -> planAmt
                    }
                } else {
                    (remainingAmount / 60.0).coerceAtLeast(25000.0)
                }
            }

            val avgWeeklySavings = avgDailySavings * 7.0
            val avgMonthlySavings = avgDailySavings * 30.0

            val projectedDays = if (remainingAmount <= 0.0) 0 else {
                kotlin.math.ceil(remainingAmount / avgDailySavings).toInt().coerceIn(0, 3650)
            }
            val projectedCompletionMillis = now + (projectedDays.toLong() * 86400000L)

            val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            val projectedDateFormatted = if (remainingAmount <= 0.0) "Target Sudah Tercapai!" else dateFormat.format(Date(projectedCompletionMillis))

            val consistencyScore = when {
                txCount >= 10 -> 95
                txCount >= 5 -> 82
                txCount >= 2 -> 70
                txCount == 1 -> 58
                else -> 50
            }
            val consistencyLabel = when {
                remainingAmount <= 0.0 -> "Tercapai Sempurna"
                consistencyScore >= 85 -> "Sangat Konsisten"
                consistencyScore >= 70 -> "Stabil & On Track"
                consistencyScore >= 50 -> "Cukup Baik"
                else -> "Estimasi Awal"
            }

            val acceleratedDaily = avgDailySavings * 1.25
            val acceleratedDays = if (remainingAmount <= 0.0) 0 else kotlin.math.ceil(remainingAmount / acceleratedDaily).toInt()
            val acceleratedDaysSaved = (projectedDays - acceleratedDays).coerceAtLeast(0)

            val points = mutableListOf<PredictivePoint>()
            val shortDateFormat = SimpleDateFormat("d MMM", Locale("id", "ID"))

            val sortedRelevantTxs = relevantTxs.sortedBy { it.timestamp }
            val pastDaysOffsets = listOf(28, 21, 14, 7) // Chronological order: 28 days ago -> 21 -> 14 -> 7 -> Today
            pastDaysOffsets.forEach { daysAgo ->
                val timePoint = now - (daysAgo * 86400000L)
                val txsUpToPoint = sortedRelevantTxs.filter { it.timestamp <= timePoint }
                val historicalBalance = if (txsUpToPoint.isNotEmpty()) {
                    txsUpToPoint.sumOf { it.amount }.coerceAtLeast(0.0)
                } else {
                    (currentAmount * (1.0 - (daysAgo / 35.0))).coerceAtLeast(0.0)
                }
                points.add(
                    PredictivePoint(
                        dateLabel = shortDateFormat.format(Date(timePoint)),
                        timestamp = timePoint,
                        amount = historicalBalance,
                        isProjected = false
                    )
                )
            }

            // Today Point
            points.add(
                PredictivePoint(
                    dateLabel = "Hari Ini",
                    timestamp = now,
                    amount = currentAmount,
                    isProjected = false,
                    isMilestone = true,
                    milestoneLabel = "Hari Ini"
                )
            )

            // Future Projected Points
            if (projectedDays > 0 && remainingAmount > 0) {
                val futureSteps = listOf(0.25f, 0.50f, 0.75f, 1.0f)
                futureSteps.forEach { ratio ->
                    val futureDays = (projectedDays * ratio).toInt().coerceAtLeast(1)
                    val futureTime = now + (futureDays * 86400000L)
                    val futureAmount = (currentAmount + (remainingAmount * ratio)).coerceAtMost(targetAmount)
                    val isTarget = ratio >= 1.0f
                    points.add(
                        PredictivePoint(
                            dateLabel = if (isTarget) "Tercapai" else "+${futureDays}h",
                            timestamp = futureTime,
                            amount = futureAmount,
                            isProjected = true,
                            isMilestone = isTarget,
                            milestoneLabel = if (isTarget) "Target (${shortDateFormat.format(Date(futureTime))})" else null
                        )
                    )
                }
            } else {
                points.add(
                    PredictivePoint(
                        dateLabel = "Tercapai",
                        timestamp = now + 86400000L,
                        amount = targetAmount,
                        isProjected = true,
                        isMilestone = true,
                        milestoneLabel = "Target Selesai"
                    )
                )
            }

            return PredictiveSavingResult(
                goalTitle = goalTitle,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                remainingAmount = remainingAmount,
                avgDailySavings = avgDailySavings,
                avgWeeklySavings = avgWeeklySavings,
                avgMonthlySavings = avgMonthlySavings,
                projectedDaysRemaining = projectedDays,
                projectedCompletionMillis = projectedCompletionMillis,
                projectedCompletionDateFormatted = projectedDateFormatted,
                totalHistoricalSavingsTxCount = txCount,
                consistencyScore = consistencyScore,
                consistencyLabel = consistencyLabel,
                acceleratedDaysSaved = acceleratedDaysSaved,
                acceleratedDailyAmount = avgDailySavings * 0.25,
                points = points
            )
        }
    }
}

data class PredictivePoint(
    val dateLabel: String,
    val timestamp: Long,
    val amount: Double,
    val isProjected: Boolean,
    val isMilestone: Boolean = false,
    val milestoneLabel: String? = null
)

data class PredictiveSavingResult(
    val goalTitle: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val remainingAmount: Double,
    val avgDailySavings: Double,
    val avgWeeklySavings: Double,
    val avgMonthlySavings: Double,
    val projectedDaysRemaining: Int,
    val projectedCompletionMillis: Long,
    val projectedCompletionDateFormatted: String,
    val totalHistoricalSavingsTxCount: Int,
    val consistencyScore: Int,
    val consistencyLabel: String,
    val acceleratedDaysSaved: Int,
    val acceleratedDailyAmount: Double,
    val points: List<PredictivePoint>
)
