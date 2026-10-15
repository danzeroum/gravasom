package com.gravasom.core.service.clip

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.data.db.AnalysisStore
import com.gravasom.core.data.db.GravasomDatabase
import com.gravasom.core.data.metrics.UsageMetrics
import com.gravasom.core.data.preferences.PreferencesRepository
import com.gravasom.core.data.repository.RoomMarkerRepository
import com.gravasom.core.data.repository.RoomRecordingRepository
import com.gravasom.core.data.vault.VaultCrypto
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.analysis.RecordingAnalyzer
import com.gravasom.core.service.library.MediaStorePublisher
import com.gravasom.core.service.library.RecordingFiles
import com.gravasom.core.service.library.RecordingIngestor
import com.gravasom.core.service.library.VaultStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.RandomAccessFile

/**
 * "Dividir" e "juntar" de ponta a ponta (grafo quase de producao: Room em memoria local, ingestor de verdade): duas
 * gravacoes NOVAS no dividir, uma no juntar, e o ORIGINAL intacto bit a bit nos dois casos (criterio de aceite da Onda 2).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class SplitJoinServiceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val rate = 16_000

    private lateinit var repo: RecordingRepository
    private lateinit var db: GravasomDatabase
    private lateinit var splits: SplitService
    private lateinit var joins: JoinService
    private lateinit var clips: ClipService
    private lateinit var vault: VaultStore
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = GravasomDatabase.create(context)
        repo = RoomRecordingRepository(db.recordingDao())
        val prefs = PreferencesRepository(context)
        val metrics = UsageMetrics(db.usageEventDao(), prefs.telemetryOptIn)
        val publisher = MediaStorePublisher(context)
        vault = VaultStore(context, lazy { VaultCrypto(VaultCrypto.newStreamingAead()) })
        vault.clearTemp()
        val markers = RoomMarkerRepository(db.markerDao())
        val analyzer = RecordingAnalyzer(AnalysisStore(db.analysisDao()))
        val ingestor = RecordingIngestor(repo, prefs, metrics, publisher, vault, markers, analyzer)
        val files = RecordingFiles(context, repo, publisher, vault)
        splits = SplitService(context, repo, files, ingestor)
        joins = JoinService(context, repo, files, ingestor)
        clips = ClipService(context, repo, files, ingestor)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun wav(name: String, frames: Int, base: Int): File {
        val f = tmp.newFile(name)
        RandomAccessFile(f, "rw").use { raf ->
            raf.write(WavHeaderWriter.build(rate, 1, 16, frames * 2L))
            val pcm = ByteArray(frames * 2)
            for (i in 0 until frames) {
                val v = (base + i) % 256
                pcm[2 * i] = v.toByte()
                pcm[2 * i + 1] = 0
            }
            raf.write(pcm)
        }
        return f
    }

    private suspend fun gravacao(
        title: String,
        file: File,
        format: RecordingFormat = RecordingFormat.WAV,
        tags: List<String> = emptyList(),
    ): Recording {
        val r = Recording(
            id = 0, title = title, path = file.path, format = format,
            durationMillis = (file.length() - HEADER_BYTES) / 2 * 1_000 / rate, sizeBytes = file.length(),
            createdAtMillis = 1, tags = tags,
        )
        val id = repo.insert(r)
        return r.copy(id = id)
    }

    private fun data(f: File): ByteArray = f.inputStream().use { it.readBytes() }.copyOfRange(HEADER_BYTES, f.length().toInt())

    @Test
    fun `dividir cria as duas partes e o original fica intacto bit a bit`() = runBlocking {
        val origem = wav("aula.wav", 20_000, base = 3)
        val antes = origem.readBytes()
        val r = gravacao("Aula de física", origem, tags = listOf("aula"))
        val at = (r.durationMillis / 2).coerceAtLeast(201L)
        val outcome = splits.split(r, at)
        assertTrue(outcome.toString(), outcome is SplitOutcome.Saved)
        outcome as SplitOutcome.Saved
        assertEquals("Aula de física · parte 1", outcome.titles.first)
        assertEquals("Aula de física · parte 2", outcome.titles.second)
        val depois = repo.observeAll().first()
        assertEquals(3, depois.size) // original + 2 partes
        val parte1 = depois.first { it.id == outcome.firstId }
        val parte2 = depois.first { it.id == outcome.secondId }
        assertEquals(listOf("aula"), parte1.tags) // etiquetas herdam
        assertEquals(listOf("aula"), parte2.tags)
        assertArrayEquals(antes, origem.readBytes()) // criterio de aceite: original intacto
        // A parte 1 cobre [0, at) e a parte 2 [at, fim): o PCM junto volta ao original.
        assertArrayEquals(data(origem), data(File(parte1.path)) + data(File(parte2.path)))
    }

    @Test
    fun `dividir perto demais do fim recusa`() = runBlocking {
        val origem = wav("curta.wav", rate / 2, base = 0) // 0,5 s
        val r = gravacao("Curta", origem)
        val outcome = splits.split(r, r.durationMillis - 1)
        assertTrue(outcome.toString(), outcome is SplitOutcome.Failed)
        assertEquals(1, repo.observeAll().first().size) // nada criado
    }

    @Test
    fun `juntar cria uma gravacao nova na ordem e os originais ficam intactos`() = runBlocking {
        val a = wav("a.wav", rate * 2, base = 10)
        val b = wav("b.wav", rate, base = 200)
        val antesA = a.readBytes()
        val antesB = b.readBytes()
        val ra = gravacao("Entrevista", a)
        val rb = gravacao("Ideia", b)
        val outcome = joins.join(listOf(ra, rb))
        assertTrue(outcome.toString(), outcome is JoinOutcome.Saved)
        outcome as JoinOutcome.Saved
        assertEquals("Entrevista e Ideia", outcome.title)
        val depois = repo.observeAll().first()
        assertEquals(3, depois.size) // 2 originais + juncao
        val juncao = depois.first { it.id == outcome.recordingId }
        assertArrayEquals(data(a) + data(b), data(File(juncao.path))) // PCM concatenado na ordem
        assertArrayEquals(antesA, a.readBytes())
        assertArrayEquals(antesB, b.readBytes())
    }

    @Test
    fun `juntar gravacoes de formatos diferentes recusa`() = runBlocking {
        val a = wav("a.wav", rate, base = 0)
        val ra = gravacao("WAV", a)
        val rb = gravacao("M4A", a, format = RecordingFormat.M4A) // mesma taxa de bytes: so a mistura de formato interessa
        val outcome = joins.join(listOf(ra, rb))
        assertTrue(outcome.toString(), outcome is JoinOutcome.Failed)
        assertEquals(2, repo.observeAll().first().size)
    }

    // ---- Cofre: Clip, Join e Split usam copia em claro de uso unico e nao deixam nada no cache. ----

    /** Gravacao PRIVADA: o WAV claro vai para o cofre (cifrado) e a duracao e calculada antes. */
    private suspend fun gravacaoPrivada(title: String, plain: File): Recording {
        val duracao = (plain.length() - HEADER_BYTES) / 2 * 1_000 / rate
        val cifrado = vault.protect(plain)!!
        val r = Recording(
            id = 0, title = title, path = cifrado.path, format = RecordingFormat.WAV,
            durationMillis = duracao, sizeBytes = cifrado.length(), createdAtMillis = 1,
        )
        return r.copy(id = repo.insert(r))
    }

    private fun semClaroNoCache() {
        for (pasta in listOf("vault-play", "vault-op", "vault-share")) {
            val sobras = File(context.cacheDir, pasta).listFiles().orEmpty().toList()
            assertTrue("sobrou claro em $pasta: $sobras", sobras.isEmpty())
        }
    }

    /** PCM de uma gravacao do cofre, lido por uma copia de uso unico. */
    private fun pcmDoCofre(r: Recording): ByteArray = vault.openPlain(r).use { data(it.file) }

    @Test
    fun `dividir gravacao privada nao deixa copia em claro e as partes continuam no cofre`() = runBlocking {
        val origem = wav("priv_div.wav", 20_000, base = 5)
        val pcm = data(origem)
        val r = gravacaoPrivada("Segredo", origem)
        val outcome = splits.split(r, (r.durationMillis / 2).coerceAtLeast(201L))
        assertTrue(outcome.toString(), outcome is SplitOutcome.Saved)
        outcome as SplitOutcome.Saved
        val depois = repo.observeAll().first()
        val p1 = depois.first { it.id == outcome.firstId }
        val p2 = depois.first { it.id == outcome.secondId }
        assertTrue("parte 1 saiu do cofre", p1.isPrivate)
        assertTrue("parte 2 saiu do cofre", p2.isPrivate)
        assertArrayEquals(pcm, pcmDoCofre(p1) + pcmDoCofre(p2))
        semClaroNoCache()
    }

    @Test
    fun `juntar gravacoes privadas nao deixa copia em claro e a juncao fica no cofre`() = runBlocking {
        val a = wav("priv_a.wav", rate * 2, base = 10)
        val b = wav("priv_b.wav", rate, base = 200)
        val pcmA = data(a)
        val pcmB = data(b)
        val ra = gravacaoPrivada("A", a)
        val rb = gravacaoPrivada("B", b)
        val outcome = joins.join(listOf(ra, rb))
        assertTrue(outcome.toString(), outcome is JoinOutcome.Saved)
        outcome as JoinOutcome.Saved
        val juncao = repo.observeAll().first().first { it.id == outcome.recordingId }
        assertTrue("juncao saiu do cofre", juncao.isPrivate)
        assertArrayEquals(pcmA + pcmB, pcmDoCofre(juncao))
        semClaroNoCache()
    }

    @Test
    fun `exportar trecho de gravacao privada nao deixa copia em claro e o trecho fica no cofre`() = runBlocking {
        val origem = wav("priv_clip.wav", rate * 2, base = 1)
        val r = gravacaoPrivada("Reuniao", origem)
        val outcome = clips.export(r, startMillis = 500L, endMillis = 1_000L)
        assertTrue(outcome.toString(), outcome is ClipOutcome.Saved)
        outcome as ClipOutcome.Saved
        val trecho = repo.observeAll().first().first { it.id == outcome.recordingId }
        assertTrue("trecho saiu do cofre", trecho.isPrivate)
        assertEquals(rate / 2 * 2, pcmDoCofre(trecho).size) // 0,5 s de PCM16 mono
        semClaroNoCache()
    }

    @Test
    fun `dividir gravacao nao privada continua sem tocar no cache do cofre`() = runBlocking {
        val origem = wav("pub_div.wav", 20_000, base = 7)
        val r = gravacao("Publica", origem)
        splits.split(r, (r.durationMillis / 2).coerceAtLeast(201L))
        semClaroNoCache()
        assertTrue("o original nao pode ser apagado pela copia de uso unico", origem.exists())
    }

    private companion object {
        const val HEADER_BYTES = 44
    }
}
