package com.gravasom.core.service.enhance

import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.audio.engine.edit.AudioBlockProcessor
import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.audio.engine.edit.WavReader
import com.gravasom.core.audio.engine.loudness.LoudnessAnalyzer
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.preset.FormatPreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** "Melhorar som" no servico, com um host falso e um redutor falso (so atenua): previa A/B, ganho da gravacao inteira, exportar. */
class EnhanceServiceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val rate = 16_000

    private class Atenua(private val intensity: Float) : AudioBlockProcessor {
        override val latencySamples = 0
        override fun process(block: FloatArray, length: Int) {
            for (i in 0 until length) block[i] *= (1f - 0.5f * intensity)
        }
    }

    private inner class Host(private val file: File) : EnhanceHost {
        val saved = mutableListOf<Triple<File, String, Recording>>()
        val projects = mutableMapOf<Long, EditProject>()
        var failSave = false
        override suspend fun playableFile(recording: Recording) = file
        override suspend fun saveDerived(file: File, title: String, source: Recording): Long? {
            if (failSave) return null
            saved += Triple(file, title, source)
            return 99L
        }
        override suspend fun loadProject(recordingId: Long) = projects[recordingId]
        override suspend fun saveProject(recordingId: Long, project: EditProject) { projects[recordingId] = project }
        override val workDir = tmp.newFolder("work")
        override val outputDir = tmp.newFolder("saida")
    }

    /** Fala sintetica baixa (silabas a 3 Hz + pausas), mono, [seconds] s. */
    private fun wav(seconds: Int, amp: Double = 0.02): File {
        val n = rate * seconds
        val f = tmp.newFile("origem_${System.nanoTime()}.wav")
        RandomAccessFile(f, "rw").use { raf ->
            raf.write(WavHeaderWriter.build(rate, 1, 16, n * 2L))
            val pcm = ByteArray(n * 2)
            for (i in 0 until n) {
                val t = i.toDouble() / rate
                val syllable = abs(sin(2 * PI * 3.0 * t))
                val v = (amp * syllable * (sin(2 * PI * 180 * t) + 0.5 * sin(2 * PI * 900 * t)) * 32767).toInt().coerceIn(-32768, 32767)
                pcm[2 * i] = (v and 0xFF).toByte()
                pcm[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
            }
            raf.write(pcm)
        }
        return f
    }

    /** Host que entrega uma origem de uso unico e registra a devolucao (como o do cofre). */
    private inner class HostUsoUnico(private val file: File) : EnhanceHost {
        var released = 0
        override suspend fun playableFile(recording: Recording): File? = file
        override suspend fun openPlayable(recording: Recording) = PlayableSource(file) { released++ }
        override suspend fun saveDerived(file: File, title: String, source: Recording): Long? = 1L
        override suspend fun loadProject(recordingId: Long): EditProject? = null
        override suspend fun saveProject(recordingId: Long, project: EditProject) = Unit
        override val workDir = tmp.newFolder("work_uu")
        override val outputDir = tmp.newFolder("saida_uu")
    }

    private fun recordingPrivada() = recording().copy(path = "/data/user/0/app/files/vault/a.wav.gvs")

    private fun recording(preset: FormatPreset? = null) = Recording(
        id = 7, title = "Aula de fisica", path = "/x/a.wav", format = RecordingFormat.WAV, durationMillis = 60_000, sizeBytes = 1,
        createdAtMillis = 1, tags = listOf("aula"), preset = preset,
    )

    private fun service(host: EnhanceHost, pt: Boolean = true) = EnhanceService(host, { i, _ -> Atenua(i) }, portuguese = { pt })

    private fun settings(denoise: Boolean = false, normalize: Boolean = true) = EnhanceSettings(
        denoise = denoise, denoiseIntensity = 0.4f, normalize = normalize, targetLufs = -16f,
    )

    private fun lufs(f: File) = LoudnessAnalyzer.measure(f).lufs!!

    @Test
    fun `padroes vem do preset da gravacao e a configuracao salva vence`() = runBlocking {
        val host = Host(wav(2))
        val s = service(host)
        assertTrue(s.settingsFor(recording(FormatPreset.CLASS)).let { it.denoise && it.normalize })
        assertFalse(s.settingsFor(recording(FormatPreset.MUSIC)).let { it.denoise || it.normalize })
        assertFalse(s.settingsFor(recording(null)).let { it.denoise || it.normalize })
        s.saveSettings(recording(FormatPreset.MUSIC), settings(denoise = true, normalize = false).copy(denoiseIntensity = 0.9f))
        val reopened = s.settingsFor(recording(FormatPreset.MUSIC))
        assertTrue(reopened.denoise)
        assertFalse(reopened.normalize)
        assertEquals(0.9f, reopened.denoiseIntensity, 0.0f)
    }

    @Test
    fun `preparar mede a loudness da gravacao inteira e nao copia um WAV que ja serve`() = runBlocking {
        val source = wav(10)
        val host = Host(source)
        val p = service(host).prepare(recording())!!
        assertEquals(source, p.wav.file) // WAV PCM16: sem copia
        assertEquals(10_000L, p.wav.durationMillis)
        assertEquals(lufs(source), p.referenceLufs!!, 0.01)
    }

    @Test
    fun `a previa tem o mesmo trecho em A e B com a mesma duracao e B mais perto do alvo`() = runBlocking {
        val host = Host(wav(30))
        val s = service(host)
        val p = s.prepare(recording())!!
        val prev = s.preview(p, settings(), startMillis = 5_000, windowMillis = 10_000)!!
        assertEquals(10_000L, prev.durationMillis)
        assertEquals(WavReader.read(prev.original).frames, WavReader.read(prev.enhanced).frames)
        assertTrue("B (${lufs(prev.enhanced)}) deve estar mais perto de -16 que A (${lufs(prev.original)})",
            abs(lufs(prev.enhanced) + 16) < abs(lufs(prev.original) + 16))
    }

    @Test
    fun `o ganho da previa vem da gravacao inteira e nao do trecho`() = runBlocking {
        val host = Host(wav(30))
        val s = service(host)
        val p = s.prepare(recording())!!
        val a = s.preview(p, settings(), 0, 5_000)!!
        val b = s.preview(p, settings(), 20_000, 5_000)!!
        assertEquals(a.gainDb, b.gainDb, 0.0)
        assertEquals(-16.0 - p.referenceLufs!!, a.gainDb, 0.01)
    }

    @Test
    fun `a previa perto do fim recua para caber a janela inteira`() = runBlocking {
        val host = Host(wav(12))
        val s = service(host)
        val p = s.prepare(recording())!!
        val prev = s.preview(p, settings(), startMillis = 11_000, windowMillis = 8_000)!!
        assertEquals(8_000L, prev.durationMillis)
        assertEquals(4_000L, prev.startMillis)
    }

    @Test
    fun `sem normalizar a previa so aplica o redutor`() = runBlocking {
        val host = Host(wav(10))
        val s = service(host)
        val p = s.prepare(recording())!!
        val prev = s.preview(p, settings(denoise = true, normalize = false), 0, 5_000)!!
        assertEquals(0.0, prev.gainDb, 0.0)
        assertTrue(lufs(prev.enhanced) < lufs(prev.original) - 1.0)
    }

    @Test
    fun `exportar salva uma copia nova com o nome melhorada e guarda o projeto`() = runBlocking {
        val source = wav(20)
        val antes = source.readBytes()
        val host = Host(source)
        val s = service(host)
        val p = s.prepare(recording())!!
        val out = s.export(p, settings(normalize = true))
        assertTrue(out.toString(), out is EnhanceOutcome.Saved)
        out as EnhanceOutcome.Saved
        assertEquals("Aula de fisica (melhorada)", out.title)
        assertEquals(99L, out.recordingId)
        val (file, title, from) = host.saved.single()
        assertEquals(title, out.title)
        assertEquals(7L, from.id)
        assertEquals(-16.0, lufs(file), 1.0)
        assertArrayEquals(antes, source.readBytes()) // o original nao muda
        assertNotNull(host.projects[7L])
        assertTrue(host.workDir.resolve("export_7").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `o titulo sai em ingles quando o aparelho esta em ingles`() = runBlocking {
        val host = Host(wav(8))
        val out = service(host, pt = false).export(service(host).prepare(recording())!!, settings())
        assertEquals("Aula de fisica (enhanced)", (out as EnhanceOutcome.Saved).title)
    }

    @Test
    fun `sem nada ligado exportar recusa em vez de copiar`() = runBlocking {
        val host = Host(wav(5))
        val s = service(host)
        val out = s.export(s.prepare(recording())!!, settings(denoise = false, normalize = false))
        assertTrue(out is EnhanceOutcome.Failed)
        assertTrue(host.saved.isEmpty())
        // redutor a 0% tambem nao muda nada
        val zero = s.export(s.prepare(recording())!!, settings(denoise = true, normalize = false).copy(denoiseIntensity = 0f))
        assertTrue(zero is EnhanceOutcome.Failed)
    }

    @Test
    fun `cancelar nao salva nada e nao deixa arquivo`() = runBlocking {
        val host = Host(wav(30))
        val s = service(host)
        val p = s.prepare(recording())!!
        var n = 0
        val out = s.export(p, settings(), cancelled = { ++n > 20 })
        assertEquals(EnhanceOutcome.Cancelled, out)
        assertTrue(host.saved.isEmpty())
        assertTrue(host.outputDir.listFiles().orEmpty().none { it.extension == "wav" })
    }

    @Test
    fun `falha ao salvar na biblioteca apaga a copia e avisa`() = runBlocking {
        val host = Host(wav(8)).also { it.failSave = true }
        val s = service(host)
        val out = s.export(s.prepare(recording())!!, settings())
        assertTrue(out is EnhanceOutcome.Failed)
        assertTrue(host.outputDir.listFiles().orEmpty().isEmpty())
        assertNull(host.projects[7L])
    }

    @Test
    fun `audio que nao decodifica nao prepara`() = runBlocking {
        val lixo = tmp.newFile("lixo.m4a").apply { writeText("nao e audio") }
        assertNull(service(Host(lixo)).prepare(recording()))
    }

    @Test
    fun `fechar a sessao de gravacao privada devolve a origem e apaga previa e sobras do cache`() = runBlocking {
        val host = HostUsoUnico(wav(10))
        val s = service(host)
        val p = s.prepare(recordingPrivada())!!
        s.preview(p, settings(), startMillis = 0, windowMillis = 4_000)!!
        val previa = File(host.workDir, "previa")
        assertTrue("a previa deveria existir antes de fechar", previa.exists())
        assertEquals(0, host.released)

        p.close()

        assertEquals("origem de uso unico nao foi devolvida", 1, host.released)
        assertFalse("previa em claro ficou no cache", previa.exists())
        p.close() // idempotente
        assertEquals(1, host.released)
    }

    @Test
    fun `fechar a sessao de gravacao nao privada devolve a origem mas mantem o cache reaproveitavel`() = runBlocking {
        val host = HostUsoUnico(wav(10))
        val s = service(host)
        val p = s.prepare(recording())!!
        s.preview(p, settings(), startMillis = 0, windowMillis = 4_000)!!

        p.close()

        assertEquals(1, host.released)
        assertTrue("previa de gravacao publica nao precisa ser apagada", File(host.workDir, "previa").exists())
    }

    @Test
    fun `quando a origem nao decodifica entao a copia de uso unico e devolvida`() = runBlocking {
        val lixo = tmp.newFile("lixo.m4a").apply { writeText("nao e audio") }
        val host = HostUsoUnico(lixo)
        // Num teste JVM puro o MediaExtractor e um stub e pode lancar em vez de devolver null: nos dois casos a origem tem de voltar.
        val p = runCatching { service(host).prepare(recordingPrivada().copy(format = RecordingFormat.M4A)) }.getOrNull()
        assertNull(p)
        assertEquals("a origem de uso unico vazou quando a decodificacao falhou", 1, host.released)
    }
}
