package com.gravasom.core.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** Razão de contraste WCAG 2.x entre duas cores opacas (fórmula da norma, sem usar o Compose para não herdar um erro dele). */
internal object Contraste {
    fun luminancia(c: Color): Double {
        fun canal(v: Float): Double {
            val x = v.toDouble()
            return if (x <= 0.03928) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * canal(c.red) + 0.7152 * canal(c.green) + 0.0722 * canal(c.blue)
    }

    fun razao(a: Color, b: Color): Double {
        val la = luminancia(a)
        val lb = luminancia(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}
