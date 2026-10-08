package com.gravasom.core.service.marker

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import com.gravasom.core.service.NotificationActions
import com.gravasom.core.service.RecordingNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** "Marcar" sem olhar a tela: a notificacao confirma com numero e instante, e o aparelho vibra uma vez. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarkerFeedbackTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val actions = object : NotificationActions {
        override val openAppIntent = Intent("abrir")
        private fun pending(action: String) =
            PendingIntent.getBroadcast(context, 0, Intent(action), PendingIntent.FLAG_IMMUTABLE)
        override fun resumePendingIntent(context: Context) = pending("retomar")
        override fun pausePendingIntent(context: Context) = pending("pausar")
        override fun markerPendingIntent(context: Context) = pending("marcar")
        override fun stopPendingIntent(context: Context) = pending("parar")
    }

    private fun texto(n: Notification) = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

    @Test
    fun relogioUsaMinutosESegundosEHorasSoQuandoPrecisa() {
        assertEquals("00:00", MarkerFeedback.clock(0))
        assertEquals("00:00", MarkerFeedback.clock(-5))
        assertEquals("01:05", MarkerFeedback.clock(65_999))
        assertEquals("59:59", MarkerFeedback.clock(3_599_000))
        assertEquals("1:00:00", MarkerFeedback.clock(3_600_000))
        assertEquals("45:10", MarkerFeedback.clock(2_710_000))
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun notificacaoMostraMarcadorNEmMinutosESegundosEmPtBr() {
        val n = RecordingNotification.build(context, 0, paused = false, actions = actions, markerCount = 3, lastMarkerMillis = 65_000)
        assertEquals("Marcador 3 às 01:05", texto(n))
    }

    @Test
    fun notificacaoMostraMarkerNAtEmIngles() {
        val n = RecordingNotification.build(context, 0, paused = false, actions = actions, markerCount = 1, lastMarkerMillis = 2_000)
        assertEquals("Marker 1 at 00:02", texto(n))
    }

    @Test
    fun semMarcadorANotificacaoNaoTemTextoDeMarcador() {
        val n = RecordingNotification.build(context, 0, paused = false, actions = actions)
        assertNull(texto(n))
    }

    @Test
    fun pausadaTambemConfirmaOMarcador() {
        val n = RecordingNotification.build(context, 0, paused = true, actions = actions, markerCount = 2, lastMarkerMillis = 10_000)
        assertEquals("Marker 2 at 00:10", texto(n))
    }

    @Test
    fun umaVibracaoCurtaPorMarcador() {
        val vibrator = shadowOf(context.getSystemService(Vibrator::class.java))
        SystemHaptics(context).tick()
        assertEquals(60L, vibrator.milliseconds)
    }
}
