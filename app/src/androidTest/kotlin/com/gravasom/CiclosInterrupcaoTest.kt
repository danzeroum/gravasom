package com.gravasom

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.GrantPermissionRule
import com.gravasom.core.audio.android.recorder.MediaRecorderM4aRecorder
import com.gravasom.core.audio.engine.RecordingError
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.service.BootRecovery
import com.gravasom.core.service.MediaProbe
import com.gravasom.core.service.RecordingService
import com.gravasom.core.service.RecordingServiceState
import com.gravasom.core.service.RecordingStorage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Fase 4: 100 ciclos de interrupcao. Cada ciclo tem de terminar sem arquivo corrompido:
 * ou o arquivo final e reproduzivel pelo MediaExtractor, ou nao existe (descartado e contado).
 * "Corrompido" = arquivo final nao reproduzivel ou .tmp esquecido apos a recuperacao.
 */
class CiclosInterrupcaoTest {

    @get:Rule(order = 0)
    val permissoes: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val tela = ActivityScenarioRule(MainActivity::class.java)

    private val audio get() = Suporte.contexto.getSystemService(AudioManager::class.java)
    private val dirServico get() = RecordingStorage.dir(Suporte.contexto)
    private var corrompidos = 0
    private var recuperados = 0
    private var descartados = 0
    private lateinit var m4aReferencia: File

    @Before
    fun preparar() {
        ControleServico.reiniciar()
        dirServico.listFiles()?.forEach { it.delete() }
        m4aReferencia = File(Suporte.testsOut(), "referencia.m4a")
        runBlocking {
            val g = MediaRecorderM4aRecorder()
            g.prepare(RecordingConfig(format = RecordingFormat.M4A), m4aReferencia.path).getOrThrow()
            g.start().getOrThrow()
            Thread.sleep(2_000)
            g.stop().getOrThrow()
            g.release()
        }
    }

    @After
    fun limpar() = ControleServico.reiniciar()

    @Test
    fun cemCiclosSemArquivoCorrompido() {
        repeat(CICLOS) { i ->
            val tipo = TIPOS[i % TIPOS.size]
            runCatching { executar(tipo, i) }
                .onFailure { corrompidos++; Suporte.medida("ciclos", "falha_ciclo_${i}_$tipo", it.toString().take(200)) }
            corrompidos += auditar(dirServico)
        }
        Suporte.medida("ciclos", "total", CICLOS)
        Suporte.medida("ciclos", "recuperados", recuperados)
        Suporte.medida("ciclos", "descartados_irrecuperaveis", descartados)
        Suporte.medida("ciclos", "corrompidos", corrompidos)
        assertEquals("arquivos corrompidos/ciclos com falha", 0, corrompidos)
    }

    private fun executar(tipo: String, i: Int) {
        when (tipo) {
            "chamada" -> cicloChamada()
            "foco_permanente" -> cicloFocoPermanente()
            "kill" -> cicloKill(i)
            "wav_truncado" -> cicloWavTruncado(i)
            "m4a_truncado" -> cicloM4aTruncado(i)
        }
    }

    private fun iniciarWav() {
        RecordingServiceState.update(RecordingState.Idle)
        ControleServico.enviar(RecordingService.ACTION_START, "WAV")
        check(ControleServico.esperarGravando()) { "nao entrou em Recording: ${RecordingServiceState.state.value}" }
        Thread.sleep(600)
    }

    private fun pararEEncerrar() {
        ControleServico.enviar(RecordingService.ACTION_STOP)
        check(ControleServico.esperarEncerrado()) { "servico nao encerrou" }
    }

    private fun focoConcorrente(permanente: Boolean): AudioFocusRequest {
        val pedido = AudioFocusRequest.Builder(
            if (permanente) AudioManager.AUDIOFOCUS_GAIN else AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
        ).setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        ).setOnAudioFocusChangeListener { }.build()
        audio.requestAudioFocus(pedido)
        return pedido
    }

    private fun cicloChamada() {
        iniciarWav()
        val pedido = focoConcorrente(permanente = false)
        check(Suporte.aguardar(5_000) { RecordingServiceState.state.value == RecordingState.Paused }) {
            "chamada nao pausou: ${RecordingServiceState.state.value}"
        }
        audio.abandonAudioFocusRequest(pedido)
        ControleServico.enviar(RecordingService.ACTION_RESUME)
        check(ControleServico.esperarGravando()) { "nao retomou apos a chamada" }
        Thread.sleep(300)
        pararEEncerrar()
    }

    private fun cicloFocoPermanente() {
        iniciarWav()
        val pedido = focoConcorrente(permanente = true)
        val estado = { RecordingServiceState.state.value }
        check(Suporte.aguardar(8_000) { estado() == RecordingState.Error(RecordingError.MIC_IN_USE) }) {
            "perda de foco nao sinalizou MIC_IN_USE: ${estado()}"
        }
        audio.abandonAudioFocusRequest(pedido)
        check(ControleServico.esperarEncerrado()) { "servico nao encerrou apos perda de foco" }
    }

    /** Fotografa o .tmp ao vivo e trunca em ponto aleatorio: o que o disco teria apos um kill. */
    private fun cicloKill(i: Int) {
        iniciarWav()
        Thread.sleep(400)
        val vivo = checkNotNull(dirServico.listFiles { f -> f.name.endsWith(".tmp") }?.firstOrNull()) { "sem .tmp ao vivo" }
        val bytes = vivo.readBytes()
        val corte = Random(i).nextInt(0, bytes.size + 1)
        val dir = File(Suporte.contexto.cacheDir, "kill_$i").apply { deleteRecursively(); mkdirs() }
        File(dir, vivo.name).writeBytes(bytes.copyOf(corte))
        recuperarEm(dir, aceitaDescarte = corte < WavHeaderWriter.HEADER_SIZE + BYTES_10MS)
        pararEEncerrar()
    }

    private fun cicloWavTruncado(i: Int) {
        val rnd = Random(1000 + i)
        val dados = rnd.nextInt(0, 2 * 44_100 * 2)
        val dir = File(Suporte.contexto.cacheDir, "wavtmp_$i").apply { deleteRecursively(); mkdirs() }
        val tmp = File(dir, "gravacao_$i.wav.tmp")
        tmp.writeBytes(WavHeaderWriter.build(44_100, 1, 16, 0) + ByteArray(dados) { (it % 7).toByte() })
        val esperadoMs = (dados / 2) * 1000L / 44_100
        recuperarEm(dir, aceitaDescarte = esperadoMs == 0L)
        File(dir, "gravacao_$i.wav").takeIf { it.exists() }?.let {
            val ms = MediaProbe.durationMillis(it)!!
            assertTrue("duracao $ms ms x esperado $esperadoMs ms", kotlin.math.abs(ms - esperadoMs) <= 5)
        }
    }

    private fun cicloM4aTruncado(i: Int) {
        val rnd = Random(2000 + i)
        val bytes = m4aReferencia.readBytes()
        val corte = (bytes.size * (0.1 + 0.8 * rnd.nextDouble())).toInt()
        val dir = File(Suporte.contexto.cacheDir, "m4atmp_$i").apply { deleteRecursively(); mkdirs() }
        File(dir, "gravacao_$i.m4a.tmp").writeBytes(bytes.copyOf(corte))
        recuperarEm(dir, aceitaDescarte = true)
    }

    private fun recuperarEm(dir: File, aceitaDescarte: Boolean) {
        var descartou = false
        BootRecovery(
            tempDir = dir,
            isPlayable = MediaProbe::isPlayable,
            onUnrecoverable = { descartou = true; descartados++ },
            onRecovered = { recuperados++ },
        ).run()
        assertTrue("descartou um arquivo recuperavel", !descartou || aceitaDescarte)
        corrompidos += auditar(dir)
    }

    /** Conta arquivos corrompidos: .tmp esquecido ou arquivo final nao reproduzivel. */
    private fun auditar(dir: File): Int = dir.listFiles().orEmpty().count { f ->
        f.name.endsWith(".tmp") || !MediaProbe.isPlayable(f)
    }

    private companion object {
        const val CICLOS = 100
        const val BYTES_10MS = 882
        val TIPOS = listOf("chamada", "foco_permanente", "kill", "wav_truncado", "m4a_truncado")
    }
}
