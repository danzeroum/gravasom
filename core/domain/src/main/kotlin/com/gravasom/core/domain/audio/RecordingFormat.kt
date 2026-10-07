package com.gravasom.core.domain.audio

/** Formatos suportados pelo gravador. */
enum class RecordingFormat(val extension: String, val mimeType: String) {
    M4A("m4a", "audio/mp4"),
    WAV("wav", "audio/wav"),
    MP3("mp3", "audio/mpeg"),
}
