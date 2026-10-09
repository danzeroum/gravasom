package com.gravasom.core.audio.engine.loudness

import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * Loudness integrada (ITU-R BS.1770-4): filtro K, blocos de 400 ms com 75% de sobreposicao (passo de 100 ms), gate absoluto
 * em -70 LUFS e gate relativo 10 LU abaixo da media. Canais somam energia com peso 1 (mono e estereo).
 *
 * Alimente cada canal com [add]; os blocos de um canal podem ter qualquer tamanho. Memoria: um `double` por 100 ms.
 */
class LoudnessMeter(private val sampleRate: Int, private val channels: Int = 1) {

    private val hop = (sampleRate * HOP_SECONDS).roundToInt().coerceAtLeast(1)
    private val filters = List(channels) {
        val (shelf, rlb) = KWeighting.stages(sampleRate)
        Biquad(shelf) to Biquad(rlb)
    }
    private val energies = List(channels) { ArrayList<Double>() }
    private val partial = DoubleArray(channels)
    private val partialCount = IntArray(channels)

    /** Acrescenta [length] amostras do [channel]. */
    fun add(channel: Int, block: FloatArray, length: Int) {
        val (shelf, rlb) = filters[channel]
        var sum = partial[channel]
        var count = partialCount[channel]
        for (i in 0 until length) {
            val y = rlb.process(shelf.process(block[i].toDouble()))
            sum += y * y
            if (++count == hop) {
                energies[channel].add(sum / hop)
                sum = 0.0
                count = 0
            }
        }
        partial[channel] = sum
        partialCount[channel] = count
    }

    /** LUFS integrada, ou null se nenhum bloco passa do gate absoluto (silencio). */
    fun integratedLufs(): Double? {
        val hops = energies.minOf { it.size }
        if (hops < BLOCK_HOPS) return null
        val power = DoubleArray(hops) { h -> energies.sumOf { it[h] } }
        val blocks = DoubleArray(hops - BLOCK_HOPS + 1) { j -> (0 until BLOCK_HOPS).sumOf { power[j + it] } / BLOCK_HOPS }
        val absolute = blocks.filter { lufs(it) > ABSOLUTE_GATE }
        if (absolute.isEmpty()) return null
        val relativeGate = lufs(absolute.average()) - RELATIVE_GATE_LU
        val kept = blocks.filter { lufs(it) > ABSOLUTE_GATE && lufs(it) > relativeGate }
        return lufs(kept.average())
    }

    private fun lufs(power: Double): Double = OFFSET + 10.0 * log10(power.coerceAtLeast(MIN_POWER))

    private companion object {
        const val HOP_SECONDS = 0.1
        const val BLOCK_HOPS = 4
        const val ABSOLUTE_GATE = -70.0
        const val RELATIVE_GATE_LU = 10.0
        const val OFFSET = -0.691
        const val MIN_POWER = 1e-20
    }
}
