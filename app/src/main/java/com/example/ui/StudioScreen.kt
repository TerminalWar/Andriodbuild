package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.automirrored.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AudioChannelConfig
import com.example.data.model.AudioChannelId
import com.example.data.model.BroadcastTelemetry
import com.example.data.model.OutputOrientation
import com.example.data.model.StreamConnectionState
import com.example.data.model.StudioPreferences
import com.example.data.model.StudioSource
import com.example.ui.components.AddSourceDialog
import com.example.ui.components.AudioChannelSettingsDialog
import com.example.ui.components.AudioMixerDock
import com.example.ui.components.CanvasPreviewStage
import com.example.ui.components.ChatDock
import com.example.ui.components.DestinationsSheetDialog
import com.example.ui.components.KeyboardShortcutsDialog
import com.example.ui.components.OverlayThemesDialog
import com.example.ui.components.RecordingsClipsDialog
import com.example.ui.components.ScenesDock
import com.example.ui.components.SourcePropertiesDialog
import com.example.ui.components.SourcesDock
import com.example.ui.components.StudioSettingsDialog
import com.example.ui.components.TextInputDialog
import com.example.ui.components.formatDuration
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.LiveCrimsonBright
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MintAccentBright
import com.example.ui.theme.RecordAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDock
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioHeader
import com.example.ui.theme.StudioRail
import com.example.ui.theme.StudioVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StudioViewModel
import java.util.Locale

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
    val chatStatus by viewModel.chatStatus.collectAsStateWithLifecycle()
    val activeAlert by viewModel.activeAlert.collectAsStateWithLifecycle()
    val donationGoal by viewModel.donationGoal.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsStateWithLifecycle()
    val hasAudioPermission by viewModel.hasAudioPermission.collectAsStateWithLifecycle()
    val previewTick by viewModel.previewTick.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var showAddSceneDialog by remember { mutableStateOf(false) }
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
    var showChatPaneDesktop by remember { mutableStateOf(true) }
    var selectedCompactTab by remember { mutableIntStateOf(0) } // 0=Mixer, 1=Scenes, 2=Sources, 3=Chat, 4=Tools

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

                // Broadcast Controls
                event.key == Key.F9 || (ctrl && event.key == Key.L) -> { viewModel.toggleLiveStream(); true }
                event.key == Key.F10 || (ctrl && event.key == Key.R) -> { viewModel.toggleRecording(); true }
                event.key == Key.F11 || (ctrl && event.key == Key.B) -> { viewModel.saveReplayBufferClip(); true }
                event.key == Key.F12 || (ctrl && event.key == Key.Slash) -> { showKeyboardShortcutsDialog = !showKeyboardShortcutsDialog; true }

                // Audio & Studio Hotkeys
                ctrl && event.key == Key.M -> { viewModel.toggleChannelMuteById(AudioChannelId.MIC_AUX); true }
                ctrl && event.key == Key.D -> { viewModel.toggleChannelMuteById(AudioChannelId.DEVICE_AUDIO); true }
                ctrl && event.key == Key.P -> { viewModel.togglePrivacyShield(); true }
                ctrl && event.key == Key.T -> { viewModel.triggerTestWidgets(); true }
                ctrl && shift && event.key == Key.S -> { viewModel.toggleStudioMode(); true }

                // Studio Mode Transition
                event.key == Key.Spacebar && preferences.studioModeEnabled && selectedSourceId == null -> {
                    viewModel.transitionPreviewToProgram(); true
                }

                // Selected Source Precision Nudging & Delete
                selectedSourceId != null && event.key == Key.DirectionLeft -> {
                    viewModel.nudgeSelectedSource(if (shift) -0.05f else -0.01f, 0f); true
                }
                selectedSourceId != null && event.key == Key.DirectionRight -> {
                    viewModel.nudgeSelectedSource(if (shift) 0.05f else 0.01f, 0f); true
                }
                selectedSourceId != null && event.key == Key.DirectionUp -> {
                    viewModel.nudgeSelectedSource(0f, if (shift) -0.05f else -0.01f); true
                }
                selectedSourceId != null && event.key == Key.DirectionDown -> {
                    viewModel.nudgeSelectedSource(0f, if (shift) 0.05f else 0.01f); true
                }
                selectedSourceId != null && event.key == Key.Delete -> {
                    viewModel.deleteSelectedSource(); true
                }
                event.key == Key.Escape -> {
                    viewModel.selectSource(null)
                    focusManager.clearFocus()
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

            when {
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
                            onOpenKeyboardShortcuts = { showKeyboardShortcutsDialog = true },
                            onOpenSettings = { showSettingsDialog = true }
                        )

                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    StudioTopOutputBar(
                                        preferences = preferences,
                                        onToggleOrientation = { viewModel.toggleDualOutput(it) },
                                        onOpenDualOutputManager = { showDestinationsDialog = true },
                                        onToggleStudioMode = { viewModel.toggleStudioMode() },
                                        onTogglePrivacyShield = { viewModel.togglePrivacyShield() }
                                    )

                                    CanvasPreviewStage(
                                        horizontalEnabled = preferences.horizontalEnabled,
                                        verticalEnabled = preferences.verticalEnabled,
                                        studioModeEnabled = preferences.studioModeEnabled,
                                        privacyShieldActive = preferences.privacyShieldActive,
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
                                        hasCameraPermission = hasCameraPermission,
                                        isScreenCaptureActive = telemetry.isScreenCaptureActive,
                                        screenBitmap = screenBitmap,
                                        getVideoOverlayBitmap = { viewModel.getVideoOverlayBitmap(it) },
                                        onSelectOrientation = { viewModel.setEditingOrientation(it) },
                                        onSelectSource = { viewModel.selectSource(it) },
                                        onMoveSource = { src, orient, dx, dy -> viewModel.moveSourceRect(src, orient, dx, dy) },
                                        onResizeSource = { src, orient, dw, dh -> viewModel.resizeSourceRect(src, orient, dw, dh) },
                                        onTransitionToProgram = { viewModel.transitionPreviewToProgram() },
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
                                            onDeleteScene = { viewModel.deleteScene(it) },
                                            onSelectTransition = { viewModel.selectTransition(it) },
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
                                        onEditTitleClick = { showEditTitleDialog = true },
                                        onConnectTwitchChannel = { viewModel.connectTwitchChat(it) },
                                        onSendLocalMessage = { viewModel.addLocalChatMessage("Host", it) },
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
                                onToggleLive = { viewModel.toggleLiveStream() }
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
                                        onOpenDestinations = { showDestinationsDialog = true },
                                        onOpenSettings = { showSettingsDialog = true }
                                    )
                                    CanvasPreviewStage(
                                        horizontalEnabled = preferences.horizontalEnabled,
                                        verticalEnabled = preferences.verticalEnabled,
                                        studioModeEnabled = preferences.studioModeEnabled,
                                        privacyShieldActive = preferences.privacyShieldActive,
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
                                        hasCameraPermission = hasCameraPermission,
                                        isScreenCaptureActive = telemetry.isScreenCaptureActive,
                                        screenBitmap = screenBitmap,
                                        getVideoOverlayBitmap = { viewModel.getVideoOverlayBitmap(it) },
                                        onSelectOrientation = { viewModel.setEditingOrientation(it) },
                                        onSelectSource = { viewModel.selectSource(it) },
                                        onMoveSource = { src, orient, dx, dy -> viewModel.moveSourceRect(src, orient, dx, dy) },
                                        onResizeSource = { src, orient, dw, dh -> viewModel.resizeSourceRect(src, orient, dw, dh) },
                                        onTransitionToProgram = { viewModel.transitionPreviewToProgram() },
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
                                            destinationsCount = destinations.count { it.isEnabled },
                                            clipsCount = clips.size,
                                            onRequestAudioPermission = requestStudioPermissions,
                                            onAddCollection = { showAddCollectionDialog = true },
                                            onAddScene = { showAddSceneDialog = true },
                                            onAddSource = { showAddSourceDialog = true },
                                            onEditSource = { editingSource = it },
                                            onEditAudioChannel = { editingAudioChannel = it },
                                            onEditTitle = { showEditTitleDialog = true },
                                            onToggleScreenCapture = launchScreenCapture,
                                            onOpenDestinations = { showDestinationsDialog = true },
                                            onOpenThemes = { showThemesDialog = true },
                                            onOpenClips = { showClipsDialog = true },
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
                                onToggleLive = { viewModel.toggleLiveStream() }
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
                            onOpenDestinations = { showDestinationsDialog = true },
                            onOpenSettings = { showSettingsDialog = true }
                        )

                        CanvasPreviewStage(
                            horizontalEnabled = preferences.horizontalEnabled,
                            verticalEnabled = preferences.verticalEnabled,
                            studioModeEnabled = preferences.studioModeEnabled,
                            privacyShieldActive = preferences.privacyShieldActive,
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
                            hasCameraPermission = hasCameraPermission,
                            isScreenCaptureActive = telemetry.isScreenCaptureActive,
                            screenBitmap = screenBitmap,
                            getVideoOverlayBitmap = { viewModel.getVideoOverlayBitmap(it) },
                            onSelectOrientation = { viewModel.setEditingOrientation(it) },
                            onSelectSource = { viewModel.selectSource(it) },
                            onMoveSource = { src, orient, dx, dy -> viewModel.moveSourceRect(src, orient, dx, dy) },
                            onResizeSource = { src, orient, dw, dh -> viewModel.resizeSourceRect(src, orient, dw, dh) },
                            onTransitionToProgram = { viewModel.transitionPreviewToProgram() },
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
                                destinationsCount = destinations.count { it.isEnabled },
                                clipsCount = clips.size,
                                onRequestAudioPermission = requestStudioPermissions,
                                onAddCollection = { showAddCollectionDialog = true },
                                onAddScene = { showAddSceneDialog = true },
                                onAddSource = { showAddSourceDialog = true },
                                onEditSource = { editingSource = it },
                                onEditAudioChannel = { editingAudioChannel = it },
                                onEditTitle = { showEditTitleDialog = true },
                                onToggleScreenCapture = launchScreenCapture,
                                onOpenDestinations = { showDestinationsDialog = true },
                                onOpenThemes = { showThemesDialog = true },
                                onOpenClips = { showClipsDialog = true },
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
                            onToggleLive = { viewModel.toggleLiveStream() }
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
            onDeleteDestination = { viewModel.deleteDestination(it) }
        )
    }

    if (showSettingsDialog) {
        StudioSettingsDialog(
            preferences = preferences,
            isScreenCaptureActive = telemetry.isScreenCaptureActive,
            onDismiss = { showSettingsDialog = false },
            onUpdatePreferences = { viewModel.updatePreferences(it) },
            onToggleScreenCapture = launchScreenCapture,
            onOpenKeyboardShortcuts = {
                showSettingsDialog = false
                showKeyboardShortcutsDialog = true
            }
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
    scenes: List<com.example.data.model.StudioScene>,
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
    destinationsCount: Int,
    clipsCount: Int,
    onRequestAudioPermission: () -> Unit,
    onAddCollection: () -> Unit,
    onAddScene: () -> Unit,
    onAddSource: () -> Unit,
    onEditSource: (StudioSource) -> Unit,
    onEditAudioChannel: (AudioChannelConfig) -> Unit,
    onEditTitle: () -> Unit,
    onToggleScreenCapture: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenClips: () -> Unit,
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
            onDeleteScene = { viewModel.deleteScene(it) },
            onSelectTransition = { viewModel.selectTransition(it) }
        )
        2 -> SourcesDock(
            sources = activeSources,
            selectedSourceId = selectedSourceId,
            editingOrientation = editingOrientation,
            onSelectSource = { viewModel.selectSource(it) },
            onAddSourceClick = onAddSource,
            onToggleVisibility = { src, orient -> viewModel.toggleSourceVisibility(src, orient) },
            onToggleLock = { viewModel.toggleSourceLock(it) },
            onMoveSourceOrder = { id, up -> viewModel.moveSourceLayer(id, up) },
            onEditSourceProperties = onEditSource
        )
        3 -> ChatDock(
            telemetry = telemetry,
            streamTitle = preferences.streamTitle,
            twitchChannel = preferences.twitchChannel,
            chatStatus = chatStatus,
            messages = chatMessages,
            onEditTitleClick = onEditTitle,
            onConnectTwitchChannel = { viewModel.connectTwitchChat(it) },
            onSendLocalMessage = { viewModel.addLocalChatMessage("Host", it) }
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
            onOpenDestinations = onOpenDestinations,
            onOpenThemes = onOpenThemes,
            onOpenClips = onOpenClips,
            onOpenShortcuts = onOpenShortcuts,
            onOpenSettings = onOpenSettings
        )
    }
}

@Composable
private fun StudioNavigationRail(
    studioModeEnabled: Boolean,
    isScreenCaptureActive: Boolean,
    privacyShieldActive: Boolean,
    chatVisible: Boolean,
    onToggleStudioMode: () -> Unit,
    onTogglePrivacyShield: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenClips: () -> Unit,
    onToggleScreenCapture: () -> Unit,
    onToggleChatPane: () -> Unit,
    onOpenKeyboardShortcuts: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(52.dp)
            .fillMaxHeight()
            .background(StudioRail)
            .border(1.dp, StudioBorder)
            .padding(vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MintAccent.copy(alpha = 0.16f))
                    .border(1.dp, MintAccent, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Rounded.Podcasts, contentDescription = "mobileLabs", tint = MintAccentBright, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(2.dp))
            RailIconButton(Icons.AutoMirrored.Rounded.ViewQuilt, "Studio Mode (Ctrl+Shift+S)", studioModeEnabled, onToggleStudioMode)
            RailIconButton(Icons.Rounded.Palette, "Free Overlay Themes", false, onOpenThemes)
            RailIconButton(Icons.Rounded.Cast, "Multistream Destinations", false, onOpenDestinations)
            RailIconButton(Icons.Rounded.VideoLibrary, "Recordings & Replay Clips", false, onOpenClips)
            RailIconButton(
                icon = if (isScreenCaptureActive) Icons.Rounded.CastConnected else Icons.Rounded.DesktopWindows,
                contentDesc = "Toggle Screen Capture",
                active = isScreenCaptureActive,
                onClick = onToggleScreenCapture
            )
            RailIconButton(
                icon = Icons.Rounded.Shield,
                contentDesc = "Privacy Shield (Ctrl+P)",
                active = privacyShieldActive,
                activeTint = RecordAmber,
                onClick = onTogglePrivacyShield
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RailIconButton(Icons.Rounded.Keyboard, "Keyboard Shortcuts (F12)", false, onOpenKeyboardShortcuts)
            RailIconButton(Icons.Rounded.ChatBubbleOutline, "Toggle Chat Panel", chatVisible, onToggleChatPane)
            RailIconButton(Icons.Rounded.Settings, "OBS Studio Settings", false, onOpenSettings)
        }
    }
}

@Composable
private fun RailIconButton(
    icon: ImageVector,
    contentDesc: String,
    active: Boolean,
    onClick: () -> Unit,
    activeTint: Color = MintAccentBright
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) activeTint.copy(alpha = 0.18f) else Color.Transparent)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = if (active) activeTint else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StudioTopOutputBar(
    preferences: StudioPreferences,
    onToggleOrientation: (OutputOrientation) -> Unit,
    onOpenDualOutputManager: () -> Unit,
    onToggleStudioMode: () -> Unit,
    onTogglePrivacyShield: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioHeader)
            .border(1.dp, StudioBorder)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("mobileLabs", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Surface(color = MintAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                Text(
                    "${preferences.resolutionHeight}p${preferences.targetFps} • ${preferences.videoBitrateKbps}k",
                    color = MintAccentBright,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutputToggleChip(
                label = "Horizontal Output",
                icon = Icons.Rounded.DesktopWindows,
                active = preferences.horizontalEnabled,
                onClick = { onToggleOrientation(OutputOrientation.HORIZONTAL) }
            )
            OutputToggleChip(
                label = "Vertical Output",
                icon = Icons.Rounded.Smartphone,
                active = preferences.verticalEnabled,
                onClick = { onToggleOrientation(OutputOrientation.VERTICAL) }
            )
            OutputToggleChip(
                label = "Studio Mode",
                icon = Icons.AutoMirrored.Rounded.ViewQuilt,
                active = preferences.studioModeEnabled,
                onClick = onToggleStudioMode
            )
            if (preferences.privacyShieldActive) {
                OutputToggleChip(
                    label = "Shield ON",
                    icon = Icons.Rounded.Shield,
                    active = true,
                    activeColor = RecordAmber,
                    onClick = onTogglePrivacyShield
                )
            }
        }

        Text(
            text = "Manage Dual Output",
            color = MintAccentBright,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onOpenDualOutputManager() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun OutputToggleChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    activeColor: Color = MintAccentBright,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (active) StudioElevated else Color.Transparent)
            .border(1.dp, if (active) activeColor else StudioBorder, RoundedCornerShape(5.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (active) activeColor else TextMuted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = if (active) TextPrimary else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CompactTopHeader(
    preferences: StudioPreferences,
    telemetry: BroadcastTelemetry,
    onToggleOrientation: (OutputOrientation) -> Unit,
    onToggleDual: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioHeader)
            .border(1.dp, StudioBorder)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Podcasts, contentDescription = null, tint = MintAccentBright, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("mobileLabs", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (telemetry.connectionState) {
                            StreamConnectionState.LIVE -> LiveCrimsonBright
                            StreamConnectionState.RECONNECTING, StreamConnectionState.CONNECTING -> RecordAmber
                            else -> TextMuted
                        }
                    )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val isDual = preferences.horizontalEnabled && preferences.verticalEnabled
            CompactPill("16:9", preferences.horizontalEnabled && !isDual) { onToggleOrientation(OutputOrientation.HORIZONTAL) }
            CompactPill("9:16", preferences.verticalEnabled && !isDual) { onToggleOrientation(OutputOrientation.VERTICAL) }
            CompactPill("Dual", isDual) { onToggleDual() }
            IconButton(onClick = onOpenDestinations, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Cast, contentDescription = "Destinations", tint = MintAccentBright, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun CompactPill(text: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (active) MintAccent.copy(alpha = 0.2f) else StudioVoid,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .border(1.dp, if (active) MintAccent else StudioBorder, RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (active) MintAccentBright else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MobileDockTabStrip(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val tabs = listOf(
        "Mixer" to Icons.Rounded.GraphicEq,
        "Scenes" to Icons.AutoMirrored.Rounded.ViewQuilt,
        "Sources" to Icons.Rounded.Layers,
        "Chat" to Icons.Rounded.ChatBubbleOutline,
        "Tools" to Icons.Rounded.Tune
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioHeader)
            .border(1.dp, StudioBorder)
    ) {
        tabs.forEachIndexed { idx, (label, icon) ->
            val active = selectedTab == idx
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectTab(idx) }
                    .padding(vertical = 6.dp)
                    .testTag("mobile_tab_${label.lowercase()}")
            ) {
                Icon(icon, contentDescription = label, tint = if (active) MintAccentBright else TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = if (active) MintAccentBright else TextSecondary)
            }
        }
    }
}

@Composable
private fun MobileStudioToolsGrid(
    studioModeEnabled: Boolean,
    isScreenCaptureActive: Boolean,
    privacyShieldActive: Boolean,
    destinationCount: Int,
    clipCount: Int,
    onToggleStudioMode: () -> Unit,
    onToggleScreenCapture: () -> Unit,
    onTogglePrivacyShield: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenClips: () -> Unit,
    onOpenShortcuts: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioDock)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Studio Tools & Outputs", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolActionCard(
                title = if (isScreenCaptureActive) "Screen Active" else "Screen Capture",
                subtitle = if (isScreenCaptureActive) "Tap to stop" else "Capture games/apps",
                icon = Icons.Rounded.DesktopWindows,
                active = isScreenCaptureActive,
                onClick = onToggleScreenCapture,
                modifier = Modifier.weight(1f)
            )
            ToolActionCard(
                title = "Privacy Shield",
                subtitle = if (privacyShieldActive) "Screen hidden" else "Hide screen fast",
                icon = Icons.Rounded.Shield,
                active = privacyShieldActive,
                onClick = onTogglePrivacyShield,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolActionCard(
                title = "Destinations",
                subtitle = "$destinationCount RTMP active",
                icon = Icons.Rounded.Cast,
                active = destinationCount > 0,
                onClick = onOpenDestinations,
                modifier = Modifier.weight(1f)
            )
            ToolActionCard(
                title = "Studio Mode",
                subtitle = if (studioModeEnabled) "Preview -> Program" else "Direct Program",
                icon = Icons.AutoMirrored.Rounded.ViewQuilt,
                active = studioModeEnabled,
                onClick = onToggleStudioMode,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolActionCard(
                title = "Recordings",
                subtitle = "$clipCount MP4 clips",
                icon = Icons.Rounded.VideoLibrary,
                active = false,
                onClick = onOpenClips,
                modifier = Modifier.weight(1f)
            )
            ToolActionCard(
                title = "Free Themes",
                subtitle = "Zero-paywall skins",
                icon = Icons.Rounded.Palette,
                active = false,
                onClick = onOpenThemes,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolActionCard(
                title = "Hotkeys",
                subtitle = "Keyboard shortcuts",
                icon = Icons.Rounded.Keyboard,
                active = false,
                onClick = onOpenShortcuts,
                modifier = Modifier.weight(1f)
            )
            ToolActionCard(
                title = "OBS Settings",
                subtitle = "Bitrate, FPS, Audio",
                icon = Icons.Rounded.Settings,
                active = false,
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ToolActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) MintAccent.copy(alpha = 0.14f) else StudioVoid.copy(alpha = 0.7f))
            .border(1.dp, if (active) MintAccent else StudioBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(9.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (active) MintAccentBright else TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BottomBroadcastControlBar(
    telemetry: BroadcastTelemetry,
    isCompact: Boolean,
    onTestWidgets: () -> Unit,
    onToggleRecord: () -> Unit,
    onSaveReplay: () -> Unit,
    onToggleLive: () -> Unit
) {
    val isLive = telemetry.connectionState == StreamConnectionState.LIVE ||
        telemetry.connectionState == StreamConnectionState.CONNECTING ||
        telemetry.connectionState == StreamConnectionState.RECONNECTING
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioHeader)
            .border(1.dp, StudioBorder)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (isCompact) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "CPU %.1f%% • %.0f FPS • %d kbps", telemetry.cpuUsagePercent, telemetry.currentFps, telemetry.currentBitrateKbps),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = telemetry.statusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (telemetry.connectionState) {
                        StreamConnectionState.ERROR -> LiveCrimsonBright
                        StreamConnectionState.RECONNECTING -> RecordAmber
                        else -> MintAccent
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!isCompact) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f%% CPU", telemetry.cpuUsagePercent),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.2f FPS", telemetry.currentFps),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${telemetry.currentBitrateKbps} kbps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MintAccent
                    )
                    Text(
                        text = "Drop: ${telemetry.droppedFrames}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Text(
                        text = telemetry.statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (telemetry.connectionState) {
                            StreamConnectionState.ERROR -> LiveCrimsonBright
                            StreamConnectionState.RECONNECTING -> RecordAmber
                            else -> TextSecondary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = if (isCompact) Modifier.fillMaxWidth() else Modifier
            ) {
                Surface(
                    color = StudioElevated,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                        .clickable { onTestWidgets() }
                        .testTag("test_widgets_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MintAccentBright, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isCompact) "Test" else "Test Widgets",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (telemetry.isRecording) LiveCrimsonBright else StudioElevated)
                        .border(1.5.dp, if (telemetry.isRecording) Color.White else LiveCrimsonBright, CircleShape)
                        .clickable { onToggleRecord() }
                        .testTag("record_button")
                ) {
                    Text(
                        text = if (telemetry.isRecording) formatDuration(telemetry.elapsedRecordSeconds).takeLast(5) else "REC",
                        color = if (telemetry.isRecording) Color.White else LiveCrimsonBright,
                        fontSize = if (telemetry.isRecording) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onSaveReplay,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioElevated)
                        .border(1.dp, StudioBorder, CircleShape)
                        .testTag("save_replay_button")
                ) {
                    Icon(Icons.Rounded.History, contentDescription = "Save Instant Replay Clip", tint = MintAccentBright, modifier = Modifier.size(18.dp))
                }

                if (isCompact) Spacer(Modifier.weight(1f))

                Button(
                    onClick = onToggleLive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLive) LiveCrimson else MintAccent,
                        contentColor = if (isLive) Color.White else StudioVoid
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(38.dp).testTag("go_live_button")
                ) {
                    Text(
                        text = when (telemetry.connectionState) {
                            StreamConnectionState.LIVE -> "End Stream"
                            StreamConnectionState.RECONNECTING -> "Reconnecting..."
                            StreamConnectionState.CONNECTING -> "Connecting..."
                            else -> "Go Live"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
