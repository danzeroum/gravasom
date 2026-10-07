package com.gravasom.core.domain.usecase

import com.gravasom.core.domain.repository.RecordingRepository

class RestoreRecording(private val repo: RecordingRepository) {
    suspend operator fun invoke(id: Long) = repo.restore(id)
}
