package com.gravasom.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {

    @Query("SELECT * FROM transcripts WHERE recordingId = :recordingId ORDER BY updatedAtMillis DESC")
    fun observeForRecording(recordingId: Long): Flow<List<TranscriptEntity>>

    @Query("SELECT * FROM transcripts WHERE recordingId = :recordingId AND language = :language LIMIT 1")
    suspend fun find(recordingId: Long, language: String): TranscriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transcript: TranscriptEntity): Long

    @Query("UPDATE transcripts SET text = :text, status = :status, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateText(id: Long, text: String?, status: String, updatedAtMillis: Long)

    @Query(
        "UPDATE transcripts SET status = :status, errorMessage = :errorMessage, updatedAtMillis = :updatedAtMillis " +
            "WHERE recordingId = :recordingId AND language = :language",
    )
    suspend fun updateStatus(recordingId: Long, language: String, status: String, errorMessage: String?, updatedAtMillis: Long)

    @Query("DELETE FROM transcripts WHERE recordingId = :recordingId AND language = :language")
    suspend fun delete(recordingId: Long, language: String)

    @Query("DELETE FROM transcripts WHERE recordingId = :recordingId")
    suspend fun deleteForRecording(recordingId: Long)
}
