package com.gravasom.core.data.vault

import java.io.File

/** Direitos do titular (LGPD): excluir tudo e excluir dados de diagnostico. */
class PrivacyManager(
    private val recordingsDir: File,
    private val vaultDir: File,
    private val diagnosticsDir: File,
) {

    /** Exclui todas as gravacoes, publicas e do cofre. Retorna quantos arquivos removeu. */
    fun deleteAllRecordings(): Int {
        var count = 0
        listOf(recordingsDir, vaultDir).forEach { dir ->
            dir.listFiles()?.forEach { f ->
                if (f.delete()) count++
            }
        }
        return count
    }

    /** Exclui dados de diagnostico. */
    fun deleteDiagnostics(): Int {
        var count = 0
        diagnosticsDir.listFiles()?.forEach { f ->
            if (f.delete()) count++
        }
        return count
    }
}
