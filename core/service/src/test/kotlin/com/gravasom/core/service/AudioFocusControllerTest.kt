package com.gravasom.core.service

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Foco de audio durante a gravacao (Given/When/Then):
 *  - ducking (notificacao/navegacao) NAO pode pausar a gravacao;
 *  - perda transiente (ligacao) pausa e, ao recuperar o foco, retoma;
 *  - perda permanente para com erro.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioFocusControllerTest {

    private var pausas = 0
    private val erros = mutableListOf<String>()
    private lateinit var listener: AudioManager.OnAudioFocusChangeListener

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = AudioFocusController(context, onPause = { pausas++ }, onStopWithError = { erros += it })
        assertTrue("pedido de foco deve ser concedido", controller.request())
        val pedido = shadowOf(context.getSystemService(AudioManager::class.java)).lastAudioFocusRequest
        assertNotNull("controller nao registrou pedido de foco", pedido)
        listener = pedido!!.onAudioFocusChangeListener!!
    }

    @Test
    fun `dado gravando quando outro app pede ducking entao a gravacao continua`() {
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        assertEquals("ducking nao deve pausar", 0, pausas)
        assertTrue(erros.isEmpty())
    }

    @Test
    fun `dado perda transiente quando o foco volta entao pausa uma vez`() {
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertEquals(1, pausas)
        // Retomar: o contrato ainda nao existe no controller; este teste fixa o comportamento esperado quando for
        // adicionado um callback onResume (ver relatorio A6). Hoje GAIN so deve ser tolerado, sem erro.
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertTrue(erros.isEmpty())
    }

    @Test
    fun `dado perda permanente quando acontece entao para com erro`() {
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        assertEquals(1, erros.size)
        assertEquals(0, pausas)
    }
}
