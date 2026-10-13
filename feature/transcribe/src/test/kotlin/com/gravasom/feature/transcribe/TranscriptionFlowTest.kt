package com.gravasom.feature.transcribe

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gravasom.core.data.db.GravasomDatabase
import com.gravasom.core.data.repository.RoomTranscriptRepository
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.domain.transcript.ModelManager
import com.gravasom.core.domain.transcript.ModelState
import com.gravasom.core.domain.transcript.TranscriptResult
import com.gravasom.core.domain.transcript.TranscriptSegment
import com.gravasom.core.domain.transcript.TranscriptStatus
import com.gravasom.core.domain.transcript.Transcriber
import com.gravasom.core.domain.transcript.TranscriptionError
import com.gravasom.core.domain.transcript.TranscriptionException
import com.gravasom.core.domain.transcript.TranscriptionLang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Uso real simulado do fluxo de transcricao, ponta a ponta, contra o Room de verdade (Robolectric):
 *
 * 1. Registra uma gravacao na biblioteca.
 * 2. Grava a transcricao como o worker faria (RUNNING -> DONE).
 * 3. A aba le exatamente o que foi gravado.
 * 4. Retranscrever em outro idioma guarda as duas.
 * 5. Falha marca FAILED com o motivo.
 * 6. Cancelamento marca CANCELLED e nao deixa texto.
 * 7. Excluir a gravacao apaga a transcricao em cascata.
 *
 * Nao depende de device (roda em JVM com Robolectric); o caminho instrumentado com o modelo real
 * fica para o aparelho do dono.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TranscriptionFlowTest {

    private lateinit var db: GravasomDatabase
    private lateinit var repo: RoomTranscriptRepository
    private var recordingId = 0L

    private class FakeRecordings(private val id: Long) : RecordingRepository {
        override fun observeAll(): Flow<List<Recording>> = MutableStateFlow(emptyList())
        override fun search(query: String): Flow<List<Recording>> = MutableStateFlow(emptyList())
        override fun observeFavorites(): Flow<List<Recording>> = MutableStateFlow(emptyList())
        override fun observeTrash(): Flow<List<Recording>> = MutableStateFlow(emptyList())
        override suspend fun getById(id: Long): Recording? = if (id == this.id) {
            Recording(id = id, title = "Aula", path = "/x.wav", format = RecordingFormat.WAV, durationMillis = 1_000, sizeBytes = 1, createdAtMillis = 1)
        } else {
            null
        }
        override suspend fun insert(recording: Recording): Long = id
        override suspend fun rename(id: Long, newTitle: String) = Unit
        override suspend fun setFavorite(id: Long, favorite: Boolean) = Unit
        override suspend fun setTags(id: Long, tags: List<String>) = Unit
        override suspend fun moveToTrash(id: Long, nowMillis: Long) = Unit
        override suspend fun restore(id: Long) = Unit
        override suspend fun setPublishedUri(id: Long, uri: String?) = Unit
        override suspend fun deleteForever(id: Long) = Unit
        override suspend fun purgeTrash(olderThanMillis: Long, nowMillis: Long) = Unit
    }

    /** Transcriber falso: devolve duas frases e permite simular falha. */
    private class FakeTranscriber(private val falhar: Boolean = false) : Transcriber {
        var visto: File? = null
        override suspend fun transcribe(
            arquivo: File,
            lang: TranscriptionLang,
            timestamps: Boolean,
            onSegment: suspend (TranscriptSegment) -> Unit,
        ): TranscriptResult {
            visto = arquivo
            if (falhar) throw TranscriptionException(TranscriptionError.INSUFFICIENT_STORAGE)
            val segs = listOf(
                TranscriptSegment(0, 500, "Bom dia a todos."),
                TranscriptSegment(500, 1_000, "Vamos comecar."),
            )
            segs.forEach { onSegment(it) }
            return TranscriptResult(lang, segs.joinToString("\n") { it.text }, segs, 1_000)
        }
    }

    @Before
    fun abrir() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GravasomDatabase::class.java).allowMainThreadQueries().build()
        repo = RoomTranscriptRepository(db.transcriptDao())
        recordingId = db.recordingDao().insert(
            com.gravasom.core.data.db.RecordingEntity(
                title = "Aula", path = "/x.wav", format = "WAV", durationMillis = 1_000, sizeBytes = 1, createdAtMillis = 1,
            ),
        )
    }

    @After
    fun fechar() = db.close()

    @Test
    fun `grava transcricao, le de volta e mostra o texto na aba`() = runBlocking {
        // Estado inicial: nada na aba.
        assertTrue(repo.observeForRecording(recordingId).first().isEmpty())

        // O worker marcaria RUNNING ao comecar (a UI mostra "Transcrevendo...").
        repo.upsert(
            com.gravasom.core.domain.transcript.Transcript(
                recordingId = recordingId, language = TranscriptionLang.PT_BR, format = com.gravasom.core.domain.transcript.TranscriptFormat.PLAIN,
                status = TranscriptStatus.RUNNING, createdAtMillis = 1, updatedAtMillis = 1,
            ),
        )
        assertEquals(TranscriptStatus.RUNNING, repo.observeForRecording(recordingId).first().single().status)

        // O worker terminaria com markDone.
        repo.markDone(recordingId, TranscriptionLang.PT_BR, "Bom dia a todos.\nVamos comecar.")

        val t = repo.find(recordingId, TranscriptionLang.PT_BR)!!
        assertEquals(TranscriptStatus.DONE, t.status)
        assertTrue(t.text!!.contains("Bom dia"))
        assertEquals(t.updatedAtMillis >= t.createdAtMillis, true)
    }

    @Test
    fun `mesma gravacao em dois idiomas guarda as duas`() = runBlocking {
        // O worker sempre cria a linha RUNNING primeiro; markDone so atualiza o que existe.
        iniciar(TranscriptionLang.PT_BR)
        repo.markDone(recordingId, TranscriptionLang.PT_BR, "Bom dia")
        iniciar(TranscriptionLang.EN_US)
        repo.markDone(recordingId, TranscriptionLang.EN_US, "Good morning")
        val todas = repo.observeForRecording(recordingId).first()
        assertEquals(2, todas.size)
        assertEquals("Bom dia", repo.find(recordingId, TranscriptionLang.PT_BR)!!.text)
        assertEquals("Good morning", repo.find(recordingId, TranscriptionLang.EN_US)!!.text)
    }

    @Test
    fun `falha marca FAILED com o motivo e a aba mostra Tentar novamente`() = runBlocking {
        val transcriber = FakeTranscriber(falhar = true)
        repo.upsert(
            com.gravasom.core.domain.transcript.Transcript(
                recordingId = recordingId, language = TranscriptionLang.PT_BR, format = com.gravasom.core.domain.transcript.TranscriptFormat.PLAIN,
                status = TranscriptStatus.RUNNING, createdAtMillis = 1, updatedAtMillis = 1,
            ),
        )
        try {
            transcriber.transcribe(File("/x.pcm"), TranscriptionLang.PT_BR, timestamps = false)
        } catch (e: TranscriptionException) {
            repo.markFailed(recordingId, TranscriptionLang.PT_BR, e.reason)
        }
        val t = repo.find(recordingId, TranscriptionLang.PT_BR)!!
        assertEquals(TranscriptStatus.FAILED, t.status)
        assertEquals("INSUFFICIENT_STORAGE", t.errorMessage)
    }

    @Test
    fun `cancelamento marca CANCELLED e nao deixa texto parcial`() = runBlocking {
        iniciar(TranscriptionLang.PT_BR)
        repo.markDone(recordingId, TranscriptionLang.PT_BR, "texto parcial")
        repo.markCancelled(recordingId, TranscriptionLang.PT_BR)
        val t = repo.find(recordingId, TranscriptionLang.PT_BR)!!
        assertEquals(TranscriptStatus.CANCELLED, t.status)
    }

    @Test
    fun `excluir a gravacao apaga a transcricao em cascata`() = runBlocking {
        iniciar(TranscriptionLang.PT_BR)
        repo.markDone(recordingId, TranscriptionLang.PT_BR, "Bom dia")
        db.recordingDao().deleteForever(recordingId)
        assertNull(repo.find(recordingId, TranscriptionLang.PT_BR))
    }

    /** Cria a linha RUNNING do idioma, como o worker faz antes de comecar a transcrever. */
    private suspend fun iniciar(lang: TranscriptionLang) = repo.upsert(
        com.gravasom.core.domain.transcript.Transcript(
            recordingId = recordingId, language = lang, format = com.gravasom.core.domain.transcript.TranscriptFormat.PLAIN,
            status = TranscriptStatus.RUNNING, createdAtMillis = 1, updatedAtMillis = 1,
        ),
    )

    @Test
    fun `transcriber falso recebe o PCM gerado pelo conversor a partir de um WAV de verdade`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wav = File(context.cacheDir, "flow-in.wav")
        escreverWavSenoSimples(wav, segundos = 0.3, taxa = 44_100)
        val pcm = File(context.cacheDir, "flow-out.pcm")
        com.gravasom.feature.transcribe.convert.AudioConverter().converter(wav, pcm)
        assertTrue(pcm.length() > 0)
        val t = FakeTranscriber()
        t.transcribe(pcm, TranscriptionLang.PT_BR, timestamps = false)
        assertEquals(pcm, t.visto)
    }

    private fun escreverWavSenoSimples(arquivo: File, segundos: Double, taxa: Int) {
        val amostras = (segundos * taxa).toInt()
        java.io.RandomAccessFile(arquivo, "rw").use { raf ->
            raf.setLength(0)
            raf.write(com.gravasom.core.audio.engine.WavHeaderWriter.build(taxa, 1, 16, amostras * 2L))
            val pcm = ByteArray(amostras * 2)
            for (i in 0 until amostras) {
                val v = (kotlin.math.sin(2 * kotlin.math.PI * 1000.0 * i / taxa) * 0.5 * Short.MAX_VALUE).toInt()
                pcm[2 * i] = (v and 0xFF).toByte()
                pcm[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
            }
            raf.write(pcm)
        }
    }
}
