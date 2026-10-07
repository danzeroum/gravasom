package com.gravasom

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.first
import androidx.test.core.app.ActivityScenario
import com.gravasom.core.service.library.RecordingFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Compartilhar (FileProvider), receber audio (ACTION_SEND), atalhos e ausencia de internet (jornada: Compartilhar). */
class EcossistemaTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun preparar() = UiSuporte.limparTudo()

    @Test
    fun compartilharUsaContentUriSemExporCaminhoBruto() {
        val gravacao = UiSuporte.inserirGravacao("Entrevista", segundos = 1.0)
        val intent = UiSuporte.app.files.shareIntent(gravacao)
        val envio = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: error("chooser sem intent de envio")
        assertEquals(Intent.ACTION_SEND, envio.action)
        val uri = envio.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: error("sem EXTRA_STREAM")
        assertEquals("content", uri.scheme)
        assertEquals(RecordingFiles.authority(Suporte.contexto), uri.authority)
        assertFalse("caminho bruto exposto: $uri", uri.toString().contains("/data/") || uri.toString().contains("/storage/"))
        assertEquals("audio/x-wav", "audio/x-wav".takeIf { gravacao.format.mimeType.contains("wav") } ?: gravacao.format.mimeType)
        assertTrue(envio.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        // o destino consegue ler os bytes pelo content://
        val lidos = Suporte.contexto.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertEquals(File(gravacao.path).length(), lidos.size.toLong())
    }

    @Test
    fun receberAudioDeOutroAppEntraNaListaEm3s() {
        val arquivo = Suporte.escreverWavSeno(File(Suporte.contexto.filesDir, "recordings/de_fora.wav"), 2.0)
        val uri = FileProvider.getUriForFile(Suporte.contexto, RecordingFiles.authority(Suporte.contexto), arquivo)
        val envio = Intent(Intent.ACTION_SEND).setClassName(Suporte.contexto.packageName, MainActivity::class.java.name)
            .setType("audio/x-wav").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val inicio = SystemClock.elapsedRealtime()
        ActivityScenario.launch<MainActivity>(envio).use {
            val ate = { SystemClock.elapsedRealtime() - inicio }
            var achou = false
            while (!achou && ate() < 10_000) {
                achou = kotlinx.coroutines.runBlocking {
                    UiSuporte.app.repo.observeAll().first().any { it.title == "de_fora" }
                }
                if (!achou) Thread.sleep(100)
            }
            Suporte.medida("receber_audio", "tempo_ate_aparecer_ms", ate())
            assertTrue("importacao nao apareceu", achou)
            assertTrue("H4: ${ate()} ms > 3000", ate() <= 3_000)
        }
    }

    @Test
    fun atalhosDoLauncherDeclarados() {
        val manager = Suporte.contexto.getSystemService(ShortcutManager::class.java)
        val ids = manager.manifestShortcuts.map { it.id }.toSet()
        assertEquals(setOf("gravar_agora", "minhas_gravacoes"), ids)
    }

    @Test
    fun appNaoPedePermissaoDeInternet() {
        val info = Suporte.contexto.packageManager.getPackageInfo(Suporte.contexto.packageName, PackageManager.GET_PERMISSIONS)
        val pedidas = info.requestedPermissions.orEmpty().toList()
        assertFalse("INTERNET declarada: $pedidas", pedidas.contains("android.permission.INTERNET"))
    }

    @Test
    fun activityAceitaEnvioDeAudio() {
        val sonda = Intent(Intent.ACTION_SEND).setType("audio/mp4").setPackage(Suporte.contexto.packageName)
        val alvos = Suporte.contexto.packageManager.queryIntentActivities(sonda, 0)
        assertTrue(alvos.any { it.activityInfo.name == MainActivity::class.java.name })
        @Suppress("UNUSED_VARIABLE") val componente = ComponentName(Suporte.contexto, MainActivity::class.java)
    }
}
