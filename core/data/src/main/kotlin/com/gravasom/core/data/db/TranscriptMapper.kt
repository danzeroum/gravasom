package com.gravasom.core.data.db

import com.gravasom.core.domain.transcript.Transcript
import com.gravasom.core.domain.transcript.TranscriptFormat
import com.gravasom.core.domain.transcript.TranscriptStatus
import com.gravasom.core.domain.transcript.TranscriptionLang

/** Mapeamento entre a entidade Room e o modelo de dominio (espelha o `RecordingMapper`). */
fun TranscriptEntity.toDomain(): Transcript = Transcript(
    id = id,
    recordingId = recordingId,
    language = TranscriptionLang.fromTag(language) ?: TranscriptionLang.PT_BR,
    format = TranscriptFormat.entries.firstOrNull { it.name == format } ?: TranscriptFormat.PLAIN,
    status = TranscriptStatus.entries.firstOrNull { it.name == status } ?: TranscriptStatus.PENDING,
    text = text,
    errorMessage = errorMessage,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

fun Transcript.toEntity(): TranscriptEntity = TranscriptEntity(
    id = id,
    recordingId = recordingId,
    language = language.tag,
    format = format.name,
    status = status.name,
    text = text,
    errorMessage = errorMessage,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)
