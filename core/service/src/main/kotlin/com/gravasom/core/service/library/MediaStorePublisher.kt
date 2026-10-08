package com.gravasom.core.service.library

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import android.provider.MediaStore
import java.io.File

/**
 * Publica uma COPIA da gravacao em Music/Recordings (MediaStore), visivel em Arquivos e outros apps (ADR-003).
 * API 29+: sem permissao de armazenamento. API 26-28: exigiria WRITE_EXTERNAL_STORAGE; a copia publica nao e feita
 * e a gravacao fica so no app (PENDENTE, ver docs/status.md).
 */
class MediaStorePublisher(private val context: Context) {

    /** Retorna a Uri (texto) da copia publicada, ou null se nao foi possivel publicar. */
    fun publish(file: File, displayName: String, mimeType: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !file.exists()) return null
        val uri = insertPending(displayName, mimeType) ?: return null
        val copied = runCatching { copyAndRelease(uri, file) }
        if (copied.isFailure) context.contentResolver.delete(uri, null, null)
        return uri.takeIf { copied.isSuccess }?.toString()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertPending(displayName: String, mimeType: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, PASTA)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        return context.contentResolver.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
    }

    private fun copyAndRelease(uri: Uri, file: File) {
        val resolver = context.contentResolver
        val out = checkNotNull(resolver.openOutputStream(uri)) { "sem stream de saida" }
        out.use { stream -> file.inputStream().use { it.copyTo(stream) } }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
    }

    /** Remove a copia publicada (ao excluir a gravacao de vez). */
    fun remove(uriText: String?) {
        if (uriText == null) return
        runCatching { context.contentResolver.delete(Uri.parse(uriText), null, null) }
    }

    companion object {
        const val PASTA = "Music/Recordings"
    }
}
