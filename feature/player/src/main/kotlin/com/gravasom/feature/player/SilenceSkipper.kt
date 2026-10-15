package com.gravasom.feature.player

import com.gravasom.core.audio.engine.analysis.SkipPlan

/** O que o pulo de silencio precisa do player: se toca, onde esta e como saltar. */
interface SkipTarget {
    val isPlaying: Boolean
    val positionMillis: Long
    fun seekTo(millis: Long)
}

/**
 * Pular silencio ao OUVIR (ADR-014): a cada batida do relogio, se esta ligado, tocando e dentro de uma lacuna do [SkipPlan],
 * salta para o fim dela. O plano vem da analise feita ao salvar (WebRTC VAD), nunca do nivel do VU. Sem Android: o relogio e do
 * [PlaybackHub], que o mantem vivo com o painel fechado e o app em segundo plano.
 */
class SilenceSkipper(private val target: SkipTarget) {

    var plan: SkipPlan? = null
        private set
    var enabled: Boolean = false
        private set

    /** Silencio ja pulado desde que a gravacao foi carregada. */
    var skippedMillis: Long = 0L
        private set

    fun configure(plan: SkipPlan?, enabled: Boolean) {
        this.plan = plan
        this.enabled = enabled
    }

    /** Mesma gravacao recarregada (volta ao inicio): zera so o total pulado; o plano e o liga/desliga do usuario continuam. */
    fun restart() {
        skippedMillis = 0L
    }

    /** Nova gravacao carregada: o plano e o total pulado da anterior nao valem mais. */
    fun reset() {
        plan = null
        enabled = false
        skippedMillis = 0L
    }

    /** Uma batida do relogio. Devolve a posicao depois de um eventual salto. */
    fun tick(): Long {
        var position = target.positionMillis
        if (enabled && target.isPlaying) {
            val jump = plan?.skipTarget(position)
            if (jump != null && jump > position) {
                skippedMillis += jump - position
                target.seekTo(jump)
                position = jump
            }
        }
        return position
    }
}
