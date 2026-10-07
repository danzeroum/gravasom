package com.gravasom

import androidx.test.platform.app.InstrumentationRegistry
import com.gravasom.feature.player.PlayerController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** Fase 6: o player Media3 toca, busca e pausa um WAV de 6 s conhecido. */
class PlayerMedia3Test {

    private val instr = InstrumentationRegistry.getInstrumentation()
    private lateinit var player: PlayerController
    private lateinit var arquivo: File

    @Before
    fun preparar() {
        arquivo = Suporte.escreverWavSeno(File(Suporte.contexto.cacheDir, "player_6s.wav"), 6.0)
        instr.runOnMainSync { player = PlayerController(Suporte.contexto) }
    }

    @After
    fun limpar() = instr.runOnMainSync { player.release() }

    private fun main(bloco: () -> Unit) = instr.runOnMainSync(bloco)

    @Test
    fun tocaBuscaEPausa() {
        main { player.load(arquivo.path); player.play() }
        assertTrue("nao comecou a tocar", Suporte.aguardar { player.isPlaying.value })

        main { player.seekTo(4_000) }
        assertTrue("busca nao refletiu na posicao", Suporte.aguardar {
            main { player.tick() }
            player.positionMillis.value >= 4_000
        })
        main { player.tick() }
        assertTrue("duracao carregada=${player.durationMillis.value}", kotlin.math.abs(player.durationMillis.value - 6_000) <= 100)

        main { player.pause() }
        assertTrue("nao pausou", Suporte.aguardar { !player.isPlaying.value })
        main { player.tick() }
        val parada = player.positionMillis.value
        Thread.sleep(800)
        main { player.tick() }
        assertEquals("posicao andou apos pausar", parada, player.positionMillis.value)
        assertFalse(player.isPlaying.value)
    }
}
