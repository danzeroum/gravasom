package com.gravasom.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EditProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: EditProjectEntity): Long

    @Query("SELECT * FROM edit_projects WHERE recordingId = :recordingId")
    suspend fun getByRecording(recordingId: Long): EditProjectEntity?

    @Query("SELECT * FROM edit_projects WHERE recordingId = :recordingId")
    fun observeByRecording(recordingId: Long): Flow<EditProjectEntity?>

    @Query("DELETE FROM edit_projects WHERE recordingId = :recordingId")
    suspend fun deleteByRecording(recordingId: Long)
}
