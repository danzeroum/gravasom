package com.gravasom

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.accessibility.disableAccessibilityChecks
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import org.junit.Assert.fail
import org.junit.rules.ExternalResource
import org.junit.rules.TestRule
import java.util.Locale

/**
 * Idioma do app nos testes de UI. O runner recebe `-e gravasom.locale pt-BR` (a CI roda uma passada em ingles, o idioma
 * do emulador, e outra em pt-BR). Usa o idioma por app do Android 13+; antes disso nao ha o que trocar.
 * Deve ser a regra mais externa: o idioma precisa valer antes de a Activity abrir.
 */
class RegraIdioma : ExternalResource() {
    override fun before() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val tag = InstrumentationRegistry.getArguments().getString(ARGUMENTO)
        val manager = Suporte.contexto.getSystemService(LocaleManager::class.java)
        manager.applicationLocales = if (tag.isNullOrBlank()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        if (!tag.isNullOrBlank()) {
            val esperado = Locale.forLanguageTag(tag).language
            Suporte.aguardar(IDIOMA_MS) { Suporte.contexto.resources.configuration.locales[0].language == esperado }
        }
    }

    companion object {
        const val ARGUMENTO = "gravasom.locale"
        private const val IDIOMA_MS = 5_000L
    }
}

/**
 * Liga as checagens do Accessibility Test Framework (Google) nos testes Compose: area de toque, texto falavel,
 * rotulos, contraste. Cada achado de nivel ERRO e anotado; ao fim do teste, qualquer erro reprova com a lista completa
 * (assim uma rodada mostra todos os problemas e nao so o primeiro).
 */
class RegraAcessibilidade<R : TestRule, A : ComponentActivity>(private val compose: AndroidComposeTestRule<R, A>) : ExternalResource() {

    private val erros = linkedSetOf<String>()

    override fun before() {
        val validator = AccessibilityValidator()
            .setThrowExceptionForErrors(false)
            .setRunChecksFromRootView(true)
            .addCheckListener(
                AccessibilityValidator.AccessibilityCheckListener { _, resultados ->
                    resultados.filter { it.type == AccessibilityCheckResultType.ERROR }.forEach { erros += descrever(it) }
                },
            )
        compose.enableAccessibilityChecks(validator)
    }

    override fun after() {
        runCatching { compose.disableAccessibilityChecks() }
        if (erros.isNotEmpty()) fail("Acessibilidade: ${erros.size} erro(s)\n" + erros.joinToString("\n") { "  - $it" })
    }

    private fun descrever(r: com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult): String {
        val elemento = runCatching { r.element?.let { "${it.className} '${it.text ?: it.contentDescription ?: ""}' ${it.boundsInScreen}" } }.getOrNull()
        return "${r.sourceCheckClass.simpleName}: ${r.getMessage(Locale.ENGLISH)} @ $elemento"
    }
}
