package com.gravasom.core.data.db

import com.gravasom.core.domain.transcript.Transcript
import com.gravasom.core.domain.transcript.TranscriptFormat
import com.gravasom.core.domain.transcript.TranscriptSegment
import com.gravasom.core.domain.transcript.TranscriptStatus
import com.gravasom.core.domain.transcript.TranscriptionLang
import org.json.JSONArray
import org.json.JSONObject

/** Mapeamento entre a entidade Room e o modelo de dominio (espelha o `RecordingMapper`). */
fun TranscriptEntity.toDomain(): Transcript = Transcript(
    id = id,
    recordingId = recordingId,
    language = TranscriptionLang.fromTag(language) ?: TranscriptionLang.PT_BR,
    format = TranscriptFormat.entries.firstOrNull { it.name == format } ?: TranscriptFormat.PLAIN,
    status = TranscriptStatus.entries.firstOrNull { it.name == status } ?: TranscriptStatus.PENDING,
    text = text,
    segments = decodeSegments(segments),
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
    segments = encodeSegments(segments),
    errorMessage = errorMessage,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

/**
 * Frases como array JSON: `[{"s":0,"e":1500,"t":"Bom dia"}]`. Texto pode conter qualquer
 * caractere (aspas, quebra de linha); o JSON cuida do escape. Nulo/vazio = lista vazia.
 */
/** Codifica frases para o formato guardado (JSON). Exposto para o worker reaproveitar. */
fun encodeSegments(segments: List<TranscriptSegment>): String? {
    if (segments.isEmpty()) return null
    val array = JSONArray()
    segments.forEach { s ->
        array.put(JSONObject().put("s", s.startMillis).put("e", s.endMillis).put("t", s.text))
    }
    return array.toString()
}

private fun decodeSegments(json: String?): List<TranscriptSegment> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            TranscriptSegment(
                startMillis = o.optLong("s"),
                endMillis = o.optLong("e"),
                text = o.optString("t"),
            )
        }
    }.getOrDefault(emptyList())
}
