package com.gravasom.core.service.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Operacoes sobre os ARQUIVOS de uma gravacao: compartilhar, salvar em..., excluir de vez. */
class RecordingFiles(
    private val context: Context,
    private val repo: RecordingRepository,
    private val publisher: MediaStorePublisher,
    private val vaultStore: VaultStore,
) {

    /** Uri `content://` via FileProvider: o destino recebe permissao de leitura e NUNCA o caminho bruto do arquivo. */
    suspend fun shareUri(recording: Recording): Uri = withContext(Dispatchers.IO) {
        FileProvider.getUriForFile(context, authority(context), vaultStore.forSharing(recording))
    }

    suspend fun shareIntent(recording: Recording): Intent {
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
            if (vaultStore.isEncrypted(recording)) {
                vaultStore.decryptTo(recording, out)
            } else {
                File(recording.path).inputStream().use { it.copyTo(out) }
            }
        } != null
    }.getOrDefault(false)

    /** Arquivo que o player pode abrir: o original ou, no cofre, uma copia em claro no cache privado. */
    suspend fun playablePath(recording: Recording): String = withContext(Dispatchers.IO) { vaultStore.playable(recording).path }

    /** Copia em claro de uso unico (apagada ao fim do [block]); para backup e outras operacoes em lote sobre o cofre. */
    suspend fun <T> withPlain(recording: Recording, block: suspend (File) -> T): T = withContext(Dispatchers.IO) {
        vaultStore.openPlain(recording).use { block(it.file) }
    }

    /** Copia em claro de uso unico para quem precisa dela ao longo de varias etapas; quem pede DEVE fechar (`use`). */
    suspend fun openPlain(recording: Recording): VaultStore.PlainCopy = withContext(Dispatchers.IO) { vaultStore.openPlain(recording) }

    /** Apaga as copias em claro que sobraram no cache (chamado ao abrir o app). */
    fun clearTemp() = vaultStore.clearTemp()

    /** Exclusao definitiva: arquivo privado + copia publicada + linha do banco. */
    suspend fun deleteForever(recording: Recording) {
        File(recording.path).delete()
        vaultStore.forget(recording)
        publisher.remove(recording.publishedUri)
        repo.deleteForever(recording.id)
    }

    companion object {
        fun authority(context: Context) = "${context.packageName}.fileprovider"
    }
}
