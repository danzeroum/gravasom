package com.gravasom.core.audio.engine.loudness

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Estereo: o limitador de pico segue a MAIOR necessidade entre os canais e aplica o MESMO ganho aos dois.
 * Com um limitador por canal, so o canal que estoura era atenuado e a imagem estereo se deslocava durante o limite.
 */
class NormalizeEstereoLigadoTest {

    private val taxa = 48_000

    private fun seno(amp: Double, n: Int, freq: Double = 440.0) = FloatArray(n) { (amp * sin(2 * PI * freq * it / taxa)).toFloat() }

    private fun processarLigado(l: FloatArray, r: FloatArray, gainDb: Double, teto: Double = -1.0): Array<FloatArray> {
        val p = NormalizeProcessor(taxa, gainDb, teto, channels = 2)
        val lat = p.latencySamples
        val el = l + FloatArray(lat)
        val er = r + FloatArray(lat)
        val sl = FloatArray(el.size)
        val sr = FloatArray(er.size)
        var pos = 0
        while (pos < el.size) {
            val n = minOf(480, el.size - pos)
            val bl = el.copyOfRange(pos, pos + n)
            val br = er.copyOfRange(pos, pos + n)
            p.processLinked(arrayOf(bl, br), n)
            bl.copyInto(sl, pos)
            br.copyInto(sr, pos)
            pos += n
        }
        return arrayOf(sl.copyOfRange(lat, lat + l.size), sr.copyOfRange(lat, lat + r.size))
    }

    @Test
    fun `dado so um canal estourando o teto quando limita entao os dois recebem o mesmo ganho`() {
        val n = taxa * 2
        val l = seno(0.9, n)  // +6 dB estoura o teto de -1 dBTP: precisa ser limitado
        val r = seno(0.05, n) // +6 dB fica muito abaixo do teto: sozinho nao seria limitado
        val (yl, yr) = processarLigado(l, r, gainDb = 6.0)

        // compara o ganho efetivo (saida/entrada) nos mesmos instantes, longe de zero-crossings, na parte estavel
        var comparados = 0
        for (i in taxa / 2 until n - 1000) {
            if (abs(l[i]) > 0.5f && abs(r[i]) > 0.03f) {
                val gl = yl[i] / l[i]
                val gr = yr[i] / r[i]
                assertEquals(gl.toDouble(), gr.toDouble(), 1e-4, "ganho L=$gl R=$gr em $i")
                comparados++
            }
        }
        assertTrue(comparados > 1000, "poucas amostras comparadas: $comparados")
        // o limitador agiu de fato: sem ele o pico de L iria a 0,9 * 1,995 = 1,8; com teto de -1 dBTP fica em ~0,89
        val picoL = yl.drop(taxa / 2).maxOf { abs(it) }
        assertTrue(picoL < 0.9f, "pico de L apos limitar: $picoL")
    }

    @Test
    fun `dado canal unico quando usa o modo ligado entao e identico ao processamento normal`() {
        val x = seno(0.8, taxa)
        val normal = NormalizeProcessor(taxa, 6.0, -1.0)
        val ligado = NormalizeProcessor(taxa, 6.0, -1.0, channels = 1)
        val a = x.copyOf()
        val b = x.copyOf()
        normal.process(a, a.size)
        ligado.processLinked(arrayOf(b), b.size)
        assertArrayEquals(a, b)
    }

    @Test
    fun `dado sinal abaixo do teto quando aplica ganho entao nao limita nenhum canal`() {
        val n = taxa
        val l = seno(0.1, n)
        val r = seno(0.2, n)
        val (yl, yr) = processarLigado(l, r, gainDb = 6.0)
        val g = Math.pow(10.0, 6.0 / 20.0).toFloat()
        for (i in 2000 until n - 2000 step 97) {
            assertEquals(l[i] * g, yl[i], 1e-4f)
            assertEquals(r[i] * g, yr[i], 1e-4f)
        }
    }
}
