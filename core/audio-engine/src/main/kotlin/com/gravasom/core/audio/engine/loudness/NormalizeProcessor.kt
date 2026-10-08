package com.gravasom.core.audio.engine.loudness

import com.gravasom.core.audio.engine.edit.AudioBlockProcessor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Quanto subir (ou descer) para chegar ao alvo em LUFS. */
object NormalizePlan {
    /** Teto do ganho: nao adianta amplificar 40 dB de ruido de fundo de uma gravacao quase muda. */
    const val MAX_GAIN_DB = 30.0

    /** Ganho em dB; 0 se a gravacao e silenciosa (sem loudness). */
    fun gainDb(measuredLufs: Double?, targetLufs: Double, maxGainDb: Double = MAX_GAIN_DB): Double =
        if (measuredLufs == null) 0.0 else (targetLufs - measuredLufs).coerceAtMost(maxGainDb)
}

/**
 * "Melhorar volume": aplica [gainDb] e segura o pico VERDADEIRO em [ceilingDbtp] com um limitador de look-ahead.
 *
 * Por amostra: o ganho desejado `d` e `teto / pico` (pico = maior valor entre as amostras e o interpolado 4x), minimo
 * sobre uma janela de [lookaheadSamples] a frente, depois suavizado por media movel de mesma largura; assim o ganho
 * ja desceu ate o pico chegar e sobe devagar depois (sem estalo). A media de minimos nunca passa de `d` da propria amostra.
 * Quando nada precisa ser limitado o ganho e exatamente [gainDb]. Atraso: [latencySamples] (compensado pelo exportador).
 */
class NormalizeProcessor(sampleRate: Int, gainDb: Double, ceilingDbtp: Double = -1.0) : AudioBlockProcessor {

    private val gain = Math.pow(10.0, gainDb / 20.0)
    private val ceiling = Math.pow(10.0, (ceilingDbtp - SAFETY_MARGIN_DB) / 20.0)
    private val look = max((sampleRate * LOOKAHEAD_SECONDS).roundToInt(), MIN_LOOKAHEAD)

    override val latencySamples: Int = TruePeakFilter.LOOKAHEAD + look

    private val ringSize = Integer.highestOneBit(latencySamples + TruePeakFilter.PER_PHASE) * 2
    private val mask = ringSize - 1
    private val gained = DoubleArray(ringSize)
    private var n = 0L // indice da proxima amostra de entrada

    // minimo deslizante de d[m-look..m] por fila monotonica (indices em dIndex, valores em dValue)
    private val dQueueIndex = LongArray(ringSize)
    private val dQueueValue = DoubleArray(ringSize)
    private var qHead = 0
    private var qTail = 0

    // media movel de M (look + 1 valores)
    private val mRing = DoubleArray(look + 1) { 1.0 }
    private var mPos = 0
    private var mSum = (look + 1).toDouble()

    private val filter = TruePeakFilter(TruePeakFilter.factorFor(sampleRate))
    private val window = DoubleArray(TruePeakFilter.PER_PHASE)

    override fun process(block: FloatArray, length: Int) {
        for (i in 0 until length) block[i] = step(block[i].toDouble()).toFloat()
    }

    private fun step(x: Double): Double {
        val v = x * gain
        gained[(n and mask.toLong()).toInt()] = v
        val m = n - TruePeakFilter.LOOKAHEAD // amostra cujo pico ja pode ser calculado
        val d = if (m >= 0) desired(m) else 1.0
        // fila monotonica de minimos: janela [m - look, m]
        while (qTail != qHead && dQueueValue[(qTail - 1) and mask] >= d) qTail--
        dQueueIndex[qTail and mask] = m
        dQueueValue[qTail and mask] = d
        qTail++
        while (dQueueIndex[qHead and mask] < m - look) qHead++
        val minimum = dQueueValue[qHead and mask]
        mSum += minimum - mRing[mPos]
        mRing[mPos] = minimum
        mPos = (mPos + 1) % mRing.size
        val smooth = mSum / mRing.size
        val out = n - latencySamples
        n++
        return if (out >= 0) gained[(out and mask.toLong()).toInt()] * smooth else 0.0
    }

    /** Ganho desejado (<= 1) para o pico verdadeiro entre a amostra [m] e a seguinte caber no teto. */
    private fun desired(m: Long): Double {
        var near = 0.0
        for (k in -NEAR_SPAN..NEAR_SPAN) near = max(near, abs(at(m + k)))
        if (near * TruePeakFilter.SAFE_RATIO <= ceiling) return 1.0
        for (k in 0 until TruePeakFilter.PER_PHASE) window[k] = at(m - TruePeakFilter.LOOKAHEAD + k)
        val peak = max(max(abs(window[TruePeakFilter.LOOKAHEAD]), abs(window[TruePeakFilter.LOOKAHEAD + 1])), filter.peakBetween(window))
        return if (peak <= ceiling) 1.0 else ceiling / peak
    }

    private fun at(index: Long): Double = if (index < 0 || index >= n + 1) 0.0 else gained[(index and mask.toLong()).toInt()]

    private companion object {
        const val LOOKAHEAD_SECONDS = 0.003

        /** O ganho varia durante o ataque; a interpolacao enxerga amostras com ganhos um pouco diferentes: folga para isso. */
        const val SAFETY_MARGIN_DB = 0.3
        const val MIN_LOOKAHEAD = 8
        /** Vizinhanca (amostras de cada lado) olhada antes de decidir se vale interpolar. */
        const val NEAR_SPAN = 4
    }
}
