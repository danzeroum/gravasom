package com.gravasom.core.audio.engine.loudness

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interpolador por [factor] (FIR de 12*factor+1 taps, janela de Kaiser) para estimar o pico VERDADEIRO (entre amostras), como
 * pede a ITU-R BS.1770-4 (anexo 2: 4x a 48 kHz). Simetrico em torno do tap central: a fase 0 reproduz as amostras originais
 * exatamente, entao o pico verdadeiro nunca fica abaixo do pico de amostra. Atraso de grupo: [LOOKAHEAD] amostras originais.
 *
 * O fator cresce quando a taxa cai ([factorFor]): a 16 kHz a fala chega perto de Nyquist e 4x subestima o pico.
 * Janela de Kaiser com beta 5 e +-12 amostras: casa (ou passa um pouco, o que e seguro) da sobre-amostragem do scipy usada pelo
 * harness; beta 8 com +-6 amostras subestimava ate 0,16 dB (medido em fala real a 16 kHz).
 */
internal class TruePeakFilter(val factor: Int) {
    private val taps = LOOKAHEAD * 2 * factor + 1

    /** `coef[fase][k]` multiplica x[t - k]; fases 0..factor-1 = posicoes 0, 1/factor, ... da amostra. */
    private val coef: Array<DoubleArray> = Array(factor) { p -> DoubleArray(PER_PHASE) { k -> tap(factor * k + p) } }

    private fun tap(j: Int): Double {
        if (j !in 0 until taps) return 0.0
        val mid = (taps - 1) / 2.0
        val x = (j - mid) / factor
        val sinc = if (x == 0.0) 1.0 else sin(PI * x) / (PI * x)
        val r = (j - mid) / mid
        val window = bessel(KAISER_BETA * sqrt(1.0 - r * r)) / bessel(KAISER_BETA)
        return sinc * window
    }

    /** Maior |valor| das fases entre [m] e [m]+1, dada a janela `w[m-12 .. m+12]` (indices 0..24 de [w], em ordem temporal). */
    fun peakBetween(w: DoubleArray): Double {
        var peak = 0.0
        for (p in 0 until factor) {
            var acc = 0.0
            val c = coef[p]
            for (k in 0 until PER_PHASE) acc += c[k] * w[PER_PHASE - 1 - k]
            peak = max(peak, abs(acc))
        }
        return peak
    }

    companion object {
        const val LOOKAHEAD = 12
        const val PER_PHASE = LOOKAHEAD * 2 + 1 // 25 amostras de entrada por saida

        /** Entre amostras o pico passa do maior vizinho por no maximo ~2x (soma dos modulos dos taps); 2,5 com folga. */
        const val SAFE_RATIO = 2.5
        private const val KAISER_BETA = 5.0
        private const val BESSEL_TERMS = 30
        private const val HIGH_RATE = 44_100
        private const val MID_RATE = 22_050

        /** 4x a partir de 44,1 kHz (como a norma), 8x entre 22,05 e 44,1 kHz, 16x abaixo disso. */
        fun factorFor(sampleRate: Int): Int = when {
            sampleRate >= HIGH_RATE -> 4
            sampleRate >= MID_RATE -> 8
            else -> 16
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
    }
}

/** Pico verdadeiro (dBTP) de um fluxo de amostras de um canal. Blocos de qualquer tamanho. */
class TruePeakMeter(sampleRate: Int) {
    private val filter = TruePeakFilter(TruePeakFilter.factorFor(sampleRate))
    private val history = DoubleArray(TruePeakFilter.PER_PHASE)
    private var peak = 0.0

    fun add(block: FloatArray, length: Int) {
        for (i in 0 until length) push(block[i].toDouble())
    }

    private fun push(v: Double) {
        System.arraycopy(history, 1, history, 0, history.size - 1)
        history[history.size - 1] = v
        // So interpola quando a vizinhanca pode superar o pico atual: em fala, quase nenhuma amostra (so os picos).
        var near = 0.0
        for (h in history) near = max(near, abs(h))
        if (near * TruePeakFilter.SAFE_RATIO > peak) peak = max(peak, filter.peakBetween(history))
    }

    /** Pico linear (inclui a cauda: as ultimas amostras ainda influenciam o que vem depois delas). */
    fun peakLinear(): Double {
        var p = peak
        val saved = history.copyOf()
        repeat(TruePeakFilter.LOOKAHEAD) {
            System.arraycopy(history, 1, history, 0, history.size - 1)
            history[history.size - 1] = 0.0
            p = max(p, filter.peakBetween(history))
        }
        saved.copyInto(history)
        return p
    }

    fun dbtp(): Double = 20.0 * log10(peakLinear().coerceAtLeast(MIN_PEAK))

    private companion object {
        const val MIN_PEAK = 1e-12
    }
}
