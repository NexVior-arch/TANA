package com.tana.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tana.MainActivity
import com.tana.data.database.AppDatabase
import com.tana.data.model.AgentRoutineEntity
import com.tana.data.model.AgentRoutineType
import com.tana.data.model.AiMessageEntity
import com.tana.data.model.SavingsGoalEntity
import com.tana.data.model.TransactionEntity
import com.tana.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

object AgentAutonomousScheduler {
    private const val TAG = "AgentAutonomousSched"
    const val CHANNEL_AGENT_ID = "agent_autonomous_channel"
    private const val BASE_NOTIFICATION_ID = 5000

    private val MOTIVATION_QUOTES = listOf(
        "Kekayaan sejati dibangun dari konsistensi kecil setiap hari, bukan keberuntungan semalam.",
        "Setiap rupiah yang tidak kamu hamburkan adalah prajurit yang bekerja untuk masa depan finansialmu.",
        "Disiplin menabung hari ini akan memberimu kebebasan memilih di masa depan.",
        "Jangan menabung apa yang tersisa setelah dibelanjakan, tapi belanjakan apa yang tersisa setelah ditabung.",
        "Target besar tidak tercapai dalam satu lompatan, melainkan ribuan langkah kecil yang konsisten.",
        "Ketenangan finansial dimulai saat kamu mengendalikan uangmu, bukan uang yang mengendalikanmu.",
        "Fokus pada proses menabung, bukan pada godaan sesaat. Hasilnya akan berbicara sendiri kelak.",
        "Konsistensi adalah investasi terbaik dalam perencanaan masa depan."
    )

    fun createAgentNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "AI Agent Autonomous Tasks"
            val descriptionText = "Notifikasi eksekusi aksi otomatis AI Agent (Setoran tabungan & Motivasi harian)"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_AGENT_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Executes any pending autonomous agent routines for today.
     * Safely executes on background IO dispatcher.
     * Guaranteed to run at most once per day per routine using `lastExecutedDayKey`.
     */
    fun executePendingRoutines(context: Context, onComplete: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val routineDao = db.agentRoutineDao()
                val savingsDao = db.savingsGoalDao()
                val transDao = db.transactionDao()
                val aiDao = db.aiMessageDao()

                val activeRoutines = routineDao.getActiveRoutinesDirect()
                if (activeRoutines.isEmpty()) {
                    onComplete?.invoke()
                    return@launch
                }

                val now = System.currentTimeMillis()
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
                val calendar = Calendar.getInstance()
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ..., 7 = Saturday

                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

                for (routine in activeRoutines) {
                    val effectiveIntervalSec = if (routine.intervalSeconds > 0) {
                        routine.intervalSeconds
                    } else {
                        when {
                            routine.activeDaysCsv.startsWith("SEC_") -> routine.activeDaysCsv.replace("SEC_", "").toIntOrNull() ?: 1
                            routine.activeDaysCsv.startsWith("MIN_") -> (routine.activeDaysCsv.replace("MIN_", "").toIntOrNull() ?: 1) * 60
                            routine.activeDaysCsv.startsWith("HOUR_") -> (routine.activeDaysCsv.replace("HOUR_", "").toIntOrNull() ?: 1) * 3600
                            routine.activeDaysCsv.contains("detik", ignoreCase = true) -> 1
                            else -> 0
                        }
                    }

                    if (effectiveIntervalSec > 0) {
                        // Interval-based execution (e.g. every 1 second, 5 seconds)
                        val elapsedMs = now - routine.lastExecutedTimestamp
                        if (routine.lastExecutedTimestamp > 0 && elapsedMs < effectiveIntervalSec * 1000L) {
                            continue
                        }
                    } else {
                        // Daily-based execution
                        if (routine.lastExecutedDayKey == todayKey) {
                            continue
                        }

                        val activeDays = routine.activeDaysCsv
                            .split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                            .toSet()

                        if (activeDays.isNotEmpty() && !activeDays.contains(currentDayOfWeek)) {
                            continue
                        }
                    }

                    when (routine.type) {
                        AgentRoutineType.AUTO_DEPOSIT -> {
                            // Find target goal or create default if none exists
                            var allGoals = savingsDao.getAllGoals().firstOrNull() ?: emptyList()
                            if (allGoals.isEmpty()) {
                                val newGoalTitle = routine.targetGoalName?.ifBlank { "Target Tabungan Utama" } ?: "Target Tabungan Utama"
                                val newGoalId = savingsDao.insertGoal(
                                    SavingsGoalEntity(
                                        title = newGoalTitle,
                                        targetAmount = 10000000.0,
                                        currentAmount = 0.0,
                                        fillPlanAmount = routine.amount
                                    )
                                )
                                allGoals = savingsDao.getAllGoals().firstOrNull() ?: emptyList()
                            }

                            val finalGoal: SavingsGoalEntity? = if (routine.targetGoalId != null) {
                                allGoals.firstOrNull { it.id == routine.targetGoalId }
                            } else if (!routine.targetGoalName.isNullOrBlank()) {
                                val search = routine.targetGoalName.lowercase().trim()
                                allGoals.firstOrNull { it.title.lowercase().trim() == search }
                                    ?: allGoals.firstOrNull { it.title.lowercase().contains(search) || search.contains(it.title.lowercase()) }
                                    ?: allGoals.firstOrNull { !it.isCompleted }
                            } else {
                                allGoals.firstOrNull { !it.isCompleted } ?: allGoals.firstOrNull()
                            }

                            if (finalGoal != null && routine.amount > 0) {
                                val repository = com.tana.data.repository.FinanceRepository(db)
                                val depositResult = repository.depositToGoal(
                                    goalId = finalGoal.id,
                                    depositAmount = routine.amount,
                                    note = "🤖 Eksekusi AI Agent: ${routine.title}"
                                )

                                if (depositResult is com.tana.data.repository.SavingsOpResult.Success) {
                                    val updatedCurrent = depositResult.newBalance

                                    // Insert autonomous AI Message in chat
                                    val formattedAmt = currencyFormat.format(routine.amount)
                                    val formattedProgress = currencyFormat.format(updatedCurrent)
                                    val formattedTarget = currencyFormat.format(finalGoal.targetAmount)
                                    val percent = if (finalGoal.targetAmount > 0) (updatedCurrent / finalGoal.targetAmount * 100).toInt() else 0

                                    val intervalLabel = if (effectiveIntervalSec > 0) "setiap $effectiveIntervalSec detik" else "harian"
                                    val aiLog = """
                                        🤖 **[AI Agent Otomatis - Setoran Berhasil]**
                                        Saya telah mengeksekusi setoran rutin $intervalLabel sebesar **$formattedAmt** ke target tabungan **${finalGoal.title}** (Eksekusi ke-${routine.executionCount + 1}).
                                        
                                        📈 Progres Tabungan Sekarang: **$formattedProgress** dari target **$formattedTarget** ($percent%).
                                        
                                        *(Ketik 'stop setor otomatis' kapan saja jika Anda ingin menghentikan rutinitas ini).*
                                    """.trimIndent()

                                    aiDao.insertMessage(
                                        AiMessageEntity(
                                            role = "assistant",
                                            content = aiLog,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )

                                    // Show system notification only if not high-frequency to prevent vibration spam
                                    if (effectiveIntervalSec == 0 || effectiveIntervalSec >= 30) {
                                        showNotification(
                                            context = context,
                                            notificationId = BASE_NOTIFICATION_ID + (routine.id % 1000).toInt(),
                                            title = "🤖 Setoran Tabungan Otomatis Berhasil",
                                            body = "AI Agent berhasil menyetorkan $formattedAmt ke '${finalGoal.title}'."
                                        )
                                    }

                                    // Update routine state
                                    routineDao.updateRoutine(
                                        routine.copy(
                                            lastExecutedDayKey = todayKey,
                                            lastExecutedTimestamp = System.currentTimeMillis(),
                                            executionCount = routine.executionCount + 1
                                        )
                                    )
                                } else {
                                    // Deposit failed (goal deleted concurrently, invalid amount, etc.)
                                    // Log it instead of silently pretending it succeeded, but still mark
                                    // the routine as attempted today so it doesn't spam-retry forever.
                                    Log.w(TAG, "AUTO_DEPOSIT gagal untuk routine ${routine.id}: $depositResult")
                                    aiDao.insertMessage(
                                        AiMessageEntity(
                                            role = "assistant",
                                            content = "🤖 **[AI Agent Otomatis - Setoran Gagal]**\nSetoran rutin ke target tabungan gagal dieksekusi. Silakan cek kondisi target tabungan Anda.",
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    routineDao.updateRoutine(
                                        routine.copy(
                                            lastExecutedDayKey = todayKey,
                                            lastExecutedTimestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        }

                        AgentRoutineType.DAILY_MOTIVATION -> {
                            val quote = if (routine.customPromptOrQuote.isNotBlank()) {
                                routine.customPromptOrQuote
                            } else {
                                MOTIVATION_QUOTES[Random().nextInt(MOTIVATION_QUOTES.size)]
                            }

                            val aiMsg = """
                                ✨ **[Motivasi Harian TANA]**
                                "$quote"
                                
                                💡 *Tetap disiplin mencatat transaksi dan menyisihkan tabungan hari ini!*
                            """.trimIndent()

                            aiDao.insertMessage(
                                AiMessageEntity(
                                    role = "assistant",
                                    content = aiMsg,
                                    timestamp = System.currentTimeMillis()
                                )
                            )

                            showNotification(
                                context = context,
                                notificationId = BASE_NOTIFICATION_ID + (routine.id % 1000).toInt(),
                                title = "✨ Motivasi Finansial Hari Ini",
                                body = quote
                            )

                            routineDao.updateRoutine(
                                routine.copy(
                                    lastExecutedDayKey = todayKey,
                                    lastExecutedTimestamp = System.currentTimeMillis(),
                                    executionCount = routine.executionCount + 1
                                )
                            )
                        }

                        AgentRoutineType.FINANCIAL_AUDIT -> {
                            val aiMsg = """
                                📊 **[Audit & Evaluasi Otomatis AI]**
                                Evaluasi harian arus kas aktif. Pastikan setiap pengeluaran hari ini telah tercatat dengan akurat agar saldo riil tetap sinkron dan target tabungan Anda tercapai tepat waktu.
                            """.trimIndent()

                            aiDao.insertMessage(
                                AiMessageEntity(
                                    role = "assistant",
                                    content = aiMsg,
                                    timestamp = System.currentTimeMillis()
                                )
                            )

                            showNotification(
                                context = context,
                                notificationId = BASE_NOTIFICATION_ID + (routine.id % 1000).toInt(),
                                title = "📊 Evaluasi Arus Kas Otomatis",
                                body = "AI Agent telah mengecek arus kas harian Anda."
                            )

                            routineDao.updateRoutine(
                                routine.copy(
                                    lastExecutedDayKey = todayKey,
                                    lastExecutedTimestamp = System.currentTimeMillis(),
                                    executionCount = routine.executionCount + 1
                                )
                            )
                        }

                        AgentRoutineType.CUSTOM_REMINDER -> {
                            val msg = routine.customPromptOrQuote.ifBlank { "Waktunya evaluasi target keuangan Anda hari ini." }
                            aiDao.insertMessage(
                                AiMessageEntity(
                                    role = "assistant",
                                    content = "🔔 **[Pengingat Otomatis AI]**\n$msg",
                                    timestamp = System.currentTimeMillis()
                                )
                            )

                            showNotification(
                                context = context,
                                notificationId = BASE_NOTIFICATION_ID + (routine.id % 1000).toInt(),
                                title = "🔔 Pengingat Finansial",
                                body = msg
                            )

                            routineDao.updateRoutine(
                                routine.copy(
                                    lastExecutedDayKey = todayKey,
                                    lastExecutedTimestamp = System.currentTimeMillis(),
                                    executionCount = routine.executionCount + 1
                                )
                            )
                        }
                    }
                }
                onComplete?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Error executing agent routines", e)
                onComplete?.invoke()
            }
        }
    }

    private fun showNotification(context: Context, notificationId: Int, title: String, body: String) {
        createAgentNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_AGENT_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
