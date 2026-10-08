package com.gravasom.core.domain.marker

import kotlinx.coroutines.flow.Flow

interface MarkerRepository {
    /** Marcadores da gravacao em ordem cronologica. */
    fun observe(recordingId: Long): Flow<List<Marker>>
    suspend fun list(recordingId: Long): List<Marker>
    suspend fun insert(marker: Marker): Long
    suspend fun update(marker: Marker)
    suspend fun delete(id: Long)
}
