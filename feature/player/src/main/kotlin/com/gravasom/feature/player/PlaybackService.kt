package com.gravasom.feature.player

import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import timber.log.Timber

/**
 * O unico ExoPlayer do processo. A tela ([PlayerController]) e o [PlaybackService] usam a MESMA instancia: por isso fechar o
 * painel do player nao interrompe o audio, e a notificacao de midia controla o que a tela esta tocando.
 */
object PlaybackHub {
    @Volatile
    private var player: ExoPlayer? = null

    /** Usar so na thread principal (como o proprio ExoPlayer). */
    fun player(context: Context): ExoPlayer = player ?: synchronized(this) {
        player ?: build(context.applicationContext).also { player = it }
    }

    private fun build(app: Context): ExoPlayer = ExoPlayer.Builder(app)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
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
