package com.gravasom.core.audio.android.recorder

import com.gravasom.core.audio.android.mp3.LameBridge
import com.gravasom.core.audio.engine.LevelMeter
import com.gravasom.core.domain.audio.AudioRecorder
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

/**
 * Grava MP3 (PCM + LAME via JNI). Escreve em `<saida>.tmp` e renomeia ao parar; o quadro
 * Xing/Info final sobrescreve o primeiro quadro para a duracao ficar correta no player.
 * Se a biblioteca nativa faltar, prepare() falha com Mp3NotAvailable, sem quebrar o app.
 */
class PcmMp3Recorder(
    private val scope: CoroutineScope,
    private val captureFactory: (RecordingConfig) -> PcmCapture = { cfg ->
        PcmCapture(cfg.sampleRateHz, cfg.channels, cfg.inputSource)
    },
) : AudioRecorder {

    override val format: RecordingFormat = RecordingFormat.MP3

    private val levelFlow = MutableStateFlow(LevelMeter.MIN_DB)
    private var handle: Long = 0L
    private var channels: Int = 1
    private var job: Job? = null
    private var capture: PcmCapture? = null
    private var file: RandomAccessFile? = null
    private var outputPath: String = ""

    /** Protege `handle` e `file`: a captura (thread de E/S) codifica e escreve; release()/stop() fecham. Sem isto, close() corria com nativeEncode. */
    private val encoderLock = Any()

    override suspend fun prepare(config: RecordingConfig, outputPath: String): Result<Unit> = runCatching {
        if (!LameBridge.ensureLoaded()) throw Mp3NotAvailable()
        handle = LameBridge.nativeInit(config.sampleRateHz, config.channels, config.bitrateKbps, QUALITY)
        if (handle == 0L) throw Mp3NotAvailable()
        this.outputPath = outputPath
        channels = config.channels
        val tmp = File("$outputPath.tmp")
        tmp.parentFile?.mkdirs()
        file = RandomAccessFile(tmp, "rw").also { it.setLength(0) }
        capture = captureFactory(config)
    }

    override suspend fun start(): Result<Unit> = runCatching {
        val cap = requireNotNull(capture) { "prepare() nao chamado" }
        val record = cap.open()
        record.startRecording()
        job = scope.launch(Dispatchers.IO) {
            val pcm = ShortArray(cap.bufferSizeBytes / 2)
            val mp3 = ByteArray(LameBridge.outputCapacityFor(pcm.size))
            while (isActive) {
                val read = record.read(pcm, 0, pcm.size)
                if (read > 0) {
                    levelFlow.value = LevelMeter.rmsDb(pcm.copyOf(read))
                    // read conta SHORTS (L+R intercalados); o LAME espera amostras por canal.
                    val frames = read / channels
                    synchronized(encoderLock) {
                        if (handle == 0L) return@launch
                        val written = LameBridge.nativeEncode(handle, pcm, frames, mp3)
                        if (written > 0) file?.write(mp3, 0, written)
                    }
                }
            }
        }
    }

    override suspend fun pause(): Result<Unit> = runCatching {
        stopWriter()
    }

    override suspend fun resume(): Result<Unit> = start()

    override suspend fun stop(): Result<String> = runCatching {
        stopWriter()
        synchronized(encoderLock) {
            val raf = requireNotNull(file) { "prepare() nao chamado" }
            val tail = ByteArray(FLUSH_BUFFER)
            val flushed = LameBridge.nativeFlush(handle, tail)
            if (flushed > 0) raf.write(tail, 0, flushed)
            val tag = ByteArray(TAG_BUFFER)
            val tagSize = LameBridge.nativeTagFrame(handle, tag)
            if (tagSize > 0) {
                raf.seek(0)
                raf.write(tag, 0, tagSize)
            }
            raf.close()
            file = null
            LameBridge.nativeClose(handle)
            handle = 0L
        }
        capture = null
        check(File("$outputPath.tmp").renameTo(File(outputPath))) { "falha ao finalizar $outputPath" }
        outputPath
    }

    override fun level(): Flow<Float> = levelFlow.asStateFlow()

    override fun release() {
        job?.cancel()
        capture?.close()
        // Entra na trava: se a captura esta dentro de nativeEncode, espera terminar antes do `delete` nativo (use-after-free).
        synchronized(encoderLock) {
            runCatching { file?.close() }
            file = null
            if (handle != 0L && LameBridge.ensureLoaded()) runCatching { LameBridge.nativeClose(handle) }
            handle = 0L
        }
    }

    /**
     * Para a captura e ESPERA o escritor terminar (mesmo padrao do PcmWavRecorder). `capture.close()` vem antes do join porque
     * destrava o `AudioRecord.read()` bloqueado. Sem a espera, pause()->resume() rodava dois jobs no mesmo encoder LAME.
     */
    private suspend fun stopWriter() {
        val writer = job
        writer?.cancel()
        capture?.close()
        withContext(Dispatchers.IO) { writer?.join() }
        job = null
    }

    private companion object {
        const val QUALITY = 5
        const val FLUSH_BUFFER = 16_384
        const val TAG_BUFFER = 4_096
    }
}

class Mp3NotAvailable : IllegalStateException("Codificador MP3 (LAME) indisponivel nesta build")
