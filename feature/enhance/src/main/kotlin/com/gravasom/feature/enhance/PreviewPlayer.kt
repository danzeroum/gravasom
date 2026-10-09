package com.gravasom.feature.enhance

import android.media.MediaPlayer
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

    override suspend fun play(file: File, onEnd: () -> Unit): Boolean = try {
        val mp = MediaPlayer()
        player = mp
        mp.setDataSource(file.path)
        suspendCancellableCoroutine { cont ->
            mp.setOnPreparedListener { cont.resume(Unit) }
            mp.setOnErrorListener { _, _, _ -> cont.resumeWithException(IllegalStateException("erro na previa")); true }
            cont.invokeOnCancellation { runCatching { mp.release() } }
            mp.prepareAsync()
        }
        mp.setOnCompletionListener { stop(); onEnd() }
        mp.start()
        true
    } catch (_: Exception) {
        stop()
        false
    }

    override fun stop() {
        val mp = player ?: return
        runCatching {
            mp.stop()
            mp.release()
        }
        player = null
    }
}
