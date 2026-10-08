package com.gravasom.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/** Constroi a notificacao persistente com pausar/retomar/parar/marcador. */
object RecordingNotification {

    const val CHANNEL_ID = "gravasom_recording"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Notificacao da gravacao. Gravando: cronometro nativo (base em [startedAtMillis]) + Pausar + Parar.
     * Pausada: sem cronometro + Retomar + Parar. Todas as acoes sao reais (PendingIntent para o RecordingService).
     */
    fun build(context: Context, startedAtMillis: Long, paused: Boolean, actions: NotificationActions): Notification {
        val contentIntent = PendingIntent.getActivity(
            context, 0, actions.openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(context.getString(if (paused) R.string.notif_title_paused else R.string.notif_title_recording))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (!paused) builder.setUsesChronometer(true).setWhen(startedAtMillis)
        if (paused) {
            builder.addAction(0, context.getString(R.string.notif_action_resume), actions.resumePendingIntent(context))
        } else {
            builder.addAction(0, context.getString(R.string.notif_action_pause), actions.pausePendingIntent(context))
        }
        builder.addAction(0, context.getString(R.string.notif_action_stop), actions.stopPendingIntent(context))
        return builder.build()
    }
}

/** Intents das acoes da notificacao. */
interface NotificationActions {
    val openAppIntent: Intent
    fun resumePendingIntent(context: Context): PendingIntent
    fun pausePendingIntent(context: Context): PendingIntent
    fun stopPendingIntent(context: Context): PendingIntent
}
