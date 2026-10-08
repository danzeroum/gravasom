package com.gravasom

import android.content.ContentValues
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.audio.engine.edit.EditStep
import com.gravasom.core.audio.engine.edit.StepType
import com.gravasom.core.data.db.EditProjectStore
import com.gravasom.core.data.db.GravasomDatabase
import com.gravasom.core.data.db.RecordingEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

/** Fase 13: projeto de edicao na Room (DAO, FK em cascata, migracao 1->3). */
class EdicaoRoomTest {

    private lateinit var db: GravasomDatabase

    @Before
    fun abrir() {
        db = Room.inMemoryDatabaseBuilder(Suporte.contexto, GravasomDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun fechar() = db.close()

    private suspend fun gravacao(): Long = db.recordingDao().insert(
        RecordingEntity(title = "t", path = "/x.wav", format = "WAV", durationMillis = 1, sizeBytes = 1, createdAtMillis = 1),
    )

    @Test
    fun salvaReabreEApagaEmCascata() = runBlocking {
        val id = gravacao()
        val store = EditProjectStore(db.editProjectDao())
        val projeto = EditProject(listOf(EditStep(StepType.HIGH_PASS, listOf(80f))), intensity = 0.7f)
        store.save(id, projeto)
        assertEquals(projeto, store.load(id))
        store.save(id, projeto.copy(bypass = true)) // 1 projeto por gravacao: substitui
        assertEquals(true, store.load(id)!!.bypass)
        db.recordingDao().deleteForever(id)
        assertNull("projeto deveria sumir com a gravacao (CASCADE)", store.load(id))
    }

    @Test
    fun migracao1Para2CriaATabelaEPreservaGravacoes() {
        val arquivo = File(Suporte.contexto.cacheDir, "migracao_v1.db").apply { delete() }
        // Banco v1 criado a mao com o esquema que a Room v1 gerava para `recordings`.
        val v1 = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(Suporte.contexto).name(arquivo.path)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `recordings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`title` TEXT NOT NULL, `path` TEXT NOT NULL, `format` TEXT NOT NULL, `durationMillis` INTEGER NOT NULL, " +
                                "`sizeBytes` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, " +
                                "`folder` TEXT, `tagsCsv` TEXT NOT NULL, `deletedAtMillis` INTEGER)",
                        )
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build(),
        )
        v1.writableDatabase.use { d ->
            d.insert("recordings", 0, ContentValues().apply {
                put("title", "antiga"); put("path", "/a.wav"); put("format", "WAV"); put("durationMillis", 5)
                put("sizeBytes", 6); put("createdAtMillis", 7); put("isFavorite", 0); put("tagsCsv", "")
            })
        }
        val novo = Room.databaseBuilder(Suporte.contexto, GravasomDatabase::class.java, arquivo.path)
            .addMigrations(GravasomDatabase.MIGRATION_1_2, GravasomDatabase.MIGRATION_2_3, GravasomDatabase.MIGRATION_3_4)
            .allowMainThreadQueries().build()
        try {
            runBlocking {
                val existente = novo.recordingDao().getById(1)
                assertNotNull("gravacao v1 perdida na migracao", existente)
                assertEquals("antiga", existente!!.title)
                val store = EditProjectStore(novo.editProjectDao())
                store.save(1, EditProject(listOf(EditStep(StepType.HIGH_PASS, listOf(100f)))))
                assertEquals(100f, store.load(1)!!.steps.single().params.single())
            }
        } finally {
            novo.close()
        }
    }
}
