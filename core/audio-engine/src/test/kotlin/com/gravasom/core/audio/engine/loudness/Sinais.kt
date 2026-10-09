package com.gravasom.core.audio.engine.loudness

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Sinais de teste deterministicos (semente fixa). */
internal object Sinais {

    fun seno(taxa: Int, segundos: Double, hz: Double, dbfs: Double, fase: Double = 0.0): FloatArray {
        val a = 10.0.pow(dbfs / 20.0)
        return FloatArray((segundos * taxa).toInt()) { i -> (a * sin(2 * PI * hz * i / taxa + fase)).toFloat() }
    }

    /**
     * "Fala" sintetica: ruido rosa-ish modulado por silabas (4 Hz) com pausas de ~25%, fator de crista alto como o da voz.
     * Normalizado para o nivel RMS pedido (dBFS) antes de aplicar a modulacao: a loudness real e medida depois.
     */
    fun fala(taxa: Int, segundos: Double, rmsDbfs: Double, semente: Int = 7): FloatArray {
        val rnd = Random(semente)
        val n = (segundos * taxa).toInt()
        val x = FloatArray(n)
        var lp1 = 0.0
        var lp2 = 0.0
        for (i in 0 until n) {
            val branco = rnd.nextDouble() * 2 - 1
            lp1 += 0.05 * (branco - lp1)
            lp2 += 0.005 * (branco - lp2)
            val tom = sin(2 * PI * 140 * i / taxa) + 0.5 * sin(2 * PI * 280 * i / taxa) + 0.3 * sin(2 * PI * 1100 * i / taxa)
            val t = i.toDouble() / taxa
            val silaba = abs(sin(2 * PI * 4.0 * t)).pow(1.5)
            val pausa = if ((t % 3.0) > 2.25) 0.02 else 1.0
            x[i] = ((0.6 * tom + 3.0 * lp1 + 8.0 * lp2) * silaba * pausa).toFloat()
        }
        return escalarRms(x, rmsDbfs)
    }

    fun escalarRms(x: FloatArray, rmsDbfs: Double): FloatArray {
        var s = 0.0
        for (v in x) s += v.toDouble() * v
        val rms = kotlin.math.sqrt(s / x.size)
        val k = 10.0.pow(rmsDbfs / 20.0) / rms
        return FloatArray(x.size) { (x[it] * k).toFloat() }
    }

    fun ganho(x: FloatArray, db: Double): FloatArray {
        val k = 10.0.pow(db / 20.0).toFloat()
        return FloatArray(x.size) { x[it] * k }
    }

    fun picoAmostraDb(x: FloatArray): Double = 20 * log10(x.maxOf { abs(it) }.toDouble().coerceAtLeast(1e-12))

    fun concat(vararg partes: FloatArray): FloatArray = partes.reduce { a, b -> a + b }

    fun silencio(taxa: Int, segundos: Double) = FloatArray((segundos * taxa).toInt())
}
