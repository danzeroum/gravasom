package com.gravasom.di

import android.content.Context
import com.gravasom.core.data.db.EditProjectStore
import com.gravasom.core.domain.marker.MarkerRepository
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.backup.BackupSaver
import com.gravasom.core.service.backup.BackupService
import com.gravasom.core.service.library.RecordingFiles
import com.gravasom.core.service.library.RecordingIngestor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Onda 2: backup/restauracao cifrados da biblioteca (pacote unico .gravasom com senha do usuario). */
@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    /** Uma gravacao restaurada entra na biblioteca pelo ingestor normal (cofre, marcadores, analise em 2o plano). */
    @Provides @Singleton
    fun backupSaver(ingestor: RecordingIngestor): BackupSaver = BackupSaver { file, title, forcePrivate, preset ->
        ingestor.ingest(file, title = title, forcePrivate = forcePrivate, preset = preset)
    }

    @Provides @Singleton
    fun backupService(
        @ApplicationContext context: Context,
        repo: RecordingRepository,
        files: RecordingFiles,
        markers: MarkerRepository,
        projects: EditProjectStore,
        saver: BackupSaver,
    ): BackupService = BackupService(context, repo, files, markers, projects, saver)
}
