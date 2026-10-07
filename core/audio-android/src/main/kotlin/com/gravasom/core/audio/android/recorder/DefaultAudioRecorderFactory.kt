package com.gravasom.core.audio.android.recorder

import com.gravasom.core.domain.audio.AudioRecorder
import com.gravasom.core.domain.audio.AudioRecorderFactory
import com.gravasom.core.domain.audio.RecordingFormat
import kotlinx.coroutines.CoroutineScope

/** Fabrica real: escolhe a estrategia a partir do formato. */
class DefaultAudioRecorderFactory(
    private val scope: CoroutineScope,
    private val mp3Enabled: Boolean = false,
) : AudioRecorderFactory {

    override fun create(format: RecordingFormat): AudioRecorder = when (format) {
        RecordingFormat.M4A -> MediaRecorderM4aRecorder()
        RecordingFormat.WAV -> PcmWavRecorder(scope)
        RecordingFormat.MP3 -> if (mp3Enabled) PcmMp3Recorder() else PcmWavRecorder(scope)
    }
}
