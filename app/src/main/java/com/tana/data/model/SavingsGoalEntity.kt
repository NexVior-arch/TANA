package com.tana.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDateMillis: Long = 0L,
    val note: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val fillPlanFrequency: String = "DAILY", // "DAILY", "WEEKLY", "MONTHLY"
    val fillPlanAmount: Double = 0.0, // e.g. 10000.0
    val activeDays: String = "1,2,3,4,5,6,7", // 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 12,
    val reminderMinute: Int = 0,
    val imageUri: String? = null,
    val currency: String = "IDR"
)
