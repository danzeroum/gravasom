package com.gravasom.core.audio.engine.analysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkipPlanTest {

    private val params = SkipParams(frameMillis = 20, headPadMillis = 300, tailPadMillis = 500, minSkipMillis = 800)

    /** Monta flags de quadros de 20 ms a partir de [inicio, fim) em ms de fala dentro de [total] ms. */
    private fun flags(total: Int, vararg fala: Pair<Int, Int>): BooleanArray {
        val f = BooleanArray(total / 20)
        for ((a, b) in fala) for (i in a / 20 until b / 20) f[i] = true
        return f
    }

    @Test
    fun `dez segundos de silencio no meio viram uma lacuna com margem de 300 ms antes e 500 ms depois da fala`() {
        // fala 0-5 s, silencio 5-15 s, fala 15-20 s
        val plan = SkipPlan.build(flags(20_000, 0 to 5_000, 15_000 to 20_000), 20_000, params)
        assertEquals(listOf(SkipRange(5_500, 14_700)), plan.ranges)
        assertEquals(9_200L, plan.skippedMillis)
        assertEquals(20_000L - 9_200L, plan.remainingMillis)
        // o enunciado do plano: com pular ligado a reproducao dura no maximo 1 s a mais que a fala
        val fala = 10_000L
        assertTrue(plan.remainingMillis - fala <= 1_000, "sobrou ${plan.remainingMillis - fala} ms alem da fala")
    }

    @Test
    fun `os padroes deixam menos de 1 s por lacuna de 10 s e cobrem o fim das frases`() {
        val d = SkipParams()
        assertEquals(700, d.headPadMillis + d.tailPadMillis)
        assertTrue(d.headPadMillis + d.tailPadMillis <= 1_000)
        assertTrue(d.tailPadMillis > d.headPadMillis, "o fim das frases some devagar")
        val plan = SkipPlan.build(flags(20_000, 0 to 5_000, 15_000 to 20_000), 20_000, d)
        assertTrue(plan.remainingMillis - 10_000L <= 1_000L)
    }

    @Test
    fun `pausa curta entre palavras nunca e pulada`() {
        val plan = SkipPlan.build(flags(10_000, 0 to 2_000, 2_900 to 10_000), 10_000, params)
        assertTrue(plan.ranges.isEmpty())
    }

    @Test
    fun `lacuna menor que o minimo depois das margens nao e pulada`() {
        // 1,5 s de silencio: depois de 300 + 500 ms de margem sobram 700 ms < 800 ms
        val plan = SkipPlan.build(flags(10_000, 0 to 2_000, 3_500 to 10_000), 10_000, params)
        assertTrue(plan.ranges.isEmpty())
    }

    @Test
    fun `silencio no comeco e no fim tambem e pulado`() {
        val plan = SkipPlan.build(flags(10_000, 3_000 to 6_000), 10_000, params)
        assertEquals(listOf(SkipRange(0, 2_700), SkipRange(6_500, 10_000)), plan.ranges)
    }

    @Test
    fun `um unico quadro de fala isolado protege a regiao em volta`() {
        val plan = SkipPlan.build(flags(10_000, 5_000 to 5_020), 10_000, params)
        assertEquals(listOf(SkipRange(0, 4_700), SkipRange(5_520, 10_000)), plan.ranges)
    }

    @Test
    fun `sem nenhuma fala toda a gravacao e pulavel`() {
        val plan = SkipPlan.build(BooleanArray(500), 10_000, params)
        assertEquals(listOf(SkipRange(0, 10_000)), plan.ranges)
    }

    @Test
    fun `duracao menor que os quadros disponiveis nao estoura`() {
        val plan = SkipPlan.build(BooleanArray(1_000), 4_000, params)
        assertEquals(listOf(SkipRange(0, 4_000)), plan.ranges)
    }

    @Test
    fun `destino do salto e o fim da lacuna so quando a posicao esta dentro dela`() {
        val plan = SkipPlan.build(flags(20_000, 0 to 5_000, 15_000 to 20_000), 20_000, params)
        assertNull(plan.skipTarget(5_499))
        assertEquals(14_700L, plan.skipTarget(5_500))
        assertEquals(14_700L, plan.skipTarget(9_000))
        assertNull(plan.skipTarget(14_700))
        assertNull(plan.skipTarget(0))
    }

    @Test
    fun `proxima lacuna a partir de uma posicao e usada para agendar o salto`() {
        val plan = SkipPlan.build(flags(30_000, 0 to 2_000, 8_000 to 10_000, 20_000 to 30_000), 30_000, params)
        assertEquals(SkipRange(2_500, 7_700), plan.nextRange(0))
        assertEquals(SkipRange(10_500, 19_700), plan.nextRange(7_700))
        assertNull(plan.nextRange(19_700))
    }

    @Test
    fun `mapeia posicao original para tempo economizado ate ali`() {
        val plan = SkipPlan.build(flags(20_000, 0 to 5_000, 15_000 to 20_000), 20_000, params)
        assertEquals(0L, plan.skippedBefore(5_000))
        assertEquals(4_500L, plan.skippedBefore(10_000))
        assertEquals(9_200L, plan.skippedBefore(20_000))
    }
}
