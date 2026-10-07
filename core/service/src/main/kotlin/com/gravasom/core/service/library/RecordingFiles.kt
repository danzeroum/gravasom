package com.gravasom.core.service.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import java.io.File

/** Operacoes sobre os ARQUIVOS de uma gravacao: compartilhar, salvar em..., excluir de vez. */
class RecordingFiles(
    private val context: Context,
    private val repo: RecordingRepository,
    private val publisher: MediaStorePublisher,
) {

    /** Uri `content://` via FileProvider: o destino recebe permissao de leitura e NUNCA o caminho bruto do arquivo. */
    fun shareUri(recording: Recording): Uri =
        FileProvider.getUriForFile(context, authority(context), File(recording.path))

    fun shareIntent(recording: Recording): Intent {
        val uri = shareUri(recording)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = recording.format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, recording.title)
            clipData = android.content.ClipData.newRawUri(recording.title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, recording.title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /** "Salvar em..." (SAF): copia o arquivo para o destino escolhido pelo usuario (Drive, Arquivos...). */
    fun copyTo(recording: Recording, destination: Uri): Boolean = runCatching {
        context.contentResolver.openOutputStream(destination)?.use { out ->
            File(recording.path).inputStream().use { it.copyTo(out) }
        } != null
    }.getOrDefault(false)

    /** Exclusao definitiva: arquivo privado + copia publicada + linha do banco. */
    suspend fun deleteForever(recording: Recording) {
        File(recording.path).delete()
        publisher.remove(recording.publishedUri)
        repo.deleteForever(recording.id)
    }

    companion object {
        fun authority(context: Context) = "${context.packageName}.fileprovider"
    }
}
