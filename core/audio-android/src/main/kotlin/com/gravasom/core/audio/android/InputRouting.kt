package com.gravasom.core.audio.android

import android.media.AudioDeviceInfo

/** Entrada de audio escolhida pelo usuario (null = deixar o sistema decidir). Definida pelo servico antes de cada gravacao. */
object InputRouting {
    @Volatile
    var preferred: AudioDeviceInfo? = null
}
