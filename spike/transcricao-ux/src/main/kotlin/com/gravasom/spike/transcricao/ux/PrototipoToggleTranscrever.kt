package com.gravasom.spike.transcricao.ux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Prototipo do toggle "Transcrever ao gravar".
 *
 * Decisao de design (a partir do ADR-016 secao 7): NAO e tela nova, e um campo do estado
 * MVI da tela Gravar (ADR-006). Aqui aparece isolado so para revisar o desenho.
 *
 * Regra de copy: sempre dizer que e offline, porque a promessa "o audio nao sai do aparelho"
 * e o diferencial do Gravasom e o usuario tem que saber disso ANTES de ligar.
 */
@Composable
fun PrototipoToggleTranscrever() {
    var ligado by remember { mutableStateOf(false) }
    var modeloBaixado by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Transcrever ao gravar", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "O texto fica no aparelho, sem internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = ligado, onCheckedChange = { ligado = it })
            }

            // Estado de erro quando o modelo nao esta baixado: nunca liga mudo.
            if (ligado && !modeloBaixado) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        "Baixe o modelo de portugues para comecar.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 6.dp).weight(1f),
                    )
                    TextButton(onClick = { /* abriria "Gerenciar modelos" */ }) { Text("Baixar") }
                }
            }

            // Botao so para ver o estado de modelo ausente no preview.
            TextButton(onClick = { modeloBaixado = !modeloBaixado }) {
                Text("Simular: modelo " + if (modeloBaixado) "baixado" else "ausente")
            }
        }
    }
}
