package com.gravasom.core.domain.transcript

/**
 * Ponto de entrada para enfileirar uma transcricao. O `RecordingIngestor` (em `core:service`) chama
 * este contrato quando uma gravacao termina e o "Transcrever ao gravar" esta ligado; a implementacao
 * concreta (WorkManager) fica em `feature:transcribe` e e injetada via Hilt. Isso mantem `core:service`
 * ignorante de WorkManager e da feature (dependencia so aponta para o dominio).
 */
interface TranscriptionScheduler {

    /**
     * Enfileira a transcricao da gravacao [recordingId] no idioma [lang]. Idempotente por (gravacao, idioma):
     * chamar de novo para o mesmo par substitui o trabalho pendente (UniqueWork REPLACE).
     */
    fun enqueue(recordingId: Long, lang: TranscriptionLang)

    /** Cancela o trabalho pendente da gravacao (se houver). Idempotente. */
    fun cancel(recordingId: Long)

    /** Sem efeito: usado onde a transcricao nao existe (testes de outras features, builds sem a feature). */
    object NoOp : TranscriptionScheduler {
        override fun enqueue(recordingId: Long, lang: TranscriptionLang) = Unit
        override fun cancel(recordingId: Long) = Unit
    }
}
