package com.gravasom.core.domain.marker

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AddMarkerTest {

    private val repo = FakeMarkerRepository()
    private val add = AddMarker(repo, durationOf = { 3_600_000L })

    @Test
    fun `nota com 140 caracteres e salva`() = runTest {
        val note = "a".repeat(MarkerNote.MAX_LENGTH)
        val result = add(recordingId = 1, positionMillis = 5_000, note = note)
        assertInstanceOf(MarkerResult.Saved::class.java, result)
        assertEquals(note, repo.observe(1).first().single().note)
    }

    @Test
    fun `nota com 141 caracteres e rejeitada e nada e salvo`() = runTest {
        val result = add(recordingId = 1, positionMillis = 5_000, note = "a".repeat(MarkerNote.MAX_LENGTH + 1))
        val rejected = assertInstanceOf(MarkerResult.Rejected::class.java, result)
        assertEquals(MarkerRejection.NOTE_TOO_LONG, rejected.reason)
        assertTrue(repo.observe(1).first().isEmpty())
    }

    @Test
    fun `conta caracteres por pontos de codigo e nao por unidades UTF-16`() = runTest {
        // Emoji ocupa 2 unidades UTF-16 mas e 1 caractere para o usuario.
        val note = "😀".repeat(MarkerNote.MAX_LENGTH)
        assertInstanceOf(MarkerResult.Saved::class.java, add(1, 0, note))
        assertInstanceOf(MarkerResult.Rejected::class.java, add(1, 0, note + "x"))
    }

    @Test
    fun `espacos nas pontas nao contam e nota vazia e permitida`() = runTest {
        assertInstanceOf(MarkerResult.Saved::class.java, add(1, 0, "  " + "a".repeat(MarkerNote.MAX_LENGTH) + "  "))
        assertInstanceOf(MarkerResult.Saved::class.java, add(1, 10, ""))
    }

    @Test
    fun `posicao fora da gravacao e rejeitada`() = runTest {
        val short = AddMarker(repo, durationOf = { 1_000L })
        assertEquals(MarkerRejection.OUT_OF_RANGE, (short(1, -1, "") as MarkerResult.Rejected).reason)
        assertEquals(MarkerRejection.OUT_OF_RANGE, (short(1, 1_001, "") as MarkerResult.Rejected).reason)
        assertInstanceOf(MarkerResult.Saved::class.java, short(1, 1_000, ""))
    }

    @Test
    fun `gravacao inexistente e rejeitada`() = runTest {
        val none = AddMarker(repo, durationOf = { null })
        assertEquals(MarkerRejection.RECORDING_NOT_FOUND, (none(9, 0, "") as MarkerResult.Rejected).reason)
    }
}
