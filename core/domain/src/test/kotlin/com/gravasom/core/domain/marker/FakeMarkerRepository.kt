package com.gravasom.core.domain.marker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMarkerRepository : MarkerRepository {
    private val items = MutableStateFlow<List<Marker>>(emptyList())

    override fun observe(recordingId: Long): Flow<List<Marker>> =
        items.map { list -> list.filter { it.recordingId == recordingId }.sortedBy { it.positionMillis } }

    override suspend fun list(recordingId: Long): List<Marker> =
        items.value.filter { it.recordingId == recordingId }.sortedBy { it.positionMillis }

    override suspend fun insert(marker: Marker): Long {
        val id = (items.value.maxOfOrNull { it.id } ?: 0L) + 1
        items.value = items.value + marker.copy(id = id)
        return id
    }

    override suspend fun update(marker: Marker) {
        items.value = items.value.map { if (it.id == marker.id) marker else it }
    }

    override suspend fun delete(id: Long) {
        items.value = items.value.filterNot { it.id == id }
    }
}
