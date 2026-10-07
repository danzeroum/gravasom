package com.gravasom.core.dsp

import com.gravasom.core.audio.engine.edit.AudioBlockProcessor
import com.gravasom.core.audio.engine.edit.EditProject

/** Cadeia DSP nativa (C++) para um canal, montada a partir de um [EditProject]. Fechar libera o handle. */
class NativeBlockProcessor(project: EditProject, sampleRate: Int) : AudioBlockProcessor {

    private var handle: Long

    init {
        check(NativeDsp.ensureLoaded()) { "libgravasom_dsp indisponivel" }
        handle = NativeDsp.nativeCreate(sampleRate)
        check(handle != 0L) { "taxa de amostragem invalida: $sampleRate" }
        try {
            for (step in project.steps.filter { it.enabled }) {
                check(NativeDsp.nativeAddStep(handle, step.type.id, step.params.toFloatArray())) {
                    "passo recusado: ${step.type} ${step.params}"
                }
            }
            NativeDsp.nativeSetBypass(handle, project.bypass)
            NativeDsp.nativeSetIntensity(handle, project.intensity)
        } catch (e: IllegalStateException) {
            close()
            throw e
        }
    }

    override val latencySamples: Int get() = NativeDsp.nativeLatency(handle)

    override fun process(block: FloatArray, length: Int) {
        check(handle != 0L) { "processador fechado" }
        check(NativeDsp.nativeProcess(handle, block, 0, length) >= 0) { "falha no processamento nativo" }
    }

    override fun close() {
        if (handle != 0L) {
            NativeDsp.nativeDestroy(handle)
            handle = 0L
        }
    }

    companion object {
        /** Cadeias vivas no lado nativo; 0 prova ausencia de vazamento. */
        fun liveChains(): Int = if (NativeDsp.ensureLoaded()) NativeDsp.nativeLiveChains() else 0
    }
}
