package com.gravasom.feature.player

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * O unico ExoPlayer do processo, com o relogio que faz o "pular silencio". A tela ([PlayerController]) e o [PlaybackService] usam a
 * MESMA instancia: por isso fechar o painel do player nao interrompe o audio, e a notificacao de midia controla o que a tela esta
 * tocando. O pulo de silencio tambem mora aqui (nao na tela): com o painel fechado ou o app em segundo plano ele continua pulando.
 */
object PlaybackHub {
    private const val POLL_MS = 40L

    @Volatile
    private var player: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private var skipTarget: SkipTarget? = null

    /** Plano e estado do pulo de silencio da gravacao que esta tocando; sobrevive ao painel (e ao controlador) da tela. */
    var skipper: SilenceSkipper = SilenceSkipper(NoTarget)
        private set

    private val poll = object : Runnable {
        override fun run() {
            tick()
            if (skipTarget?.isPlaying == true) handler.postDelayed(this, POLL_MS)
        }
    }

    private val playingListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            handler.removeCallbacks(poll)
            if (playing) handler.post(poll)
        }
    }

    /** Usar so na thread principal (como o proprio ExoPlayer). */
    fun player(context: Context): ExoPlayer = player ?: synchronized(this) {
        player ?: build(context.applicationContext).also {
            player = it
            install(ExoSkipTarget(it))
            it.addListener(playingListener)
        }
    }

    /** Uma batida do relogio: aplica o pulo (se ligado e numa lacuna) e avisa quem observa (a tela atualiza posicao e duracao). */
    fun tick() {
        skipper.tick()
        listeners.forEach { it() }
    }

    fun addTickListener(listener: () -> Unit) {
        listeners.addIfAbsent(listener)
        if (skipTarget?.isPlaying == true) {
            handler.removeCallbacks(poll)
            handler.post(poll)
        }
    }

    fun removeTickListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    /** Troca o alvo do pulo (so os testes usam um falso); o plano e o estado do pulo anteriores sao descartados. */
    internal fun install(target: SkipTarget) {
        skipTarget = target
        skipper = SilenceSkipper(target)
    }

    private fun build(app: Context): ExoPlayer = ExoPlayer.Builder(app)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private object NoTarget : SkipTarget {
        override val isPlaying = false
        override val positionMillis = 0L
        override fun seekTo(millis: Long) = Unit
    }
}

/** O ExoPlayer visto pelo [SilenceSkipper]. */
private class ExoSkipTarget(private val exo: ExoPlayer) : SkipTarget {
    override val isPlaying: Boolean get() = exo.isPlaying
    override val positionMillis: Long get() = exo.currentPosition
    override fun seekTo(millis: Long) = exo.seekTo(millis)
}

/**
 * Reproducao em segundo plano com notificacao de midia (pausar/retomar). A sessao envolve o [PlaybackHub]; ao terminar a
 * gravacao o servico se encerra. A sessao e liberada com o servico, o player nao (a tela continua usando-o).
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    /** Para os testes: a sessao publicada na notificacao de midia. */
    internal val mediaSession: MediaSession? get() = session

    internal val endListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = PlaybackHub.player(this)
        player.addListener(endListener)
        session = MediaSession.Builder(this, player).setId(SESSION_ID).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.run {
            player.removeListener(endListener)
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        const val SESSION_ID = "gravasom-playback"

        /** Garante o servico de pe quando a reproducao comeca; chamado com o app em primeiro plano. */
        fun ensureRunning(context: Context) {
            try {
                context.startService(Intent(context, PlaybackService::class.java))
            } catch (e: IllegalStateException) {
                // Sem permissao para iniciar em segundo plano: a tela continua tocando, so sem notificacao.
                Timber.w(e, "nao foi possivel iniciar o servico de reproducao")
            }
        }
    }
}
