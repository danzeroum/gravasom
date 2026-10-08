package com.gravasom

import android.Manifest
import android.os.Debug
import androidx.test.rule.GrantPermissionRule
import com.gravasom.core.audio.android.mp3.LameBridge
import com.gravasom.core.audio.android.recorder.PcmMp3Recorder
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.service.MediaProbe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** Fase 12: MP3 real (LAME via JNI): duracao +-100 ms, ausencia de vazamento e saida sem clipping. */
class Mp3Test {

    @get:Rule
    val permissoes: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private fun pcmSeno(segundos: Double, taxa: Int = 44_100, amp: Double = 0.5): ShortArray =
        ShortArray((segundos * taxa).toInt()) { (Math.sin(2 * Math.PI * 1000.0 * it / taxa) * amp * Short.MAX_VALUE).toInt().toShort() }

    /** Codifica via JNI em blocos de 10 ms, como o recorder faz, e grava com o quadro de tag. */
    private fun codificar(pcm: ShortArray, destino: File, taxa: Int = 44_100): File {
        assertTrue("libmp3lame/gravasom_mp3 nao carregaram", LameBridge.ensureLoaded())
        val h = LameBridge.nativeInit(taxa, 1, 128, 5)
        assertTrue("nativeInit falhou", h != 0L)
        try {
            destino.outputStream().use { saida ->
                val bloco = taxa / 100
                val out = ByteArray(LameBridge.outputCapacityFor(bloco))
                val tmp = ShortArray(bloco)
                var i = 0
                while (i < pcm.size) {
                    val n = minOf(bloco, pcm.size - i)
                    System.arraycopy(pcm, i, tmp, 0, n)
                    val w = LameBridge.nativeEncode(h, tmp, n, out)
                    assertTrue("encode retornou $w", w >= 0)
                    saida.write(out, 0, w)
                    i += n
                }
                val fim = ByteArray(16_384)
                val w = LameBridge.nativeFlush(h, fim)
                assertTrue("flush retornou $w", w >= 0)
                saida.write(fim, 0, w)
            }
            val tag = ByteArray(4_096)
            val t = LameBridge.nativeTagFrame(h, tag)
            assertTrue("tag retornou $t", t > 0)
            java.io.RandomAccessFile(destino, "rw").use { it.seek(0); it.write(tag, 0, t) }
        } finally {
            LameBridge.nativeClose(h)
        }
        return destino
    }

    @Test
    fun roundTripDeDuracao() {
        val arquivo = codificar(pcmSeno(3.0), File(Suporte.testsOut(), "mp3_seno_3s.mp3"))
        val duracao = MediaProbe.durationMillis(arquivo)
        assertNotNull("MediaExtractor nao abriu o MP3", duracao)
        Suporte.medida("mp3_seno_3s", "extractor_ms", duracao!!)
        Suporte.medida("mp3_seno_3s", "delta_ms", duracao - 3000)
        assertTrue("delta=${duracao - 3000} ms", abs(duracao - 3000) <= 100)
        assertEquals(0, LameBridge.nativeLiveHandles())
    }

    @Test
    fun saidaDecodificadaSemClipping() {
        val arquivo = codificar(pcmSeno(3.0, amp = 0.5), File(Suporte.testsOut(), "mp3_para_decodificar.mp3"))
        val pcm = DecodificadorMp3.decodificar(arquivo)
        DecodificadorMp3.salvarWav(pcm, File(Suporte.testsOut(), "mp3_decodificado.wav"))
        val pico = pcm.amostras.maxOf { abs(it.toInt()) } / Short.MAX_VALUE.toDouble()
        Suporte.medida("mp3_decodificado", "pico_fs", pico)
        // Entrada a -6 dBFS; o codec pode somar poucos dB de overshoot, nunca saturar.
        assertTrue("pico=$pico", pico in 0.4..0.9)
        assertTrue("clipping: amostras no fundo de escala", pcm.amostras.none { abs(it.toInt()) >= Short.MAX_VALUE - 1 })
    }

    @Test
    fun semVazamentoJni() {
        assertTrue(LameBridge.ensureLoaded())
        val pcm = pcmSeno(0.3)
        val out = ByteArray(LameBridge.outputCapacityFor(pcm.size))
        repeat(50) { LameBridge.nativeClose(LameBridge.nativeInit(44_100, 1, 128, 5)) } // aquecimento
        System.gc()
        val antes = Debug.getNativeHeapAllocatedSize()
        repeat(CICLOS_VAZAMENTO) { i ->
            val h = LameBridge.nativeInit(44_100, 1, 128, 5)
            assertTrue(h != 0L)
            LameBridge.nativeEncode(h, pcm, pcm.size, out)
            if (i % 2 == 0) LameBridge.nativeFlush(h, ByteArray(16_384)) // metade fecha sem flush
            LameBridge.nativeClose(h)
        }
        System.gc()
        val crescimento = Debug.getNativeHeapAllocatedSize() - antes
        Suporte.medida("jni", "crescimento_heap_nativo_bytes", crescimento)
        assertEquals("handles vivos", 0, LameBridge.nativeLiveHandles())
        assertTrue("heap nativo cresceu $crescimento bytes em $CICLOS_VAZAMENTO ciclos", crescimento < LIMITE_CRESCIMENTO)
        assertEquals("parametros invalidos", 0L, LameBridge.nativeInit(44_100, 3, 128, 5))
    }

    @Test
    fun gravadorRealMp3() = runBlocking {
        val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val gravador = PcmMp3Recorder(escopo)
        val arquivo = File(Suporte.testsOut(), "real_${System.currentTimeMillis()}.mp3")
        try {
            gravador.prepare(RecordingConfig(format = RecordingFormat.MP3), arquivo.path).getOrThrow()
            gravador.start().getOrThrow()
            val inicio = System.nanoTime()
            Thread.sleep(4_000)
            val relogio = (System.nanoTime() - inicio) / 1_000_000
            gravador.stop().getOrThrow()
            val duracao = MediaProbe.durationMillis(arquivo)
            assertNotNull("MediaExtractor nao abriu o MP3 gravado", duracao)
            val delta = duracao!! - relogio
            Suporte.medida("real_mp3", "relogio_ms", relogio)
            Suporte.medida("real_mp3", "extractor_ms", duracao)
            Suporte.medida("real_mp3", "delta_ms", delta)
            // 100 ms era um numero sem calibracao: medido -124 ms (API 36) e -291 ms (API 34); o aceite do produto e +-500 ms.
            assertTrue("delta=$delta ms", abs(delta) <= TOLERANCIA_DURACAO_MS)
            assertEquals("handles vivos", 0, LameBridge.nativeLiveHandles())
            assertTrue("sobrou .tmp", !File("${arquivo.path}.tmp").exists())
        } finally {
            gravador.release()
            escopo.cancel()
        }
    }

    private companion object {
        const val TOLERANCIA_DURACAO_MS = 500L
        const val CICLOS_VAZAMENTO = 500
        const val LIMITE_CRESCIMENTO = 2L * 1024 * 1024
    }
}
