package com.gravasom.core.data.metrics

import com.gravasom.core.data.db.UsageEventDao
import com.gravasom.core.data.db.UsageEventEntity
import com.gravasom.core.domain.metrics.UsageEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Registra eventos de uso SOMENTE no aparelho e SOMENTE com opt-in (ADR-011). A API so aceita enums e numeros:
 * nao ha como gravar nome de arquivo, caminho, audio ou localizacao.
 */
class UsageMetrics(
    private val dao: UsageEventDao,
    private val optIn: Flow<Boolean>,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun record(type: UsageEventType, value: Long? = null, label: Enum<*>? = null) {
        if (!optIn.first()) return
        dao.insert(UsageEventEntity(type = type.name, timestampMillis = clock(), value = value, label = label?.name))
    }

    fun count(type: UsageEventType): Flow<Int> = dao.observeCount(type.name)

    /** Mediana de um tipo com valor numerico (ex.: TEMPO_ATE_GRAVAR em ms), ou null se nao ha dados. */
    suspend fun median(type: UsageEventType): Long? = dao.values(type.name).let { if (it.isEmpty()) null else it[it.size / 2] }

    suspend fun clear() = dao.clear()

    /** CSV para exportacao MANUAL: so tipo, momento, valor e rotulo (enums/numeros). */
    suspend fun exportCsv(): String = buildString {
        appendLine("tipo,momento_ms,valor,rotulo")
        dao.all().forEach { appendLine("${it.type},${it.timestampMillis},${it.value ?: ""},${it.label ?: ""}") }
    }
}
