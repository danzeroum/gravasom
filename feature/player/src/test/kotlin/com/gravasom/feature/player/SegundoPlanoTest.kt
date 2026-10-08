package com.gravasom.feature.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * Achado 4 da avaliacao heuristica: fechar o player tocando nao interrompe o audio; a pausa e pela notificacao de midia
 * (MediaSessionService sobre o MESMO ExoPlayer da tela).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SegundoPlanoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var audio: String

    @Before
    fun setUp() {
        audio = File.createTempFile("aula", ".wav").apply { writeBytes(ByteArray(64)) }.path
    }

    @After
    fun tearDown() {
        PlaybackHub.player(context).apply { stop(); clearMediaItems() }
    }

    @Test
    fun telaEServicoUsamOMesmoPlayer() {
        val hub = PlaybackHub.player(context)
        val a = PlayerController(context)
        val b = PlayerController(context)
        a.load(audio, title = "Aula")
        assertEquals(1, hub.mediaItemCount)
        assertSame(hub, PlaybackHub.player(context))
        b.release()
        a.release()
    }

    @Test
    fun fecharOPainelNaoPausaNemLiberaOPlayer() {
        val hub = PlaybackHub.player(context)
        val controller = PlayerController(context)
        controller.load(audio, title = "Aula")
        controller.play()
        assertTrue(hub.playWhenReady)

        controller.release() // o que acontece quando o painel/aba sai de cena

        assertTrue("fechar o painel pausou o audio", hub.playWhenReady)
        assertEquals(1, hub.mediaItemCount)
        assertNotNull(hub.currentMediaItem)
    }

    @Test
    fun aTelaReabertaEnxergaOAudioQueSeguiaTocando() {
        val hub = PlaybackHub.player(context)
        val first = PlayerController(context)
        first.load(audio, title = "Aula")
        first.play()
        first.release()

        val second = PlayerController(context)
        second.pause() // pausa de fora, como a notificacao
        assertFalse(hub.playWhenReady)
        second.release()
    }

    @Test
    fun carregarIniciaOServicoDeReproducao() {
        PlayerController(context).load(audio, title = "Aula")
        val started = shadowOf(context as Application).nextStartedService
        assertEquals(ComponentName(context, PlaybackService::class.java), started.component)
    }

    @Test
    fun gravacaoPrivadaNaoVazaONomeNaNotificacao() {
        val hub = PlaybackHub.player(context)
        val controller = PlayerController(context)
        controller.load(audio, title = null)
        assertEquals("Private recording", hub.currentMediaItem!!.mediaMetadata.title.toString())
        controller.load(audio, title = "Aula de fisica")
        assertEquals("Aula de fisica", hub.currentMediaItem!!.mediaMetadata.title.toString())
        controller.release()
    }

    @Test
    fun aSessaoDeMidiaDoServicoEnvolveOPlayerDaTelaEPausaPelaNotificacao() {
        val hub = PlaybackHub.player(context)
        val service = Robolectric.setupService(PlaybackService::class.java)
        val session = service.mediaSession
        assertNotNull(session)
        assertSame("a sessao de midia precisa envolver o player da tela", hub, session!!.player)

        val controller = PlayerController(context)
        controller.load(audio, title = "Aula")
        controller.play()
        assertTrue(hub.playWhenReady)

        // "Pausar" na notificacao de midia = comando de reproducao na sessao.
        session.player.pause()
        assertFalse(hub.playWhenReady)
        controller.release()
        service.onDestroy()
        assertFalse("liberar o servico nao pode liberar o player da tela", hub.isReleased)
    }

    @Test
    fun oServicoSeEncerraQuandoAGravacaoAcaba() {
        val service = Robolectric.setupService(PlaybackService::class.java)
        assertFalse(shadowOf(service).isStoppedBySelf)
        service.endListener.onPlaybackStateChanged(Player.STATE_READY)
        assertFalse("nao pode encerrar enquanto ha audio", shadowOf(service).isStoppedBySelf)
        service.endListener.onPlaybackStateChanged(Player.STATE_ENDED)
        assertTrue(shadowOf(service).isStoppedBySelf)
        service.onDestroy()
    }

    @Test
    fun omanifestDeclaraOServicoComoMidiaENaoExportado() {
        val info = context.packageManager.getServiceInfo(ComponentName(context, PlaybackService::class.java), PackageManager.GET_META_DATA)
        assertFalse("outros apps nao podem controlar a reproducao", info.exported)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, info.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }
}
