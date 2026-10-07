package com.gravasom.core.domain.audio

/** Escolhe a estrategia de gravacao a partir do formato pedido. */
interface AudioRecorderFactory {
    fun create(format: RecordingFormat): AudioRecorder
}
