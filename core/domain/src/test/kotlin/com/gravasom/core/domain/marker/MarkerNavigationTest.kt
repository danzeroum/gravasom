package com.gravasom.core.domain.marker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MarkerNavigationTest {

    private fun m(vararg positions: Long) = positions.mapIndexed { i, p -> Marker(i + 1L, 1, p, "M$i", "", MarkerColor.AMBER) }

    @Test
    fun `proximo e o primeiro marcador estritamente depois da posicao`() {
        val ms = m(1_000, 5_000, 9_000)
        assertEquals(1_000L, MarkerNavigation.next(ms, -1)?.positionMillis)
        assertEquals(5_000L, MarkerNavigation.next(ms, 1_000)?.positionMillis)
        assertNull(MarkerNavigation.next(ms, 9_000))
    }

    @Test
    fun `anterior ignora marcador logo abaixo da posicao para nao ficar preso`() {
        val ms = m(1_000, 5_000, 9_000)
        // 5_100 ms: o marcador de 5_000 esta a 100 ms; "anterior" deve ir ao de 1_000 (toque repetido anda para tras).
        assertEquals(1_000L, MarkerNavigation.previous(ms, 5_100)?.positionMillis)
        assertEquals(5_000L, MarkerNavigation.previous(ms, 6_500)?.positionMillis)
        assertNull(MarkerNavigation.previous(ms, 1_200))
    }

    @Test
    fun `lista fora de ordem tambem funciona`() {
        val ms = m(9_000, 1_000, 5_000)
        assertEquals(5_000L, MarkerNavigation.next(ms, 1_000)?.positionMillis)
    }
}
