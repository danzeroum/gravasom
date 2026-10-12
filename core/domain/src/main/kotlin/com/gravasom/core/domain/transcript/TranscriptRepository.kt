package com.gravasom.core.domain.transcript

import kotlinx.coroutines.flow.Flow

/**
 * Acesso as transcricoes persistidas. Esconde o Room da UI e do worker.
 * Uma gravacao pode ter varias (uma por idioma).
 */
interface TranscriptRepository {

    /** Transcricoes da gravacao, mais recentes primeiro. */
    fun observeForRecording(recordingId: Long): Flow<List<Transcript>>

    /** Transcricao da gravacao no idioma, se existir. */
    suspend fun find(recordingId: Long, lang: TranscriptionLang): Transcript?

    /** Cria/atualiza o registro (idempotente por gravacao+idioma). Devolve o id. */
    suspend fun upsert(transcript: Transcript): Long

    /** Grava o texto final e marca como DONE. */
    suspend fun markDone(recordingId: Long, lang: TranscriptionLang, text: String)

    /** Marca como falha com o motivo; nao apaga o texto parcial (a UI decide). */
    suspend fun markFailed(recordingId: Long, lang: TranscriptionLang, reason: TranscriptionError)

    /** Marca como cancelada; limpa o texto parcial (nao serve para nada). */
    suspend fun markCancelled(recordingId: Long, lang: TranscriptionLang)

    /** Apaga a transcricao do idioma. Idempotente. */
    suspend fun delete(recordingId: Long, lang: TranscriptionLang)
}
