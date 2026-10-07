package com.gravasom.core.domain.usecase

import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.model.Recording
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoreUseCasesTest {

    private fun rec(id: Long, title: String, at: Long, private: Boolean = false) = Recording(
        id = id, title = title,
        path = if (private) "/vault/$id.wav" else "/x/$id.m4a",
        format = RecordingFormat.WAV,
        durationMillis = 1000, sizeBytes = 100, createdAtMillis = at,
    )

    @Test
    fun `favoritos e lixeira filtram corretamente`() = runTest {
        val repo = FakeRecordingRepository().apply { seed(rec(1, "a", 10), rec(2, "b", 20)) }
        repo.setFavorite(1, true)
        assertEquals(listOf(1L), repo.observeFavorites().first().map { it.id })
        repo.moveToTrash(2, 999)
        assertEquals(listOf(2L), repo.observeTrash().first().map { it.id })
    }

    @Test
    fun `purge remove apenas lixeira antiga`() = runTest {
        val repo = FakeRecordingRepository().apply { seed(rec(1, "a", 10), rec(2, "b", 20)) }
        repo.moveToTrash(1, nowMillis = 100)
        repo.moveToTrash(2, nowMillis = 10_000)
        repo.purgeTrash(olderThanMillis = 1_000, nowMillis = 11_000)
        assertTrue(repo.getById(1) == null, "item antigo deveria ter sido purgado")
        assertTrue(repo.getById(2) != null, "item recente deveria permanecer")
    }

    @Test
    fun `insert atribui id e getById encontra`() = runTest {
        val repo = FakeRecordingRepository()
        val id = repo.insert(rec(0, "nova", 5))
        assertEquals("nova", repo.getById(id)!!.title)
    }

    @Test
    fun `deleteForever remove de fato`() = runTest {
        val repo = FakeRecordingRepository().apply { seed(rec(1, "a", 10)) }
        repo.deleteForever(1)
        assertTrue(repo.getById(1) == null)
    }

    @Test
    fun `modelo distingue privado e deletado`() = runTest {
        val priv = rec(1, "p", 10, private = true)
        assertTrue(priv.isPrivate)
        assertFalse(priv.isDeleted)
        assertTrue(priv.copy(deletedAtMillis = 1).isDeleted)
    }
}
