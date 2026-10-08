package com.gravasom.feature.player

import com.gravasom.core.audio.engine.analysis.SkipPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Player falso para testes de tela: guarda o estado e anota as chamadas, sem Media3. */
class FakePlayerPort(duration: Long = 3_600_000L) : PlayerPort {
    override val isPlaying = MutableStateFlow(false)
    override val positionMillis = MutableStateFlow(0L)
    override val durationMillis: StateFlow<Long> = MutableStateFlow(duration)
    override val skippedMillis = MutableStateFlow(0L)

    val calls = mutableListOf<String>()
    var skipPlan: SkipPlan? = null
    var skipEnabled = false

    override fun play() { calls += "play"; isPlaying.value = true }
    override fun pause() { calls += "pause"; isPlaying.value = false }
    override fun seekTo(millis: Long) { calls += "seekTo($millis)"; positionMillis.value = millis }
    override fun seekToAndPlay(millis: Long) { calls += "seekToAndPlay($millis)"; positionMillis.value = millis; isPlaying.value = true }
    override fun setSpeed(speed: Float) { calls += "setSpeed($speed)" }
    override fun setSkip(plan: SkipPlan?, enabled: Boolean) { skipPlan = plan; skipEnabled = enabled }
}
