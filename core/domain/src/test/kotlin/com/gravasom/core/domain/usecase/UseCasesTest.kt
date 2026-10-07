package com.gravasom.core.domain.usecase

import app.cash.turbine.test
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.model.Recording
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UseCasesTest {

    private fun rec(id: Long, title: String, at: Long) = Recording(
        id = id, title = title, path = "/x/$id.m4a", format = RecordingFormat.M4A,
        durationMillis = 1000, sizeBytes = 100, createdAtMillis = at,
    )

    @Test
    fun `list ordena por data desc`() = runTest {
        val repo = FakeRecordingRepository().apply { seed(rec(1, "a", 10), rec(2, "b", 30)) }
        ListRecordings(repo)().test {
            assertEquals(listOf(2L, 1L), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search vazio devolve tudo e busca filtra`() = runTest {
        val repo = FakeRecordingRepository().apply { seed(rec(1, "Aula", 10), rec(2, "Entrevista", 20)) }
        SearchRecordings(repo)("").test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
        SearchRecordings(repo)("entre").test {
            assertEquals(listOf(2L), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete move para lixeira e restore volta`() = runTest {
        val repo = FakeRecordingRepository().apply { seed(rec(1, "a", 10)) }
        DeleteRecording(repo)(1L, nowMillis = 500)
        assertTrue(repo.getById(1)!!.isDeleted)
        RestoreRecording(repo)(1L)
        assertTrue(!repo.getById(1)!!.isDeleted)
    }

    @Test
    fun `rename rejeita titulo vazio`() = runTest {
        val repo = FakeRecordingRepository().apply { seed(rec(1, "a", 10)) }
        var thrown: Throwable? = null
        try {
            RenameRecording(repo)(1L, "   ")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown is IllegalArgumentException, "esperado IllegalArgumentException, veio $thrown")
    }

    @Test
    fun `rename aplica titulo com trim`() = runTest {
        val repo = FakeRecordingRepository().apply { seed(rec(1, "a", 10)) }
        RenameRecording(repo)(1L, "  Nova  ")
        assertEquals("Nova", repo.getById(1)!!.title)
    }
}
