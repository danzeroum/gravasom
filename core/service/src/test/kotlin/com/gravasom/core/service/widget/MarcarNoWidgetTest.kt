package com.gravasom.core.service.widget

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gravasom.core.audio.engine.RecordingError
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.service.RecordingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/** Correcao da Onda 1: "Marcar" no widget fora de gravacao era ignorado em silencio; agora mostra um aviso curto. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class MarcarNoWidgetTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun servicoIniciado() = shadowOf(context as Application).nextStartedService

    @Test
    fun `sem gravacao mostra o aviso e nao liga o servico`() {
        ShadowToast.reset()
        markFromWidget(context, RecordingState.Idle)
        assertEquals("Nenhuma gravação em andamento para marcar", ShadowToast.getTextOfLatestToast())
        assertNull(servicoIniciado())
    }

    @Test
    fun `em qualquer estado sem gravar tambem avisa`() {
        for (estado in listOf(RecordingState.Preparing, RecordingState.Stopping, RecordingState.Saved("/x.m4a"), RecordingState.Error(RecordingError.MIC_IN_USE))) {
            ShadowToast.reset()
            markFromWidget(context, estado)
            assertEquals("estado $estado", "Nenhuma gravação em andamento para marcar", ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun `gravando pede o marcador ao servico e nao mostra aviso`() {
        ShadowToast.reset()
        markFromWidget(context, RecordingState.Recording(startedAtMillis = 5L))
        val intent = servicoIniciado()
        assertEquals(RecordingService.ACTION_MARKER, intent.action)
        assertEquals(RecordingService::class.java.name, intent.component!!.className)
        assertEquals(0, ShadowToast.shownToastCount())
    }

    @Test
    fun `pausada tambem marca`() {
        ShadowToast.reset()
        markFromWidget(context, RecordingState.Paused)
        assertEquals(RecordingService.ACTION_MARKER, servicoIniciado().action)
        assertEquals(0, ShadowToast.shownToastCount())
    }

    @Test
    @Config(qualifiers = "en")
    fun `o aviso sai em ingles`() {
        ShadowToast.reset()
        markFromWidget(context, RecordingState.Idle)
        assertEquals("No recording in progress to mark", ShadowToast.getTextOfLatestToast())
    }
}
