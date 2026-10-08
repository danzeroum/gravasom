// GERADO por docs/design/ferramentas/gerar_kotlin.py a partir de docs/design/tokens.json. NAO EDITE A MAO:
// mude o tokens.json (ou o script) e rode o gerador de novo.
// tokens.json sha256: bfb38b9f739d953e266185aa0d8d5a5065e2aa079e18b8bf8b2f4c5394dc01b2
@file:Suppress("ktlint", "MaxLineLength")

package com.gravasom.core.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val GravasomShapeScale = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Formas por componente. `botaoGravar` e `botaoParar` são a mesma peça em dois instantes (ver [GravasomShapeRadii]). */
object GravasomShapes {
    val botaoGravar: Shape = CircleShape
    val botaoParar: Shape = RoundedCornerShape(28.dp)
    val botaoTransporte: Shape = CircleShape
    val faixaEstado: Shape = CircleShape
    val chip: Shape = RoundedCornerShape(8.dp)
    val cartao: Shape = RoundedCornerShape(16.dp)
    val folhaInferior: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val dialogo: Shape = RoundedCornerShape(28.dp)
    val campoBusca: Shape = CircleShape
    val bandeiraMarcador: Shape = RoundedCornerShape(6.dp)
    val onda: Shape = RoundedCornerShape(12.dp)
    val segmentoVu: Shape = RoundedCornerShape(2.dp)
    val widget: Shape = RoundedCornerShape(20.dp)
}

/** Raios em dp (os "50%" do tokens.json ficam de fora: são metade do lado), para animar a transição de forma. */
object GravasomShapeRadii {
    val botaoParar = 28.dp
    val chip = 8.dp
    val cartao = 16.dp
    val folhaInferior = 28.dp
    val dialogo = 28.dp
    val bandeiraMarcador = 6.dp
    val onda = 12.dp
    val segmentoVu = 2.dp
    val widget = 20.dp
}
