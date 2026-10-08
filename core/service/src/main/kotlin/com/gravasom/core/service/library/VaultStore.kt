package com.gravasom.core.service.library

import android.content.Context
import com.gravasom.core.data.vault.VaultCrypto
import com.gravasom.core.domain.model.Recording
import java.io.File
import java.io.OutputStream

/**
 * Cofre no disco: gravacoes privadas ficam em `files/vault/<nome>.<ext>.gvs`, cifradas com AES-256-GCM (Tink).
 * Um arquivo `.gvs` nao abre como audio. Para tocar ou compartilhar, a copia em claro e gerada num diretorio de
 * cache PRIVADO do app (`cache/vault-play`, `cache/vault-share`) e apagada na proxima abertura do app ([clearTemp]).
 * A chave so e criada quando o cofre e usado pela primeira vez (`vault` e preguicoso).
 */
class VaultStore(private val context: Context, private val vault: Lazy<VaultCrypto>) {

    fun dir(): File = File(context.filesDir, DIR).apply { mkdirs() }

    fun isEncrypted(recording: Recording): Boolean = recording.path.endsWith(".$EXTENSION")

    /** Cifra [plain] para o cofre e apaga o claro. Retorna o arquivo cifrado, ou null se falhou (o claro permanece). */
    fun protect(plain: File): File? {
        val target = File(dir(), "${plain.name}.$EXTENSION")
        return runCatching {
            vault.value.encryptFile(plain, target)
            check(plain.delete() || !plain.exists()) { "nao foi possivel apagar o arquivo em claro" }
            target
        }.getOrElse {
            target.delete()
            null
        }
    }

    /** Arquivo reproduzivel: o proprio caminho (nao cifrada) ou a copia em claro no cache privado (cifrada). */
    fun playable(recording: Recording): File {
        if (!isEncrypted(recording)) return File(recording.path)
        val target = File(tempDir(PLAY), "${recording.id}.${recording.format.extension}")
        if (!target.isFile || target.length() == 0L) vault.value.decryptFile(File(recording.path), target)
        return target
    }

    /** Copia em claro, com nome legivel, para o FileProvider entregar a outro app. */
    fun forSharing(recording: Recording): File {
        if (!isEncrypted(recording)) return File(recording.path)
        val safe = recording.title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").ifBlank { "gravacao" }
        val target = File(tempDir(SHARE), "$safe.${recording.format.extension}")
        vault.value.decryptFile(File(recording.path), target)
        return target
    }

    fun decryptTo(recording: Recording, out: OutputStream) = vault.value.decryptTo(File(recording.path), out)

    fun forget(recording: Recording) {
        File(tempDir(PLAY), "${recording.id}.${recording.format.extension}").delete()
    }

    /** Apaga qualquer copia em claro que tenha sobrado no cache. */
    fun clearTemp() {
        listOf(PLAY, SHARE).forEach { name -> File(context.cacheDir, name).deleteRecursively() }
    }

    private fun tempDir(name: String): File = File(context.cacheDir, name).apply { mkdirs() }

    companion object {
        const val EXTENSION = "gvs"
        private const val DIR = "vault"
        private const val PLAY = "vault-play"
        private const val SHARE = "vault-share"
    }
}
