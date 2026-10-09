package com.gravasom.core.data.backup

import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cifra do backup (Onda 2): o pacote inteiro e cifrado em fluxo pelo Tink (AES256_GCM_HKDF_4KB, o mesmo esquema do cofre,
 * ADR-012), e a chave do pacote (keyset) viaja ENVOLVIDA por uma chave derivada da SENHA do usuario (PBKDF2-HMAC-SHA256,
 * 210 mil iteracoes, sal por pacote, IV aleatorio). Assim o pacote abre em outro aparelho: a chave nao depende do Keystore.
 * Senha errada falha na abertura do envelope (tag AES-GCM) ANTES de qualquer restauracao.
 */
class BackupCipher private constructor(
    private val handle: KeysetHandle,
    private val streaming: StreamingAead,
) {

    /** Parametros que viajam no cabecalho do pacote: sal e contagem de iteracoes. */
    class Salt(val bytes: ByteArray, val iterations: Int)

    /** IV + keyset cifrado em AES-GCM com a chave derivada da senha: o que vai no cabecalho do pacote. */
    fun wrappedKeyset(key: SecretKey): ByteArray {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(TinkProtoKeysetFormat.serializeKeyset(handle, InsecureSecretKeyAccess.get()))
        return iv + encrypted
    }

    fun encrypting(output: OutputStream): OutputStream = streaming.newEncryptingStream(output, AAD)

    fun decrypting(input: InputStream): InputStream = streaming.newDecryptingStream(input, AAD)

    companion object {

        private const val TEMPLATE = "AES256_GCM_HKDF_4KB"
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val KEY_BITS = 256
        private const val GCM_TAG_BITS = 128
        private val AAD = "gravasom-backup-v1".toByteArray()

        fun newSalt(iterations: Int = DEFAULT_ITERATIONS): Salt {
            val bytes = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
            return Salt(bytes, iterations)
        }

        /** Deriva a chave de envolvimento da senha (PBKDF2-HMAC-SHA256). */
        fun deriveKey(password: CharArray, salt: Salt): SecretKey {
            val spec = PBEKeySpec(password, salt.bytes, salt.iterations, KEY_BITS)
            return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        }

        /** Pacote novo: keyset de fluxo fresco. */
        fun newCipher(): BackupCipher {
            StreamingAeadConfig.register()
            return of(KeysetHandle.generateNew(KeyTemplates.get(TEMPLATE)))
        }

        /** Abre o envelope do cabecalho; [GeneralSecurityException] com senha errada (a tag nao confere). */
        fun open(wrapped: ByteArray, key: SecretKey): BackupCipher {
            val iv = wrapped.copyOfRange(0, IV_BYTES)
            val encrypted = wrapped.copyOfRange(IV_BYTES, wrapped.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            return of(TinkProtoKeysetFormat.parseKeyset(cipher.doFinal(encrypted), InsecureSecretKeyAccess.get()))
        }

        private fun of(handle: KeysetHandle) = BackupCipher(handle, handle.getPrimitive(StreamingAead::class.java))

        const val DEFAULT_ITERATIONS = 210_000
    }
}
