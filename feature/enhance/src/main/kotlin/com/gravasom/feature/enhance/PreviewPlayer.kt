package com.gravasom.feature.enhance

import android.media.MediaPlayer
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Toca um trecho de previa do "Melhorar som": arquivo curto e local, sem notificacao de midia (nao passa pelo PlaybackService). */
interface PreviewPlayer {
    /** Toca [file] do inicio ao fim, avisando [onEnd] ao terminar; false se nao abriu. */
    suspend fun play(file: File, onEnd: () -> Unit): Boolean

    fun stop()
}

/** MediaPlayer puro: a previa e um WAV de ~20 s no cache privado. */
class MediaPlayerPreviewPlayer : PreviewPlayer {

    private var player: MediaPlayer? = null

    /** Preparacao em andamento: stop() a cancela, senao a coroutine de play() ficava suspensa para sempre (o listener nunca dispara apos release). */
    private var pending: CancellableContinuation<Unit>? = null

    override suspend fun play(file: File, onEnd: () -> Unit): Boolean {
        val mp = MediaPlayer()
        player = mp
        return try {
            mp.setDataSource(file.path)
            suspendCancellableCoroutine { cont ->
                pending = cont
                // `isActive`: depois de retomada, um erro tardio chamava resumeWithException de novo ("Already resumed" = crash).
                mp.setOnPreparedListener { if (cont.isActive) cont.resume(Unit) }
                mp.setOnErrorListener { _, _, _ ->
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("erro na previa"))
                    true
                }
                cont.invokeOnCancellation { runCatching { mp.release() } }
                mp.prepareAsync()
            }
            pending = null
            // Erro DURANTE a reproducao: encerra a previa em vez de deixar o estado "tocando" preso.
            mp.setOnErrorListener { _, _, _ ->
                stop()
                onEnd()
                true
            }
            mp.setOnCompletionListener { stop(); onEnd() }
            mp.start()
            true
        } catch (e: CancellationException) {
            pending = null
            throw e
        } catch (_: Exception) {
            pending = null
            stop()
            false
        }
    }

    override fun stop() {
        pending?.cancel()
        pending = null
        val mp = player ?: return
        player = null
        // Separados: MediaPlayer.stop() lanca em Idle/Initialized/Preparing e o release() (recurso nativo) nao podia ficar de fora.
        runCatching { mp.stop() }
        runCatching { mp.release() }
    }
}
