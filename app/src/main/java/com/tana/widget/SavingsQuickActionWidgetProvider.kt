package com.tana.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class SavingsQuickActionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdateHelper.updateAllWidgets(context)
    }
}
