package com.gravasom.core.audio.engine.analysis

/**
 * Reamostra PCM mono de qualquer taxa para 16 kHz (a taxa do VAD) por media de area: cada amostra de saida e a media
 * da entrada na sua janela. Aritmetica inteira exata (razao reduzida): mesmo resultado em qualquer divisao em pedacos.
 * A media de area atenua o que passa de 8 kHz o suficiente para o VAD (que olha so a faixa da voz); nao e um conversor
 * de qualidade de estudio e nao e usado para nada que o usuario escute.
 */
class Resampler16k(inRate: Int) {

    private val divisor = gcd(inRate, TARGET_RATE)
    private val num = (inRate / divisor).toLong() // unidades de entrada por amostra de saida
    private val den = (TARGET_RATE / divisor).toLong() // unidades por amostra de entrada
    private val passthrough = inRate == TARGET_RATE
    private var acc = 0L
    private var filled = 0L

    init {
        require(inRate > 0) { "taxa invalida" }
    }

    /** Capacidade segura do destino para [inputLength] amostras de entrada. */
    fun maxOutput(inputLength: Int): Int = (inputLength * den / num + 2).toInt()

    /** Consome [length] amostras de [input] a partir de [offset]; escreve em [out] e devolve quantas escreveu. */
    fun process(input: ShortArray, offset: Int, length: Int, out: ShortArray): Int {
        if (passthrough) {
            System.arraycopy(input, offset, out, 0, length)
            return length
        }
        var produced = 0
        for (i in offset until offset + length) {
            val v = input[i].toLong()
            var remaining = den
            while (remaining > 0) {
                val take = minOf(remaining, num - filled)
                acc += v * take
                filled += take
                remaining -= take
                if (filled == num) {
                    out[produced++] = roundDiv(acc, num)
                    acc = 0
                    filled = 0
                }
            }
        }
        return produced
    }

    /** Fim do fluxo: emite a media do bin parcial (no maximo 1 amostra). */
    fun finish(out: ShortArray): Int {
        if (passthrough || filled == 0L) return 0
        out[0] = roundDiv(acc, filled)
        acc = 0
        filled = 0
        return 1
    }

    private fun roundDiv(sum: Long, count: Long): Short {
        val q = if (sum >= 0) (sum + count / 2) / count else -((-sum + count / 2) / count)
        return q.coerceIn(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong()).toShort()
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    companion object {
        const val TARGET_RATE = 16_000
    }
}
