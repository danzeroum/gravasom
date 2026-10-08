package com.gravasom.core.service.library

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Publica uma COPIA da gravacao em Music/Recordings (MediaStore), visivel em Arquivos e outros apps (ADR-003).
 * API 29+: MediaStore com RELATIVE_PATH, sem permissao de armazenamento.
 * API 26-28: grava o arquivo em Music/Recordings e registra no MediaStore; exige WRITE_EXTERNAL_STORAGE (manifesto
 * com maxSdkVersion=28). Sem a permissao, nada e publicado e a gravacao fica so no app.
 */
class MediaStorePublisher(private val context: Context) {

    /** Retorna a Uri (texto) da copia publicada, ou null se nao foi possivel publicar. */
    fun publish(file: File, displayName: String, mimeType: String): String? {
        if (!file.exists()) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return publishLegacy(file, displayName, mimeType)
        val uri = insertPending(displayName, mimeType) ?: return null
        val copied = runCatching { copyAndRelease(uri, file) }
        if (copied.isFailure) context.contentResolver.delete(uri, null, null)
        return uri.takeIf { copied.isSuccess }?.toString()
    }

    /** API 26-28: copia o arquivo para a pasta publica e cria a linha no MediaStore (o scanner completa a duracao). */
    private fun publishLegacy(file: File, displayName: String, mimeType: String): String? {
        val dir = legacyDir() ?: return null
        val target = uniqueFile(dir, displayName)
        if (runCatching { file.copyTo(target) }.isFailure) return null
        val uri = insertLegacyRow(target, mimeType)
        if (uri == null) target.delete() else MediaScannerConnection.scanFile(context, arrayOf(target.path), arrayOf(mimeType), null)
        return uri?.toString()
    }

    /** Music/Recordings, ou null sem a permissao de armazenamento / sem midia montada / sem conseguir criar a pasta. */
    @Suppress("DEPRECATION")
    private fun legacyDir(): File? {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted || Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) return null
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Recordings")
        return dir.takeIf { it.isDirectory || it.mkdirs() }
    }

    @Suppress("DEPRECATION")
    private fun insertLegacyRow(target: File, mimeType: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DATA, target.path)
            put(MediaStore.Audio.Media.DISPLAY_NAME, target.name)
            put(MediaStore.Audio.Media.TITLE, target.nameWithoutExtension)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.SIZE, target.length())
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
        }
        return runCatching { context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) }.getOrNull()
    }

    private fun uniqueFile(dir: File, displayName: String): File {
        val base = displayName.substringBeforeLast('.')
        val ext = displayName.substringAfterLast('.', "")
        var candidate = File(dir, displayName)
        var n = 1
        while (candidate.exists()) {
            candidate = File(dir, if (ext.isEmpty()) "$base ($n)" else "$base ($n).$ext")
            n++
        }
        return candidate
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
        val uri = Uri.parse(uriText)
        runCatching {
            // Antes da API 29 apagar a linha nem sempre apaga o arquivo: remove o arquivo pelo caminho registrado.
            val path = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) legacyPath(uri) else null
            context.contentResolver.delete(uri, null, null)
            path?.let { File(it).delete() }
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyPath(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    companion object {
        const val PASTA = "Music/Recordings"
    }
}
