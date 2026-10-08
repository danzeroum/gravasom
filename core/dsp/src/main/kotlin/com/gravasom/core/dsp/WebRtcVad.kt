package com.gravasom.core.dsp

import com.gravasom.core.audio.engine.analysis.SpeechClassifier

/**
 * WebRTC VAD (vendorizado, ADR-014) como [SpeechClassifier]: PCM mono 16 kHz, quadros de 20 ms.
 * [mode] 0 (permissivo: mais quadros contam como fala) a 3 (agressivo: pula mais, arrisca mais). Fechar libera o handle.
 */
class WebRtcVad(val mode: Int) : SpeechClassifier {

    private var handle: Long

    init {
        require(mode in MODE_RANGE) { "modo do VAD deve ser 0..3" }
        check(NativeDsp.ensureLoaded()) { "libgravasom_dsp indisponivel" }
        handle = NativeVad.nativeCreate(mode)
        check(handle != 0L) { "VAD recusou o modo $mode" }
    }

    override fun classify(pcm16k: ShortArray, length: Int): ByteArray {
        check(handle != 0L) { "VAD fechado" }
        val flags = ByteArray(length / FRAME_SAMPLES + 2)
        val n = NativeVad.nativeProcess(handle, pcm16k, 0, length, flags)
        check(n >= 0) { "falha no VAD nativo" }
        return flags.copyOf(n)
    }

    override fun finish(): ByteArray {
        check(handle != 0L) { "VAD fechado" }
        val flags = ByteArray(1)
        val n = NativeVad.nativeFlush(handle, flags)
        check(n >= 0) { "falha no VAD nativo" }
        return flags.copyOf(n)
    }

    override fun close() {
        if (handle != 0L) {
            NativeVad.nativeDestroy(handle)
            handle = 0L
        }
    }

    companion object {
        val MODE_RANGE = 0..3
        const val FRAME_SAMPLES = 320

        /** Instancias vivas no lado nativo; 0 prova ausencia de vazamento. */
        fun liveInstances(): Int = if (NativeDsp.ensureLoaded()) NativeVad.nativeLiveInstances() else 0
    }
}
