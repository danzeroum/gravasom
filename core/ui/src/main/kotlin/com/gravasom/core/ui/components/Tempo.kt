package com.gravasom.core.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import com.gravasom.core.ui.R

private const val MILLIS_POR_SEGUNDO = 1000L
private const val SEGUNDOS_POR_MINUTO = 60L
private const val SEGUNDOS_POR_HORA = 3600L

/** mm:ss, ou h:mm:ss a partir de 1 hora. */
fun formatarTempo(millis: Long): String {
    val total = millis.coerceAtLeast(0L) / MILLIS_POR_SEGUNDO
    val h = total / SEGUNDOS_POR_HORA
    val m = (total % SEGUNDOS_POR_HORA) / SEGUNDOS_POR_MINUTO
    val s = total % SEGUNDOS_POR_MINUTO
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** O tempo por extenso para o leitor de tela ("1 minuto e 5 segundos"), em vez de "01:05". */
@Composable
fun tempoFalado(millis: Long): String {
    val total = (millis.coerceAtLeast(0L) / MILLIS_POR_SEGUNDO).toInt()
    val h = total / SEGUNDOS_POR_HORA.toInt()
    val m = (total % SEGUNDOS_POR_HORA.toInt()) / SEGUNDOS_POR_MINUTO.toInt()
    val s = total % SEGUNDOS_POR_MINUTO.toInt()
    val partes = buildList {
        if (h > 0) add(pluralStringResource(R.plurals.ui_spoken_hours, h, h))
        if (m > 0) add(pluralStringResource(R.plurals.ui_spoken_minutes, m, m))
        if (s > 0 || isEmpty()) add(pluralStringResource(R.plurals.ui_spoken_seconds, s, s))
    }
    return partes.joinToString(" · ")
}

/** Verdadeiro se o usuário desligou as animações do sistema (escala 0): sem pulso nem piscar, o estado segue dito por texto e ícone. */
@Composable
fun rememberReduzirMovimento(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
}
