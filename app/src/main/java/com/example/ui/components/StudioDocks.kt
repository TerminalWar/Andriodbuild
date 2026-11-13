package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SceneCollectionEntity
import com.example.data.model.AudioChannelConfig
import com.example.data.model.AudioMeterReading
import com.example.data.model.BroadcastTelemetry
import com.example.data.model.ChatPlatform
import com.example.data.model.OutputOrientation
import com.example.data.model.SourceType
import com.example.data.model.StreamChatMessage
import com.example.data.model.StreamConnectionState
import com.example.data.model.StudioScene
import com.example.data.model.StudioSource
import com.example.data.model.TransitionType
import com.example.streaming.ChatConnectionStatus
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
import com.example.ui.theme.VuGreen
import com.example.ui.theme.VuRed
import com.example.ui.theme.VuTrackBg
import com.example.ui.theme.VuYellow
import java.util.Locale

@Composable
fun ScenesDock(
    collections: List<SceneCollectionEntity>,
    scenes: List<StudioScene>,
    activeSceneId: Long?,
    selectedTransition: TransitionType,
    onSelectCollection: (Long) -> Unit,
    onAddCollectionClick: () -> Unit,
    onSelectScene: (Long) -> Unit,
    onAddSceneClick: () -> Unit,
    onRenameSceneClick: (StudioScene) -> Unit,
    onDuplicateScene: (StudioScene) -> Unit,
    onDeleteScene: (Long) -> Unit,
    onSelectTransition: (TransitionType) -> Unit,
    onDropMarker: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCollectionMenu by remember { mutableStateOf(false) }
    var showTransitionMenu by remember { mutableStateOf(false) }
    val activeCollectionName = collections.firstOrNull { it.isActive }?.name
        ?: collections.firstOrNull()?.name ?: "Gaming Scene"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDock)
            .border(1.dp, StudioBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioHeader)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showCollectionMenu = true }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = activeCollectionName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Switch scene collection", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showCollectionMenu, onDismissRequest = { showCollectionMenu = false }) {
                    collections.forEach { col ->
                        DropdownMenuItem(
                            text = { Text(col.name, color = if (col.isActive) MintAccent else TextPrimary) },
                            onClick = {
                                onSelectCollection(col.id)
                                showCollectionMenu = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("+ New Scene Collection", color = MintAccentBright) },
                        onClick = {
                            showCollectionMenu = false
                            onAddCollectionClick()
                        }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(
                        color = StudioElevated,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { showTransitionMenu = true }
                            .padding(end = 3.dp)
                    ) {
                        Text(
                            text = selectedTransition.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MintAccentBright,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    DropdownMenu(expanded = showTransitionMenu, onDismissRequest = { showTransitionMenu = false }) {
                        TransitionType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text("${t.label} (${t.durationMs}ms)") },
                                onClick = {
                                    onSelectTransition(t)
                                    showTransitionMenu = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDropMarker,
                    modifier = Modifier.size(30.dp).testTag("drop_marker_button")
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Drop Stream Marker (F8)", tint = RecordAmber, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onAddSceneClick,
                    modifier = Modifier.size(30.dp).testTag("add_scene_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add scene", tint = MintAccentBright, modifier = Modifier.size(18.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(scenes, key = { _, s -> s.id }) { idx, scene ->
                val isSelected = scene.id == activeSceneId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) StudioElevated else Color.Transparent)
                        .clickable { onSelectScene(scene.id) }
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                        .testTag("scene_item_${scene.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(if (isSelected) MintAccent else Color.Transparent, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = scene.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (idx < 7) {
                            Text(
                                text = "F${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted.copy(alpha = 0.7f)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Rename scene",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { onRenameSceneClick(scene) }
                            )
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = "Duplicate scene",
                                tint = MintAccentBright,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { onDuplicateScene(scene) }
                            )
                            if (scenes.size > 1) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete scene",
                                    tint = LiveCrimsonBright,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable { onDeleteScene(scene.id) }
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
fun SourcesDock(
    sources: List<StudioSource>,
    selectedSourceId: Long?,
    editingOrientation: OutputOrientation,
    onSelectSource: (Long) -> Unit,
    onAddSourceClick: () -> Unit,
    onToggleVisibility: (StudioSource, OutputOrientation) -> Unit,
    onToggleLock: (StudioSource) -> Unit,
    onDuplicateSource: (StudioSource) -> Unit,
    onMoveSourceOrder: (Long, Boolean) -> Unit,
    onEditSourceProperties: (StudioSource) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDock)
            .border(1.dp, StudioBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioHeader)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sources", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.width(6.dp))
                Surface(color = StudioElevated, shape = RoundedCornerShape(4.dp)) {
                    Text(
                        text = editingOrientation.aspectLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MintAccent,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val selected = sources.firstOrNull { it.id == selectedSourceId }
                if (selected != null) {
                    IconButton(onClick = { onMoveSourceOrder(selected.id, true) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move source up", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onMoveSourceOrder(selected.id, false) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move source down", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onDuplicateSource(selected) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicate source", tint = TextSecondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { onEditSourceProperties(selected) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Source properties", tint = MintAccentBright, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(
                    onClick = onAddSourceClick,
                    modifier = Modifier.size(30.dp).testTag("add_source_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add source", tint = MintAccentBright, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (sources.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tap + to add Camera, Screen Capture, or Video Overlays", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(sources, key = { it.id }) { src ->
                    val isSelected = src.id == selectedSourceId
                    val isVisible = if (editingOrientation == OutputOrientation.HORIZONTAL) {
                        src.isVisibleHorizontal
                    } else {
                        src.isVisibleVertical
                    }
                    val indent = if (src.parentFolderId != null) 16.dp else 0.dp

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) StudioElevated else Color.Transparent)
                            .clickable { onSelectSource(src.id) }
                            .padding(start = 10.dp + indent, end = 6.dp, top = 5.dp, bottom = 5.dp)
                            .testTag("source_item_${src.id}")
                    ) {
                        Icon(
                            imageVector = sourceTypeIcon(src.type),
                            contentDescription = null,
                            tint = if (isSelected) MintAccent else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = src.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isVisible) TextPrimary else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (src.chromaKeyEnabled) {
                            Surface(color = MintAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp)) {
                                Text("CK", color = MintAccentBright, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        Icon(
                            imageVector = if (editingOrientation == OutputOrientation.HORIZONTAL) Icons.Rounded.DesktopWindows else Icons.Rounded.Smartphone,
                            contentDescription = null,
                            tint = if (isVisible) MintAccent.copy(alpha = 0.7f) else TextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { onToggleVisibility(src, editingOrientation) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = "Toggle source visibility",
                                tint = if (isVisible) TextSecondary else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { onToggleLock(src) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (src.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = "Toggle source lock",
                                tint = if (src.isLocked) LiveCrimsonBright else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioMixerDock(
    channels: List<AudioChannelConfig>,
    readings: Map<String, AudioMeterReading>,
    hasAudioPermission: Boolean,
    audioSampleRateHz: Int,
    onRequestAudioPermission: () -> Unit,
    onGainChange: (AudioChannelConfig, Float) -> Unit,
    onToggleMute: (AudioChannelConfig) -> Unit,
    onOpenChannelSettings: (AudioChannelConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDock)
            .border(1.dp, StudioBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioHeader)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("Mixer", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            if (!hasAudioPermission) {
                Surface(
                    color = LiveCrimsonBright.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .border(1.dp, LiveCrimsonBright, RoundedCornerShape(4.dp))
                        .clickable { onRequestAudioPermission() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Rounded.Mic, contentDescription = null, tint = LiveCrimsonBright, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Grant Mic", color = LiveCrimsonBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val khz = String.format(Locale.US, "%.1f kHz", audioSampleRateHz / 1000f)
                Text("$khz AAC", style = MaterialTheme.typography.labelSmall, color = MintAccent)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(channels, key = { it.id }) { ch ->
                val meter = readings[ch.id]
                val dbText = if (ch.isMuted) "MUTED" else String.format(Locale.US, "%+.1f dB", ch.gainDb)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioVoid.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = ch.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (ch.noiseGateEnabled) {
                                Spacer(Modifier.width(4.dp))
                                SmallBadgePill("GATE", MintAccent)
                            }
                            if (ch.monitorEnabled) {
                                Spacer(Modifier.width(4.dp))
                                SmallBadgePill("MON", Color(0xFF38BDF8))
                            }
                            if (ch.syncOffsetMs != 0) {
                                Spacer(Modifier.width(4.dp))
                                SmallBadgePill("${ch.syncOffsetMs}ms", RecordAmber)
                            }
                            if (meter?.isDucking == true) {
                                Spacer(Modifier.width(4.dp))
                                SmallBadgePill("DUCK", RecordAmber)
                            }
                            if (meter?.isClipping == true) {
                                Spacer(Modifier.width(4.dp))
                                SmallBadgePill("CLIP", LiveCrimsonBright)
                            }
                        }
                        Text(
                            text = dbText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ch.isMuted) LiveCrimsonBright else TextSecondary
                        )
                    }

                    Spacer(Modifier.height(5.dp))

                    AudioVuMeterBar(
                        rmsDb = if (ch.isMuted) -60f else (meter?.rmsDb ?: -60f),
                        peakHoldDb = if (ch.isMuted) -60f else (meter?.peakHoldDb ?: meter?.peakDb ?: -60f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Slider(
                            value = ch.gainDb,
                            onValueChange = { onGainChange(ch, it) },
                            valueRange = -48f..12f,
                            colors = SliderDefaults.colors(
                                thumbColor = TextPrimary,
                                activeTrackColor = if (ch.isMuted) TextMuted else MintAccent,
                                inactiveTrackColor = StudioElevated
                            ),
                            modifier = Modifier.weight(1f).height(28.dp)
                        )
                        IconButton(
                            onClick = { onToggleMute(ch) },
                            modifier = Modifier.size(32.dp).testTag("mute_${ch.id}")
                        ) {
                            Icon(
                                imageVector = if (ch.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Mute ${ch.name}",
                                tint = if (ch.isMuted) LiveCrimsonBright else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onOpenChannelSettings(ch) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Audio settings for ${ch.name}",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallBadgePill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(3.dp)) {
        Text(
            text = text,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun AudioVuMeterBar(
    rmsDb: Float,
    peakHoldDb: Float,
    modifier: Modifier = Modifier
) {
    val rmsNorm = ((rmsDb + 60f) / 60f).coerceIn(0f, 1f)
    val peakNorm = ((peakHoldDb + 60f) / 60f).coerceIn(0f, 1f)

    Canvas(modifier = modifier.clip(RoundedCornerShape(3.dp)).background(VuTrackBg)) {
        val w = size.width
        val h = size.height

        val totalSegments = 32
        val gap = 2f
        val segW = (w - gap * (totalSegments - 1)) / totalSegments
        val activeSegments = (rmsNorm * totalSegments).toInt()
        val peakSeg = (peakNorm * totalSegments).toInt().coerceIn(0, totalSegments - 1)

        for (i in 0 until totalSegments) {
            val fraction = i.toFloat() / totalSegments
            val segColor = when {
                fraction < 0.70f -> VuGreen
                fraction < 0.90f -> VuYellow
                else -> VuRed
            }
            val isLit = i < activeSegments || (peakNorm > 0.03f && i == peakSeg)
            drawRoundRect(
                color = if (isLit) segColor else segColor.copy(alpha = 0.14f),
                topLeft = Offset(i * (segW + gap), 0f),
                size = Size(segW, h),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

@Composable
fun ChatDock(
    telemetry: BroadcastTelemetry,
    streamTitle: String,
    twitchChannel: String,
    chatStatus: ChatConnectionStatus,
    messages: List<StreamChatMessage>,
    pinnedMessage: StreamChatMessage?,
    onEditTitleClick: () -> Unit,
    onConnectTwitchChannel: (String) -> Unit,
    onSendLocalMessage: (String) -> Unit,
    onPinMessage: (StreamChatMessage?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var channelInput by remember(twitchChannel) { mutableStateOf(twitchChannel) }
    var chatInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDock)
            .border(1.dp, StudioBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioHeader)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(
                            when (telemetry.connectionState) {
                                StreamConnectionState.LIVE -> LiveCrimsonBright
                                StreamConnectionState.CONNECTING, StreamConnectionState.RECONNECTING -> RecordAmber
                                StreamConnectionState.ERROR -> Color(0xFFFF6B6B)
                                StreamConnectionState.OFFLINE -> TextMuted
                            }
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (telemetry.connectionState) {
                        StreamConnectionState.LIVE -> "Live ${formatDuration(telemetry.elapsedLiveSeconds)}"
                        StreamConnectionState.CONNECTING -> "Connecting..."
                        StreamConnectionState.RECONNECTING -> "Reconnecting..."
                        StreamConnectionState.ERROR -> "Error"
                        StreamConnectionState.OFFLINE -> "Offline"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
            }
            Text(
                text = "${telemetry.currentBitrateKbps} kbps",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditTitleClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = streamTitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.Edit, contentDescription = "Edit stream title", tint = TextMuted, modifier = Modifier.size(14.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder)
        ) {
            listOf("Twitch", "All Chat", "Activity").forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = idx }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (active) TextPrimary else TextMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(2.dp)
                            .background(if (active) MintAccent else Color.Transparent)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioVoid.copy(alpha = 0.7f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            BasicTextField(
                value = channelInput,
                onValueChange = { channelInput = it },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp),
                cursorBrush = SolidColor(MintAccent),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StudioElevated)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (channelInput.isEmpty()) {
                            Text("Twitch channel (e.g. shroud)", color = TextMuted, fontSize = 11.sp)
                        }
                        inner()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            Surface(
                color = if (chatStatus == ChatConnectionStatus.CONNECTED) MintAccent.copy(alpha = 0.2f) else MintAccent,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .clickable { onConnectTwitchChannel(channelInput) }
                    .testTag("connect_twitch_chat_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Connect Twitch IRC chat",
                        tint = if (chatStatus == ChatConnectionStatus.CONNECTED) MintAccentBright else StudioVoid,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = if (chatStatus == ChatConnectionStatus.CONNECTED) "Live" else "Join",
                        color = if (chatStatus == ChatConnectionStatus.CONNECTED) MintAccentBright else StudioVoid,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (pinnedMessage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MintAccent.copy(alpha = 0.14f))
                    .clickable { onPinMessage(null) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Icon(Icons.Rounded.PushPin, contentDescription = "Unpin message", tint = MintAccentBright, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Pinned: ${pinnedMessage.username}: ${pinnedMessage.message}",
                    color = MintAccentBright,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val filtered = remember(messages, selectedTab) {
            when (selectedTab) {
                0 -> messages.filter { it.platform == ChatPlatform.TWITCH || it.platform == ChatPlatform.SYSTEM }
                2 -> messages.filter { it.badge == "SUB" || it.badge == "HOST" || it.platform == ChatPlatform.SYSTEM }
                else -> messages
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(filtered, key = { it.id }) { msg ->
                val isPinned = pinnedMessage?.id == msg.id
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPinned) MintAccent.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onPinMessage(msg) }
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                ) {
                    Surface(
                        color = Color(msg.platform.colorHex).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.padding(top = 2.dp, end = 5.dp)
                    ) {
                        Text(
                            text = msg.badge ?: msg.platform.shortTag,
                            color = Color(msg.platform.colorHex),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(msg.nameColorHex), fontWeight = FontWeight.Bold)) {
                                append("${msg.username}: ")
                            }
                            withStyle(SpanStyle(color = TextPrimary)) {
                                append(msg.message)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioHeader)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            BasicTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp),
                cursorBrush = SolidColor(MintAccent),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StudioVoid)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (chatInput.isEmpty()) {
                            Text("Send host pin / tap msg to pin...", color = TextMuted, fontSize = 11.sp)
                        }
                        inner()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (chatInput.isNotBlank()) {
                        onSendLocalMessage(chatInput)
                        chatInput = ""
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send chat note", tint = MintAccentBright, modifier = Modifier.size(16.dp))
            }
        }
    }
}

fun sourceTypeIcon(type: SourceType) = when (type) {
    SourceType.SCREEN_CAPTURE -> Icons.Rounded.DesktopWindows
    SourceType.CAMERA -> Icons.Rounded.Videocam
    SourceType.WEBCAM_FRAME -> Icons.Rounded.CropSquare
    SourceType.VIDEO_OVERLAY -> Icons.Rounded.Movie
    SourceType.IMAGE_OVERLAY -> Icons.Rounded.Image
    SourceType.TEXT_BANNER -> Icons.Rounded.TextFields
    SourceType.STREAM_TIMER -> Icons.Rounded.Timer
    SourceType.ALERTBOX -> Icons.Rounded.AutoAwesome
    SourceType.DONATION_GOAL -> Icons.AutoMirrored.Rounded.TrendingUp
    SourceType.CHATBOX -> Icons.Rounded.ChatBubbleOutline
    SourceType.COLOR_SOURCE -> Icons.Rounded.Palette
    SourceType.FOLDER -> Icons.Rounded.Folder
}

fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}
