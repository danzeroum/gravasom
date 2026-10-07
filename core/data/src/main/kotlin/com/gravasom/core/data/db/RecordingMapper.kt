package com.gravasom.core.data.db

import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.model.Recording

fun RecordingEntity.toDomain(): Recording = Recording(
    id = id,
    title = title,
    path = path,
    format = runCatching { RecordingFormat.valueOf(format) }.getOrDefault(RecordingFormat.M4A),
    durationMillis = durationMillis,
    sizeBytes = sizeBytes,
    createdAtMillis = createdAtMillis,
    isFavorite = isFavorite,
    folder = folder,
    tags = tagsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    deletedAtMillis = deletedAtMillis,
    publishedUri = publishedUri,
)

fun Recording.toEntity(): RecordingEntity = RecordingEntity(
    id = id,
    title = title,
    path = path,
    format = format.name,
    durationMillis = durationMillis,
    sizeBytes = sizeBytes,
    createdAtMillis = createdAtMillis,
    isFavorite = isFavorite,
    folder = folder,
    tagsCsv = tags.joinToString(","),
    deletedAtMillis = deletedAtMillis,
    publishedUri = publishedUri,
)
