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
    private var retomadas = 0
    private val erros = mutableListOf<String>()
    private lateinit var listener: AudioManager.OnAudioFocusChangeListener

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = AudioFocusController(context, onPause = { pausas++ }, onStopWithError = { erros += it }, onResume = { retomadas++ })
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
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals("deve retomar quando o foco volta", 1, retomadas)
        assertTrue(erros.isEmpty())
    }

    @Test
    fun `dado ganho de foco sem pausa causada pelo foco quando acontece entao nao retoma`() {
        // ex.: o usuario pausou manualmente; um GAIN avulso nao pode desfazer a pausa dele
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(0, retomadas)
    }

    @Test
    fun `dado perda permanente quando acontece entao para com erro`() {
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        assertEquals(1, erros.size)
        assertEquals(0, pausas)
    }
}
