package com.gravasom.core.data.db

import com.gravasom.core.audio.engine.edit.EditCodec
import com.gravasom.core.audio.engine.edit.EditProject

/** Guarda e reabre o [EditProject] de uma gravacao. O original nunca e tocado: so a tabela `edit_projects`. */
class EditProjectStore(private val dao: EditProjectDao, private val clock: () -> Long = System::currentTimeMillis) {

    suspend fun save(recordingId: Long, project: EditProject) {
        dao.upsert(
            EditProjectEntity(
                recordingId = recordingId,
                schemaVersion = EditCodec.VERSION,
                stepsEncoded = EditCodec.encode(project),
                updatedAtMillis = clock(),
            ),
        )
    }

    suspend fun load(recordingId: Long): EditProject? =
        dao.getByRecording(recordingId)?.let { EditCodec.decode(it.stepsEncoded) }

    suspend fun delete(recordingId: Long) = dao.deleteByRecording(recordingId)
}
