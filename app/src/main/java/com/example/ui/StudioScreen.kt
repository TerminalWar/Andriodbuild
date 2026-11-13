package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ViewQuilt
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AudioChannelConfig
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
    val screenPreviewTick by viewModel.screenPreviewTick.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    // Modals & Sheets State
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
    var showChatPaneDesktop by remember { mutableStateOf(true) }

    // Permissions Launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val cam = results[Manifest.permission.CAMERA]
            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        val mic = results[Manifest.permission.RECORD_AUDIO]
            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        viewModel.updatePermissions(cam, mic)
    }

    // Screen Capture MediaProjection Launcher
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

    // Sync initial permissions on launch
    LaunchedEffect(Unit) {
        val cam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissions(cam, mic)
        if (!cam || !mic) {
            requestStudioPermissions()
        }
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearToast()
    }

    val activeSourcesForDock = if (preferences.studioModeEnabled) previewSources else programSources
    val activeSceneForDock = if (preferences.studioModeEnabled) previewScene else programScene
    val screenBitmap = remember(screenPreviewTick) { viewModel.getLatestScreenBitmap() }

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
        ) {
            val isWideStudio = maxWidth >= 600.dp

            if (isWideStudio) {
                // 1-to-1 Desktop OBS / Streamlabs Layout (Landscape / Tablet)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    // Left Vertical Utility Rail
                    StudioNavigationRail(
                        studioModeEnabled = preferences.studioModeEnabled,
                        isScreenCaptureActive = telemetry.isScreenCaptureActive,
                        chatVisible = showChatPaneDesktop,
                        onToggleStudioMode = { viewModel.toggleStudioMode() },
                        onOpenThemes = { showThemesDialog = true },
                        onOpenDestinations = { showDestinationsDialog = true },
                        onOpenClips = { showClipsDialog = true },
                        onToggleScreenCapture = launchScreenCapture,
                        onToggleChatPane = { showChatPaneDesktop = !showChatPaneDesktop },
                        onOpenSettings = { showSettingsDialog = true }
                    )

                    // Main Center + Right Chat + Bottom Bar
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            // Center Preview + 3-Column Docks
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                StudioTopOutputBar(
                                    preferences = preferences,
                                    telemetry = telemetry,
                                    onToggleOrientation = { viewModel.toggleDualOutput(it) },
                                    onOpenDualOutputManager = { showDestinationsDialog = true },
                                    onToggleStudioMode = { viewModel.toggleStudioMode() }
                                )

                                CanvasPreviewStage(
                                    horizontalEnabled = preferences.horizontalEnabled,
                                    verticalEnabled = preferences.verticalEnabled,
                                    studioModeEnabled = preferences.studioModeEnabled,
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
                                    onSelectOrientation = { viewModel.setEditingOrientation(it) },
                                    onSelectSource = { viewModel.selectSource(it) },
                                    onMoveSource = { src, orient, dx, dy -> viewModel.moveSourceRect(src, orient, dx, dy) },
                                    onTransitionToProgram = { viewModel.transitionPreviewToProgram() },
                                    onRequestCameraPermission = requestStudioPermissions,
                                    onRequestScreenCapture = launchScreenCapture,
                                    modifier = Modifier.weight(1.12f)
                                )

                                // 3-Column Bottom Docks: Scenes | Sources | Audio Mixer
                                Row(
                                    modifier = Modifier
                                        .weight(0.88f)
                                        .fillMaxWidth()
                                ) {
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
                                        onRequestAudioPermission = requestStudioPermissions,
                                        onGainChange = { ch, db -> viewModel.setAudioGain(ch, db) },
                                        onToggleMute = { viewModel.toggleAudioMute(it) },
                                        onOpenChannelSettings = { editingAudioChannel = it },
                                        modifier = Modifier.weight(1.1f)
                                    )
                                }
                            }

                            // Right Dock: Stream Chat
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
                                    modifier = Modifier.width(268.dp).fillMaxHeight()
                                )
                            }
                        }

                        // Bottom Telemetry & Broadcast Action Bar
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
            } else {
                // Compact Portrait Phone Layout (Preserves all docks via clean workspace tabs!)
                var selectedMobileTab by remember { mutableIntStateOf(0) } // 0=Mixer, 1=Scenes, 2=Sources, 3=Chat, 4=Studio

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
                        onSelectOrientation = { viewModel.setEditingOrientation(it) },
                        onSelectSource = { viewModel.selectSource(it) },
                        onMoveSource = { src, orient, dx, dy -> viewModel.moveSourceRect(src, orient, dx, dy) },
                        onTransitionToProgram = { viewModel.transitionPreviewToProgram() },
                        onRequestCameraPermission = requestStudioPermissions,
                        onRequestScreenCapture = launchScreenCapture,
                        modifier = Modifier.weight(0.39f)
                    )

                    // Mobile Dock Tab Switcher
                    MobileDockTabStrip(
                        selectedTab = selectedMobileTab,
                        onSelectTab = { selectedMobileTab = it }
                    )

                    Box(modifier = Modifier.weight(0.61f).fillMaxWidth()) {
                        when (selectedMobileTab) {
                            0 -> AudioMixerDock(
                                channels = audioChannels,
                                readings = meterReadings,
                                hasAudioPermission = hasAudioPermission,
                                onRequestAudioPermission = requestStudioPermissions,
                                onGainChange = { ch, db -> viewModel.setAudioGain(ch, db) },
                                onToggleMute = { viewModel.toggleAudioMute(it) },
                                onOpenChannelSettings = { editingAudioChannel = it }
                            )
                            1 -> ScenesDock(
                                collections = collections,
                                scenes = scenes,
                                activeSceneId = activeSceneForDock?.id,
                                selectedTransition = selectedTransition,
                                onSelectCollection = { viewModel.selectCollection(it) },
                                onAddCollectionClick = { showAddCollectionDialog = true },
                                onSelectScene = { viewModel.selectScene(it) },
                                onAddSceneClick = { showAddSceneDialog = true },
                                onDeleteScene = { viewModel.deleteScene(it) },
                                onSelectTransition = { viewModel.selectTransition(it) }
                            )
                            2 -> SourcesDock(
                                sources = activeSourcesForDock,
                                selectedSourceId = selectedSourceId,
                                editingOrientation = editingOrientation,
                                onSelectSource = { viewModel.selectSource(it) },
                                onAddSourceClick = { showAddSourceDialog = true },
                                onToggleVisibility = { src, orient -> viewModel.toggleSourceVisibility(src, orient) },
                                onToggleLock = { viewModel.toggleSourceLock(it) },
                                onMoveSourceOrder = { id, up -> viewModel.moveSourceLayer(id, up) },
                                onEditSourceProperties = { editingSource = it }
                            )
                            3 -> ChatDock(
                                telemetry = telemetry,
                                streamTitle = preferences.streamTitle,
                                twitchChannel = preferences.twitchChannel,
                                chatStatus = chatStatus,
                                messages = chatMessages,
                                onEditTitleClick = { showEditTitleDialog = true },
                                onConnectTwitchChannel = { viewModel.connectTwitchChat(it) },
                                onSendLocalMessage = { viewModel.addLocalChatMessage("Host", it) }
                            )
                            else -> MobileStudioToolsGrid(
                                studioModeEnabled = preferences.studioModeEnabled,
                                isScreenCaptureActive = telemetry.isScreenCaptureActive,
                                destinationCount = destinations.count { it.isEnabled },
                                clipCount = clips.size,
                                onToggleStudioMode = { viewModel.toggleStudioMode() },
                                onToggleScreenCapture = launchScreenCapture,
                                onOpenDestinations = { showDestinationsDialog = true },
                                onOpenThemes = { showThemesDialog = true },
                                onOpenClips = { showClipsDialog = true },
                                onOpenSettings = { showSettingsDialog = true }
                            )
                        }
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
            onToggleScreenCapture = launchScreenCapture
        )
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
private fun StudioNavigationRail(
    studioModeEnabled: Boolean,
    isScreenCaptureActive: Boolean,
    chatVisible: Boolean,
    onToggleStudioMode: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenClips: () -> Unit,
    onToggleScreenCapture: () -> Unit,
    onToggleChatPane: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(48.dp)
            .fillMaxHeight()
            .background(StudioRail)
            .border(1.dp, StudioBorder)
            .padding(vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Brand emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MintAccent.copy(alpha = 0.16f))
                    .border(1.dp, MintAccent, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Rounded.Podcasts, contentDescription = "mobileLabs", tint = MintAccentBright, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(4.dp))
            RailIconButton(Icons.Rounded.ViewQuilt, "Studio Mode", studioModeEnabled, onToggleStudioMode)
            RailIconButton(Icons.Rounded.Palette, "Free Overlay Themes", false, onOpenThemes)
            RailIconButton(Icons.Rounded.Cast, "Multistream Destinations", false, onOpenDestinations)
            RailIconButton(Icons.Rounded.VideoLibrary, "Recordings & Replay Clips", false, onOpenClips)
            RailIconButton(
                icon = if (isScreenCaptureActive) Icons.Rounded.CastConnected else Icons.Rounded.DesktopWindows,
                contentDesc = "Toggle Screen Capture",
                active = isScreenCaptureActive,
                onClick = onToggleScreenCapture
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RailIconButton(Icons.Rounded.ChatBubbleOutline, "Toggle Chat Panel", chatVisible, onToggleChatPane)
            RailIconButton(Icons.Rounded.Settings, "Encoder & Studio Settings", false, onOpenSettings)
        }
    }
}

@Composable
private fun RailIconButton(
    icon: ImageVector,
    contentDesc: String,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) MintAccent.copy(alpha = 0.18f) else Color.Transparent)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = if (active) MintAccentBright else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StudioTopOutputBar(
    preferences: StudioPreferences,
    telemetry: BroadcastTelemetry,
    onToggleOrientation: (OutputOrientation) -> Unit,
    onOpenDualOutputManager: () -> Unit,
    onToggleStudioMode: () -> Unit
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
                    "FREE • ALL UNLOCKED",
                    color = MintAccentBright,
                    fontSize = 9.sp,
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
                icon = Icons.Rounded.ViewQuilt,
                active = preferences.studioModeEnabled,
                onClick = onToggleStudioMode
            )
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
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (active) StudioElevated else Color.Transparent)
            .border(1.dp, if (active) MintAccent else StudioBorder, RoundedCornerShape(5.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (active) MintAccentBright else TextMuted, modifier = Modifier.size(14.dp))
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
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Podcasts, contentDescription = null, tint = MintAccentBright, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("mobileLabs", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (telemetry.connectionState == StreamConnectionState.LIVE) LiveCrimsonBright else TextMuted
                    )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val isDual = preferences.horizontalEnabled && preferences.verticalEnabled
            CompactPill("16:9", preferences.horizontalEnabled && !isDual) { onToggleOrientation(OutputOrientation.HORIZONTAL) }
            CompactPill("9:16", preferences.verticalEnabled && !isDual) { onToggleOrientation(OutputOrientation.VERTICAL) }
            CompactPill("Dual", isDual) { onToggleDual() }
            IconButton(onClick = onOpenDestinations, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Cast, contentDescription = "Destinations", tint = MintAccentBright, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(18.dp))
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
        "Scenes" to Icons.Rounded.ViewQuilt,
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
                    .padding(vertical = 7.dp)
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
    destinationCount: Int,
    clipCount: Int,
    onToggleStudioMode: () -> Unit,
    onToggleScreenCapture: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenClips: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioDock)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Studio Tools & Outputs", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolActionCard(
                title = if (isScreenCaptureActive) "Screen Active" else "Screen Capture",
                subtitle = if (isScreenCaptureActive) "Tap to stop" else "Capture games/apps",
                icon = Icons.Rounded.DesktopWindows,
                active = isScreenCaptureActive,
                onClick = onToggleScreenCapture,
                modifier = Modifier.weight(1f)
            )
            ToolActionCard(
                title = "Destinations",
                subtitle = "$destinationCount RTMP enabled",
                icon = Icons.Rounded.Cast,
                active = destinationCount > 0,
                onClick = onOpenDestinations,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolActionCard(
                title = "Studio Mode",
                subtitle = if (studioModeEnabled) "Preview -> Program ON" else "Direct Program mode",
                icon = Icons.Rounded.ViewQuilt,
                active = studioModeEnabled,
                onClick = onToggleStudioMode,
                modifier = Modifier.weight(1f)
            )
            ToolActionCard(
                title = "Free Themes",
                subtitle = "Zero-paywall overlays",
                icon = Icons.Rounded.Palette,
                active = false,
                onClick = onOpenThemes,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolActionCard(
                title = "Recordings",
                subtitle = "$clipCount saved MP4 clips",
                icon = Icons.Rounded.VideoLibrary,
                active = false,
                onClick = onOpenClips,
                modifier = Modifier.weight(1f)
            )
            ToolActionCard(
                title = "Encoder",
                subtitle = "Bitrate, FPS, Replay",
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
            .padding(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (active) MintAccentBright else TextSecondary, modifier = Modifier.size(20.dp))
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
    val isLive = telemetry.connectionState == StreamConnectionState.LIVE || telemetry.connectionState == StreamConnectionState.CONNECTING
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
                    color = if (telemetry.connectionState == StreamConnectionState.ERROR) LiveCrimsonBright else MintAccent,
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
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = if (isCompact) Modifier.fillMaxWidth() else Modifier
            ) {
                // Test Widgets button
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

                // REC circular button
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

                // Replay Buffer Save button
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

                // Primary Go Live / End Stream Button
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
