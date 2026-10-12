package com.gravasom.feature.transcribe.model

import com.gravasom.core.domain.transcript.TranscriptResult
import com.gravasom.core.domain.transcript.TranscriptSegment
import com.gravasom.core.domain.transcript.Transcriber
import com.gravasom.core.domain.transcript.TranscriptionError
import com.gravasom.core.domain.transcript.TranscriptionException
import com.gravasom.core.domain.transcript.TranscriptionLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Implementacao do [Transcriber] sobre whisper.cpp (ADR-013/016). Le PCM 16 bits mono 16 kHz
 * do arquivo gerado pelo AudioConverter e devolve os segmentos com tempos.
 *
 * Chunking: arquivos longos sao processados em janelas de 30 s com 1 s de sobreposicao
 * (ADR-016 secao 3). O motor ve cada janela como um audio proprio; os segmentos sao costurados
 * e o offset da janela e somado ao tempo.
 */
@Singleton
class WhisperCppTranscriber @Inject constructor(
    private val models: AndroidModelManager,
) : Transcriber {

    override suspend fun transcribe(
        arquivo: File,
        lang: TranscriptionLang,
        timestamps: Boolean,
        onSegment: suspend (TranscriptSegment) -> Unit,
    ): TranscriptResult = withContext(Dispatchers.IO) {
        val modelo = models.caminho(lang)
        if (!modelo.exists()) throw TranscriptionException(TranscriptionError.MODEL_NOT_DOWNLOADED)
        if (!models.verify(lang)) throw TranscriptionException(TranscriptionError.MODEL_CORRUPTED)

        val handle = WhisperBridge.nativeInit(modelo.absolutePath)
        if (handle == 0L) throw TranscriptionException(TranscriptionError.MODEL_CORRUPTED, "whisper nao abriu o modelo")

        try {
            val amostras = lerPcm16k(arquivo)
            val duracaoMs = amostras.size.toLong() * 1000 / 16_000
            val segmentos = ArrayList<TranscriptSegment>()
            var offset = 0
            while (offset < amostras.size) {
                coroutineContext.ensureActive()
                val fim = minOf(offset + JANELA_AMOSTRAS, amostras.size)
                val janela = amostras.copyOfRange(offset, fim)
                val janelaMs = offset.toLong() * 1000 / 16_000
                val texto = WhisperBridge.nativeTranscribe(handle, janela, 0, lang.tag)
                    ?: throw TranscriptionException(TranscriptionError.UNKNOWN, "whisper_full falhou")
                texto.lineSequence().filter { it.isNotBlank() }.forEach { linha ->
                    val partes = linha.split('\t', limit = 3)
                    if (partes.size < 3) return@forEach
                    val seg = TranscriptSegment(
                        startMillis = partes[0].toLong() + janelaMs,
                        endMillis = partes[1].toLong() + janelaMs,
                        text = partes[2].trim(),
                    )
                    segmentos += seg
                    onSegment(seg)
                }
                offset = fim - SOBREPOSICAO_AMOSTRAS
                if (offset <= 0 || fim >= amostras.size) break
            }
            val texto = segmentos.joinToString("\n") { it.text }
            TranscriptResult(lang, texto, segmentos, duracaoMs)
        } catch (t: Throwable) {
            Timber.w(t, "transcricao falhou (lang=%s)", lang.tag)
            throw t
        } finally {
            WhisperBridge.nativeFree(handle)
        }
    }

    /** Le o PCM cru 16 bits LE de [arquivo] como FloatArray em [-1, 1]. */
    private fun lerPcm16k(arquivo: File): FloatArray {
        val bytes = arquivo.readBytes()
        val shorts = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return FloatArray(shorts.remaining()) { shorts.get(it) / 32768f }
    }

    companion object {
        private const val JANELA_AMOSTRAS = 30 * 16_000
        private const val SOBREPOSICAO_AMOSTRAS = 1 * 16_000
    }
}
