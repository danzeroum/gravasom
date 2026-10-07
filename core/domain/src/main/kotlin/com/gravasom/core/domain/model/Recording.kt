package com.gravasom.core.domain.model

import com.gravasom.core.domain.audio.RecordingFormat

/** Metadados de uma gravacao, sem dependencia de Android. */
data class Recording(
    val id: Long = 0L,
    val title: String,
    val path: String,
    val format: RecordingFormat,
    val durationMillis: Long,
    val sizeBytes: Long,
    val createdAtMillis: Long,
    val isFavorite: Boolean = false,
    val folder: String? = null,
    val tags: List<String> = emptyList(),
    val deletedAtMillis: Long? = null,
) {
    val isDeleted: Boolean get() = deletedAtMillis != null
    val isPrivate: Boolean get() = path.contains("/vault/")
}
