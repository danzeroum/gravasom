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

    /** Controle positivo: um arquivo 300 ms mais curto TEM de ficar fora da tolerancia, senao ela nao discrimina. */
    @Test
    fun controlePositivoArquivoCortadoReprova() {
        val cortado = Suporte.escreverWavSeno(File(Suporte.contexto.cacheDir, "cortado_2_7s.wav"), 2.7)
        val duracao = MediaProbe.durationMillis(cortado)!!
        Suporte.medida("controle_positivo", "delta_ms", duracao - 3000)
        assertTrue("tolerancia nao pegou o corte de 300 ms (delta=${duracao - 3000})", abs(duracao - 3000) > TOLERANCIA_MS)
    }

    /** WAV e MP3 vem do AudioRecord: partida determinística, tolerancia rigida de 100 ms em toda repeticao. */
    @Test
    fun gravacaoRealWav() = repetir(RecordingFormat.WAV, "wav", REPETICOES) { deltas ->
        assertTrue("WAV fora de +-100 ms: $deltas", deltas.all { abs(it) <= TOLERANCIA_MS })
    }

    /**
     * M4A vem do MediaRecorder, cuja latencia de partida varia no emulador (medido: -4 a -383 ms).
     * Calibracao (docs/calibracao-duracao.md): mediana dentro de 100 ms e pior caso dentro de 500 ms.
     */
    @Test
    fun gravacaoRealM4a() = repetir(RecordingFormat.M4A, "m4a", REPETICOES) { deltas ->
        val mediana = deltas.map { abs(it) }.sorted()[deltas.size / 2]
        Suporte.medida("real_m4a", "mediana_abs_delta_ms", mediana)
        assertTrue("mediana |delta|=$mediana ms: $deltas", mediana <= TOLERANCIA_MS)
        assertTrue("pior caso fora de ${TOLERANCIA_PIOR_CASO_M4A_MS} ms: $deltas", deltas.all { abs(it) <= TOLERANCIA_PIOR_CASO_M4A_MS })
    }

    private fun repetir(formato: RecordingFormat, extensao: String, vezes: Int, verificar: (List<Long>) -> Unit) {
        val deltas = (1..vezes).map { gravarEMedir(formato, extensao, it) }
        Suporte.medida("real_$extensao", "deltas_ms", deltas.joinToString(","))
        verificar(deltas)
    }

    private fun gravarEMedir(formato: RecordingFormat, extensao: String, n: Int): Long = runBlocking {
        val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val gravador: AudioRecorder = when (formato) {
            RecordingFormat.WAV -> PcmWavRecorder(escopo)
            else -> MediaRecorderM4aRecorder()
        }
        val arquivo = File(Suporte.testsOut(), "real_${extensao}_$n.$extensao")
        try {
            gravador.prepare(RecordingConfig(format = formato), arquivo.path).getOrThrow()
            gravador.start().getOrThrow()
            val inicio = System.nanoTime()
            Thread.sleep(JANELA_MS)
            val relogioMs = (System.nanoTime() - inicio) / NANOS_POR_MS
            gravador.stop().getOrThrow()
            val duracao = MediaProbe.durationMillis(arquivo)
            assertNotNull("MediaExtractor nao abriu o $extensao", duracao)
            Suporte.medida("real_${extensao}_$n", "relogio_ms", relogioMs)
            Suporte.medida("real_${extensao}_$n", "extractor_ms", duracao!!)
            duracao - relogioMs
        } finally {
            gravador.release()
            escopo.cancel()
        }
    }

    private companion object {
        const val TOLERANCIA_MS = 100L
        const val TOLERANCIA_PIOR_CASO_M4A_MS = 500L
        const val REPETICOES = 5
        const val JANELA_MS = 3_000L
        const val NANOS_POR_MS = 1_000_000L
    }
}
