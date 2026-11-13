package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MobileLabsApplication
import com.example.data.local.SceneCollectionEntity
import com.example.data.model.AudioChannelConfig
import com.example.data.model.AudioChannelId
import com.example.data.model.AudioMeterReading
import com.example.data.model.BroadcastLogEntry
import com.example.data.model.BroadcastTelemetry
import com.example.data.model.ChatPlatform
import com.example.data.model.DestinationHealthState
import com.example.data.model.DonationGoalState
import com.example.data.model.LiveAlertEvent
import com.example.data.model.NormalizedRect
import com.example.data.model.OutputOrientation
import com.example.data.model.OverlayThemePreset
import com.example.data.model.RecordingClip
import com.example.data.model.SourceType
import com.example.data.model.StreamChatMessage
import com.example.data.model.StreamConnectionState
import com.example.data.model.StreamDestination
import com.example.data.model.StreamHealthLevel
import com.example.data.model.StreamMarker
import com.example.data.model.StudioPreferences
import com.example.data.model.StudioScene
import com.example.data.model.StudioSource
import com.example.data.model.TransitionType
import com.example.streaming.ChatConnectionStatus
import com.example.streaming.MediaProjectionHolder
import com.example.streaming.RtmpClient
import com.example.streaming.StreamBroadcastService
import com.example.streaming.VideoOverlayManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

enum class TransformPreset(val label: String) {
    CENTER_BOTH("Center Screen"),
    CENTER_H("Center H"),
    CENTER_V("Center V"),
    FIT_SCREEN("Fit Full"),
    TOP_LEFT("Top-Left"),
    TOP_RIGHT("Top-Right"),
    BOTTOM_LEFT("Bottom-Left"),
    BOTTOM_RIGHT("Bottom-Right")
}

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MobileLabsApplication
    private val repo = app.studioRepository
    private val audioEngine = app.audioMixerEngine
    private val chatClient = app.twitchChatClient
    private val pipeline = app.encoderPipeline
    private val logTimeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    val collections: StateFlow<List<SceneCollectionEntity>> = repo.collections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeCollectionId = MutableStateFlow<Long?>(null)

    val scenes: StateFlow<List<StudioScene>> = combine(collections, activeCollectionId) { cols, overrideId ->
        overrideId ?: cols.firstOrNull { it.isActive }?.id ?: cols.firstOrNull()?.id
    }.flatMapLatest { colId ->
        if (colId == null) flowOf(emptyList()) else repo.observeScenes(colId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _programSceneId = MutableStateFlow<Long?>(null)
    private val _previewSceneId = MutableStateFlow<Long?>(null)

    val programScene: StateFlow<StudioScene?> = combine(scenes, _programSceneId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val previewScene: StateFlow<StudioScene?> = combine(scenes, _previewSceneId, programScene) { list, id, prog ->
        list.firstOrNull { it.id == id } ?: prog
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Optimistic in-memory rect map for 120Hz smooth canvas dragging/resizing before committing to Room
    private val dragRectOverrides = MutableStateFlow<Map<Pair<Long, OutputOrientation>, NormalizedRect>>(emptyMap())

    private val dbProgramSources = programScene.flatMapLatest { scene ->
        if (scene == null) flowOf(emptyList()) else repo.observeSources(scene.id)
    }

    private val dbPreviewSources = previewScene.flatMapLatest { scene ->
        if (scene == null) flowOf(emptyList()) else repo.observeSources(scene.id)
    }

    val programSources: StateFlow<List<StudioSource>> = combine(dbProgramSources, dragRectOverrides) { list, overrides ->
        applyRectOverrides(list, overrides)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previewSources: StateFlow<List<StudioSource>> = combine(dbPreviewSources, dragRectOverrides) { list, overrides ->
        applyRectOverrides(list, overrides)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioChannels: StateFlow<List<AudioChannelConfig>> = repo.audioChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meterReadings: StateFlow<Map<String, AudioMeterReading>> = audioEngine.meterReadings

    val destinations: StateFlow<List<StreamDestination>> = repo.destinations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clips: StateFlow<List<RecordingClip>> = repo.clips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<StudioPreferences> = repo.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioPreferences())

    private val _selectedTransition = MutableStateFlow(TransitionType.FADE)
    val selectedTransition: StateFlow<TransitionType> = _selectedTransition.asStateFlow()

    private val _selectedSourceId = MutableStateFlow<Long?>(null)
    val selectedSourceId: StateFlow<Long?> = _selectedSourceId.asStateFlow()

    private val _editingOrientation = MutableStateFlow(OutputOrientation.HORIZONTAL)
    val editingOrientation: StateFlow<OutputOrientation> = _editingOrientation.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            StreamChatMessage(
                id = "welcome-1",
                platform = ChatPlatform.SYSTEM,
                username = "mobileLabs",
                message = "Pro broadcast engine ready. Connect Twitch IRC above or press F12 for keyboard shortcuts.",
                nameColorHex = 0xFF31C3A2
            )
        )
    )
    val chatMessages: StateFlow<List<StreamChatMessage>> = _chatMessages.asStateFlow()

    private val _pinnedChatMessage = MutableStateFlow<StreamChatMessage?>(null)
    val pinnedChatMessage: StateFlow<StreamChatMessage?> = _pinnedChatMessage.asStateFlow()

    val chatStatus: StateFlow<ChatConnectionStatus> = chatClient.status

    private val _activeAlert = MutableStateFlow<LiveAlertEvent?>(null)
    val activeAlert: StateFlow<LiveAlertEvent?> = _activeAlert.asStateFlow()

    private val _donationGoal = MutableStateFlow(DonationGoalState())
    val donationGoal: StateFlow<DonationGoalState> = _donationGoal.asStateFlow()

    private val _telemetry = MutableStateFlow(BroadcastTelemetry())
    val telemetry: StateFlow<BroadcastTelemetry> = _telemetry.asStateFlow()

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    private val _hasAudioPermission = MutableStateFlow(false)
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    private val _previewTick = MutableStateFlow(0L)
    val previewTick: StateFlow<Long> = _previewTick.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _streamMarkers = MutableStateFlow<List<StreamMarker>>(emptyList())
    val streamMarkers: StateFlow<List<StreamMarker>> = _streamMarkers.asStateFlow()

    private val _broadcastLogs = MutableStateFlow<List<BroadcastLogEntry>>(emptyList())
    val broadcastLogs: StateFlow<List<BroadcastLogEntry>> = _broadcastLogs.asStateFlow()

    private var testWidgetIndex = 0
    private var lastCpuTimeMs = 0L
    private var lastWallTimeMs = 0L

    val builtInOverlayThemes = listOf(
        OverlayThemePreset("mint_pro", "Mint Studio Pro", "Signature teal-mint frames & crisp glass", 0xFF31C3A2, 0xFF56E3C2, 12),
        OverlayThemePreset("cyber_crimson", "Cyber Crimson", "High-contrast esports red & obsidian", 0xFFFF4557, 0xFFF59E0B, 6),
        OverlayThemePreset("twitch_violet", "Creator Violet", "Neon purple glow for Twitch & Kick", 0xFF9146FF, 0xFF38BDF8, 16),
        OverlayThemePreset("clean_minimal", "Broadcast Minimal", "Sharp broadcast white & slate lines", 0xFFE2E8F0, 0xFF38BDF8, 4)
    )

    init {
        appendLog("INFO", "STUDIO", "mobileLabs broadcast engine initialized")
        pipeline.onLogEvent = { level, tag, msg ->
            appendLog(level, tag, msg)
        }

        viewModelScope.launch {
            repo.ensureSeeded()
        }
        viewModelScope.launch {
            audioChannels.collect { channels ->
                audioEngine.updateConfigs(channels)
            }
        }
        viewModelScope.launch {
            combine(
                programScene,
                programSources,
                activeAlert,
                donationGoal,
                chatMessages
            ) { scene, srcs, alert, goal, chat ->
                pipeline.currentScene = scene
                pipeline.currentSources = srcs
                pipeline.currentAlert = alert
                pipeline.currentGoal = goal
                pipeline.currentChat = chat

                val activeVideoSources = srcs.filter { it.type == SourceType.VIDEO_OVERLAY && !it.imageUri.isNullOrBlank() }
                val activeVideoUris = activeVideoSources.mapNotNull { it.imageUri }.toSet()
                activeVideoSources.forEach { src ->
                    src.imageUri?.let { uri -> VideoOverlayManager.ensurePlaying(app, uri, src.videoSpeed) }
                }
                VideoOverlayManager.stopUnused(activeVideoUris)
            }.collect {}
        }
        viewModelScope.launch {
            preferences.collect { prefs ->
                pipeline.activePreferences = prefs
                pipeline.updateLiveBitrate(prefs.videoBitrateKbps)
                audioEngine.updateSampleRate(prefs.audioSampleRateHz)
                audioEngine.autoDuckingEnabled = prefs.autoDuckingEnabled
                audioEngine.duckingAmountDb = prefs.duckingAmountDb
                _selectedTransition.value = prefs.transitionType
                if (prefs.twitchChannel.isNotBlank() && chatClient.status.value == ChatConnectionStatus.DISCONNECTED) {
                    chatClient.connect(prefs.twitchChannel)
                    appendLog("INFO", "CHAT", "Connecting Twitch IRC: #${prefs.twitchChannel}")
                }
            }
        }
        viewModelScope.launch {
            chatClient.messages.collect { msg ->
                _chatMessages.update { (it + msg).takeLast(120) }
            }
        }
        viewModelScope.launch {
            chatClient.alerts.collect { alert ->
                showAlert(alert)
            }
        }
        viewModelScope.launch {
            MediaProjectionHolder.isScreenCapturing.collect { capturing ->
                _telemetry.update { it.copy(isScreenCaptureActive = capturing) }
            }
        }
        // Adaptive Preview Refresh + 1Hz Telemetry Watchdog
        viewModelScope.launch {
            var lastTelemetryMs = 0L
            while (isActive) {
                val srcs = programSources.value
                val prefs = preferences.value
                val hasVideoOverlay = srcs.any { it.type == SourceType.VIDEO_OVERLAY }
                val hasCamera = _hasCameraPermission.value && srcs.any { it.type == SourceType.CAMERA }
                val needsFastPreview = MediaProjectionHolder.isScreenCapturing.value || hasVideoOverlay ||
                    (hasCamera && (prefs.studioModeEnabled || (prefs.horizontalEnabled && prefs.verticalEnabled) || srcs.any { it.chromaKeyEnabled }))

                val stepMs = if (prefs.gamePerformanceMode) 200L else 50L
                if (needsFastPreview) {
                    _previewTick.value = System.nanoTime()
                }
                val now = System.currentTimeMillis()
                if (now - lastTelemetryMs >= 1000L) {
                    lastTelemetryMs = now
                    updateTelemetry1Hz()
                }
                delay(stepMs)
            }
        }
    }

    private fun applyRectOverrides(
        sources: List<StudioSource>,
        overrides: Map<Pair<Long, OutputOrientation>, NormalizedRect>
    ): List<StudioSource> {
        if (overrides.isEmpty()) return sources
        return sources.map { src ->
            val hOver = overrides[src.id to OutputOrientation.HORIZONTAL]
            val vOver = overrides[src.id to OutputOrientation.VERTICAL]
            if (hOver == null && vOver == null) src
            else src.copy(
                hRect = hOver ?: src.hRect,
                vRect = vOver ?: src.vRect
            )
        }
    }

    fun getLatestScreenBitmap(): Bitmap? = MediaProjectionHolder.getLatestScreenBitmap()

    fun getVideoOverlayBitmap(uri: String?): Bitmap? = uri?.let { MediaProjectionHolder.getVideoOverlayFrame(it) }

    fun appendLog(level: String, tag: String, message: String) {
        val entry = BroadcastLogEntry(
            id = System.nanoTime(),
            timeText = logTimeFmt.format(Date()),
            level = level,
            tag = tag,
            message = message
        )
        _broadcastLogs.update { (it + entry).takeLast(150) }
    }

    fun clearLogs() {
        _broadcastLogs.value = emptyList()
    }

    fun updatePermissions(cameraGranted: Boolean, audioGranted: Boolean) {
        val audioChanged = audioGranted != _hasAudioPermission.value
        _hasCameraPermission.value = cameraGranted
        _hasAudioPermission.value = audioGranted
        if (audioChanged || audioGranted) {
            audioEngine.startCapture(
                hasMicPermission = audioGranted,
                mediaProjection = MediaProjectionHolder.getProjection(),
                sampleRateHz = preferences.value.audioSampleRateHz
            )
        }
    }

    fun onScreenProjectionGranted(resultCode: Int, data: Intent) {
        StreamBroadcastService.startScreenProjection(app, resultCode, data)
        _telemetry.update { it.copy(statusMessage = "Screen / Game Capture active") }
        appendLog("INFO", "CAPTURE", "MediaProjection display capture started")
        showMessage("Screen Capture active!")
    }

    fun stopScreenProjection() {
        MediaProjectionHolder.release()
        _telemetry.update { it.copy(isScreenCaptureActive = false, statusMessage = "Screen Capture stopped") }
        appendLog("INFO", "CAPTURE", "MediaProjection display capture stopped")
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showMessage(msg: String) {
        _toastMessage.value = msg
    }

    fun setEditingOrientation(orientation: OutputOrientation) {
        _editingOrientation.value = orientation
        pipeline.currentOrientation = orientation
    }

    fun selectTransition(type: TransitionType) {
        _selectedTransition.value = type
        viewModelScope.launch {
            repo.updatePreferences { it.copy(transitionType = type, transitionDurationMs = type.durationMs) }
        }
    }

    fun selectCollection(collectionId: Long) {
        activeCollectionId.value = collectionId
        viewModelScope.launch { repo.switchCollection(collectionId) }
    }

    fun addCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repo.addCollection(name.trim())
            activeCollectionId.value = id
        }
    }

    fun selectScene(sceneId: Long) {
        if (preferences.value.studioModeEnabled) {
            _previewSceneId.value = sceneId
        } else {
            _programSceneId.value = sceneId
            _previewSceneId.value = sceneId
        }
        _selectedSourceId.value = null
    }

    fun switchSceneByIndex(index: Int) {
        val list = scenes.value
        if (index in list.indices) {
            selectScene(list[index].id)
            showMessage("Switched to ${list[index].name}")
        }
    }

    fun transitionPreviewToProgram() {
        val targetId = previewScene.value?.id ?: return
        _programSceneId.value = targetId
        _selectedSourceId.value = null
        showMessage("Transitioned (${_selectedTransition.value.label}) to ${previewScene.value?.name}")
    }

    fun addScene(name: String) {
        val colId = collections.value.firstOrNull { it.isActive }?.id ?: collections.value.firstOrNull()?.id ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = repo.addScene(colId, name.trim(), scenes.value.size)
            selectScene(newId)
        }
    }

    fun renameScene(scene: StudioScene, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repo.renameScene(scene, newName.trim()) }
    }

    fun updateSceneBackdrop(scene: StudioScene, colorHex: Long) {
        viewModelScope.launch { repo.updateSceneBackdrop(scene, colorHex) }
    }

    fun duplicateScene(scene: StudioScene) {
        viewModelScope.launch {
            val newId = repo.duplicateScene(scene, scenes.value.size)
            selectScene(newId)
            showMessage("Duplicated '${scene.name}'")
        }
    }

    fun deleteScene(sceneId: Long) {
        if (scenes.value.size <= 1) {
            showMessage("At least one scene is required")
            return
        }
        viewModelScope.launch { repo.deleteScene(sceneId) }
    }

    fun selectSource(sourceId: Long?) {
        _selectedSourceId.value = sourceId
    }

    fun addSource(
        type: SourceType,
        name: String,
        textContent: String = "",
        imageUri: String? = null,
        accentColorHex: Long = 0xFF31C3A2
    ) {
        val targetScene = if (preferences.value.studioModeEnabled) previewScene.value else programScene.value
        val sceneId = targetScene?.id ?: return
        val existing = if (preferences.value.studioModeEnabled) previewSources.value else programSources.value
        val defaultRectH = when (type) {
            SourceType.SCREEN_CAPTURE -> NormalizedRect(0f, 0f, 1f, 1f)
            SourceType.CAMERA -> NormalizedRect(0.05f, 0.55f, 0.28f, 0.38f)
            SourceType.WEBCAM_FRAME -> NormalizedRect(0.045f, 0.54f, 0.29f, 0.40f)
            SourceType.VIDEO_OVERLAY -> NormalizedRect(0.15f, 0.15f, 0.45f, 0.45f)
            SourceType.STREAM_TIMER -> NormalizedRect(0.03f, 0.04f, 0.34f, 0.08f)
            SourceType.ALERTBOX -> NormalizedRect(0.25f, 0.05f, 0.50f, 0.14f)
            SourceType.DONATION_GOAL -> NormalizedRect(0.32f, 0.86f, 0.36f, 0.09f)
            SourceType.CHATBOX -> NormalizedRect(0.72f, 0.40f, 0.25f, 0.52f)
            else -> NormalizedRect(0.20f, 0.25f, 0.35f, 0.22f)
        }
        val defaultRectV = when (type) {
            SourceType.SCREEN_CAPTURE -> NormalizedRect(0f, 0.48f, 1f, 0.52f)
            SourceType.CAMERA -> NormalizedRect(0.06f, 0.05f, 0.88f, 0.30f)
            SourceType.WEBCAM_FRAME -> NormalizedRect(0.05f, 0.045f, 0.90f, 0.31f)
            SourceType.VIDEO_OVERLAY -> NormalizedRect(0.10f, 0.25f, 0.80f, 0.30f)
            SourceType.STREAM_TIMER -> NormalizedRect(0.06f, 0.015f, 0.88f, 0.06f)
            SourceType.ALERTBOX -> NormalizedRect(0.08f, 0.36f, 0.84f, 0.11f)
            SourceType.DONATION_GOAL -> NormalizedRect(0.08f, 0.38f, 0.84f, 0.08f)
            SourceType.CHATBOX -> NormalizedRect(0.06f, 0.72f, 0.88f, 0.24f)
            else -> NormalizedRect(0.15f, 0.35f, 0.70f, 0.18f)
        }
        viewModelScope.launch {
            val id = repo.addSource(
                StudioSource(
                    sceneId = sceneId,
                    name = name.ifBlank { type.displayName },
                    type = type,
                    sortOrder = 0,
                    hRect = defaultRectH,
                    vRect = defaultRectV,
                    textContent = textContent,
                    imageUri = imageUri,
                    accentColorHex = accentColorHex
                )
            )
            existing.forEachIndexed { index, src ->
                repo.updateSource(src.copy(sortOrder = index + 1))
            }
            _selectedSourceId.value = id
        }
    }

    fun duplicateSource(source: StudioSource) {
        viewModelScope.launch {
            val newId = repo.duplicateSource(source)
            _selectedSourceId.value = newId
            showMessage("Duplicated '${source.name}'")
        }
    }

    fun updateSource(source: StudioSource) {
        viewModelScope.launch {
            repo.updateSource(source)
            dragRectOverrides.update { map ->
                map - (source.id to OutputOrientation.HORIZONTAL) - (source.id to OutputOrientation.VERTICAL)
            }
        }
    }

    fun toggleSourceVisibility(source: StudioSource, orientation: OutputOrientation) {
        val updated = if (orientation == OutputOrientation.HORIZONTAL) {
            source.copy(isVisibleHorizontal = !source.isVisibleHorizontal)
        } else {
            source.copy(isVisibleVertical = !source.isVisibleVertical)
        }
        viewModelScope.launch { repo.updateSource(updated) }
    }

    fun toggleSourceLock(source: StudioSource) {
        viewModelScope.launch { repo.updateSource(source.copy(isLocked = !source.isLocked)) }
    }

    fun toggleCameraFacing(source: StudioSource) {
        if (source.type != SourceType.CAMERA) return
        val nextFront = !source.cameraFacingFront
        updateSource(source.copy(cameraFacingFront = nextFront))
        showMessage(if (nextFront) "Switched to Front Camera" else "Switched to Rear Camera")
    }

    fun moveSourceLayer(sourceId: Long, moveUp: Boolean) {
        val current = if (preferences.value.studioModeEnabled) previewSources.value else programSources.value
        viewModelScope.launch { repo.moveSourceOrder(current, sourceId, moveUp) }
    }

    fun deleteSource(sourceId: Long) {
        viewModelScope.launch {
            repo.deleteSource(sourceId)
            dragRectOverrides.update { map ->
                map - (sourceId to OutputOrientation.HORIZONTAL) - (sourceId to OutputOrientation.VERTICAL)
            }
            if (_selectedSourceId.value == sourceId) _selectedSourceId.value = null
        }
    }

    fun moveSourceRect(sourceId: Long, orientation: OutputOrientation, dx: Float, dy: Float) {
        val list = if (preferences.value.studioModeEnabled) previewSources.value else programSources.value
        val source = list.firstOrNull { it.id == sourceId } ?: return
        if (source.isLocked) return

        val key = sourceId to orientation
        val rect = dragRectOverrides.value[key]
            ?: if (orientation == OutputOrientation.HORIZONTAL) source.hRect else source.vRect

        var newX = (rect.x + dx).coerceIn(0f, (1f - rect.width).coerceAtLeast(0f))
        var newY = (rect.y + dy).coerceIn(0f, (1f - rect.height).coerceAtLeast(0f))

        if (preferences.value.gridSnapEnabled) {
            val centerX = newX + rect.width * 0.5f
            val centerY = newY + rect.height * 0.5f
            if (abs(centerX - 0.5f) < 0.015f) newX = (0.5f - rect.width * 0.5f).coerceIn(0f, 1f)
            if (abs(centerY - 0.5f) < 0.015f) newY = (0.5f - rect.height * 0.5f).coerceIn(0f, 1f)
        }

        dragRectOverrides.update { it + (key to rect.copy(x = newX, y = newY)) }
    }

    fun resizeSourceRect(sourceId: Long, orientation: OutputOrientation, dw: Float, dh: Float) {
        val list = if (preferences.value.studioModeEnabled) previewSources.value else programSources.value
        val source = list.firstOrNull { it.id == sourceId } ?: return
        if (source.isLocked) return

        val key = sourceId to orientation
        val rect = dragRectOverrides.value[key]
            ?: if (orientation == OutputOrientation.HORIZONTAL) source.hRect else source.vRect

        val maxW = (1f - rect.x).coerceAtLeast(0.08f)
        val maxH = (1f - rect.y).coerceAtLeast(0.06f)
        val newW = (rect.width + dw).coerceIn(0.08f, maxW)
        val newH = (rect.height + dh).coerceIn(0.06f, maxH)
        dragRectOverrides.update { it + (key to rect.copy(width = newW, height = newH)) }
    }

    fun commitSourceTransform(sourceId: Long, orientation: OutputOrientation) {
        val key = sourceId to orientation
        val overrideRect = dragRectOverrides.value[key] ?: return
        val list = if (preferences.value.studioModeEnabled) dbPreviewSourcesSync() else dbProgramSourcesSync()
        val baseSource = list.firstOrNull { it.id == sourceId } ?: return
        val updated = if (orientation == OutputOrientation.HORIZONTAL) {
            baseSource.copy(hRect = overrideRect)
        } else {
            baseSource.copy(vRect = overrideRect)
        }
        viewModelScope.launch {
            repo.updateSource(updated)
            dragRectOverrides.update { it - key }
        }
    }

    private fun dbProgramSourcesSync(): List<StudioSource> = programSources.value
    private fun dbPreviewSourcesSync(): List<StudioSource> = previewSources.value

    fun applyTransformPreset(source: StudioSource, orientation: OutputOrientation, preset: TransformPreset) {
        if (source.isLocked) {
            showMessage("Unlock '${source.name}' before transforming")
            return
        }
        val cur = if (orientation == OutputOrientation.HORIZONTAL) source.hRect else source.vRect
        val pad = 0.03f
        val nextRect = when (preset) {
            TransformPreset.CENTER_BOTH -> cur.copy(x = (1f - cur.width) * 0.5f, y = (1f - cur.height) * 0.5f)
            TransformPreset.CENTER_H -> cur.copy(x = (1f - cur.width) * 0.5f)
            TransformPreset.CENTER_V -> cur.copy(y = (1f - cur.height) * 0.5f)
            TransformPreset.FIT_SCREEN -> NormalizedRect(0f, 0f, 1f, 1f)
            TransformPreset.TOP_LEFT -> cur.copy(x = pad, y = pad)
            TransformPreset.TOP_RIGHT -> cur.copy(x = (1f - cur.width - pad).coerceAtLeast(0f), y = pad)
            TransformPreset.BOTTOM_LEFT -> cur.copy(x = pad, y = (1f - cur.height - pad).coerceAtLeast(0f))
            TransformPreset.BOTTOM_RIGHT -> cur.copy(
                x = (1f - cur.width - pad).coerceAtLeast(0f),
                y = (1f - cur.height - pad).coerceAtLeast(0f)
            )
        }
        val updated = if (orientation == OutputOrientation.HORIZONTAL) {
            source.copy(hRect = nextRect)
        } else {
            source.copy(vRect = nextRect)
        }
        updateSource(updated)
    }

    fun nudgeSelectedSource(dx: Float, dy: Float, resize: Boolean = false) {
        val id = _selectedSourceId.value ?: return
        val orient = editingOrientation.value
        if (resize) {
            resizeSourceRect(id, orient, dx, dy)
        } else {
            moveSourceRect(id, orient, dx, dy)
        }
        commitSourceTransform(id, orient)
    }

    fun deleteSelectedSource() {
        val id = _selectedSourceId.value ?: return
        deleteSource(id)
        showMessage("Source removed")
    }

    fun setAudioGain(channel: AudioChannelConfig, gainDb: Float) {
        viewModelScope.launch { repo.updateAudioChannel(channel.copy(gainDb = gainDb.coerceIn(-48f, 12f))) }
    }

    fun toggleAudioMute(channel: AudioChannelConfig) {
        viewModelScope.launch { repo.updateAudioChannel(channel.copy(isMuted = !channel.isMuted)) }
    }

    fun toggleChannelMuteById(channelId: AudioChannelId) {
        val ch = audioChannels.value.firstOrNull { it.id == channelId.name } ?: return
        toggleAudioMute(ch)
        showMessage("${ch.name}: ${if (!ch.isMuted) "Muted" else "Unmuted"}")
    }

    fun updateAudioChannel(channel: AudioChannelConfig) {
        viewModelScope.launch { repo.updateAudioChannel(channel) }
    }

    fun togglePrivacyShield() {
        viewModelScope.launch {
            val next = !preferences.value.privacyShieldActive
            repo.updatePreferences { it.copy(privacyShieldActive = next) }
            appendLog("INFO", "PRIVACY", if (next) "Privacy Shield enabled" else "Privacy Shield disabled")
            showMessage(if (next) "Privacy Shield ON — Screen capture hidden" else "Privacy Shield OFF — Screen live")
        }
    }

    fun triggerTestWidgets() {
        audioEngine.triggerAlertAudioPulse()
        val samples = listOf(
            LiveAlertEvent(System.currentTimeMillis(), "PixelKnight_99 followed!", "Welcome to the squad", "FOLLOW", 0xFF31C3A2),
            LiveAlertEvent(System.currentTimeMillis(), "NovaStream gifted 5 Subs!", "Tier 1 Community Gift", "SUB", 0xFF9146FF),
            LiveAlertEvent(System.currentTimeMillis(), "ApexRacer tipped $10.00!", "Keep grinding, stream looks crisp!", "TIP", 0xFFF59E0B),
            LiveAlertEvent(System.currentTimeMillis(), "VortexGaming raided (84 viewers)!", "Let's gooo!", "RAID", 0xFFFF4557)
        )
        val picked = samples[testWidgetIndex % samples.size]
        testWidgetIndex++
        showAlert(picked)

        if (picked.kind == "TIP") {
            _donationGoal.update { goal ->
                goal.copy(currentAmount = (goal.currentAmount + 10f).coerceAtMost(goal.targetAmount * 1.5f))
            }
        }
        _chatMessages.update { list ->
            (list + StreamChatMessage(
                id = UUID.randomUUID().toString(),
                platform = ChatPlatform.TWITCH,
                username = picked.title.substringBefore(" "),
                message = "Tested ${picked.kind} widget on mobileLabs!",
                nameColorHex = picked.accentHex,
                badge = "SUB"
            )).takeLast(120)
        }
    }

    private fun showAlert(alert: LiveAlertEvent) {
        _activeAlert.value = alert
        viewModelScope.launch {
            delay(4200)
            if (_activeAlert.value?.id == alert.id) {
                _activeAlert.value = null
            }
        }
    }

    fun connectTwitchChat(channel: String) {
        val clean = channel.trim().removePrefix("#")
        viewModelScope.launch {
            repo.updatePreferences { it.copy(twitchChannel = clean) }
        }
        if (clean.isBlank()) {
            chatClient.disconnect()
            appendLog("INFO", "CHAT", "Disconnected Twitch IRC")
        } else {
            chatClient.connect(clean)
            appendLog("INFO", "CHAT", "Joined Twitch IRC #$clean")
        }
    }

    fun addLocalChatMessage(username: String, text: String, platform: ChatPlatform = ChatPlatform.TWITCH) {
        if (text.isBlank()) return
        _chatMessages.update {
            (it + StreamChatMessage(
                id = UUID.randomUUID().toString(),
                platform = platform,
                username = username,
                message = text.trim(),
                nameColorHex = platform.colorHex,
                badge = "HOST"
            )).takeLast(120)
        }
    }

    fun pinChatMessage(msg: StreamChatMessage?) {
        val next = if (_pinnedChatMessage.value?.id == msg?.id) null else msg
        _pinnedChatMessage.value = next
        pipeline.currentPinnedChat = next
        if (next != null) {
            showMessage("Pinned ${next.username}'s message to Chatbox")
        } else {
            showMessage("Unpinned chat message")
        }
    }

    fun dropStreamMarker(note: String = "Highlight Moment") {
        val elapsed = _telemetry.value.elapsedLiveSeconds
        val sceneName = programScene.value?.name ?: "Scene"
        val marker = StreamMarker(elapsedSec = elapsed, sceneName = sceneName, note = note.ifBlank { "Marker" })
        _streamMarkers.update { (it + marker).takeLast(100) }
        appendLog("INFO", "MARKER", "Dropped marker '$note' at $sceneName")
        showMessage("Stream Marker saved ($sceneName)")
    }

    fun toggleDualOutput(orientation: OutputOrientation) {
        viewModelScope.launch {
            repo.updatePreferences { prefs ->
                when (orientation) {
                    OutputOrientation.HORIZONTAL -> {
                        val next = !prefs.horizontalEnabled
                        if (!next && !prefs.verticalEnabled) prefs else prefs.copy(horizontalEnabled = next)
                    }
                    OutputOrientation.VERTICAL -> {
                        val next = !prefs.verticalEnabled
                        if (!next && !prefs.horizontalEnabled) prefs else prefs.copy(verticalEnabled = next)
                    }
                }
            }
        }
    }

    fun toggleStudioMode() {
        viewModelScope.launch {
            val next = !preferences.value.studioModeEnabled
            repo.updatePreferences { it.copy(studioModeEnabled = next) }
            showMessage(if (next) "Studio Mode ON" else "Studio Mode OFF")
        }
    }

    fun updatePreferences(transform: (StudioPreferences) -> StudioPreferences) {
        viewModelScope.launch {
            repo.updatePreferences(transform)
        }
    }

    fun applyOverlayTheme(preset: OverlayThemePreset) {
        viewModelScope.launch {
            repo.updatePreferences { it.copy(overlayThemeId = preset.id) }
            programSources.value.forEach { src ->
                if (src.type == SourceType.WEBCAM_FRAME || src.type == SourceType.ALERTBOX ||
                    src.type == SourceType.DONATION_GOAL || src.type == SourceType.TEXT_BANNER ||
                    src.type == SourceType.STREAM_TIMER
                ) {
                    repo.updateSource(
                        src.copy(
                            accentColorHex = preset.primaryHex,
                            cornerRadiusDp = preset.frameCornerDp
                        )
                    )
                }
            }
            showMessage("Applied '${preset.name}' theme")
        }
    }

    fun saveDestination(destination: StreamDestination) {
        viewModelScope.launch {
            if (destination.id == 0L) repo.saveDestination(destination)
            else repo.updateDestination(destination)
        }
    }

    fun toggleDestinationEnabled(destination: StreamDestination) {
        viewModelScope.launch {
            repo.updateDestination(destination.copy(isEnabled = !destination.isEnabled))
        }
    }

    fun deleteDestination(id: Long) {
        viewModelScope.launch { repo.deleteDestination(id) }
    }

    fun testRtmpDestination(rtmpUrl: String, streamKey: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                RtmpClient.testHandshake(rtmpUrl, streamKey)
            }
            val msg = if (res.isSuccess) {
                "RTMP Handshake OK (${res.getOrNull()} ms)"
            } else {
                "Test failed: ${res.exceptionOrNull()?.message ?: "Unable to connect"}"
            }
            appendLog(if (res.isSuccess) "INFO" else "WARN", "PING", msg)
            onResult(msg)
        }
    }

    fun exportCollectionJson(onExported: (String) -> Unit) {
        viewModelScope.launch {
            val activeCol = collections.value.firstOrNull { it.isActive } ?: collections.value.firstOrNull() ?: return@launch
            val json = withContext(Dispatchers.IO) {
                repo.exportSceneCollectionJson(activeCol.id, activeCol.name, scenes.value)
            }
            appendLog("INFO", "BACKUP", "Exported Scene Collection '${activeCol.name}' JSON")
            onExported(json)
        }
    }

    fun importCollectionJson(rawJson: String) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                repo.importSceneCollectionJson(rawJson)
            }
            if (res.isSuccess) {
                activeCollectionId.value = res.getOrNull()
                appendLog("INFO", "BACKUP", "Imported Scene Collection JSON")
                showMessage("Scene Collection imported!")
            } else {
                showMessage("Invalid Scene Collection JSON")
            }
        }
    }

    fun toggleLiveStream() {
        val currentState = _telemetry.value.connectionState
        if (currentState == StreamConnectionState.LIVE ||
            currentState == StreamConnectionState.CONNECTING ||
            currentState == StreamConnectionState.RECONNECTING
        ) {
            pipeline.stopStreaming()
            pipeline.currentLiveElapsedSec = 0L
            appendLog("INFO", "STREAM", "Broadcast stopped by host")
            _telemetry.update {
                it.copy(
                    connectionState = StreamConnectionState.OFFLINE,
                    elapsedLiveSeconds = 0L,
                    currentBitrateKbps = 0,
                    healthLevel = StreamHealthLevel.IDLE,
                    destinationHealth = emptyMap(),
                    statusMessage = "Stream ended"
                )
            }
            if (!_telemetry.value.isRecording && !MediaProjectionHolder.isScreenCapturing.value) {
                StreamBroadcastService.stopService(app)
            }
        } else {
            val enabled = destinations.value.filter { it.isEnabled && it.rtmpUrl.isNotBlank() && it.streamKey.isNotBlank() }
            if (enabled.isEmpty()) {
                _telemetry.update {
                    it.copy(
                        connectionState = StreamConnectionState.ERROR,
                        statusMessage = "Configure a Stream Key in Destinations first"
                    )
                }
                showMessage("Add your RTMP Stream Key in Destinations to go live")
                return
            }
            StreamBroadcastService.startForegroundStudio(app, "Streaming Live (${enabled.size} target${if (enabled.size > 1) "s" else ""})")
            _telemetry.update {
                it.copy(
                    connectionState = StreamConnectionState.CONNECTING,
                    elapsedLiveSeconds = 0L,
                    targetBitrateKbps = preferences.value.videoBitrateKbps,
                    statusMessage = "Connecting to ${enabled.joinToString { d -> d.platform.displayName }}..."
                )
            }
            appendLog("INFO", "STREAM", "Starting stream to ${enabled.joinToString { d -> d.name }}")
            pipeline.startStreaming(enabled, preferences.value) { errorMsg ->
                _telemetry.update {
                    it.copy(
                        connectionState = if (pipeline.collectStats().activeDestinations > 0) StreamConnectionState.LIVE else StreamConnectionState.ERROR,
                        statusMessage = errorMsg
                    )
                }
                showMessage(errorMsg)
            }
        }
    }

    fun toggleRecording() {
        if (_telemetry.value.isRecording) {
            pipeline.stopRecording { savedClip ->
                viewModelScope.launch {
                    repo.addRecordingClip(savedClip)
                    appendLog("INFO", "RECORD", "Saved MP4: ${savedClip.title}.mp4")
                    showMessage("Saved recording: ${savedClip.title}.mp4")
                }
            }
            _telemetry.update {
                it.copy(isRecording = false, elapsedRecordSeconds = 0L, statusMessage = "Recording saved to Movies/mobileLabs")
            }
            if (_telemetry.value.connectionState == StreamConnectionState.OFFLINE && !MediaProjectionHolder.isScreenCapturing.value) {
                StreamBroadcastService.stopService(app)
            }
        } else {
            StreamBroadcastService.startForegroundStudio(app, "Recording Local MP4")
            val started = pipeline.startRecording(preferences.value, editingOrientation.value)
            if (started) {
                appendLog("INFO", "RECORD", "Started local MP4 recording")
                _telemetry.update {
                    it.copy(isRecording = true, elapsedRecordSeconds = 0L, statusMessage = "Recording local MP4...")
                }
            } else {
                appendLog("ERR", "RECORD", "Failed to initialize MediaMuxer/MediaCodec")
                showMessage("Failed to initialize hardware MP4 encoder")
            }
        }
    }

    fun saveReplayBufferClip() {
        showMessage("Saving instant replay clip...")
        pipeline.saveReplayClip(preferences.value) { clip ->
            viewModelScope.launch {
                if (clip != null) {
                    repo.addRecordingClip(clip)
                    appendLog("INFO", "REPLAY", "Saved replay buffer: ${clip.title}.mp4")
                    showMessage("Replay saved: ${clip.title}.mp4")
                } else {
                    showMessage("Replay buffer warming up — try again in 2 seconds")
                }
            }
        }
    }

    fun deleteClip(id: Long) {
        viewModelScope.launch { repo.deleteClip(id) }
    }

    private fun updateTelemetry1Hz() {
        val stats = pipeline.collectStats()
        val nowWall = System.currentTimeMillis()
        val nowCpu = Process.getElapsedCpuTime()
        val cpuPct = if (lastWallTimeMs > 0L) {
            val wallDelta = (nowWall - lastWallTimeMs).coerceAtLeast(1L)
            val cpuDelta = (nowCpu - lastCpuTimeMs).coerceAtLeast(0L)
            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            ((cpuDelta.toFloat() / (wallDelta * cores)) * 100f).coerceIn(0.4f, 99.9f)
        } else {
            1.7f
        }
        lastWallTimeMs = nowWall
        lastCpuTimeMs = nowCpu

        // Hardware battery, thermal & storage checks
        val batteryPct = runCatching {
            val bm = app.getSystemService(BatteryManager::class.java)
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 } ?: 100
        }.getOrDefault(100)

        val freeGb = runCatching {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                ?: app.filesDir
            val stat = StatFs(dir.path)
            (stat.availableBytes / (1024f * 1024f * 1024f)).coerceAtLeast(0.1f)
        }.getOrDefault(16f)

        val thermalThrottled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val pm = app.getSystemService(PowerManager::class.java)
                (pm?.currentThermalStatus ?: 0) >= PowerManager.THERMAL_STATUS_SEVERE
            }.getOrDefault(false)
        } else false

        _telemetry.update { prev ->
            val nextConn = when {
                stats.activeDestinations > 0 -> StreamConnectionState.LIVE
                stats.reconnectingDestinations > 0 -> StreamConnectionState.RECONNECTING
                prev.connectionState == StreamConnectionState.CONNECTING -> StreamConnectionState.CONNECTING
                prev.connectionState == StreamConnectionState.ERROR -> StreamConnectionState.ERROR
                else -> StreamConnectionState.OFFLINE
            }
            val nextElapsed = if (nextConn == StreamConnectionState.LIVE || nextConn == StreamConnectionState.RECONNECTING) {
                prev.elapsedLiveSeconds + 1
            } else 0L
            pipeline.currentLiveElapsedSec = nextElapsed

            val dropRatio = if (stats.totalFrames > 30) stats.droppedFrames.toFloat() / stats.totalFrames.toFloat() else 0f
            val health = when {
                nextConn == StreamConnectionState.OFFLINE -> StreamHealthLevel.IDLE
                nextConn == StreamConnectionState.RECONNECTING || nextConn == StreamConnectionState.ERROR -> StreamHealthLevel.POOR
                thermalThrottled || dropRatio > 0.05f -> StreamHealthLevel.FAIR
                dropRatio > 0.01f || stats.currentBitrateKbps < stats.targetBitrateKbps * 0.65f -> StreamHealthLevel.GOOD
                else -> StreamHealthLevel.EXCELLENT
            }

            val nextStatus = when (nextConn) {
                StreamConnectionState.LIVE -> "LIVE to ${stats.activeDestinations} target${if (stats.activeDestinations > 1) "s" else ""}"
                StreamConnectionState.RECONNECTING -> "Reconnecting RTMP (${stats.reconnectingDestinations})..."
                else -> prev.statusMessage
            }

            prev.copy(
                connectionState = nextConn,
                elapsedLiveSeconds = nextElapsed,
                elapsedRecordSeconds = if (prev.isRecording) prev.elapsedRecordSeconds + 1 else 0L,
                currentBitrateKbps = stats.currentBitrateKbps,
                targetBitrateKbps = stats.targetBitrateKbps,
                currentFps = if (stats.currentFps > 0f) stats.currentFps else preferences.value.targetFps.toFloat(),
                cpuUsagePercent = cpuPct,
                droppedFrames = stats.droppedFrames,
                totalFrames = stats.totalFrames,
                batteryPercent = batteryPct,
                freeStorageGb = freeGb,
                isThermalThrottling = thermalThrottled,
                healthLevel = health,
                activeDestinationsCount = stats.activeDestinations,
                destinationHealth = stats.destinationHealth,
                statusMessage = nextStatus
            )
        }
    }

    override fun onCleared() {
        audioEngine.stopCapture()
        chatClient.disconnect()
        pipeline.stopAll()
        super.onCleared()
    }
}
