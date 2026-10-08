package com.gravasom.core.domain.metrics

/**
 * Tipos de evento de uso (ADR-011). O esquema e FECHADO: um evento so carrega o tipo, um numero opcional e um rotulo que
 * e sempre um enum. Nao existe campo de texto livre, entao nome de arquivo, caminho, audio ou localizacao nao cabem.
 */
enum class UsageEventType {
    APP_ABERTO,
    GRAVACAO_INICIADA,
    TEMPO_ATE_GRAVAR,
    GRAVACAO_CONCLUIDA,
    PERMISSAO_NEGADA,
    BUSCA_USADA,
    REPRODUCAO_INICIADA,
    COMPARTILHADO,
    IMPORTADO,
    FORMATO_ALTERADO,
    NUMEROS_ABERTO,
}

/** Canais de compartilhamento (rotulo de COMPARTILHADO). */
enum class ShareChannel { FOLHA_DO_SISTEMA, SALVAR_EM }
