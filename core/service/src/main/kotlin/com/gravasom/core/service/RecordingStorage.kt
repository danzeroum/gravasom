package com.gravasom.core.service

import android.content.Context
import android.os.Environment
import java.io.File

/** Pasta das gravacoes: area propria do app (sem permissao de armazenamento). */
object RecordingStorage {

    fun dir(context: Context): File {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        return File(root, "Recordings").apply { mkdirs() }
    }
}
