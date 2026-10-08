package com.gravasom.core.data.repository

import com.gravasom.core.data.db.MarkerDao
import com.gravasom.core.data.db.MarkerEntity
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerColor
import com.gravasom.core.domain.marker.MarkerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMarkerRepository(private val dao: MarkerDao) : MarkerRepository {

    override fun observe(recordingId: Long): Flow<List<Marker>> = dao.observe(recordingId).map { list -> list.map { it.toDomain() } }

    override suspend fun list(recordingId: Long): List<Marker> = dao.list(recordingId).map { it.toDomain() }

    override suspend fun insert(marker: Marker): Long = dao.insert(marker.toEntity())

    override suspend fun update(marker: Marker) = dao.update(marker.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)

    /** Grava varios de uma vez (marcadores feitos durante a gravacao, ao salvar). */
    suspend fun insertAll(markers: List<Marker>) = dao.insertAll(markers.map { it.toEntity() })

    private fun MarkerEntity.toDomain() = Marker(
        id = id, recordingId = recordingId, positionMillis = positionMillis, label = label, note = note,
        color = runCatching { MarkerColor.valueOf(color) }.getOrDefault(MarkerColor.AMBER),
    )

    private fun Marker.toEntity() = MarkerEntity(
        id = id, recordingId = recordingId, positionMillis = positionMillis, label = label, note = note, color = color.name,
    )
}
