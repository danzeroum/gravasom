package com.gravasom.feature.enhance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gravasom.core.ui.theme.GravasomSpacing

/** Tela "Melhorar som" de uma gravacao (rota melhorar/{recordingId}). Sair cancela o que estiver rodando: nada fica pela metade. */
@Composable
fun EnhanceScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EnhanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onDone)
    Column(modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = GravasomSpacing.xs, vertical = GravasomSpacing.xs),
        ) {
            IconButton(onClick = onDone, modifier = Modifier.testTag("voltar_melhorar")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.enhance_back))
            }
            Text(stringResource(R.string.enhance_title), style = MaterialTheme.typography.titleLarge)
        }
        EnhanceContent(
            state,
            EnhanceCallbacks(
                onDenoise = viewModel::setDenoise,
                onDenoiseIntensity = viewModel::setDenoiseIntensity,
                onNormalize = viewModel::setNormalize,
                onTargetLufs = viewModel::setTargetLufs,
                onPreviewStart = viewModel::setPreviewStart,
                onPreview = viewModel::preview,
                onExport = viewModel::export,
                onCancelExport = viewModel::cancelExport,
                onDone = onDone,
            ),
            Modifier.weight(1f),
        )
    }
}
