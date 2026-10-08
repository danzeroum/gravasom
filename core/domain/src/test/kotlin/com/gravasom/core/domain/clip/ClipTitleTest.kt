package com.gravasom.core.domain.clip

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClipTitleTest {

    @Test
    fun `nome do trecho leva o original e o intervalo em pt e en`() {
        assertEquals("Aula de física · trecho 12:03–12:40", ClipTitle.build("Aula de física", 723_000, 760_000, pt = true))
        assertEquals("Class · clip 12:03–12:40", ClipTitle.build("Class", 723_000, 760_000, pt = false))
    }

    @Test
    fun `a partir de uma hora usa h mm ss`() {
        assertEquals("Reunião · trecho 1:02:03–1:30:00", ClipTitle.build("Reunião", 3_723_000, 5_400_000, pt = true))
    }

    @Test
    fun `nome muito longo e encurtado sem perder o intervalo`() {
        val t = ClipTitle.build("x".repeat(300), 0, 5_000, pt = true)
        assertTrue(t.length <= ClipTitle.MAX_LENGTH)
        assertTrue(t.endsWith("trecho 00:00–00:05"))
    }

    @Test
    fun `formato de tempo do intervalo`() {
        assertEquals("00:00", ClipTitle.time(0))
        assertEquals("59:59", ClipTitle.time(3_599_999))
        assertEquals("1:00:00", ClipTitle.time(3_600_000))
    }
}
