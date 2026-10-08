package com.gravasom.core.audio.engine.analysis

import java.io.ByteArrayOutputStream

/** Classifica fala/nao-fala em quadros de 20 ms de PCM mono 16 kHz. Uma instancia por modo de sensibilidade. */
interface SpeechClassifier : AutoCloseable {
    /** Consome [length] amostras e devolve 1 byte (0/1) por quadro completo formado. */
    fun classify(pcm16k: ShortArray, length: Int): ByteArray

    /** Fim do fluxo: classifica o quadro parcial, se houver. */
    fun finish(): ByteArray
}

/** Bits de fala empacotados (8 quadros por byte), para guardar horas de gravacao em poucos KB. */
object SpeechBits {
    fun pack(flags: BooleanArray): ByteArray {
        val out = ByteArray((flags.size + BITS - 1) / BITS)
        for (i in flags.indices) if (flags[i]) out[i / BITS] = (out[i / BITS].toInt() or (1 shl (i % BITS))).toByte()
        return out
    }

    fun unpack(packed: ByteArray, frames: Int): BooleanArray =
        BooleanArray(frames) { i -> i / BITS < packed.size && (packed[i / BITS].toInt() shr (i % BITS)) and 1 == 1 }

    private const val BITS = 8
}

/** Resultado da analise feita ao salvar: onda + fala por modo do VAD (do menos ao mais agressivo). */
class RecordingAnalysis(
    val waveform: Waveform,
    val frameMillis: Int,
    val frameCount: Int,
    val speechByMode: List<ByteArray>,
) {
    val durationMillis: Long get() = waveform.durationMillis

    fun speechFrames(mode: Int): BooleanArray = SpeechBits.unpack(speechByMode[mode], frameCount)
}

/**
 * Uma passada pelo PCM decodificado produz tudo: pico por 10 ms (taxa original) e fala por quadro de 20 ms
 * (mono 16 kHz) para cada classificador. Aceita pedacos de qualquer tamanho.
 */
class AnalysisPipeline(
    sampleRate: Int,
    private val channels: Int,
    private val classifiers: List<SpeechClassifier>,
    private val frameMillis: Int = FRAME_MILLIS,
) {
    private val waveform = WaveformBuilder(sampleRate, channels)
    private val resampler = Resampler16k(sampleRate)
    private val flags = classifiers.map { ByteArrayOutputStream() }
    private var mono = ShortArray(0)
    private var resampled = ShortArray(0)

    init {
        require(channels in 1..MAX_CHANNELS) { "canais invalidos" }
    }

    fun add(pcm: ShortArray, offset: Int, length: Int) {
        waveform.add(pcm, offset, length)
        val frames = length / channels
        if (frames == 0) return
        if (mono.size < frames) mono = ShortArray(frames)
        downmix(pcm, offset, frames)
        val cap = resampler.maxOutput(frames)
        if (resampled.size < cap) resampled = ShortArray(cap)
        val n = resampler.process(mono, 0, frames, resampled)
        classify(n)
    }

    fun finish(): RecordingAnalysis {
        val tail = ShortArray(1)
        if (resampler.finish(tail) == 1) {
            resampled = tail
            classify(1)
        }
        classifiers.forEachIndexed { i, c -> flags[i].write(c.finish()) }
        val frameCount = flags.firstOrNull()?.size() ?: 0
        val speech = flags.map { f -> SpeechBits.pack(f.toByteArray().let { b -> BooleanArray(b.size) { b[it].toInt() != 0 } }) }
        return RecordingAnalysis(waveform.finish(), frameMillis, frameCount, speech)
    }

    private fun classify(n: Int) {
        if (n == 0) return
        classifiers.forEachIndexed { i, c -> flags[i].write(c.classify(resampled, n)) }
    }

    private fun downmix(pcm: ShortArray, offset: Int, frames: Int) {
        if (channels == 1) {
            System.arraycopy(pcm, offset, mono, 0, frames)
            return
        }
        for (f in 0 until frames) {
            var sum = 0
            for (c in 0 until channels) sum += pcm[offset + f * channels + c]
            mono[f] = (sum / channels).toShort()
        }
    }

    companion object {
        const val FRAME_MILLIS = 20
        private const val MAX_CHANNELS = 8
    }
}
