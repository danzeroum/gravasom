package com.gravasom.core.audio.engine.loudness

import com.gravasom.core.audio.engine.edit.LinkedBlockProcessor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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
 *
 * Com [channels] > 1 o ganho de limitacao e UM so para todos os canais: o `d` de cada amostra e o menor entre os canais (o pico que
 * mais precisa de reducao manda) e a mesma suavizacao e aplicada a todos. Assim a imagem estereo nao se desloca quando so um lado estoura.
 * Use [processLinked] com um bloco por canal; [process] serve so para um canal.
 */
class NormalizeProcessor(
    sampleRate: Int,
    gainDb: Double,
    ceilingDbtp: Double = -1.0,
    private val channels: Int = 1,
) : LinkedBlockProcessor {

    init {
        require(channels >= 1) { "channels deve ser >= 1" }
    }

    private val gain = Math.pow(10.0, gainDb / 20.0)
    private val ceiling = Math.pow(10.0, (ceilingDbtp - SAFETY_MARGIN_DB) / 20.0)
    private val look = max((sampleRate * LOOKAHEAD_SECONDS).roundToInt(), MIN_LOOKAHEAD)

    override val latencySamples: Int = TruePeakFilter.LOOKAHEAD + look

    private val ringSize = Integer.highestOneBit(latencySamples + TruePeakFilter.PER_PHASE) * 2
    private val mask = ringSize - 1
    private val gained = Array(channels) { DoubleArray(ringSize) }
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

    private val filters = Array(channels) { TruePeakFilter(TruePeakFilter.factorFor(sampleRate)) }
    private val windows = Array(channels) { DoubleArray(TruePeakFilter.PER_PHASE) }

    override fun process(block: FloatArray, length: Int) {
        require(channels == 1) { "process() serve so para 1 canal; use processLinked" }
        processLinked(arrayOf(block), length)
    }

    override fun processLinked(blocks: Array<FloatArray>, length: Int) {
        require(blocks.size == channels) { "esperava $channels canais, recebeu ${blocks.size}" }
        val out = FloatArray(channels)
        for (i in 0 until length) {
            for (c in 0 until channels) out[c] = 0f
            step(blocks, i, out)
            for (c in 0 until channels) blocks[c][i] = out[c]
        }
    }

    /** Uma amostra de cada canal (indice [i] dos [blocks]); escreve a saida (ja atrasada) em [result]. */
    private fun step(blocks: Array<FloatArray>, i: Int, result: FloatArray) {
        val slot = (n and mask.toLong()).toInt()
        for (c in 0 until channels) gained[c][slot] = blocks[c][i].toDouble() * gain
        val m = n - TruePeakFilter.LOOKAHEAD // amostra cujo pico ja pode ser calculado
        var d = 1.0
        if (m >= 0) for (c in 0 until channels) d = min(d, desired(c, m))
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
        if (out >= 0) {
            val outSlot = (out and mask.toLong()).toInt()
            for (c in 0 until channels) result[c] = (gained[c][outSlot] * smooth).toFloat()
        }
    }

    /** Ganho desejado (<= 1) para o pico verdadeiro entre a amostra [m] e a seguinte caber no teto. */
    private fun desired(c: Int, m: Long): Double {
        var near = 0.0
        for (k in -NEAR_SPAN..NEAR_SPAN) near = max(near, abs(at(c, m + k)))
        if (near * TruePeakFilter.SAFE_RATIO <= ceiling) return 1.0
        val window = windows[c]
        for (k in 0 until TruePeakFilter.PER_PHASE) window[k] = at(c, m - TruePeakFilter.LOOKAHEAD + k)
        val peak = max(max(abs(window[TruePeakFilter.LOOKAHEAD]), abs(window[TruePeakFilter.LOOKAHEAD + 1])), filters[c].peakBetween(window))
        return if (peak <= ceiling) 1.0 else ceiling / peak
    }

    private fun at(c: Int, index: Long): Double = if (index < 0 || index >= n + 1) 0.0 else gained[c][(index and mask.toLong()).toInt()]

    private companion object {
        const val LOOKAHEAD_SECONDS = 0.003

        /** O ganho varia durante o ataque; a interpolacao enxerga amostras com ganhos um pouco diferentes: folga para isso. */
        const val SAFETY_MARGIN_DB = 0.3
        const val MIN_LOOKAHEAD = 8
        /** Vizinhanca (amostras de cada lado) olhada antes de decidir se vale interpolar. */
        const val NEAR_SPAN = 4
    }
}
