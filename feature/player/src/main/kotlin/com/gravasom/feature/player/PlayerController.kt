package com.gravasom.feature.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Envolve o ExoPlayer e expoe estado observavel para a UI. */
class PlayerController(context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMillis = MutableStateFlow(0L)
    val positionMillis: StateFlow<Long> = _positionMillis.asStateFlow()

    private val _durationMillis = MutableStateFlow(0L)
    val durationMillis: StateFlow<Long> = _durationMillis.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
        }
    }

    init {
        player.addListener(listener)
    }

    fun load(path: String) {
        player.setMediaItem(MediaItem.fromUri(path))
        player.prepare()
        _durationMillis.value = 0L
        _positionMillis.value = 0L
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun seekTo(millis: Long) = player.seekTo(millis)

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
    }

    fun tick() {
        _positionMillis.value = player.currentPosition
        if (player.duration > 0) _durationMillis.value = player.duration
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }
}
