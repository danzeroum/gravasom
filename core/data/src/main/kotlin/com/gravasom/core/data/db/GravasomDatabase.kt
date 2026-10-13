package com.gravasom.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RecordingEntity::class, EditProjectEntity::class, UsageEventEntity::class, MarkerEntity::class,
        RecordingAnalysisEntity::class, TranscriptEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class GravasomDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun editProjectDao(): EditProjectDao
    abstract fun usageEventDao(): UsageEventDao
    abstract fun markerDao(): MarkerDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun transcriptDao(): TranscriptDao

    companion object {
        /** v1 -> v2: projeto de edicao nao destrutiva (ADR-008). Nao altera a tabela `recordings`. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `edit_projects` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordingId` INTEGER NOT NULL, " +
                        "`schemaVersion` INTEGER NOT NULL, `stepsEncoded` TEXT NOT NULL, `updatedAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`recordingId`) REFERENCES `recordings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_edit_projects_recordingId` ON `edit_projects` (`recordingId`)")
            }
        }

        /** v2 -> v3: uri publicada da gravacao + eventos de uso locais (ADR-011). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `recordings` ADD COLUMN `publishedUri` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `usage_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, `timestampMillis` INTEGER NOT NULL, `value` INTEGER, `label` TEXT)",
                )
            }
        }

        /**
         * v3 -> v4 (Onda 1): marcadores e analise (onda + fala) por gravacao. Escrita a mao; NAO toca em `recordings`
         * (gravacoes e tags ficam como estao) nem nas outras tabelas.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `markers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`recordingId` INTEGER NOT NULL, `positionMillis` INTEGER NOT NULL, `label` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, `color` TEXT NOT NULL, " +
                        "FOREIGN KEY(`recordingId`) REFERENCES `recordings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_markers_recordingId_positionMillis` " +
                        "ON `markers` (`recordingId`, `positionMillis`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recording_analysis` (`recordingId` INTEGER NOT NULL, `version` INTEGER NOT NULL, " +
                        "`peaks` BLOB NOT NULL, `frameMillis` INTEGER NOT NULL, `frameCount` INTEGER NOT NULL, `speech` BLOB NOT NULL, " +
                        "PRIMARY KEY(`recordingId`), " +
                        "FOREIGN KEY(`recordingId`) REFERENCES `recordings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
            }
        }

        /**
         * v4 -> v5 (Onda 2): preset de formato com que a gravacao foi feita. Escrita a mao; so acrescenta uma coluna anulavel
         * (gravacoes existentes ficam com `preset` NULL = sem efeitos ligados por padrao).
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `recordings` ADD COLUMN `preset` TEXT")
            }
        }

        /**
         * v5 -> v6 (Onda 3): transcricoes por (gravacao, idioma). Escrita a mao; cria so a tabela
         * nova `transcripts` com FK CASCADE para `recordings`. Nao toca em nada existente.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transcripts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`recordingId` INTEGER NOT NULL, `language` TEXT NOT NULL, `format` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, `text` TEXT, `errorMessage` TEXT, " +
                        "`createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`recordingId`) REFERENCES `recordings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_transcripts_recordingId_language` " +
                        "ON `transcripts` (`recordingId`, `language`)",
                )
            }
        }

        /**
         * v6 -> v7 (Onda 3, player sincronizado): coluna `segments` (frases com tempo, JSON) na
         * tabela `transcripts`. Anulavel: transcricoes antigas e PLAIN ficam com NULL.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transcripts` ADD COLUMN `segments` TEXT")
            }
        }

        fun create(context: Context): GravasomDatabase =
            Room.databaseBuilder(context, GravasomDatabase::class.java, "gravasom.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()
    }
}
