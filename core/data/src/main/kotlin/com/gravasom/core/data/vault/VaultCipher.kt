package com.gravasom.core.data.vault

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig

/**
 * Cofre opcional: AES-256-GCM via Tink. A chave fica no Android Keystore.
 * Nada aqui sai do aparelho.
 */
class VaultCipher {

    private val aead: Aead by lazy {
        AeadConfig.register()
        val handle = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
        handle.getPrimitive(Aead::class.java)
    }

    fun encrypt(plain: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray =
        aead.encrypt(plain, associatedData)

    fun decrypt(cipher: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray =
        aead.decrypt(cipher, associatedData)
}
