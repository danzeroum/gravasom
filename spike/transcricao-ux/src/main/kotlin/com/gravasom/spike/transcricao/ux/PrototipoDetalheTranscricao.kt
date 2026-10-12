package com.gravasom.spike.transcricao.ux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Prototipo da aba "Transcricao" no detalhe. Tres estados que importam (ADR-016 secao 7):
 * em progresso, concluida, falha com "Tentar novamente".
 */
@Composable
fun PrototipoDetalheTranscricao() {
    var estado by remember { mutableIntStateOf(1) } // 0=rodando 1=ok 2=falha
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TabRow(selectedTabIndex = 1) {
            Tab(selected = false, onClick = {}, text = { Text("Onda") })
            Tab(selected = true, onClick = {}, text = { Text("Transcricao") })
            Tab(selected = false, onClick = {}, text = { Text("Marcadores") })
        }
        when (estado) {
            0 -> EstadoRodando()
            1 -> EstadoConcluido()
            else -> EstadoFalha()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { estado = 0 }, label = { Text("Ver rodando") })
            AssistChip(onClick = { estado = 1 }, label = { Text("Ver concluida") })
            AssistChip(onClick = { estado = 2 }, label = { Text("Ver falha") })
        }
    }
}

@Composable
private fun EstadoRodando() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Transcrevendo...", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { 0.42f }, modifier = Modifier.fillMaxWidth())
            Text("42% - restam ~3 min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EstadoConcluido() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Reuniao de equipe, 12 de outubro", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Bom dia a todos. Vamos comecar revisando os pontos da semana passada e depois passamos para os numeros do mes.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Copiar tudo") },
                )
                AssistChip(
                    onClick = {},
                    leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Exportar .txt") },
                )
            }
        }
    }
}

@Composable
private fun EstadoFalha() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Nao foi possivel transcrever. Espaco insuficiente.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            AssistChip(
                onClick = {},
                leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text("Tentar novamente") },
            )
        }
    }
}
