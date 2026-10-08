package com.gravasom.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageEventDao {
    @Insert
    suspend fun insert(event: UsageEventEntity): Long

    @Query("SELECT COUNT(*) FROM usage_events WHERE type = :type")
    fun observeCount(type: String): Flow<Int>

    @Query("SELECT * FROM usage_events ORDER BY timestampMillis ASC")
    suspend fun all(): List<UsageEventEntity>

    @Query("SELECT value FROM usage_events WHERE type = :type AND value IS NOT NULL ORDER BY value ASC")
    suspend fun values(type: String): List<Long>

    @Query("DELETE FROM usage_events")
    suspend fun clear()
}
