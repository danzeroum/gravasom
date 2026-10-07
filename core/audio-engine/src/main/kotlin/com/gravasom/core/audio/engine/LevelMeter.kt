package com.gravasom.core.audio.engine

import kotlin.math.log10
import kotlin.math.sqrt

/** Calcula o nivel RMS em dB a partir de amostras PCM 16 bits. Puro. */
object LevelMeter {

    const val MIN_DB = -60f

    fun rmsDb(samples: ShortArray): Float {
        if (samples.isEmpty()) return MIN_DB
        var sum = 0.0
        for (s in samples) {
            val v = s.toDouble() / Short.MAX_VALUE
            sum += v * v
        }
        val rms = sqrt(sum / samples.size)
        if (rms <= 0.0) return MIN_DB
        val db = 20.0 * log10(rms)
        return db.coerceIn(MIN_DB.toDouble(), 0.0).toFloat()
    }
}
