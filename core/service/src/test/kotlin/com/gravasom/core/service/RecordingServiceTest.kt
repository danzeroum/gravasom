package com.gravasom.core.service

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.domain.audio.AudioRecorder
import com.gravasom.core.domain.audio.AudioRecorderFactory
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import java.io.File

/**
 * Ciclo de vida do RecordingService com gravador falso (tudo local, sem emulador):
 *  - A1: START duplicado (Tile/widget so enviam START) nao pode criar um segundo gravador;
 *  - A2: sem RECORD_AUDIO o servico iniciado por startForegroundService precisa chamar startForeground antes de parar;
 *  - A3: falha na partida libera o gravador e encerra o servico;
 *  - A4: stop() que falha nao pode virar "gravacao salva" vazia;
 *  - A5: STOP durante a preparacao nao deixa o microfone aberto.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordingServiceTest {

    private class GravadorFalso(
        private val falhaNoPrepare: Boolean = false,
        private val falhaNoStop: Boolean = false,
        private val atrasoPrepare: Boolean = false,
    ) : AudioRecorder {
        override val format = RecordingFormat.WAV
        var prepares = 0
        var starts = 0
        var stops = 0
        var releases = 0
        @Volatile var liberarPrepare = !atrasoPrepare
        private lateinit var caminho: String

        override suspend fun prepare(config: RecordingConfig, outputPath: String): Result<Unit> {
            prepares++
            while (!liberarPrepare) kotlinx.coroutines.delay(10)
            caminho = outputPath
            return if (falhaNoPrepare) Result.failure(IllegalStateException("encoder")) else Result.success(Unit)
        }

        override suspend fun start(): Result<Unit> { starts++; return Result.success(Unit) }
        override suspend fun pause(): Result<Unit> = Result.success(Unit)
        override suspend fun resume(): Result<Unit> = Result.success(Unit)
        override suspend fun stop(): Result<String> {
            stops++
            if (falhaNoStop) return Result.failure(java.io.IOException("disco cheio"))
            File(caminho).writeBytes(ByteArray(10))
            return Result.success(caminho)
        }
        override fun level(): Flow<Float> = emptyFlow()
        override fun release() { releases++ }
    }

    private val criados = mutableListOf<GravadorFalso>()
    private var proximo: () -> GravadorFalso = { GravadorFalso() }
    private lateinit var app: Application
    private lateinit var controller: ServiceController<RecordingService>

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        RecordingServiceState.update(RecordingState.Idle)
        DefaultRecorderFactoryHolder.factory = AudioRecorderFactory { _ -> proximo().also { criados += it } }
        controller = Robolectric.buildService(RecordingService::class.java).create()
    }

    @After
    fun tearDown() {
        DefaultRecorderFactoryHolder.factory = null
        RecordingServiceState.update(RecordingState.Idle)
        RecordingStorage.dir(app).deleteRecursively()
    }

    private fun conceder() = shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

    private fun enviar(acao: String) {
        controller.withIntent(Intent(app, RecordingService::class.java).setAction(acao).putExtra(RecordingService.EXTRA_FORMAT, "WAV"))
            .startCommand(0, 0)
    }

    /** Roda o looper principal e espera [condicao] (o DataStore le preferencias em outra thread). */
    private fun aguardar(condicao: () -> Boolean, ms: Long = 8_000): Boolean {
        val fim = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < fim) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condicao()) return true
            Thread.sleep(20)
        }
        return condicao()
    }

    @Test
    fun `dado gravando quando chegam mais STARTs entao so existe um gravador`() {
        conceder()
        enviar(RecordingService.ACTION_START)
        assertTrue("nao entrou em Recording: ${RecordingServiceState.state.value}", aguardar { RecordingServiceState.state.value is RecordingState.Recording })

        repeat(3) { enviar(RecordingService.ACTION_START) }
        aguardar({ false }, ms = 600)

        assertEquals("START duplicado criou outro gravador", 1, criados.size)
        assertTrue(RecordingServiceState.state.value is RecordingState.Recording)
        assertEquals(1, criados.single().starts)
        assertEquals("o gravador original nao pode ser liberado", 0, criados.single().releases)
    }

    @Test
    fun `dado servico iniciado como foreground sem permissao de microfone quando START entao chama startForeground antes de parar`() {
        // sem conceder RECORD_AUDIO
        enviar(RecordingService.ACTION_START)
        shadowOf(Looper.getMainLooper()).idle()

        val servico = controller.get()
        assertTrue("startForeground nao foi chamado: o app cairia com ForegroundServiceDidNotStartInTimeException",
            shadowOf(servico).lastForegroundNotification != null)
        assertEquals(RecordingState.Error(com.gravasom.core.audio.engine.RecordingError.PERMISSION_DENIED), RecordingServiceState.state.value)
        assertTrue(criados.isEmpty())
        assertTrue(shadowOf(servico).isStoppedBySelf)
    }

    @Test
    fun `dado falha ao preparar o gravador quando START entao libera o gravador e encerra o servico`() {
        conceder()
        proximo = { GravadorFalso(falhaNoPrepare = true) }
        enviar(RecordingService.ACTION_START)

        assertTrue("estado: ${RecordingServiceState.state.value}", aguardar { RecordingServiceState.state.value is RecordingState.Error })
        assertTrue("gravador nao foi liberado", aguardar { criados.singleOrNull()?.releases?.let { it >= 1 } == true })
        assertTrue("servico ficou em primeiro plano apos a falha", aguardar { shadowOf(controller.get()).isStoppedBySelf })
    }

    @Test
    fun `dado stop do gravador falhando quando STOP entao vira erro e nao gravacao salva vazia`() {
        conceder()
        proximo = { GravadorFalso(falhaNoStop = true) }
        val salvos = mutableListOf<String>()
        val job = kotlinx.coroutines.runBlocking { null }
        enviar(RecordingService.ACTION_START)
        assertTrue(aguardar { RecordingServiceState.state.value is RecordingState.Recording })
        val coleta = Thread { kotlinx.coroutines.runBlocking { RecordingServiceState.saved.collect { salvos += it } } }.apply { isDaemon = true; start() }

        enviar(RecordingService.ACTION_STOP)
        assertTrue(aguardar { RecordingServiceState.state.value !is RecordingState.Recording && RecordingServiceState.state.value !is RecordingState.Stopping })
        aguardar({ false }, ms = 300)
        coleta.interrupt()

        assertTrue("estado final: ${RecordingServiceState.state.value}", RecordingServiceState.state.value is RecordingState.Error)
        assertFalse("emitiu Saved para caminho vazio: $salvos", salvos.any { it.isEmpty() })
        assertTrue(job == null)
    }

    @Test
    fun `dado STOP durante a preparacao quando o gravador termina de preparar entao nunca inicia nem deixa microfone aberto`() {
        conceder()
        proximo = { GravadorFalso(atrasoPrepare = true) }
        enviar(RecordingService.ACTION_START)
        assertTrue(aguardar { criados.size == 1 })

        enviar(RecordingService.ACTION_STOP)
        criados.single().liberarPrepare = true
        aguardar({ false }, ms = 800)

        val g = criados.single()
        assertEquals("gravador iniciou depois do STOP", 0, g.starts)
        assertTrue("gravador ficou sem release", g.releases >= 1)
    }
}
