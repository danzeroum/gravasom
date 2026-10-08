package com.gravasom.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Evento de uso local (ADR-011). Sem texto livre: `type` e `label` vem de enums; `value` e numero. */
@Entity(tableName = "usage_events")
data class UsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val timestampMillis: Long,
    val value: Long? = null,
    val label: String? = null,
)
