package com.gravasom.core.service.analysis

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.gravasom.core.audio.engine.edit.WavReader
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodifica um arquivo de audio (M4A, MP3 ou WAV) para PCM 16 bits intercalado, em pedacos, sem guardar o arquivo todo
 * na memoria. WAV PCM16 le direto do disco; o resto passa pelo MediaCodec (a mesma via do player).
 */
object PcmDecoder {

    /** Chamado uma vez, antes do primeiro pedaco, e de novo se o formato mudar no meio do fluxo. */
    fun interface FormatListener {
        fun onFormat(sampleRate: Int, channels: Int)
    }

    fun interface PcmSink {
        /** [length] amostras validas em [pcm] (intercaladas por canal). */
        fun onPcm(pcm: ShortArray, length: Int)
    }

    /** true se decodificou ate o fim; false se o arquivo nao abre, nao tem audio ou [cancelled] interrompeu. */
    fun decode(file: File, format: FormatListener, sink: PcmSink, cancelled: () -> Boolean = { false }): Boolean =
        runCatching { decodeWav(file, format, sink, cancelled) }.getOrNull() ?: decodeWithCodec(file, format, sink, cancelled)

    /** null = nao e um WAV PCM16 que esta via entenda (cai no MediaCodec). */
    private fun decodeWav(file: File, listener: FormatListener, sink: PcmSink, cancelled: () -> Boolean): Boolean? {
        if (!file.name.endsWith(".wav", ignoreCase = true)) return null
        val info = WavReader.read(file)
        listener.onFormat(info.sampleRate, info.channels)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(info.dataOffset)
            val bytes = ByteArray(WAV_CHUNK_FRAMES * info.channels * 2)
            val pcm = ShortArray(bytes.size / 2)
            var left = info.dataBytes
            while (left > 0) {
                if (cancelled()) return false
                val n = minOf(left, bytes.size.toLong()).toInt()
                raf.readFully(bytes, 0, n)
                ByteBuffer.wrap(bytes, 0, n).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm, 0, n / 2)
                sink.onPcm(pcm, n / 2)
                left -= n
            }
        }
        return true
    }

    private fun decodeWithCodec(file: File, listener: FormatListener, sink: PcmSink, cancelled: () -> Boolean): Boolean {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return false
            val input = extractor.getTrackFormat(track)
            extractor.selectTrack(track)
            codec = MediaCodec.createDecoderByType(requireNotNull(input.getString(MediaFormat.KEY_MIME)))
            codec.configure(input, null, null, 0)
            codec.start()
            return pump(extractor, codec, input, listener, sink, cancelled)
        } catch (_: java.io.IOException) {
            return false
        } catch (_: RuntimeException) {
            return false
        } finally {
            runCatching { codec?.stop() }
            codec?.release()
            extractor.release()
        }
    }

    private class Output(var sampleRate: Int, var channels: Int, var float: Boolean) {
        var chunk = ShortArray(0)
    }

    private fun pump(
        extractor: MediaExtractor,
        codec: MediaCodec,
        input: MediaFormat,
        listener: FormatListener,
        sink: PcmSink,
        cancelled: () -> Boolean,
    ): Boolean {
        val out = Output(input.getInteger(MediaFormat.KEY_SAMPLE_RATE), input.getInteger(MediaFormat.KEY_CHANNEL_COUNT), false)
        listener.onFormat(out.sampleRate, out.channels)
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        while (!cancelled()) {
            if (!inputDone) inputDone = feed(extractor, codec)
            val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                onFormatChanged(codec.outputFormat, out, listener)
            } else if (index >= 0) {
                emit(codec, index, info, out, sink)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return true
            }
        }
        return false
    }

    private fun onFormatChanged(f: MediaFormat, out: Output, listener: FormatListener) {
        val rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val ch = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        out.float = f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
            f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
        if (rate != out.sampleRate || ch != out.channels) {
            out.sampleRate = rate
            out.channels = ch
            listener.onFormat(rate, ch)
        }
    }

    /** Entrega o PCM de um buffer de saida e o devolve ao codec. */
    private fun emit(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo, out: Output, sink: PcmSink) {
        if (info.size > 0) {
            val buffer = requireNotNull(codec.getOutputBuffer(index))
            buffer.position(info.offset).limit(info.offset + info.size)
            val samples = info.size / (if (out.float) FLOAT_BYTES else SHORT_BYTES)
            if (out.chunk.size < samples) out.chunk = ShortArray(samples)
            toShorts(buffer, out.float, out.chunk, samples)
            sink.onPcm(out.chunk, samples)
        }
        codec.releaseOutputBuffer(index, false)
    }

    /** Entrega uma amostra comprimida ao decodificador; true quando acabou a entrada. */
    private fun feed(extractor: MediaExtractor, codec: MediaCodec): Boolean {
        val index = codec.dequeueInputBuffer(TIMEOUT_US)
        if (index < 0) return false
        val buffer = requireNotNull(codec.getInputBuffer(index))
        val size = extractor.readSampleData(buffer, 0)
        return if (size < 0) {
            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            true
        } else {
            codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
            extractor.advance()
            false
        }
    }

    private fun toShorts(buffer: ByteBuffer, float: Boolean, dest: ShortArray, samples: Int) {
        val ordered = buffer.order(ByteOrder.nativeOrder())
        if (float) {
            val f = ordered.asFloatBuffer()
            for (i in 0 until samples) dest[i] = (f.get().coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        } else {
            ordered.asShortBuffer().get(dest, 0, samples)
        }
    }

    private const val TIMEOUT_US = 10_000L
    private const val WAV_CHUNK_FRAMES = 16_384
    private const val SHORT_BYTES = 2
    private const val FLOAT_BYTES = 4
}
