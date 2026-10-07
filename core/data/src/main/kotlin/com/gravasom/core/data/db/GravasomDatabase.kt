package com.gravasom.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RecordingEntity::class], version = 1, exportSchema = false)
abstract class GravasomDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}
