package com.gravasom

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import com.gravasom.core.audio.android.recorder.MediaRecorderM4aRecorder
import com.gravasom.core.audio.android.recorder.PcmWavRecorder
import com.gravasom.core.domain.audio.AudioRecorder
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.service.MediaProbe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * Fase 3: duracao medida pelo MediaExtractor (a via do player) por formato real.
 * Tolerancia: +-100 ms (plano). A calibracao fica em docs/calibracao-duracao.md,
 * gerada a partir de medidas.csv do CI.
 */
class DuracaoMediaExtractorTest {

    @get:Rule
    val permissoes: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun wavSinteticoTemDuracaoExata() {
        val arquivo = Suporte.escreverWavSeno(File(Suporte.contexto.cacheDir, "sintetico_3s.wav"), 3.0)
        val duracao = MediaProbe.durationMillis(arquivo)
        assertNotNull("MediaExtractor nao abriu o WAV", duracao)
        Suporte.medida("wav_sintetico_3s", "delta_ms", duracao!! - 3000)
        assertTrue("delta=${duracao - 3000} ms", abs(duracao - 3000) <= TOLERANCIA_MS)
    }

    @Test
    fun gravacaoRealWav() = gravarEMedir(RecordingFormat.WAV, "wav")

    @Test
    fun gravacaoRealM4a() = gravarEMedir(RecordingFormat.M4A, "m4a")

    private fun gravarEMedir(formato: RecordingFormat, extensao: String) = runBlocking {
        val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val gravador: AudioRecorder = when (formato) {
            RecordingFormat.WAV -> PcmWavRecorder(escopo)
            else -> MediaRecorderM4aRecorder()
        }
        val arquivo = File(Suporte.testsOut(), "real_${System.currentTimeMillis()}.$extensao")
        try {
            gravador.prepare(RecordingConfig(format = formato), arquivo.path).getOrThrow()
            gravador.start().getOrThrow()
            val inicio = System.nanoTime()
            Thread.sleep(JANELA_MS)
            val relogioMs = (System.nanoTime() - inicio) / NANOS_POR_MS
            gravador.stop().getOrThrow()

            val duracao = MediaProbe.durationMillis(arquivo)
            assertNotNull("MediaExtractor nao abriu o $extensao", duracao)
            val delta = duracao!! - relogioMs
            Suporte.medida("real_$extensao", "relogio_ms", relogioMs)
            Suporte.medida("real_$extensao", "extractor_ms", duracao)
            Suporte.medida("real_$extensao", "delta_ms", delta)
            assertTrue("delta=$delta ms (extractor=$duracao, relogio=$relogioMs)", abs(delta) <= TOLERANCIA_MS)
        } finally {
            gravador.release()
            escopo.cancel()
        }
    }

    private companion object {
        const val TOLERANCIA_MS = 100L
        const val JANELA_MS = 4_000L
        const val NANOS_POR_MS = 1_000_000L
    }
}
