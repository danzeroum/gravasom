package com.gravasom.core.service.clip

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.audio.engine.edit.ClipResult
import com.gravasom.core.audio.engine.edit.WavInfo
import com.gravasom.core.audio.engine.edit.WavReader
import com.gravasom.core.domain.audio.RecordingFormat
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Junta gravacoes do MESMO formato num arquivo novo, SEM reencodar (a mesma ideia do [ClipExporter]): WAV concatena os
 * bytes PCM (mesma taxa e canais); M4A recopia os quadros AAC para um MP4 novo, deslocando os tempos de arquivo em arquivo;
 * MP3 concatena os quadros. Os originais nunca sao alterados. Escreve em `.tmp` e renomeia.
 */
class JoinExporter {

    fun join(sources: List<File>, format: RecordingFormat, destination: File): ClipResult {
        if (sources.size < 2) return ClipResult.Failed("precisa de ao menos duas gravacoes")
        val tmp = File(destination.path + ".tmp")
        val result = runCatchingKnown {
            destination.parentFile?.mkdirs()
            val duration = when (format) {
                RecordingFormat.WAV -> joinWav(sources, tmp)
                RecordingFormat.M4A -> joinM4a(sources, tmp)
                RecordingFormat.MP3 -> joinRaw(sources, tmp)
            }
            if (duration <= 0L) {
                ClipResult.Failed("nada para juntar")
            } else {
                Files.move(tmp.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
                ClipResult.Done(destination, duration)
            }
        }
        if (result !is ClipResult.Done) tmp.delete()
        return result
    }

    /** Falhas esperadas de E/S e do MediaExtractor/MediaMuxer viram [ClipResult.Failed]; o resto sobe. */
    private fun runCatchingKnown(block: () -> ClipResult): ClipResult = try {
        block()
    } catch (e: IOException) {
        ClipResult.Failed(e.message ?: "erro de E/S")
    } catch (e: IllegalStateException) {
        ClipResult.Failed(e.message ?: "formato nao suportado")
    } catch (e: IllegalArgumentException) {
        ClipResult.Failed(e.message ?: "formato nao suportado")
    }

    /** WAV: um cabecalho com o total e os blocos PCM de cada arquivo, na ordem. Devolve a duracao em ms. */
    private fun joinWav(sources: List<File>, tmp: File): Long {
        val infos = sources.map { WavReader.read(it) }
        val rate = infos.first().sampleRate
        val channels = infos.first().channels
        if (infos.any { it.sampleRate != rate || it.channels != channels }) {
            throw IOException("taxas ou canais diferentes entre as gravacoes")
        }
        val totalBytes = infos.sumOf { it.dataBytes }
        RandomAccessFile(tmp, "rw").use { out ->
            out.write(WavHeaderWriter.build(rate, channels, BITS, totalBytes))
            for (i in sources.indices) {
                copyData(sources[i], infos[i], out)
            }
        }
        return totalBytes / (BYTES_PER_SAMPLE * channels) * MILLIS / rate
    }

    private fun copyData(source: File, info: WavInfo, out: RandomAccessFile) {
        RandomAccessFile(source, "r").use { input ->
            input.seek(info.dataOffset)
            val buf = ByteArray(BUFFER_BYTES)
            var left = info.dataBytes
            while (left > 0) {
                val n = minOf(left, buf.size.toLong()).toInt()
                input.readFully(buf, 0, n)
                out.write(buf, 0, n)
                left -= n
            }
        }
    }

    /** M4A: um MP4 novo com a faixa da primeira gravacao; os quadros das demais entram com tempo deslocado. */
    private fun joinM4a(sources: List<File>, tmp: File): Long {
        val muxer = MediaMuxer(tmp.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started = false
        try {
            val first = audioTrackOf(sources.first())
            val track = muxer.addTrack(first.second)
            muxer.start()
            started = true
            var offsetUs = 0L
            var lastWrittenUs = -1L
            for (source in sources) {
                val fileEndUs = appendFrames(source, muxer, track, first.second, offsetUs)
                if (fileEndUs < 0) continue // arquivo sem quadros: pula
                lastWrittenUs = offsetUs + fileEndUs
                offsetUs = lastWrittenUs + FRAME_US
            }
            if (lastWrittenUs <= 0L) return 0L
            return (lastWrittenUs + FRAME_US) / MICROS
        } finally {
            if (started) runCatching { muxer.stop() }
            muxer.release()
        }
    }

    /** Recopia os quadros de [source] deslocados por [offsetUs]; devolve o ultimo tempo do arquivo (ou -1 se vazio). */
    private fun appendFrames(source: File, muxer: MediaMuxer, track: Int, expected: MediaFormat, offsetUs: Long): Long {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(source.absolutePath)
            val trackIndex = audioTrack(extractor) ?: throw IOException("sem faixa de audio: $source")
            extractor.selectTrack(trackIndex)
            checkCompatible(expected, extractor.getTrackFormat(trackIndex), source)
            val buffer = ByteBuffer.allocate(BUFFER_BYTES)
            val info = MediaCodec.BufferInfo()
            var lastUs = -1L
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                val time = extractor.sampleTime
                if (size < 0 || time < 0) break
                info.set(0, size, offsetUs + time, bufferFlags(extractor.sampleFlags))
                muxer.writeSampleData(track, buffer, info)
                lastUs = time
                extractor.advance()
            }
            return lastUs
        } finally {
            extractor.release()
        }
    }

    /** MP3: os quadros de cada arquivo, um apos o outro (sem recipiente). Devolve a duracao total em ms. */
    private fun joinRaw(sources: List<File>, tmp: File): Long {
        var totalUs = 0L
        tmp.outputStream().buffered().use { out ->
            for (source in sources) {
                totalUs += appendRaw(source, out)
            }
        }
        return totalUs / MICROS
    }

    /** Copia os quadros MP3 de [source] para [out]; devolve a duracao do arquivo em microssegundos. */
    private fun appendRaw(source: File, out: OutputStream): Long {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(source.absolutePath)
            val trackIndex = audioTrack(extractor) ?: throw IOException("sem faixa de audio: $source")
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            val buffer = ByteBuffer.allocate(BUFFER_BYTES)
            val chunk = ByteArray(BUFFER_BYTES)
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0 || extractor.sampleTime < 0) break
                buffer.position(0).limit(size)
                buffer.get(chunk, 0, size)
                out.write(chunk, 0, size)
                extractor.advance()
            }
            return durationUs
        } finally {
            extractor.release()
        }
    }

    /** (indice da faixa, formato) da primeira faixa de audio do arquivo. */
    private fun audioTrackOf(source: File): Pair<Int, MediaFormat> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(source.absolutePath)
            val index = audioTrack(extractor) ?: throw IOException("sem faixa de audio: $source")
            return index to extractor.getTrackFormat(index)
        } finally {
            extractor.release()
        }
    }

    private fun audioTrack(extractor: MediaExtractor): Int? =
        (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }

    /** Juntar exige o mesmo codec, taxa e canais (senao o arquivo de saida sai errado). */
    private fun checkCompatible(a: MediaFormat, b: MediaFormat, source: File) {
        val same = a.getString(MediaFormat.KEY_MIME) == b.getString(MediaFormat.KEY_MIME) &&
            a.getInteger(MediaFormat.KEY_SAMPLE_RATE) == b.getInteger(MediaFormat.KEY_SAMPLE_RATE) &&
            a.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == b.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        check(same) { "gravacao incompativel para juntar: $source" }
    }

    /** Marca de quadro-chave do extrator -> do codec (todo quadro AAC e chave). */
    private fun bufferFlags(sampleFlags: Int): Int =
        if (sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0

    private companion object {
        const val MICROS = 1_000L
        const val MILLIS = 1_000L
        const val FRAME_US = 26_000L // ultimo quadro conta na duracao (26,1 ms MP3 / 23,2 ms AAC)
        const val BUFFER_BYTES = 1 shl 18
        const val BITS = 16
        const val BYTES_PER_SAMPLE = 2
    }
}
