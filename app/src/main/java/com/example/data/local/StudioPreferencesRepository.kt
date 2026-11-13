package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.EncoderProfile
import com.example.data.model.RateControlMode
import com.example.data.model.StudioPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "mobilelabs_prefs")

class StudioPreferencesRepository(private val context: Context) {

    private object Keys {
        val VIDEO_BITRATE_KBPS = intPreferencesKey("video_bitrate_kbps")
        val RECORDING_BITRATE_KBPS = intPreferencesKey("recording_bitrate_kbps")
        val TARGET_FPS = intPreferencesKey("target_fps")
        val RESOLUTION_HEIGHT = intPreferencesKey("resolution_height")
        val KEYFRAME_INTERVAL_SEC = intPreferencesKey("keyframe_interval_sec")
        val RATE_CONTROL = stringPreferencesKey("rate_control")
        val ENCODER_PROFILE = stringPreferencesKey("encoder_profile")
        val AUDIO_BITRATE_KBPS = intPreferencesKey("audio_bitrate_kbps")
        val AUDIO_SAMPLE_RATE = intPreferencesKey("audio_sample_rate")
        val HORIZONTAL_ENABLED = booleanPreferencesKey("horizontal_enabled")
        val VERTICAL_ENABLED = booleanPreferencesKey("vertical_enabled")
        val STUDIO_MODE_ENABLED = booleanPreferencesKey("studio_mode_enabled")
        val REPLAY_BUFFER_SECONDS = intPreferencesKey("replay_buffer_seconds")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val DYNAMIC_BITRATE = booleanPreferencesKey("dynamic_bitrate")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val PRIVACY_SHIELD = booleanPreferencesKey("privacy_shield")
        val GRID_SNAP = booleanPreferencesKey("grid_snap")
        val TWITCH_CHANNEL = stringPreferencesKey("twitch_channel")
        val STREAM_TITLE = stringPreferencesKey("stream_title")
        val OVERLAY_THEME_ID = stringPreferencesKey("overlay_theme_id")
    }

    val preferencesFlow: Flow<StudioPreferences> = context.studioDataStore.data.map { prefs ->
        readFromPreferences(prefs)
    }

    private fun readFromPreferences(prefs: Preferences): StudioPreferences = StudioPreferences(
        videoBitrateKbps = prefs[Keys.VIDEO_BITRATE_KBPS] ?: 6000,
        recordingBitrateKbps = prefs[Keys.RECORDING_BITRATE_KBPS] ?: 12000,
        targetFps = prefs[Keys.TARGET_FPS] ?: 60,
        resolutionHeight = prefs[Keys.RESOLUTION_HEIGHT] ?: 720,
        keyframeIntervalSec = prefs[Keys.KEYFRAME_INTERVAL_SEC] ?: 2,
        rateControl = runCatching {
            RateControlMode.valueOf(prefs[Keys.RATE_CONTROL] ?: RateControlMode.CBR.name)
        }.getOrDefault(RateControlMode.CBR),
        encoderProfile = runCatching {
            EncoderProfile.valueOf(prefs[Keys.ENCODER_PROFILE] ?: EncoderProfile.HIGH.name)
        }.getOrDefault(EncoderProfile.HIGH),
        audioBitrateKbps = prefs[Keys.AUDIO_BITRATE_KBPS] ?: 160,
        audioSampleRateHz = prefs[Keys.AUDIO_SAMPLE_RATE] ?: 44100,
        horizontalEnabled = prefs[Keys.HORIZONTAL_ENABLED] ?: true,
        verticalEnabled = prefs[Keys.VERTICAL_ENABLED] ?: true,
        studioModeEnabled = prefs[Keys.STUDIO_MODE_ENABLED] ?: false,
        replayBufferSeconds = prefs[Keys.REPLAY_BUFFER_SECONDS] ?: 30,
        autoReconnectEnabled = prefs[Keys.AUTO_RECONNECT] ?: true,
        dynamicBitrateEnabled = prefs[Keys.DYNAMIC_BITRATE] ?: true,
        keepScreenAwake = prefs[Keys.KEEP_SCREEN_AWAKE] ?: true,
        privacyShieldActive = prefs[Keys.PRIVACY_SHIELD] ?: false,
        gridSnapEnabled = prefs[Keys.GRID_SNAP] ?: true,
        twitchChannel = prefs[Keys.TWITCH_CHANNEL] ?: "",
        streamTitle = prefs[Keys.STREAM_TITLE] ?: "Pro Mobile Stream | mobileLabs",
        overlayThemeId = prefs[Keys.OVERLAY_THEME_ID] ?: "mint_pro"
    )

    suspend fun updatePreferences(transform: (StudioPreferences) -> StudioPreferences) {
        context.studioDataStore.edit { prefs ->
            val current = readFromPreferences(prefs)
            val updated = transform(current)
            prefs[Keys.VIDEO_BITRATE_KBPS] = updated.videoBitrateKbps
            prefs[Keys.RECORDING_BITRATE_KBPS] = updated.recordingBitrateKbps
            prefs[Keys.TARGET_FPS] = updated.targetFps
            prefs[Keys.RESOLUTION_HEIGHT] = updated.resolutionHeight
            prefs[Keys.KEYFRAME_INTERVAL_SEC] = updated.keyframeIntervalSec
            prefs[Keys.RATE_CONTROL] = updated.rateControl.name
            prefs[Keys.ENCODER_PROFILE] = updated.encoderProfile.name
            prefs[Keys.AUDIO_BITRATE_KBPS] = updated.audioBitrateKbps
            prefs[Keys.AUDIO_SAMPLE_RATE] = updated.audioSampleRateHz
            prefs[Keys.HORIZONTAL_ENABLED] = updated.horizontalEnabled
            prefs[Keys.VERTICAL_ENABLED] = updated.verticalEnabled
            prefs[Keys.STUDIO_MODE_ENABLED] = updated.studioModeEnabled
            prefs[Keys.REPLAY_BUFFER_SECONDS] = updated.replayBufferSeconds
            prefs[Keys.AUTO_RECONNECT] = updated.autoReconnectEnabled
            prefs[Keys.DYNAMIC_BITRATE] = updated.dynamicBitrateEnabled
            prefs[Keys.KEEP_SCREEN_AWAKE] = updated.keepScreenAwake
            prefs[Keys.PRIVACY_SHIELD] = updated.privacyShieldActive
            prefs[Keys.GRID_SNAP] = updated.gridSnapEnabled
            prefs[Keys.TWITCH_CHANNEL] = updated.twitchChannel
            prefs[Keys.STREAM_TITLE] = updated.streamTitle
            prefs[Keys.OVERLAY_THEME_ID] = updated.overlayThemeId
        }
    }
}
