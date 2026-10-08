package com.gravasom.core.audio.engine.analysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ViewWindowTest {

    private val hour = 3_600_000L

    @Test
    fun `comeca mostrando a gravacao inteira`() {
        val w = ViewWindow.full(hour)
        assertEquals(0L, w.startMillis)
        assertEquals(hour, w.spanMillis)
        assertEquals(hour, w.endMillis)
    }

    @Test
    fun `zoom mantem o ponto sob os dedos no mesmo lugar da tela`() {
        val w = ViewWindow.full(hour)
        val focus = 1_800_000L // meio
        val z = w.zoom(factor = 4f, focusMillis = focus)
        assertEquals(hour / 4, z.spanMillis)
        // o foco continua na mesma fracao da largura (50%)
        assertEquals(0.5, (focus - z.startMillis).toDouble() / z.spanMillis, 1e-6)
    }

    @Test
    fun `zoom nunca passa da gravacao inteira nem do minimo`() {
        val w = ViewWindow.full(hour)
        assertEquals(hour, w.zoom(0.1f, 0L).spanMillis) // afastar alem do total: fica no total
        val deep = (1..40).fold(w) { acc, _ -> acc.zoom(2f, hour / 2) }
        assertEquals(ViewWindow.MIN_SPAN_MILLIS, deep.spanMillis)
        assertTrue(deep.startMillis >= 0 && deep.endMillis <= hour)
    }

    @Test
    fun `zoom perto das bordas nao empurra a janela para fora`() {
        val z = ViewWindow.full(hour).zoom(10f, focusMillis = 0L)
        assertEquals(0L, z.startMillis)
        val e = ViewWindow.full(hour).zoom(10f, focusMillis = hour)
        assertEquals(hour, e.endMillis)
    }

    @Test
    fun `arrastar desloca e prende nas bordas`() {
        val w = ViewWindow(hour, startMillis = 600_000, spanMillis = 600_000)
        assertEquals(900_000L, w.pan(300_000).startMillis)
        assertEquals(0L, w.pan(-10_000_000).startMillis)
        assertEquals(hour - 600_000, w.pan(10_000_000).startMillis)
    }

    @Test
    fun `seguir a posicao so rola quando ela sai da janela`() {
        val w = ViewWindow(hour, startMillis = 0, spanMillis = 60_000)
        assertEquals(w, w.follow(30_000))
        val rolled = w.follow(90_000)
        assertTrue(90_000 in rolled.startMillis..rolled.endMillis)
        // a posicao fica a 10% do comeco da nova janela, nao grudada na borda
        assertEquals(84_000L, rolled.startMillis)
    }

    @Test
    fun `conversao entre pixel e tempo e inversa`() {
        val w = ViewWindow(hour, startMillis = 120_000, spanMillis = 60_000)
        assertEquals(150_000L, w.millisAt(x = 540f, width = 1080f))
        assertEquals(540f, w.xFor(150_000, 1080f), 1e-3f)
        assertEquals(0L, ViewWindow.full(hour).millisAt(-50f, 1080f))
        assertEquals(hour, ViewWindow.full(hour).millisAt(5000f, 1080f))
    }

    @Test
    fun `gravacao mais curta que o minimo mostra tudo`() {
        val w = ViewWindow.full(500)
        assertEquals(500L, w.zoom(10f, 100).spanMillis)
    }

    @Test
    fun `duracao zero nao quebra`() {
        val w = ViewWindow.full(0)
        assertEquals(0L, w.spanMillis)
        assertEquals(0L, w.millisAt(10f, 100f))
    }
}
