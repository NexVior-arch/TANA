package com.tana.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tana.data.model.AgentRoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentRoutineDao {
    @Query("SELECT * FROM agent_routines ORDER BY createdAt DESC")
    fun getAllRoutines(): Flow<List<AgentRoutineEntity>>

    @Query("SELECT * FROM agent_routines WHERE isEnabled = 1 AND isApproved = 1")
    fun getActiveRoutines(): Flow<List<AgentRoutineEntity>>

    @Query("SELECT * FROM agent_routines WHERE isEnabled = 1 AND isApproved = 1")
    suspend fun getActiveRoutinesDirect(): List<AgentRoutineEntity>

    @Query("SELECT * FROM agent_routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): AgentRoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: AgentRoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: AgentRoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: AgentRoutineEntity)

    @Query("DELETE FROM agent_routines WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE agent_routines SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE agent_routines SET isApproved = :approved, isEnabled = :approved WHERE id = :id")
    suspend fun setApproved(id: Long, approved: Boolean)

    @Query("UPDATE agent_routines SET isEnabled = 0 WHERE title LIKE '%' || :titleQuery || '%' OR targetGoalName LIKE '%' || :titleQuery || '%'")
    suspend fun disableRoutinesByQuery(titleQuery: String)

    @Query("DELETE FROM agent_routines")
    suspend fun clearAll()
}
