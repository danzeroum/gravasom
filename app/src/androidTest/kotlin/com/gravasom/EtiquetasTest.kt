package com.gravasom

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Etiquetas: editar numa gravacao e filtrar a lista por elas (todas as escolhidas) e o nome automatico "data · preset". */
class EtiquetasTest {

    @get:Rule(order = 0)
    val permissoes = UiSuporte.permissoes()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val acessibilidade = RegraAcessibilidade(compose)

    @Before
    fun preparar() = UiSuporte.limparTudo()

    private fun comEtiquetas(titulo: String, vararg tags: String) {
        val r = UiSuporte.inserirGravacao(titulo)
        runBlocking { UiSuporte.app.repo.setTags(r.id, tags.toList()) }
    }

    private fun titulos() = UiSuporte.textos(compose, "titulo_item").sorted()

    @Test
    fun etiquetasFiltramALista() {
        comEtiquetas("Fisica 1", "aula", "fisica")
        comEtiquetas("Fisica 2", "aula")
        comEtiquetas("Reuniao", "reuniao")
        compose.onNodeWithTag("aba_gravacoes").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "filtro_etiqueta_aula") }
        assertEquals(listOf("Fisica 1", "Fisica 2", "Reuniao"), titulos())

        compose.onNodeWithTag("filtro_etiqueta_aula").performClick()
        UiSuporte.aguardar(compose) { titulos() == listOf("Fisica 1", "Fisica 2") }

        compose.onNodeWithTag("filtro_etiqueta_fisica").performClick() // as duas: so a que tem ambas
        UiSuporte.aguardar(compose) { titulos() == listOf("Fisica 1") }

        compose.onNodeWithTag("filtro_etiqueta_reuniao").performClick() // nenhuma tem as tres
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "limpar_filtro_etiquetas") }
        compose.onNodeWithTag("limpar_filtro_etiquetas").performClick()
        UiSuporte.aguardar(compose) { titulos().size == 3 }
    }

    @Test
    fun editarEtiquetasPeloPlayerAparecemNaLista() {
        UiSuporte.inserirGravacao("Ensaio")
        compose.onNodeWithTag("aba_gravacoes").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
        compose.onNodeWithTag("item_gravacao").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "editar_etiquetas") }
        compose.onNodeWithTag("editar_etiquetas").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "campo_etiquetas") }
        compose.onNodeWithTag("campo_etiquetas").performTextReplacement("Musica, ensaio, MUSICA")
        compose.onNodeWithTag("salvar_etiquetas").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.textos(compose, "etiquetas_item") == listOf("#musica  #ensaio") }
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "filtro_etiqueta_musica") }
    }

    @Test
    fun nomeAutomaticoDaGravacaoTemDataEPreset() {
        UiSuporte.gravarPelaTela(compose, segundos = 3)
        compose.onNodeWithTag("aba_gravacoes").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
        val titulo = UiSuporte.textos(compose, "titulo_item").single()
        Suporte.medida("nome_automatico", "titulo", titulo)
        // "08/10/2026 14:32 · M4A 128 kbps mono" (pt) ou "2026-10-08 14:32 · M4A 128 kbps mono" (en)
        val padrao = Regex("""^(\d{2}/\d{2}/\d{4}|\d{4}-\d{2}-\d{2}) \d{2}:\d{2} · M4A \d+ kbps (mono|stereo|estéreo)$""")
        org.junit.Assert.assertTrue("titulo '$titulo' fora do padrao data · preset", padrao.matches(titulo))
    }
}
