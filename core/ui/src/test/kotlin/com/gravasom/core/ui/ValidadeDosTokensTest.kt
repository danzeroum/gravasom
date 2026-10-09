package com.gravasom.core.ui

import com.gravasom.core.ui.theme.TOKENS_SHA256
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Acusa o Kotlin gerado que ficou para trás do tokens.json (rode docs/design/ferramentas/gerar_kotlin.py). */
class ValidadeDosTokensTest {
    @Test
    fun `o codigo gerado e do tokens json atual`() {
        val tokens = File("../../docs/design/tokens.json")
        val sha = MessageDigest.getInstance("SHA-256").digest(tokens.readBytes()).joinToString("") { "%02x".format(it) }
        assertEquals("Rode: python3 docs/design/ferramentas/gerar_kotlin.py", sha, TOKENS_SHA256)
    }
}
