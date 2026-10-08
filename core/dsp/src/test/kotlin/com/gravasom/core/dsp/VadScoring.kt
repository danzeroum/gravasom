package com.gravasom.core.dsp

import com.gravasom.core.audio.engine.analysis.SkipPlan

/** Pontuacao de um plano de pulo contra o oraculo de fala (quadros de 20 ms). */
internal class Score(val runs: Int, val runsCut: Int, val frames: Int, val framesCut: Int, val cutRuns: List<IntRange>)

internal object VadScoring {
    const val FRAME_MS = 20L
    private const val MERGE_GAP_FRAMES = 5
    private const val MIN_RUN_FRAMES = 3

    /** Trechos de fala do oraculo: quadros de fala unidos quando a lacuna e <= 100 ms; trechos < 60 ms (estalos) nao contam. */
    fun speechRuns(oracle: BooleanArray): List<IntRange> {
        val groups = ArrayList<IntRange>()
        for (i in oracle.indices.filter { oracle[it] }) {
            val previous = groups.lastOrNull()
            if (previous != null && i - previous.last <= MERGE_GAP_FRAMES + 1) {
                groups[groups.size - 1] = previous.first..i
            } else {
                groups += i..i
            }
        }
        return groups.filter { it.last - it.first + 1 >= MIN_RUN_FRAMES }
    }

    private fun isSkipped(frame: Int, plan: SkipPlan): Boolean =
        plan.ranges.any { frame * FRAME_MS < it.endMillis && (frame + 1) * FRAME_MS > it.startMillis }

    fun score(oracle: BooleanArray, plan: SkipPlan): Score {
        val runs = speechRuns(oracle)
        var frames = 0
        var framesCut = 0
        val cutRuns = ArrayList<IntRange>()
        for (run in runs) {
            val speech = run.filter { oracle[it] }
            val cut = speech.count { isSkipped(it, plan) }
            frames += speech.size
            framesCut += cut
            if (cut > 0) cutRuns += run
        }
        return Score(runs.size, cutRuns.size, frames, framesCut, cutRuns)
    }
}
