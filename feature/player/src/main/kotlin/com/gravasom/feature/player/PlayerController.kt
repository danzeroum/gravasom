package com.gravasom.feature.player

import android.content.Context
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
 * Pular silencio: o plano ([SkipPlan], da analise feita ao salvar com o WebRTC VAD) e o relogio ficam no [PlaybackHub]
 * ([SilenceSkipper]); aqui so se configura o plano e se observa a posicao. Assim o pulo continua com o painel fechado.
 */
class PlayerController(context: Context) : PlayerPort {

    private val appContext: Context = context.applicationContext

    /** O ExoPlayer e unico no processo ([PlaybackHub]): o [PlaybackService] o expoe na notificacao de midia. */
    private val player: ExoPlayer = PlaybackHub.player(appContext)

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMillis = MutableStateFlow(0L)
    override val positionMillis: StateFlow<Long> = _positionMillis.asStateFlow()

    private val _durationMillis = MutableStateFlow(0L)
    override val durationMillis: StateFlow<Long> = _durationMillis.asStateFlow()

    private val _skippedMillis = MutableStateFlow(0L)

    override val skippedMillis: StateFlow<Long> = _skippedMillis.asStateFlow()

    private val onTick: () -> Unit = { refresh() }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
        }
    }

    init {
        player.addListener(listener)
        // O player pode ja estar tocando (a tela reabriu enquanto o audio seguia em segundo plano).
        _isPlaying.value = player.isPlaying
        _skippedMillis.value = PlaybackHub.skipper.skippedMillis
        PlaybackHub.addTickListener(onTick)
        refresh()
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
        PlaybackHub.skipper.reset()
        _durationMillis.value = 0L
        _positionMillis.value = 0L
        _skippedMillis.value = 0L
        PlaybackService.ensureRunning(appContext)
    }

    /** Define o plano de pulo e se ele vale (guardado no [PlaybackHub]: continua valendo com o painel fechado). */
    override fun setSkip(plan: SkipPlan?, enabled: Boolean) = PlaybackHub.skipper.configure(plan, enabled)

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

    /** Atualiza posicao, duracao e silencio pulado observaveis (o pulo em si e feito pelo relogio do [PlaybackHub]). */
    fun refresh() {
        _positionMillis.value = player.currentPosition
        _skippedMillis.value = PlaybackHub.skipper.skippedMillis
        if (player.duration > 0) _durationMillis.value = player.duration
    }

    /** Uma batida do relogio do hub agora (os testes de aparelho chamam isto em vez de esperar os 40 ms). */
    fun tick() = PlaybackHub.tick()

    /**
     * Solta SO este controlador (observadores; o relogio do pulo de silencio e do hub e segue). O ExoPlayer NAO e liberado nem pausado:
     * fechar o painel do player nao interrompe o audio; quem pausa e o usuario (botao ou notificacao de midia).
     */
    fun release() {
        PlaybackHub.removeTickListener(onTick)
        player.removeListener(listener)
    }
}
