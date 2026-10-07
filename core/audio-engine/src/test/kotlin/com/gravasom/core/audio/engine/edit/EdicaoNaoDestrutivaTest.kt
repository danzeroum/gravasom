package com.gravasom.core.audio.engine.edit

import com.gravasom.core.audio.engine.WavHeaderWriter
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.sin

class EdicaoNaoDestrutivaTest {

    @TempDir
    lateinit var dir: File

    private val taxa = 44_100

    /** Processador de teste: ganho 0,5 com atraso configuravel (look-ahead) para provar a compensacao. */
    private class GanhoComAtraso(override val latencySamples: Int, private val ganho: Float = 0.5f) : AudioBlockProcessor {
        private val fila = FloatArray(latencySamples)
        private var pos = 0
        override fun process(block: FloatArray, length: Int) {
            for (i in 0 until length) {
                val v = block[i] * ganho
                if (latencySamples == 0) {
                    block[i] = v
                } else {
                    block[i] = fila[pos]
                    fila[pos] = v
                    pos = (pos + 1) % latencySamples
                }
            }
        }
    }

    private fun exportador(latencia: Int = 0) = ProjectExporter { _, _ -> GanhoComAtraso(latencia) }

    private fun projeto(vararg passos: EditStep) = EditProject(steps = passos.toList())
    private val passo = EditStep(StepType.HIGH_PASS, listOf(80f))

    private fun sha256(f: File): String =
        MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) }

    private fun wavSeno(nome: String, segundos: Double, amostrasExtras: Int = 0): File {
        val n = (segundos * taxa).toInt() + amostrasExtras
        val f = File(dir, nome)
        RandomAccessFile(f, "rw").use { raf ->
            raf.write(WavHeaderWriter.build(taxa, 1, 16, n * 2L))
            val pcm = ByteArray(n * 2)
            for (i in 0 until n) {
                val v = (sin(2 * PI * 440 * i / taxa) * 0.4 * Short.MAX_VALUE).toInt()
                pcm[2 * i] = (v and 0xFF).toByte()
                pcm[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
            }
            raf.write(pcm)
        }
        return f
    }

    private fun amostrasDe(f: File): Int = ((f.length() - WavHeaderWriter.HEADER_SIZE) / 2).toInt()

    @Test
    fun `original tem o mesmo sha256 apos editar exportar desfazer e refazer`() {
        val original = wavSeno("orig.wav", 2.0)
        val antes = sha256(original)
        val hist = EditHistory()
        hist.apply(projeto(passo))
        val r1 = exportador().export(original, hist.current, File(dir, "e1.wav"))
        assertTrue(r1 is ExportResult.Done)
        hist.undo()
        hist.redo()
        exportador().export(original, hist.current, File(dir, "e2.wav"))
        assertEquals(antes, sha256(original))
    }

    @Test
    fun `bypass e intensidade zero exportam bytes identicos ao original`() {
        val original = wavSeno("orig.wav", 1.5)
        exportador().export(original, projeto(passo).copy(bypass = true), File(dir, "b.wav"))
        exportador().export(original, projeto(passo).copy(intensity = 0f), File(dir, "i0.wav"))
        exportador().export(original, EditProject(), File(dir, "vazio.wav"))
        for (nome in listOf("b.wav", "i0.wav", "vazio.wav")) assertArrayEquals(original.readBytes(), File(dir, nome).readBytes(), nome)
    }

    @Test
    fun `export com processamento muda o audio mas mantem a duracao em amostras mesmo com latencia`() {
        val original = wavSeno("orig.wav", 1.0, amostrasExtras = 37)
        for (latencia in listOf(0, 1, 480, 1333)) {
            val saida = File(dir, "l$latencia.wav")
            val r = exportador(latencia).export(original, projeto(passo), saida)
            assertTrue(r is ExportResult.Done)
            assertEquals(amostrasDe(original), amostrasDe(saida), "latencia=$latencia")
        }
        assertNotEquals(sha256(original), sha256(File(dir, "l0.wav")))
    }

    @Test
    fun `projeto reaberto da o mesmo hash de export`() {
        val original = wavSeno("orig.wav", 1.0)
        val proj = EditProject(listOf(passo, EditStep(StepType.HIGH_PASS, listOf(120f), enabled = false)), intensity = 0.8f)
        exportador().export(original, proj, File(dir, "a.wav"))
        val reaberto = EditCodec.decode(EditCodec.encode(proj))
        assertEquals(proj, reaberto)
        exportador().export(original, reaberto, File(dir, "b.wav"))
        assertEquals(sha256(File(dir, "a.wav")), sha256(File(dir, "b.wav")))
    }

    @Test
    fun `cancelamento no meio nao deixa arquivo nem temporario`() {
        val original = wavSeno("orig.wav", 3.0)
        val antes = sha256(original)
        var blocos = 0
        val destino = File(dir, "c.wav")
        val r = exportador().export(original, projeto(passo), destino, cancelled = { ++blocos > 20 })
        assertEquals(ExportResult.Cancelled, r)
        assertFalse(destino.exists())
        assertFalse(File(dir, "c.wav.tmp").exists())
        assertEquals(antes, sha256(original))
    }

    @Test
    fun `desfazer e refazer navegam no historico`() {
        val h = EditHistory()
        assertFalse(h.canUndo)
        val a = projeto(passo)
        val b = a.copy(intensity = 0.5f)
        h.apply(a)
        h.apply(b)
        assertEquals(b, h.current)
        assertEquals(a, h.undo())
        assertTrue(h.canRedo)
        assertEquals(b, h.redo())
        h.undo()
        h.apply(projeto())
        assertFalse(h.canRedo, "novo apply descarta o refazer")
    }

    @Test
    fun `codec rejeita versao desconhecida e texto invalido`() {
        val t = EditCodec.encode(projeto(passo))
        assertTrue(t.startsWith("v1"))
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) { EditCodec.decode("v99\n") }
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) { EditCodec.decode("lixo") }
    }

    @Test
    fun `duas horas de audio exportam em streaming sem estourar a memoria`() {
        // 8 kHz mono PCM16, 2 h = 115 MB; o teste roda com heap de 96 MB (build.gradle.kts), logo nao cabe inteiro.
        val taxaBaixa = 8_000
        val n = 2L * 3600 * taxaBaixa
        val original = File(dir, "duas_horas.wav")
        RandomAccessFile(original, "rw").use { raf ->
            raf.write(WavHeaderWriter.build(taxaBaixa, 1, 16, n * 2))
            val bloco = ByteArray(taxaBaixa * 2) { (it % 50).toByte() }
            repeat((n / taxaBaixa).toInt()) { raf.write(bloco) }
        }
        val saida = File(dir, "duas_horas_saida.wav")
        val r = exportador().export(original, projeto(passo), saida)
        assertTrue(r is ExportResult.Done)
        assertEquals(original.length(), saida.length())
    }
}
