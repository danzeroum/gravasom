package com.gravasom

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gravasom.core.data.repository.RoomTranscriptRepository
import com.gravasom.core.domain.transcript.TranscriptStatus
import com.gravasom.core.domain.transcript.TranscriptionLang
import com.gravasom.feature.transcribe.convert.AudioConverter
import com.gravasom.feature.transcribe.model.AndroidModelManager
import com.gravasom.feature.transcribe.model.WhisperCppTranscriber
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Uso real da transcricao no APARELHO (nao roda em JVM). Precisa de:
 *
 * 1. Um WAV de fala em pt-BR colocado em `filesDir/transcricao-teste.wav` (o proprio teste
 *    pula se nao existir; nao inventa audio).
 * 2. O modelo baixado em `filesDir/modelos/pt-BR/modelo.bin` (o AndroidModelManager verifica
 *    o SHA-256; sem o modelo, o teste pula).
 *
 * O que prova:
 * - o modelo e reconhecido como pronto (SHA-256 bate);
 * - a conversao WAV -> PCM 16 kHz funciona no aparelho;
 * - a transcricao devolve texto nao vazio e a linha vira DONE no Room;
 * - o tempo total e a memoria ficam registrados em medidas.csv (para o relatorio da Fase 5).
 *
 * Rodar: `./gradlew -PwhisperSrc=... :app:connectedDebugAndroidTest --tests '*TranscricaoTest*'`
 */
@RunWith(AndroidJUnit4::class)
class TranscricaoTest {

    private val contexto = Suporte.contexto

    private fun wavDeTeste(): File = File(contexto.filesDir, "transcricao-teste.wav")

    private fun modeloPtBr(): File = File(contexto.filesDir, "modelos/pt-BR/modelo.bin")

    @Test
    fun modeloAusenteEhReportadoComoSemModelo() = runBlocking {
        val manager = AndroidModelManager(contexto)
        val estado = manager.state(TranscriptionLang.PT_BR)
        // Nao assume o que esta no aparelho: so confere que a resposta e coerente com o disco.
        if (modeloPtBr().exists()) {
            assertTrue("modelo presente mas nao verificado", manager.verify(TranscriptionLang.PT_BR))
        } else {
            assertTrue("sem modelo, o estado deveria ser Missing", estado is com.gravasom.core.domain.transcript.ModelState.Missing)
        }
    }

    @Test
    fun fluxoCompletoGravaTranscreveELe() = runBlocking {
        val wav = wavDeTeste()
        assumeTrue("coloque um WAV de fala pt-BR em ${wav.path} para rodar este teste", wav.exists())
        val modelo = modeloPtBr()
        assumeTrue("coloque o modelo em ${modelo.path} para rodar este teste", modelo.exists())

        // 1. Converte o WAV para PCM 16 kHz (o que o worker faz).
        val pcm = File(contexto.cacheDir, "transcricao-teste.pcm")
        val t0 = System.currentTimeMillis()
        val duracaoMs = AudioConverter().converter(wav, pcm)
        val msConversao = System.currentTimeMillis() - t0
        assertTrue("PCM vazio", pcm.length() > 0)

        // 2. Transcreve de verdade (whisper.cpp no aparelho).
        val manager = AndroidModelManager(contexto)
        val transcriber = WhisperCppTranscriber(manager)
        val t1 = System.currentTimeMillis()
        val resultado = transcriber.transcribe(pcm, TranscriptionLang.PT_BR, timestamps = true)
        val msTranscricao = System.currentTimeMillis() - t1
        assertTrue("texto vazio", resultado.text.isNotBlank())

        // 3. Grava no Room (o worker faria isto) e le de volta.
        val db = com.gravasom.core.data.db.GravasomDatabase.create(contexto)
        val repo = RoomTranscriptRepository(db.transcriptDao())
        val recId = db.recordingDao().insert(
            com.gravasom.core.data.db.RecordingEntity(
                title = "teste", path = wav.path, format = "WAV", durationMillis = duracaoMs,
                sizeBytes = wav.length(), createdAtMillis = System.currentTimeMillis(),
            ),
        )
        repo.upsert(
            com.gravasom.core.domain.transcript.Transcript(
                recordingId = recId, language = TranscriptionLang.PT_BR,
                format = com.gravasom.core.domain.transcript.TranscriptFormat.PLAIN,
                status = TranscriptStatus.RUNNING, createdAtMillis = 1, updatedAtMillis = 1,
            ),
        )
        repo.markDone(recId, TranscriptionLang.PT_BR, resultado.text)
        val lido = repo.find(recId, TranscriptionLang.PT_BR)!!
        assertEquals(TranscriptStatus.DONE, lido.status)
        assertEquals(resultado.text, lido.text)
        db.recordingDao().deleteForever(recId)
        db.close()

        // 4. Mede (artefato do relatorio de qualidade).
        Suporte.medida("TranscricaoTest", "duracao_audio_ms", duracaoMs)
        Suporte.medida("TranscricaoTest", "tempo_conversao_ms", msConversao)
        Suporte.medida("TranscricaoTest", "tempo_transcricao_ms", msTranscricao)
        Suporte.medida("TranscricaoTest", "rtf", "%.3f".format(msTranscricao.toDouble() / maxOf(duracaoMs, 1)))
        Suporte.medida("TranscricaoTest", "caracteres", resultado.text.length)
        pcm.delete()
    }
}
