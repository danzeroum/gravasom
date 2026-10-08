package com.gravasom.core.service.analysis

import com.gravasom.core.audio.engine.analysis.AnalysisPipeline
import com.gravasom.core.audio.engine.analysis.RecordingAnalysis
import com.gravasom.core.audio.engine.analysis.SilenceSensitivity
import com.gravasom.core.audio.engine.analysis.SpeechClassifier
import com.gravasom.core.data.db.AnalysisStore
import com.gravasom.core.dsp.WebRtcVad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Calcula (uma vez, ao salvar) a onda e a fala de uma gravacao e guarda no banco; o player so le.
 * Gravacoes do cofre NAO persistem a analise (o desenho da atividade de fala seria um vazamento): ficam so em memoria.
 */
class RecordingAnalyzer(
    private val store: AnalysisStore,
    private val classifiers: () -> List<SpeechClassifier> = { SilenceSensitivity.entries.map { WebRtcVad(it.vadMode) } },
) {

    private val lock = Mutex()
    private var memory: Pair<Long, RecordingAnalysis>? = null

    /** Analise do banco; se nao ha, calcula de [audio] (arquivo legivel) e, salvo no cofre, persiste. */
    suspend fun ensure(recordingId: Long, audio: File, persist: Boolean): RecordingAnalysis? = lock.withLock {
        memory?.takeIf { it.first == recordingId }?.second?.let { return it }
        store.load(recordingId)?.let { return it }
        val computed = withContext(Dispatchers.Default) { analyze(audio) } ?: return null
        if (persist) store.save(recordingId, computed) else memory = recordingId to computed
        computed
    }

    /** Decodifica e analisa numa passada. null se o arquivo nao decodifica. */
    fun analyze(audio: File, cancelled: () -> Boolean = { false }): RecordingAnalysis? {
        val models = classifiers()
        try {
            var pipeline: AnalysisPipeline? = null
            var format: Pair<Int, Int>? = null
            var broken = false
            val ok = PcmDecoder.decode(
                audio,
                { rate, channels ->
                    if (format == null) {
                        format = rate to channels
                        pipeline = AnalysisPipeline(rate, channels, models)
                    } else if (format != rate to channels) {
                        broken = true // formato mudou no meio: nao ha como misturar taxas na mesma onda
                    }
                },
                { pcm, length -> if (!broken) pipeline?.add(pcm, 0, length) },
                { broken || cancelled() },
            )
            return if (ok && !broken) pipeline?.finish() else null
        } finally {
            models.forEach { it.close() }
        }
    }
}
