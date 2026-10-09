package com.gravasom.feature.enhance

/** Tudo que a tela "Melhorar som" pede ao resto do app (agrupado numa so peça, como RecordCallbacks). */
data class EnhanceCallbacks(
    val onBack: () -> Unit = {},
    val onDenoise: (Boolean) -> Unit = {},
    val onDenoiseIntensity: (Float) -> Unit = {},
    val onNormalize: (Boolean) -> Unit = {},
    val onTargetLufs: (Float) -> Unit = {},
    val onPreviewStart: (Long) -> Unit = {},
    val onPreview: (EnhanceSide) -> Unit = {},
    val onExport: () -> Unit = {},
    val onCancelExport: () -> Unit = {},
    val onDone: () -> Unit = {},
)
