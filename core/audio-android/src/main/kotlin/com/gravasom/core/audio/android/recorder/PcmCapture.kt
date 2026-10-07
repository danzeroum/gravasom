package com.gravasom.core.audio.android.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.gravasom.core.audio.android.InputRouting
import com.gravasom.core.domain.audio.InputSource

/**
 * Captura PCM comum a WAV e MP3. Le em thread dedicada e entrega os buffers.
 * Nao escreve em disco: quem consome decide o formato.
 */
class PcmCapture(
    private val sampleRateHz: Int,
    private val channels: Int,
    private val inputSource: InputSource,
) {

    private var record: AudioRecord? = null

    val bufferSizeBytes: Int
        get() = AudioRecord.getMinBufferSize(sampleRateHz, channelMask(), encoding())
            .coerceAtLeast(MIN_BUFFER)

    /**
     * A permissao RECORD_AUDIO e verificada por quem inicia a captura (RecordingService antes do
     * primeiro plano; testes via GrantPermissionRule). Sem ela o AudioRecord falha ao iniciar.
     */
    @SuppressLint("MissingPermission")
    fun open(): AudioRecord {
        val size = bufferSizeBytes
        val r = AudioRecord.Builder()
            .setAudioSource(sourceFor(inputSource))
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(channelMask())
                    .setEncoding(encoding())
                    .build(),
            )
            .setBufferSizeInBytes(size * 2)
            .build()
        InputRouting.preferred?.let { r.setPreferredDevice(it) }
        record = r
        return r
    }

    fun close() {
        record?.release()
        record = null
    }

    private fun channelMask(): Int =
        if (channels == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO

    private fun encoding(): Int = AudioFormat.ENCODING_PCM_16BIT

    private fun sourceFor(source: InputSource): Int = when (source) {
        InputSource.MIC -> MediaRecorder.AudioSource.MIC
        InputSource.UNPROCESSED -> MediaRecorder.AudioSource.UNPROCESSED
        InputSource.BLUETOOTH -> MediaRecorder.AudioSource.VOICE_RECOGNITION
        InputSource.USB -> MediaRecorder.AudioSource.MIC
    }

    private companion object {
        const val MIN_BUFFER = 4096
    }
}
