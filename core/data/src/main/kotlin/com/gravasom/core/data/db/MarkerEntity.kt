package com.gravasom.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Marcador de uma gravacao. Some junto com ela (CASCADE). A nota tem no maximo 140 caracteres (regra no dominio). */
@Entity(
    tableName = "markers",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["recordingId", "positionMillis"])],
)
data class MarkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordingId: Long,
    val positionMillis: Long,
    val label: String,
    val note: String,
    val color: String,
)

/**
 * Analise feita ao salvar (waveform + fala por quadro). 1 por gravacao.
 * [peaks]: 1 byte por 10 ms. [speech]: bits de fala, [frameCount] quadros de [frameMillis] ms por modo do VAD,
 * os modos em ordem (SilenceSensitivity.entries), cada um com (frameCount + 7) / 8 bytes.
 */
@Entity(
    tableName = "recording_analysis",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    primaryKeys = ["recordingId"],
)
data class RecordingAnalysisEntity(
    val recordingId: Long,
    val version: Int,
    val peaks: ByteArray,
    val frameMillis: Int,
    val frameCount: Int,
    val speech: ByteArray,
) {
    override fun equals(other: Any?): Boolean = other is RecordingAnalysisEntity && recordingId == other.recordingId &&
        version == other.version && frameMillis == other.frameMillis && frameCount == other.frameCount &&
        peaks.contentEquals(other.peaks) && speech.contentEquals(other.speech)

    override fun hashCode(): Int = recordingId.hashCode() * 31 + version
}
