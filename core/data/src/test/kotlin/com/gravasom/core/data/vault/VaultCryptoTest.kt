package com.gravasom.core.data.vault

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.Random

class VaultCryptoTest {

    @get:Rule
    val pasta = TemporaryFolder()

    private val cofre = VaultCrypto(VaultCrypto.newStreamingAead())

    /** Parece um M4A de verdade: caixa `ftyp` no byte 4, depois ruido. */
    private fun audioFalso(bytes: Int): File {
        val dados = ByteArray(bytes).also { Random(42).nextBytes(it) }
        "ftypM4A ".toByteArray().copyInto(dados, destinationOffset = 4)
        return pasta.newFile("a.m4a").also { it.writeBytes(dados) }
    }

    @Test
    fun `cifrar e decifrar devolve exatamente os mesmos bytes`() {
        val original = audioFalso(1_500_000) // cobre varios segmentos de 4 KB
        val cifrado = File(pasta.root, "a.m4a.gvs")
        cofre.encryptFile(original, cifrado)
        val volta = File(pasta.root, "volta.m4a")
        cofre.decryptFile(cifrado, volta)
        assertArrayEquals(original.readBytes(), volta.readBytes())
    }

    @Test
    fun `o arquivo cifrado nao parece audio e nao contem o conteudo`() {
        val original = audioFalso(100_000)
        val cifrado = File(pasta.root, "a.gvs")
        cofre.encryptFile(original, cifrado)
        val bytes = cifrado.readBytes()
        assertNotEquals("ftyp", String(bytes, 4, 4, Charsets.ISO_8859_1))
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("ftypM4A"))
        assertTrue("cifrado deve ser maior que o claro (cabecalho + tags)", bytes.size > original.length())
    }

    @Test
    fun `bytes adulterados sao detectados e nada e entregue como valido`() {
        val original = audioFalso(50_000)
        val cifrado = File(pasta.root, "a.gvs")
        cofre.encryptFile(original, cifrado)
        val adulterado = cifrado.readBytes().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 0x01).toByte() }
        cifrado.writeBytes(adulterado)
        assertThrows(IOException::class.java) { cofre.decryptFile(cifrado, File(pasta.root, "x")) }
        assertFalse("nao pode sobrar arquivo parcial", File(pasta.root, "x").exists())
    }

    @Test
    fun `outra chave nao decifra`() {
        val original = audioFalso(10_000)
        val cifrado = File(pasta.root, "a.gvs")
        cofre.encryptFile(original, cifrado)
        val outro = VaultCrypto(VaultCrypto.newStreamingAead())
        assertThrows(IOException::class.java) { outro.decryptFile(cifrado, File(pasta.root, "y")) }
    }

    @Test
    fun `arquivo vazio e cifrado e decifrado`() {
        val vazio = pasta.newFile("vazio").also { it.writeBytes(ByteArray(0)) }
        val cifrado = File(pasta.root, "vazio.gvs")
        cofre.encryptFile(vazio, cifrado)
        val volta = File(pasta.root, "vazio2")
        cofre.decryptFile(cifrado, volta)
        assertEquals(0L, volta.length())
    }

    @Test
    fun `decryptTo entrega o claro em um stream sem tocar o disco`() {
        val original = audioFalso(20_000)
        val cifrado = File(pasta.root, "a.gvs")
        cofre.encryptFile(original, cifrado)
        val saida = ByteArrayOutputStream()
        cofre.decryptTo(cifrado, saida)
        assertArrayEquals(original.readBytes(), saida.toByteArray())
    }

    @Test
    fun `cifrar duas vezes o mesmo arquivo gera saidas diferentes`() {
        val original = audioFalso(8_000)
        val a = File(pasta.root, "1.gvs").also { cofre.encryptFile(original, it) }
        val b = File(pasta.root, "2.gvs").also { cofre.encryptFile(original, it) }
        assertFalse(a.readBytes().contentEquals(b.readBytes()))
    }

    @Test
    fun `falha ao cifrar nao deixa o destino pela metade`() {
        val inexistente = File(pasta.root, "nao-existe.m4a")
        val destino = File(pasta.root, "d.gvs")
        assertThrows(IOException::class.java) { cofre.encryptFile(inexistente, destino) }
        assertFalse(destino.exists())
    }
}
