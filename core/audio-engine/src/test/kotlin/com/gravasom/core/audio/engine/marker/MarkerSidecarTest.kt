package com.gravasom.core.audio.engine.marker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MarkerSidecarTest {

    @TempDir
    lateinit var dir: File

    private fun audio() = File(dir, "gravacao_1.m4a")

    @Test
    fun `marcadores gravados voltam na mesma ordem`() {
        val sidecar = MarkerSidecar.forAudio(audio())
        sidecar.append(PendingMarker(12_500, "Pergunta"))
        sidecar.append(PendingMarker(60_000, ""))
        assertEquals(listOf(PendingMarker(12_500, "Pergunta"), PendingMarker(60_000, "")), sidecar.read())
    }

    @Test
    fun `rotulo com tab ou quebra de linha nao corrompe o arquivo`() {
        val sidecar = MarkerSidecar.forAudio(audio())
        sidecar.append(PendingMarker(1_000, "a\tb\nc"))
        sidecar.append(PendingMarker(2_000, "ok"))
        assertEquals(listOf(PendingMarker(1_000, "a b c"), PendingMarker(2_000, "ok")), sidecar.read())
    }

    @Test
    fun `linha cortada pelo fim abrupto do processo e ignorada, o resto fica`() {
        val sidecar = MarkerSidecar.forAudio(audio())
        sidecar.append(PendingMarker(1_000, "inteiro"))
        sidecar.file.appendText("lixo sem numero\n")
        sidecar.file.appendText("-7\tnegativo\n")
        sidecar.file.appendText("2000\tmeio") // sem quebra de linha final: escrita interrompida
        assertEquals(listOf(PendingMarker(1_000, "inteiro")), sidecar.read())
    }

    @Test
    fun `sem arquivo a lista e vazia e apagar e idempotente`() {
        val sidecar = MarkerSidecar.forAudio(audio())
        assertTrue(sidecar.read().isEmpty())
        sidecar.append(PendingMarker(1, "x"))
        sidecar.delete()
        sidecar.delete()
        assertFalse(sidecar.file.exists())
    }

    @Test
    fun `o arquivo lateral nao termina em tmp para nao ser confundido com gravacao interrompida`() {
        assertFalse(MarkerSidecar.forAudio(File(dir, "x.wav")).file.name.endsWith(".tmp"))
        assertEquals("x.wav.markers", MarkerSidecar.forAudio(File(dir, "x.wav")).file.name)
    }

    @Test
    fun `posicao negativa e descartada`() {
        val sidecar = MarkerSidecar.forAudio(audio())
        sidecar.append(PendingMarker(-5, "x"))
        assertTrue(sidecar.read().isEmpty())
    }
}
