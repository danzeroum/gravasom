package com.gravasom

import android.os.Environment
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * API 26-28: sem MediaStore com RELATIVE_PATH. A copia publica vai para Music/Recordings por WRITE_EXTERNAL_STORAGE
 * (manifesto com maxSdkVersion=28). VERIFICACAO em emulador; o app Arquivos de um aparelho real fica no roteiro humano.
 */
@SdkSuppress(maxSdkVersion = 28)
class PublicacaoApi28Test {

    @get:Rule(order = 0)
    val permissoes = UiSuporte.permissoes()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<com.gravasom.MainActivity>()

    @Before
    fun preparar() = UiSuporte.limparTudo()

    @Test
    fun gravar10sPublicaEmMusicRecordingsVisivelParaOutrosApps() {
        val (_, gravadoMs) = UiSuporte.gravarPelaTela(compose, segundos = 10)
        Thread.sleep(2_000)
        val pub = checkNotNull(UiSuporte.publicadoMaisRecente("m4a")) { "nada publicado no MediaStore (API legada)" }
        Suporte.medida("api_legada_m4a", "pasta", pub.pasta)
        Suporte.medida("api_legada_m4a", "duracao_ms", pub.duracaoMs)
        assertEquals("Music/Recordings/", pub.pasta)
        assertTrue("duracao ${pub.duracaoMs} ms x relogio $gravadoMs ms", kotlin.math.abs(pub.duracaoMs - gravadoMs) <= 500)

        // O arquivo existe de fato na pasta publica que o app Arquivos lista.
        @Suppress("DEPRECATION")
        val pasta = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Recordings")
        assertTrue("arquivo ausente em ${pasta.path}: ${pasta.list()?.toList()}", File(pasta, pub.nome).isFile)
    }

    @Test
    fun excluirDeVezRemoveACopiaPublica() {
        UiSuporte.gravarPelaTela(compose, segundos = 2)
        Thread.sleep(2_000)
        val pub = checkNotNull(UiSuporte.publicadoMaisRecente("m4a"))
        @Suppress("DEPRECATION")
        val arquivo = File(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Recordings"), pub.nome)
        assertTrue(arquivo.isFile)
        UiSuporte.limparTudo() // apaga de vez: linha do Room, arquivo do app e copia publica
        Thread.sleep(500)
        assertTrue("copia publica sobrou: ${arquivo.path}", !arquivo.exists())
        assertNotNull(compose.activity)
    }
}
