package com.example.streaming

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface
import com.example.data.model.DestinationHealthState
import com.example.data.model.DonationGoalState
import com.example.data.model.EncoderProfile
import com.example.data.model.LiveAlertEvent
import com.example.data.model.OutputOrientation
import com.example.data.model.RateControlMode
import com.example.data.model.RecordingClip
import com.example.data.model.StreamChatMessage
import com.example.data.model.StreamDestination
import com.example.data.model.StudioPreferences
import com.example.data.model.StudioScene
import com.example.data.model.StudioSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class PipelineStats(
    val currentFps: Float = 0f,
    val currentBitrateKbps: Int = 0,
    val targetBitrateKbps: Int = 6000,
    val droppedFrames: Int = 0,
    val totalFrames: Int = 0,
    val activeDestinations: Int = 0,
    val reconnectingDestinations: Int = 0,
    val destinationHealth: Map<Long, DestinationHealthState> = emptyMap()
)

/**
 * Hardware H.264 + AAC MediaCodec pipeline built for multi-hour marathon stability:
 * - Strictly monotonic presentation timestamps for MediaMuxer & RTMP
 * - Automatic exponential-backoff RTMP reconnect watchdog per destination
 * - CBR / VBR rate control, configurable keyframe interval, and H.264 profile
 * - Dynamic bitrate adaptation and GOP-aligned Replay Buffer
 */
class HardwareEncoderPipeline(
    private val context: Context,
    private val audioMixerEngine: AudioMixerEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val compositor = FrameCompositor(context)

    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null
    private var inputSurface: Surface? = null

    private var renderJob: Job? = null
    private var videoDrainJob: Job? = null
    private var audioDrainJob: Job? = null
    private var watchdogJob: Job? = null

    private val isPipelineRunning = AtomicBoolean(false)
    private val isStreamingActive = AtomicBoolean(false)
    private val isRecordingActive = AtomicBoolean(false)

    // Muxer for continuous MP4 recording
    private val muxerLock = Any()
    private var mediaMuxer: MediaMuxer? = null
    private var muxerVideoTrack = -1
    private var muxerAudioTrack = -1
    private var muxerStarted = false
    private var recordingFile: File? = null
    private var recordingStartMs = 0L
    private var recordingOrientation = OutputOrientation.HORIZONTAL
    private val lastMuxVideoPts = AtomicLong(-1L)
    private val lastMuxAudioPts = AtomicLong(-1L)

    private var cachedVideoFormat: MediaFormat? = null
    private var cachedAudioFormat: MediaFormat? = null
    private var cachedSps: ByteArray? = null
    private var cachedPps: ByteArray? = null
    private var cachedAacConfig: ByteArray? = null

    private data class BufferedPacket(
        val isVideo: Boolean,
        val data: ByteArray,
        val ptsUs: Long,
        val flags: Int
    )
    private val replayBuffer = ConcurrentLinkedDeque<BufferedPacket>()
    private var maxReplayUs: Long = 30_000_000L

    private val rtmpClients = ConcurrentHashMap<Long, RtmpClient>()
    private val targetDestinations = ConcurrentHashMap<Long, StreamDestination>()
    private val destinationHealth = ConcurrentHashMap<Long, DestinationHealthState>()
    private val reconnectAttempts = ConcurrentHashMap<Long, Int>()

    @Volatile var currentScene: StudioScene? = null
    @Volatile var currentSources: List<StudioSource> = emptyList()
    @Volatile var currentAlert: LiveAlertEvent? = null
    @Volatile var currentGoal: DonationGoalState = DonationGoalState()
    @Volatile var currentChat: List<StreamChatMessage> = emptyList()
    @Volatile var currentOrientation: OutputOrientation = OutputOrientation.HORIZONTAL
    @Volatile var activePreferences: StudioPreferences = StudioPreferences()

    private val framesRendered = AtomicInteger(0)
    private val framesDropped = AtomicInteger(0)
    private val totalFramesCounter = AtomicInteger(0)
    private var pipelineStartNs = 0L
    private var encodedWidth = 1280
    private var encodedHeight = 720
    @Volatile private var dynamicTargetBitrateKbps = 6000

    fun updateLiveBitrate(bitrateKbps: Int) {
        dynamicTargetBitrateKbps = bitrateKbps.coerceIn(800, 18000)
        val codec = videoEncoder ?: return
        runCatching {
            val bundle = Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, dynamicTargetBitrateKbps * 1000)
            }
            codec.setParameters(bundle)
        }
    }

    fun startStreaming(
        destinations: List<StreamDestination>,
        prefs: StudioPreferences,
        onStatus: (String) -> Unit
    ) {
        activePreferences = prefs
        dynamicTargetBitrateKbps = prefs.videoBitrateKbps
        scope.launch(Dispatchers.IO) {
            val enabled = destinations.filter { it.isEnabled && it.rtmpUrl.isNotBlank() && it.streamKey.isNotBlank() }
            if (enabled.isEmpty()) {
                onStatus("Add and enable at least one Stream Destination with a valid RTMP URL and Stream Key.")
                return@launch
            }

            ensurePipelineStarted(prefs, forRecordingOnly = false)
            targetDestinations.clear()
            destinationHealth.clear()
            reconnectAttempts.clear()
            enabled.forEach { dest ->
                targetDestinations[dest.id] = dest
                destinationHealth[dest.id] = DestinationHealthState.CONNECTING
            }

            val connectedNames = mutableListOf<String>()
            val failures = mutableListOf<String>()

            for (dest in enabled) {
                val client = RtmpClient(dest.id, dest.name, dest.rtmpUrl, dest.streamKey).apply {
                    audioSampleRateHz = prefs.audioSampleRateHz
                    audioBitrateKbps = prefs.audioBitrateKbps
                }
                val res = client.connect(encodedWidth, encodedHeight, prefs.targetFps, prefs.videoBitrateKbps)
                if (res.isSuccess) {
                    cachedAacConfig?.let { client.sendAudioConfig(it) }
                    val sps = cachedSps
                    val pps = cachedPps
                    if (sps != null && pps != null) {
                        client.sendVideoConfig(sps, pps)
                    }
                    rtmpClients[dest.id] = client
                    destinationHealth[dest.id] = DestinationHealthState.LIVE
                    connectedNames.add(dest.name)
                } else {
                    destinationHealth[dest.id] = DestinationHealthState.FAILED
                    failures.add("${dest.name}: ${res.exceptionOrNull()?.message ?: "Connection failed"}")
                }
            }

            if (rtmpClients.isEmpty()) {
                isStreamingActive.set(false)
                stopPipelineIfIdle()
                onStatus(failures.joinToString("\n").ifBlank { "Unable to connect to RTMP server." })
            } else {
                isStreamingActive.set(true)
                startWatchdog(prefs)
                if (failures.isNotEmpty()) {
                    onStatus("Live on ${connectedNames.size}/${enabled.size}. Failed: ${failures.joinToString("; ")}")
                }
            }
        }
    }

    private fun startWatchdog(prefs: StudioPreferences) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch(Dispatchers.IO) {
            while (isActive && isStreamingActive.get()) {
                delay(2500)
                if (!activePreferences.autoReconnectEnabled) continue
                for ((id, dest) in targetDestinations) {
                    val client = rtmpClients[id]
                    if (client == null || !client.isActive()) {
                        val attempt = (reconnectAttempts[id] ?: 0) + 1
                        reconnectAttempts[id] = attempt
                        destinationHealth[id] = DestinationHealthState.RECONNECTING
                        val backoffMs = (1500L * attempt).coerceAtMost(12_000L)
                        delay(backoffMs)
                        if (!isStreamingActive.get()) break

                        val newClient = RtmpClient(dest.id, dest.name, dest.rtmpUrl, dest.streamKey).apply {
                            audioSampleRateHz = prefs.audioSampleRateHz
                            audioBitrateKbps = prefs.audioBitrateKbps
                        }
                        val res = newClient.connect(encodedWidth, encodedHeight, prefs.targetFps, dynamicTargetBitrateKbps)
                        if (res.isSuccess) {
                            cachedAacConfig?.let { newClient.sendAudioConfig(it) }
                            val sps = cachedSps
                            val pps = cachedPps
                            if (sps != null && pps != null) {
                                newClient.sendVideoConfig(sps, pps)
                            }
                            rtmpClients[id] = newClient
                            destinationHealth[id] = DestinationHealthState.LIVE
                            reconnectAttempts[id] = 0
                        } else {
                            destinationHealth[id] = DestinationHealthState.RECONNECTING
                        }
                    }
                }
            }
        }
    }

    fun stopStreaming() {
        isStreamingActive.set(false)
        watchdogJob?.cancel()
        watchdogJob = null
        rtmpClients.values.forEach { it.close() }
        rtmpClients.clear()
        targetDestinations.clear()
        destinationHealth.clear()
        reconnectAttempts.clear()
        stopPipelineIfIdle()
    }

    fun startRecording(prefs: StudioPreferences, orientation: OutputOrientation): Boolean {
        if (isRecordingActive.get()) return true
        activePreferences = prefs
        recordingOrientation = orientation
        ensurePipelineStarted(prefs, forRecordingOnly = !isStreamingActive.get())

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "mobileLabs_$timestamp.mp4"
        val tempFile = File(context.cacheDir, fileName)
        recordingFile = tempFile
        recordingStartMs = System.currentTimeMillis()
        lastMuxVideoPts.set(-1L)
        lastMuxAudioPts.set(-1L)

        val muxer = runCatching {
            MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }.getOrNull() ?: return false

        synchronized(muxerLock) {
            mediaMuxer = muxer
            muxerVideoTrack = -1
            muxerAudioTrack = -1
            muxerStarted = false
            cachedVideoFormat?.let { muxerVideoTrack = muxer.addTrack(it) }
            cachedAudioFormat?.let { muxerAudioTrack = muxer.addTrack(it) }
            if (muxerVideoTrack >= 0 && muxerAudioTrack >= 0) {
                muxer.start()
                muxerStarted = true
            }
        }
        isRecordingActive.set(true)
        return true
    }

    fun stopRecording(onSaved: (RecordingClip) -> Unit) {
        if (!isRecordingActive.getAndSet(false)) return
        val tempFile = recordingFile
        val durationSec = ((System.currentTimeMillis() - recordingStartMs) / 1000L).coerceAtLeast(1L)

        synchronized(muxerLock) {
            runCatching { if (muxerStarted) mediaMuxer?.stop() }
            runCatching { mediaMuxer?.release() }
            mediaMuxer = null
            muxerVideoTrack = -1
            muxerAudioTrack = -1
            muxerStarted = false
        }
        stopPipelineIfIdle()

        if (tempFile != null && tempFile.exists() && tempFile.length() > 1024L) {
            scope.launch(Dispatchers.IO) {
                val uri = exportMp4ToMediaStore(tempFile, tempFile.name)
                val size = tempFile.length()
                tempFile.delete()
                if (uri != null) {
                    onSaved(
                        RecordingClip(
                            title = tempFile.nameWithoutExtension,
                            fileUri = uri.toString(),
                            durationSec = durationSec,
                            sizeBytes = size,
                            isReplayClip = false,
                            orientation = recordingOrientation
                        )
                    )
                }
            }
        }
    }

    fun saveReplayClip(prefs: StudioPreferences, onSaved: (RecordingClip?) -> Unit) {
        activePreferences = prefs
        scope.launch(Dispatchers.IO) {
            if (!isPipelineRunning.get()) {
                ensurePipelineStarted(prefs, forRecordingOnly = true)
                delay(1200)
            }
            val snapshot = replayBuffer.toList()
            val vFmt = cachedVideoFormat
            val aFmt = cachedAudioFormat
            if (snapshot.isEmpty() || vFmt == null || aFmt == null) {
                onSaved(null)
                return@launch
            }
            val firstKeyIdx = snapshot.indexOfFirst {
                it.isVideo && (it.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
            }
            val trimmed = if (firstKeyIdx >= 0) snapshot.drop(firstKeyIdx) else snapshot
            if (trimmed.isEmpty()) {
                onSaved(null)
                return@launch
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "mobileLabs_Replay_$timestamp.mp4"
            val tempFile = File(context.cacheDir, fileName)
            var savedUri: Uri? = null
            var durationSec = 5L
            var sizeBytes = 0L

            runCatching {
                val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val vTrack = muxer.addTrack(vFmt)
                val aTrack = muxer.addTrack(aFmt)
                muxer.start()

                val basePts = trimmed.first().ptsUs
                var lastPts = basePts
                var prevV = -1L
                var prevA = -1L
                val info = MediaCodec.BufferInfo()
                for (pkt in trimmed) {
                    var pts = (pkt.ptsUs - basePts).coerceAtLeast(0L)
                    if (pkt.isVideo) {
                        pts = maxOf(pts, prevV + 1L)
                        prevV = pts
                    } else {
                        pts = maxOf(pts, prevA + 1L)
                        prevA = pts
                    }
                    lastPts = maxOf(lastPts, pkt.ptsUs)
                    info.set(0, pkt.data.size, pts, pkt.flags)
                    muxer.writeSampleData(
                        if (pkt.isVideo) vTrack else aTrack,
                        ByteBuffer.wrap(pkt.data),
                        info
                    )
                }
                muxer.stop()
                muxer.release()
                durationSec = ((lastPts - basePts) / 1_000_000L).coerceAtLeast(1L)
                sizeBytes = tempFile.length()
                savedUri = exportMp4ToMediaStore(tempFile, fileName)
                tempFile.delete()
            }

            onSaved(
                savedUri?.let {
                    RecordingClip(
                        title = fileName.removeSuffix(".mp4"),
                        fileUri = it.toString(),
                        durationSec = durationSec,
                        sizeBytes = sizeBytes,
                        isReplayClip = true,
                        orientation = currentOrientation
                    )
                }
            )
        }
    }

    fun collectStats(): PipelineStats {
        val fps = framesRendered.getAndSet(0).toFloat()
        val bytes = rtmpClients.values.sumOf { it.getAndResetBytesSent() }
        val kbps = ((bytes * 8L) / 1000L).toInt()
        val activeCount = rtmpClients.values.count { it.isActive() }
        val reconnectingCount = destinationHealth.values.count { it == DestinationHealthState.RECONNECTING }
        return PipelineStats(
            currentFps = if (isPipelineRunning.get()) fps else 0f,
            currentBitrateKbps = kbps,
            targetBitrateKbps = dynamicTargetBitrateKbps,
            droppedFrames = framesDropped.get(),
            totalFrames = totalFramesCounter.get(),
            activeDestinations = activeCount,
            reconnectingDestinations = reconnectingCount,
            destinationHealth = destinationHealth.toMap()
        )
    }

    private fun ensurePipelineStarted(prefs: StudioPreferences, forRecordingOnly: Boolean) {
        if (isPipelineRunning.getAndSet(true)) return
        maxReplayUs = prefs.replayBufferSeconds.coerceIn(10, 120) * 1_000_000L

        val isVertical = currentOrientation == OutputOrientation.VERTICAL
        val baseH = when {
            prefs.resolutionHeight >= 1080 -> 1080
            prefs.resolutionHeight <= 480 -> 480
            else -> 720
        }
        val width = if (isVertical) baseH else (baseH * 16 / 9)
        val height = if (isVertical) (baseH * 16 / 9) else baseH
        encodedWidth = width
        encodedHeight = height

        val targetKbps = if (forRecordingOnly) prefs.recordingBitrateKbps else prefs.videoBitrateKbps
        dynamicTargetBitrateKbps = targetKbps
        val bitrateBps = targetKbps * 1000
        val fps = prefs.targetFps.coerceIn(24, 60)

        runCatching {
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, prefs.keyframeIntervalSec.coerceIn(1, 5))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val mode = if (prefs.rateControl == RateControlMode.CBR) {
                        MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
                    } else {
                        MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
                    }
                    setInteger(MediaFormat.KEY_BITRATE_MODE, mode)
                }
                val profileInt = when (prefs.encoderProfile) {
                    EncoderProfile.BASELINE -> MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
                    EncoderProfile.MAIN -> MediaCodecInfo.CodecProfileLevel.AVCProfileMain
                    EncoderProfile.HIGH -> MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
                }
                setInteger(MediaFormat.KEY_PROFILE, profileInt)
            }
            val vCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            runCatching {
                vCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }.onFailure {
                // Fallback without strict profile/CBR if a chipset restricts them
                val fallback = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
                    setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, prefs.keyframeIntervalSec.coerceIn(1, 5))
                }
                vCodec.configure(fallback, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
            val surface = vCodec.createInputSurface()
            vCodec.start()
            videoEncoder = vCodec
            inputSurface = surface

            val audioFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                AudioMixerEngine.SAMPLE_RATE,
                1
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, prefs.audioBitrateKbps * 1000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            val aCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            aCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            aCodec.start()
            audioEncoder = aCodec

            pipelineStartNs = System.nanoTime()
            framesDropped.set(0)
            totalFramesCounter.set(0)

            audioMixerEngine.onMixedPcmFrame = { pcm, count ->
                feedAudioPcm(pcm, count)
            }

            startRenderLoop(surface, width, height, fps)
            startVideoDrainLoop(vCodec)
            startAudioDrainLoop(aCodec)
        }.onFailure {
            stopAll()
        }
    }

    private fun startRenderLoop(surface: Surface, width: Int, height: Int, fps: Int) {
        val frameIntervalNs = 1_000_000_000L / fps.coerceIn(24, 60)
        renderJob = scope.launch(Dispatchers.Default) {
            var nextFrameDeadlineNs = System.nanoTime()
            while (isActive && isPipelineRunning.get()) {
                try {
                    val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        surface.lockHardwareCanvas()
                    } else {
                        surface.lockCanvas(null)
                    }
                    try {
                        compositor.renderFrame(
                            canvas = canvas,
                            width = width,
                            height = height,
                            orientation = currentOrientation,
                            scene = currentScene,
                            sources = currentSources,
                            activeAlert = currentAlert,
                            donationGoal = currentGoal,
                            recentChat = currentChat,
                            privacyShieldActive = activePreferences.privacyShieldActive
                        )
                    } finally {
                        surface.unlockCanvasAndPost(canvas)
                    }
                    framesRendered.incrementAndGet()
                    totalFramesCounter.incrementAndGet()
                } catch (_: Throwable) {
                    framesDropped.incrementAndGet()
                }
                nextFrameDeadlineNs += frameIntervalNs
                val sleepNs = nextFrameDeadlineNs - System.nanoTime()
                if (sleepNs > 1_000_000L) {
                    delay(sleepNs / 1_000_000L)
                } else if (sleepNs < -frameIntervalNs * 3) {
                    // Re-sync deadline if system lagged briefly
                    nextFrameDeadlineNs = System.nanoTime()
                }
            }
        }
    }

    private fun feedAudioPcm(pcm: ShortArray, sampleCount: Int) {
        val codec = audioEncoder ?: return
        if (!isPipelineRunning.get()) return
        try {
            val inIdx = codec.dequeueInputBuffer(0)
            if (inIdx >= 0) {
                val buf = codec.getInputBuffer(inIdx) ?: return
                buf.clear()
                val bytesLen = minOf(sampleCount * 2, buf.remaining())
                for (i in 0 until bytesLen / 2) {
                    val s = pcm[i].toInt()
                    buf.put((s and 0xFF).toByte())
                    buf.put(((s ushr 8) and 0xFF).toByte())
                }
                val ptsUs = ((System.nanoTime() - pipelineStartNs) / 1000L).coerceAtLeast(0L)
                codec.queueInputBuffer(inIdx, 0, bytesLen, ptsUs, 0)
            }
        } catch (_: Throwable) {
        }
    }

    private fun startVideoDrainLoop(codec: MediaCodec) {
        videoDrainJob = scope.launch(Dispatchers.IO) {
            val info = MediaCodec.BufferInfo()
            while (isActive && isPipelineRunning.get()) {
                try {
                    val outIdx = codec.dequeueOutputBuffer(info, 15_000)
                    when {
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val fmt = codec.outputFormat
                            cachedVideoFormat = fmt
                            extractSpsPps(fmt)
                            synchronized(muxerLock) {
                                if (mediaMuxer != null && muxerVideoTrack < 0) {
                                    muxerVideoTrack = mediaMuxer!!.addTrack(fmt)
                                    if (muxerVideoTrack >= 0 && muxerAudioTrack >= 0 && !muxerStarted) {
                                        mediaMuxer!!.start()
                                        muxerStarted = true
                                    }
                                }
                            }
                        }

                        outIdx >= 0 -> {
                            val encodedBuf = codec.getOutputBuffer(outIdx)
                            if (encodedBuf != null && info.size > 0) {
                                encodedBuf.position(info.offset)
                                encodedBuf.limit(info.offset + info.size)
                                val bytes = ByteArray(info.size)
                                encodedBuf.get(bytes)

                                val isConfig = (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                                if (isConfig) {
                                    parseAnnexBConfig(bytes)
                                } else {
                                    val rawPts = ((System.nanoTime() - pipelineStartNs) / 1000L).coerceAtLeast(0L)
                                    val monoPts = lastMuxVideoPts.updateAndGet { prev -> maxOf(prev + 1L, rawPts) }

                                    synchronized(muxerLock) {
                                        if (muxerStarted && muxerVideoTrack >= 0) {
                                            info.presentationTimeUs = monoPts
                                            runCatching {
                                                mediaMuxer?.writeSampleData(muxerVideoTrack, ByteBuffer.wrap(bytes), info)
                                            }
                                        }
                                    }
                                    appendReplayPacket(
                                        BufferedPacket(isVideo = true, data = bytes, ptsUs = monoPts, flags = info.flags)
                                    )
                                    if (isStreamingActive.get()) {
                                        val isKey = (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                                        val avcc = annexBToAvcc(bytes)
                                        val tsMs = (monoPts / 1000L).toInt()
                                        for ((id, client) in rtmpClients) {
                                            val ok = client.sendVideoFrame(avcc, isKey, tsMs)
                                            if (!ok) {
                                                destinationHealth[id] = DestinationHealthState.RECONNECTING
                                            }
                                        }
                                    }
                                }
                            }
                            codec.releaseOutputBuffer(outIdx, false)
                        }
                    }
                } catch (_: Throwable) {
                    delay(10)
                }
            }
        }
    }

    private fun startAudioDrainLoop(codec: MediaCodec) {
        audioDrainJob = scope.launch(Dispatchers.IO) {
            val info = MediaCodec.BufferInfo()
            while (isActive && isPipelineRunning.get()) {
                try {
                    val outIdx = codec.dequeueOutputBuffer(info, 15_000)
                    when {
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val fmt = codec.outputFormat
                            cachedAudioFormat = fmt
                            fmt.getByteBuffer("csd-0")?.let { csd ->
                                val arr = ByteArray(csd.remaining())
                                csd.get(arr)
                                cachedAacConfig = arr
                                rtmpClients.values.forEach { it.sendAudioConfig(arr) }
                            }
                            synchronized(muxerLock) {
                                if (mediaMuxer != null && muxerAudioTrack < 0) {
                                    muxerAudioTrack = mediaMuxer!!.addTrack(fmt)
                                    if (muxerVideoTrack >= 0 && muxerAudioTrack >= 0 && !muxerStarted) {
                                        mediaMuxer!!.start()
                                        muxerStarted = true
                                    }
                                }
                            }
                        }

                        outIdx >= 0 -> {
                            val encodedBuf = codec.getOutputBuffer(outIdx)
                            if (encodedBuf != null && info.size > 0) {
                                encodedBuf.position(info.offset)
                                encodedBuf.limit(info.offset + info.size)
                                val bytes = ByteArray(info.size)
                                encodedBuf.get(bytes)

                                val isConfig = (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                                if (isConfig) {
                                    cachedAacConfig = bytes
                                    rtmpClients.values.forEach { it.sendAudioConfig(bytes) }
                                } else {
                                    val rawPts = ((System.nanoTime() - pipelineStartNs) / 1000L).coerceAtLeast(0L)
                                    val monoPts = lastMuxAudioPts.updateAndGet { prev -> maxOf(prev + 1L, rawPts) }

                                    synchronized(muxerLock) {
                                        if (muxerStarted && muxerAudioTrack >= 0) {
                                            info.presentationTimeUs = monoPts
                                            runCatching {
                                                mediaMuxer?.writeSampleData(muxerAudioTrack, ByteBuffer.wrap(bytes), info)
                                            }
                                        }
                                    }
                                    appendReplayPacket(
                                        BufferedPacket(isVideo = false, data = bytes, ptsUs = monoPts, flags = info.flags)
                                    )
                                    if (isStreamingActive.get()) {
                                        val tsMs = (monoPts / 1000L).toInt()
                                        for ((id, client) in rtmpClients) {
                                            val ok = client.sendAudioFrame(bytes, tsMs)
                                            if (!ok) {
                                                destinationHealth[id] = DestinationHealthState.RECONNECTING
                                            }
                                        }
                                    }
                                }
                            }
                            codec.releaseOutputBuffer(outIdx, false)
                        }
                    }
                } catch (_: Throwable) {
                    delay(10)
                }
            }
        }
    }

    private fun appendReplayPacket(pkt: BufferedPacket) {
        replayBuffer.addLast(pkt)
        val cutoff = pkt.ptsUs - maxReplayUs
        while (true) {
            val head = replayBuffer.peekFirst() ?: break
            if (head.ptsUs < cutoff) {
                replayBuffer.pollFirst()
            } else {
                break
            }
        }
    }

    private fun extractSpsPps(fmt: MediaFormat) {
        val csd0 = fmt.getByteBuffer("csd-0")
        val csd1 = fmt.getByteBuffer("csd-1")
        if (csd0 != null && csd1 != null) {
            val spsRaw = ByteArray(csd0.remaining()).also { csd0.get(it) }
            val ppsRaw = ByteArray(csd1.remaining()).also { csd1.get(it) }
            val sps = stripStartCode(spsRaw)
            val pps = stripStartCode(ppsRaw)
            cachedSps = sps
            cachedPps = pps
            rtmpClients.values.forEach { it.sendVideoConfig(sps, pps) }
        }
    }

    private fun parseAnnexBConfig(bytes: ByteArray) {
        val nalus = splitAnnexBNalus(bytes)
        var sps: ByteArray? = null
        var pps: ByteArray? = null
        for (nalu in nalus) {
            if (nalu.isEmpty()) continue
            val type = nalu[0].toInt() and 0x1F
            if (type == 7) sps = nalu
            if (type == 8) pps = nalu
        }
        if (sps != null && pps != null) {
            cachedSps = sps
            cachedPps = pps
            rtmpClients.values.forEach { it.sendVideoConfig(sps, pps) }
        }
    }

    private fun splitAnnexBNalus(data: ByteArray): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        var i = 0
        var start = -1
        while (i < data.size - 3) {
            val is4Byte = data[i] == 0.toByte() && data[i + 1] == 0.toByte() &&
                data[i + 2] == 0.toByte() && data[i + 3] == 1.toByte()
            val is3Byte = !is4Byte && data[i] == 0.toByte() && data[i + 1] == 0.toByte() && data[i + 2] == 1.toByte()
            if (is4Byte || is3Byte) {
                if (start >= 0 && i > start) {
                    result.add(data.copyOfRange(start, i))
                }
                i += if (is4Byte) 4 else 3
                start = i
            } else {
                i++
            }
        }
        if (start >= 0 && start < data.size) {
            result.add(data.copyOfRange(start, data.size))
        } else if (result.isEmpty() && data.isNotEmpty()) {
            result.add(data)
        }
        return result
    }

    private fun stripStartCode(nalu: ByteArray): ByteArray {
        return when {
            nalu.size >= 4 && nalu[0] == 0.toByte() && nalu[1] == 0.toByte() && nalu[2] == 0.toByte() && nalu[3] == 1.toByte() ->
                nalu.copyOfRange(4, nalu.size)
            nalu.size >= 3 && nalu[0] == 0.toByte() && nalu[1] == 0.toByte() && nalu[2] == 1.toByte() ->
                nalu.copyOfRange(3, nalu.size)
            else -> nalu
        }
    }

    private fun annexBToAvcc(data: ByteArray): ByteArray {
        val nalus = splitAnnexBNalus(data)
        val total = nalus.sumOf { 4 + it.size }
        val out = ByteArray(total)
        var pos = 0
        for (nalu in nalus) {
            val len = nalu.size
            out[pos++] = ((len ushr 24) and 0xFF).toByte()
            out[pos++] = ((len ushr 16) and 0xFF).toByte()
            out[pos++] = ((len ushr 8) and 0xFF).toByte()
            out[pos++] = (len and 0xFF).toByte()
            System.arraycopy(nalu, 0, out, pos, len)
            pos += len
        }
        return out
    }

    private fun exportMp4ToMediaStore(sourceFile: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/mobileLabs")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { inp -> inp.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        }.getOrNull()
    }

    private fun stopPipelineIfIdle() {
        if (!isStreamingActive.get() && !isRecordingActive.get()) {
            stopAll()
        }
    }

    fun stopAll() {
        isPipelineRunning.set(false)
        watchdogJob?.cancel()
        watchdogJob = null
        audioMixerEngine.onMixedPcmFrame = null
        renderJob?.cancel()
        videoDrainJob?.cancel()
        audioDrainJob?.cancel()
        renderJob = null
        videoDrainJob = null
        audioDrainJob = null

        runCatching { videoEncoder?.stop() }
        runCatching { videoEncoder?.release() }
        videoEncoder = null

        runCatching { audioEncoder?.stop() }
        runCatching { audioEncoder?.release() }
        audioEncoder = null

        runCatching { inputSurface?.release() }
        inputSurface = null

        rtmpClients.values.forEach { it.close() }
        rtmpClients.clear()
        replayBuffer.clear()
    }
}
