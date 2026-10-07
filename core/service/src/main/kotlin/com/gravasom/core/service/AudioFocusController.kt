package com.gravasom.core.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/**
 * Pede foco de audio e reage a perda/ganho. Perda transiente (ligacao) pausa;
 * perda permanente (outro app tomou o mic) para e sinaliza erro.
 */
class AudioFocusController(
    context: Context,
    private val onPause: () -> Unit,
    private val onStopWithError: (String) -> Unit,
) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var request: AudioFocusRequest? = null

    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onPause()
            AudioManager.AUDIOFOCUS_LOSS -> onStopWithError("Microfone em uso por outro app")
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onPause()
            AudioManager.AUDIOFOCUS_GAIN -> Unit
        }
    }

    fun request(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val r = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(listener)
                .build()
            request = r
            return audioManager.requestAudioFocus(r) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        @Suppress("DEPRECATION")
        return audioManager.requestAudioFocus(
            listener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandon() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            request?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(listener)
        }
        request = null
    }
}
