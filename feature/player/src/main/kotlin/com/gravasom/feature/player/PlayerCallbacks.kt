package com.gravasom.feature.player

import com.gravasom.core.audio.engine.analysis.SilenceSensitivity
import com.gravasom.core.domain.marker.Marker

/** Marcadores: criar na posicao atual, editar um existente, selecionar do marcador ate o proximo. */
class MarkerCallbacks(
    val onAdd: (positionMillis: Long) -> Unit,
    val onEdit: (Marker) -> Unit,
    val onSelectToNext: (Marker) -> Unit,
)

/** Trecho: ligar/desligar a selecao, mudar as pontas e exportar. */
class SelectionCallbacks(
    val onToggle: (on: Boolean, positionMillis: Long) -> Unit,
    val onChange: (startMillis: Long, endMillis: Long) -> Unit,
    val onExport: () -> Unit,
)

/** Pular silencio: ligar/desligar e escolher a sensibilidade. */
class SkipCallbacks(
    val onEnabled: (Boolean) -> Unit,
    val onSensitivity: (SilenceSensitivity) -> Unit,
)

/** Tudo que o painel do player pede ao resto do app. */
class PlayerCallbacks(
    val onClose: () -> Unit,
    val onEditTags: () -> Unit,
    val markers: MarkerCallbacks,
    val selection: SelectionCallbacks,
    val skip: SkipCallbacks,
)
