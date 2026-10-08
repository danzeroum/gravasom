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
        val loudness = LoudnessMeter(info.sampleRate, info.channels)
        val peaks = List(info.channels) { TruePeakMeter(info.sampleRate) }
        val floats = Array(info.channels) { FloatArray(BLOCK) }
        val raw = ByteArray(BLOCK * info.channels * 2)
        var samplePeak = 0f
        val source = file.inputStream().also { it.skip(info.dataOffset) }
        DataInputStream(BufferedInputStream(source, BUFFER)).use { input ->
            var left = info.frames
            while (left > 0) {
                val n = minOf(BLOCK.toLong(), left).toInt()
                PcmBlocos.ler(input, raw, floats, n, available = left)
                for (c in 0 until info.channels) {
                    loudness.add(c, floats[c], n)
                    peaks[c].add(floats[c], n)
                    for (i in 0 until n) samplePeak = maxOf(samplePeak, abs(floats[c][i]))
                }
                left -= n
            }
        }
        val truePeak = peaks.maxOf { it.peakLinear() }
        return LoudnessReport(
            lufs = loudness.integratedLufs(),
            truePeakDbtp = 20.0 * log10(truePeak.coerceAtLeast(MIN_PEAK)),
            samplePeakDbfs = 20.0 * log10(samplePeak.toDouble().coerceAtLeast(MIN_PEAK)),
        )
    }
}
