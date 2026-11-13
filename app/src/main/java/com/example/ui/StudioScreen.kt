package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AudioChannelConfig
import com.example.data.model.AudioChannelId
import com.example.data.model.BroadcastTelemetry
import com.example.data.model.OutputOrientation
import com.example.data.model.StreamConnectionState
import com.example.data.model.StudioPreferences
import com.example.data.model.StudioScene
import com.example.data.model.StudioSource
import com.example.streaming.MediaProjectionHolder
import com.example.ui.components.AddSourceDialog
import com.example.ui.components.AudioChannelSettingsDialog
import com.example.ui.components.AudioMixerDock
import com.example.ui.components.BottomBroadcastControlBar
import com.example.ui.components.BroadcastLogsDialog
import com.example.ui.components.CanvasPreviewStage
import com.example.ui.components.ChatDock
import com.example.ui.components.CompactTopHeader
import com.example.ui.components.ConfirmEndStreamDialog
import com.example.ui.components.DestinationsSheetDialog
import com.example.ui.components.KeyboardShortcutsDialog
import com.example.ui.components.MobileDockTabStrip
import com.example.ui.components.MobileStudioToolsGrid
import com.example.ui.components.OverlayThemesDialog
import com.example.ui.components.RecordingsClipsDialog
import com.example.ui.components.SceneCollectionBackupDialog
import com.example.ui.components.ScenesDock
import com.example.ui.components.SourcePropertiesDialog
import com.example.ui.components.SourcesDock
import com.example.ui.components.StudioNavigationRail
import com.example.ui.components.StudioSettingsDialog
import com.example.ui.components.StudioTopOutputBar
import com.example.ui.components.TextInputDialog
import com.example.ui.theme.StudioVoid
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun StudioScreen(
    viewModel: StudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val rootFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val scenes by viewModel.scenes.collectAsStateWithLifecycle()
    val programScene by viewModel.programScene.collectAsStateWithLifecycle()
    val previewScene by viewModel.previewScene.collectAsStateWithLifecycle()
    val programSources by viewModel.programSources.collectAsStateWithLifecycle()
    val previewSources by viewModel.previewSources.collectAsStateWithLifecycle()
    val selectedSourceId by viewModel.selectedSourceId.collectAsStateWithLifecycle()
    val editingOrientation by viewModel.editingOrientation.collectAsStateWithLifecycle()
    val selectedTransition by viewModel.selectedTransition.collectAsStateWithLifecycle()
    val audioChannels by viewModel.audioChannels.collectAsStateWithLifecycle()
    val meterReadings by viewModel.meterReadings.collectAsStateWithLifecycle()
    val destinations by viewModel.destinations.collectAsStateWithLifecycle()
    val clips by viewModel.clips.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val pinnedChatMessage by viewModel.pinnedChatMessage.collectAsStateWithLifecycle()
    val chatStatus by viewModel.chatStatus.collectAsStateWithLifecycle()
    val activeAlert by viewModel.activeAlert.collectAsStateWithLifecycle()
    val donationGoal by viewModel.donationGoal.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsStateWithLifecycle()
    val hasAudioPermission by viewModel.hasAudioPermission.collectAsStateWithLifecycle()
    val previewTick by viewModel.previewTick.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val streamMarkers by viewModel.streamMarkers.collectAsStateWithLifecycle()
    val broadcastLogs by viewModel.broadcastLogs.collectAsStateWithLifecycle()

    var showAddSceneDialog by remember { mutableStateOf(false) }
    var renamingScene by remember { mutableStateOf<StudioScene?>(null) }
    var showAddCollectionDialog by remember { mutableStateOf(false) }
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<StudioSource?>(null) }
    var editingAudioChannel by remember { mutableStateOf<AudioChannelConfig?>(null) }
    var showDestinationsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showThemesDialog by remember { mutableStateOf(false) }
    var showClipsDialog by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var showKeyboardShortcutsDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var exportedBackupJson by remember { mutableStateOf("") }
    var showConfirmEndStream by remember { mutableStateOf(false) }
    var isFullscreenPreview by remember { mutableStateOf(false) }
    var showChatPaneDesktop by remember { mutableStateOf(true) }
    var selectedCompactTab by remember { mutableIntStateOf(0) } // 0=Mixer, 1=Scenes, 2=Sources, 3=Chat, 4=Tools

    BackHandler(enabled = isFullscreenPreview) {
        isFullscreenPreview = false
    }

    // Keep Tablet / Phone screen awake during live broadcasts
    DisposableEffect(preferences.keepScreenAwake, telemetry.connectionState, telemetry.isRecording) {
        val window = (context as? Activity)?.window
        val shouldKeepAwake = preferences.keepScreenAwake &&
            (telemetry.connectionState == StreamConnectionState.LIVE || telemetry.isRecording)
        if (shouldKeepAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val cam = results[Manifest.permission.CAMERA]
            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        val mic = results[Manifest.permission.RECORD_AUDIO]
            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        viewModel.updatePermissions(cam, mic)
    }

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.onScreenProjectionGranted(result.resultCode, result.data!!)
        } else {
            viewModel.showMessage("Screen Capture permission cancelled")
        }
    }

    val requestStudioPermissions = {
        val perms = buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionsLauncher.launch(perms.toTypedArray())
    }

    val launchScreenCapture = {
        if (telemetry.isScreenCaptureActive) {
            viewModel.stopScreenProjection()
        } else {
            val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            screenCaptureLauncher.launch(mpm.createScreenCaptureIntent())
        }
    }

    val handleToggleLive = {
        val isActiveLive = telemetry.connectionState == StreamConnectionState.LIVE ||
            telemetry.connectionState == StreamConnectionState.CONNECTING ||
            telemetry.connectionState == StreamConnectionState.RECONNECTING
        if (isActiveLive && preferences.confirmStopStream) {
            showConfirmEndStream = true
        } else {
            viewModel.toggleLiveStream()
        }
    }

    LaunchedEffect(Unit) {
        val cam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissions(cam, mic)
        if (!cam || !mic) {
            requestStudioPermissions()
        }
        runCatching { rootFocusRequester.requestFocus() }
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearToast()
    }

    val activeSourcesForDock = if (preferences.studioModeEnabled) previewSources else programSources
    val activeSceneForDock = if (preferences.studioModeEnabled) previewScene else programScene
    val screenBitmap = remember(previewTick) { viewModel.getLatestScreenBitmap() }
    val cameraBitmap = remember(previewTick) { MediaProjectionHolder.getLatestCameraBitmap() }

    // Hardware Keyboard Shortcuts Handler (OBS Tablet / Phone Keyboard Compatibility)
    val keyboardModifier = Modifier
        .focusRequester(rootFocusRequester)
        .focusable()
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val ctrl = event.isCtrlPressed
            val shift = event.isShiftPressed
            when {
                // F1 - F7 direct scene switch
                event.key == Key.F1 || (ctrl && event.key == Key.One) -> { viewModel.switchSceneByIndex(0); true }
                event.key == Key.F2 || (ctrl && event.key == Key.Two) -> { viewModel.switchSceneByIndex(1); true }
                event.key == Key.F3 || (ctrl && event.key == Key.Three) -> { viewModel.switchSceneByIndex(2); true }
                event.key == Key.F4 || (ctrl && event.key == Key.Four) -> { viewModel.switchSceneByIndex(3); true }
                event.key == Key.F5 || (ctrl && event.key == Key.Five) -> { viewModel.switchSceneByIndex(4); true }
                event.key == Key.F6 || (ctrl && event.key == Key.Six) -> { viewModel.switchSceneByIndex(5); true }
                event.key == Key.F7 || (ctrl && event.key == Key.Seven) -> { viewModel.switchSceneByIndex(6); true }

                // Stream Marker & Broadcast Controls
                event.key == Key.F8 -> { viewModel.dropStreamMarker(); true }
                event.key == Key.F9 || (ctrl && event.key == Key.L) -> { handleToggleLive(); true }
                event.key == Key.F10 || (ctrl && event.key == Key.R) -> { viewModel.toggleRecording(); true }
                event.key == Key.F11 || (ctrl && event.key == Key.B) -> { viewModel.saveReplayBufferClip(); true }
                event.key == Key.F12 || (ctrl && event.key == Key.Slash) -> { showKeyboardShortcutsDialog = !showKeyboardShortcutsDialog; true }

                // Audio & Studio Hotkeys
                ctrl && event.key == Key.M -> { viewModel.toggleChannelMuteById(AudioChannelId.MIC_AUX); true }
                ctrl && event.key == Key.D -> { viewModel.toggleChannelMuteById(AudioChannelId.DEVICE_AUDIO); true }
                ctrl && event.key == Key.P -> { viewModel.togglePrivacyShield(); true }
                ctrl && event.key == Key.T -> { viewModel.triggerTestWidgets(); true }
                ctrl && shift && event.key == Key.S -> { viewModel.toggleStudioMode(); true }
                ctrl && shift && event.key == Key.F -> { isFullscreenPreview = !isFullscreenPreview; true }

                // Studio Mode Transition
                event.key == Key.Spacebar && preferences.studioModeEnabled && selectedSourceId == null -> {
                    viewModel.transitionPreviewToProgram(); true
                }

                // Selected Source Precision Nudging, Resizing (Ctrl+Arrows) & Delete
                selectedSourceId != null && event.key == Key.DirectionLeft -> {
                    viewModel.nudgeSelectedSource(if (shift) -0.05f else -0.01f, 0f, resize = ctrl); true
                }
                selectedSourceId != null && event.key == Key.DirectionRight -> {
                    viewModel.nudgeSelectedSource(if (shift) 0.05f else 0.01f, 0f, resize = ctrl); true
                }
                selectedSourceId != null && event.key == Key.DirectionUp -> {
                    viewModel.nudgeSelectedSource(0f, if (shift) -0.05f else -0.01f, resize = ctrl); true
                }
                selectedSourceId != null && event.key == Key.DirectionDown -> {
                    viewModel.nudgeSelectedSource(0f, if (shift) 0.05f else 0.01f, resize = ctrl); true
                }
                selectedSourceId != null && event.key == Key.Delete -> {
                    viewModel.deleteSelectedSource(); true
                }
                event.key == Key.Escape -> {
                    if (isFullscreenPreview) {
                        isFullscreenPreview = false
                    } else {
                        viewModel.selectSource(null)
                        focusManager.clearFocus()
                    }
                    true
                }
                else -> false
            }
        }

    Scaffold(
        containerColor = StudioVoid,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioVoid)
                .statusBarsPadding()
                .displayCutoutPadding()
                .then(keyboardModifier)
        ) {
            val isTabletDesktop = maxWidth >= 820.dp && maxHeight >= 500.dp
            val isCompactLandscape = maxWidth >= 600.dp && !isTabletDesktop

            if (isFullscreenPreview) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    CanvasPreviewStage(
                        horizontalEnabled = preferences.horizontalEnabled,
                        verticalEnabled = preferences.verticalEnabled,
                        studioModeEnabled = preferences.studioModeEnabled,
                        privacyShieldActive = preferences.privacyShieldActive,
                        isFullscreenPreview = true,
                        editingOrientation = editingOrientation,
                        programScene = programScene,
                        previewScene = previewScene,
                        programSources = programSources,
                        previewSources = previewSources,
                        selectedSourceId = selectedSourceId,
                        activeTransition = selectedTransition,
                        activeAlert = activeAlert,
                        donationGoal = donationGoal,
                        recentChat = chatMessages,
                        pinnedChatMessage = pinnedChatMessage,
                        liveElapsedSeconds = telemetry.elapsedLiveSeconds,
                        currentFps = telemetry.currentFps,
                        hasCameraPermission = hasCameraPermission,
                        isScreenCaptureActive = telemetry.isScreenCaptureActive,
                        screenBitmap = screenBitmap,
                        cameraBitmap = cameraBitmap,
                        getVideoOverlayBitmap = { viewModel.getVideoOverlayBitmap(it) },
                        onSelectOrientation = { viewModel.setEditingOrientation(it) },
                        onSelectSource = { viewModel.selectSource(it) },
                        onMoveSource = { srcId, orient, dx, dy -> viewModel.moveSourceRect(srcId, orient, dx, dy) },
                        onResizeSource = { srcId, orient, dw, dh -> viewModel.resizeSourceRect(srcId, orient, dw, dh) },
                        onCommitTransform = { srcId, orient -> viewModel.commitSourceTransform(srcId, orient) },
                        onApplyTransformPreset = { src, orient, preset -> viewModel.applyTransformPreset(src, orient, preset) },
                        onToggleSourceLock = { viewModel.toggleSourceLock(it) },
                        onToggleCameraFacing = { viewModel.toggleCameraFacing(it) },
                        onEditSourceProperties = { editingSource = it },
                        onDeleteSource = { viewModel.deleteSource(it) },
                        onTransitionToProgram = { viewModel.transitionPreviewToProgram() },
                        onToggleFullscreenPreview = { isFullscreenPreview = false },
                        onRequestCameraPermission = requestStudioPermissions,
                        onRequestScreenCapture = launchScreenCapture,
                        modifier = Modifier.weight(1f)
                    )
                    BottomBroadcastControlBar(
                        telemetry = telemetry,
                        isCompact = !isTabletDesktop,
                        onTestWidgets = { viewModel.triggerTestWidgets() },
                        onToggleRecord = { viewModel.toggleRecording() },
                        onSaveReplay = { viewModel.saveReplayBufferClip() },
                        onToggleLive = handleToggleLive
                    )
                }
            } else when {
                isTabletDesktop -> {
                    // 1-to-1 Desktop OBS / Streamlabs Layout (Tablets & Large Landscape)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        StudioNavigationRail(
                            studioModeEnabled = preferences.studioModeEnabled,
                            isScreenCaptureActive = telemetry.isScreenCaptureActive,
                            privacyShieldActive = preferences.privacyShieldActive,
                            chatVisible = showChatPaneDesktop,
                            onToggleStudioMode = { viewModel.toggleStudioMode() },
                            onTogglePrivacyShield = { viewModel.togglePrivacyShield() },
                            onOpenThemes = { showThemesDialog = true },
                            onOpenDestinations = { showDestinationsDialog = true },
                            onOpenClips = { showClipsDialog = true },
                            onToggleScreenCapture = launchScreenCapture,
                            onToggleChatPane = { showChatPaneDesktop = !showChatPaneDesktop },
                            onOpenLogs = { showLogsDialog = true },
                            onOpenKeyboardShortcuts = { showKeyboardShortcutsDialog = true },
                            onOpenSettings = { showSettingsDialog = true }
                        )

                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    StudioTopOutputBar(
                                        preferences = preferences,
                                        telemetry = telemetry,
                                        onToggleOrientation = { viewModel.toggleDualOutput(it) },
                                        onOpenDualOutputManager = { showDestinationsDialog = true },
                                        onToggleStudioMode = { viewModel.toggleStudioMode() },
                                        onTogglePrivacyShield = { viewModel.togglePrivacyShield() },
                                        onOpenLogs = { showLogsDialog = true }
                                    )

                                    CanvasPreviewStage(
                                        horizontalEnabled = preferences.horizontalEnabled,
                                        verticalEnabled = preferences.verticalEnabled,
                                        studioModeEnabled = preferences.studioModeEnabled,
                                        privacyShieldActive = preferences.privacyShieldActive,
                                        isFullscreenPreview = false,
                                        editingOrientation = editingOrientation,
                                        programScene = programScene,
                                        previewScene = previewScene,
                                        programSources = programSources,
                                        previewSources = previewSources,
                                        selectedSourceId = selectedSourceId,
                                        activeTransition = selectedTransition,
                                        activeAlert = activeAlert,
                                        donationGoal = donationGoal,
                                        recentChat = chatMessages,
                                        pinnedChatMessage = pinnedChatMessage,
                                        liveElapsedSeconds = telemetry.elapsedLiveSeconds,
                                        currentFps = telemetry.currentFps,
                                        hasCameraPermission = hasCameraPermission,
                                        isScreenCaptureActive = telemetry.isScreenCaptureActive,
                                        screenBitmap = screenBitmap,
                                        cameraBitmap = cameraBitmap,
                                        getVideoOverlayBitmap = { viewModel.getVideoOverlayBitmap(it) },
                                        onSelectOrientation = { viewModel.setEditingOrientation(it) },
                                        onSelectSource = { viewModel.selectSource(it) },
                                        onMoveSource = { srcId, orient, dx, dy -> viewModel.moveSourceRect(srcId, orient, dx, dy) },
                                        onResizeSource = { srcId, orient, dw, dh -> viewModel.resizeSourceRect(srcId, orient, dw, dh) },
                                        onCommitTransform = { srcId, orient -> viewModel.commitSourceTransform(srcId, orient) },
                                        onApplyTransformPreset = { src, orient, preset -> viewModel.applyTransformPreset(src, orient, preset) },
                                        onToggleSourceLock = { viewModel.toggleSourceLock(it) },
                                        onToggleCameraFacing = { viewModel.toggleCameraFacing(it) },
                                        onEditSourceProperties = { editingSource = it },
                                        onDeleteSource = { viewModel.deleteSource(it) },
                                        onTransitionToProgram = { viewModel.transitionPreviewToProgram() },
                                        onToggleFullscreenPreview = { isFullscreenPreview = true },
                                        onRequestCameraPermission = requestStudioPermissions,
                                        onRequestScreenCapture = launchScreenCapture,
                                        modifier = Modifier.weight(1.12f)
                                    )

                                    Row(modifier = Modifier.weight(0.88f).fillMaxWidth()) {
                                        ScenesDock(
                                            collections = collections,
                                            scenes = scenes,
                                            activeSceneId = activeSceneForDock?.id,
                                            selectedTransition = selectedTransition,
                                            onSelectCollection = { viewModel.selectCollection(it) },
                                            onAddCollectionClick = { showAddCollectionDialog = true },
                                            onSelectScene = { viewModel.selectScene(it) },
                                            onAddSceneClick = { showAddSceneDialog = true },
                                            onRenameSceneClick = { renamingScene = it },
                                            onDuplicateScene = { viewModel.duplicateScene(it) },
                                            onDeleteScene = { viewModel.deleteScene(it) },
                                            onSelectTransition = { viewModel.selectTransition(it) },
                                            onDropMarker = { viewModel.dropStreamMarker() },
                                            modifier = Modifier.weight(0.95f)
                                        )
                                        SourcesDock(
                                            sources = activeSourcesForDock,
                                            selectedSourceId = selectedSourceId,
                                            editingOrientation = editingOrientation,
                                            onSelectSource = { viewModel.selectSource(it) },
                                            onAddSourceClick = { showAddSourceDialog = true },
                                            onToggleVisibility = { src, orient -> viewModel.toggleSourceVisibility(src, orient) },
                                            onToggleLock = { viewModel.toggleSourceLock(it) },
                                            onDuplicateSource = { viewModel.duplicateSource(it) },
                                            onMoveSourceOrder = { id, up -> viewModel.moveSourceLayer(id, up) },
                                            onEditSourceProperties = { editingSource = it },
                                            modifier = Modifier.weight(1.05f)
                                        )
                                        AudioMixerDock(
                                            channels = audioChannels,
                                            readings = meterReadings,
                                            hasAudioPermission = hasAudioPermission,
                                            audioSampleRateHz = preferences.audioSampleRateHz,
                                            onRequestAudioPermission = requestStudioPermissions,
                                            onGainChange = { ch, db -> viewModel.setAudioGain(ch, db) },
                                            onToggleMute = { viewModel.toggleAudioMute(it) },
                                            onOpenChannelSettings = { editingAudioChannel = it },
                                            modifier = Modifier.weight(1.1f)
                                        )
                                    }
                                }

                                if (showChatPaneDesktop) {
                                    ChatDock(
                                        telemetry = telemetry,
                                        streamTitle = preferences.streamTitle,
                                        twitchChannel = preferences.twitchChannel,
                                        chatStatus = chatStatus,
                                        messages = chatMessages,
                                        pinnedMessage = pinnedChatMessage,
                                        onEditTitleClick = { showEditTitleDialog = true },
                                        onConnectTwitchChannel = { viewModel.connectTwitchChat(it) },
                                        onSendLocalMessage = { viewModel.addLocalChatMessage("Host", it) },
                                        onPinMessage = { viewModel.pinChatMessage(it) },
                                        modifier = Modifier.width(276.dp).fillMaxHeight()
                                    )
                                }
                            }

                            BottomBroadcastControlBar(
                                telemetry = telemetry,
                                isCompact = false,
                                onTestWidgets = { viewModel.triggerTestWidgets() },
                                onToggleRecord = { viewModel.toggleRecording() },
                                onSaveReplay = { viewModel.saveReplayBufferClip() },
                                onToggleLive = handleToggleLive
                            )
                        }
                    }
                }

                isCompactLandscape -> {
                    // Compact Landscape Phone Layout (Left Rail + Live Stage + Right Tabbed Inspector)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        StudioNavigationRail(
                            studioModeEnabled = preferences.studioModeEnabled,
                            isScreenCaptureActive = telemetry.isScreenCaptureActive,
                            privacyShieldActive = preferences.privacyShieldActive,
                            chatVisible = selectedCompactTab == 3,
                            onToggleStudioMode = { viewModel.toggleStudioMode() },
                            onTogglePrivacyShield = { viewModel.togglePrivacyShield() },
                            onOpenThemes = { showThemesDialog = true },
                            onOpenDestinations = { showDestinationsDialog = true },
                            onOpenClips = { showClipsDialog = true },
                            onToggleScreenCapture = launchScreenCapture,
                            onToggleChatPane = { selectedCompactTab = 3 },
                            onOpenLogs = { showLogsDialog = true },
                            onOpenKeyboardShortcuts = { showKeyboardShortcutsDialog = true },
                            onOpenSettings = { showSettingsDialog = true }
                        )

                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1.15f).fillMaxHeight()) {
                                    CompactTopHeader(
                                        preferences = preferences,
                                        telemetry = telemetry,
                                        onToggleOrientation = { orient ->
                                            viewModel.setEditingOrientation(orient)
                                            if (orient == OutputOrientation.HORIZONTAL && !preferences.horizontalEnabled) {
                                                viewModel.toggleDualOutput(OutputOrientation.HORIZONTAL)
                                            } else if (orient == OutputOrientation.VERTICAL && !preferences.verticalEnabled) {
                                                viewModel.toggleDualOutput(OutputOrientation.VERTICAL)
                                            }
                                        },
                                        onToggleDual = {
                                            if (preferences.horizontalEnabled && preferences.verticalEnabled) {
                                                viewModel.toggleDualOutput(OutputOrientation.VERTICAL)
                                            } else {
                                                if (!preferences.horizontalEnabled) viewModel.toggleDualOutput(OutputOrientation.HORIZONTAL)
                                                if (!preferences.verticalEnabled) viewModel.toggleDualOutput(OutputOrientation.VERTICAL)
                                            }
                                        },
                                        onOpenLogs = { showLogsDialog = true },
                                        onOpenDestinations = { showDestinationsDialog = true },
                                        onOpenSettings = { showSettingsDialog = true }
                                    )
                                    CanvasPreviewStage(
                                        horizontalEnabled = preferences.horizontalEnabled,
                                        verticalEnabled = preferences.verticalEnabled,
                                        studioModeEnabled = preferences.studioModeEnabled,
                                        privacyShieldActive = preferences.privacyShieldActive,
                                        isFullscreenPreview = false,
                                        editingOrientation = editingOrientation,
                                        programScene = programScene,
                                        previewScene = previewScene,
                                        programSources = programSources,
                                        previewSources = previewSources,
                                        selectedSourceId = selectedSourceId,
                                        activeTransition = selectedTransition,
                                        activeAlert = activeAlert,
                                        donationGoal = donationGoal,
                                        recentChat = chatMessages,
                                        pinnedChatMessage = pinnedChatMessage,
                                        liveElapsedSeconds = telemetry.elapsedLiveSeconds,
                                        currentFps = telemetry.currentFps,
                                        hasCameraPermission = hasCameraPermission,
                                        isScreenCaptureActive = telemetry.isScreenCaptureActive,
                                        screenBitmap = screenBitmap,
                                        cameraBitmap = cameraBitmap,
                                        getVideoOverlayBitmap = { viewModel.getVideoOverlayBitmap(it) },
                                        onSelectOrientation = { viewModel.setEditingOrientation(it) },
                                        onSelectSource = { viewModel.selectSource(it) },
                                        onMoveSource = { srcId, orient, dx, dy -> viewModel.moveSourceRect(srcId, orient, dx, dy) },
                                        onResizeSource = { srcId, orient, dw, dh -> viewModel.resizeSourceRect(srcId, orient, dw, dh) },
                                        onCommitTransform = { srcId, orient -> viewModel.commitSourceTransform(srcId, orient) },
                                        onApplyTransformPreset = { src, orient, preset -> viewModel.applyTransformPreset(src, orient, preset) },
                                        onToggleSourceLock = { viewModel.toggleSourceLock(it) },
                                        onToggleCameraFacing = { viewModel.toggleCameraFacing(it) },
                                        onEditSourceProperties = { editingSource = it },
                                        onDeleteSource = { viewModel.deleteSource(it) },
                                        onTransitionToProgram = { viewModel.transitionPreviewToProgram() },
                                        onToggleFullscreenPreview = { isFullscreenPreview = true },
                                        onRequestCameraPermission = requestStudioPermissions,
                                        onRequestScreenCapture = launchScreenCapture,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Column(modifier = Modifier.weight(0.85f).fillMaxHeight()) {
                                    MobileDockTabStrip(
                                        selectedTab = selectedCompactTab,
                                        onSelectTab = { selectedCompactTab = it }
                                    )
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        CompactDockContent(
                                            selectedTab = selectedCompactTab,
                                            viewModel = viewModel,
                                            collections = collections,
                                            scenes = scenes,
                                            activeSceneId = activeSceneForDock?.id,
                                            activeSources = activeSourcesForDock,
                                            selectedSourceId = selectedSourceId,
                                            editingOrientation = editingOrientation,
                                            selectedTransition = selectedTransition,
                                            audioChannels = audioChannels,
                                            meterReadings = meterReadings,
                                            hasAudioPermission = hasAudioPermission,
                                            preferences = preferences,
                                            telemetry = telemetry,
                                            chatStatus = chatStatus,
                                            chatMessages = chatMessages,
                                            pinnedMessage = pinnedChatMessage,
                                            destinationsCount = destinations.count { it.isEnabled },
                                            clipsCount = clips.size,
                                            onRequestAudioPermission = requestStudioPermissions,
                                            onAddCollection = { showAddCollectionDialog = true },
                                            onAddScene = { showAddSceneDialog = true },
                                            onRenameScene = { renamingScene = it },
                                            onAddSource = { showAddSourceDialog = true },
                                            onEditSource = { editingSource = it },
                                            onEditAudioChannel = { editingAudioChannel = it },
                                            onEditTitle = { showEditTitleDialog = true },
                                            onToggleScreenCapture = launchScreenCapture,
                                            onToggleFullscreenPreview = { isFullscreenPreview = true },
                                            onOpenDestinations = { showDestinationsDialog = true },
                                            onOpenThemes = { showThemesDialog = true },
                                            onOpenClips = { showClipsDialog = true },
                                            onOpenLogs = { showLogsDialog = true },
                                            onOpenBackup = {
                                                viewModel.exportCollectionJson { json ->
                                                    exportedBackupJson = json
                                                    showBackupDialog = true
                                                }
                                            },
                                            onOpenShortcuts = { showKeyboardShortcutsDialog = true },
                                            onOpenSettings = { showSettingsDialog = true }
                                        )
                                    }
                                }
                            }

                            BottomBroadcastControlBar(
                                telemetry = telemetry,
                                isCompact = false,
                                onTestWidgets = { viewModel.triggerTestWidgets() },
                                onToggleRecord = { viewModel.toggleRecording() },
                                onSaveReplay = { viewModel.saveReplayBufferClip() },
                                onToggleLive = handleToggleLive
                            )
                        }
                    }
                }

                else -> {
                    // Compact Portrait Phone Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        CompactTopHeader(
                            preferences = preferences,
                            telemetry = telemetry,
                            onToggleOrientation = { orient ->
                                viewModel.setEditingOrientation(orient)
                                if (orient == OutputOrientation.HORIZONTAL && !preferences.horizontalEnabled) {
                                    viewModel.toggleDualOutput(OutputOrientation.HORIZONTAL)
                                } else if (orient == OutputOrientation.VERTICAL && !preferences.verticalEnabled) {
                                    viewModel.toggleDualOutput(OutputOrientation.VERTICAL)
                                }
                            },
                            onToggleDual = {
                                if (preferences.horizontalEnabled && preferences.verticalEnabled) {
                                    viewModel.toggleDualOutput(OutputOrientation.VERTICAL)
                                } else {
                                    if (!preferences.horizontalEnabled) viewModel.toggleDualOutput(OutputOrientation.HORIZONTAL)
                                    if (!preferences.verticalEnabled) viewModel.toggleDualOutput(OutputOrientation.VERTICAL)
                                }
                            },
                            onOpenLogs = { showLogsDialog = true },
                            onOpenDestinations = { showDestinationsDialog = true },
                            onOpenSettings = { showSettingsDialog = true }
                        )

                        CanvasPreviewStage(
                            horizontalEnabled = preferences.horizontalEnabled,
                            verticalEnabled = preferences.verticalEnabled,
                            studioModeEnabled = preferences.studioModeEnabled,
                            privacyShieldActive = preferences.privacyShieldActive,
                            isFullscreenPreview = false,
                            editingOrientation = editingOrientation,
                            programScene = programScene,
                            previewScene = previewScene,
                            programSources = programSources,
                            previewSources = previewSources,
                            selectedSourceId = selectedSourceId,
                            activeTransition = selectedTransition,
                            activeAlert = activeAlert,
                            donationGoal = donationGoal,
                            recentChat = chatMessages,
                            pinnedChatMessage = pinnedChatMessage,
                            liveElapsedSeconds = telemetry.elapsedLiveSeconds,
                            currentFps = telemetry.currentFps,
                            hasCameraPermission = hasCameraPermission,
                            isScreenCaptureActive = telemetry.isScreenCaptureActive,
                            screenBitmap = screenBitmap,
                            cameraBitmap = cameraBitmap,
                            getVideoOverlayBitmap = { viewModel.getVideoOverlayBitmap(it) },
                            onSelectOrientation = { viewModel.setEditingOrientation(it) },
                            onSelectSource = { viewModel.selectSource(it) },
                            onMoveSource = { srcId, orient, dx, dy -> viewModel.moveSourceRect(srcId, orient, dx, dy) },
                            onResizeSource = { srcId, orient, dw, dh -> viewModel.resizeSourceRect(srcId, orient, dw, dh) },
                            onCommitTransform = { srcId, orient -> viewModel.commitSourceTransform(srcId, orient) },
                            onApplyTransformPreset = { src, orient, preset -> viewModel.applyTransformPreset(src, orient, preset) },
                            onToggleSourceLock = { viewModel.toggleSourceLock(it) },
                            onToggleCameraFacing = { viewModel.toggleCameraFacing(it) },
                            onEditSourceProperties = { editingSource = it },
                            onDeleteSource = { viewModel.deleteSource(it) },
                            onTransitionToProgram = { viewModel.transitionPreviewToProgram() },
                            onToggleFullscreenPreview = { isFullscreenPreview = true },
                            onRequestCameraPermission = requestStudioPermissions,
                            onRequestScreenCapture = launchScreenCapture,
                            modifier = Modifier.weight(0.39f)
                        )

                        MobileDockTabStrip(
                            selectedTab = selectedCompactTab,
                            onSelectTab = { selectedCompactTab = it }
                        )

                        Box(modifier = Modifier.weight(0.61f).fillMaxWidth()) {
                            CompactDockContent(
                                selectedTab = selectedCompactTab,
                                viewModel = viewModel,
                                collections = collections,
                                scenes = scenes,
                                activeSceneId = activeSceneForDock?.id,
                                activeSources = activeSourcesForDock,
                                selectedSourceId = selectedSourceId,
                                editingOrientation = editingOrientation,
                                selectedTransition = selectedTransition,
                                audioChannels = audioChannels,
                                meterReadings = meterReadings,
                                hasAudioPermission = hasAudioPermission,
                                preferences = preferences,
                                telemetry = telemetry,
                                chatStatus = chatStatus,
                                chatMessages = chatMessages,
                                pinnedMessage = pinnedChatMessage,
                                destinationsCount = destinations.count { it.isEnabled },
                                clipsCount = clips.size,
                                onRequestAudioPermission = requestStudioPermissions,
                                onAddCollection = { showAddCollectionDialog = true },
                                onAddScene = { showAddSceneDialog = true },
                                onRenameScene = { renamingScene = it },
                                onAddSource = { showAddSourceDialog = true },
                                onEditSource = { editingSource = it },
                                onEditAudioChannel = { editingAudioChannel = it },
                                onEditTitle = { showEditTitleDialog = true },
                                onToggleScreenCapture = launchScreenCapture,
                                onToggleFullscreenPreview = { isFullscreenPreview = true },
                                onOpenDestinations = { showDestinationsDialog = true },
                                onOpenThemes = { showThemesDialog = true },
                                onOpenClips = { showClipsDialog = true },
                                onOpenLogs = { showLogsDialog = true },
                                onOpenBackup = {
                                    viewModel.exportCollectionJson { json ->
                                        exportedBackupJson = json
                                        showBackupDialog = true
                                    }
                                },
                                onOpenShortcuts = { showKeyboardShortcutsDialog = true },
                                onOpenSettings = { showSettingsDialog = true }
                            )
                        }

                        BottomBroadcastControlBar(
                            telemetry = telemetry,
                            isCompact = true,
                            onTestWidgets = { viewModel.triggerTestWidgets() },
                            onToggleRecord = { viewModel.toggleRecording() },
                            onSaveReplay = { viewModel.saveReplayBufferClip() },
                            onToggleLive = handleToggleLive
                        )
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showAddSceneDialog) {
        TextInputDialog(
            title = "Create Scene",
            label = "Scene Name (e.g. Just Chatting)",
            confirmLabel = "Create",
            onDismiss = { showAddSceneDialog = false },
            onConfirm = { viewModel.addScene(it) }
        )
    }

    renamingScene?.let { targetScene ->
        TextInputDialog(
            title = "Rename Scene",
            label = "Scene Name",
            initialValue = targetScene.name,
            confirmLabel = "Rename",
            onDismiss = { renamingScene = null },
            onConfirm = { newName -> viewModel.renameScene(targetScene, newName) }
        )
    }

    if (showAddCollectionDialog) {
        TextInputDialog(
            title = "New Scene Collection",
            label = "Collection Name",
            confirmLabel = "Create",
            onDismiss = { showAddCollectionDialog = false },
            onConfirm = { viewModel.addCollection(it) }
        )
    }

    if (showEditTitleDialog) {
        TextInputDialog(
            title = "Stream Title",
            label = "Broadcast Title",
            initialValue = preferences.streamTitle,
            confirmLabel = "Update",
            onDismiss = { showEditTitleDialog = false },
            onConfirm = { title -> viewModel.updatePreferences { it.copy(streamTitle = title) } }
        )
    }

    if (showAddSourceDialog) {
        AddSourceDialog(
            onDismiss = { showAddSourceDialog = false },
            onAddSource = { type, name, text, uri ->
                viewModel.addSource(type = type, name = name, textContent = text, imageUri = uri)
            }
        )
    }

    editingSource?.let { src ->
        SourcePropertiesDialog(
            source = src,
            orientation = editingOrientation,
            onDismiss = { editingSource = null },
            onSave = { viewModel.updateSource(it) },
            onDuplicate = { viewModel.duplicateSource(it) },
            onDelete = { viewModel.deleteSource(it) }
        )
    }

    editingAudioChannel?.let { ch ->
        AudioChannelSettingsDialog(
            channel = ch,
            onDismiss = { editingAudioChannel = null },
            onSave = { viewModel.updateAudioChannel(it) }
        )
    }

    if (showDestinationsDialog) {
        DestinationsSheetDialog(
            destinations = destinations,
            healthMap = telemetry.destinationHealth,
            onDismiss = { showDestinationsDialog = false },
            onSaveDestination = { viewModel.saveDestination(it) },
            onToggleEnabled = { viewModel.toggleDestinationEnabled(it) },
            onDeleteDestination = { viewModel.deleteDestination(it) },
            onTestDestination = { url, key, onRes -> viewModel.testRtmpDestination(url, key, onRes) }
        )
    }

    if (showSettingsDialog) {
        StudioSettingsDialog(
            preferences = preferences,
            freeStorageGb = telemetry.freeStorageGb,
            isScreenCaptureActive = telemetry.isScreenCaptureActive,
            onDismiss = { showSettingsDialog = false },
            onUpdatePreferences = { viewModel.updatePreferences(it) },
            onToggleScreenCapture = launchScreenCapture,
            onOpenKeyboardShortcuts = {
                showSettingsDialog = false
                showKeyboardShortcutsDialog = true
            },
            onOpenBackupDialog = {
                showSettingsDialog = false
                viewModel.exportCollectionJson { json ->
                    exportedBackupJson = json
                    showBackupDialog = true
                }
            },
            onOpenLogsDialog = {
                showSettingsDialog = false
                showLogsDialog = true
            }
        )
    }

    if (showLogsDialog) {
        BroadcastLogsDialog(
            logs = broadcastLogs,
            markers = streamMarkers,
            onDismiss = { showLogsDialog = false },
            onClearLogs = { viewModel.clearLogs() },
            onDropMarker = { viewModel.dropStreamMarker() }
        )
    }

    if (showBackupDialog) {
        SceneCollectionBackupDialog(
            exportedJson = exportedBackupJson,
            onDismiss = { showBackupDialog = false },
            onImportJson = { viewModel.importCollectionJson(it) }
        )
    }

    if (showConfirmEndStream) {
        ConfirmEndStreamDialog(
            elapsedSec = telemetry.elapsedLiveSeconds,
            onDismiss = { showConfirmEndStream = false },
            onConfirmEnd = { viewModel.toggleLiveStream() }
        )
    }

    if (showKeyboardShortcutsDialog) {
        KeyboardShortcutsDialog(onDismiss = { showKeyboardShortcutsDialog = false })
    }

    if (showThemesDialog) {
        OverlayThemesDialog(
            presets = viewModel.builtInOverlayThemes,
            activeThemeId = preferences.overlayThemeId,
            onDismiss = { showThemesDialog = false },
            onApplyTheme = { viewModel.applyOverlayTheme(it) }
        )
    }

    if (showClipsDialog) {
        RecordingsClipsDialog(
            clips = clips,
            onDismiss = { showClipsDialog = false },
            onDeleteClip = { viewModel.deleteClip(it) }
        )
    }
}

@Composable
private fun CompactDockContent(
    selectedTab: Int,
    viewModel: StudioViewModel,
    collections: List<com.example.data.local.SceneCollectionEntity>,
    scenes: List<StudioScene>,
    activeSceneId: Long?,
    activeSources: List<StudioSource>,
    selectedSourceId: Long?,
    editingOrientation: OutputOrientation,
    selectedTransition: com.example.data.model.TransitionType,
    audioChannels: List<AudioChannelConfig>,
    meterReadings: Map<String, com.example.data.model.AudioMeterReading>,
    hasAudioPermission: Boolean,
    preferences: StudioPreferences,
    telemetry: BroadcastTelemetry,
    chatStatus: com.example.streaming.ChatConnectionStatus,
    chatMessages: List<com.example.data.model.StreamChatMessage>,
    pinnedMessage: com.example.data.model.StreamChatMessage?,
    destinationsCount: Int,
    clipsCount: Int,
    onRequestAudioPermission: () -> Unit,
    onAddCollection: () -> Unit,
    onAddScene: () -> Unit,
    onRenameScene: (StudioScene) -> Unit,
    onAddSource: () -> Unit,
    onEditSource: (StudioSource) -> Unit,
    onEditAudioChannel: (AudioChannelConfig) -> Unit,
    onEditTitle: () -> Unit,
    onToggleScreenCapture: () -> Unit,
    onToggleFullscreenPreview: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenClips: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenShortcuts: () -> Unit,
    onOpenSettings: () -> Unit
) {
    when (selectedTab) {
        0 -> AudioMixerDock(
            channels = audioChannels,
            readings = meterReadings,
            hasAudioPermission = hasAudioPermission,
            audioSampleRateHz = preferences.audioSampleRateHz,
            onRequestAudioPermission = onRequestAudioPermission,
            onGainChange = { ch, db -> viewModel.setAudioGain(ch, db) },
            onToggleMute = { viewModel.toggleAudioMute(it) },
            onOpenChannelSettings = onEditAudioChannel
        )
        1 -> ScenesDock(
            collections = collections,
            scenes = scenes,
            activeSceneId = activeSceneId,
            selectedTransition = selectedTransition,
            onSelectCollection = { viewModel.selectCollection(it) },
            onAddCollectionClick = onAddCollection,
            onSelectScene = { viewModel.selectScene(it) },
            onAddSceneClick = onAddScene,
            onRenameSceneClick = onRenameScene,
            onDuplicateScene = { viewModel.duplicateScene(it) },
            onDeleteScene = { viewModel.deleteScene(it) },
            onSelectTransition = { viewModel.selectTransition(it) },
            onDropMarker = { viewModel.dropStreamMarker() }
        )
        2 -> SourcesDock(
            sources = activeSources,
            selectedSourceId = selectedSourceId,
            editingOrientation = editingOrientation,
            onSelectSource = { viewModel.selectSource(it) },
            onAddSourceClick = onAddSource,
            onToggleVisibility = { src, orient -> viewModel.toggleSourceVisibility(src, orient) },
            onToggleLock = { viewModel.toggleSourceLock(it) },
            onDuplicateSource = { viewModel.duplicateSource(it) },
            onMoveSourceOrder = { id, up -> viewModel.moveSourceLayer(id, up) },
            onEditSourceProperties = onEditSource
        )
        3 -> ChatDock(
            telemetry = telemetry,
            streamTitle = preferences.streamTitle,
            twitchChannel = preferences.twitchChannel,
            chatStatus = chatStatus,
            messages = chatMessages,
            pinnedMessage = pinnedMessage,
            onEditTitleClick = onEditTitle,
            onConnectTwitchChannel = { viewModel.connectTwitchChat(it) },
            onSendLocalMessage = { viewModel.addLocalChatMessage("Host", it) },
            onPinMessage = { viewModel.pinChatMessage(it) }
        )
        else -> MobileStudioToolsGrid(
            studioModeEnabled = preferences.studioModeEnabled,
            isScreenCaptureActive = telemetry.isScreenCaptureActive,
            privacyShieldActive = preferences.privacyShieldActive,
            destinationCount = destinationsCount,
            clipCount = clipsCount,
            onToggleStudioMode = { viewModel.toggleStudioMode() },
            onToggleScreenCapture = onToggleScreenCapture,
            onTogglePrivacyShield = { viewModel.togglePrivacyShield() },
            onToggleFullscreenPreview = onToggleFullscreenPreview,
            onOpenDestinations = onOpenDestinations,
            onOpenThemes = onOpenThemes,
            onOpenClips = onOpenClips,
            onOpenLogs = onOpenLogs,
            onOpenBackup = onOpenBackup,
            onOpenShortcuts = onOpenShortcuts,
            onOpenSettings = onOpenSettings
        )
    }
}
