package com.gravasom.core.audio.engine.loudness

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs

/** "Melhorar volume": ganho para o alvo em LUFS com limitador de pico verdadeiro (<= -1 dBTP). */
class NormalizeProcessorTest {

    private val taxa = 48_000

    private fun lufs(x: FloatArray): Double = LoudnessMeter(taxa).also { it.add(0, x, x.size) }.integratedLufs()!!

    private fun dbtp(x: FloatArray): Double = TruePeakMeter(taxa).also { it.add(x, x.size) }.dbtp()

    /** Processa o sinal inteiro em blocos de [bloco] amostras e devolve a saida ja sem o atraso do look-ahead. */
    private fun processar(x: FloatArray, gainDb: Double, teto: Double = -1.0, bloco: Int = 480): FloatArray {
        val p = NormalizeProcessor(taxa, gainDb, teto)
        val entrada = x + FloatArray(p.latencySamples)
        val saida = FloatArray(entrada.size)
        var pos = 0
        while (pos < entrada.size) {
            val n = minOf(bloco, entrada.size - pos)
            val b = entrada.copyOfRange(pos, pos + n)
            p.process(b, n)
            b.copyInto(saida, pos)
            pos += n
        }
        return saida.copyOfRange(p.latencySamples, p.latencySamples + x.size)
    }

    @Test
    fun gravacaoDeMenos30LufsVaiParaMenos16Mais_Menos1LU() {
        val x = Sinais.fala(taxa, 30.0, -34.0)
        val antes = lufs(x)
        val plano = NormalizePlan.gainDb(antes, targetLufs = -16.0)
        val y = processar(x, plano)
        assertEquals(-16.0, lufs(y), 1.0, "antes: $antes, ganho: $plano")
        assertTrue(dbtp(y) <= -1.0 + 0.05, "pico verdadeiro ${dbtp(y)}")
    }

    @Test
    fun limitadorSegura_o_picoVerdadeiroNoTeto() {
        // Pulsos estreitos + fala: o ganho de +20 dB empurraria o pico para muito acima de 0 dBFS.
        val fala = Sinais.fala(taxa, 10.0, -35.0)
        val x = fala.copyOf().also { for (i in 5_000 until fala.size step 24_000) it[i] = 0.2f }
        val y = processar(x, gainDb = 20.0, teto = -1.0)
        assertTrue(dbtp(y) <= -1.0 + 0.05, "pico verdadeiro ${dbtp(y)} dBTP")
        assertTrue(Sinais.picoAmostraDb(y) <= -1.0 + 0.05)
    }

    @Test
    fun tetoMaisBaixoDaPicoMaisBaixo() {
        val x = Sinais.fala(taxa, 8.0, -30.0)
        val y = processar(x, gainDb = 18.0, teto = -3.0)
        assertTrue(dbtp(y) <= -3.0 + 0.05, "pico verdadeiro ${dbtp(y)}")
    }

    @Test
    fun semLimitacaoOGanhoEExatamenteOPedido() {
        val x = Sinais.fala(taxa, 8.0, -40.0)
        val y = processar(x, gainDb = 6.0)
        val k = Math.pow(10.0, 6.0 / 20.0).toFloat()
        for (i in x.indices) assertEquals(x[i] * k, y[i], 1e-6f)
    }

    @Test
    fun oResultadoNaoDependeDoTamanhoDoBloco() {
        val x = Sinais.fala(taxa, 6.0, -30.0)
        val a = processar(x, 16.0, bloco = 480)
        val b = processar(x, 16.0, bloco = 77)
        val c = processar(x, 16.0, bloco = 4096)
        assertArrayEquals(a, b)
        assertArrayEquals(a, c)
    }

    @Test
    fun aLatenciaDeclaradaEOAtrasoReal() {
        val p = NormalizeProcessor(taxa, 0.0)
        val x = FloatArray(2_000)
        x[100] = 0.1f // pulso baixo: sem limitacao, so atravessa a linha de atraso
        val entrada = x + FloatArray(p.latencySamples)
        p.process(entrada, entrada.size)
        val pos = entrada.indices.maxByOrNull { abs(entrada[it]) }!!
        assertEquals(100 + p.latencySamples, pos)
        assertTrue(p.latencySamples in 1..taxa / 100, "latencia razoavel (<= 10 ms): ${p.latencySamples}")
    }

    @Test
    fun saidaFicaFinitaEDentroDoTeto() {
        // Pior caso para o pico entre amostras: seno em fs/4 com fase de 45 graus (amostras em +-0,71 do pico real).
        val x = Sinais.seno(taxa, 2.0, 12_000.0, -1.0, fase = PI / 4)
        val y = processar(x, gainDb = 12.0)
        assertTrue(y.all { it.isFinite() })
        assertTrue(dbtp(y) <= -1.0 + 0.05, "pico ${dbtp(y)}")
    }

    @Test
    fun planoDeGanho() {
        assertEquals(14.0, NormalizePlan.gainDb(-30.0, -16.0), 1e-9)
        assertEquals(0.0, NormalizePlan.gainDb(null, -16.0), 0.0) // silencio: nada a fazer
        assertEquals(30.0, NormalizePlan.gainDb(-70.0, -16.0), 1e-9) // teto de +30 dB para nao amplificar so ruido
        assertEquals(-4.0, NormalizePlan.gainDb(-12.0, -16.0), 1e-9) // tambem abaixa o que esta alto demais
    }
}
