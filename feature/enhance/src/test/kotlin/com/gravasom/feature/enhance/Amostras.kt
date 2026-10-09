package com.gravasom.feature.enhance

import com.gravasom.core.service.enhance.EnhanceSettings

/** Estados de exemplo da tela "Melhorar som" (iguais nos testes e nas capturas). */
internal object Amostras {

    private const val TITULO = "Aula de física"

    /** Pronto, nada ligado (gravacao sem preset): o botao de salvar comeca desabilitado. */
    val pronto = EnhanceUiState(
        status = EnhanceStatus.READY,
        title = TITULO,
        durationMillis = 372_000,
        settings = EnhanceSettings(denoise = false, denoiseIntensity = 0f, normalize = false, targetLufs = -16f),
        previewMillis = 20_000,
    )

    /** Ajustado: redutor a 40% (padrao do ADR-15) e volume em -16 LUFS. */
    val ajustado = pronto.copy(settings = EnhanceSettings(denoise = true, denoiseIntensity = 0.4f, normalize = true, targetLufs = -16f))

    /** Exportando no meio (42%): progresso e botao de cancelar. */
    val exportando = ajustado.copy(status = EnhanceStatus.EXPORTING, progress = 0.42f)

    /** Copia salva: titulo com sufixo e botao de concluir. */
    val salvo = pronto.copy(
        status = EnhanceStatus.SAVED,
        settings = ajustado.settings,
        savedTitle = "$TITULO (melhorada)",
    )

    /** Previa tocando o lado B. */
    val previaB = ajustado.copy(previewing = EnhanceSide.B, previewStartMillis = 63_000)

    fun callbacks() = EnhanceCallbacks()
}
