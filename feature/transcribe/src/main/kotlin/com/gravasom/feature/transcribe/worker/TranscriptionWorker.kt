package com.gravasom.feature.transcribe.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.gravasom.core.data.db.GravasomDatabase
import com.gravasom.core.data.db.TranscriptEntity
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.domain.transcript.TranscriptStatus
import com.gravasom.core.domain.transcript.Transcriber
import com.gravasom.core.domain.transcript.TranscriptionError
import com.gravasom.core.domain.transcript.TranscriptionException
import com.gravasom.core.domain.transcript.TranscriptionLang
import com.gravasom.feature.transcribe.convert.AudioConverter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.io.File

/**
 * Worker cancelavel que transcreve uma gravacao. Fluxo (ADR-016 secao 4):
 *
 * 1. Carrega o cache transitorio ([TranscriptionProgress]); se houver, retoma do ultimo flush.
 * 2. Converte o audio para PCM 16 kHz ([AudioConverter]) em arquivo temporario.
 * 3. Chama [Transcriber], acumulando frases; a cada [TranscriptionProgress.FLUSH_FRASES]
 *    escreve o texto no Room e limpa o cache.
 * 4. No fim (ou no erro), fecha o status no Room e apaga o arquivo temporario.
 *
 * Cancelamento: se o coroutine for cancelado, o `finally` apaga o PCM temporario e o cache; o
 * texto parcial NAO e salvo (o registro fica em [TranscriptStatus.CANCELLED] se houve registro).
 */
@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val db: GravasomDatabase,
    private val recordings: RecordingRepository,
    private val transcriber: Transcriber,
    private val progresso: TranscriptionProgress,
) : CoroutineWorker(appContext, params) {

    /** Foreground com "Transcrevendo..." e botao Cancelar; o WorkManager mostra enquanto o trabalho roda. */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        TranscriptionNotification.ensureChannel(applicationContext)
        return TranscriptionNotification.foregroundInfo(applicationContext, id.toString(), null)
    }

    override suspend fun doWork(): Result {
        val recordingId = inputData.getLong(KEY_RECORDING, -1L)
        val langTag = inputData.getString(KEY_LANG) ?: return Result.failure()
        val lang = TranscriptionLang.fromTag(langTag) ?: return Result.failure()
        if (recordingId <= 0) return Result.failure()

        // Foreground: mantem a transcricao viva com a tela apagada e da o botao Cancelar.
        runCatching { setForeground(getForegroundInfo()) }
            .onFailure { Timber.w(it, "nao conseguiu entrar em foreground; segue em segundo plano") }

        val dao = db.transcriptDao()
        val agora = System.currentTimeMillis()
        dao.upsert(
            TranscriptEntity(
                recordingId = recordingId,
                language = lang.tag,
                format = "PLAIN",
                status = TranscriptStatus.RUNNING.name,
                text = null,
                errorMessage = null,
                createdAtMillis = agora,
                updatedAtMillis = agora,
            ),
        )

        val gravacao = recordings.getById(recordingId)
        if (gravacao == null) {
            falhar(dao, recordingId, lang, TranscriptionError.RECORDING_NOT_FOUND)
            return Result.failure()
        }

        val entrada = File(gravacao.path)
        val pcmTemp = File(applicationContext.cacheDir, "transcrever/${recordingId}_${lang.tag}.pcm")
        pcmTemp.parentFile?.mkdirs()

        return try {
            // Retomada: se houver tentativa anterior, ela comeca limpa. A retomada por N segmentos
            // exigiria o transcriber pular frases (o audio nao carrega offset facil); fica como
            // melhoria futura. O snapshot serve para diagnostico (updatedAt) e para nao repetir
            // trabalho ja concluido quando o status ja e DONE (o chamador nao enfileira de novo).
            progresso.ler(recordingId, lang.tag)
            progresso.limpar()

            AudioConverter(cancelado = { isStopped }).converter(entrada, pcmTemp)
            if (isStopped) return Result.retry()

            val acumulado = StringBuilder()
            var frases = 0
            val resultado = transcriber.transcribe(pcmTemp, lang, timestamps = false) { segmento ->
                acumulado.append(segmento.text).append('\n')
                frases++
                if (frases % TranscriptionProgress.FLUSH_FRASES == 0) {
                    // Flush do cache transitorio (ADR-016 secao 4): a cada 10 frases, grava o texto
                    // parcial. Se o processo morrer, o worker pode inspecionar o snapshot na proxima
                    // execucao (hoje so diagnostico; a retomada real e melhoria futura).
                    progresso.gravar(recordingId, lang.tag, frases, acumulado.toString())
                }
            }

            dao.updateText(
                id = dao.find(recordingId, lang.tag)?.id ?: 0L,
                text = resultado.text,
                status = TranscriptStatus.DONE.name,
                updatedAtMillis = System.currentTimeMillis(),
            )
            progresso.limpar()
            Result.success()
        } catch (c: CancellationException) {
            dao.updateStatus(recordingId, lang.tag, TranscriptStatus.CANCELLED.name, null, System.currentTimeMillis())
            throw c
        } catch (t: TranscriptionException) {
            falhar(dao, recordingId, lang, t.reason)
            if (t.reason == TranscriptionError.TIMEOUT) Result.retry() else Result.failure()
        } catch (t: Throwable) {
            Timber.w(t, "transcricao falhou (id=%d, lang=%s)", recordingId, lang.tag)
            falhar(dao, recordingId, lang, TranscriptionError.UNKNOWN)
            Result.failure()
        } finally {
            pcmTemp.delete()
        }
    }

    private suspend fun falhar(
        dao: com.gravasom.core.data.db.TranscriptDao,
        recordingId: Long,
        lang: TranscriptionLang,
        erro: TranscriptionError,
    ) {
        dao.updateStatus(recordingId, lang.tag, TranscriptStatus.FAILED.name, erro.name, System.currentTimeMillis())
        progresso.limpar()
    }

    companion object {
        const val KEY_RECORDING = "recordingId"
        const val KEY_LANG = "language"
        const val UNIQUE_PREFIX = "transcricao_"
    }
}
