package com.gravasom.core.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** Razão de contraste WCAG 2.x entre duas cores opacas. */
internal object ContrasteWcag {
    private const val LIMIAR = 0.03928
    private const val LINEAR = 12.92
    private const val DESLOCAMENTO = 0.055
    private const val ESCALA = 1.055
    private const val GAMA = 2.4
    private const val PISO = 0.05

    private fun canal(v: Float): Double {
        val x = v.toDouble()
        return if (x <= LIMIAR) x / LINEAR else ((x + DESLOCAMENTO) / ESCALA).pow(GAMA)
    }

    fun luminancia(c: Color): Double = 0.2126 * canal(c.red) + 0.7152 * canal(c.green) + 0.0722 * canal(c.blue)

    fun razao(a: Color, b: Color): Double {
        val la = luminancia(a)
        val lb = luminancia(b)
        return (maxOf(la, lb) + PISO) / (minOf(la, lb) + PISO)
    }
}
