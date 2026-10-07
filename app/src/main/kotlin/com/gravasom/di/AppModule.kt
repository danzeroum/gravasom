package com.gravasom.di

import android.content.Context
import com.gravasom.core.data.db.GravasomDatabase
import com.gravasom.core.data.metrics.UsageMetrics
import com.gravasom.core.data.preferences.PreferencesRepository
import com.gravasom.core.data.repository.RoomRecordingRepository
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.library.AudioImporter
import com.gravasom.core.service.library.MediaStorePublisher
import com.gravasom.core.service.library.RecordingFiles
import com.gravasom.core.service.library.RecordingIngestor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): GravasomDatabase = GravasomDatabase.create(context)

    @Provides @Singleton
    fun recordingRepository(db: GravasomDatabase): RecordingRepository = RoomRecordingRepository(db.recordingDao())

    @Provides @Singleton
    fun preferences(@ApplicationContext context: Context): PreferencesRepository = PreferencesRepository(context)

    @Provides @Singleton
    fun usageMetrics(db: GravasomDatabase, prefs: PreferencesRepository): UsageMetrics =
        UsageMetrics(db.usageEventDao(), prefs.telemetryOptIn)

    @Provides @Singleton
    fun publisher(@ApplicationContext context: Context): MediaStorePublisher = MediaStorePublisher(context)

    @Provides @Singleton
    fun recordingFiles(@ApplicationContext context: Context, repo: RecordingRepository, publisher: MediaStorePublisher): RecordingFiles =
        RecordingFiles(context, repo, publisher)

    @Provides @Singleton
    fun audioImporter(@ApplicationContext context: Context, repo: RecordingRepository, metrics: UsageMetrics): AudioImporter =
        AudioImporter(context, repo, metrics)

    @Provides @Singleton
    fun ingestor(
        repo: RecordingRepository,
        prefs: PreferencesRepository,
        metrics: UsageMetrics,
        publisher: MediaStorePublisher,
    ): RecordingIngestor =
        RecordingIngestor(repo, prefs, metrics, publisher)
}
