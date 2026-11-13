package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MobileLabsDatabase
import com.example.data.local.StudioPreferencesRepository
import com.example.data.model.AudioChannelConfig
import com.example.data.model.EncoderProfile
import com.example.data.model.NormalizedRect
import com.example.data.model.RateControlMode
import com.example.data.model.SourceType
import com.example.data.model.StudioSource
import com.example.data.model.TransitionType
import com.example.data.repository.StudioRepository
import com.example.streaming.AudioMixerEngine
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
    fun `studio repository seeds default OBS scenes, chroma key, video speed, duplication & JSON backup`() = runTest {
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

            val liveScene = scenes.first()
            val liveSources = repo.observeSources(liveScene.id).first()
            assertTrue(liveSources.any { it.name == "Alertbox" })
            assertTrue(liveSources.any { it.name == "Video Capture Device" })
            assertTrue(liveSources.any { it.name == "Screen / Game Capture" })

            // Verify Room v3 fields (chromaKeyEnabled, chromaKeyColorHex, chromaKeyTolerance, videoSpeed, STREAM_TIMER)
            val timerId = repo.addSource(
                StudioSource(
                    sceneId = liveScene.id,
                    name = "Live Uptime Counter",
                    type = SourceType.STREAM_TIMER,
                    sortOrder = 0,
                    hRect = NormalizedRect(0.03f, 0.04f, 0.34f, 0.08f),
                    textContent = "LIVE"
                )
            )
            val overlayId = repo.addSource(
                StudioSource(
                    sceneId = liveScene.id,
                    name = "Green Screen Cam",
                    type = SourceType.VIDEO_OVERLAY,
                    sortOrder = 1,
                    hRect = NormalizedRect(0.1f, 0.1f, 0.5f, 0.5f),
                    imageUri = "content://media/video/42",
                    cameraMirror = false,
                    cameraZoom = 2.2f,
                    cropToFill = false,
                    chromaKeyEnabled = true,
                    chromaKeyColorHex = 0xFF00B140,
                    chromaKeyTolerance = 0.32f,
                    videoSpeed = 1.5f
                )
            )
            val updatedSources = repo.observeSources(liveScene.id).first()
            val addedOverlay = updatedSources.first { it.id == overlayId }
            val addedTimer = updatedSources.first { it.id == timerId }
            assertEquals(SourceType.STREAM_TIMER, addedTimer.type)
            assertEquals(SourceType.VIDEO_OVERLAY, addedOverlay.type)
            assertEquals(2.2f, addedOverlay.cameraZoom, 0.01f)
            assertFalse(addedOverlay.cameraMirror)
            assertFalse(addedOverlay.cropToFill)
            assertTrue(addedOverlay.chromaKeyEnabled)
            assertEquals(0xFF00B140L, addedOverlay.chromaKeyColorHex)
            assertEquals(0.32f, addedOverlay.chromaKeyTolerance, 0.01f)
            assertEquals(1.5f, addedOverlay.videoSpeed, 0.01f)

            // Scene duplication + JSON roundtrip backup/restore
            val dupSceneId = repo.duplicateScene(liveScene, scenes.size)
            val dupSources = repo.observeSources(dupSceneId).first()
            assertEquals(updatedSources.size, dupSources.size)

            val json = repo.exportSceneCollectionJson(activeCol.id, activeCol.name, scenes)
            val importedIdRes = repo.importSceneCollectionJson(json)
            assertTrue(importedIdRes.isSuccess)
            val importedScenes = repo.observeScenes(importedIdRes.getOrThrow()).first()
            assertEquals(scenes.size, importedScenes.size)

            val channels = repo.audioChannels.first()
            assertEquals(3, channels.size)
            repo.updateAudioChannel(channels.first().copy(syncOffsetMs = 150, monitorEnabled = true))
            val reloadedMic = repo.audioChannels.first().first { it.id == channels.first().id }
            assertEquals(150, reloadedMic.syncOffsetMs)
            assertTrue(reloadedMic.monitorEnabled)
        } finally {
            db.close()
        }
    }

    @Test
    fun `preferences repository persists desktop OBS, 900p, auto ducking & mobile pro settings`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = StudioPreferencesRepository(context)

        prefsRepo.updatePreferences {
            it.copy(
                videoBitrateKbps = 8000,
                recordingBitrateKbps = 24000,
                targetFps = 60,
                resolutionHeight = 900,
                keyframeIntervalSec = 4,
                rateControl = RateControlMode.VBR,
                encoderProfile = EncoderProfile.MAIN,
                audioBitrateKbps = 320,
                audioSampleRateHz = 48000,
                autoDuckingEnabled = true,
                duckingAmountDb = -14f,
                gamePerformanceMode = true,
                confirmStopStream = false,
                transitionType = TransitionType.SLIDE,
                transitionDurationMs = 350,
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
        assertEquals(900, loaded.resolutionHeight)
        assertEquals(4, loaded.keyframeIntervalSec)
        assertEquals(RateControlMode.VBR, loaded.rateControl)
        assertEquals(EncoderProfile.MAIN, loaded.encoderProfile)
        assertEquals(320, loaded.audioBitrateKbps)
        assertEquals(48000, loaded.audioSampleRateHz)
        assertTrue(loaded.autoDuckingEnabled)
        assertEquals(-14f, loaded.duckingAmountDb, 0.1f)
        assertTrue(loaded.gamePerformanceMode)
        assertFalse(loaded.confirmStopStream)
        assertEquals(TransitionType.SLIDE, loaded.transitionType)
        assertEquals(350, loaded.transitionDurationMs)
        assertFalse(loaded.dynamicBitrateEnabled)
        assertFalse(loaded.keepScreenAwake)
        assertTrue(loaded.privacyShieldActive)
        assertFalse(loaded.gridSnapEnabled)
    }

    @Test
    fun `audio mixer DSP enforces mute, noise gate, compressor & ring delay`() {
        val engine = AudioMixerEngine()

        // 1. Muted channel zeroes buffer
        val muteBuf = shortArrayOf(4000, -4000, 8000, -8000)
        val muteCfg = AudioChannelConfig(id = "MIC_AUX", name = "Mic", gainDb = 0f, isMuted = true)
        val (mRms, _) = engine.processChannelDsp(muteBuf, muteBuf.size, muteCfg)
        assertEquals(-60f, mRms, 0.01f)
        assertTrue(muteBuf.all { it == 0.toShort() })

        // 2. Compressor softens peaks above knee without clipping past limiter
        val hotBuf = ShortArray(64) { 28000 }
        val compCfg = AudioChannelConfig(id = "MIC_AUX", name = "Mic", gainDb = 6f, isMuted = false, compressorEnabled = true)
        engine.processChannelDsp(hotBuf, hotBuf.size, compCfg)
        assertTrue(hotBuf.all { abs(it.toInt()) <= 31500 })

        // 3. Sync offset delay line shifts samples by N frames
        val ring = ShortArray(16)
        val src = shortArrayOf(100, 200, 300, 400)
        engine.applySyncDelay(src, src.size, ring, 0, delaySamples = 2)
        assertEquals(0.toShort(), src[0])
        assertEquals(0.toShort(), src[1])
        assertEquals(100.toShort(), src[2])
        assertEquals(200.toShort(), src[3])
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

    @Test
    fun `rtmp client uri builder and parser support rtmp, rtmps, and preflight checks`() {
        val twitchUrl = com.example.streaming.RtmpClient.buildFullUrl("rtmp://live.twitch.tv/app/", "live_12345_secret")
        assertEquals("rtmp://live.twitch.tv/app/live_12345_secret", twitchUrl)
        val parsedTwitch = com.example.streaming.RtmpClient.parseRtmpUri(twitchUrl, requireStreamKey = true)
        assertEquals("rtmp", parsedTwitch.scheme)
        assertEquals("live.twitch.tv", parsedTwitch.host)
        assertEquals("app", parsedTwitch.app)
        assertEquals("live_12345_secret", parsedTwitch.streamName)
        assertEquals("rtmp://live.twitch.tv/app", parsedTwitch.tcUrl)

        val kickPreflight = com.example.streaming.RtmpClient.parseRtmpUri("rtmps://fa723fc1b171.global-contribute.live-video.net:443/app", requireStreamKey = false)
        assertEquals("rtmps", kickPreflight.scheme)
        assertEquals(443, kickPreflight.port)
        assertEquals("app", kickPreflight.app)
        assertEquals("", kickPreflight.streamName)
    }

    @Test
    fun `hardware encoder pipeline converts Annex-B 3-byte and 4-byte NALUs cleanly to AVCC`() {
        val annexB = byteArrayOf(
            0, 0, 0, 1, 0x67, 0x42, 0xC0.toByte(),
            0, 0, 1, 0x68, 0xCE.toByte(), 0x38, 0x80.toByte()
        )
        val nalus = com.example.streaming.HardwareEncoderPipeline.splitAnnexBNalus(annexB)
        assertEquals(2, nalus.size)
        assertEquals(0x67.toByte(), nalus[0][0])
        assertEquals(3, nalus[0].size)
        assertEquals(0x68.toByte(), nalus[1][0])
        assertEquals(4, nalus[1].size)

        val avcc = com.example.streaming.HardwareEncoderPipeline.annexBToAvcc(annexB)
        // Expected: [0,0,0,3, 67,42,C0, 0,0,0,4, 68,CE,38,80] = 15 bytes
        assertEquals(15, avcc.size)
        assertEquals(3.toByte(), avcc[3])
        assertEquals(4.toByte(), avcc[10])
    }
}

