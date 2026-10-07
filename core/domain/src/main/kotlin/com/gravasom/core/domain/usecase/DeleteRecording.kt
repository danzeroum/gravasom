package com.gravasom.core.domain.usecase

import com.gravasom.core.domain.repository.RecordingRepository

/** Move para a lixeira (30 dias) — nunca apaga direto. */
class DeleteRecording(private val repo: RecordingRepository) {
    suspend operator fun invoke(id: Long, nowMillis: Long = System.currentTimeMillis()) {
        repo.moveToTrash(id, nowMillis)
    }
}
