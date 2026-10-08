package com.gravasom.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [RecordingEntity::class, EditProjectEntity::class, UsageEventEntity::class], version = 3, exportSchema = false)
abstract class GravasomDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun editProjectDao(): EditProjectDao
    abstract fun usageEventDao(): UsageEventDao

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

        fun create(context: Context): GravasomDatabase =
            Room.databaseBuilder(context, GravasomDatabase::class.java, "gravasom.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
