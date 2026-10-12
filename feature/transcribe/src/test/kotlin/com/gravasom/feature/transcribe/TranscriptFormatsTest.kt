package com.gravasom.feature.transcribe

import com.gravasom.core.domain.transcript.TranscriptFormat
import com.gravasom.core.domain.transcript.TranscriptSegment
import com.gravasom.feature.transcribe.model.TranscriptFormats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptFormatsTest {

    private val segmentos = listOf(
        TranscriptSegment(0, 1_500, "Bom dia a todos."),
        TranscriptSegment(1_500, 3_000, "Vamos comecar."),
    )

    @Test
    fun `plain so texto, uma frase por linha`() {
        val saida = TranscriptFormats.render(TranscriptFormat.PLAIN, segmentos)
        assertEquals("Bom dia a todos.\nVamos comecar.\n", saida)
    }

    @Test
    fun `vtt tem cabecalho e timestamps`() {
        val saida = TranscriptFormats.render(TranscriptFormat.VTT, segmentos)
        assertTrue(saida.startsWith("WEBVTT\n\n"))
        assertTrue(saida.contains("00:01.500 --> 00:03.000"))
        assertTrue(saida.contains("Vamos comecar."))
    }

    @Test
    fun `srt sempre tem horas`() {
        val saida = TranscriptFormats.render(TranscriptFormat.SRT, segmentos)
        assertTrue(saida.startsWith("1\n00:00:00,000 --> 00:00:01,500"))
        assertTrue(saida.contains("Bom dia a todos."))
    }

    @Test
    fun `vtt usa horas quando passa de 1h`() {
        val longo = listOf(TranscriptSegment(3_700_000, 3_705_000, "muito depois"))
        val saida = TranscriptFormats.render(TranscriptFormat.VTT, longo)
        assertTrue(saida.contains("01:01:40.000 --> 01:01:45.000"))
    }
}
