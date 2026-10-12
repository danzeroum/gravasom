package com.gravasom.core.domain.transcript

import java.io.File

/**
 * Contrato de dominio para transcricao de audio. Sem dependencia de Android: a
 * implementacao (whisper.cpp, Vosk) fica em `feature/transcribe` (ADR-016 secao 1).
 *
 * Este contrato e o ponto de troca de motor: se a medicao no aparelho reprovar o whisper,
 * um `VoskTranscriber` pode substituir `WhisperCppTranscriber` sem tocar na UI nem no worker.
 */
interface Transcriber {
    /**
     * Transcreve [arquivo] (PCM 16 kHz mono 16 bits, WAV ou PCM cru) no [lang].
     *
     * @param timestamps se `true`, cada segmento carrega [TranscriptSegment.startMillis]/[endMillis].
     * @param onSegment callback opcional chamado a cada segmento (permite o worker fazer flush
     *   em lotes sem esperar o fim). Nao pode bloquear: o worker enfileira.
     * @throws TranscriptionException com [TranscriptionError] classificado.
     */
    suspend fun transcribe(
        arquivo: File,
        lang: TranscriptionLang,
        timestamps: Boolean,
        onSegment: (TranscriptSegment) -> Unit = {},
    ): TranscriptResult
}

/** Erro de transcricao com motivo classificado. O worker traduz em [TranscriptStatus.FAILED]. */
class TranscriptionException(
    val reason: TranscriptionError,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message ?: reason.name, cause)

/**
 * Modelo de idioma sob demanda. O app NAO embute modelo (ADR-016 secao 2) e NAO usa INTERNET:
 * o download e feito pelo sistema (SAF/navegador), verificado por SHA-256 e guardado no sandbox.
 */
interface ModelManager {
    /** Estado atual do modelo do [lang]. */
    suspend fun state(lang: TranscriptionLang): ModelState

    /** Verifica se o modelo existe e bate o SHA-256 esperado. */
    suspend fun verify(lang: TranscriptionLang): Boolean

    /** Remove o modelo do [lang]. Idempotente. */
    suspend fun remove(lang: TranscriptionLang)
}

/** Estado de um modelo de idioma. */
sealed interface ModelState {
    /** Baixado, verificado e pronto para uso. */
    data class Ready(val bytes: Long) : ModelState

    /** Ainda nao baixado. */
    data object Missing : ModelState

    /** Arquivo presente, mas corrompido (SHA-256 nao bate). */
    data object Corrupted : ModelState

    /** Download em andamento, com [progress] de 0f a 1f. */
    data class Downloading(val progress: Float) : ModelState
}
