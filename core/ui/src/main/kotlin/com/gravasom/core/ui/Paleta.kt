package com.gravasom.core.ui

import android.os.Build

/**
 * Paleta de cores escolhida em Ajustes › Aparência. [GRAVASOM] é o padrão. [SISTEMA] usa as cores dinâmicas do Android 12+;
 * em versões anteriores cai em [GRAVASOM] (ver [efetiva]). Gravando, pausado, medidor e marcadores têm as mesmas cores nas três.
 */
enum class Paleta(val id: String) {
    GRAVASOM("gravasom"),
    MIXLIROUS("mixlirous"),
    SISTEMA("sistema"),
    ;

    /** A paleta que de fato vale neste aparelho: "Cores do sistema" não existe antes do Android 12. */
    fun efetiva(sdkInt: Int = Build.VERSION.SDK_INT): Paleta =
        if (this == SISTEMA && sdkInt < Build.VERSION_CODES.S) GRAVASOM else this

    companion object {
        /** Id guardado nas preferências; nulo ou desconhecido volta ao padrão. */
        fun fromId(id: String?): Paleta = entries.firstOrNull { it.id == id } ?: GRAVASOM

        /** "Cores do sistema" só aparece na lista de escolha a partir do Android 12. */
        fun disponiveis(sdkInt: Int = Build.VERSION.SDK_INT): List<Paleta> = entries.filter { it.efetiva(sdkInt) == it }
    }
}
