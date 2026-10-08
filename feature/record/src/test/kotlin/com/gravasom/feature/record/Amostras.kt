package com.gravasom.feature.record

import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.service.InputOption

/** Estados de exemplo da tela Gravar (iguais nos testes e nas capturas). */
internal object Amostras {
    val entrada = InputOption(1, "Microfone do aparelho")

    val pronto = RecordUiState(activeInput = entrada, format = RecordingFormat.M4A)

    val gravando = RecordUiState(
        status = RecordStatus.Recording, elapsedMillis = 12_000, levelDb = -22f, markerCount = 2,
        activeInput = entrada, format = RecordingFormat.M4A,
    )

    val pausado = gravando.copy(status = RecordStatus.Paused, levelDb = RecordUiState.MIN_DB)

    val salvando = gravando.copy(status = RecordStatus.Saving, levelDb = RecordUiState.MIN_DB)

    val salvo = pronto.copy(justSaved = true, elapsedMillis = 0)

    fun callbacks(
        onRecord: () -> Unit = {},
        onPause: () -> Unit = {},
        onResume: () -> Unit = {},
        onStop: () -> Unit = {},
        onMarker: () -> Unit = {},
    ) = RecordCallbacks(
        onRecord = onRecord, onPause = onPause, onResume = onResume, onStop = onStop, onMarker = onMarker,
        onFormat = {}, onInput = {}, onOpenSettings = {},
    )
}
