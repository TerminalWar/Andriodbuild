package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.StudioPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "mobilelabs_prefs")

class StudioPreferencesRepository(private val context: Context) {

    private object Keys {
        val VIDEO_BITRATE_KBPS = intPreferencesKey("video_bitrate_kbps")
        val TARGET_FPS = intPreferencesKey("target_fps")
        val RESOLUTION_HEIGHT = intPreferencesKey("resolution_height")
        val HORIZONTAL_ENABLED = booleanPreferencesKey("horizontal_enabled")
        val VERTICAL_ENABLED = booleanPreferencesKey("vertical_enabled")
        val STUDIO_MODE_ENABLED = booleanPreferencesKey("studio_mode_enabled")
        val REPLAY_BUFFER_SECONDS = intPreferencesKey("replay_buffer_seconds")
        val TWITCH_CHANNEL = stringPreferencesKey("twitch_channel")
        val STREAM_TITLE = stringPreferencesKey("stream_title")
        val OVERLAY_THEME_ID = stringPreferencesKey("overlay_theme_id")
    }

    val preferencesFlow: Flow<StudioPreferences> = context.studioDataStore.data.map { prefs ->
        StudioPreferences(
            videoBitrateKbps = prefs[Keys.VIDEO_BITRATE_KBPS] ?: 4500,
            targetFps = prefs[Keys.TARGET_FPS] ?: 30,
            resolutionHeight = prefs[Keys.RESOLUTION_HEIGHT] ?: 720,
            horizontalEnabled = prefs[Keys.HORIZONTAL_ENABLED] ?: true,
            verticalEnabled = prefs[Keys.VERTICAL_ENABLED] ?: true,
            studioModeEnabled = prefs[Keys.STUDIO_MODE_ENABLED] ?: false,
            replayBufferSeconds = prefs[Keys.REPLAY_BUFFER_SECONDS] ?: 30,
            twitchChannel = prefs[Keys.TWITCH_CHANNEL] ?: "",
            streamTitle = prefs[Keys.STREAM_TITLE] ?: "Live on mobileLabs Studio",
            overlayThemeId = prefs[Keys.OVERLAY_THEME_ID] ?: "mint_pro"
        )
    }

    suspend fun updatePreferences(transform: (StudioPreferences) -> StudioPreferences) {
        context.studioDataStore.edit { prefs ->
            val current = StudioPreferences(
                videoBitrateKbps = prefs[Keys.VIDEO_BITRATE_KBPS] ?: 4500,
                targetFps = prefs[Keys.TARGET_FPS] ?: 30,
                resolutionHeight = prefs[Keys.RESOLUTION_HEIGHT] ?: 720,
                horizontalEnabled = prefs[Keys.HORIZONTAL_ENABLED] ?: true,
                verticalEnabled = prefs[Keys.VERTICAL_ENABLED] ?: true,
                studioModeEnabled = prefs[Keys.STUDIO_MODE_ENABLED] ?: false,
                replayBufferSeconds = prefs[Keys.REPLAY_BUFFER_SECONDS] ?: 30,
                twitchChannel = prefs[Keys.TWITCH_CHANNEL] ?: "",
                streamTitle = prefs[Keys.STREAM_TITLE] ?: "Live on mobileLabs Studio",
                overlayThemeId = prefs[Keys.OVERLAY_THEME_ID] ?: "mint_pro"
            )
            val updated = transform(current)
            prefs[Keys.VIDEO_BITRATE_KBPS] = updated.videoBitrateKbps
            prefs[Keys.TARGET_FPS] = updated.targetFps
            prefs[Keys.RESOLUTION_HEIGHT] = updated.resolutionHeight
            prefs[Keys.HORIZONTAL_ENABLED] = updated.horizontalEnabled
            prefs[Keys.VERTICAL_ENABLED] = updated.verticalEnabled
            prefs[Keys.STUDIO_MODE_ENABLED] = updated.studioModeEnabled
            prefs[Keys.REPLAY_BUFFER_SECONDS] = updated.replayBufferSeconds
            prefs[Keys.TWITCH_CHANNEL] = updated.twitchChannel
            prefs[Keys.STREAM_TITLE] = updated.streamTitle
            prefs[Keys.OVERLAY_THEME_ID] = updated.overlayThemeId
        }
    }
}
