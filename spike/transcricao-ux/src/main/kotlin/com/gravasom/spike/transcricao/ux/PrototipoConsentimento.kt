package com.gravasom.spike.transcricao.ux

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Prototipo do dialogo de consentimento de primeira vez (ADR-016 secao 7).
 *
 * Regra: dizer em uma frase que o audio e o texto NUNCA saem do aparelho, porque essa
 * e a promessa do Gravasom. Sem promessa vaga; sem letra miuda.
 */
@Composable
fun PrototipoConsentimento() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Transcrever sem internet") },
        text = {
            Text(
                "A transcricao acontece dentro do aparelho. O audio e o texto nao sao enviados para lugar nenhum.\n\n" +
                    "Para comecar, o Gravasom baixa uma vez o modelo do idioma escolhido (~57 MB) por Wi-Fi. Depois disso, funciona offline para sempre.",
            )
        },
        confirmButton = { TextButton(onClick = {}) { Text("Baixar modelo de portugues") } },
        dismissButton = { TextButton(onClick = {}) { Text("Agora nao") } },
    )
}
