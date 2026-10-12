package com.gravasom.spike.transcricao.ux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class LinhaModelo(
    val idioma: String,
    val tamanho: String,
    val estado: EstadoModelo,
)

private enum class EstadoModelo { Baixado, Ausente, Baixando }

/**
 * Prototipo da tela "Gerenciar modelos de idioma" (Ajustes -> Transcricao).
 *
 * Regra de copy (ADR-016 secao 2): o app NAO baixa sozinho e NAO usa INTERNET do app — o
 * usuario confirma, baixa por Wi-Fi e o arquivo e verificado por SHA-256. A UI diz isso.
 */
@Composable
fun PrototipoGerenciarModelos() {
    val linhas = listOf(
        LinhaModelo("Portugues (Brasil)", "57 MB", EstadoModelo.Baixando),
        LinhaModelo("Ingles (Estados Unidos)", "57 MB", EstadoModelo.Baixado),
        LinhaModelo("Espanhol (Espanha)", "57 MB", EstadoModelo.Ausente),
        LinhaModelo("Frances (Franca)", "57 MB", EstadoModelo.Ausente),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Baixe uma vez; depois a transcricao funciona sem internet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        linhas.forEach { Linha(it) }
    }
}

@Composable
private fun Linha(linha: LinhaModelo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(linha.idioma, style = MaterialTheme.typography.titleMedium)
                Text("Modelo ~${linha.tamanho}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (linha.estado == EstadoModelo.Baixando) {
                    LinearProgressIndicator(progress = { 0.35f }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
            }
            when (linha.estado) {
                EstadoModelo.Baixado -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Baixado", tint = MaterialTheme.colorScheme.primary)
                EstadoModelo.Ausente -> AssistChip(
                    onClick = {},
                    leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Baixar") },
                )
                EstadoModelo.Baixando -> AssistChip(onClick = {}, label = { Text("Cancelar") })
            }
        }
    }
}
