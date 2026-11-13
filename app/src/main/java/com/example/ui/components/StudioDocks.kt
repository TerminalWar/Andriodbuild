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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.TrendingUp
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
    onDeleteScene: (Long) -> Unit,
    onSelectTransition: (TransitionType) -> Unit,
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
        // Dock Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioHeader)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box {
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
                // Transition picker
                Box {
                    Surface(
                        color = StudioElevated,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { showTransitionMenu = true }
                            .padding(end = 4.dp)
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
                    onClick = onAddSceneClick,
                    modifier = Modifier.size(32.dp).testTag("add_scene_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add scene", tint = MintAccentBright, modifier = Modifier.size(18.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(scenes, key = { it.id }) { scene ->
                val isSelected = scene.id == activeSceneId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) StudioElevated else Color.Transparent)
                        .clickable { onSelectScene(scene.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("scene_item_${scene.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(if (isSelected) MintAccent else Color.Transparent, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = scene.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (scenes.size > 1 && isSelected) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete scene",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onDeleteScene(scene.id) }
                        )
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
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sources", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = StudioElevated,
                    shape = RoundedCornerShape(4.dp)
                ) {
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
                    IconButton(onClick = { onEditSourceProperties(selected) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Source properties", tint = MintAccentBright, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(
                    onClick = onAddSourceClick,
                    modifier = Modifier.size(32.dp).testTag("add_source_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add source", tint = MintAccentBright, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (sources.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tap + to add Camera, Screen Capture, or Overlays", color = TextMuted, style = MaterialTheme.typography.bodySmall)
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
                    val indent = if (src.parentFolderId != null) 18.dp else 0.dp

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) StudioElevated else Color.Transparent)
                            .clickable { onSelectSource(src.id) }
                            .padding(start = 10.dp + indent, end = 6.dp, top = 6.dp, bottom = 6.dp)
                            .testTag("source_item_${src.id}")
                    ) {
                        Icon(
                            imageVector = sourceTypeIcon(src.type),
                            contentDescription = null,
                            tint = if (isSelected) MintAccent else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = src.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isVisible) TextPrimary else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Horizontal / Vertical independent visibility badge + eye
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
                Text("44.1 kHz Stereo", style = MaterialTheme.typography.labelSmall, color = MintAccent)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ch.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            if (ch.noiseGateEnabled) {
                                Spacer(Modifier.width(6.dp))
                                Surface(color = MintAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp)) {
                                    Text("GATE", color = MintAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }
                        Text(
                            text = dbText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ch.isMuted) LiveCrimsonBright else TextSecondary
                        )
                    }

                    Spacer(Modifier.height(5.dp))

                    // Authentic Segmented VU Meter Bar (-60 to 0 dBFS)
                    AudioVuMeterBar(
                        rmsDb = if (ch.isMuted) -60f else (meter?.rmsDb ?: -60f),
                        peakDb = if (ch.isMuted) -60f else (meter?.peakDb ?: -60f),
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
private fun AudioVuMeterBar(
    rmsDb: Float,
    peakDb: Float,
    modifier: Modifier = Modifier
) {
    val rmsNorm = ((rmsDb + 60f) / 60f).coerceIn(0f, 1f)
    val peakNorm = ((peakDb + 60f) / 60f).coerceIn(0f, 1f)

    Canvas(modifier = modifier.clip(RoundedCornerShape(3.dp)).background(VuTrackBg)) {
        val w = size.width
        val h = size.height

        // Draw segmented LED blocks like OBS/Streamlabs Mixer
        val totalSegments = 30
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
    onEditTitleClick: () -> Unit,
    onConnectTwitchChannel: (String) -> Unit,
    onSendLocalMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Twitch, 1 = Multistream
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
        // Top Live Status Header matching screenshot
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
                                StreamConnectionState.CONNECTING -> Color(0xFFF59E0B)
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

        // Stream Title Row
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

        // Platform Tabs: Twitch / Multistream
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder)
        ) {
            listOf("Twitch", "Multistream").forEachIndexed { idx, label ->
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

        // Live Twitch IRC Channel Connect Bar
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

        // Messages Feed
        val filtered = remember(messages, selectedTab) {
            if (selectedTab == 0) {
                messages.filter { it.platform == ChatPlatform.TWITCH || it.platform == ChatPlatform.SYSTEM }
            } else {
                messages
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
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
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
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Quick Host Note / Chatbox Test Input
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
                            Text("Send host pin / test chat...", color = TextMuted, fontSize = 11.sp)
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
    SourceType.IMAGE_OVERLAY -> Icons.Rounded.Image
    SourceType.TEXT_BANNER -> Icons.Rounded.TextFields
    SourceType.ALERTBOX -> Icons.Rounded.AutoAwesome
    SourceType.DONATION_GOAL -> Icons.Rounded.TrendingUp
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
