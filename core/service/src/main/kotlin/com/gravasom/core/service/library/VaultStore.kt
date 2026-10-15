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
        decryptOnce(File(recording.path), target)
        return target
    }

    /** Copia em claro de uso unico: [close] a apaga (so se foi criada aqui; gravacao fora do cofre devolve o proprio arquivo). */
    class PlainCopy(val file: File, private val temporary: Boolean) : AutoCloseable {
        override fun close() {
            if (temporary) file.delete()
        }
    }

    /**
     * Copia em claro PROPRIA para operacoes em lote (backup da biblioteca inteira). Usar [playable] deixava no cache a copia em claro de
     * TODAS as gravacoes privadas ate a proxima abertura do app. O player continua usando [playable] (compartilhada, vive enquanto ele
     * toca); esta aqui nao pode ser apagada por baixo dele. Quem pede deve fechar (`use`).
     */
    fun openPlain(recording: Recording): PlainCopy {
        if (!isEncrypted(recording)) return PlainCopy(File(recording.path), temporary = false)
        val copy = File.createTempFile("op-", ".${recording.format.extension}", tempDir(OPERATION))
        try {
            vault.value.decryptFile(File(recording.path), copy)
        } catch (e: Exception) {
            copy.delete()
            throw e
        }
        return PlainCopy(copy, temporary = true)
    }

    /** Copia em claro, com nome legivel, para o FileProvider entregar a outro app. */
    fun forSharing(recording: Recording): File {
        if (!isEncrypted(recording)) return File(recording.path)
        val safe = recording.title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").ifBlank { "gravacao" }
        // Nome limitado: titulo com mais de ~255 bytes daria ENAMETOOLONG.
        val target = File(tempDir(SHARE), "${safe.take(MAX_SHARE_NAME)}.${recording.format.extension}")
        decryptOnce(File(recording.path), target)
        return target
    }

    fun decryptTo(recording: Recording, out: OutputStream) = vault.value.decryptTo(File(recording.path), out)

    fun forget(recording: Recording) {
        val target = File(tempDir(PLAY), "${recording.id}.${recording.format.extension}")
        synchronized(lockFor(target)) {
            target.delete()
            marker(target).delete()
        }
    }

    /**
     * Decifra [source] para [target] uma unica vez, mesmo com varios consumidores (player, backup, clip, join) pedindo ao mesmo tempo:
     * sem a trava os dois escreviam o mesmo `.tmp` e renomeavam um arquivo corrompido. O marcador guarda o tamanho final; uma copia
     * que sobrou truncada (decifragem interrompida) nao bate com ele e e refeita, em vez de ser reaproveitada.
     */
    private fun decryptOnce(source: File, target: File) {
        synchronized(lockFor(target)) {
            val ok = marker(target)
            val pronta = target.isFile && ok.isFile && ok.readText().trim() == target.length().toString()
            if (pronta) return
            ok.delete()
            vault.value.decryptFile(source, target)
            ok.writeText(target.length().toString())
        }
    }

    private fun marker(target: File) = File(target.path + ".ok")

    private fun lockFor(target: File): Any = locks.getOrPut(target.path) { Any() }

    private val locks = java.util.concurrent.ConcurrentHashMap<String, Any>()

    /** Apaga qualquer copia em claro que tenha sobrado no cache. */
    fun clearTemp() {
        // "melhorar": WAV decodificados e previas do "Melhorar som" (cache/melhorar). Se o processo morreu no meio de uma sessao com
        // gravacao privada, o claro ficaria la; e cache descartavel (so custa decodificar de novo).
        listOf(PLAY, SHARE, OPERATION, ENHANCE_CACHE).forEach { name -> File(context.cacheDir, name).deleteRecursively() }
    }

    private fun tempDir(name: String): File = File(context.cacheDir, name).apply { mkdirs() }

    companion object {
        private const val MAX_SHARE_NAME = 100
        const val EXTENSION = "gvs"
        private const val DIR = "vault"
        private const val PLAY = "vault-play"
        private const val SHARE = "vault-share"
        private const val OPERATION = "vault-op"
        private const val ENHANCE_CACHE = "melhorar"
    }
}
