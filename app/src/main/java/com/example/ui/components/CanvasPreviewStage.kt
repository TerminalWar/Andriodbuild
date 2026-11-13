package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.model.DonationGoalState
import com.example.data.model.LiveAlertEvent
import com.example.data.model.OutputOrientation
import com.example.data.model.SourceType
import com.example.data.model.StreamChatMessage
import com.example.data.model.StudioScene
import com.example.data.model.StudioSource
import com.example.data.model.TransitionType
import com.example.streaming.MediaProjectionHolder
import com.example.ui.theme.LiveCrimsonBright
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MintAccentBright
import com.example.ui.theme.RecordAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDock
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioHeader
import com.example.ui.theme.StudioVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TransformPreset
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CanvasPreviewStage(
    horizontalEnabled: Boolean,
    verticalEnabled: Boolean,
    studioModeEnabled: Boolean,
    privacyShieldActive: Boolean,
    isFullscreenPreview: Boolean,
    editingOrientation: OutputOrientation,
    programScene: StudioScene?,
    previewScene: StudioScene?,
    programSources: List<StudioSource>,
    previewSources: List<StudioSource>,
    selectedSourceId: Long?,
    activeTransition: TransitionType,
    activeAlert: LiveAlertEvent?,
    donationGoal: DonationGoalState,
    recentChat: List<StreamChatMessage>,
    pinnedChatMessage: StreamChatMessage?,
    liveElapsedSeconds: Long,
    currentFps: Float,
    hasCameraPermission: Boolean,
    isScreenCaptureActive: Boolean,
    screenBitmap: Bitmap?,
    cameraBitmap: Bitmap?,
    getVideoOverlayBitmap: (String?) -> Bitmap?,
    onSelectOrientation: (OutputOrientation) -> Unit,
    onSelectSource: (Long?) -> Unit,
    onMoveSource: (Long, OutputOrientation, Float, Float) -> Unit,
    onResizeSource: (Long, OutputOrientation, Float, Float) -> Unit,
    onCommitTransform: (Long, OutputOrientation) -> Unit,
    onApplyTransformPreset: (StudioSource, OutputOrientation, TransformPreset) -> Unit,
    onToggleSourceLock: (StudioSource) -> Unit,
    onToggleCameraFacing: (StudioSource) -> Unit,
    onEditSourceProperties: (StudioSource) -> Unit,
    onDeleteSource: (Long) -> Unit,
    onTransitionToProgram: () -> Unit,
    onToggleFullscreenPreview: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSources = if (studioModeEnabled) previewSources else programSources
    val selectedSource = remember(activeSources, selectedSourceId) {
        activeSources.firstOrNull { it.id == selectedSourceId }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioVoid)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isFullscreenPreview) {
                InteractiveStudioCanvas(
                    orientation = editingOrientation,
                    scene = programScene,
                    sources = programSources,
                    selectedSourceId = null,
                    activeAlert = activeAlert,
                    donationGoal = donationGoal,
                    recentChat = recentChat,
                    pinnedChatMessage = pinnedChatMessage,
                    liveElapsedSeconds = liveElapsedSeconds,
                    currentFps = currentFps,
                    hasCameraPermission = hasCameraPermission,
                    isScreenCaptureActive = isScreenCaptureActive,
                    privacyShieldActive = privacyShieldActive,
                    screenBitmap = screenBitmap,
                    cameraBitmap = cameraBitmap,
                    getVideoOverlayBitmap = getVideoOverlayBitmap,
                    isInteractive = false,
                    isPrimaryCameraHost = true,
                    onSelectSource = {},
                    onMoveSource = { _, _, _ -> },
                    onResizeSource = { _, _, _ -> },
                    onCommitTransform = {},
                    onRequestCameraPermission = onRequestCameraPermission,
                    onRequestScreenCapture = onRequestScreenCapture,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (studioModeEnabled) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CanvasHeaderLabel(
                            title = "PREVIEW: ${previewScene?.name ?: "Scene"}",
                            accent = MintAccent,
                            isSelected = true
                        )
                        Spacer(Modifier.height(4.dp))
                        InteractiveStudioCanvas(
                            orientation = editingOrientation,
                            scene = previewScene,
                            sources = previewSources,
                            selectedSourceId = selectedSourceId,
                            activeAlert = activeAlert,
                            donationGoal = donationGoal,
                            recentChat = recentChat,
                            pinnedChatMessage = pinnedChatMessage,
                            liveElapsedSeconds = liveElapsedSeconds,
                            currentFps = currentFps,
                            hasCameraPermission = hasCameraPermission,
                            isScreenCaptureActive = isScreenCaptureActive,
                            privacyShieldActive = privacyShieldActive,
                            screenBitmap = screenBitmap,
                            cameraBitmap = cameraBitmap,
                            getVideoOverlayBitmap = getVideoOverlayBitmap,
                            isInteractive = true,
                            isPrimaryCameraHost = true,
                            onSelectSource = onSelectSource,
                            onMoveSource = { id, dx, dy -> onMoveSource(id, editingOrientation, dx, dy) },
                            onResizeSource = { id, dw, dh -> onResizeSource(id, editingOrientation, dw, dh) },
                            onCommitTransform = { id -> onCommitTransform(id, editingOrientation) },
                            onRequestCameraPermission = onRequestCameraPermission,
                            onRequestScreenCapture = onRequestScreenCapture,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Button(
                            onClick = onTransitionToProgram,
                            colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = StudioVoid),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("studio_transition_button")
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = "Transition to program", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(activeTransition.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CanvasHeaderLabel(
                            title = "PROGRAM: ${programScene?.name ?: "Scene"}",
                            accent = LiveCrimsonBright,
                            isSelected = false
                        )
                        Spacer(Modifier.height(4.dp))
                        // Secondary canvas uses captured frames so it NEVER steals CameraX from Preview
                        InteractiveStudioCanvas(
                            orientation = editingOrientation,
                            scene = programScene,
                            sources = programSources,
                            selectedSourceId = null,
                            activeAlert = activeAlert,
                            donationGoal = donationGoal,
                            recentChat = recentChat,
                            pinnedChatMessage = pinnedChatMessage,
                            liveElapsedSeconds = liveElapsedSeconds,
                            currentFps = currentFps,
                            hasCameraPermission = hasCameraPermission,
                            isScreenCaptureActive = isScreenCaptureActive,
                            privacyShieldActive = privacyShieldActive,
                            screenBitmap = screenBitmap,
                            cameraBitmap = cameraBitmap,
                            getVideoOverlayBitmap = getVideoOverlayBitmap,
                            isInteractive = false,
                            isPrimaryCameraHost = false,
                            onSelectSource = {},
                            onMoveSource = { _, _, _ -> },
                            onResizeSource = { _, _, _ -> },
                            onCommitTransform = {},
                            onRequestCameraPermission = onRequestCameraPermission,
                            onRequestScreenCapture = onRequestScreenCapture,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (horizontalEnabled) {
                        Column(
                            modifier = Modifier
                                .weight(if (verticalEnabled) 1.78f else 1f)
                                .fillMaxHeight()
                                .clickable { onSelectOrientation(OutputOrientation.HORIZONTAL) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CanvasHeaderLabel(
                                title = "Horizontal Output (16:9)",
                                accent = MintAccent,
                                isSelected = editingOrientation == OutputOrientation.HORIZONTAL,
                                icon = {
                                    Icon(
                                        Icons.Rounded.DesktopWindows,
                                        contentDescription = null,
                                        tint = if (editingOrientation == OutputOrientation.HORIZONTAL) MintAccent else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                            Spacer(Modifier.height(3.dp))
                            InteractiveStudioCanvas(
                                orientation = OutputOrientation.HORIZONTAL,
                                scene = programScene,
                                sources = programSources,
                                selectedSourceId = if (editingOrientation == OutputOrientation.HORIZONTAL) selectedSourceId else null,
                                activeAlert = activeAlert,
                                donationGoal = donationGoal,
                                recentChat = recentChat,
                                pinnedChatMessage = pinnedChatMessage,
                                liveElapsedSeconds = liveElapsedSeconds,
                                currentFps = currentFps,
                                hasCameraPermission = hasCameraPermission,
                                isScreenCaptureActive = isScreenCaptureActive,
                                privacyShieldActive = privacyShieldActive,
                                screenBitmap = screenBitmap,
                                cameraBitmap = cameraBitmap,
                                getVideoOverlayBitmap = getVideoOverlayBitmap,
                                isInteractive = true,
                                isPrimaryCameraHost = true,
                                onSelectSource = {
                                    onSelectOrientation(OutputOrientation.HORIZONTAL)
                                    onSelectSource(it)
                                },
                                onMoveSource = { id, dx, dy ->
                                    onSelectOrientation(OutputOrientation.HORIZONTAL)
                                    onMoveSource(id, OutputOrientation.HORIZONTAL, dx, dy)
                                },
                                onResizeSource = { id, dw, dh ->
                                    onSelectOrientation(OutputOrientation.HORIZONTAL)
                                    onResizeSource(id, OutputOrientation.HORIZONTAL, dw, dh)
                                },
                                onCommitTransform = { id -> onCommitTransform(id, OutputOrientation.HORIZONTAL) },
                                onRequestCameraPermission = onRequestCameraPermission,
                                onRequestScreenCapture = onRequestScreenCapture,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (verticalEnabled) {
                        Column(
                            modifier = Modifier
                                .weight(if (horizontalEnabled) 0.92f else 1f)
                                .fillMaxHeight()
                                .clickable { onSelectOrientation(OutputOrientation.VERTICAL) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CanvasHeaderLabel(
                                title = "Vertical Output (9:16)",
                                accent = MintAccent,
                                isSelected = editingOrientation == OutputOrientation.VERTICAL,
                                icon = {
                                    Icon(
                                        Icons.Rounded.Smartphone,
                                        contentDescription = null,
                                        tint = if (editingOrientation == OutputOrientation.VERTICAL) MintAccent else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                            Spacer(Modifier.height(3.dp))
                            // Primary camera owner only if Horizontal is disabled, preventing camera conflict
                            InteractiveStudioCanvas(
                                orientation = OutputOrientation.VERTICAL,
                                scene = programScene,
                                sources = programSources,
                                selectedSourceId = if (editingOrientation == OutputOrientation.VERTICAL) selectedSourceId else null,
                                activeAlert = activeAlert,
                                donationGoal = donationGoal,
                                recentChat = recentChat,
                                pinnedChatMessage = pinnedChatMessage,
                                liveElapsedSeconds = liveElapsedSeconds,
                                currentFps = currentFps,
                                hasCameraPermission = hasCameraPermission,
                                isScreenCaptureActive = isScreenCaptureActive,
                                privacyShieldActive = privacyShieldActive,
                                screenBitmap = screenBitmap,
                                cameraBitmap = cameraBitmap,
                                getVideoOverlayBitmap = getVideoOverlayBitmap,
                                isInteractive = true,
                                isPrimaryCameraHost = !horizontalEnabled,
                                onSelectSource = {
                                    onSelectOrientation(OutputOrientation.VERTICAL)
                                    onSelectSource(it)
                                },
                                onMoveSource = { id, dx, dy ->
                                    onSelectOrientation(OutputOrientation.VERTICAL)
                                    onMoveSource(id, OutputOrientation.VERTICAL, dx, dy)
                                },
                                onResizeSource = { id, dw, dh ->
                                    onSelectOrientation(OutputOrientation.VERTICAL)
                                    onResizeSource(id, OutputOrientation.VERTICAL, dw, dh)
                                },
                                onCommitTransform = { id -> onCommitTransform(id, OutputOrientation.VERTICAL) },
                                onRequestCameraPermission = onRequestCameraPermission,
                                onRequestScreenCapture = onRequestScreenCapture,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Fullscreen / Clean Program toggle button (top-right corner of stage)
            IconButton(
                onClick = onToggleFullscreenPreview,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(StudioHeader.copy(alpha = 0.85f))
                    .border(1.dp, StudioBorder, CircleShape)
                    .testTag("toggle_fullscreen_preview")
            ) {
                Icon(
                    imageVector = if (isFullscreenPreview) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    contentDescription = if (isFullscreenPreview) "Exit Fullscreen Preview" else "Clean Program Preview",
                    tint = MintAccentBright,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Selected Source Quick-Action Transform Toolbar
        if (selectedSource != null && !isFullscreenPreview) {
            Spacer(Modifier.height(4.dp))
            SelectedSourceQuickToolbar(
                source = selectedSource,
                orientation = editingOrientation,
                onCenterBoth = { onApplyTransformPreset(selectedSource, editingOrientation, TransformPreset.CENTER_BOTH) },
                onFitScreen = { onApplyTransformPreset(selectedSource, editingOrientation, TransformPreset.FIT_SCREEN) },
                onFlipCamera = { onToggleCameraFacing(selectedSource) },
                onToggleLock = { onToggleSourceLock(selectedSource) },
                onOpenProperties = { onEditSourceProperties(selectedSource) },
                onDelete = { onDeleteSource(selectedSource.id) }
            )
        }
    }
}

@Composable
private fun SelectedSourceQuickToolbar(
    source: StudioSource,
    orientation: OutputOrientation,
    onCenterBoth: () -> Unit,
    onFitScreen: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenProperties: () -> Unit,
    onDelete: () -> Unit
) {
    val rect = if (orientation == OutputOrientation.HORIZONTAL) source.hRect else source.vRect
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(StudioHeader)
            .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${source.name} (${(rect.x * 100).toInt()}%,${(rect.y * 100).toInt()}% • ${(rect.width * 100).toInt()}×${(rect.height * 100).toInt()}%)",
            color = MintAccentBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Spacer(Modifier.width(4.dp))
        QuickActionChip(Icons.Rounded.CenterFocusStrong, "Center", enabled = !source.isLocked, onClick = onCenterBoth)
        QuickActionChip(Icons.Rounded.AspectRatio, "Fit Full", enabled = !source.isLocked, onClick = onFitScreen)
        if (source.type == SourceType.CAMERA) {
            QuickActionChip(Icons.Rounded.Cameraswitch, "Flip Cam", onClick = onFlipCamera)
        }
        QuickActionChip(
            icon = if (source.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            label = if (source.isLocked) "Unlock" else "Lock",
            highlight = source.isLocked,
            onClick = onToggleLock
        )
        QuickActionChip(Icons.Rounded.Settings, "Props", onClick = onOpenProperties)
        QuickActionChip(Icons.Rounded.DeleteOutline, "Remove", highlight = true, highlightColor = LiveCrimsonBright, onClick = onDelete)
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    highlight: Boolean = false,
    highlightColor: Color = MintAccentBright,
    onClick: () -> Unit
) {
    Surface(
        color = if (highlight) highlightColor.copy(alpha = 0.16f) else StudioElevated,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .border(1.dp, if (highlight) highlightColor else StudioBorder, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled) { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(icon, contentDescription = label, tint = if (highlight) highlightColor else TextPrimary, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = if (highlight) highlightColor else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CanvasHeaderLabel(
    title: String,
    accent: Color,
    isSelected: Boolean,
    icon: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) StudioHeader else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (isSelected) accent else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun InteractiveStudioCanvas(
    orientation: OutputOrientation,
    scene: StudioScene?,
    sources: List<StudioSource>,
    selectedSourceId: Long?,
    activeAlert: LiveAlertEvent?,
    donationGoal: DonationGoalState,
    recentChat: List<StreamChatMessage>,
    pinnedChatMessage: StreamChatMessage?,
    liveElapsedSeconds: Long,
    currentFps: Float,
    hasCameraPermission: Boolean,
    isScreenCaptureActive: Boolean,
    privacyShieldActive: Boolean,
    screenBitmap: Bitmap?,
    cameraBitmap: Bitmap?,
    getVideoOverlayBitmap: (String?) -> Bitmap?,
    isInteractive: Boolean,
    isPrimaryCameraHost: Boolean,
    onSelectSource: (Long?) -> Unit,
    onMoveSource: (Long, Float, Float) -> Unit,
    onResizeSource: (Long, Float, Float) -> Unit,
    onCommitTransform: (Long) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = if (orientation == OutputOrientation.HORIZONTAL) 16f / 9f else 9f / 16f
    val borderColor = if (selectedSourceId != null) MintAccent else StudioBorder

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val outerW = maxWidth
        val outerH = maxHeight.coerceAtLeast(1.dp)
        val outerRatio = outerW / outerH
        val sizedModifier = if (outerRatio > ratio) {
            Modifier.fillMaxHeight().aspectRatio(ratio, matchHeightConstraintsFirst = true)
        } else {
            Modifier.fillMaxWidth().aspectRatio(ratio, matchHeightConstraintsFirst = false)
        }

        BoxWithConstraints(
            modifier = sizedModifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(scene?.backdropHex ?: 0xFF0C1520))
                .border(1.5.dp, borderColor, RoundedCornerShape(6.dp))
                .clickable(enabled = isInteractive) { onSelectSource(null) }
        ) {
            val density = LocalDensity.current
            val canvasWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val canvasHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

            val selectedSrc = remember(sources, selectedSourceId) {
                sources.firstOrNull { it.id == selectedSourceId }
            }
            val selectedRect = selectedSrc?.let {
                if (orientation == OutputOrientation.HORIZONTAL) it.hRect else it.vRect
            }
            val showCenterGuideX = selectedRect != null && abs((selectedRect.x + selectedRect.width * 0.5f) - 0.5f) < 0.018f
            val showCenterGuideY = selectedRect != null && abs((selectedRect.y + selectedRect.height * 0.5f) - 0.5f) < 0.018f

            // Studio calibration grid + magnetic center snap lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridStep = size.width / 12f
                var x = gridStep
                while (x < size.width) {
                    drawLine(Color(0x1431C3A2), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += gridStep
                }
                var y = gridStep
                while (y < size.height) {
                    drawLine(Color(0x1431C3A2), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += gridStep
                }
                if (showCenterGuideX) {
                    drawLine(
                        color = LiveCrimsonBright.copy(alpha = 0.85f),
                        start = Offset(size.width * 0.5f, 0f),
                        end = Offset(size.width * 0.5f, size.height),
                        strokeWidth = 2f
                    )
                }
                if (showCenterGuideY) {
                    drawLine(
                        color = LiveCrimsonBright.copy(alpha = 0.85f),
                        start = Offset(0f, size.height * 0.5f),
                        end = Offset(size.width, size.height * 0.5f),
                        strokeWidth = 2f
                    )
                }
            }

            val folderVisibility = remember(sources, orientation) {
                sources.filter { it.type == SourceType.FOLDER }.associate {
                    it.id to if (orientation == OutputOrientation.HORIZONTAL) it.isVisibleHorizontal else it.isVisibleVertical
                }
            }

            val ordered = remember(sources) { sources.sortedByDescending { it.sortOrder } }

            ordered.forEach { src ->
                if (src.type == SourceType.FOLDER) return@forEach
                val visible = if (orientation == OutputOrientation.HORIZONTAL) src.isVisibleHorizontal else src.isVisibleVertical
                if (!visible) return@forEach
                if (src.parentFolderId != null && folderVisibility[src.parentFolderId] == false) return@forEach

                val rect = if (orientation == OutputOrientation.HORIZONTAL) src.hRect else src.vRect
                val leftPx = (rect.x * canvasWidthPx).roundToInt()
                val topPx = (rect.y * canvasHeightPx).roundToInt()
                val boxW = with(density) { (rect.width * canvasWidthPx).toDp() }.coerceAtLeast(24.dp)
                val boxH = with(density) { (rect.height * canvasHeightPx).toDp() }.coerceAtLeast(20.dp)
                val isSelected = selectedSourceId == src.id

                val dragModifier = if (isInteractive) {
                    Modifier
                        .offset { IntOffset(leftPx, topPx) }
                        .size(boxW, boxH)
                        .pointerInput(src.id, src.isLocked, orientation, canvasWidthPx, canvasHeightPx) {
                            detectDragGestures(
                                onDragStart = { onSelectSource(src.id) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (!src.isLocked) {
                                        onMoveSource(src.id, dragAmount.x / canvasWidthPx, dragAmount.y / canvasHeightPx)
                                    }
                                },
                                onDragEnd = { onCommitTransform(src.id) },
                                onDragCancel = { onCommitTransform(src.id) }
                            )
                        }
                        .clickable { onSelectSource(src.id) }
                } else {
                    Modifier
                        .offset { IntOffset(leftPx, topPx) }
                        .size(boxW, boxH)
                }

                Box(modifier = dragModifier.alpha(src.opacity.coerceIn(0.1f, 1f))) {
                    SourceLayerRenderer(
                        source = src,
                        activeAlert = activeAlert,
                        donationGoal = donationGoal,
                        recentChat = recentChat,
                        pinnedChatMessage = pinnedChatMessage,
                        liveElapsedSeconds = liveElapsedSeconds,
                        currentFps = currentFps,
                        hasCameraPermission = hasCameraPermission,
                        isPrimaryCameraHost = isPrimaryCameraHost,
                        isScreenCaptureActive = isScreenCaptureActive,
                        privacyShieldActive = privacyShieldActive,
                        screenBitmap = screenBitmap,
                        cameraBitmap = cameraBitmap,
                        videoOverlayBitmap = if (src.type == SourceType.VIDEO_OVERLAY) getVideoOverlayBitmap(src.imageUri) else null,
                        onRequestCameraPermission = onRequestCameraPermission,
                        onRequestScreenCapture = onRequestScreenCapture
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, LiveCrimsonBright, RoundedCornerShape(4.dp))
                        ) {
                            Surface(
                                color = LiveCrimsonBright,
                                shape = RoundedCornerShape(bottomEnd = 4.dp),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    if (src.isLocked) {
                                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                        Spacer(Modifier.width(2.dp))
                                    }
                                    Text(
                                        src.name + if (src.chromaKeyEnabled) " [CK]" else "",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                            // 48dp accessible touch target around the visible corner resize handle
                            if (!src.isLocked && isInteractive) {
                                Box(
                                    contentAlignment = Alignment.BottomEnd,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(48.dp)
                                        .pointerInput(src.id, orientation, canvasWidthPx, canvasHeightPx) {
                                            detectDragGestures(
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    onResizeSource(
                                                        src.id,
                                                        dragAmount.x / canvasWidthPx,
                                                        dragAmount.y / canvasHeightPx
                                                    )
                                                },
                                                onDragEnd = { onCommitTransform(src.id) },
                                                onDragCancel = { onCommitTransform(src.id) }
                                            )
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(MintAccentBright)
                                            .border(1.5.dp, StudioVoid, CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Rounded.OpenInFull,
                                            contentDescription = "Resize source",
                                            tint = StudioVoid,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceLayerRenderer(
    source: StudioSource,
    activeAlert: LiveAlertEvent?,
    donationGoal: DonationGoalState,
    recentChat: List<StreamChatMessage>,
    pinnedChatMessage: StreamChatMessage?,
    liveElapsedSeconds: Long,
    currentFps: Float,
    hasCameraPermission: Boolean,
    isPrimaryCameraHost: Boolean,
    isScreenCaptureActive: Boolean,
    privacyShieldActive: Boolean,
    screenBitmap: Bitmap?,
    cameraBitmap: Bitmap?,
    videoOverlayBitmap: Bitmap?,
    onRequestCameraPermission: () -> Unit,
    onRequestScreenCapture: () -> Unit
) {
    val accent = Color(source.accentColorHex)
    val shape = RoundedCornerShape(source.cornerRadiusDp.dp)
    val scaleMode = if (source.cropToFill) ContentScale.Crop else ContentScale.Fit

    when (source.type) {
        SourceType.SCREEN_CAPTURE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0xFF0B131D)),
                contentAlignment = Alignment.Center
            ) {
                if (privacyShieldActive) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0E141E))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = "Privacy Shield Active", tint = RecordAmber, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("PRIVACY SHIELD ACTIVE", color = RecordAmber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Screen capture hidden from viewers", color = TextSecondary, fontSize = 10.sp)
                    }
                } else if (isScreenCaptureActive && screenBitmap != null && !screenBitmap.isRecycled) {
                    Image(
                        bitmap = screenBitmap.asImageBitmap(),
                        contentDescription = "Live screen capture",
                        contentScale = scaleMode,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0E1824), Color(0xFF132233), Color(0xFF0C141E))
                                )
                            )
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CastConnected,
                            contentDescription = "Screen capture standby",
                            tint = MintAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isScreenCaptureActive) "Capturing Display..." else "Screen / Game Capture Ready",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
                        if (!isScreenCaptureActive) {
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                color = MintAccent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .border(1.dp, MintAccent, RoundedCornerShape(6.dp))
                                    .clickable { onRequestScreenCapture() }
                            ) {
                                Text(
                                    text = "Tap to Capture Screen",
                                    color = MintAccentBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        SourceType.CAMERA -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(if (source.chromaKeyEnabled) Color(source.chromaKeyColorHex).copy(alpha = 0.18f) else Color(0xFF171326)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    if (isPrimaryCameraHost) {
                        CameraXPreviewBox(
                            lensFacingFront = source.cameraFacingFront,
                            mirrorFront = source.cameraMirror,
                            zoomRatio = source.cameraZoom,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (cameraBitmap != null && !cameraBitmap.isRecycled) {
                        Image(
                            bitmap = cameraBitmap.asImageBitmap(),
                            contentDescription = source.name,
                            contentScale = scaleMode,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = source.cameraZoom.coerceIn(1f, 4f)
                                    scaleY = source.cameraZoom.coerceIn(1f, 4f)
                                }
                        )
                    } else {
                        Text("Mirroring Camera…", color = TextSecondary, fontSize = 10.sp)
                    }
                    if (source.chromaKeyEnabled) {
                        Surface(
                            color = StudioVoid.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                        ) {
                            Text(
                                "CHROMA LIVE",
                                color = MintAccentBright,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onRequestCameraPermission() }
                            .padding(6.dp)
                    ) {
                        Icon(Icons.Rounded.Videocam, contentDescription = "Enable camera", tint = MintAccent, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Enable Cam",
                            color = MintAccentBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        SourceType.WEBCAM_FRAME -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(listOf(accent, MintAccentBright, accent)),
                        shape = shape
                    )
            )
        }

        SourceType.COLOR_SOURCE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(accent)
            )
        }

        SourceType.IMAGE_OVERLAY -> {
            if (!source.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = source.imageUri.toUri(),
                    contentDescription = source.name,
                    contentScale = scaleMode,
                    modifier = Modifier.fillMaxSize().clip(shape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(StudioDock.copy(alpha = 0.8f))
                        .border(1.dp, StudioBorder, shape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(source.name, color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }

        SourceType.VIDEO_OVERLAY -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0xFF121C28)),
                contentAlignment = Alignment.Center
            ) {
                if (videoOverlayBitmap != null && !videoOverlayBitmap.isRecycled) {
                    Image(
                        bitmap = videoOverlayBitmap.asImageBitmap(),
                        contentDescription = source.name,
                        contentScale = scaleMode,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
                        Icon(Icons.Rounded.Movie, contentDescription = null, tint = MintAccentBright, modifier = Modifier.size(20.dp))
                        Text(source.name, color = TextSecondary, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }

        SourceType.TEXT_BANNER -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0xCC0C1219))
                    .border(1.5.dp, accent.copy(alpha = 0.8f), shape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = source.textContent.ifBlank { source.name },
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        SourceType.STREAM_TIMER -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0xD40D131B))
                    .border(1.5.dp, accent, shape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (liveElapsedSeconds > 0L) LiveCrimsonBright else accent)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${source.textContent.ifBlank { if (liveElapsedSeconds > 0L) "LIVE" else "UPTIME" }}  ${formatDuration(liveElapsedSeconds)}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = MintAccentBright, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${currentFps.toInt()} FPS",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        SourceType.ALERTBOX -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedVisibility(
                    visible = activeAlert != null,
                    enter = fadeIn() + scaleIn(initialScale = 0.88f),
                    exit = fadeOut() + scaleOut(targetScale = 0.92f)
                ) {
                    activeAlert?.let { alert ->
                        val alertColor = Color(alert.accentHex)
                        Surface(
                            color = Color(0xEE111822),
                            shape = shape,
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, alertColor, shape)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = alertColor, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        alert.title,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        alert.subtitle,
                                        color = alertColor,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SourceType.DONATION_GOAL -> {
            val pct = (donationGoal.currentAmount / donationGoal.targetAmount.coerceAtLeast(1f)).coerceIn(0f, 1f)
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0xD90E151E))
                    .border(1.dp, accent.copy(alpha = 0.5f), shape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        donationGoal.title,
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "$${donationGoal.currentAmount.toInt()}/$${donationGoal.targetAmount.toInt()}",
                        color = MintAccentBright,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(pct)
                            .background(Brush.horizontalGradient(listOf(accent, MintAccentBright)))
                    )
                }
            }
        }

        SourceType.CHATBOX -> {
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0xAD0A0F15))
                    .border(1.dp, Color(0x33FFFFFF), shape)
                    .padding(6.dp)
            ) {
                if (pinnedChatMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MintAccent.copy(alpha = 0.22f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Rounded.PushPin, contentDescription = null, tint = MintAccentBright, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${pinnedChatMessage.username}: ${pinnedChatMessage.message}",
                            color = MintAccentBright,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                }
                recentChat.takeLast(5).forEach { msg ->
                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                        Text(
                            text = "${msg.username}: ",
                            color = Color(msg.nameColorHex),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = msg.message,
                            color = TextPrimary,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        SourceType.FOLDER -> Unit
    }
}

@Composable
private fun CameraXPreviewBox(
    lensFacingFront: Boolean,
    mirrorFront: Boolean,
    zoomRatio: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(boundCamera, zoomRatio) {
        runCatching {
            boundCamera?.cameraControl?.setZoomRatio(zoomRatio.coerceIn(1f, 4f))
        }
    }

    DisposableEffect(lensFacingFront, mirrorFront, previewView) {
        val view = previewView ?: return@DisposableEffect onDispose {}
        var isDisposed = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            if (isDisposed) return@Runnable
            runCatching {
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = view.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { analyzer ->
                        analyzer.setAnalyzer(cameraExecutor) { image: ImageProxy ->
                            runCatching {
                                val raw = image.toBitmap()
                                val rotation = image.imageInfo.rotationDegrees.toFloat()
                                val shouldMirror = lensFacingFront && mirrorFront
                                val finalBmp = if (rotation != 0f || shouldMirror) {
                                    val m = Matrix().apply {
                                        if (rotation != 0f) postRotate(rotation)
                                        if (shouldMirror) postScale(-1f, 1f)
                                    }
                                    Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true).also {
                                        if (it !== raw) raw.recycle()
                                    }
                                } else {
                                    raw
                                }
                                MediaProjectionHolder.updateCameraBitmap(finalBmp)
                            }
                            image.close()
                        }
                    }

                val selector = if (lensFacingFront) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                provider.unbindAll()
                val cam = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                boundCamera = cam
                cam.cameraControl.setZoomRatio(zoomRatio.coerceIn(1f, 4f))
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            isDisposed = true
            boundCamera = null
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }.also { previewView = it }
        },
        modifier = modifier
    )
}
