// GERADO por docs/design/ferramentas/gerar_kotlin.py a partir de docs/design/tokens.json. NAO EDITE A MAO:
// mude o tokens.json (ou o script) e rode o gerador de novo.
// tokens.json sha256: bfb38b9f739d953e266185aa0d8d5a5065e2aa079e18b8bf8b2f4c5394dc01b2
@file:Suppress("ktlint", "MaxLineLength")

package com.gravasom.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

object GravasomDuration {
    const val short1: Int = 50
    const val short2: Int = 100
    const val short3: Int = 150
    const val short4: Int = 200
    const val medium1: Int = 250
    const val medium2: Int = 300
    const val medium3: Int = 350
    const val medium4: Int = 400
    const val long1: Int = 450
    const val long2: Int = 500
    const val long3: Int = 550
    const val long4: Int = 600
    const val extraLong1: Int = 700
    const val extraLong2: Int = 800
    const val extraLong3: Int = 900
    const val extraLong4: Int = 1000
}

object GravasomEasing {
    val linear: Easing = CubicBezierEasing(0.0f, 0.0f, 1.0f, 1.0f)
    val standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val standardDecelerate: Easing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val standardAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)
    val emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
}

/** Duração e curva de uma animação nomeada do tokens.json. */
data class MotionSpec(val durationMillis: Int, val easing: Easing)

/** Animações nomeadas. Com "Remover animações" ligado (escala 0) a tela não usa pulso nem piscar: o estado segue dito por texto e ícone. */
object GravasomMotion {
    val morfarBotao = MotionSpec(300, GravasomEasing.emphasizedDecelerate)
    val faixaEstadoEntra = MotionSpec(250, GravasomEasing.emphasizedDecelerate)
    val faixaEstadoSai = MotionSpec(200, GravasomEasing.emphasizedAccelerate)
    val pulsoGravando = MotionSpec(1600, GravasomEasing.standard)
    val piscarPausado = MotionSpec(1000, GravasomEasing.linear)
    val vuSubida = MotionSpec(50, GravasomEasing.linear)
    val vuQueda = MotionSpec(300, GravasomEasing.standardAccelerate)
    val vuPicoRetido = MotionSpec(1000, GravasomEasing.linear)
    val confirmarMarcador = MotionSpec(400, GravasomEasing.emphasizedDecelerate)
    val snackbar = MotionSpec(4000, GravasomEasing.linear)

    // Parâmetros numéricos de algumas animações
    const val pulsoGravandoEscalaDe: Float = 1.0f
    const val pulsoGravandoEscalaAte: Float = 1.6f
    const val pulsoGravandoAlfaDe: Float = 0.6f
    const val pulsoGravandoAlfaAte: Float = 0.0f
    const val piscarPausadoAlfaDe: Float = 1.0f
    const val piscarPausadoAlfaAte: Float = 0.45f
}
