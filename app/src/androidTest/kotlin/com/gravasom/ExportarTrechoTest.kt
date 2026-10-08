package com.gravasom

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.gravasom.core.audio.android.mp3.LameBridge
import com.gravasom.core.audio.engine.edit.ClipResult
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.service.clip.ClipExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Exportar o trecho selecionado como arquivo novo que toca em OUTRO player (MediaPlayer do sistema, nao o ExoPlayer do app). */
class ExportarTrechoTest {

    @get:Rule(order = 0)
    val permissoes = UiSuporte.permissoes()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun preparar() = UiSuporte.limparTudo()

    private fun pasta() = File(Suporte.contexto.cacheDir, "trechos").apply { mkdirs() }

    private fun mp3(segundos: Double, destino: File): File {
        assertTrue("libmp3lame nao carregou", LameBridge.ensureLoaded())
        val taxa = 44_100
        val pcm = ShortArray((segundos * taxa).toInt()) { (Math.sin(2 * Math.PI * 1000.0 * it / taxa) * 0.5 * Short.MAX_VALUE).toInt().toShort() }
        val h = LameBridge.nativeInit(taxa, 1, 128, 5)
        try {
            destino.outputStream().use { saida ->
                val bloco = taxa / 100
                val out = ByteArray(LameBridge.outputCapacityFor(bloco))
                val tmp = ShortArray(bloco)
                var i = 0
                while (i < pcm.size) {
                    val n = minOf(bloco, pcm.size - i)
                    System.arraycopy(pcm, i, tmp, 0, n)
                    saida.write(out, 0, LameBridge.nativeEncode(h, tmp, n, out))
                    i += n
                }
                val fim = ByteArray(16_384)
                saida.write(fim, 0, LameBridge.nativeFlush(h, fim))
            }
        } finally {
            LameBridge.nativeClose(h)
        }
        return destino
    }

    /** Toca o arquivo com o MediaPlayer ate o fim e devolve (duracao anunciada, ms que levou tocando). */
    private fun tocarComMediaPlayer(arquivo: File): Pair<Int, Long> {
        val fim = CountDownLatch(1)
        val erro = arrayOfNulls<String>(1)
        val mp = MediaPlayer()
        try {
            mp.setDataSource(arquivo.path)
            mp.setOnCompletionListener { fim.countDown() }
            mp.setOnErrorListener { _, what, extra -> erro[0] = "erro $what/$extra"; fim.countDown(); true }
            mp.prepare()
            val duracao = mp.duration
            val inicio = System.currentTimeMillis()
            mp.start()
            assertTrue("nao terminou de tocar", fim.await(duracao + 5_000L, TimeUnit.MILLISECONDS))
            assertEquals("MediaPlayer falhou: ${erro[0]}", null, erro[0])
            return duracao to (System.currentTimeMillis() - inicio)
        } finally {
            mp.release()
        }
    }

    private fun exportar(formato: RecordingFormat, origem: File, inicio: Long, fim: Long): File {
        val destino = File(pasta(), "trecho_${formato.name}.${formato.extension}")
        val r = ClipExporter().export(origem, formato, inicio, fim, destino)
        assertTrue("exportacao falhou: $r", r is ClipResult.Done)
        val duracao = (r as ClipResult.Done).durationMillis
        assertTrue("duracao relatada $duracao ms", abs(duracao - (fim - inicio)) <= 100)
        return destino
    }

    @Test
    fun trechoDeWavTocaNoMediaPlayer() {
        val origem = Suporte.escreverWavSeno(File(pasta(), "origem.wav"), 10.0)
        val trecho = exportar(RecordingFormat.WAV, origem, 2_000, 5_000)
        val (duracao, tocou) = tocarComMediaPlayer(trecho)
        Suporte.medida("trecho_wav", "duracao_ms", duracao)
        assertTrue("duracao $duracao", abs(duracao - 3_000) <= 100)
        assertTrue("tocou por $tocou ms", abs(tocou - 3_000) <= 600)
    }

    @Test
    fun trechoDeM4aTocaNoMediaPlayer() {
        val origem = Suporte.escreverM4aSeno(File(pasta(), "origem.m4a"), 10.0)
        val trecho = exportar(RecordingFormat.M4A, origem, 2_000, 5_000)
        val (duracao, tocou) = tocarComMediaPlayer(trecho)
        Suporte.medida("trecho_m4a", "duracao_ms", duracao)
        assertTrue("duracao $duracao", abs(duracao - 3_000) <= 120)
        assertTrue("tocou por $tocou ms", abs(tocou - 3_000) <= 700)
        // o original nao foi tocado
        assertTrue(abs(com.gravasom.core.service.MediaProbe.durationMillis(origem)!! - 10_000L) <= 150)
    }

    @Test
    fun trechoDeMp3TocaNoMediaPlayer() {
        val origem = mp3(10.0, File(pasta(), "origem.mp3"))
        val trecho = exportar(RecordingFormat.MP3, origem, 2_000, 5_000)
        val (duracao, tocou) = tocarComMediaPlayer(trecho)
        Suporte.medida("trecho_mp3", "duracao_ms", duracao)
        assertTrue("duracao $duracao", abs(duracao - 3_000) <= 150)
        assertTrue("tocou por $tocou ms", abs(tocou - 3_000) <= 800)
    }

    @Test
    fun intervaloInvalidoFalhaSemDeixarArquivo() {
        val origem = Suporte.escreverM4aSeno(File(pasta(), "origem2.m4a"), 3.0)
        val destino = File(pasta(), "nada.m4a")
        assertTrue(ClipExporter().export(origem, RecordingFormat.M4A, 2_000, 2_000, destino) is ClipResult.Failed)
        assertTrue(ClipExporter().export(origem, RecordingFormat.M4A, 9_000, 12_000, destino) is ClipResult.Failed)
        assertTrue(!destino.exists() && !File(destino.path + ".tmp").exists())
    }

    /** Pela tela: selecionar o trecho, exportar e ver a gravacao nova na lista, com a duracao do trecho. */
    @Test
    fun exportarPelaTelaCriaGravacaoNovaComADuracaoDoTrecho() {
        UiSuporte.inserirGravacao("Aula", segundos = 30.0)
        compose.onNodeWithTag("aba_gravacoes").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
        compose.onNodeWithTag("item_gravacao").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "selecionar_trecho") }
        compose.onNodeWithTag("mini_play_pause").performClick() // pausa para a selecao partir de uma posicao estavel
        compose.onNodeWithTag("selecionar_trecho").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "exportar_trecho") }
        compose.onNodeWithTag("exportar_trecho").performClick()
        UiSuporte.aguardar(compose, 20_000) { UiSuporte.textos(compose, "titulo_item").size == 2 }
        val novo = UiSuporte.textos(compose, "titulo_item").first { it != "Aula" }
        assertTrue("titulo '$novo'", novo.startsWith("Aula · "))
        val gravacao = runBlocking {
            UiSuporte.app.repo.observeAll().first().first { it.title == novo }
        }
        val mmr = MediaMetadataRetriever()
        val duracao = try {
            mmr.setDataSource(gravacao.path)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
        } finally {
            mmr.release()
        }
        Suporte.medida("exportar_pela_tela", "duracao_ms", duracao)
        assertTrue("duracao do trecho $duracao ms (esperado ~10 s)", abs(duracao - 10_000) <= 200)
    }

}
