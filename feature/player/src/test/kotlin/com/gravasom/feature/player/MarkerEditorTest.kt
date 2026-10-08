package com.gravasom.feature.player

import com.gravasom.core.domain.marker.AddMarker
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerColor
import com.gravasom.core.domain.marker.MarkerRepository
import com.gravasom.core.domain.marker.MarkerResult
import com.gravasom.core.domain.marker.UpdateMarker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeMarkerRepository : MarkerRepository {
    private val all = MutableStateFlow<List<Marker>>(emptyList())
    private var nextId = 1L

    override fun observe(recordingId: Long): Flow<List<Marker>> = all.map { l -> l.filter { it.recordingId == recordingId } }
    override suspend fun list(recordingId: Long) = all.value.filter { it.recordingId == recordingId }.sortedBy { it.positionMillis }
    override suspend fun insert(marker: Marker): Long {
        val id = if (marker.id != 0L) marker.id else nextId++
        all.value = all.value + marker.copy(id = id)
        return id
    }
    override suspend fun update(marker: Marker) { all.value = all.value.map { if (it.id == marker.id) marker else it } }
    override suspend fun delete(id: Long) { all.value = all.value.filterNot { it.id == id } }
}

/** Achados 2 e 3 da avaliacao heuristica: cancelar nao salva marcador vazio; excluir e desfazivel por 5 s. */
@OptIn(ExperimentalCoroutinesApi::class)
class MarkerEditorTest {

    private val repo = FakeMarkerRepository()

    private fun TestScope.editor() = MarkerEditor(
        repo, AddMarker(repo, durationOf = { 3_600_000L }), UpdateMarker(repo), scope = this.backgroundScope,
    )

    @Test
    fun cancelarODialogoDeNovoMarcadorNaoSalvaNada() = runTest {
        val editor = editor()
        editor.beginAdd(recordingId = 1, positionMillis = 600_000)
        assertEquals(MarkerDialogTarget.Draft(1, 600_000), editor.dialog.value)
        assertTrue("o rascunho nao pode gravar nada ao abrir", repo.list(1).isEmpty())

        editor.dismiss()
        advanceUntilIdle()

        assertNull(editor.dialog.value)
        assertTrue("cancelar deixou um marcador vazio", repo.list(1).isEmpty())
    }

    @Test
    fun salvarORascunhoCriaExatamenteUmMarcadorComANotaEAPosicao() = runTest {
        val editor = editor()
        editor.beginAdd(1, 600_000)
        val result = editor.save(editor.dialog.value!!, note = "revisar depois", label = "Prova")
        assertTrue(result is MarkerResult.Saved)
        val saved = repo.list(1).single()
        assertEquals(600_000L, saved.positionMillis)
        assertEquals("revisar depois", saved.note)
        assertEquals("Prova", saved.label)
    }

    @Test
    fun salvarRascunhoComNotaDe141CaracteresRejeitaENaoGrava() = runTest {
        val editor = editor()
        editor.beginAdd(1, 1_000)
        val result = editor.save(editor.dialog.value!!, note = "a".repeat(141), label = "")
        assertTrue(result is MarkerResult.Rejected)
        assertTrue(repo.list(1).isEmpty())
    }

    @Test
    fun editarUmMarcadorExistenteAtualizaSemCriarOutro() = runTest {
        val id = repo.insert(Marker(recordingId = 1, positionMillis = 5_000, label = "", note = "antiga"))
        val editor = editor()
        editor.beginEdit(repo.list(1).single())
        editor.save(editor.dialog.value!!, note = "nova", label = "")
        assertEquals(listOf("nova"), repo.list(1).map { it.note })
        assertEquals(id, repo.list(1).single().id)
    }

    @Test
    fun excluirEDesfazerRestauraNotaCorEPosicaoComOMesmoId() = runTest {
        val original = Marker(
            recordingId = 1, positionMillis = 2_710_000, label = "Pendulo", note = "Anotar a formula", color = MarkerColor.PURPLE,
        )
        val id = repo.insert(original)
        val editor = editor()
        val marker = repo.list(1).single()

        editor.delete(marker)
        runCurrent()
        assertTrue(repo.list(1).isEmpty())
        assertEquals(marker, editor.undo.value)

        assertTrue(editor.undoDelete())

        assertEquals(listOf(original.copy(id = id)), repo.list(1))
        assertNull(editor.undo.value)
    }

    @Test
    fun odesfazerFicaDisponivelPor5SegundosEDepoisExpira() = runTest {
        repo.insert(Marker(recordingId = 1, positionMillis = 1_000, label = "", note = "x", color = MarkerColor.RED))
        val editor = editor()
        editor.delete(repo.list(1).single())
        runCurrent()

        advanceTimeBy(MarkerEditor.UNDO_WINDOW_MILLIS - 1)
        assertTrue("ainda dentro dos 5 s", editor.undo.value != null)

        advanceTimeBy(2)
        assertNull("passados 5 s o Desfazer some", editor.undo.value)
        assertFalse(editor.undoDelete())
        assertTrue(repo.list(1).isEmpty())
    }

    @Test
    fun desfazerSemExclusaoNaoFazNada() = runTest {
        assertFalse(editor().undoDelete())
    }

    @Test
    fun umaSegundaExclusaoSubstituiAPrimeiraNaJanelaDeDesfazer() = runTest {
        repo.insert(Marker(recordingId = 1, positionMillis = 1_000, label = "a", note = ""))
        repo.insert(Marker(recordingId = 1, positionMillis = 2_000, label = "b", note = ""))
        val editor = editor()
        val (a, b) = repo.list(1)
        editor.delete(a)
        runCurrent()
        advanceTimeBy(3_000)
        editor.delete(b)
        runCurrent()
        advanceTimeBy(3_000) // 6 s desde a primeira, 3 s desde a segunda
        assertEquals(b, editor.undo.value)
        assertTrue(editor.undoDelete())
        assertEquals(listOf("b"), repo.list(1).map { it.label })
    }
}
