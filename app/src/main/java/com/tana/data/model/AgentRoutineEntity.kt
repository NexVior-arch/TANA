package com.tana.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AgentRoutineType {
    AUTO_DEPOSIT,       // Setor otomatis ke target tabungan
    DAILY_MOTIVATION,   // Motivasi finansial harian mandiri dari AI
    FINANCIAL_AUDIT,    // Evaluasi harian arus kas mandiri dari AI
    CUSTOM_REMINDER     // Pengingat harian/kustom
}

@Entity(tableName = "agent_routines")
data class AgentRoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: AgentRoutineType,
    val title: String,
    val description: String = "",
    val targetGoalId: Long? = null,
    val targetGoalName: String? = null,
    val amount: Double = 0.0,
    val activeDaysCsv: String = "2,3,4,5,6,7", // 1 = Sunday, 2 = Monday, ..., 7 = Saturday
    val executionHour: Int = 8,
    val executionMinute: Int = 0,
    val intervalSeconds: Int = 0, // 0 = daily schedule based on activeDaysCsv; >0 = execute every N seconds
    val customPromptOrQuote: String = "",
    val isEnabled: Boolean = true,
    val isApproved: Boolean = true, // User permission status
    val lastExecutedDayKey: String = "", // Format: "yyyy-MM-dd"
    val lastExecutedTimestamp: Long = 0L,
    val executionCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
