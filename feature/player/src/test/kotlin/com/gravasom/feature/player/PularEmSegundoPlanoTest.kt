package com.gravasom.feature.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gravasom.core.audio.engine.analysis.SkipPlan
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Correcao da Onda 1: "pular silencio so atuava com o painel do player aberto". O plano e o relogio agora ficam no PlaybackHub
 * (o mesmo lugar do ExoPlayer e da notificacao de midia); fechar o painel solta o controlador, nao o pulo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PularEmSegundoPlanoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private class Fake(var position: Long) : SkipTarget {
        override val isPlaying = true
        override val positionMillis get() = position
        override fun seekTo(millis: Long) { position = millis }
    }

    private val plan = SkipPlan.build(BooleanArray(1_000) { it < 250 || it >= 750 }, 20_000L)

    @Before
    fun setUp() {
        PlaybackHub.player(context) // cria o hub (o ExoPlayer real nao toca nada aqui)
    }

    @After
    fun tearDown() {
        PlaybackHub.skipper.reset()
        PlaybackHub.player(context).stop()
    }

    @Test
    fun `fechar o painel com o pulo ligado nao desliga o pulo e o silencio de 10 s e pulado`() {
        val fake = Fake(position = 6_000L) // dentro dos 10 s de silencio (de 5 s a 15 s)
        PlaybackHub.install(fake)
        val controller = PlayerController(context)
        controller.setSkip(plan, enabled = true)

        controller.release() // o painel fechou (ou a aba saiu de cena)
        PlaybackHub.tick() // o relogio do hub segue batendo em segundo plano

        assertTrue("saltou para ${fake.position}", fake.position in 14_000L..15_000L)
        assertTrue(PlaybackHub.skipper.skippedMillis >= 8_000L)
    }

    @Test
    fun `o plano continua no hub depois que o controlador e solto e um novo controlador o enxerga`() {
        PlaybackHub.install(Fake(position = 0L))
        val first = PlayerController(context)
        first.setSkip(plan, enabled = true)
        first.release()

        assertEquals(plan, PlaybackHub.skipper.plan)
        assertTrue(PlaybackHub.skipper.enabled)
        val second = PlayerController(context)
        PlaybackHub.tick()
        assertNotNull(second.skippedMillis.value)
        second.release()
    }

    @Test
    fun `carregar outra gravacao descarta o plano da anterior`() {
        PlaybackHub.install(Fake(position = 0L))
        val controller = PlayerController(context)
        controller.setSkip(plan, enabled = true)
        val file = java.io.File.createTempFile("outra", ".wav").apply { writeBytes(ByteArray(64)) }
        controller.load(file.path, title = "Outra")
        assertEquals(null, PlaybackHub.skipper.plan)
        assertEquals(0L, PlaybackHub.skipper.skippedMillis)
        controller.release()
    }

    @Test
    fun `o relogio avisa quem observa e para de avisar depois de solto`() {
        PlaybackHub.install(Fake(position = 0L))
        var batidas = 0
        val listener: () -> Unit = { batidas++ }
        PlaybackHub.addTickListener(listener)
        PlaybackHub.tick()
        PlaybackHub.removeTickListener(listener)
        PlaybackHub.tick()
        assertEquals(1, batidas)
    }
}
