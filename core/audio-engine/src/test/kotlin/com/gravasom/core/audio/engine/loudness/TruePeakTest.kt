package com.gravasom.core.audio.engine.loudness

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI

class TruePeakTest {

    private fun dbtp(x: FloatArray, taxa: Int): Double {
        val m = TruePeakMeter(taxa)
        m.add(x, x.size)
        return m.dbtp()
    }

    @Test
    fun senoEmFsSobre4ComFase45GrausTemPicoVerdadeiroDe0dB() {
        // Amostras caem em +-0,7071: o pico de AMOSTRA e -3 dBFS, mas o pico real da forma de onda e 0 dBFS.
        val x = Sinais.seno(48_000, 1.0, 12_000.0, 0.0, fase = PI / 4)
        assertEquals(-3.0, Sinais.picoAmostraDb(x), 0.05)
        assertEquals(0.0, dbtp(x, 48_000), 0.1)
    }

    @Test
    fun senoLentoTemPicoVerdadeiroIgualAoDeAmostra() {
        val x = Sinais.seno(48_000, 1.0, 440.0, -6.0)
        assertEquals(-6.0, dbtp(x, 48_000), 0.05)
    }

    @Test
    fun picoVerdadeiroNuncaFicaAbaixoDoPicoDeAmostra() {
        val x = Sinais.fala(44_100, 5.0, -20.0)
        assertTrue(dbtp(x, 44_100) >= Sinais.picoAmostraDb(x) - 1e-6)
    }

    @Test
    fun bloquesDeQualquerTamanhoDaoOMesmoPico() {
        val x = Sinais.fala(48_000, 3.0, -20.0)
        val inteiro = dbtp(x, 48_000)
        val m = TruePeakMeter(48_000)
        var pos = 0
        while (pos < x.size) {
            val n = minOf(97, x.size - pos)
            m.add(x.copyOfRange(pos, pos + n), n)
            pos += n
        }
        assertEquals(inteiro, m.dbtp(), 1e-9)
    }

    @Test
    fun silencioTemPicoMuitoBaixo() {
        assertTrue(dbtp(Sinais.silencio(48_000, 1.0), 48_000) < -100.0)
    }
}
