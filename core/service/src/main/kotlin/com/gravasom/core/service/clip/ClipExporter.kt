package com.gravasom.core.service.clip

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.gravasom.core.audio.engine.edit.ClipResult
import com.gravasom.core.audio.engine.edit.WavClipper
import com.gravasom.core.domain.audio.RecordingFormat
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Exporta um trecho como arquivo NOVO no mesmo formato, SEM reencodar (sem perda de qualidade e instantaneo):
 * WAV copia bytes; M4A recopia os quadros AAC para um MP4 novo; MP3 recopia os quadros. O corte cai no limite do
 * quadro (~23 ms no AAC, ~26 ms no MP3). O original nunca e alterado. Escreve em `.tmp` e renomeia.
 */
class ClipExporter {

    fun export(source: File, format: RecordingFormat, startMillis: Long, endMillis: Long, destination: File): ClipResult {
        if (endMillis <= startMillis) return ClipResult.Failed("intervalo vazio")
        if (format == RecordingFormat.WAV) return WavClipper.clip(source, startMillis, endMillis, destination)
        val tmp = File(destination.path + ".tmp")
        val result = runCatchingKnown("formato nao suportado") {
            destination.parentFile?.mkdirs()
            val duration = copyFrames(source, format, startMillis, endMillis, tmp)
            if (duration <= 0L) {
                ClipResult.Failed("nenhum audio no intervalo")
            } else {
                java.nio.file.Files.move(tmp.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                ClipResult.Done(destination, duration)
            }
        }
        if (result !is ClipResult.Done) tmp.delete()
        return result
    }

    /** Falhas esperadas de E/S e do MediaExtractor/MediaMuxer viram [ClipResult.Failed]; o resto sobe. */
    private fun runCatchingKnown(fallback: String, block: () -> ClipResult): ClipResult = try {
        block()
    } catch (e: IOException) {
        ClipResult.Failed(e.message ?: "erro de E/S")
    } catch (e: IllegalStateException) {
        ClipResult.Failed(e.message ?: fallback)
    } catch (e: IllegalArgumentException) {
        ClipResult.Failed(e.message ?: fallback)
    }

    /** Devolve a duracao copiada em ms (0 se nada coube). */
    private fun copyFrames(source: File, format: RecordingFormat, startMillis: Long, endMillis: Long, tmp: File): Long {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(source.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("sem faixa de audio")
            val trackFormat = extractor.getTrackFormat(track)
            val totalUs = if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) trackFormat.getLong(MediaFormat.KEY_DURATION) else Long.MAX_VALUE
            // seekTo alem do fim pousa no ultimo quadro em alguns aparelhos: sem este corte, copiaria um quadro que nao esta no intervalo.
            if (startMillis * MICROS >= totalUs) return 0L
            extractor.selectTrack(track)
            extractor.seekTo(startMillis * MICROS, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val limitUs = endMillis * MICROS
            return if (format == RecordingFormat.M4A) {
                writeMp4(extractor, trackFormat, limitUs, tmp)
            } else {
                writeRaw(extractor, limitUs, tmp)
            }
        } finally {
            extractor.release()
        }
    }

    private fun writeMp4(extractor: MediaExtractor, trackFormat: MediaFormat, limitUs: Long, tmp: File): Long {
        val muxer = MediaMuxer(tmp.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started = false
        try {
            val out = muxer.addTrack(trackFormat)
            muxer.start()
            started = true
            val buffer = ByteBuffer.allocate(BUFFER_BYTES)
            val info = MediaCodec.BufferInfo()
            var firstUs = -1L
            var lastUs = 0L
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                val time = extractor.sampleTime
                if (size < 0 || time < 0 || time >= limitUs) break
                if (firstUs < 0) firstUs = time
                info.set(0, size, time - firstUs, bufferFlags(extractor.sampleFlags))
                muxer.writeSampleData(out, buffer, info)
                lastUs = time - firstUs
                extractor.advance()
            }
            if (firstUs < 0) return 0L
            return (lastUs + FRAME_US) / MICROS
        } finally {
            if (started) runCatching { muxer.stop() }
            muxer.release()
        }
    }

    private fun writeRaw(extractor: MediaExtractor, limitUs: Long, tmp: File): Long {
        val buffer = ByteBuffer.allocate(BUFFER_BYTES)
        val chunk = ByteArray(BUFFER_BYTES)
        var firstUs = -1L
        var lastUs = 0L
        tmp.outputStream().buffered().use { out ->
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                val time = extractor.sampleTime
                if (size < 0 || time < 0 || time >= limitUs) break
                if (firstUs < 0) firstUs = time
                buffer.position(0).limit(size)
                buffer.get(chunk, 0, size)
                out.write(chunk, 0, size)
                lastUs = time - firstUs
                extractor.advance()
            }
        }
        return if (firstUs < 0) 0L else (lastUs + FRAME_US) / MICROS
    }

    /** Marca de quadro-chave do extrator -> do codec (todo quadro AAC e chave). */
    private fun bufferFlags(sampleFlags: Int): Int =
        if (sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0

    private companion object {
        const val MICROS = 1000L
        const val FRAME_US = 26_000L // quadro de MP3 (26,1 ms) / AAC (23,2 ms): o ultimo quadro conta na duracao
        const val BUFFER_BYTES = 1 shl 18
    }
}
