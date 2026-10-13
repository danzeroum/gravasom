package com.gravasom.feature.transcribe.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.gravasom.feature.transcribe.R

/**
 * Notificacao do worker de transcricao, com botao "Cancelar" que cancela o trabalho de verdade
 * (o receiver chama WorkManager.cancelWorkById). Canal separado do canal de gravacao para o usuario
 * poder silenciar a transcricao sem silenciar a gravacao.
 */
object TranscriptionNotification {

    const val CHANNEL_ID = "gravasom_transcription"
    const val NOTIFICATION_ID = 2001
    const val ACAO_CANCELAR = "com.gravasom.action.CANCEL_TRANSCRIPTION"
    const val EXTRA_WORK_ID = "workId"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.transcribe_notif_canal),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.transcribe_notif_texto)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Notificacao de progresso do work [workId]. */
    fun build(context: Context, workId: String, openAppIntent: Intent?): Notification {
        val contentIntent = openAppIntent?.let {
            PendingIntent.getActivity(
                context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        val cancelar = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, CancelTranscriptionReceiver::class.java).setAction(ACAO_CANCELAR).putExtra(EXTRA_WORK_ID, workId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(context.getString(R.string.transcribe_notif_titulo))
            .setContentText(context.getString(R.string.transcribe_notif_texto))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, context.getString(R.string.transcribe_notif_cancelar), cancelar)
        if (contentIntent != null) builder.setContentIntent(contentIntent)
        return builder.build()
    }

    /** ForegroundInfo com tipo dataSync (obrigatorio para foreground a partir do Android 14). */
    fun foregroundInfo(context: Context, workId: String, openAppIntent: Intent?): ForegroundInfo =
        ForegroundInfo(NOTIFICATION_ID, build(context, workId, openAppIntent), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
}
