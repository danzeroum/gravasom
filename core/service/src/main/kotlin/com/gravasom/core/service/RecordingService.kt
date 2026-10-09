package com.gravasom.core.service

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import com.gravasom.core.audio.android.InputRouting
import com.gravasom.core.audio.android.recorder.DefaultAudioRecorderFactory
import com.gravasom.core.audio.engine.RecordingError
import com.gravasom.core.audio.engine.RecordingEvent
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.audio.engine.RecordingStateMachine
import com.gravasom.core.audio.engine.TransitionResult
import com.gravasom.core.audio.engine.marker.MarkerSidecar
import com.gravasom.core.audio.engine.marker.PendingMarker
import com.gravasom.core.audio.engine.marker.RecordingClock
import com.gravasom.core.data.preferences.PreferencesRepository
import com.gravasom.core.domain.audio.AudioRecorder
import com.gravasom.core.domain.audio.AudioRecorderFactory
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.service.marker.Haptics
import com.gravasom.core.service.marker.SystemHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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

    /** Tempo de gravacao sem pausas = posicao no arquivo; cada marcador e gravado na hora num arquivo lateral. */
    private val clock = RecordingClock(SystemClock::elapsedRealtime)
    private var sidecar: MarkerSidecar? = null
    private var markerCount = 0
    private var lastMarkerMillis: Long? = null
    private val haptics: Haptics by lazy { SystemHaptics(this) }

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
            ACTION_MARKER -> addMarker()
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
        machine.transition(RecordingEvent.Start)
        RecordingServiceState.update(machine.state)
        focus?.request()
        startForegroundCompat()
        scope.launch {
            val prefs = PreferencesRepository(applicationContext)
            val format = formatName?.let { runCatching { RecordingFormat.valueOf(it) }.getOrNull() } ?: prefs.format.first()
            InputRouting.preferred = AudioInputs.deviceById(applicationContext, prefs.preferredInputId.first())
            val path = File(RecordingStorage.dir(this@RecordingService), "gravacao_${System.currentTimeMillis()}.${format.extension}").path
            sidecar = MarkerSidecar.forAudio(File(path)).also { it.delete() }
            markerCount = 0
            lastMarkerMillis = null
            RecordingServiceState.updateMarkerCount(0)
            val factory = DefaultRecorderFactoryHolder.factory ?: DefaultAudioRecorderFactory(scope)
            val rec = factory.create(format)
            recorder = rec
            val config = RecordingConfig(
                format = format,
                sampleRateHz = prefs.sampleRate.first(),
                bitrateKbps = prefs.bitrate.first(),
                channels = prefs.channels.first(),
            )
            val started = rec.prepare(config, path).mapCatching { rec.start().getOrThrow() }
            started
                .onSuccess {
                    machine.transition(RecordingEvent.Prepared)
                    RecordingServiceState.update(machine.state)
                    startedAtMillis = System.currentTimeMillis()
                    clock.start()
                    refreshNotification(paused = false)
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
            clock.pause()
            refreshNotification(paused = true)
            scope.launch { recorder?.pause() }
        }
    }

    private fun resume() {
        if (machine.transition(RecordingEvent.Resume) is TransitionResult.Ok) {
            RecordingServiceState.update(machine.state)
            clock.resume()
            refreshNotification(paused = false)
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
        clock.stop()
        scope.launch {
            val path: String = recorder?.stop()?.getOrNull() ?: ""
            recorder?.release()
            recorder = null
            if (path.isNotEmpty()) RecordingServiceState.emitSaved(path)
            if (cause != null) {
                machine.transition(RecordingEvent.Fail(cause))
            } else {
                machine.transition(RecordingEvent.Saved(path))
            }
            RecordingServiceState.update(machine.state)
            teardown()
        }
    }

    /** Marca o instante atual. Sem gravacao em curso a acao e ignorada (e o servico, se acordou so para isto, encerra). */
    private fun addMarker() {
        val recording = machine.state is RecordingState.Recording || machine.state is RecordingState.Paused
        val file = sidecar
        if (!recording || file == null) {
            if (machine.state == RecordingState.Idle) stopSelf()
            return
        }
        val position = clock.elapsedMillis()
        file.append(PendingMarker(position, label = ""))
        markerCount++
        lastMarkerMillis = position
        RecordingServiceState.updateMarkerCount(markerCount)
        haptics.tick()
        refreshNotification(paused = machine.state is RecordingState.Paused)
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
        val notification = notification(paused = false)
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

    private fun notification(paused: Boolean) =
        RecordingNotification.build(
            this, startedAtMillis.takeIf { it > 0 } ?: System.currentTimeMillis(), paused, ServiceActions(this),
            markerCount, lastMarkerMillis,
        )

    /** Atualiza a notificacao quando o estado muda (pausa/retomada/inicio efetivo). */
    private fun refreshNotification(paused: Boolean) {
        getSystemService(NotificationManager::class.java).notify(RecordingNotification.NOTIFICATION_ID, notification(paused))
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

/** Acoes reais da notificacao: cada botao reenvia a acao ao proprio servico; tocar na notificacao abre o app. */
private class ServiceActions(private val context: Context) : NotificationActions {
    override val openAppIntent: Intent =
        context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()

    private fun service(action: String, code: Int): PendingIntent = PendingIntent.getService(
        context, code, Intent(context, RecordingService::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE,
    )

    override fun resumePendingIntent(context: Context) = service(RecordingService.ACTION_RESUME, 1)
    override fun pausePendingIntent(context: Context) = service(RecordingService.ACTION_PAUSE, 2)
    override fun stopPendingIntent(context: Context) = service(RecordingService.ACTION_STOP, 3)
    override fun markerPendingIntent(context: Context) = service(RecordingService.ACTION_MARKER, 4)
}
