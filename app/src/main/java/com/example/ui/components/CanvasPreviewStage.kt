package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDock
import com.example.ui.theme.StudioHeader
import com.example.ui.theme.StudioVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun CanvasPreviewStage(
    horizontalEnabled: Boolean,
    verticalEnabled: Boolean,
    studioModeEnabled: Boolean,
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
    hasCameraPermission: Boolean,
    isScreenCaptureActive: Boolean,
    screenBitmap: Bitmap?,
    onSelectOrientation: (OutputOrientation) -> Unit,
    onSelectSource: (Long?) -> Unit,
    onMoveSource: (StudioSource, OutputOrientation, Float, Float) -> Unit,
    onTransitionToProgram: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioVoid)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (studioModeEnabled) {
            // OBS Studio Mode: Side-by-side Preview (Left) -> Transition Button -> Program (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                        hasCameraPermission = hasCameraPermission,
                        isScreenCaptureActive = isScreenCaptureActive,
                        screenBitmap = screenBitmap,
                        isInteractive = true,
                        onSelectSource = onSelectSource,
                        onMoveSource = { src, dx, dy -> onMoveSource(src, editingOrientation, dx, dy) },
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
                    InteractiveStudioCanvas(
                        orientation = editingOrientation,
                        scene = programScene,
                        sources = programSources,
                        selectedSourceId = null,
                        activeAlert = activeAlert,
                        donationGoal = donationGoal,
                        recentChat = recentChat,
                        hasCameraPermission = hasCameraPermission,
                        isScreenCaptureActive = isScreenCaptureActive,
                        screenBitmap = screenBitmap,
                        isInteractive = false,
                        onSelectSource = {},
                        onMoveSource = { _, _, _ -> },
                        onRequestCameraPermission = onRequestCameraPermission,
                        onRequestScreenCapture = onRequestScreenCapture,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Dual Output (Horizontal 16:9 + Vertical 9:16) matching screenshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (horizontalEnabled) {
                    Column(
                        modifier = Modifier
                            .weight(if (verticalEnabled) 1.85f else 1f)
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
                        Spacer(Modifier.height(4.dp))
                        InteractiveStudioCanvas(
                            orientation = OutputOrientation.HORIZONTAL,
                            scene = programScene,
                            sources = programSources,
                            selectedSourceId = if (editingOrientation == OutputOrientation.HORIZONTAL) selectedSourceId else null,
                            activeAlert = activeAlert,
                            donationGoal = donationGoal,
                            recentChat = recentChat,
                            hasCameraPermission = hasCameraPermission,
                            isScreenCaptureActive = isScreenCaptureActive,
                            screenBitmap = screenBitmap,
                            isInteractive = true,
                            onSelectSource = {
                                onSelectOrientation(OutputOrientation.HORIZONTAL)
                                onSelectSource(it)
                            },
                            onMoveSource = { src, dx, dy ->
                                onSelectOrientation(OutputOrientation.HORIZONTAL)
                                onMoveSource(src, OutputOrientation.HORIZONTAL, dx, dy)
                            },
                            onRequestCameraPermission = onRequestCameraPermission,
                            onRequestScreenCapture = onRequestScreenCapture,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (verticalEnabled) {
                    Column(
                        modifier = Modifier
                            .weight(if (horizontalEnabled) 0.9f else 1f)
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
                        Spacer(Modifier.height(4.dp))
                        InteractiveStudioCanvas(
                            orientation = OutputOrientation.VERTICAL,
                            scene = programScene,
                            sources = programSources,
                            selectedSourceId = if (editingOrientation == OutputOrientation.VERTICAL) selectedSourceId else null,
                            activeAlert = activeAlert,
                            donationGoal = donationGoal,
                            recentChat = recentChat,
                            hasCameraPermission = hasCameraPermission,
                            isScreenCaptureActive = isScreenCaptureActive,
                            screenBitmap = screenBitmap,
                            isInteractive = true,
                            onSelectSource = {
                                onSelectOrientation(OutputOrientation.VERTICAL)
                                onSelectSource(it)
                            },
                            onMoveSource = { src, dx, dy ->
                                onSelectOrientation(OutputOrientation.VERTICAL)
                                onMoveSource(src, OutputOrientation.VERTICAL, dx, dy)
                            },
                            onRequestCameraPermission = onRequestCameraPermission,
                            onRequestScreenCapture = onRequestScreenCapture,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
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
    hasCameraPermission: Boolean,
    isScreenCaptureActive: Boolean,
    screenBitmap: Bitmap?,
    isInteractive: Boolean,
    onSelectSource: (Long?) -> Unit,
    onMoveSource: (StudioSource, Float, Float) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = if (orientation == OutputOrientation.HORIZONTAL) 16f / 9f else 9f / 16f
    val borderColor = if (selectedSourceId != null) MintAccent else StudioBorder

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(scene?.backdropHex ?: 0xFF0C1520))
                .border(1.5.dp, borderColor, RoundedCornerShape(6.dp))
                .clickable(enabled = isInteractive) { onSelectSource(null) }
        ) {
            val density = LocalDensity.current
            val canvasWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val canvasHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

            // Studio calibration grid background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridStep = size.width / 12f
                var x = gridStep
                while (x < size.width) {
                    drawLine(
                        color = Color(0x1431C3A2),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += gridStep
                }
                var y = gridStep
                while (y < size.height) {
                    drawLine(
                        color = Color(0x1431C3A2),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += gridStep
                }
            }

            val folderVisibility = remember(sources, orientation) {
                sources.filter { it.type == SourceType.FOLDER }.associate {
                    it.id to if (orientation == OutputOrientation.HORIZONTAL) it.isVisibleHorizontal else it.isVisibleVertical
                }
            }

            // Render bottom-to-top
            val ordered = remember(sources) { sources.sortedByDescending { it.sortOrder } }

            ordered.forEach { src ->
                if (src.type == SourceType.FOLDER) return@forEach
                val visible = if (orientation == OutputOrientation.HORIZONTAL) src.isVisibleHorizontal else src.isVisibleVertical
                if (!visible) return@forEach
                if (src.parentFolderId != null && folderVisibility[src.parentFolderId] == false) return@forEach

                val rect = if (orientation == OutputOrientation.HORIZONTAL) src.hRect else src.vRect
                val leftPx = (rect.x * canvasWidthPx).roundToInt()
                val topPx = (rect.y * canvasHeightPx).roundToInt()
                val boxW = with(density) { (rect.width * canvasWidthPx).toDp() }.coerceAtLeast(20.dp)
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
                                        onMoveSource(src, dragAmount.x / canvasWidthPx, dragAmount.y / canvasHeightPx)
                                    }
                                }
                            )
                        }
                        .clickable { onSelectSource(src.id) }
                } else {
                    Modifier
                        .offset { IntOffset(leftPx, topPx) }
                        .size(boxW, boxH)
                }

                Box(modifier = dragModifier) {
                    SourceLayerRenderer(
                        source = src,
                        activeAlert = activeAlert,
                        donationGoal = donationGoal,
                        recentChat = recentChat,
                        hasCameraPermission = hasCameraPermission,
                        isScreenCaptureActive = isScreenCaptureActive,
                        screenBitmap = screenBitmap,
                        onRequestCameraPermission = onRequestCameraPermission,
                        onRequestScreenCapture = onRequestScreenCapture
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, LiveCrimsonBright, RoundedCornerShape(4.dp))
                        ) {
                            // Top-left source badge
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
                                        src.name,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                            // Corner handles
                            listOf(Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).forEach { cornerAlign ->
                                Box(
                                    modifier = Modifier
                                        .align(cornerAlign)
                                        .size(7.dp)
                                        .background(MintAccentBright, CircleShape)
                                )
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
    hasCameraPermission: Boolean,
    isScreenCaptureActive: Boolean,
    screenBitmap: Bitmap?,
    onRequestCameraPermission: () -> Unit,
    onRequestScreenCapture: () -> Unit
) {
    val accent = Color(source.accentColorHex)
    val shape = RoundedCornerShape(source.cornerRadiusDp.dp)

    when (source.type) {
        SourceType.SCREEN_CAPTURE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0xFF0B131D)),
                contentAlignment = Alignment.Center
            ) {
                if (isScreenCaptureActive && screenBitmap != null && !screenBitmap.isRecycled) {
                    Image(
                        bitmap = screenBitmap.asImageBitmap(),
                        contentDescription = "Live screen capture",
                        contentScale = ContentScale.Crop,
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
                    .background(Color(0xFF171326)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    CameraXPreviewBox(
                        lensFacingFront = source.cameraFacingFront,
                        modifier = Modifier.fillMaxSize()
                    )
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
                    .background(accent.copy(alpha = source.opacity))
            )
        }

        SourceType.IMAGE_OVERLAY -> {
            if (!source.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(source.imageUri),
                    contentDescription = source.name,
                    contentScale = ContentScale.Fit,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(lensFacingFront, previewView) {
        val view = previewView ?: return@DisposableEffect onDispose {}
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
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
                                val bmp = image.toBitmap()
                                MediaProjectionHolder.updateCameraBitmap(bmp)
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
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
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
