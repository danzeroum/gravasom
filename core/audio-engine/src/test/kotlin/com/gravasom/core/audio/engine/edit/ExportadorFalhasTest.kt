package com.gravasom.core.audio.engine.edit

import com.gravasom.core.audio.engine.enhance.WavTeste
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** Falhas no meio do export: processadores nativos sempre fechados, sem `.tmp` sobrando e sem excecao vazando. */
class ExportadorFalhasTest {

    @TempDir
    lateinit var dir: File

    private val projeto = EditProject(steps = listOf(EditStep(StepType.HIGH_PASS, listOf(80f))))

    private class Fechavel(val id: Int) : AudioBlockProcessor {
        var fechado = false
        override val latencySamples = 0
        override fun process(block: FloatArray, length: Int) = Unit
        override fun close() { fechado = true }
    }

    private fun estereo(nome: String, taxa: Int = 8_000): File =
        WavTeste.escrever(File(dir, nome), taxa, FloatArray(taxa / 10) { 0.1f }, FloatArray(taxa / 10) { -0.1f })

    @Test
    fun `dado falha ao criar o processador do segundo canal quando exportar entao fecha o primeiro e devolve Failed`() {
        val criados = mutableListOf<Fechavel>()
        val exportador = ProjectExporter { _, _ ->
            if (criados.size == 1) throw IllegalStateException("libgravasom_dsp indisponivel")
            Fechavel(criados.size).also { criados += it }
        }
        val destino = File(dir, "saida.wav")

        val resultado = exportador.export(estereo("in.wav"), projeto, destino)

        assertTrue(resultado is ExportResult.Failed, "esperava Failed, veio $resultado")
        assertTrue(criados.all { it.fechado }, "processador nativo vazado")
        assertFalse(File(destino.path + ".tmp").exists(), ".tmp nao foi apagado")
        assertFalse(destino.exists())
    }

    @Test
    fun `dado excecao de runtime durante o processamento quando exportar entao Failed e tudo fechado`() {
        val criados = mutableListOf<Fechavel>()
        val exportador = ProjectExporter { _, _ ->
            object : AudioBlockProcessor {
                val f = Fechavel(criados.size).also { criados += it }
                override val latencySamples = 0
                override fun process(block: FloatArray, length: Int) = throw IllegalStateException("falha no processamento nativo")
                override fun close() = f.close()
            }
        }
        val destino = File(dir, "saida2.wav")

        val resultado = exportador.export(estereo("in2.wav"), projeto, destino)

        assertTrue(resultado is ExportResult.Failed, "esperava Failed, veio $resultado")
        assertTrue(criados.all { it.fechado })
        assertFalse(File(destino.path + ".tmp").exists())
    }

    @Test
    fun `dado cancelamento no meio quando exportar entao Cancelled e sem tmp`() {
        val exportador = ProjectExporter { _, _ -> Fechavel(0) }
        val destino = File(dir, "saida3.wav")
        var chamadas = 0

        val resultado = exportador.export(estereo("in3.wav"), projeto, destino, cancelled = { ++chamadas > 3 })

        assertEquals(ExportResult.Cancelled, resultado)
        assertFalse(File(destino.path + ".tmp").exists())
        assertFalse(destino.exists())
    }

    @Test
    fun `dado original com taxa invalida quando exportar entao Failed rapido e nao laco infinito`() {
        // Cabecalho com 50 Hz: block = 50/100 = 0 e o laco nunca avancaria. O prazo evita travar a suite no estado atual.
        val b = java.nio.ByteBuffer.allocate(44 + 400).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        b.putInt(0x46464952).putInt(0).putInt(0x45564157).putInt(0x20746D66).putInt(16)
        b.putShort(1).putShort(1).putInt(50).putInt(100).putShort(2).putShort(16)
        b.putInt(0x61746164).putInt(400)
        val original = File(dir, "taxa50.wav").apply { writeBytes(b.array()) }
        val limite = System.nanoTime() + 3_000_000_000L

        val resultado = ProjectExporter { _, _ -> Fechavel(0) }
            .export(original, projeto, File(dir, "saida4.wav"), cancelled = { System.nanoTime() > limite })

        assertTrue(resultado is ExportResult.Failed, "taxa invalida deve falhar, nao travar/cancelar: $resultado")
    }
}
