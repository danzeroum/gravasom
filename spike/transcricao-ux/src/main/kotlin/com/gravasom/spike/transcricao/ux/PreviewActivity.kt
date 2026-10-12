package com.gravasom.spike.transcricao.ux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Mostra os tres prototipos de UX da Fase 1 empilhados, para revisar em aparelho/emulador.
 * Sem logica de produto: e so desenho (ver docs/ux-validation.md para o roteiro de validacao).
 */
class PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Secao("1. Toggle \"Transcrever ao gravar\" (na tela Gravar)")
                        PrototipoToggleTranscrever()

                        Secao("2. Aba \"Transcricao\" no detalhe da gravacao")
                        PrototipoDetalheTranscricao()

                        Secao("3. Gerenciar modelos de idioma")
                        PrototipoGerenciarModelos()

                        Secao("4. Consentimento de primeira vez")
                        PrototipoConsentimento()
                    }
                }
            }
        }
    }
}

@Composable
private fun Secao(titulo: String) {
    Text(titulo, style = MaterialTheme.typography.titleMedium)
}
