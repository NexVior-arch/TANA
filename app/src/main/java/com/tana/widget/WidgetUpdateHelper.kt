package com.tana.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import com.tana.R
import com.tana.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

object WidgetUpdateHelper {

    private const val PREFS_NAME = "finmonochrome_widget_prefs"
    private const val KEY_SELECTED_GOAL_ID = "selected_widget_goal_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSelectedGoalId(context: Context): String {
        return getPrefs(context).getString(KEY_SELECTED_GOAL_ID, "ALL") ?: "ALL"
    }

    fun setSelectedGoalId(context: Context, goalId: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_GOAL_ID, goalId).apply()
        updateAllWidgets(context)
    }

    suspend fun cycleWidgetGoal(context: Context, isNext: Boolean) {
        val db = AppDatabase.getInstance(context)
        val goals = db.savingsGoalDao().getAllGoalsDirect()
        val options = mutableListOf("ALL")
        options.addAll(goals.map { it.id.toString() })

        if (options.size <= 1) return

        val currentGoalId = getSelectedGoalId(context)
        val currentIndex = options.indexOf(currentGoalId).let { if (it == -1) 0 else it }

        val newIndex = if (isNext) {
            (currentIndex + 1) % options.size
        } else {
            (currentIndex - 1 + options.size) % options.size
        }

        val newGoalId = options[newIndex]
        getPrefs(context).edit().putString(KEY_SELECTED_GOAL_ID, newGoalId).apply()
        
        try {
            db.userPreferenceDao().setPreference(
                com.tana.data.model.UserPreferenceEntity("dashboard_selected_goal_id", newGoalId)
            )
        } catch (_: Exception) {}

        updateAllWidgets(context)
    }

    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val goals = db.savingsGoalDao().getAllGoalsDirect()

                var selectedGoalId = getSelectedGoalId(context)
                if (selectedGoalId == "ALL" && goals.isEmpty()) {
                    selectedGoalId = "ALL"
                }

                val selectedGoal = if (selectedGoalId != "ALL") {
                    goals.find { it.id.toString() == selectedGoalId }
                } else null

                val totalCurrentSavings = goals.sumOf { it.currentAmount }

                val displayedCurrent = selectedGoal?.currentAmount ?: totalCurrentSavings
                val displayedTitle = selectedGoal?.title ?: if (goals.isEmpty()) "Tabungan Utama" else "Semua Tabungan"
                val headerTitle = if (selectedGoal != null) "Target Tabungan" else "Total Semua Tabungan"

                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                    maximumFractionDigits = 0
                }
                val formattedCurrent = formatter.format(displayedCurrent)
                val compactCurrent = formatCompactRupiah(displayedCurrent)

                val appWidgetManager = AppWidgetManager.getInstance(context)

                // 1. Large Widget ("Dashboard" style)
                val largeComponent = ComponentName(context, SavingsLargeWidgetProvider::class.java)
                val largeIds = appWidgetManager.getAppWidgetIds(largeComponent)
                if (largeIds != null && largeIds.isNotEmpty()) {
                    for (appWidgetId in largeIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_savings_large)
                        views.setTextViewText(R.id.tv_widget_large_header, headerTitle)
                        views.setTextViewText(R.id.tv_widget_large_active_goal, displayedTitle)
                        views.setTextViewText(R.id.tv_widget_large_amount, formattedCurrent)

                        // Cycle Goal PendingIntent via Broadcast (stays on homescreen)
                        val nextIntent = Intent(context, SavingsLargeWidgetProvider::class.java).apply {
                            action = SavingsLargeWidgetProvider.ACTION_NEXT_GOAL
                        }
                        val cyclePendingIntent = PendingIntent.getBroadcast(
                            context,
                            402,
                            nextIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        views.setOnClickPendingIntent(R.id.btn_widget_large_next_goal, cyclePendingIntent)
                        views.setOnClickPendingIntent(R.id.layout_widget_large_goal_selector, cyclePendingIntent)

                        // Quick Setor & Tarik buttons open QuickWidgetActionActivity in isolated task
                        views.setOnClickPendingIntent(
                            R.id.btn_widget_large_deposit,
                            createQuickActionPendingIntent(context, "ACTION_DEPOSIT", 101)
                        )
                        views.setOnClickPendingIntent(
                            R.id.btn_widget_large_withdraw,
                            createQuickActionPendingIntent(context, "ACTION_WITHDRAW", 102)
                        )

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }

                // 2. Small Widget ("Quick Action" 2x2 style)
                val smallComponent = ComponentName(context, SavingsSmallWidgetProvider::class.java)
                val smallIds = appWidgetManager.getAppWidgetIds(smallComponent)
                if (smallIds != null && smallIds.isNotEmpty()) {
                    for (appWidgetId in smallIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_savings_small)
                        views.setTextViewText(R.id.tv_widget_small_title, displayedTitle)
                        views.setTextViewText(R.id.tv_widget_small_amount, compactCurrent)

                        views.setOnClickPendingIntent(
                            R.id.btn_widget_small_deposit,
                            createQuickActionPendingIntent(context, "ACTION_DEPOSIT", 201)
                        )
                        views.setOnClickPendingIntent(
                            R.id.btn_widget_small_withdraw,
                            createQuickActionPendingIntent(context, "ACTION_WITHDRAW", 202)
                        )

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }

                // 3. Quick Action Capsule Widget
                val quickComponent = ComponentName(context, SavingsQuickActionWidgetProvider::class.java)
                val quickIds = appWidgetManager.getAppWidgetIds(quickComponent)
                if (quickIds != null && quickIds.isNotEmpty()) {
                    for (appWidgetId in quickIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_savings_quick_action)

                        views.setOnClickPendingIntent(
                            R.id.btn_quick_deposit,
                            createQuickActionPendingIntent(context, "ACTION_DEPOSIT", 301)
                        )
                        views.setOnClickPendingIntent(
                            R.id.btn_quick_withdraw,
                            createQuickActionPendingIntent(context, "ACTION_WITHDRAW", 302)
                        )

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("WidgetUpdateHelper", "Error updating widgets", e)
            }
        }
    }

    private fun formatCompactRupiah(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> {
                val formatted = String.format(Locale("id", "ID"), "%.1f", amount / 1_000_000_000)
                    .replace(",0", "")
                "Rp $formatted Miliar"
            }
            amount >= 1_000_000 -> {
                val formatted = String.format(Locale("id", "ID"), "%.1f", amount / 1_000_000)
                    .replace(",0", "")
                "Rp $formatted Juta"
            }
            amount >= 1_000 -> {
                val formatted = String.format(Locale("id", "ID"), "%.1f", amount / 1_000)
                    .replace(",0", "")
                "Rp $formatted Ribu"
            }
            else -> {
                "Rp ${amount.toInt()}"
            }
        }
    }

    private fun createQuickActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, QuickWidgetActionActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
