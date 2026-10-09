package com.gravasom.core.audio.engine.loudness

import kotlin.math.PI
import kotlin.math.tan

/** Coeficientes normalizados (a0 = 1) de um biquad. */
class BiquadCoefficients(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double)

/** Biquad em forma direta II transposta; estado em double (um por canal). */
class Biquad(private val c: BiquadCoefficients) {
    private var z1 = 0.0
    private var z2 = 0.0

    fun process(x: Double): Double {
        val y = c.b0 * x + z1
        z1 = c.b1 * x - c.a1 * y + z2
        z2 = c.b2 * x - c.a2 * y
        return y
    }
}

/**
 * Filtro K da ITU-R BS.1770-4: estagio 1 (prefiltro, prateleira de agudos de +4 dB) seguido do estagio 2 (RLB, passa-altas de
 * 38 Hz). Projeto por transformada bilinear dos parametros da norma (mesma formulacao da libebur128): a 48 kHz reproduz as
 * tabelas 1 e 2 da norma (ver o teste) e vale para qualquer outra taxa.
 */
object KWeighting {

    private const val SHELF_GAIN_DB = 3.999843853973347
    private const val SHELF_Q = 0.7071752369554196
    private const val SHELF_HZ = 1681.974450955533
    private const val SHELF_BAND_EXPONENT = 0.4996667741545416
    private const val HIGH_PASS_Q = 0.5003270373238773
    private const val HIGH_PASS_HZ = 38.13547087602444

    fun stages(sampleRate: Int): Pair<BiquadCoefficients, BiquadCoefficients> =
        highShelf(sampleRate) to highPass(sampleRate)

    private fun highShelf(fs: Int): BiquadCoefficients {
        val k = tan(PI * SHELF_HZ / fs)
        val vh = Math.pow(10.0, SHELF_GAIN_DB / 20.0)
        val vb = Math.pow(vh, SHELF_BAND_EXPONENT)
        val a0 = 1.0 + k / SHELF_Q + k * k
        return BiquadCoefficients(
            b0 = (vh + vb * k / SHELF_Q + k * k) / a0,
            b1 = 2.0 * (k * k - vh) / a0,
            b2 = (vh - vb * k / SHELF_Q + k * k) / a0,
            a1 = 2.0 * (k * k - 1.0) / a0,
            a2 = (1.0 - k / SHELF_Q + k * k) / a0,
        )
    }

    private fun highPass(fs: Int): BiquadCoefficients {
        val k = tan(PI * HIGH_PASS_HZ / fs)
        val a0 = 1.0 + k / HIGH_PASS_Q + k * k
        return BiquadCoefficients(
            b0 = 1.0, b1 = -2.0, b2 = 1.0,
            a1 = 2.0 * (k * k - 1.0) / a0,
            a2 = (1.0 - k / HIGH_PASS_Q + k * k) / a0,
        )
    }
}
