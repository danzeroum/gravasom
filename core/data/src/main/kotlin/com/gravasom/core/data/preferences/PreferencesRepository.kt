package com.gravasom.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gravasom.core.domain.audio.RecordingFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "gravasom_prefs")

/** Preferencias do usuario (formato, qualidade, privacidade, telemetria). */
class PreferencesRepository(private val context: Context) {

    val format: Flow<RecordingFormat> = context.dataStore.data.map { prefs ->
        runCatching { RecordingFormat.valueOf(prefs[KEY_FORMAT] ?: "M4A") }
            .getOrDefault(RecordingFormat.M4A)
    }

    val sampleRate: Flow<Int> = context.dataStore.data.map { it[KEY_SAMPLE_RATE] ?: 44_100 }
    val bitrate: Flow<Int> = context.dataStore.data.map { it[KEY_BITRATE] ?: 128 }
    val mp3Enabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_MP3] ?: false }
    val privateVault: Flow<Boolean> = context.dataStore.data.map { it[KEY_VAULT] ?: false }
    val telemetryOptIn: Flow<Boolean> = context.dataStore.data.map { it[KEY_TELEMETRY] ?: false }

    val preferredInputId: Flow<Int> = context.dataStore.data.map { it[KEY_INPUT] ?: NO_INPUT }

    suspend fun setPreferredInputId(id: Int) = context.dataStore.edit { it[KEY_INPUT] = id }
    suspend fun setFormat(format: RecordingFormat) = context.dataStore.edit { it[KEY_FORMAT] = format.name }
    suspend fun setSampleRate(hz: Int) = context.dataStore.edit { it[KEY_SAMPLE_RATE] = hz }
    suspend fun setBitrate(kbps: Int) = context.dataStore.edit { it[KEY_BITRATE] = kbps }
    suspend fun setMp3Enabled(enabled: Boolean) = context.dataStore.edit { it[KEY_MP3] = enabled }
    suspend fun setPrivateVault(enabled: Boolean) = context.dataStore.edit { it[KEY_VAULT] = enabled }
    suspend fun setTelemetryOptIn(enabled: Boolean) = context.dataStore.edit { it[KEY_TELEMETRY] = enabled }

    companion object {
        const val NO_INPUT = -1
        private val KEY_INPUT = intPreferencesKey("preferred_input")
        private val KEY_FORMAT = stringPreferencesKey("format")
        private val KEY_SAMPLE_RATE = intPreferencesKey("sample_rate")
        private val KEY_BITRATE = intPreferencesKey("bitrate")
        private val KEY_MP3 = booleanPreferencesKey("mp3_enabled")
        private val KEY_VAULT = booleanPreferencesKey("private_vault")
        private val KEY_TELEMETRY = booleanPreferencesKey("telemetry_opt_in")
    }
}
