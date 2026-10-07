package com.gravasom.core.domain.usecase

import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow

class ListRecordings(private val repo: RecordingRepository) {
    operator fun invoke(): Flow<List<Recording>> = repo.observeAll()
}
