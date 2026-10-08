package com.gravasom.core.service.marker

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.gravasom.core.service.R

/** Texto de confirmacao de um marcador criado sem olhar a tela (notificacao, widget): "Marcador 3 as 12:40". */
object MarkerFeedback {

    private const val SECONDS_PER_MINUTE = 60
    private const val SECONDS_PER_HOUR = 3_600
    private const val MILLIS_PER_SECOND = 1_000

    /** `mm:ss`; a partir de 1 h, `h:mm:ss`. */
    fun clock(positionMillis: Long): String {
        val total = (positionMillis.coerceAtLeast(0) / MILLIS_PER_SECOND).toInt()
        val hours = total / SECONDS_PER_HOUR
        val minutes = total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val seconds = total % SECONDS_PER_MINUTE
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
    }

    fun text(context: Context, number: Int, positionMillis: Long): String =
        context.getString(R.string.notif_marker_added, number, clock(positionMillis))
}

/** Pulso tatil curto. Interface para que o servico seja testavel sem vibrador de verdade. */
fun interface Haptics {
    fun tick()
}

/** Uma unica vibracao curta no vibrador do aparelho; sem vibrador (ou sem permissao) nao faz nada. */
class SystemHaptics(private val context: Context) : Haptics {

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    }

    override fun tick() {
        val target = vibrator?.takeIf { it.hasVibrator() } ?: return
        target.vibrate(VibrationEffect.createOneShot(PULSE_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private companion object {
        const val PULSE_MILLIS = 60L
    }
}
