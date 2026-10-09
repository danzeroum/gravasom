package com.gravasom.core.domain.enhance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnhancedTitleTest {
    @Test
    fun acrescentaOSufixoNoIdiomaCerto() {
        assertEquals("Aula de fisica (melhorada)", EnhancedTitle.build("Aula de fisica", pt = true))
        assertEquals("Lecture (enhanced)", EnhancedTitle.build("Lecture", pt = false))
    }

    @Test
    fun naoDuplicaOSufixo() {
        assertEquals("Aula (melhorada)", EnhancedTitle.build("Aula (melhorada)", pt = true))
        assertEquals("Aula (enhanced)", EnhancedTitle.build("Aula (melhorada)", pt = false))
    }

    @Test
    fun ignoraEspacosNasPontas() {
        assertEquals("Ideia (melhorada)", EnhancedTitle.build("  Ideia  ", pt = true))
    }
}
