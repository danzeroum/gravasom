package com.gravasom.core.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.gravasom.core.audio.android.recorder.DefaultAudioRecorderFactory
import com.gravasom.core.audio.engine.RecordingError
import com.gravasom.core.audio.engine.RecordingEvent
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.audio.engine.RecordingStateMachine
import com.gravasom.core.audio.engine.TransitionResult
import com.gravasom.core.domain.audio.AudioRecorder
import com.gravasom.core.domain.audio.AudioRecorderFactory
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

/**
 * Servico em primeiro plano (type=microphone). Detem a maquina de estados, o
 * gravador e o foco de audio. Sobrevive a tela bloqueada e a ligacoes.
 */
class RecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val machine = RecordingStateMachine()
    private var recorder: AudioRecorder? = null
    private var focus: AudioFocusController? = null
    private var startedAtMillis: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        RecordingNotification.ensureChannel(this)
        focus = AudioFocusController(
            context = this,
            onPause = { pause() },
            onStopWithError = { stopRecording(RecordingError.MIC_IN_USE) },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent.getStringExtra(EXTRA_FORMAT))
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_MARKER -> Unit
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(formatName: String?) {
        // Primeiro plano com tipo microfone exige RECORD_AUDIO concedida; sem ela o sistema lanca SecurityException.
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            RecordingServiceState.error(RecordingError.PERMISSION_DENIED)
            stopSelf()
            return
        }
        val format = runCatching { RecordingFormat.valueOf(formatName ?: "M4A") }
            .getOrDefault(RecordingFormat.M4A)
        machine.transition(RecordingEvent.Start)
        RecordingServiceState.update(machine.state)
        focus?.request()
        startForegroundCompat()
        scope.launch {
            val path = File(RecordingStorage.dir(this@RecordingService), "gravacao_${System.currentTimeMillis()}.${format.extension}").path
            val factory = DefaultRecorderFactoryHolder.factory ?: DefaultAudioRecorderFactory(scope)
            val rec = factory.create(format)
            recorder = rec
            val started = rec.prepare(RecordingConfig(format = format), path).mapCatching { rec.start().getOrThrow() }
            started
                .onSuccess {
                    machine.transition(RecordingEvent.Prepared)
                    RecordingServiceState.update(machine.state)
                    startedAtMillis = System.currentTimeMillis()
                    observeLevel(rec)
                }
                .onFailure {
                    machine.transition(RecordingEvent.Fail(RecordingError.ENCODER_FAILURE))
                    RecordingServiceState.update(machine.state)
                }
        }
    }

    private fun pause() {
        if (machine.transition(RecordingEvent.Pause) is TransitionResult.Ok) {
            RecordingServiceState.update(machine.state)
            scope.launch { recorder?.pause() }
        }
    }

    private fun resume() {
        if (machine.transition(RecordingEvent.Resume) is TransitionResult.Ok) {
            RecordingServiceState.update(machine.state)
            scope.launch { recorder?.resume() }
        }
    }

    /** Para e finaliza o arquivo; com [cause], sinaliza o erro depois de preservar o que foi gravado. */
    private fun stopRecording(cause: RecordingError? = null) {
        if (machine.transition(RecordingEvent.Stop) !is TransitionResult.Ok) {
            // Sem gravacao em curso (ex.: erro na partida): so desfaz o primeiro plano.
            if (machine.state is RecordingState.Error || machine.state == RecordingState.Idle) teardown()
            return
        }
        RecordingServiceState.update(machine.state)
        scope.launch {
            val path: String = recorder?.stop()?.getOrNull() ?: ""
            recorder?.release()
            recorder = null
            if (cause != null) {
                machine.transition(RecordingEvent.Fail(cause))
            } else {
                machine.transition(RecordingEvent.Saved(path))
            }
            RecordingServiceState.update(machine.state)
            teardown()
        }
    }

    private fun teardown() {
        recorder?.release()
        recorder = null
        focus?.abandon()
        stopForegroundCompat()
        stopSelf()
    }

    private fun observeLevel(rec: AudioRecorder) {
        rec.level().onEach { db -> RecordingServiceState.updateLevel(db) }.launchIn(scope)
    }

    private fun startForegroundCompat() {
        val notification = RecordingNotification.build(
            context = this,
            elapsedLabel = "00:00",
            paused = false,
            actions = NoopActions,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                RecordingNotification.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(RecordingNotification.NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        recorder?.release()
        focus?.abandon()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.gravasom.action.START"
        const val ACTION_PAUSE = "com.gravasom.action.PAUSE"
        const val ACTION_RESUME = "com.gravasom.action.RESUME"
        const val ACTION_MARKER = "com.gravasom.action.MARKER"
        const val ACTION_STOP = "com.gravasom.action.STOP"
        const val EXTRA_FORMAT = "extra_format"
    }
}

/** Ponte temporaria para a factory; substituida por injecao Hilt na Fase 5. */
object DefaultRecorderFactoryHolder {
    var factory: AudioRecorderFactory? = null
}

/** Acoes vazias enquanto a UI nao liga os PendingIntents reais. */
private object NoopActions : NotificationActions {
    override val openAppIntent: Intent = Intent()
    override fun resumePendingIntent(context: android.content.Context) =
        android.app.PendingIntent.getService(context, 0, Intent(), android.app.PendingIntent.FLAG_IMMUTABLE)
    override fun pausePendingIntent(context: android.content.Context) =
        android.app.PendingIntent.getService(context, 1, Intent(), android.app.PendingIntent.FLAG_IMMUTABLE)
    override fun markerPendingIntent(context: android.content.Context) =
        android.app.PendingIntent.getService(context, 2, Intent(), android.app.PendingIntent.FLAG_IMMUTABLE)
    override fun stopPendingIntent(context: android.content.Context) =
        android.app.PendingIntent.getService(context, 3, Intent(), android.app.PendingIntent.FLAG_IMMUTABLE)
}
