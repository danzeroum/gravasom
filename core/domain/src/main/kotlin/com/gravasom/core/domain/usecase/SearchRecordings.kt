package com.gravasom.core.domain.usecase

import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow

class SearchRecordings(private val repo: RecordingRepository) {
    operator fun invoke(query: String): Flow<List<Recording>> =
        if (query.isBlank()) repo.observeAll() else repo.search(query.trim())
}
