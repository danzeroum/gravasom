package com.gravasom.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/** Constroi a notificacao persistente com pausar/retomar/parar/marcador. */
object RecordingNotification {

    const val CHANNEL_ID = "gravasom_recording"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gravacao em andamento",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Mostra a gravacao ativa e seus controles"
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun build(
        context: Context,
        elapsedLabel: String,
        paused: Boolean,
        actions: NotificationActions,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            actions.openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(if (paused) "Gravacao pausada" else "Gravando")
            .setContentText(elapsedLabel)
            .setOngoing(!paused)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (paused) {
            builder.addAction(0, "Retomar", actions.resumePendingIntent(context))
        } else {
            builder.addAction(0, "Pausar", actions.pausePendingIntent(context))
        }
        builder.addAction(0, "Marcador", actions.markerPendingIntent(context))
        builder.addAction(0, "Parar", actions.stopPendingIntent(context))
        return builder.build()
    }
}

/** Intents das acoes da notificacao. */
interface NotificationActions {
    val openAppIntent: Intent
    fun resumePendingIntent(context: Context): PendingIntent
    fun pausePendingIntent(context: Context): PendingIntent
    fun markerPendingIntent(context: Context): PendingIntent
    fun stopPendingIntent(context: Context): PendingIntent
}
