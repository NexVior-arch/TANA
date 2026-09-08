package com.tana.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SavingsLargeWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV_GOAL = "com.tana.widget.ACTION_PREV_GOAL"
        const val ACTION_NEXT_GOAL = "com.tana.widget.ACTION_NEXT_GOAL"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PREV_GOAL -> {
                CoroutineScope(Dispatchers.IO).launch {
                    WidgetUpdateHelper.cycleWidgetGoal(context, isNext = false)
                }
            }
            ACTION_NEXT_GOAL -> {
                CoroutineScope(Dispatchers.IO).launch {
                    WidgetUpdateHelper.cycleWidgetGoal(context, isNext = true)
                }
            }
        }
    }
}
