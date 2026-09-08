package com.tana.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tana.MainActivity
import com.tana.R
import java.util.Random

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule after device reboot and check pending agent actions
            ReminderScheduler.rescheduleFromPreferences(context)
            AgentAutonomousScheduler.executePendingRoutines(context)
            return
        }

        showDailyReminderNotification(context)

        // Execute autonomous AI agent tasks (auto-deposits, daily motivations)
        AgentAutonomousScheduler.executePendingRoutines(context)

        // Reschedule for next day if repeating
        ReminderScheduler.scheduleNextAlarm(context)
    }

    companion object {
        const val CHANNEL_ID = "daily_financial_discipline_channel"
        const val NOTIFICATION_ID = 1001

        private val DISCIPLINE_QUOTES = listOf(
            "Waktunya mencatat transaksi hari ini. Disiplin harian adalah kunci kebebasan finansial.",
            "Sudahkah menyisihkan tabungan hari ini? Pantau progres target impianmu sekarang.",
            "Evaluasi pengeluaran hari ini dan pastikan arus kas tetap terkendali dengan bijak.",
            "Disiplin finansial bukan tentang menahan diri, tapi tentang memprioritaskan masa depan.",
            "Satu catatan transaksi per hari menjauhkan dari kebocoran anggaran yang tak terduga.",
            "Setiap rupiah yang ditabung hari ini adalah fondasi keamanan finansial Anda di masa depan."
        )

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Pengingat Disiplin Finansial"
                val descriptionText = "Notifikasi pengingat harian untuk mencatat transaksi dan menyisihkan tabungan"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                    setShowBadge(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun showDailyReminderNotification(context: Context, customMessage: String? = null) {
            createNotificationChannel(context)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val quote = customMessage?.takeIf { it.isNotBlank() }
                ?: DISCIPLINE_QUOTES[Random().nextInt(DISCIPLINE_QUOTES.size)]

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Evaluasi & Disiplin Finansial Harian")
                .setContentText(quote)
                .setStyle(NotificationCompat.BigTextStyle().bigText(quote))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }
}
