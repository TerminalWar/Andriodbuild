package com.example.data.model

enum class TransitionType(val label: String, val durationMs: Int) {
    CUT("Cut", 0),
    FADE("Fade", 300),
    SLIDE("Slide", 350)
}

enum class OutputOrientation(val label: String, val aspectLabel: String) {
    HORIZONTAL("Horizontal", "16:9"),
    VERTICAL("Vertical", "9:16")
}

enum class SourceType(val displayName: String, val description: String) {
    SCREEN_CAPTURE("Screen / Game Capture", "Real-time Android MediaProjection display capture"),
    CAMERA("Video Capture Device", "Live front or rear CameraX feed"),
    WEBCAM_FRAME("Webcam Frame", "Styled border & glow frame for your camera"),
    IMAGE_OVERLAY("Image / Logo", "PNG/JPG overlay from Android Photo Picker"),
    TEXT_BANNER("Text (GDI+ / Ticker)", "Custom stream label, handle, or BRB banner"),
    ALERTBOX("Alertbox", "Live follower, sub & tip pop-up notifications"),
    DONATION_GOAL("Donation Goal", "Real-time progress bar overlay"),
    CHATBOX("Chatbox", "On-stream transparent live chat overlay"),
    COLOR_SOURCE("Color / Backdrop", "Solid or gradient studio backdrop"),
    FOLDER("Group Folder", "Organize and toggle multiple sources together")
}

data class NormalizedRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class StudioScene(
    val id: Long = 0,
    val collectionId: Long,
    val name: String,
    val sortOrder: Int,
    val backdropHex: Long = 0xFF0E1722
)

data class StudioSource(
    val id: Long = 0,
    val sceneId: Long,
    val parentFolderId: Long? = null,
    val name: String,
    val type: SourceType,
    val sortOrder: Int,
    val isVisibleHorizontal: Boolean = true,
    val isVisibleVertical: Boolean = true,
    val isLocked: Boolean = false,
    // Normalized [0..1] placement in Horizontal (16:9) canvas
    val hRect: NormalizedRect = NormalizedRect(0f, 0f, 1f, 1f),
    // Normalized [0..1] placement in Vertical (9:16) canvas
    val vRect: NormalizedRect = NormalizedRect(0f, 0f, 1f, 1f),
    // Source-specific properties
    val textContent: String = "",
    val accentColorHex: Long = 0xFF31C3A2,
    val imageUri: String? = null,
    val cameraFacingFront: Boolean = true,
    val opacity: Float = 1f,
    val cornerRadiusDp: Int = 10
)

enum class AudioChannelId(val label: String) {
    MIC_AUX("Mic/Aux"),
    DEVICE_AUDIO("Device Audio"),
    ALERTS_MEDIA("Alerts & Media")
}

data class AudioChannelConfig(
    val id: String,
    val name: String,
    val gainDb: Float = 0f, // -60f to +12f
    val isMuted: Boolean = false,
    val noiseGateEnabled: Boolean = true,
    val noiseGateThresholdDb: Float = -45f,
    val compressorEnabled: Boolean = true,
    val monitorEnabled: Boolean = false
)

data class AudioMeterReading(
    val channelId: String,
    val rmsDb: Float = -60f,
    val peakDb: Float = -60f,
    val isClipping: Boolean = false
)

enum class StreamPlatform(
    val displayName: String,
    val defaultIngestUrl: String,
    val colorHex: Long
) {
    TWITCH("Twitch", "rtmp://live.twitch.tv/app/", 0xFF9146FF),
    YOUTUBE("YouTube Live", "rtmp://a.rtmp.youtube.com/live2/", 0xFFFF0033),
    KICK("Kick", "rtmps://fa723fc1b171.global-contribute.live-video.net/app/", 0xFF53FC18),
    FACEBOOK("Facebook Live", "rtmps://live-api-s.facebook.com:443/rtmp/", 0xFF1877F2),
    TIKTOK("TikTok Live", "rtmp://push.tiktokv.com/live/", 0xFFFE2C55),
    CUSTOM_RTMP("Custom RTMP", "rtmp://", 0xFF38BDF8)
}

data class StreamDestination(
    val id: Long = 0,
    val name: String,
    val platform: StreamPlatform,
    val rtmpUrl: String,
    val streamKey: String,
    val orientation: OutputOrientation = OutputOrientation.HORIZONTAL,
    val isEnabled: Boolean = true
)

enum class ChatPlatform(val shortTag: String, val colorHex: Long) {
    TWITCH("TW", 0xFF9146FF),
    YOUTUBE("YT", 0xFFFF0033),
    KICK("KK", 0xFF53FC18),
    FACEBOOK("FB", 0xFF1877F2),
    SYSTEM("SYS", 0xFF31C3A2)
}

data class StreamChatMessage(
    val id: String,
    val platform: ChatPlatform,
    val username: String,
    val message: String,
    val nameColorHex: Long = 0xFF31C3A2,
    val badge: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

data class LiveAlertEvent(
    val id: Long,
    val title: String,
    val subtitle: String,
    val kind: String, // "FOLLOW", "SUB", "TIP", "RAID"
    val accentHex: Long
)

data class DonationGoalState(
    val title: String = "New Streaming Mic Goal",
    val currentAmount: Float = 145f,
    val targetAmount: Float = 200f
)

data class RecordingClip(
    val id: Long = 0,
    val title: String,
    val fileUri: String,
    val durationSec: Long,
    val sizeBytes: Long,
    val isReplayClip: Boolean,
    val orientation: OutputOrientation,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class StudioPreferences(
    val videoBitrateKbps: Int = 4500,
    val targetFps: Int = 30,
    val resolutionHeight: Int = 720, // 720 or 1080
    val horizontalEnabled: Boolean = true,
    val verticalEnabled: Boolean = true,
    val studioModeEnabled: Boolean = false,
    val replayBufferSeconds: Int = 30,
    val twitchChannel: String = "",
    val streamTitle: String = "Late Night Ranked Grind | mobileLabs",
    val overlayThemeId: String = "mint_pro"
)

data class OverlayThemePreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val frameCornerDp: Int
)

enum class StreamConnectionState {
    OFFLINE,
    CONNECTING,
    LIVE,
    ERROR
}

data class BroadcastTelemetry(
    val connectionState: StreamConnectionState = StreamConnectionState.OFFLINE,
    val isRecording: Boolean = false,
    val isReplayBufferArmed: Boolean = true,
    val isScreenCaptureActive: Boolean = false,
    val elapsedLiveSeconds: Long = 0L,
    val elapsedRecordSeconds: Long = 0L,
    val currentBitrateKbps: Int = 0,
    val currentFps: Float = 0f,
    val cpuUsagePercent: Float = 1.7f,
    val droppedFrames: Int = 0,
    val totalFrames: Int = 0,
    val activeDestinationsCount: Int = 0,
    val connectedViewerHint: Int = 0,
    val statusMessage: String = "Ready — All features unlocked"
)
