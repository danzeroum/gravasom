package com.gravasom.feature.transcribe.model

import com.gravasom.core.domain.transcript.TranscriptFormat
import com.gravasom.core.domain.transcript.TranscriptSegment

/**
 * Serializa segmentos para os formatos de saida. VTT e SRT usam timestamps; PLAIN so o texto.
 * WebVTT nao tem horas quando o tempo e < 1 h; SRT sempre tem. A saida termina com quebra de linha.
 */
object TranscriptFormats {

    fun render(format: TranscriptFormat, segments: List<TranscriptSegment>): String = when (format) {
        TranscriptFormat.PLAIN -> segments.joinToString("\n") { it.text.trim() } + "\n"
        TranscriptFormat.VTT -> buildString {
            append("WEBVTT\n\n")
            segments.forEachIndexed { i, s ->
                append(i + 1).append('\n')
                append(vttTime(s.startMillis)).append(" --> ").append(vttTime(s.endMillis)).append('\n')
                append(s.text.trim()).append("\n\n")
            }
        }
        TranscriptFormat.SRT -> buildString {
            segments.forEachIndexed { i, s ->
                append(i + 1).append('\n')
                append(srtTime(s.startMillis)).append(" --> ").append(srtTime(s.endMillis)).append('\n')
                append(s.text.trim()).append("\n\n")
            }
        }
    }

    private fun vttTime(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        val s = (ms % 60_000) / 1000
        val frac = ms % 1000
        return if (h > 0) {
            "%02d:%02d:%02d.%03d".format(h, m, s, frac)
        } else {
            "%02d:%02d.%03d".format(m, s, frac)
        }
    }

    private fun srtTime(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        val s = (ms % 60_000) / 1000
        val frac = ms % 1000
        return "%02d:%02d:%02d,%03d".format(h, m, s, frac)
    }
}
