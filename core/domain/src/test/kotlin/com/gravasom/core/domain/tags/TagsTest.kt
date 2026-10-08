package com.gravasom.core.domain.tags

import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.model.Recording
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TagsTest {

    private fun rec(id: Long, vararg tags: String) = Recording(
        id = id, title = "r$id", path = "/x/$id.m4a", format = RecordingFormat.M4A,
        durationMillis = 1_000, sizeBytes = 1, createdAtMillis = id, tags = tags.toList(),
    )

    @Test
    fun `normaliza espacos caixa e duplicatas e separa por virgula`() {
        assertEquals(listOf("aula", "reuniao de terca"), Tags.normalize(listOf("  Aula ", "AULA", "Reuniao  de   terca", "", ",")))
        assertEquals(listOf("a", "b"), Tags.normalize(listOf("a,b"))) // virgula separa tags
    }

    @Test
    fun `limita tamanho da tag e quantidade por gravacao`() {
        val long = "x".repeat(Tags.MAX_TAG_LENGTH + 10)
        assertEquals(Tags.MAX_TAG_LENGTH, Tags.normalize(listOf(long)).single().length)
        val many = (1..Tags.MAX_TAGS_PER_RECORDING + 5).map { "t$it" }
        assertEquals(Tags.MAX_TAGS_PER_RECORDING, Tags.normalize(many).size)
    }

    @Test
    fun `parse aceita texto livre separado por virgula`() {
        assertEquals(listOf("aula", "ideia"), Tags.parse("Aula, ideia ,aula"))
    }

    @Test
    fun `filtro exige todas as tags escolhidas`() {
        val all = listOf(rec(1, "aula", "fisica"), rec(2, "aula"), rec(3, "reuniao"))
        assertEquals(listOf(1L, 2L), TagFilter.apply(all, setOf("aula")).map { it.id })
        assertEquals(listOf(1L), TagFilter.apply(all, setOf("aula", "fisica")).map { it.id })
        assertEquals(all.map { it.id }, TagFilter.apply(all, emptySet()).map { it.id })
        assertTrue(TagFilter.apply(all, setOf("musica")).isEmpty())
    }

    @Test
    fun `filtro ignora caixa e acento da tag escolhida`() {
        val all = listOf(rec(1, "reuniao"))
        assertEquals(listOf(1L), TagFilter.apply(all, setOf("Reunião")).map { it.id })
    }

    @Test
    fun `tags disponiveis ordenam por uso e depois por nome`() {
        val all = listOf(rec(1, "b", "a"), rec(2, "b"), rec(3, "c"))
        assertEquals(listOf("b", "a", "c"), TagFilter.available(all))
    }
}
