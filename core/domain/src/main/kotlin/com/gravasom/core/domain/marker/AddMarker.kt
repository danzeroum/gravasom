package com.gravasom.core.domain.marker

/**
 * Cria um marcador validando nota (<= 140) e posicao (dentro da gravacao).
 * [durationOf] devolve a duracao da gravacao, ou null se ela nao existe.
 */
class AddMarker(
    private val repo: MarkerRepository,
    private val durationOf: suspend (recordingId: Long) -> Long?,
) {
    suspend operator fun invoke(
        recordingId: Long,
        positionMillis: Long,
        note: String,
        label: String = "",
        color: MarkerColor = MarkerColor.AMBER,
    ): MarkerResult {
        val duration = durationOf(recordingId) ?: return MarkerResult.Rejected(MarkerRejection.RECORDING_NOT_FOUND)
        if (!MarkerNote.isValid(note)) return MarkerResult.Rejected(MarkerRejection.NOTE_TOO_LONG)
        if (positionMillis !in 0..duration) return MarkerResult.Rejected(MarkerRejection.OUT_OF_RANGE)
        val id = repo.insert(
            Marker(
                recordingId = recordingId, positionMillis = positionMillis, label = label.trim(),
                note = MarkerNote.normalize(note), color = color,
            ),
        )
        return MarkerResult.Saved(id)
    }
}

/** Edita nota/rotulo/cor de um marcador existente com a mesma regra de nota. */
class UpdateMarker(private val repo: MarkerRepository) {
    suspend operator fun invoke(marker: Marker): MarkerResult {
        if (!MarkerNote.isValid(marker.note)) return MarkerResult.Rejected(MarkerRejection.NOTE_TOO_LONG)
        repo.update(marker.copy(note = MarkerNote.normalize(marker.note), label = marker.label.trim()))
        return MarkerResult.Saved(marker.id)
    }
}
