package com.gravasom.feature.player

import com.gravasom.core.audio.engine.analysis.SkipPlan
import kotlinx.coroutines.flow.StateFlow

/**
 * O que a tela precisa de um player. O [PlayerController] (ExoPlayer) e a implementacao de producao; os testes de tela usam
 * um falso, sem Media3.
 */
interface PlayerPort {
    val isPlaying: StateFlow<Boolean>
    val positionMillis: StateFlow<Long>
    val durationMillis: StateFlow<Long>

    /** Silencio ja pulado desde que a gravacao foi carregada. */
    val skippedMillis: StateFlow<Long>

    fun play()
    fun pause()
    fun seekTo(millis: Long)
    fun seekToAndPlay(millis: Long)
    fun setSpeed(speed: Float)
    fun setSkip(plan: SkipPlan?, enabled: Boolean)
}
