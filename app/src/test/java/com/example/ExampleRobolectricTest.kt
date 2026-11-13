package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MobileLabsDatabase
import com.example.data.local.StudioPreferencesRepository
import com.example.data.model.EncoderProfile
import com.example.data.model.NormalizedRect
import com.example.data.model.RateControlMode
import com.example.data.model.SourceType
import com.example.data.model.StudioSource
import com.example.data.repository.StudioRepository
import com.example.streaming.MediaProjectionHolder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app_name matches mobileLabs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("mobileLabs", appName)
    }

    @Test
    fun `studio repository seeds default OBS scenes, sources, and supports video overlay & audio sync`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MobileLabsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = StudioRepository(db.studioDao(), StudioPreferencesRepository(context))
            repo.ensureSeeded()

            val collections = repo.collections.first()
            assertTrue(collections.any { it.name == "Gaming Scene" })

            val activeCol = collections.first { it.isActive }
            val scenes = repo.observeScenes(activeCol.id).first()
            assertEquals(7, scenes.size)
            assertEquals("Live Scene", scenes.first().name)

            val liveSceneId = scenes.first().id
            val liveSources = repo.observeSources(liveSceneId).first()
            assertTrue(liveSources.any { it.name == "Alertbox" })
            assertTrue(liveSources.any { it.name == "Video Capture Device" })
            assertTrue(liveSources.any { it.name == "Screen / Game Capture" })

            // Verify Room v2 fields (VIDEO_OVERLAY, cameraZoom, cameraMirror, cropToFill)
            val overlayId = repo.addSource(
                StudioSource(
                    sceneId = liveSceneId,
                    name = "Animated Stinger",
                    type = SourceType.VIDEO_OVERLAY,
                    sortOrder = 0,
                    hRect = NormalizedRect(0.1f, 0.1f, 0.5f, 0.5f),
                    imageUri = "content://media/video/42",
                    cameraMirror = false,
                    cameraZoom = 2.2f,
                    cropToFill = false
                )
            )
            val updatedSources = repo.observeSources(liveSceneId).first()
            val addedOverlay = updatedSources.first { it.id == overlayId }
            assertEquals(SourceType.VIDEO_OVERLAY, addedOverlay.type)
            assertEquals(2.2f, addedOverlay.cameraZoom, 0.01f)
            assertFalse(addedOverlay.cameraMirror)
            assertFalse(addedOverlay.cropToFill)

            val channels = repo.audioChannels.first()
            assertEquals(3, channels.size)
            repo.updateAudioChannel(channels.first().copy(syncOffsetMs = 150))
            val reloadedMic = repo.audioChannels.first().first { it.id == channels.first().id }
            assertEquals(150, reloadedMic.syncOffsetMs)
        } finally {
            db.close()
        }
    }

    @Test
    fun `preferences repository persists desktop OBS and mobile pro settings`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = StudioPreferencesRepository(context)

        prefsRepo.updatePreferences {
            it.copy(
                videoBitrateKbps = 8000,
                recordingBitrateKbps = 24000,
                targetFps = 60,
                resolutionHeight = 1080,
                keyframeIntervalSec = 4,
                rateControl = RateControlMode.VBR,
                encoderProfile = EncoderProfile.MAIN,
                audioBitrateKbps = 320,
                audioSampleRateHz = 48000,
                autoReconnectEnabled = true,
                dynamicBitrateEnabled = false,
                keepScreenAwake = false,
                privacyShieldActive = true,
                gridSnapEnabled = false
            )
        }

        val loaded = prefsRepo.preferencesFlow.first()
        assertEquals(8000, loaded.videoBitrateKbps)
        assertEquals(24000, loaded.recordingBitrateKbps)
        assertEquals(1080, loaded.resolutionHeight)
        assertEquals(4, loaded.keyframeIntervalSec)
        assertEquals(RateControlMode.VBR, loaded.rateControl)
        assertEquals(EncoderProfile.MAIN, loaded.encoderProfile)
        assertEquals(320, loaded.audioBitrateKbps)
        assertEquals(48000, loaded.audioSampleRateHz)
        assertFalse(loaded.dynamicBitrateEnabled)
        assertFalse(loaded.keepScreenAwake)
        assertTrue(loaded.privacyShieldActive)
        assertFalse(loaded.gridSnapEnabled)
    }

    @Test
    fun `media projection sizing preserves aspect ratio and produces even encoder dimensions`() {
        // 16:10 Landscape Tablet (2560x1600 -> 1280x800)
        val (tw, th) = MediaProjectionHolder.computeCaptureSize(2560, 1600)
        assertEquals(1280, tw)
        assertEquals(800, th)
        assertEquals(0, tw % 2)
        assertEquals(0, th % 2)

        // 4:3 Landscape Tablet (2048x1536 -> 1280x960)
        val (fw, fh) = MediaProjectionHolder.computeCaptureSize(2048, 1536)
        assertEquals(1280, fw)
        assertEquals(960, fh)
        assertEquals(0, fw % 2)
        assertEquals(0, fh % 2)

        // 20:9 Portrait Phone (1080x2400 -> 576x1280)
        val (pw, ph) = MediaProjectionHolder.computeCaptureSize(1080, 2400)
        assertEquals(576, pw)
        assertEquals(1280, ph)
        assertTrue(abs((pw.toFloat() / ph.toFloat()) - (1080f / 2400f)) < 0.01f)
        assertEquals(0, pw % 2)
        assertEquals(0, ph % 2)
    }
}
