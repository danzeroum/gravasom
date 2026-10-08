package com.gravasom.core.audio.engine.loudness

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interpolador 4x (FIR de 49 taps, janela de Kaiser) para estimar o pico VERDADEIRO (entre amostras), como pede a
 * ITU-R BS.1770-4 (anexo 2). Simetrico em torno do tap 24: a fase 0 reproduz as amostras originais exatamente, entao o pico
 * verdadeiro nunca fica abaixo do pico de amostra. Atraso de grupo: [LOOKAHEAD] amostras originais.
 */
internal object TruePeakFilter {
    const val FACTOR = 4
    const val TAPS = 49
    const val LOOKAHEAD = (TAPS - 1) / (2 * FACTOR) // 6
    const val PER_PHASE = LOOKAHEAD * 2 + 1 // 13 amostras de entrada por saida (a fase 0 so usa uma de fato)
    private const val KAISER_BETA = 8.0
    private const val BESSEL_TERMS = 30

    /** `coef[fase][k]` multiplica x[t - k]; fases 0..3 = posicoes 0, 1/4, 2/4, 3/4 de amostra. */
    val coef: Array<DoubleArray> = Array(FACTOR) { p -> DoubleArray(PER_PHASE) { k -> tap(FACTOR * k + p) } }

    private fun tap(j: Int): Double {
        if (j !in 0 until TAPS) return 0.0
        val mid = (TAPS - 1) / 2.0
        val x = (j - mid) / FACTOR
        val sinc = if (x == 0.0) 1.0 else sin(PI * x) / (PI * x)
        val r = (j - mid) / mid
        val window = bessel(KAISER_BETA * sqrt(1.0 - r * r)) / bessel(KAISER_BETA)
        return sinc * window
    }

    private fun bessel(x: Double): Double {
        var sum = 1.0
        var term = 1.0
        for (k in 1..BESSEL_TERMS) {
            term *= (x / (2.0 * k)) * (x / (2.0 * k))
            sum += term
        }
        return sum
    }

    /** Maior |valor| das 4 fases entre [m] e [m]+1, dada a janela `w[m-6 .. m+6]` (indices 0..12 de [w], em ordem temporal). */
    fun peakBetween(w: DoubleArray): Double {
        var peak = 0.0
        for (p in 0 until FACTOR) {
            var acc = 0.0
            val c = coef[p]
            for (k in 0 until PER_PHASE) acc += c[k] * w[PER_PHASE - 1 - k]
            peak = max(peak, abs(acc))
        }
        return peak
    }
}

/** Pico verdadeiro (dBTP) de um fluxo de amostras de um canal. Blocos de qualquer tamanho. */
class TruePeakMeter(@Suppress("unused") private val sampleRate: Int) {
    private val history = DoubleArray(TruePeakFilter.PER_PHASE)
    private var peak = 0.0

    fun add(block: FloatArray, length: Int) {
        for (i in 0 until length) push(block[i].toDouble())
    }

    private fun push(v: Double) {
        System.arraycopy(history, 1, history, 0, history.size - 1)
        history[history.size - 1] = v
        peak = max(peak, TruePeakFilter.peakBetween(history))
    }

    /** Pico linear (inclui a cauda: as ultimas amostras ainda influenciam o que vem depois delas). */
    fun peakLinear(): Double {
        var p = peak
        val saved = history.copyOf()
        repeat(TruePeakFilter.LOOKAHEAD) {
            System.arraycopy(history, 1, history, 0, history.size - 1)
            history[history.size - 1] = 0.0
            p = max(p, TruePeakFilter.peakBetween(history))
        }
        saved.copyInto(history)
        return p
    }

    fun dbtp(): Double = 20.0 * log10(peakLinear().coerceAtLeast(MIN_PEAK))

    private companion object {
        const val MIN_PEAK = 1e-12
    }
}
