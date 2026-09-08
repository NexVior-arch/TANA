package com.tana.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.tana.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"
    private const val REQUEST_CODE_REMINDER = 2001

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = "com.tana.ACTION_DAILY_FINANCE_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time is before current time, schedule for next day
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Daily reminder scheduled for: ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm, falling back to standard alarm", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Daily reminder cancelled")
    }

    fun scheduleNextAlarm(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val enabled = db.userPreferenceDao().getPreferenceDirect("reminder_enabled")?.toBoolean() ?: true
                if (enabled) {
                    val hour = db.userPreferenceDao().getPreferenceDirect("reminder_hour")?.toIntOrNull() ?: 20
                    val minute = db.userPreferenceDao().getPreferenceDirect("reminder_minute")?.toIntOrNull() ?: 0
                    scheduleDailyReminder(context, hour, minute)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule next alarm", e)
            }
        }
    }

    fun rescheduleFromPreferences(context: Context) {
        scheduleNextAlarm(context)
    }

    fun triggerTestNotification(context: Context, customMessage: String? = null) {
        DailyReminderReceiver.showDailyReminderNotification(context, customMessage)
    }
}
