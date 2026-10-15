package com.gravasom.core.service.library

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Avisos da biblioteca que acontecem sem a tela aberta (gravacao iniciada pelo Tile, importacao em segundo plano).
 * E um contador, nao um evento: se ninguem estiver olhando quando acontece, a tela mostra o aviso na proxima abertura.
 */
object LibraryNotices {

    private val _vaultFailures = MutableStateFlow(0)

    /** Quantas gravacoes/importacoes ficaram SEM cifrar porque o cofre falhou (Keystore indisponivel, disco cheio...). */
    val vaultFailures: StateFlow<Int> = _vaultFailures.asStateFlow()

    fun vaultProtectFailed() {
        _vaultFailures.value += 1
    }

    fun acknowledgeVaultFailures() {
        _vaultFailures.value = 0
    }
}
