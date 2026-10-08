package com.gravasom.core.domain.metrics

import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.model.Recording
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LibraryStatsTest {

    private fun rec(format: RecordingFormat, ms: Long, deleted: Boolean = false) = Recording(
        title = "t", path = "/p", format = format, durationMillis = ms, sizeBytes = 1, createdAtMillis = 1,
        deletedAtMillis = if (deleted) 5L else null,
    )

    @Test
    fun `biblioteca vazia nao tem formato mais usado`() {
        val s = LibraryStats.of(emptyList())
        assertEquals(0, s.recordings)
        assertEquals(0L, s.totalDurationMillis)
        assertNull(s.mostUsedFormat)
    }

    @Test
    fun `soma duracao e acha o formato mais usado ignorando a lixeira`() {
        val s = LibraryStats.of(
            listOf(
                rec(RecordingFormat.WAV, 1_000), rec(RecordingFormat.M4A, 2_000), rec(RecordingFormat.M4A, 3_000),
                rec(RecordingFormat.WAV, 9_000, deleted = true), rec(RecordingFormat.WAV, 9_000, deleted = true),
            ),
        )
        assertEquals(3, s.recordings)
        assertEquals(6_000L, s.totalDurationMillis)
        assertEquals(RecordingFormat.M4A, s.mostUsedFormat)
    }
}
