package com.gravasom.core.audio.engine.loudness

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Medidor de loudness integrada (ITU-R BS.1770-4: K-weighting + gate absoluto e relativo). */
class LoudnessTest {

    private fun lufs(x: FloatArray, taxa: Int): Double? {
        val m = LoudnessMeter(taxa)
        m.add(0, x, x.size)
        return m.integratedLufs()
    }

    @Test
    fun senoMonoDe1kHzTemLoudnessDoNivelMenos3dB() {
        // BS.1770: seno de 997 Hz a -23 dBFS em DOIS canais = -23 LUFS; em UM canal, metade da energia = -26,01 LUFS.
        val l = lufs(Sinais.seno(48_000, 20.0, 997.0, -23.0), 48_000)
        assertEquals(-26.01, l!!, 0.05)
    }

    @Test
    fun doisCanaisIguaisSomamEnergia() {
        val x = Sinais.seno(48_000, 20.0, 997.0, -23.0)
        val m = LoudnessMeter(48_000, channels = 2)
        m.add(0, x, x.size)
        m.add(1, x, x.size)
        assertEquals(-23.0, m.integratedLufs()!!, 0.05)
    }

    @Test
    fun aMedidaNaoDependeDoTamanhoDosBlocosDeEntrada() {
        val x = Sinais.fala(44_100, 12.0, -30.0)
        val inteiro = lufs(x, 44_100)!!
        val m = LoudnessMeter(44_100)
        var pos = 0
        val tamanhos = intArrayOf(441, 1, 4410, 777, 100_000)
        var k = 0
        while (pos < x.size) {
            val n = minOf(tamanhos[k++ % tamanhos.size], x.size - pos)
            m.add(0, x.copyOfRange(pos, pos + n), n)
            pos += n
        }
        assertEquals(inteiro, m.integratedLufs()!!, 1e-9)
    }

    @Test
    fun silencioFicaAbaixoDoGateAbsolutoENaoTemLoudness() {
        assertNull(lufs(Sinais.silencio(48_000, 5.0), 48_000))
        assertNull(lufs(Sinais.seno(48_000, 5.0, 997.0, -90.0), 48_000))
    }

    @Test
    fun gateRelativoIgnoraTrechosMuitoMaisBaixosQueOResto() {
        val forte = Sinais.seno(48_000, 20.0, 997.0, -20.0)
        val baixo = Sinais.seno(48_000, 20.0, 997.0, -50.0) // 30 dB abaixo: fora do gate relativo (-10 LU)
        val soForte = lufs(forte, 48_000)!!
        val misto = lufs(Sinais.concat(forte, baixo), 48_000)!!
        assertEquals(soForte, misto, 0.05)
    }

    @Test
    fun trechosSoUmPoucoMaisBaixosEntramNaMedia() {
        val forte = Sinais.seno(48_000, 20.0, 997.0, -20.0)
        val meio = Sinais.seno(48_000, 20.0, 997.0, -26.0) // 6 dB abaixo: dentro do gate (-10 LU)
        val misto = lufs(Sinais.concat(forte, meio), 48_000)!!
        val soForte = lufs(forte, 48_000)!!
        assertTrue(misto < soForte - 0.5, "o trecho mais baixo precisa puxar a media para baixo ($misto vs $soForte)")
    }

    @Test
    fun filtroKReduzGravesDeFormaPrevista() {
        // Abaixo de ~100 Hz o filtro K atenua (RLB, passa-altas em 38 Hz): 30 Hz fica bem mais baixo que 1 kHz.
        val grave = lufs(Sinais.seno(48_000, 20.0, 30.0, -23.0), 48_000)!!
        val medio = lufs(Sinais.seno(48_000, 20.0, 1000.0, -23.0), 48_000)!!
        assertTrue(grave < medio - 3.0, "30 Hz = $grave, 1 kHz = $medio")
    }

    @Test
    fun funcionaEmTaxasBaixas() {
        for (taxa in intArrayOf(8_000, 16_000, 22_050, 44_100, 48_000)) {
            val l = lufs(Sinais.seno(taxa, 20.0, 997.0, -23.0), taxa)
            assertNotNull(l)
            assertEquals(-26.01, l!!, 0.1, "taxa $taxa")
        }
    }

    @Test
    fun kWeightingUsaOsMesmosCoeficientesDaNormaA48k() {
        // Tabela 1 e 2 da BS.1770-4 (48 kHz): estagio 1 (prefiltro) e estagio 2 (RLB).
        val (s1, s2) = KWeighting.stages(48_000)
        assertEquals(1.53512485958697, s1.b0, 1e-6)
        assertEquals(-2.69169618940638, s1.b1, 1e-6)
        assertEquals(1.19839281085285, s1.b2, 1e-6)
        assertEquals(-1.69065929318241, s1.a1, 1e-6)
        assertEquals(0.73248077421585, s1.a2, 1e-6)
        assertEquals(1.0, s2.b0, 1e-6)
        assertEquals(-2.0, s2.b1, 1e-6)
        assertEquals(1.0, s2.b2, 1e-6)
        assertEquals(-1.99004745483398, s2.a1, 1e-6)
        assertEquals(0.99007225036621, s2.a2, 1e-6)
    }
}
