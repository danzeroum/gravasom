package com.gravasom.di

import android.content.Context
import com.gravasom.core.data.db.EditProjectStore
import com.gravasom.core.data.db.GravasomDatabase
import com.gravasom.core.dsp.NoiseReducerFactory
import com.gravasom.core.service.enhance.AppEnhanceHost
import com.gravasom.core.service.enhance.EnhanceHost
import com.gravasom.core.service.enhance.EnhanceService
import com.gravasom.core.service.library.RecordingFiles
import com.gravasom.core.service.library.RecordingIngestor
import com.gravasom.feature.enhance.MediaPlayerPreviewPlayer
import com.gravasom.feature.enhance.PreviewPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Onda 2: "Melhorar som" — projeto de efeitos por gravacao, host de producao e o redutor RNNoise nativo. */
@Module
@InstallIn(SingletonComponent::class)
object EnhanceModule {

    @Provides @Singleton
    fun editProjectStore(db: GravasomDatabase): EditProjectStore = EditProjectStore(db.editProjectDao())

    @Provides @Singleton
    fun enhanceHost(
        @ApplicationContext context: Context,
        files: RecordingFiles,
        ingestor: RecordingIngestor,
        projects: EditProjectStore,
    ): EnhanceHost = AppEnhanceHost(context, files, ingestor, projects)

    @Provides @Singleton
    fun enhanceService(host: EnhanceHost): EnhanceService = EnhanceService(host, NoiseReducerFactory())

    /** Por ViewModel (com estado): o jogador da previa A/B. */
    @Provides
    fun previewPlayer(): PreviewPlayer = MediaPlayerPreviewPlayer()
}
