package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC, id ASC")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getAllEnabled(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AlarmEntity): Long

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "UPDATE alarms SET isEnabled = :enabled, nextTriggerMillis = :nextTriggerMillis, " +
            "remainingSnoozes = :remainingSnoozes WHERE id = :id"
    )
    suspend fun updateScheduleState(
        id: Long,
        enabled: Boolean,
        nextTriggerMillis: Long,
        remainingSnoozes: Int
    )
}
