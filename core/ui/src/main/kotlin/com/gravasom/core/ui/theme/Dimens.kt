// GERADO por docs/design/ferramentas/gerar_kotlin.py a partir de docs/design/tokens.json. NAO EDITE A MAO:
// mude o tokens.json (ou o script) e rode o gerador de novo.
// tokens.json sha256: bfb38b9f739d953e266185aa0d8d5a5065e2aa079e18b8bf8b2f4c5394dc01b2
@file:Suppress("ktlint", "MaxLineLength")

package com.gravasom.core.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

object GravasomSpacing {
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
    val margemTela: Dp = 16.dp
    val entreGrupos: Dp = 24.dp
}

object GravasomSizes {
    val alvoToqueMin: Dp = 48.dp
    val botaoGravar: Dp = 96.dp
    val botaoParar: Dp = 96.dp
    val botaoTransporte: Dp = 72.dp
    val iconeBotaoGravar: Dp = 40.dp
    val iconeTransporte: Dp = 32.dp
    val icone: Dp = 24.dp
    val iconePequeno: Dp = 18.dp
    val faixaEstadoAltura: Dp = 48.dp
    val vuAltura: Dp = 16.dp
    const val vuSegmentos: Int = 24
    val vuEspacoSegmento: Dp = 2.dp
    val ondaAltura: Dp = 128.dp
    val bandeiraMarcador: Dp = 20.dp
    val linhaMarcador: Dp = 2.dp
    val alvoMarcador: Dp = 48.dp
    val alcaSelecao: Dp = 24.dp
    val pontoGravando: Dp = 12.dp
    val miniPlayerAltura: Dp = 64.dp
    val itemListaMin: Dp = 72.dp
    val widgetMinLargura: Dp = 110.dp
    val widgetMinAltura: Dp = 40.dp
    val widget2x1 = DpSize(110.dp, 40.dp)
    val widget4x1 = DpSize(250.dp, 40.dp)
    val widgetPontoQuebra: Dp = 250.dp
}

object GravasomElevation {
    val level0: Dp = 0.dp
    val level1: Dp = 1.dp
    val level2: Dp = 3.dp
    val level3: Dp = 6.dp
    val level4: Dp = 8.dp
    val level5: Dp = 12.dp
}
