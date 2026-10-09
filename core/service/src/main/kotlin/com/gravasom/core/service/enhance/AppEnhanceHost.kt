package com.gravasom.core.service.enhance

import android.content.Context
import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.data.db.EditProjectStore
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.service.RecordingStorage
import com.gravasom.core.service.library.RecordingFiles
import com.gravasom.core.service.library.RecordingIngestor
import java.io.File

/**
 * [EnhanceHost] de producao: arquivos legiveis pelo [RecordingFiles] (cofre incluso), copias derivadas entram na biblioteca pelo
 * [RecordingIngestor] (derivado de gravacao privada continua no cofre) e o projeto fica no Room. Pastas: cache privado e o
 * diretorio de gravacoes.
 */
class AppEnhanceHost(
    private val context: Context,
    private val files: RecordingFiles,
    private val ingestor: RecordingIngestor,
    private val projects: EditProjectStore,
) : EnhanceHost {

    override suspend fun playableFile(recording: Recording): File? {
        val path = runCatching { files.playablePath(recording) }.getOrNull() ?: return null
        return File(path).takeIf { it.isFile }
    }

    override suspend fun saveDerived(file: File, title: String, source: Recording): Long? =
        ingestor.ingest(file, title = title, forcePrivate = source.isPrivate, preset = null)

    override suspend fun loadProject(recordingId: Long): EditProject? = projects.load(recordingId)

    override suspend fun saveProject(recordingId: Long, project: EditProject) = projects.save(recordingId, project)

    /** Cache privado: os WAV de trabalho e as previas somem com o sistema quando precisa de espaco. */
    override val workDir: File get() = File(context.cacheDir, "melhorar").apply { mkdirs() }

    override val outputDir: File get() = RecordingStorage.dir(context)
}
