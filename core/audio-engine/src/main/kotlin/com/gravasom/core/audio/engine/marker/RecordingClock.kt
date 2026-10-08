package com.gravasom.core.audio.engine.marker

/**
 * Tempo de GRAVACAO decorrido, sem contar pausas: e a posicao do instante atual no arquivo, que e onde um marcador
 * criado agora deve cair. [now] e um relogio monotonico em ms (SystemClock.elapsedRealtime no app).
 */
class RecordingClock(private val now: () -> Long) {

    private var accumulated = 0L
    private var runningSince: Long? = null
    private var started = false

    fun start() {
        accumulated = 0L
        runningSince = now()
        started = true
    }

    fun pause() {
        val since = runningSince ?: return
        accumulated += (now() - since).coerceAtLeast(0L)
        runningSince = null
    }

    fun resume() {
        if (started && runningSince == null) runningSince = now()
    }

    fun stop() = pause()

    fun elapsedMillis(): Long {
        val since = runningSince ?: return accumulated
        return accumulated + (now() - since).coerceAtLeast(0L)
    }
}
