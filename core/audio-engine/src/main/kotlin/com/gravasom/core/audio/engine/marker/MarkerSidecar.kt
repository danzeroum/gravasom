package com.gravasom.core.audio.engine.marker

import java.io.File

/** Marcador feito durante a gravacao, ainda sem gravacao no banco. */
data class PendingMarker(val positionMillis: Long, val label: String)

/**
 * Arquivo lateral `<audio>.markers` com um marcador por linha (`posicaoMs<TAB>rotulo`). Cada marcador e gravado no
 * ato: se o processo morrer no meio da gravacao, a recuperacao encontra os marcadores junto com o audio reparado.
 * Linhas incompletas ou invalidas sao ignoradas.
 */
class MarkerSidecar private constructor(val file: File) {

    @Synchronized
    fun append(marker: PendingMarker) {
        val label = marker.label.replace(Regex("[\\t\\r\\n]+"), " ").trim()
        file.parentFile?.mkdirs()
        file.appendText("${marker.positionMillis}\t$label\n")
    }

    fun read(): List<PendingMarker> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        val complete = text.substring(0, text.lastIndexOf('\n') + 1) // descarta a linha sem '\n' final (escrita interrompida)
        return complete.lineSequence().mapNotNull(::parse).toList()
    }

    fun delete() {
        file.delete()
    }

    private fun parse(line: String): PendingMarker? {
        if (line.isEmpty()) return null
        val tab = line.indexOf('\t')
        if (tab <= 0) return null
        val position = line.substring(0, tab).toLongOrNull()?.takeIf { it >= 0 } ?: return null
        return PendingMarker(position, line.substring(tab + 1))
    }

    companion object {
        fun forAudio(audio: File): MarkerSidecar = MarkerSidecar(File(audio.parentFile, audio.name + ".markers"))
    }
}
