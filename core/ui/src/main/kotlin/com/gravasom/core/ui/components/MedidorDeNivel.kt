package com.gravasom.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.gravasom.core.ui.GravasomTheme
import com.gravasom.core.ui.R
import com.gravasom.core.ui.theme.GravasomShapes
import com.gravasom.core.ui.theme.GravasomSizes
import com.gravasom.core.ui.theme.GravasomSpacing

/** Zona do medidor e o texto que a acompanha: o nível nunca é dito só pela cor. */
enum class ZonaDeNivel { Baixo, Bom, AltoDemais }

/** Regras do medidor (telas.md §1b): 24 segmentos de 2,5 dB entre -60 e 0 dBFS. */
object MedidorDeNivel {
    const val MIN_DB = -60f
    const val DB_POR_SEGMENTO = 2.5f

    /** Até aqui (inclusive) o nível é "Baixo" (RMS <= -40 dBFS). */
    const val LIMIAR_BAIXO_DB = -40f

    /** Pico a partir do qual o aviso é "Alto demais" (>= -1 dBFS). */
    const val LIMIAR_ALTO_DB = -1f

    private const val ULTIMO_SEGMENTO_BAIXO = 8
    private const val ULTIMO_SEGMENTO_OK = 22

    /** Quantos segmentos acendem para [db] (0..[GravasomSizes.vuSegmentos]). */
    fun segmentosAcesos(db: Float): Int =
        ((db - MIN_DB) / DB_POR_SEGMENTO).toInt().coerceIn(0, GravasomSizes.vuSegmentos)

    /** Zona do segmento [indice] (1..24): 1–8 baixo, 9–22 ok, 23–24 clipando. */
    fun zonaDoSegmento(indice: Int): ZonaDeNivel = when {
        indice <= ULTIMO_SEGMENTO_BAIXO -> ZonaDeNivel.Baixo
        indice <= ULTIMO_SEGMENTO_OK -> ZonaDeNivel.Bom
        else -> ZonaDeNivel.AltoDemais
    }

    /** Rótulo: "Baixo" até -40 dBFS, "Alto demais" no pico a -1 dBFS ou mais, senão "Bom". */
    fun zona(nivelDb: Float, picoDb: Float = nivelDb): ZonaDeNivel = when {
        picoDb >= LIMIAR_ALTO_DB -> ZonaDeNivel.AltoDemais
        nivelDb <= LIMIAR_BAIXO_DB -> ZonaDeNivel.Baixo
        else -> ZonaDeNivel.Bom
    }
}

/**
 * Medidor de nível segmentado com rótulo. Com [ativo] falso (pronto ou pausado) todos os segmentos ficam apagados e não há rótulo.
 * [picoDb] é o pico do bloco (aviso de "Alto demais"); sem ele, o próprio [nivelDb] decide, como no gravador M4A.
 */
@Composable
fun MedidorDeNivel(nivelDb: Float, ativo: Boolean, modifier: Modifier = Modifier, picoDb: Float = nivelDb) {
    val cores = GravasomTheme.extra.vu
    val acesos = if (ativo) MedidorDeNivel.segmentosAcesos(nivelDb) else 0
    val zona = MedidorDeNivel.zona(nivelDb, picoDb)
    val rotulo = if (ativo) zona.rotulo() else null
    val descricao = if (rotulo != null) stringResource(R.string.ui_level_desc, rotulo) else stringResource(R.string.ui_level_off_desc)
    Row(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = descricao },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GravasomSpacing.md),
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(GravasomSizes.vuEspacoSegmento)) {
            for (i in 1..GravasomSizes.vuSegmentos) {
                val cor = if (i <= acesos) cores.cor(MedidorDeNivel.zonaDoSegmento(i)) else cores.trilho
                Box(Modifier.weight(1f).height(GravasomSizes.vuAltura).clip(GravasomShapes.segmentoVu).background(cor))
            }
        }
        Row(Modifier.width(RotuloLargura), verticalAlignment = Alignment.CenterVertically) {
            if (rotulo != null) {
                if (zona == ZonaDeNivel.AltoDemais) {
                    Icon(
                        Icons.Rounded.Warning, contentDescription = null, tint = cores.clipando,
                        modifier = Modifier.size(GravasomSizes.iconePequeno),
                    )
                }
                Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val RotuloLargura = 104.dp

private fun com.gravasom.core.ui.theme.VuColors.cor(zona: ZonaDeNivel): Color = when (zona) {
    ZonaDeNivel.Baixo -> baixo
    ZonaDeNivel.Bom -> ok
    ZonaDeNivel.AltoDemais -> clipando
}

@Composable
private fun ZonaDeNivel.rotulo(): String = stringResource(
    when (this) {
        ZonaDeNivel.Baixo -> R.string.ui_level_low
        ZonaDeNivel.Bom -> R.string.ui_level_ok
        ZonaDeNivel.AltoDemais -> R.string.ui_level_high
    },
)
