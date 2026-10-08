package com.gravasom.core.service

import android.content.Context
import android.os.StatFs

/** Espaco livre na pasta das gravacoes. */
object FreeSpace {

    /** Gancho de TESTE (instrumentados): simula este valor em bytes em vez de ler o disco. Nulo em producao. */
    @Volatile
    var overrideBytes: Long? = null

    fun bytes(context: Context): Long =
        overrideBytes ?: runCatching { StatFs(RecordingStorage.dir(context).path).availableBytes }.getOrDefault(Long.MAX_VALUE)
}
