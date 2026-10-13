package com.gravasom.feature.transcribe.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

/**
 * Cancelamento da transcricao pela notificacao. O botao "Cancelar" da notificacao manda um broadcast
 * com o id do trabalho; aqui o WorkManager cancela o trabalho e o worker marca CANCELLED no Room no
 * `finally` (comportamento ja testado em TranscriptionFlowTest).
 */
class CancelTranscriptionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val workId = intent.getStringExtra(TranscriptionNotification.EXTRA_WORK_ID) ?: return
        WorkManager.getInstance(context).cancelWorkById(java.util.UUID.fromString(workId))
    }
}
