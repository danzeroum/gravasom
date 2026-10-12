package com.gravasom.core.domain.transcript

/**
 * Idioma da transcricao, em BCP-47. pt-BR e o unico obrigatorio; os demais sao
 * opcionais e exigem o modelo baixado (ADR-016 secao 8).
 */
enum class TranscriptionLang(val tag: String, val displayName: String) {
    PT_BR("pt-BR", "Portugues (Brasil)"),
    EN_US("en-US", "Ingles (Estados Unidos)"),
    ES_ES("es-ES", "Espanhol (Espanha)"),
    FR_FR("fr-FR", "Frances (Franca)"),
    ;

    companion object {
        fun fromTag(tag: String): TranscriptionLang? = entries.firstOrNull { it.tag == tag }
    }
}

/** Formato de saida da transcricao. [PLAIN] nao tem tempo; [VTT] e [SRT] tem. */
enum class TranscriptFormat { PLAIN, VTT, SRT }

/** Estado da transcricao de uma gravacao. Um registro por (gravacao, idioma). */
enum class TranscriptStatus { PENDING, RUNNING, DONE, FAILED, CANCELLED }

/**
 * Um trecho transcrito. [startMillis] e [endMillis] em ms do ARQUIVO, na mesma
 * base do [com.gravasom.core.domain.marker.Marker].
 */
data class TranscriptSegment(
    val startMillis: Long,
    val endMillis: Long,
    val text: String,
    val confidence: Float = 1.0f,
)

/** Resultado de uma transcricao concluida. */
data class TranscriptResult(
    val language: TranscriptionLang,
    val text: String,
    val segments: List<TranscriptSegment>,
    val durationMillis: Long,
)

/**
 * Transcricao persistida de uma gravacao. [segments] nao e guardado no Room: e reconstruido
 * do [text] quando o formato pede tempo (VTT/SRT).
 */
data class Transcript(
    val id: Long = 0L,
    val recordingId: Long,
    val language: TranscriptionLang,
    val format: TranscriptFormat,
    val status: TranscriptStatus,
    val text: String? = null,
    val errorMessage: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

/** Motivos pelos quais uma transcricao pode falhar. Vira texto na UI em `feature:transcribe`. */
enum class TranscriptionError {
    MODEL_NOT_DOWNLOADED,
    MODEL_CORRUPTED,
    INSUFFICIENT_STORAGE,
    AUDIO_DECODE_FAILED,
    AUDIO_UNSUPPORTED,
    TIMEOUT,
    CANCELLED,
    RECORDING_NOT_FOUND,
    UNKNOWN,
}
