package com.gravasom.core.data.repository

import com.gravasom.core.data.db.TranscriptDao
import com.gravasom.core.data.db.toDomain
import com.gravasom.core.data.db.toEntity
import com.gravasom.core.domain.transcript.Transcript
import com.gravasom.core.domain.transcript.TranscriptRepository
import com.gravasom.core.domain.transcript.TranscriptStatus
import com.gravasom.core.domain.transcript.TranscriptionError
import com.gravasom.core.domain.transcript.TranscriptionLang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementacao Room do [TranscriptRepository]. Espelha o `RoomMarkerRepository`. */
class RoomTranscriptRepository(private val dao: TranscriptDao) : TranscriptRepository {

    override fun observeForRecording(recordingId: Long): Flow<List<Transcript>> =
        dao.observeForRecording(recordingId).map { linhas -> linhas.map { it.toDomain() } }

    override suspend fun find(recordingId: Long, lang: TranscriptionLang): Transcript? =
        dao.find(recordingId, lang.tag)?.toDomain()

    override suspend fun upsert(transcript: Transcript): Long = dao.upsert(transcript.toEntity())

    override suspend fun markDone(recordingId: Long, lang: TranscriptionLang, text: String) {
        val atual = dao.find(recordingId, lang.tag) ?: return
        dao.updateText(atual.id, text, TranscriptStatus.DONE.name, System.currentTimeMillis())
    }

    override suspend fun markFailed(recordingId: Long, lang: TranscriptionLang, reason: TranscriptionError) {
        dao.updateStatus(recordingId, lang.tag, TranscriptStatus.FAILED.name, reason.name, System.currentTimeMillis())
    }

    override suspend fun markCancelled(recordingId: Long, lang: TranscriptionLang) {
        dao.updateStatus(recordingId, lang.tag, TranscriptStatus.CANCELLED.name, null, System.currentTimeMillis())
    }

    override suspend fun delete(recordingId: Long, lang: TranscriptionLang) = dao.delete(recordingId, lang.tag)
}
