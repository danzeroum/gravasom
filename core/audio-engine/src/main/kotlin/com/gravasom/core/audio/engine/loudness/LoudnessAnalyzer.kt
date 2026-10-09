package com.gravasom.core.audio.engine.loudness

import com.gravasom.core.audio.engine.edit.PcmBlocos
import com.gravasom.core.audio.engine.edit.WavReader
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import kotlin.math.abs
import kotlin.math.log10

/** Resultado da medicao de um WAV. [lufs] e null para silencio (nenhum bloco acima do gate absoluto). */
data class LoudnessReport(val lufs: Double?, val truePeakDbtp: Double, val samplePeakDbfs: Double)

/** Mede um WAV PCM16 em streaming (blocos de 4096 quadros): memoria constante, qualquer duracao. */
object LoudnessAnalyzer {

    private const val BLOCK = 4096
    private const val BUFFER = 1 shl 16
    private const val MIN_PEAK = 1e-12

    fun measure(file: File): LoudnessReport {
        val info = WavReader.read(file)
        val meters = Meters(info.sampleRate, info.channels)
        val floats = Array(info.channels) { FloatArray(BLOCK) }
        val raw = ByteArray(BLOCK * info.channels * 2)
        val source = file.inputStream().also { it.skip(info.dataOffset) }
        DataInputStream(BufferedInputStream(source, BUFFER)).use { input ->
            var left = info.frames
            while (left > 0) {
                val n = minOf(BLOCK.toLong(), left).toInt()
                PcmBlocos.ler(input, raw, floats, n, available = left)
                meters.add(floats, n)
                left -= n
            }
        }
        return meters.report()
    }

    /** Acumula a loudness, o pico verdadeiro por canal e o pico de amostra, bloco a bloco. */
    private class Meters(sampleRate: Int, private val channels: Int) {
        private val loudness = LoudnessMeter(sampleRate, channels)
        private val peaks = List(channels) { TruePeakMeter(sampleRate) }
        private var samplePeak = 0f

        fun add(block: Array<FloatArray>, frames: Int) {
            for (c in 0 until channels) {
                loudness.add(c, block[c], frames)
                peaks[c].add(block[c], frames)
                samplePeak = maxOf(samplePeak, peakOf(block[c], frames))
            }
        }

        fun report(): LoudnessReport {
            val truePeak = peaks.maxOf { it.peakLinear() }
            return LoudnessReport(
                lufs = loudness.integratedLufs(),
                truePeakDbtp = 20.0 * log10(truePeak.coerceAtLeast(MIN_PEAK)),
                samplePeakDbfs = 20.0 * log10(samplePeak.toDouble().coerceAtLeast(MIN_PEAK)),
            )
        }

        private fun peakOf(channel: FloatArray, frames: Int): Float {
            var p = 0f
            for (i in 0 until frames) p = maxOf(p, abs(channel[i]))
            return p
        }
    }
}
