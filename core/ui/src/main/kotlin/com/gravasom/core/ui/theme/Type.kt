// GERADO por docs/design/ferramentas/gerar_kotlin.py a partir de docs/design/tokens.json. NAO EDITE A MAO:
// mude o tokens.json (ou o script) e rode o gerador de novo.
// tokens.json sha256: bfb38b9f739d953e266185aa0d8d5a5065e2aa079e18b8bf8b2f4c5394dc01b2
@file:Suppress("ktlint", "MaxLineLength")

package com.gravasom.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gravasom.core.ui.R

/** Atkinson Hyperlegible Next (SIL Open Font License 1.1), empacotada em res/font: o app não usa a internet nem o provedor de fontes. */
val GravasomFontFamily = FontFamily(
    Font(R.font.atkinson_next_regular, FontWeight(400)),
    Font(R.font.atkinson_next_semibold, FontWeight(600)),
)

/** Pesos que existem de verdade no APK. Pedir outro faria o Android sintetizar (negrito falso). */
val GravasomPesosEmpacotados: Set<Int> = setOf(400, 600)

val GravasomTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(400),
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(400),
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(400),
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(400),
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(400),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(400),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * Estilos fora da escala do Material. `faixaEstado` pede texto em MAIÚSCULAS (textTransform = uppercase no token): o
 * Compose não tem essa propriedade, então quem usa chama `.uppercase()` no texto.
 */
object GravasomTextStyles {
    val cronometro = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(400),
        fontSize = 64.sp,
        lineHeight = 72.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )

    val cronometroCompacto = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )

    val tempo = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = "tnum",
    )

    val faixaEstado = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 1.sp,
    )

    val numeroMarcador = TextStyle(
        fontFamily = GravasomFontFamily,
        fontWeight = FontWeight(600),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )

    val todos: List<TextStyle> get() = listOf(cronometro, cronometroCompacto, tempo, faixaEstado, numeroMarcador)
}
