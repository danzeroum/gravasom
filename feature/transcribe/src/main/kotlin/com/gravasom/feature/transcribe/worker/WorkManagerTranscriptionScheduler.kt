package com.gravasom.feature.transcribe.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gravasom.core.domain.transcript.TranscriptionLang
import com.gravasom.core.domain.transcript.TranscriptionScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacao do [TranscriptionScheduler] sobre WorkManager (ADR-016 secao 4).
 *
 * Cada (gravacao, idioma) tem um trabalho unico; enfileirar de novo substitui (REPLACE),
 * entao tocar duas vezes no botao de tentar de novo nao cria fila duplicada.
 *
 * Sem restricao de rede: a transcricao e offline. Sem carregador obrigatorio: o usuario
 * escolheu transcrever; a UI avisa se a bateria esta baixa (o worker nao impoe).
 */
@Singleton
class WorkManagerTranscriptionScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : TranscriptionScheduler {

    override fun enqueue(recordingId: Long, lang: TranscriptionLang) {
        val dados = Data.Builder()
            .putLong(TranscriptionWorker.KEY_RECORDING, recordingId)
            .putString(TranscriptionWorker.KEY_LANG, lang.tag)
            .build()
        val pedido = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(dados)
            .setConstraints(Constraints.NONE)
            .addTag(tag(recordingId))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(nomeUnico(recordingId, lang), ExistingWorkPolicy.REPLACE, pedido)
    }

    override fun cancel(recordingId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag(recordingId))
    }

    private fun tag(recordingId: Long): String = "${TranscriptionWorker.UNIQUE_PREFIX}$recordingId"

    private fun nomeUnico(recordingId: Long, lang: TranscriptionLang): String =
        "${TranscriptionWorker.UNIQUE_PREFIX}${recordingId}_${lang.tag}"
}
