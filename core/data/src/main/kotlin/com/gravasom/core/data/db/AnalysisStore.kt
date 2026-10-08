package com.gravasom.core.data.db

import com.gravasom.core.audio.engine.analysis.RecordingAnalysis
import com.gravasom.core.audio.engine.analysis.SilenceSensitivity
import com.gravasom.core.audio.engine.analysis.Waveform

/** Guarda e le a analise (onda + fala) de uma gravacao. A versao do formato permite recalcular se ele mudar. */
class AnalysisStore(private val dao: AnalysisDao) {

    suspend fun save(recordingId: Long, analysis: RecordingAnalysis) {
        require(analysis.speechByMode.size == MODES) { "esperava $MODES modos de VAD" }
        dao.upsert(
            RecordingAnalysisEntity(
                recordingId = recordingId,
                version = VERSION,
                peaks = analysis.waveform.peaks,
                frameMillis = analysis.frameMillis,
                frameCount = analysis.frameCount,
                speech = analysis.speechByMode.fold(ByteArray(0)) { acc, b -> acc + b },
            ),
        )
    }

    /** null se nao ha analise ou se ela e de uma versao antiga (precisa recalcular). */
    suspend fun load(recordingId: Long): RecordingAnalysis? {
        val e = dao.get(recordingId)?.takeIf { it.version == VERSION } ?: return null
        val bytesPerMode = (e.frameCount + BITS - 1) / BITS
        if (e.speech.size != bytesPerMode * MODES) return null
        val modes = List(MODES) { i -> e.speech.copyOfRange(i * bytesPerMode, (i + 1) * bytesPerMode) }
        return RecordingAnalysis(Waveform(e.peaks), e.frameMillis, e.frameCount, modes)
    }

    suspend fun has(recordingId: Long): Boolean = dao.get(recordingId)?.version == VERSION

    companion object {
        const val VERSION = 1
        private const val BITS = 8
        val MODES = SilenceSensitivity.entries.size
    }
}
