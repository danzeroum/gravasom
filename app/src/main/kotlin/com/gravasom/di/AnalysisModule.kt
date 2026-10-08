package com.gravasom.di

import android.content.Context
import com.gravasom.core.data.db.AnalysisStore
import com.gravasom.core.data.db.GravasomDatabase
import com.gravasom.core.data.repository.RoomMarkerRepository
import com.gravasom.core.domain.marker.MarkerRepository
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.analysis.RecordingAnalyzer
import com.gravasom.core.service.clip.ClipService
import com.gravasom.core.service.library.RecordingFiles
import com.gravasom.core.service.library.RecordingIngestor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Onda 1: marcadores, analise (onda + fala) e exportar trecho. */
@Module
@InstallIn(SingletonComponent::class)
object AnalysisModule {

    @Provides @Singleton
    fun markerRepository(db: GravasomDatabase): MarkerRepository = RoomMarkerRepository(db.markerDao())

    @Provides @Singleton
    fun analysisStore(db: GravasomDatabase): AnalysisStore = AnalysisStore(db.analysisDao())

    @Provides @Singleton
    fun recordingAnalyzer(store: AnalysisStore): RecordingAnalyzer = RecordingAnalyzer(store)

    @Provides @Singleton
    fun clipService(
        @ApplicationContext context: Context,
        repo: RecordingRepository,
        files: RecordingFiles,
        ingestor: RecordingIngestor,
    ): ClipService = ClipService(context, repo, files, ingestor)
}
