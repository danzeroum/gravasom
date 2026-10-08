package com.gravasom.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkerDao {

    @Query("SELECT * FROM markers WHERE recordingId = :recordingId ORDER BY positionMillis, id")
    fun observe(recordingId: Long): Flow<List<MarkerEntity>>

    @Query("SELECT * FROM markers WHERE recordingId = :recordingId ORDER BY positionMillis, id")
    suspend fun list(recordingId: Long): List<MarkerEntity>

    @Insert
    suspend fun insert(marker: MarkerEntity): Long

    @Insert
    suspend fun insertAll(markers: List<MarkerEntity>)

    @Update
    suspend fun update(marker: MarkerEntity)

    @Query("DELETE FROM markers WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AnalysisDao {

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsert(analysis: RecordingAnalysisEntity)

    @Query("SELECT * FROM recording_analysis WHERE recordingId = :recordingId")
    suspend fun get(recordingId: Long): RecordingAnalysisEntity?

    @Query("SELECT recordingId FROM recording_analysis WHERE recordingId = :recordingId")
    suspend fun exists(recordingId: Long): Long?

    @Query("DELETE FROM recording_analysis WHERE recordingId = :recordingId")
    suspend fun delete(recordingId: Long)
}
