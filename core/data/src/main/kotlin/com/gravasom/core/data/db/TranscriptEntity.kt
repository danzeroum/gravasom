package com.gravasom.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transcricao de uma gravacao. Uma por (gravacao, idioma): o usuario pode ter a mesma
 * gravacao transcrita em pt-BR e en-US sem perder a primeira. Some junto com a gravacao
 * (CASCADE). [text] e nulo enquanto nao termina; [errorMessage] e preenchido quando falha.
 *
 * Tabela nova da Onda 3 (ADR-016 secao 5). Escrita a mao; NAO toca em `recordings`.
 */
@Entity(
    tableName = "transcripts",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["recordingId", "language"], unique = true)],
)
data class TranscriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordingId: Long,
    val language: String,
    val format: String,
    val status: String,
    val text: String?,
    /** Frases com tempo, codificadas (ver TranscriptMapper). Nulo em transcricoes PLAIN ou antigas. */
    val segments: String? = null,
    val errorMessage: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
