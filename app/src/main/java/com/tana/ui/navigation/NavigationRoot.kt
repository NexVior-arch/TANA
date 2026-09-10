package com.tana.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tana.data.model.TransactionType
import com.tana.ui.components.AddTransactionSheet
import com.tana.ui.components.NabungActionSheet
import com.tana.ui.screens.AiScreen
import com.tana.ui.screens.DashboardScreen
import com.tana.ui.screens.FullScreenSecurityLockScreen
import com.tana.ui.screens.SavingsScreen
import com.tana.ui.screens.SettingsScreen
import com.tana.ui.screens.TransactionsScreen
import com.tana.ui.viewmodel.FinanceViewModel

enum class AppNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Ringkasan", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_tab_dashboard"),
    TRANSACTIONS("Transaksi", Icons.Filled.Receipt, Icons.Outlined.Receipt, "nav_tab_transactions"),
    SAVINGS("Tabungan", Icons.Filled.Savings, Icons.Outlined.Savings, "nav_tab_savings"),
    AI("AI Asisten", Icons.Filled.Psychology, Icons.Outlined.Psychology, "nav_tab_ai")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NavigationRoot(
    viewModel: FinanceViewModel,
    initialWidgetAction: String? = null,
    onWidgetActionHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppNavTab.DASHBOARD) }
    var tabHistory by remember { mutableStateOf(listOf(AppNavTab.DASHBOARD)) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    // Add Transaction Sheet State
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var showNabungActionSheet by remember { mutableStateOf(false) }
    var addInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var addPreselectedGoalId by remember { mutableStateOf<Long?>(null) }

    // Navigation function that records previous tabs in history
    fun navigateToTab(targetTab: AppNavTab) {
        if (targetTab != currentTab) {
            tabHistory = tabHistory + targetTab
            currentTab = targetTab
        }
    }

    // Custom Back Button behavior: Return to previous tab or close open sheets/dialogs before exiting
    BackHandler(enabled = true) {
        if (showSettingsScreen) {
            showSettingsScreen = false
        } else if (showAddTransactionSheet) {
            showAddTransactionSheet = false
        } else if (showNabungActionSheet) {
            showNabungActionSheet = false
        } else if (tabHistory.size > 1) {
            val updatedHistory = tabHistory.dropLast(1)
            tabHistory = updatedHistory
            currentTab = updatedHistory.last()
        } else if (currentTab != AppNavTab.DASHBOARD) {
            currentTab = AppNavTab.DASHBOARD
            tabHistory = listOf(AppNavTab.DASHBOARD)
        } else {
            // No more previous tabs in history and on Dashboard -> Exit app
            (context as? android.app.Activity)?.finish()
        }
    }

    LaunchedEffect(initialWidgetAction) {
        if (initialWidgetAction != null) {
            when (initialWidgetAction) {
                "ACTION_DEPOSIT", "ACTION_WITHDRAW" -> {
                    navigateToTab(AppNavTab.SAVINGS)
                    showNabungActionSheet = true
                    onWidgetActionHandled()
                }
                "ACTION_OPEN_SAVINGS" -> {
                    navigateToTab(AppNavTab.SAVINGS)
                    onWidgetActionHandled()
                }
            }
        }
    }

    // Collect StateFlows
    val transactions by viewModel.transactions.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val savingsErrorMessage by viewModel.savingsErrorMessage.collectAsState()
    val dashboardSelectedGoalId by viewModel.dashboardSelectedGoalId.collectAsState()
    val selectedChartMode by viewModel.selectedChartMode.collectAsState()
    val aiMessages by viewModel.aiMessages.collectAsState()
    val agentRoutines by viewModel.agentRoutines.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsState()
    val openRouterModel by viewModel.openRouterModel.collectAsState()
    val isReminderEnabled by viewModel.isReminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()
    val reminderCustomMessage by viewModel.reminderCustomMessage.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val aiTestResult by viewModel.aiTestResult.collectAsState()
    val isAiTestLoading by viewModel.isAiTestLoading.collectAsState()

    // Security & App Lock States
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val appLockPin by viewModel.appLockPin.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val telegramBotToken by viewModel.telegramBotToken.collectAsState()
    val telegramChatId by viewModel.telegramChatId.collectAsState()
    val failedUnlockAttempts by viewModel.failedUnlockAttempts.collectAsState()
    val maxAllowedFailedAttempts by viewModel.maxAllowedFailedAttempts.collectAsState()
    val telegramTestStatus by viewModel.telegramTestStatus.collectAsState()

    // If App Lock is enabled and user has not unlocked yet, render FULL SCREEN lock screen!
    if (isAppLockEnabled && !isAppUnlocked && appLockPin.isNotBlank()) {
        FullScreenSecurityLockScreen(
            correctPin = appLockPin,
            isBiometricAllowed = isBiometricEnabled,
            failedAttemptsCount = failedUnlockAttempts,
            maxFailedAttempts = maxAllowedFailedAttempts,
            onUnlockSuccess = {
                viewModel.setAppUnlocked(true)
            },
            onFailedAttempt = {
                viewModel.recordFailedUnlockAttempt()
            }
        )
        return
    }

    // Request Notification Permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val isImeVisible = WindowInsets.isImeVisible

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        bottomBar = {
            if (!showSettingsScreen && (!isImeVisible || currentTab != AppNavTab.AI)) {
                CustomBottomNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = { navigateToTab(it) },
                    onFabClick = {
                        showNabungActionSheet = true
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showSettingsScreen) {
                SettingsScreenHost(
                    viewModel = viewModel,
                    onBack = { showSettingsScreen = false }
                )
            } else {
                Crossfade(
                    targetState = currentTab,
                    animationSpec = tween(300),
                    label = "tab_crossfade"
                ) { tab ->
                    when (tab) {
                        AppNavTab.DASHBOARD -> DashboardScreen(
                            transactions = transactions,
                            savingsGoals = savingsGoals,
                            selectedSavingsGoalId = dashboardSelectedGoalId,
                            onSelectSavingsGoal = { viewModel.setDashboardSelectedGoalId(it) },
                            selectedChartMode = selectedChartMode,
                            onChartModeChanged = { viewModel.setSelectedChartMode(it) },
                            onOpenAddTransaction = { type ->
                                addInitialType = type
                                addPreselectedGoalId = null
                                showAddTransactionSheet = true
                            },
                            onOpenWithdrawSavings = { goalId ->
                                navigateToTab(AppNavTab.SAVINGS)
                            },
                            onNavigateToTransactions = { navigateToTab(AppNavTab.TRANSACTIONS) },
                            onNavigateToSavings = { navigateToTab(AppNavTab.SAVINGS) },
                            onNavigateToSettings = { showSettingsScreen = true },
                            onQuickDepositGoal = { goalId ->
                                addInitialType = TransactionType.SAVINGS
                                addPreselectedGoalId = goalId
                                showAddTransactionSheet = true
                            }
                        )

                        AppNavTab.TRANSACTIONS -> TransactionsScreen(
                            transactions = transactions,
                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                            onOpenAddTransaction = { type ->
                                addInitialType = type
                                addPreselectedGoalId = null
                                showAddTransactionSheet = true
                            }
                        )

                        AppNavTab.SAVINGS -> SavingsScreen(
                            savingsGoals = savingsGoals,
                            onAddGoal = { title, target, initial, planAmt, planFreq, activeDays, reminder, note ->
                                viewModel.addSavingsGoal(
                                    title = title,
                                    targetAmount = target,
                                    initialAmount = initial,
                                    fillPlanFrequency = planFreq,
                                    fillPlanAmount = planAmt,
                                    activeDays = activeDays.joinToString(","),
                                    reminderEnabled = true,
                                    reminderHour = 20,
                                    reminderMinute = 0,
                                    note = note
                                )
                            },
                            onDepositGoal = { goalId, amount, note ->
                                viewModel.depositToGoal(goalId, amount, note)
                            },
                            onWithdrawGoal = { goalId, amount, note ->
                                viewModel.withdrawFromGoal(goalId, amount, note)
                            },
                            onDeleteGoal = { viewModel.deleteSavingsGoal(it) },
                            onUpdateGoal = { viewModel.updateSavingsGoal(it) },
                            onUpdateSavingsTransaction = { tx, amt, note, time ->
                                viewModel.updateSavingsTransaction(tx, amt, note, time)
                            },
                            onDeleteSavingsTransaction = { txId, goalId ->
                                viewModel.deleteSavingsTransaction(txId, goalId)
                            },
                            errorMessage = savingsErrorMessage,
                            onErrorShown = { viewModel.clearSavingsErrorMessage() },
                            transactions = transactions
                        )

                        AppNavTab.AI -> AiScreen(
                            aiMessages = aiMessages,
                            agentRoutines = agentRoutines,
                            isAiLoading = isAiLoading,
                            openRouterApiKey = openRouterApiKey,
                            openRouterModel = openRouterModel,
                            onSendMessage = { prompt, mediaName, mediaType, imageUri ->
                                viewModel.sendAiPrompt(prompt, mediaName, mediaType, imageUri)
                            },
                            onClearHistory = { viewModel.clearAiHistory() },
                            onNavigateToSettings = { showSettingsScreen = true },
                            onAddGoalFromAi = { title, target, initial, planAmt, planFreq, activeDays, reminder, note ->
                                viewModel.addSavingsGoal(
                                    title = title,
                                    targetAmount = target,
                                    initialAmount = initial,
                                    fillPlanFrequency = planFreq,
                                    fillPlanAmount = planAmt,
                                    activeDays = activeDays.joinToString(","),
                                    reminderEnabled = true,
                                    reminderHour = 20,
                                    reminderMinute = 0,
                                    note = note
                                )
                            },
                            onDepositFromAi = { goalName, amount, note ->
                                viewModel.depositToGoalByName(goalName, amount, note)
                            },
                            onWithdrawFromAi = { goalName, amount, note ->
                                viewModel.withdrawFromGoalByName(goalName, amount, note)
                            },
                            onDeleteGoalFromAi = { goalName ->
                                viewModel.deleteSavingsGoalByName(goalName)
                            },
                            onUpdateGoalFromAi = { goalName, newTarget, newPlan ->
                                viewModel.updateGoalByName(goalName, newTarget, newPlan)
                            },
                            onFixSavingsTransactionFromAi = { goalName, newAmt, newNote ->
                                viewModel.fixLastSavingsTransactionByName(goalName, newAmt, newNote)
                            },
                            onAddTransactionFromAi = { type, amount, category, note ->
                                viewModel.addTransaction(
                                    type = type,
                                    amount = amount,
                                    category = category,
                                    note = note
                                )
                            },
                            onCreateAgentRoutine = { type, title, goalName, amount, scheduleDays, desc ->
                                viewModel.createAgentRoutine(
                                    type = type,
                                    title = title,
                                    description = desc,
                                    targetGoalName = goalName,
                                    amount = amount,
                                    activeDaysCsv = scheduleDays
                                )
                            },
                            onStopAgentRoutine = { query ->
                                viewModel.stopAgentRoutinesByQuery(query)
                            },
                            onToggleAgentRoutine = { id, enabled ->
                                viewModel.toggleAgentRoutine(id, enabled)
                            },
                            onDeleteAgentRoutine = { id ->
                                viewModel.deleteAgentRoutine(id)
                            },
                            onExecuteRoutinesNow = {
                                viewModel.executePendingAgentRoutinesNow()
                            },
                            onUpdateAiMessageContent = { id, content ->
                                viewModel.updateAiMessageContent(id, content)
                            },
                            onStopAiGeneration = {
                                viewModel.stopAiGeneration()
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Nabung Action Sheet
    if (showNabungActionSheet) {
        val activeGoals = savingsGoals.filter { !it.isCompleted }
        NabungActionSheet(
            activeSavingsGoals = activeGoals,
            transactions = transactions,
            onDismiss = { showNabungActionSheet = false },
            onDepositGoal = { goalId, amount, note ->
                viewModel.depositToGoal(goalId, amount, note)
            },
            onWithdrawGoal = { goalId, amount, note ->
                viewModel.withdrawFromGoal(goalId, amount, note)
            },
            onCreateNewGoal = {
                currentTab = AppNavTab.SAVINGS
            }
        )
    }

    // Modal Add Transaction Sheet
    if (showAddTransactionSheet) {
        val activeGoals = savingsGoals.filter { !it.isCompleted }
        AddTransactionSheet(
            activeSavingsGoals = activeGoals,
            initialType = addInitialType,
            preselectedGoalId = addPreselectedGoalId,
            onDismiss = { showAddTransactionSheet = false },
            onSave = { type, amount, category, note, goalId ->
                viewModel.addTransaction(
                    type = type,
                    amount = amount,
                    category = category,
                    note = note,
                    goalId = goalId
                )
            }
        )
    }
}

/**
 * Isolated host for SettingsScreen. Collects ONLY the StateFlows Settings actually
 * needs (11, not the 28 collected at NavigationRoot's top level) and passes stable
 * method references instead of freshly-allocated lambdas. This means Settings no
 * longer recomposes every time unrelated state changes elsewhere in the app (e.g.
 * `transactions`/`savingsGoals` updating from the autonomous AI deposit agent every
 * few seconds while the user is sitting on the Settings screen) - that unrelated
 * churn was the cause of Settings feeling laggy.
 */
@Composable
private fun SettingsScreenHost(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsState()
    val openRouterModel by viewModel.openRouterModel.collectAsState()
    val isReminderEnabled by viewModel.isReminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()
    val reminderCustomMessage by viewModel.reminderCustomMessage.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val aiTestResult by viewModel.aiTestResult.collectAsState()
    val isAiTestLoading by viewModel.isAiTestLoading.collectAsState()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val appLockPin by viewModel.appLockPin.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val telegramBotToken by viewModel.telegramBotToken.collectAsState()
    val telegramChatId by viewModel.telegramChatId.collectAsState()
    val maxAllowedFailedAttempts by viewModel.maxAllowedFailedAttempts.collectAsState()
    val telegramTestStatus by viewModel.telegramTestStatus.collectAsState()

    SettingsScreen(
        openRouterApiKey = openRouterApiKey,
        openRouterModel = openRouterModel,
        isReminderEnabled = isReminderEnabled,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        reminderCustomMessage = reminderCustomMessage,
        currencyCode = currencyCode,
        currencySymbol = currencySymbol,
        isAppLockEnabled = isAppLockEnabled,
        appLockPin = appLockPin,
        isBiometricEnabled = isBiometricEnabled,
        telegramBotToken = telegramBotToken,
        telegramChatId = telegramChatId,
        maxAllowedFailedAttempts = maxAllowedFailedAttempts,
        telegramTestStatus = telegramTestStatus,
        aiTestResult = aiTestResult,
        isAiTestLoading = isAiTestLoading,
        onSaveApiKey = remember(viewModel) { { key: String -> viewModel.setOpenRouterApiKey(key) } },
        onSelectModel = remember(viewModel) { { model: String -> viewModel.setOpenRouterModel(model) } },
        onTestOpenRouter = remember(viewModel) { { key: String, model: String -> viewModel.testOpenRouterConnection(key, model) } },
        onClearTestResult = remember(viewModel) { { viewModel.clearAiTestResult() } },
        onUpdateReminder = remember(viewModel) {
            { enabled: Boolean, h: Int, m: Int, msg: String -> viewModel.updateReminderSettings(enabled, h, m, msg) }
        },
        onTriggerTestNotification = remember(viewModel) { { viewModel.triggerTestNotification() } },
        onUpdateSecuritySettings = remember(viewModel) {
            { lockEnabled: Boolean, pin: String, bioEnabled: Boolean, botToken: String, chatId: String, maxAttempts: Int ->
                viewModel.updateSecuritySettings(lockEnabled, pin, bioEnabled, botToken, chatId, maxAttempts)
            }
        },
        onTestTelegramConnection = remember(viewModel) {
            { botToken: String, chatId: String -> viewModel.testTelegramBotConnection(botToken, chatId) { _, _ -> } }
        },
        onSetCurrency = remember(viewModel) { { code: String, sym: String -> viewModel.setCurrency(code, sym) } },
        onClearAllData = remember(viewModel) { { viewModel.clearAllData() } },
        onBack = onBack
    )
}

@Composable
private fun CustomBottomNavigationBar(
    currentTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 2 tabs: Dashboard, Transactions
                BottomNavItem(
                    tab = AppNavTab.DASHBOARD,
                    isSelected = currentTab == AppNavTab.DASHBOARD,
                    onClick = { onTabSelected(AppNavTab.DASHBOARD) },
                    modifier = Modifier.weight(1f)
                )

                BottomNavItem(
                    tab = AppNavTab.TRANSACTIONS,
                    isSelected = currentTab == AppNavTab.TRANSACTIONS,
                    onClick = { onTabSelected(AppNavTab.TRANSACTIONS) },
                    modifier = Modifier.weight(1f)
                )

                // Center Floating Action Button with modern gradient halo
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFFC6A15B)
                                )
                            )
                        )
                        .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)), CircleShape)
                        .clickable { onFabClick() }
                        .testTag("floating_add_transaction_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Catat Transaksi",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Right 2 tabs: Savings, AI
                BottomNavItem(
                    tab = AppNavTab.SAVINGS,
                    isSelected = currentTab == AppNavTab.SAVINGS,
                    onClick = { onTabSelected(AppNavTab.SAVINGS) },
                    modifier = Modifier.weight(1f)
                )

                BottomNavItem(
                    tab = AppNavTab.AI,
                    isSelected = currentTab == AppNavTab.AI,
                    onClick = { onTabSelected(AppNavTab.AI) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: AppNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag(tab.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.title,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(20.dp)
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isSelected,
            enter = androidx.compose.animation.fadeIn(tween(180)) + androidx.compose.animation.expandVertically(tween(180)),
            exit = androidx.compose.animation.fadeOut(tween(120)) + androidx.compose.animation.shrinkVertically(tween(120))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    ),
                    color = activeColor,
                    maxLines = 1
                )
            }
        }
    }
}

