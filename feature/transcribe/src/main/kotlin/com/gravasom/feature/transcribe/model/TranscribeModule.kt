package com.gravasom.feature.transcribe.model

import com.gravasom.core.domain.transcript.ModelManager
import com.gravasom.core.domain.transcript.Transcriber
import com.gravasom.core.domain.transcript.TranscriptionScheduler
import com.gravasom.feature.transcribe.worker.WorkManagerTranscriptionScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Liga os contratos de dominio as implementacoes (ADR-016 secao 1).
 * Trocar o motor e so mudar a ligacao de [Transcriber]; nada da UI nem do worker muda.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TranscribeModule {

    @Binds
    @Singleton
    abstract fun bindModelManager(impl: AndroidModelManager): ModelManager

    @Binds
    @Singleton
    abstract fun bindTranscriber(impl: WhisperCppTranscriber): Transcriber

    @Binds
    @Singleton
    abstract fun bindScheduler(impl: WorkManagerTranscriptionScheduler): TranscriptionScheduler
}
