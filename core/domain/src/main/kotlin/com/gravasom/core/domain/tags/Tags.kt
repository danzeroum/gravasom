package com.gravasom.core.domain.tags

import com.gravasom.core.domain.model.Recording
import java.text.Normalizer
import java.util.Locale

object Tags {
    const val MAX_TAG_LENGTH = 24
    const val MAX_TAGS_PER_RECORDING = 10

    /** Minusculas, espacos colapsados, sem virgula; remove vazias e duplicatas; limita tamanho e quantidade. */
    fun normalize(raw: List<String>): List<String> =
        raw.flatMap { it.split(',') }
            .map { it.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT).take(MAX_TAG_LENGTH).trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_TAGS_PER_RECORDING)

    fun parse(text: String): List<String> = normalize(text.split(','))

    /** Chave de comparacao: sem acento e sem caixa. */
    fun key(tag: String): String =
        Normalizer.normalize(tag, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()
}

object TagFilter {
    /** Gravacoes que tem TODAS as tags escolhidas. Conjunto vazio devolve tudo. */
    fun apply(all: List<Recording>, selected: Set<String>): List<Recording> {
        if (selected.isEmpty()) return all
        val wanted = selected.map(Tags::key).toSet()
        return all.filter { r -> r.tags.map(Tags::key).toSet().containsAll(wanted) }
    }

    /** Tags existentes: mais usadas primeiro, empate por nome. */
    fun available(all: List<Recording>): List<String> =
        all.flatMap { it.tags }.groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
}
