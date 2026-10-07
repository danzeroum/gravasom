package com.gravasom.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Projeto de edicao nao destrutiva de uma gravacao (1 por gravacao). Os passos ficam em texto versionado (EditCodec). */
@Entity(
    tableName = "edit_projects",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["recordingId"], unique = true)],
)
data class EditProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordingId: Long,
    val schemaVersion: Int,
    val stepsEncoded: String,
    val updatedAtMillis: Long,
)
