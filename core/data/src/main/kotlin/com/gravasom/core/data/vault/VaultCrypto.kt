package com.gravasom.core.data.vault

import android.content.Context
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException

/**
 * Cofre de gravacoes: AES-256-GCM em fluxo (Tink, AES256_GCM_HKDF_4KB), por arquivo, sem carregar o audio inteiro na memoria.
 * Em producao a chave (keyset) fica cifrada por uma chave-mestra do Android Keystore ([forContext]); nada sai do aparelho.
 * Escreve em `<destino>.tmp` e renomeia: nunca deixa um arquivo pela metade. Falha de integridade vira [IOException].
 */
class VaultCrypto(private val streamingAead: StreamingAead) {

    fun encryptFile(source: File, destination: File) {
        atomically(destination) { tmp ->
            source.inputStream().use { input ->
                streamingAead.newEncryptingStream(tmp.outputStream(), ASSOCIATED_DATA).use { input.copyTo(it) }
            }
        }
    }

    fun decryptFile(source: File, destination: File) {
        atomically(destination) { tmp -> tmp.outputStream().use { decryptTo(source, it) } }
    }

    /** Entrega o claro direto em [out] (ex.: "Salvar em..."), sem arquivo temporario em claro. */
    fun decryptTo(source: File, out: OutputStream) {
        try {
            source.inputStream().use { raw -> decryptingStream(raw).use { it.copyTo(out) } }
        } catch (e: GeneralSecurityException) {
            throw IOException("cofre: arquivo adulterado ou chave errada", e)
        }
    }

    private fun decryptingStream(raw: InputStream): InputStream = streamingAead.newDecryptingStream(raw, ASSOCIATED_DATA)

    private fun atomically(destination: File, write: (File) -> Unit) {
        val tmp = File(destination.path + ".tmp")
        val failure = try {
            write(tmp)
            if (tmp.renameTo(destination)) null else IOException("cofre: nao foi possivel finalizar ${destination.name}")
        } catch (e: IOException) {
            e
        } catch (e: GeneralSecurityException) {
            IOException("cofre: arquivo adulterado ou chave errada", e)
        }
        if (failure != null) {
            tmp.delete()
            destination.delete()
            throw failure
        }
    }

    companion object {
        private const val TEMPLATE = "AES256_GCM_HKDF_4KB"
        private const val KEYSET_PREFS = "gravasom_vault_prefs"
        private const val KEYSET_NAME = "gravasom_vault_keyset"
        private const val MASTER_KEY_URI = "android-keystore://gravasom_vault_master"
        private val ASSOCIATED_DATA = "gravasom-vault-v1".toByteArray()

        /** Chave nova em memoria (testes na JVM). */
        fun newStreamingAead(): StreamingAead {
            StreamingAeadConfig.register()
            return KeysetHandle.generateNew(KeyTemplates.get(TEMPLATE)).getPrimitive(StreamingAead::class.java)
        }

        /** Chave persistente do aparelho, protegida pelo Android Keystore. Criada na primeira vez que o cofre e usado. */
        fun forContext(context: Context): VaultCrypto {
            AeadConfig.register()
            StreamingAeadConfig.register()
            val handle = AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
                .withKeyTemplate(KeyTemplates.get(TEMPLATE))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
            return VaultCrypto(handle.getPrimitive(StreamingAead::class.java))
        }
    }
}
