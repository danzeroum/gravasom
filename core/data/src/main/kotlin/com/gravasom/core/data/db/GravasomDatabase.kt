package com.gravasom.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [RecordingEntity::class, EditProjectEntity::class], version = 2, exportSchema = false)
abstract class GravasomDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun editProjectDao(): EditProjectDao

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

        fun create(context: Context): GravasomDatabase =
            Room.databaseBuilder(context, GravasomDatabase::class.java, "gravasom.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
