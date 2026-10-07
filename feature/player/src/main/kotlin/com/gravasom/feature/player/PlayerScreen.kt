package com.gravasom.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PlayerScreen(
    controller: PlayerController,
    title: String,
    onTrim: (Long, Long) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val position by controller.positionMillis.collectAsStateWithLifecycle()
    val duration by controller.durationMillis.collectAsStateWithLifecycle()
    var speed by remember { mutableFloatStateOf(1f) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text("${position / 1000}s / ${duration / 1000}s")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (isPlaying) controller.pause() else controller.play() }) {
                Text(if (isPlaying) "Pausar" else "Tocar")
            }
            Button(onClick = onShare) { Text("Compartilhar") }
            Button(onClick = onDelete) { Text("Excluir") }
        }
        Text("Velocidade: ${'$'}speed x")
        Slider(
            value = speed,
            onValueChange = { speed = it; controller.setSpeed(it) },
            valueRange = 0.5f..2f,
            steps = 2,
        )
        Button(onClick = { onTrim(position, duration) }) { Text("Cortar de aqui ate o fim") }
    }
}
