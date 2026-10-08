package com.gravasom.core.audio.engine.enhance

import com.gravasom.core.audio.engine.edit.AudioBlockProcessor
import com.gravasom.core.audio.engine.edit.EditCodec
import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.audio.engine.edit.EditStep
import com.gravasom.core.audio.engine.edit.StepType
import com.gravasom.core.audio.engine.edit.WavReader
import com.gravasom.core.audio.engine.loudness.LoudnessAnalyzer
import com.gravasom.core.audio.engine.loudness.Sinais
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** "Melhorar som": reducao de ruido e normalizacao como efeitos nao destrutivos, em duas passagens e sem tocar no original. */
class EnhancePipelineTest {

    @TempDir
    lateinit var dir: File

    private val taxa = 44_100

    /** Redutor falso: atenua em 6 dB (prova a ordem das etapas e a mistura por intensidade, sem depender do DSP nativo). */
    private class AtenuaMetade(private val intensidade: Float) : AudioBlockProcessor {
        override val latencySamples = 0
        override fun process(block: FloatArray, length: Int) {
            for (i in 0 until length) block[i] = block[i] * (1f - 0.5f * intensidade)
        }
    }

    private var chamadasRedutor = 0

    private fun pipeline() = EnhancePipeline { intensidade, _ ->
        chamadasRedutor++
        AtenuaMetade(intensidade)
    }

    private fun original(rmsDbfs: Double = -34.0, segundos: Double = 20.0): File =
        WavTeste.escrever(File(dir, "orig.wav"), taxa, Sinais.fala(taxa, segundos, rmsDbfs))

    private fun normaliza(alvo: Float = -16f, teto: Float = -1f) = EditProject(listOf(EditStep(StepType.NORMALIZE, listOf(alvo, teto))))

    private fun medida(f: File) = LoudnessAnalyzer.measure(f)

    private fun rodar(
        original: File,
        projeto: EditProject,
        destino: File = File(dir, "saida.wav"),
        cancelled: () -> Boolean = { false },
    ) = pipeline().run(original, projeto, destino, workDir = File(dir, "work"), cancelled = cancelled)

    @Test
    fun melhorarVolumeDeMenos30ParaMenos16Mais_Menos1LUEPicoAbaixoDeMenos1dBTP() {
        val orig = original(rmsDbfs = -34.0)
        val antes = medida(orig).lufs!!
        assertTrue(antes < -28.0, "a gravacao de teste precisa estar baixa (estava em $antes LUFS)")
        val r = rodar(orig, normaliza())
        assertTrue(r is EnhanceResult.Done, r.toString())
        val saida = medida((r as EnhanceResult.Done).file)
        assertEquals(-16.0, saida.lufs!!, 1.0)
        assertTrue(saida.truePeakDbtp <= -1.0 + 0.01, "pico verdadeiro ${saida.truePeakDbtp}")
        assertEquals(-16.0, r.report.lufsAfter!!, 1.0)
    }

    @Test
    fun oOriginalNuncaMudaEADuracaoEmAmostrasEIgual() {
        val orig = original()
        val antes = WavTeste.sha256(orig)
        val r = rodar(orig, normaliza()) as EnhanceResult.Done
        assertEquals(antes, WavTeste.sha256(orig))
        assertEquals(WavReader.read(orig).frames, WavReader.read(r.file).frames)
    }

    @Test
    fun redutorAZeroPorCentoDaUmaCopiaIdenticaBitABit() {
        val orig = original()
        val projeto = EditProject(listOf(EditStep(StepType.NOISE_REDUCTION, listOf(0f))))
        val r = rodar(orig, projeto) as EnhanceResult.Done
        assertArrayEquals(orig.readBytes(), r.file.readBytes())
        assertEquals(0, chamadasRedutor, "com 0% o redutor nem deve ser criado")
    }

    @Test
    fun projetoSemEfeitosAtivosTambemCopiaIdentico() {
        val orig = original()
        val desligado = EditProject(listOf(EditStep(StepType.NORMALIZE, listOf(-16f, -1f), enabled = false)))
        assertArrayEquals(orig.readBytes(), (rodar(orig, desligado) as EnhanceResult.Done).file.readBytes())
        assertArrayEquals(orig.readBytes(), (rodar(orig, EditProject(), File(dir, "v.wav")) as EnhanceResult.Done).file.readBytes())
        assertArrayEquals(orig.readBytes(), (rodar(orig, normaliza().copy(bypass = true), File(dir, "b.wav")) as EnhanceResult.Done).file.readBytes())
    }

    @Test
    fun aNormalizacaoMedeDepoisDoRedutor() {
        // O redutor falso tira 6 dB (intensidade 100%); a normalizacao tem de compensar e ainda chegar a -16.
        val orig = original(rmsDbfs = -30.0)
        val projeto = EditProject(
            listOf(EditStep(StepType.NOISE_REDUCTION, listOf(1f)), EditStep(StepType.NORMALIZE, listOf(-16f, -1f))),
        )
        val r = rodar(orig, projeto) as EnhanceResult.Done
        assertEquals(1, chamadasRedutor)
        assertEquals(-16.0, medida(r.file).lufs!!, 1.0)
        assertTrue(r.report.gainDb > medida(orig).lufs!!.let { -16.0 - it }, "o ganho precisa incluir os 6 dB tirados pelo redutor")
    }

    @Test
    fun apenasRedutorSemNormalizacaoNaoAlteraOGanho() {
        val orig = original()
        val projeto = EditProject(listOf(EditStep(StepType.NOISE_REDUCTION, listOf(1f))))
        val r = rodar(orig, projeto) as EnhanceResult.Done
        assertEquals(medida(orig).lufs!! - 6.02, medida(r.file).lufs!!, 0.3)
        assertEquals(0.0, r.report.gainDb, 0.0)
    }

    @Test
    fun ganhoAltoLimitaOPicoSemEstourarOTeto() {
        // Fala baixa com picos curtos: +20 dB empurraria os picos muito acima de 0 dBFS sem o limitador.
        val fala = Sinais.fala(taxa, 15.0, -42.0)
        for (i in 7_000 until fala.size step 30_000) fala[i] = 0.05f
        val orig = WavTeste.escrever(File(dir, "picos.wav"), taxa, fala)
        val r = rodar(orig, normaliza()) as EnhanceResult.Done
        val saida = medida(r.file)
        assertTrue(saida.truePeakDbtp <= -1.0 + 0.01, "pico verdadeiro ${saida.truePeakDbtp}")
        assertEquals(-16.0, saida.lufs!!, 1.0)
    }

    @Test
    fun gravacaoSilenciosaNaoRecebeGanho() {
        val orig = WavTeste.escrever(File(dir, "mudo.wav"), taxa, Sinais.silencio(taxa, 5.0))
        val r = rodar(orig, normaliza()) as EnhanceResult.Done
        assertEquals(0.0, r.report.gainDb, 0.0)
        assertArrayEquals(orig.readBytes(), r.file.readBytes())
    }

    @Test
    fun estereoNormalizaPelaSomaDosCanaisComOMesmoGanho() {
        val a = Sinais.fala(taxa, 15.0, -36.0, semente = 1)
        val b = Sinais.fala(taxa, 15.0, -36.0, semente = 2)
        val orig = WavTeste.escrever(File(dir, "est.wav"), taxa, a, b)
        val r = rodar(orig, normaliza()) as EnhanceResult.Done
        val saida = medida(r.file)
        assertEquals(-16.0, saida.lufs!!, 1.0)
        assertTrue(saida.truePeakDbtp <= -1.0 + 0.01)
        assertEquals(2, WavReader.read(r.file).channels)
    }

    @Test
    fun cancelarNaoDeixaArquivoNemTemporario() {
        val orig = original(segundos = 30.0)
        var n = 0
        val destino = File(dir, "cancelada.wav")
        val r = rodar(orig, normaliza(), destino, cancelled = { ++n > 30 })
        assertEquals(EnhanceResult.Cancelled, r)
        assertFalse(destino.exists())
        assertFalse(File(destino.path + ".tmp").exists())
        assertTrue(File(dir, "work").listFiles().orEmpty().isEmpty(), "pasta de trabalho precisa ficar vazia")
    }

    @Test
    fun arquivoQueNaoEWavFalhaSemDeixarNada() {
        val lixo = File(dir, "x.wav").apply { writeText("isto nao e um wav") }
        val destino = File(dir, "y.wav")
        val r = rodar(lixo, normaliza(), destino)
        assertTrue(r is EnhanceResult.Failed)
        assertFalse(destino.exists())
        assertTrue(File(dir, "work").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun oProjetoDeEfeitosSobreviveAoCodecEDaOMesmoResultado() {
        val orig = original()
        val projeto = EditProject(
            listOf(EditStep(StepType.NOISE_REDUCTION, listOf(0.6f)), EditStep(StepType.NORMALIZE, listOf(-16f, -1f))),
        )
        val a = rodar(orig, projeto, File(dir, "a.wav")) as EnhanceResult.Done
        val b = rodar(orig, EditCodec.decode(EditCodec.encode(projeto)), File(dir, "b.wav")) as EnhanceResult.Done
        assertEquals(WavTeste.sha256(a.file), WavTeste.sha256(b.file))
    }

    @Test
    fun relatorioTrazOsNumerosDeAntesEDepois() {
        val orig = original()
        val r = rodar(orig, normaliza()) as EnhanceResult.Done
        assertEquals(medida(orig).lufs!!, r.report.lufsBefore!!, 0.01)
        assertTrue(r.report.gainDb > 10.0)
        assertTrue(r.report.passes in 1..4)
    }

    @Test
    fun ganhoFixoAplicaExatamenteEsseGanhoSemMedirOArquivo() {
        val orig = original(rmsDbfs = -34.0)
        val antes = medida(orig).lufs!!
        val r = pipeline().run(
            orig, normaliza(), File(dir, "fixo.wav"), File(dir, "work"), options = EnhanceOptions(fixedNormalizeGainDb = 6.0),
        ) as EnhanceResult.Done
        assertEquals(6.0, r.report.gainDb, 0.0)
        assertEquals(1, r.report.passes)
        assertEquals(antes + 6.0, medida(r.file).lufs!!, 0.1)
    }

    @Test
    fun ganhoFixoAindaRespeitaOTetoDePicoVerdadeiro() {
        val fala = Sinais.fala(taxa, 10.0, -30.0)
        for (i in 7_000 until fala.size step 30_000) fala[i] = 0.08f
        val orig = WavTeste.escrever(File(dir, "picos2.wav"), taxa, fala)
        val r = pipeline().run(
            orig, normaliza(), File(dir, "fixo2.wav"), File(dir, "work"), options = EnhanceOptions(fixedNormalizeGainDb = 24.0),
        ) as EnhanceResult.Done
        assertTrue(medida(r.file).truePeakDbtp <= -1.0 + 0.01, "pico ${medida(r.file).truePeakDbtp}")
    }
}
