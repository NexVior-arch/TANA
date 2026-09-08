package com.tana.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    SAVINGS
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val goalId: Long? = null
)
