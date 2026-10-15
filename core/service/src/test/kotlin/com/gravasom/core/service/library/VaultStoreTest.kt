package com.gravasom.core.service.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gravasom.core.data.vault.VaultCrypto
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.model.Recording
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Cofre (B4/B5/B6), tudo local com Tink real:
 *  - decifrar a mesma gravacao em paralelo nao pode corromper a copia em claro;
 *  - titulos longos/estranhos nao quebram o compartilhamento;
 *  - falha ao cifrar deve ser visivel (null) e preservar o claro, sem deixar arquivo parcial no cofre.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var vault: VaultStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        vault = VaultStore(context, lazy { VaultCrypto(VaultCrypto.newStreamingAead()) })
        vault.clearTemp()
    }

    private fun claro(nome: String, bytes: Int): File =
        tmp.newFile(nome).apply { writeBytes(ByteArray(bytes) { (it * 31 + 7).toByte() }) }

    private fun gravacao(id: Long, arquivo: File, titulo: String = "Aula") = Recording(
        id = id, title = titulo, path = arquivo.path, format = RecordingFormat.WAV,
        durationMillis = 1_000, sizeBytes = arquivo.length(), createdAtMillis = 1,
    )

    @Test
    fun `dado cofre ligado quando protege entao cifra, apaga o claro e a copia decifrada e identica`() {
        val original = claro("a.wav", 300_000)
        val bytes = original.readBytes()
        val cifrado = vault.protect(original)
        assertNotNull(cifrado)
        assertFalse("claro nao foi apagado", original.exists())
        val volta = vault.playable(gravacao(1, cifrado!!))
        assertArrayEquals(bytes, volta.readBytes())
    }

    @Test
    fun `dado decifragem simultanea da mesma gravacao quando varios consumidores pedem entao o arquivo continua integro`() {
        val original = claro("b.wav", 2_000_000)
        val bytes = original.readBytes()
        val cifrado = vault.protect(original)!!
        val rec = gravacao(2, cifrado)
        val pool = Executors.newFixedThreadPool(6)
        val largada = CountDownLatch(1)
        val resultados = (1..6).map {
            pool.submit<Boolean> {
                largada.await()
                runCatching { vault.playable(rec).readBytes().contentEquals(bytes) }.getOrDefault(false)
            }
        }
        largada.countDown()
        val ok = resultados.map { it.get(60, TimeUnit.SECONDS) }
        pool.shutdownNow()
        assertTrue("alguma leitura recebeu arquivo corrompido ou falhou: $ok", ok.all { it })
        assertArrayEquals("copia final corrompida", bytes, vault.playable(rec).readBytes())
    }

    @Test
    fun `dado copia em claro truncada no cache quando pede para tocar entao decifra de novo em vez de reaproveitar`() {
        val original = claro("c.wav", 500_000)
        val bytes = original.readBytes()
        val rec = gravacao(3, vault.protect(original)!!)
        val copia = vault.playable(rec)
        copia.writeBytes(bytes.copyOf(1_000)) // sobra de uma decifragem interrompida
        assertArrayEquals("reaproveitou arquivo truncado", bytes, vault.playable(rec).readBytes())
    }

    @Test
    fun `dado gravacao privada quando esquece a copia entao nao sobra claro no cache`() {
        val rec = gravacao(4, vault.protect(claro("d.wav", 10_000))!!)
        val copia = vault.playable(rec)
        assertTrue(copia.exists())
        vault.forget(rec)
        assertFalse("claro permaneceu em ${copia.path}", copia.exists())
    }

    @Test
    fun `dado copia de uso unico quando fecha entao o claro some e o player nao e afetado`() {
        val original = claro("g.wav", 50_000)
        val bytes = original.readBytes()
        val rec = gravacao(6, vault.protect(original)!!)
        val daPlayer = vault.playable(rec) // copia compartilhada do player
        val usada = vault.openPlain(rec).use { copia ->
            assertArrayEquals(bytes, copia.file.readBytes())
            copia.file
        }
        assertFalse("a copia de uso unico ficou no disco", usada.exists())
        assertTrue("fechar a copia de uso unico nao pode apagar a do player", daPlayer.exists())
    }

    @Test
    fun `dado gravacao fora do cofre quando abre copia de uso unico entao devolve o original e nao o apaga`() {
        val arquivo = claro("h.wav", 1_000)
        val rec = gravacao(7, arquivo)
        vault.openPlain(rec).use { assertEquals(arquivo.path, it.file.path) }
        assertTrue(arquivo.exists())
    }

    @Test
    fun `dado titulo com barras, dois pontos e 300 caracteres quando prepara para compartilhar entao o nome e valido`() {
        val rec = gravacao(5, vault.protect(claro("e.wav", 5_000))!!, titulo = "08/10/2026 14:32 · " + "x".repeat(300))
        val arquivo = vault.forSharing(rec)
        assertTrue(arquivo.exists())
        assertFalse(arquivo.name.contains('/'))
        assertTrue("nome com ${arquivo.name.toByteArray().size} bytes passa de 255", arquivo.name.toByteArray().size <= 255)
    }

    @Test
    fun `dado falha ao cifrar quando protege entao devolve null, preserva o claro e nao deixa parcial no cofre`() {
        val quebrado = VaultStore(context, lazy { throw IllegalStateException("Keystore indisponivel") })
        val original = claro("f.wav", 20_000)
        val resultado = runCatching { quebrado.protect(original) }
        // Contrato atual: null (claro preservado). O chamador e quem precisa avisar o usuario (RecordingIngestorTest).
        assertTrue(resultado.isSuccess && resultado.getOrNull() == null)
        assertTrue("o claro nao pode ser perdido", original.exists())
        assertEquals(0, quebrado.dir().listFiles { f -> f.name.startsWith("f.wav") }?.size ?: 0)
        assertNull(resultado.getOrNull())
    }
}
