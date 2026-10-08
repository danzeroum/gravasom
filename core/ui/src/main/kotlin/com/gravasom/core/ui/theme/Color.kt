// GERADO por docs/design/ferramentas/gerar_kotlin.py a partir de docs/design/tokens.json. NAO EDITE A MAO:
// mude o tokens.json (ou o script) e rode o gerador de novo.
// tokens.json sha256: bfb38b9f739d953e266185aa0d8d5a5065e2aa079e18b8bf8b2f4c5394dc01b2
@file:Suppress("ktlint", "MaxLineLength")

package com.gravasom.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

const val TOKENS_SHA256 = "bfb38b9f739d953e266185aa0d8d5a5065e2aa079e18b8bf8b2f4c5394dc01b2"

/** Quatro papéis de um estado (gravando, pausado, pronto, cofre): cor sólida, texto sobre ela, faixa e texto sobre a faixa. */
@Immutable
data class StateColors(val base: Color, val onBase: Color, val container: Color, val onContainer: Color)

/** Medidor de nível: trilho apagado e as três zonas (clipando é laranja, nunca vermelho). */
@Immutable
data class VuColors(val trilho: Color, val baixo: Color, val ok: Color, val clipando: Color)

/** Forma de onda do player. */
@Immutable
data class WaveColors(
    val fundo: Color,
    val barra: Color,
    val tocada: Color,
    val cursor: Color,
    val silencio: Color,
    val selecao: Color,
    val alca: Color,
)

/** Uma cor de marcador: linha e bandeira (fill), número sobre a bandeira (onFill) e chip da lista. */
@Immutable
data class MarkerSwatch(val fill: Color, val onFill: Color, val container: Color, val onContainer: Color)

/** As seis cores de marcador, na ordem fixa de `color.marker.ordem`. */
@Immutable
data class MarkerSwatches(
    val ambar: MarkerSwatch,
    val verde: MarkerSwatch,
    val azul: MarkerSwatch,
    val roxo: MarkerSwatch,
    val rosa: MarkerSwatch,
    val grafite: MarkerSwatch,
) {
    val ordem: List<MarkerSwatch> get() = listOf(ambar, verde, azul, roxo, rosa, grafite)

    /** Cor do marcador pelo nome do enum do domínio (`MarkerColor.name`); nome desconhecido cai em âmbar. */
    fun forDomainName(nome: String): MarkerSwatch = when (nome) {
        "AMBER" -> ambar
        "GREEN" -> verde
        "BLUE" -> azul
        "PURPLE" -> roxo
        "RED" -> rosa
        "GRAPHITE" -> grafite
        else -> ambar
    }
}

/**
 * Cores que não pertencem ao esquema do Material: estados da gravação, medidor, onda e marcadores. Nunca mudam com a
 * cor dinâmica; só pronto, cofre e onda mudam entre as paletas Gravasom e Mixlirous.
 */
@Immutable
data class GravasomExtraColors(
    val gravando: StateColors,
    val pausado: StateColors,
    val pronto: StateColors,
    val cofre: StateColors,
    val vu: VuColors,
    val onda: WaveColors,
    val marker: MarkerSwatches,
)


// ---- Paleta Gravasom (padrão): color.scheme + color.semantic

val GravasomLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF006877),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA5EEFF),
    onPrimaryContainer = Color(0xFF004E5A),
    inversePrimary = Color(0xFF76D4E8),
    secondary = Color(0xFF4E6267),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E6EC),
    onSecondaryContainer = Color(0xFF374A4F),
    tertiary = Color(0xFF785177),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD6FB),
    onTertiaryContainer = Color(0xFF5E3A5E),
    background = Color(0xFFF6FAFB),
    onBackground = Color(0xFF181C1D),
    surface = Color(0xFFF6FAFB),
    onSurface = Color(0xFF181C1D),
    surfaceVariant = Color(0xFFD9E4E8),
    onSurfaceVariant = Color(0xFF3E494B),
    surfaceTint = Color(0xFF006877),
    inverseSurface = Color(0xFF2D3132),
    inverseOnSurface = Color(0xFFEEF1F3),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF6E797C),
    outlineVariant = Color(0xFFBDC8CB),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF6FAFB),
    surfaceDim = Color(0xFFD7DBDC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F5),
    surfaceContainer = Color(0xFFEBEEF0),
    surfaceContainerHigh = Color(0xFFE5E9EA),
    surfaceContainerHighest = Color(0xFFDFE3E4),
)

val GravasomDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF76D4E8),
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004E5A),
    onPrimaryContainer = Color(0xFFA5EEFF),
    inversePrimary = Color(0xFF006877),
    secondary = Color(0xFFB5CAD0),
    onSecondary = Color(0xFF203338),
    secondaryContainer = Color(0xFF374A4F),
    onSecondaryContainer = Color(0xFFD1E6EC),
    tertiary = Color(0xFFE7B8E4),
    onTertiary = Color(0xFF462447),
    tertiaryContainer = Color(0xFF5E3A5E),
    onTertiaryContainer = Color(0xFFFFD6FB),
    background = Color(0xFF101415),
    onBackground = Color(0xFFDFE3E4),
    surface = Color(0xFF101415),
    onSurface = Color(0xFFDFE3E4),
    surfaceVariant = Color(0xFF3E494B),
    onSurfaceVariant = Color(0xFFBDC8CB),
    surfaceTint = Color(0xFF76D4E8),
    inverseSurface = Color(0xFFDFE3E4),
    inverseOnSurface = Color(0xFF2D3132),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF889396),
    outlineVariant = Color(0xFF3E494B),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF353A3B),
    surfaceDim = Color(0xFF101415),
    surfaceContainerLowest = Color(0xFF0A0F10),
    surfaceContainerLow = Color(0xFF181C1D),
    surfaceContainer = Color(0xFF1C2022),
    surfaceContainerHigh = Color(0xFF262B2C),
    surfaceContainerHighest = Color(0xFF313537),
)

val GravasomExtraLight: GravasomExtraColors = GravasomExtraColors(
    gravando = StateColors(
        base = Color(0xFFBA1A1A), onBase = Color(0xFFFFFFFF), container = Color(0xFFBA1A1A), onContainer = Color(0xFFFFFFFF)
    ),
    pausado = StateColors(
        base = Color(0xFF926000), onBase = Color(0xFFFFFFFF), container = Color(0xFFFFCC85), onContainer = Color(0xFF291800)
    ),
    pronto = StateColors(
        base = Color(0xFF006877), onBase = Color(0xFFFFFFFF), container = Color(0xFFA5EEFF), onContainer = Color(0xFF004E5A)
    ),
    cofre = StateColors(
        base = Color(0xFF785177), onBase = Color(0xFFFFFFFF), container = Color(0xFFFFD6FB), onContainer = Color(0xFF5E3A5E)
    ),
    vu = VuColors(trilho = Color(0xFFDFE3E4), baixo = Color(0xFF6F797B), ok = Color(0xFF007A54), clipando = Color(0xFFBF5A00)),
    onda = WaveColors(
        fundo = Color(0xFFF0F4F5), barra = Color(0xFF6E797C), tocada = Color(0xFF006877), cursor = Color(0xFF181C1D), silencio = Color(0xFFBDC8CB), selecao = Color(0xFFD1E6EC), alca = Color(0xFF4E6267)
    ),
    marker = MarkerSwatches(
        ambar = MarkerSwatch(fill = Color(0xFF8D6300), onFill = Color(0xFFFFFFFF), container = Color(0xFFFFDEAB), onContainer = Color(0xFF271900)),
        verde = MarkerSwatch(fill = Color(0xFF327932), onFill = Color(0xFFFFFFFF), container = Color(0xFFA9F59F), onContainer = Color(0xFF002203)),
        azul = MarkerSwatch(fill = Color(0xFF1A6DB5), onFill = Color(0xFFFFFFFF), container = Color(0xFFD2E4FF), onContainer = Color(0xFF001C37)),
        roxo = MarkerSwatch(fill = Color(0xFF755BB5), onFill = Color(0xFFFFFFFF), container = Color(0xFFE9DDFF), onContainer = Color(0xFF23005C)),
        rosa = MarkerSwatch(fill = Color(0xFFAF4282), onFill = Color(0xFFFFFFFF), container = Color(0xFFFFD8E8), onContainer = Color(0xFF3C0028)),
        grafite = MarkerSwatch(fill = Color(0xFF686B6C), onFill = Color(0xFFFFFFFF), container = Color(0xFFE1E3E4), onContainer = Color(0xFF191C1D)),
    ),
)

val GravasomExtraDark: GravasomExtraColors = GravasomExtraColors(
    gravando = StateColors(
        base = Color(0xFFFF897D), onBase = Color(0xFF410002), container = Color(0xFFCC2925), onContainer = Color(0xFFFFFFFF)
    ),
    pausado = StateColors(
        base = Color(0xFFFFB94E), onBase = Color(0xFF291800), container = Color(0xFFFFD6A1), onContainer = Color(0xFF291800)
    ),
    pronto = StateColors(
        base = Color(0xFF76D4E8), onBase = Color(0xFF00363F), container = Color(0xFFA5EEFF), onContainer = Color(0xFF001F25)
    ),
    cofre = StateColors(
        base = Color(0xFFE7B8E4), onBase = Color(0xFF462447), container = Color(0xFF5E3A5E), onContainer = Color(0xFFFFD6FB)
    ),
    vu = VuColors(trilho = Color(0xFF313537), baixo = Color(0xFF969FA2), ok = Color(0xFF60CC9C), clipando = Color(0xFFFF9651)),
    onda = WaveColors(
        fundo = Color(0xFF181C1D), barra = Color(0xFF889396), tocada = Color(0xFF76D4E8), cursor = Color(0xFFDFE3E4), silencio = Color(0xFF3E494B), selecao = Color(0xFF374A4F), alca = Color(0xFFB5CAD0)
    ),
    marker = MarkerSwatches(
        ambar = MarkerSwatch(fill = Color(0xFFF2AC00), onFill = Color(0xFF271900), container = Color(0xFF5F4100), onContainer = Color(0xFFFFDEAB)),
        verde = MarkerSwatch(fill = Color(0xFF80CA79), onFill = Color(0xFF002203), container = Color(0xFF035310), onContainer = Color(0xFFA9F59F)),
        azul = MarkerSwatch(fill = Color(0xFF85BCFF), onFill = Color(0xFF001C37), container = Color(0xFF00497F), onContainer = Color(0xFFD2E4FF)),
        roxo = MarkerSwatch(fill = Color(0xFFC4ACFF), onFill = Color(0xFF23005C), container = Color(0xFF50358E), onContainer = Color(0xFFE9DDFF)),
        rosa = MarkerSwatch(fill = Color(0xFFFF98CE), onFill = Color(0xFF3C0028), container = Color(0xFF811A5C), onContainer = Color(0xFFFFD8E8)),
        grafite = MarkerSwatch(fill = Color(0xFFB7B9BA), onFill = Color(0xFF191C1D), container = Color(0xFF444748), onContainer = Color(0xFFE1E3E4)),
    ),
)

// ---- Paleta Mixlirous: color.themes.mixlirous (pronto, cofre e onda); gravando, pausado, VU e marcadores não mudam

val MixlirousLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF804792),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFAD7FF),
    onPrimaryContainer = Color(0xFF662F79),
    inversePrimary = Color(0xFFEFB0FF),
    secondary = Color(0xFF6A596C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2DCF3),
    onSecondaryContainer = Color(0xFF514254),
    tertiary = Color(0xFF0C6A65),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA2F1EA),
    onTertiaryContainer = Color(0xFF00504C),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF211A13),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF211A13),
    surfaceVariant = Color(0xFFF4DFCB),
    onSurfaceVariant = Color(0xFF524436),
    surfaceTint = Color(0xFF804792),
    inverseSurface = Color(0xFF372F27),
    inverseOnSurface = Color(0xFFFCEEE2),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF857464),
    outlineVariant = Color(0xFFD7C3B0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF8F4),
    surfaceDim = Color(0xFFE5D8CC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1E6),
    surfaceContainer = Color(0xFFFAEBE0),
    surfaceContainerHigh = Color(0xFFF4E6DA),
    surfaceContainerHighest = Color(0xFFEEE0D5),
)

val MixlirousDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFEFB0FF),
    onPrimary = Color(0xFF4D1660),
    primaryContainer = Color(0xFF662F79),
    onPrimaryContainer = Color(0xFFFAD7FF),
    inversePrimary = Color(0xFF804792),
    secondary = Color(0xFFD5C0D7),
    onSecondary = Color(0xFF3A2B3D),
    secondaryContainer = Color(0xFF514254),
    onSecondaryContainer = Color(0xFFF2DCF3),
    tertiary = Color(0xFF87D4CE),
    onTertiary = Color(0xFF003734),
    tertiaryContainer = Color(0xFF00504C),
    onTertiaryContainer = Color(0xFFA2F1EA),
    background = Color(0xFF18120C),
    onBackground = Color(0xFFEEE0D5),
    surface = Color(0xFF18120C),
    onSurface = Color(0xFFEEE0D5),
    surfaceVariant = Color(0xFF524436),
    onSurfaceVariant = Color(0xFFD7C3B0),
    surfaceTint = Color(0xFFEFB0FF),
    inverseSurface = Color(0xFFEEE0D5),
    inverseOnSurface = Color(0xFF372F27),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF9F8E7D),
    outlineVariant = Color(0xFF524436),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF403830),
    surfaceDim = Color(0xFF18120C),
    surfaceContainerLowest = Color(0xFF130D07),
    surfaceContainerLow = Color(0xFF211A13),
    surfaceContainer = Color(0xFF251E17),
    surfaceContainerHigh = Color(0xFF302921),
    surfaceContainerHighest = Color(0xFF3B332C),
)

val MixlirousExtraLight: GravasomExtraColors = GravasomExtraColors(
    gravando = StateColors(
        base = Color(0xFFBA1A1A), onBase = Color(0xFFFFFFFF), container = Color(0xFFBA1A1A), onContainer = Color(0xFFFFFFFF)
    ),
    pausado = StateColors(
        base = Color(0xFF926000), onBase = Color(0xFFFFFFFF), container = Color(0xFFFFCC85), onContainer = Color(0xFF291800)
    ),
    pronto = StateColors(
        base = Color(0xFF804792), onBase = Color(0xFFFFFFFF), container = Color(0xFFFAD7FF), onContainer = Color(0xFF662F79)
    ),
    cofre = StateColors(
        base = Color(0xFF0C6A65), onBase = Color(0xFFFFFFFF), container = Color(0xFFA2F1EA), onContainer = Color(0xFF00504C)
    ),
    vu = VuColors(trilho = Color(0xFFDFE3E4), baixo = Color(0xFF6F797B), ok = Color(0xFF007A54), clipando = Color(0xFFBF5A00)),
    onda = WaveColors(
        fundo = Color(0xFFFFF1E6), barra = Color(0xFF857464), tocada = Color(0xFF804792), cursor = Color(0xFF211A13), silencio = Color(0xFFD7C3B0), selecao = Color(0xFFF2DCF3), alca = Color(0xFF6A596C)
    ),
    marker = MarkerSwatches(
        ambar = MarkerSwatch(fill = Color(0xFF8D6300), onFill = Color(0xFFFFFFFF), container = Color(0xFFFFDEAB), onContainer = Color(0xFF271900)),
        verde = MarkerSwatch(fill = Color(0xFF327932), onFill = Color(0xFFFFFFFF), container = Color(0xFFA9F59F), onContainer = Color(0xFF002203)),
        azul = MarkerSwatch(fill = Color(0xFF1A6DB5), onFill = Color(0xFFFFFFFF), container = Color(0xFFD2E4FF), onContainer = Color(0xFF001C37)),
        roxo = MarkerSwatch(fill = Color(0xFF755BB5), onFill = Color(0xFFFFFFFF), container = Color(0xFFE9DDFF), onContainer = Color(0xFF23005C)),
        rosa = MarkerSwatch(fill = Color(0xFFAF4282), onFill = Color(0xFFFFFFFF), container = Color(0xFFFFD8E8), onContainer = Color(0xFF3C0028)),
        grafite = MarkerSwatch(fill = Color(0xFF686B6C), onFill = Color(0xFFFFFFFF), container = Color(0xFFE1E3E4), onContainer = Color(0xFF191C1D)),
    ),
)

val MixlirousExtraDark: GravasomExtraColors = GravasomExtraColors(
    gravando = StateColors(
        base = Color(0xFFFF897D), onBase = Color(0xFF410002), container = Color(0xFFCC2925), onContainer = Color(0xFFFFFFFF)
    ),
    pausado = StateColors(
        base = Color(0xFFFFB94E), onBase = Color(0xFF291800), container = Color(0xFFFFD6A1), onContainer = Color(0xFF291800)
    ),
    pronto = StateColors(
        base = Color(0xFFEFB0FF), onBase = Color(0xFF4D1660), container = Color(0xFFFAD7FF), onContainer = Color(0xFF330045)
    ),
    cofre = StateColors(
        base = Color(0xFF87D4CE), onBase = Color(0xFF003734), container = Color(0xFF00504C), onContainer = Color(0xFFA2F1EA)
    ),
    vu = VuColors(trilho = Color(0xFF313537), baixo = Color(0xFF969FA2), ok = Color(0xFF60CC9C), clipando = Color(0xFFFF9651)),
    onda = WaveColors(
        fundo = Color(0xFF211A13), barra = Color(0xFF9F8E7D), tocada = Color(0xFFEFB0FF), cursor = Color(0xFFEEE0D5), silencio = Color(0xFF524436), selecao = Color(0xFF514254), alca = Color(0xFFD5C0D7)
    ),
    marker = MarkerSwatches(
        ambar = MarkerSwatch(fill = Color(0xFFF2AC00), onFill = Color(0xFF271900), container = Color(0xFF5F4100), onContainer = Color(0xFFFFDEAB)),
        verde = MarkerSwatch(fill = Color(0xFF80CA79), onFill = Color(0xFF002203), container = Color(0xFF035310), onContainer = Color(0xFFA9F59F)),
        azul = MarkerSwatch(fill = Color(0xFF85BCFF), onFill = Color(0xFF001C37), container = Color(0xFF00497F), onContainer = Color(0xFFD2E4FF)),
        roxo = MarkerSwatch(fill = Color(0xFFC4ACFF), onFill = Color(0xFF23005C), container = Color(0xFF50358E), onContainer = Color(0xFFE9DDFF)),
        rosa = MarkerSwatch(fill = Color(0xFFFF98CE), onFill = Color(0xFF3C0028), container = Color(0xFF811A5C), onContainer = Color(0xFFFFD8E8)),
        grafite = MarkerSwatch(fill = Color(0xFFB7B9BA), onFill = Color(0xFF191C1D), container = Color(0xFF444748), onContainer = Color(0xFFE1E3E4)),
    ),
)

/** Cores extras do tema atual (definidas por [com.gravasom.core.ui.GravasomTheme]); o padrão serve a pré-visualizações. */
val LocalGravasomExtraColors = staticCompositionLocalOf { GravasomExtraLight }
