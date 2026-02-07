package com.eplaytime.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "playtime_settings")

/**
 * Playback state to remember
 */
data class PlaybackState(
    val lastSongUri: String = "",
    val lastPosition: Long = 0L,
    val lastPlaybackSpeed: Float = 1.0f,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val lastVolume: Float = 0.5f
)

/**
 * PlayTimeDataStore - Persistent storage for app settings and playback state
 */
@Singleton
class PlayTimeDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val LAST_SONG_URI = stringPreferencesKey("last_song_uri")
        val LAST_POSITION = longPreferencesKey("last_position")
        val LAST_PLAYBACK_SPEED = floatPreferencesKey("last_playback_speed")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val LAST_VOLUME = floatPreferencesKey("last_volume")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val FILTER_SHORT_AUDIO = booleanPreferencesKey("filter_short_audio") // Hide clips < 30s
        val FILTER_CALL_RECORDINGS = booleanPreferencesKey("filter_call_recordings") // Hide call/voice recordings
        val USER_NAME = stringPreferencesKey("user_name")
    }

    /**
     * Get playback state as Flow
     */
    val playbackState: Flow<PlaybackState> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            PlaybackState(
                lastSongUri = prefs[Keys.LAST_SONG_URI] ?: "",
                lastPosition = prefs[Keys.LAST_POSITION] ?: 0L,
                lastPlaybackSpeed = prefs[Keys.LAST_PLAYBACK_SPEED] ?: 1.0f,
                shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
                repeatMode = prefs[Keys.REPEAT_MODE] ?: 0,
                lastVolume = prefs[Keys.LAST_VOLUME] ?: 0.5f
            )
        }

    /**
     * Save playback state
     */
    suspend fun savePlaybackState(
        songUri: String,
        position: Long,
        playbackSpeed: Float = 1.0f,
        shuffleEnabled: Boolean = false,
        repeatMode: Int = 0
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SONG_URI] = songUri
            prefs[Keys.LAST_POSITION] = position
            prefs[Keys.LAST_PLAYBACK_SPEED] = playbackSpeed
            prefs[Keys.SHUFFLE_ENABLED] = shuffleEnabled
            prefs[Keys.REPEAT_MODE] = repeatMode
        }
    }

    /**
     * Save last song URI only
     */
    suspend fun saveLastSong(songUri: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SONG_URI] = songUri
        }
    }

    /**
     * Save last position only
     */
    suspend fun saveLastPosition(position: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_POSITION] = position
        }
    }

    /**
     * Clear playback state
     */
    suspend fun clearPlaybackState() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.LAST_SONG_URI)
            prefs.remove(Keys.LAST_POSITION)
        }
    }

    /**
     * Get theme mode
     */
    val themeMode: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.THEME_MODE] ?: "dark" }

    /**
     * Save theme mode
     */
    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    /**
     * Get filter short audio setting
     */
    val filterShortAudio: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.FILTER_SHORT_AUDIO] ?: false }

    /**
     * Save filter short audio setting
     */
    suspend fun setFilterShortAudio(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FILTER_SHORT_AUDIO] = enabled
        }
    }

    /**
     * Get filter call recordings setting
     */
    val filterCallRecordings: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.FILTER_CALL_RECORDINGS] ?: false }

    /**
     * Save filter call recordings setting
     */
    suspend fun setFilterCallRecordings(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FILTER_CALL_RECORDINGS] = enabled
        }
    }

    /**
     * Get user name
     */
    val userName: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.USER_NAME] }

    /**
     * Save user name
     */
    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name
        }
    }
}
