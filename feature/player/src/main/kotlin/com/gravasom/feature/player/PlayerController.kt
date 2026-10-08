package com.gravasom.feature.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.gravasom.core.audio.engine.analysis.SkipPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Envolve o ExoPlayer e expoe estado observavel para a UI. Usar so na thread principal.
 *
 * Reproducao em segundo plano: o ExoPlayer vive no [PlaybackHub] e o [PlaybackService] o publica como sessao de midia.
 *
 * Pular silencio: com um [SkipPlan] e o pulo ligado, a posicao e conferida a cada [POLL_MS] enquanto toca; ao entrar numa
 * lacuna o player salta para o fim dela. O plano vem da analise feita ao salvar (WebRTC VAD), nunca do nivel do VU.
 */
class PlayerController(context: Context) : PlayerPort {

    private val appContext: Context = context.applicationContext

    /** O ExoPlayer e unico no processo ([PlaybackHub]): o [PlaybackService] o expoe na notificacao de midia. */
    private val player: ExoPlayer = PlaybackHub.player(appContext)
    private val handler = Handler(Looper.getMainLooper())

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMillis = MutableStateFlow(0L)
    override val positionMillis: StateFlow<Long> = _positionMillis.asStateFlow()

    private val _durationMillis = MutableStateFlow(0L)
    override val durationMillis: StateFlow<Long> = _durationMillis.asStateFlow()

    private val _skippedMillis = MutableStateFlow(0L)

    override val skippedMillis: StateFlow<Long> = _skippedMillis.asStateFlow()

    private var plan: SkipPlan? = null
    private var skipEnabled = false

    private val poll = object : Runnable {
        override fun run() {
            tick()
            if (player.isPlaying) handler.postDelayed(this, POLL_MS)
        }
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            handler.removeCallbacks(poll)
            if (playing) handler.post(poll)
        }
    }

    init {
        player.addListener(listener)
        // O player pode ja estar tocando (a tela reabriu enquanto o audio seguia em segundo plano).
        _isPlaying.value = player.isPlaying
        if (player.isPlaying) handler.post(poll)
    }

    /**
     * Carrega a gravacao. [title] aparece na notificacao de midia; null para gravacao privada (nunca vaza o nome na tela
     * bloqueada: a notificacao mostra um titulo generico).
     */
    fun load(path: String, title: String? = null) {
        val metadata = MediaMetadata.Builder()
            .setTitle(title ?: appContext.getString(R.string.player_notification_private_title))
            .build()
        player.setMediaItem(MediaItem.Builder().setUri(path).setMediaMetadata(metadata).build())
        player.prepare()
        _durationMillis.value = 0L
        _positionMillis.value = 0L
        _skippedMillis.value = 0L
        PlaybackService.ensureRunning(appContext)
    }

    /** Define o plano de pulo e se ele vale. Pode ser chamado a qualquer momento (ex.: usuario liga o pulo tocando). */
    override fun setSkip(plan: SkipPlan?, enabled: Boolean) {
        this.plan = plan
        this.skipEnabled = enabled
    }

    override fun play() = player.play()
    override fun pause() = player.pause()
    override fun seekTo(millis: Long) {
        player.seekTo(millis.coerceAtLeast(0L))
        _positionMillis.value = millis.coerceAtLeast(0L)
    }

    override fun seekToAndPlay(millis: Long) {
        seekTo(millis)
        player.play()
    }

    override fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
    }

    /** Atualiza a posicao observavel e aplica o pulo se estiver numa lacuna. */
    fun tick() {
        var position = player.currentPosition
        if (skipEnabled && player.isPlaying) {
            val target = plan?.skipTarget(position)
            if (target != null && target > position) {
                _skippedMillis.value += target - position
                player.seekTo(target)
                position = target
            }
        }
        _positionMillis.value = position
        if (player.duration > 0) _durationMillis.value = player.duration
    }

    /**
     * Solta SO este controlador (observadores e o relogio do pulo de silencio). O ExoPlayer NAO e liberado nem pausado:
     * fechar o painel do player nao interrompe o audio; quem pausa e o usuario (botao ou notificacao de midia).
     */
    fun release() {
        handler.removeCallbacks(poll)
        player.removeListener(listener)
    }

    private companion object {
        const val POLL_MS = 40L
    }
}
