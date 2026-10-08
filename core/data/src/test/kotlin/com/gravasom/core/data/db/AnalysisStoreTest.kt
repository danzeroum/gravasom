package com.gravasom.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gravasom.core.audio.engine.analysis.RecordingAnalysis
import com.gravasom.core.audio.engine.analysis.SpeechBits
import com.gravasom.core.audio.engine.analysis.Waveform
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnalysisStoreTest {

    private lateinit var db: GravasomDatabase
    private lateinit var store: AnalysisStore

    @Before
    fun abrir() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GravasomDatabase::class.java)
            .allowMainThreadQueries().build()
        store = AnalysisStore(db.analysisDao())
    }

    @After
    fun fechar() = db.close()

    private fun analysis(frames: Int): RecordingAnalysis {
        val modes = List(AnalysisStore.MODES) { m -> SpeechBits.pack(BooleanArray(frames) { (it + m) % 3 == 0 }) }
        return RecordingAnalysis(Waveform(ByteArray(frames * 2) { (it % 251).toByte() }), 20, frames, modes)
    }

    private suspend fun gravacao() = db.recordingDao().insert(
        RecordingEntity(title = "t", path = "/x.m4a", format = "M4A", durationMillis = 1, sizeBytes = 1, createdAtMillis = 1),
    )

    @Test
    fun `salva e le de volta onda e fala dos tres modos`() = runBlocking {
        val id = gravacao()
        val original = analysis(frames = 1_003) // nao multiplo de 8: testa o resto do ultimo byte
        store.save(id, original)
        val lida = store.load(id)!!
        assertArrayEquals(original.waveform.peaks, lida.waveform.peaks)
        assertEquals(1_003, lida.frameCount)
        for (m in 0 until AnalysisStore.MODES) assertArrayEquals(original.speechFrames(m), lida.speechFrames(m))
        assertTrue(store.has(id))
    }

    @Test
    fun `uma hora de gravacao cabe em menos de 500 KB`() = runBlocking {
        val id = gravacao()
        val frames = 3_600_000 / 20
        val a = RecordingAnalysis(Waveform(ByteArray(360_000)), 20, frames, List(AnalysisStore.MODES) { ByteArray((frames + 7) / 8) })
        store.save(id, a)
        val e = db.analysisDao().get(id)!!
        assertTrue("${e.peaks.size + e.speech.size} bytes", e.peaks.size + e.speech.size < 500_000)
    }

    @Test
    fun `versao antiga ou dados truncados contam como sem analise`() = runBlocking {
        val id = gravacao()
        db.analysisDao().upsert(RecordingAnalysisEntity(id, version = 0, peaks = ByteArray(5), frameMillis = 20, frameCount = 8, speech = ByteArray(3)))
        assertNull(store.load(id))
        assertFalse(store.has(id))
        db.analysisDao().upsert(RecordingAnalysisEntity(id, AnalysisStore.VERSION, ByteArray(5), 20, 800, ByteArray(7)))
        assertNull("speech truncado", store.load(id))
    }

    @Test
    fun `salvar de novo substitui`() = runBlocking {
        val id = gravacao()
        store.save(id, analysis(80))
        store.save(id, analysis(160))
        assertEquals(160, store.load(id)!!.frameCount)
    }
}
