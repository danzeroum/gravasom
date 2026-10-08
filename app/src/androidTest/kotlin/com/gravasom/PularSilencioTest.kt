package com.gravasom

import androidx.test.platform.app.InstrumentationRegistry
import com.gravasom.core.audio.engine.analysis.SilenceSensitivity
import com.gravasom.core.audio.engine.analysis.SkipPlan
import com.gravasom.feature.player.PlayerController
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * "Pular silencio" de ponta a ponta no aparelho: o analisador real (decodifica -> reamostra -> WebRTC VAD por JNI) gera o plano
 * e o player real (ExoPlayer) o executa. 10 s de silencio no meio: a reproducao com o pulo ligado dura no maximo 1 s a mais
 * que a fala.
 */
class PularSilencioTest {

    private val instr = InstrumentationRegistry.getInstrumentation()
    private lateinit var player: PlayerController
    private lateinit var arquivo: File

    @Before
    fun preparar() {
        UiSuporte.limparTudo()
        // 5 s de fala + 10 s de silencio + 5 s de fala
        arquivo = Suporte.escreverWavSegmentos(File(Suporte.contexto.cacheDir, "fala_silencio_fala.wav"), 5.0 to true, 10.0 to false, 5.0 to true)
        instr.runOnMainSync { player = PlayerController(Suporte.contexto) }
    }

    @After
    fun limpar() = instr.runOnMainSync { player.release() }

    private fun plano(): SkipPlan {
        val analise = checkNotNull(UiSuporte.app.analyzer.analyze(arquivo)) { "o analisador nao decodificou o WAV" }
        assertEquals(20_000L, analise.durationMillis)
        val indice = SilenceSensitivity.entries.indexOf(SilenceSensitivity.NORMAL)
        return SkipPlan.build(analise.speechFrames(indice), analise.durationMillis)
    }

    @Test
    fun analisadorAcha_lacunaDe10sNoMeio() {
        val p = plano()
        Suporte.medida("pular_silencio", "lacunas", p.ranges.size)
        Suporte.medida("pular_silencio", "economia_prevista_ms", p.skippedMillis)
        val lacuna = p.ranges.maxByOrNull { it.lengthMillis }
        assertNotNull("nenhuma lacuna encontrada", lacuna)
        assertTrue("lacuna $lacuna deveria cobrir o silencio de 5 s a 15 s", lacuna!!.startMillis in 5_000..6_500 && lacuna.endMillis in 14_000..15_000)
        assertTrue("sobra ${p.remainingMillis} ms", p.remainingMillis - 10_000 <= 1_000)
    }

    @Test
    fun reproducaoComPularLigadoDuraNoMaximo1sAMaisQueAFala() {
        val p = plano()
        instr.runOnMainSync { player.load(arquivo.path); player.setSkip(p, true); player.play() }
        assertTrue("nao comecou a tocar", Suporte.aguardar { player.isPlaying.value })
        val inicio = System.nanoTime()
        // ate o fim: a posicao chega a duracao e o player para
        assertTrue("nao terminou em 30 s", Suporte.aguardar(30_000, 20) {
            instr.runOnMainSync { player.tick() }
            !player.isPlaying.value && player.positionMillis.value >= 19_500
        })
        val duracaoMs = (System.nanoTime() - inicio) / 1_000_000
        Suporte.medida("pular_silencio", "duracao_com_pulo_ms", duracaoMs)
        Suporte.medida("pular_silencio", "pulado_ms", player.skippedMillis.value)
        val fala = 10_000L
        assertTrue("tocou por $duracaoMs ms: mais de 1 s alem dos ${fala} ms de fala", duracaoMs <= fala + 1_000)
        assertTrue("economizou so ${player.skippedMillis.value} ms de ${p.skippedMillis}", abs(player.skippedMillis.value - p.skippedMillis) <= 300)
    }

    @Test
    fun comPularDesligadoTocaOArquivoInteiro() {
        val p = plano()
        instr.runOnMainSync { player.load(arquivo.path); player.setSkip(p, false); player.play(); player.seekTo(4_000) }
        assertTrue(Suporte.aguardar { player.isPlaying.value })
        Thread.sleep(3_000) // passa de 4 s para ~7 s, dentro do silencio: sem pulo a posicao anda em tempo real
        instr.runOnMainSync { player.tick() }
        assertTrue("posicao ${player.positionMillis.value}", player.positionMillis.value in 6_500..8_000)
        assertEquals(0L, player.skippedMillis.value)
    }
}
