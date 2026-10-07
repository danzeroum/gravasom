package com.gravasom.core.domain.usecase

import com.gravasom.core.domain.repository.RecordingRepository

class RenameRecording(private val repo: RecordingRepository) {
    suspend operator fun invoke(id: Long, newTitle: String) {
        require(newTitle.isNotBlank()) { "Titulo nao pode ser vazio" }
        repo.rename(id, newTitle.trim())
    }
}
