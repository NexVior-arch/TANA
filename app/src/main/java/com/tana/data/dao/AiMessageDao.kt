package com.tana.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tana.data.model.AiMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {
    @Query("SELECT * FROM ai_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<AiMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity): Long

    @Query("UPDATE ai_messages SET content = :content WHERE id = :id")
    suspend fun updateMessageContent(id: Long, content: String)

    @Query("DELETE FROM ai_messages")
    suspend fun clearHistory()
}
