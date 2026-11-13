package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BroadcastTelemetry
import com.example.data.model.OutputOrientation
import com.example.data.model.StreamConnectionState
import com.example.data.model.StreamHealthLevel
import com.example.data.model.StudioPreferences
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
import java.util.Locale

@Composable
fun StudioNavigationRail(
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
    onOpenLogs: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
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
            RailIconButton(Icons.Rounded.Terminal, "Broadcast Diagnostics & Markers", false, onOpenLogs)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
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
fun StudioTopOutputBar(
    preferences: StudioPreferences,
    telemetry: BroadcastTelemetry,
    onToggleOrientation: (OutputOrientation) -> Unit,
    onOpenDualOutputManager: () -> Unit,
    onToggleStudioMode: () -> Unit,
    onTogglePrivacyShield: () -> Unit,
    onOpenLogs: () -> Unit
) {
    val healthColor = healthLevelColor(telemetry.healthLevel)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioHeader)
            .border(1.dp, StudioBorder)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("mobileLabs", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)

            Surface(
                color = StudioVoid,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .border(1.dp, StudioBorder, RoundedCornerShape(4.dp))
                    .clickable { onOpenLogs() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(healthColor))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (telemetry.connectionState == StreamConnectionState.OFFLINE) "STANDBY" else telemetry.healthLevel.label,
                        color = healthColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(color = MintAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                Text(
                    "${preferences.resolutionHeight}p${preferences.targetFps} • ${preferences.videoBitrateKbps}k",
                    color = MintAccentBright,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Surface(color = StudioElevated, shape = RoundedCornerShape(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = if (telemetry.batteryPercent > 20) Icons.Rounded.BatteryStd else Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = if (telemetry.batteryPercent <= 15) LiveCrimsonBright else MintAccentBright,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = String.format(Locale.US, "%d%% • %.1fGB", telemetry.batteryPercent, telemetry.freeStorageGb),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (telemetry.isThermalThrottling) {
                        Spacer(Modifier.width(4.dp))
                        Text("HOT", color = LiveCrimsonBright, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            OutputToggleChip(
                label = "Horizontal",
                icon = Icons.Rounded.DesktopWindows,
                active = preferences.horizontalEnabled,
                onClick = { onToggleOrientation(OutputOrientation.HORIZONTAL) }
            )
            OutputToggleChip(
                label = "Vertical",
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

        Spacer(Modifier.width(8.dp))
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
fun CompactTopHeader(
    preferences: StudioPreferences,
    telemetry: BroadcastTelemetry,
    onToggleOrientation: (OutputOrientation) -> Unit,
    onToggleDual: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val healthColor = when (telemetry.connectionState) {
        StreamConnectionState.LIVE -> healthLevelColor(telemetry.healthLevel)
        StreamConnectionState.RECONNECTING, StreamConnectionState.CONNECTING -> RecordAmber
        StreamConnectionState.ERROR -> LiveCrimsonBright
        else -> TextMuted
    }
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
                    .background(healthColor)
            )
            Spacer(Modifier.width(5.dp))
            Text("${telemetry.batteryPercent}%", color = TextMuted, fontSize = 10.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val isDual = preferences.horizontalEnabled && preferences.verticalEnabled
            CompactPill("16:9", preferences.horizontalEnabled && !isDual) { onToggleOrientation(OutputOrientation.HORIZONTAL) }
            CompactPill("9:16", preferences.verticalEnabled && !isDual) { onToggleOrientation(OutputOrientation.VERTICAL) }
            CompactPill("Dual", isDual) { onToggleDual() }
            IconButton(onClick = onOpenLogs, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Terminal, contentDescription = "Broadcast Logs", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
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
fun MobileDockTabStrip(
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
fun MobileStudioToolsGrid(
    studioModeEnabled: Boolean,
    isScreenCaptureActive: Boolean,
    privacyShieldActive: Boolean,
    destinationCount: Int,
    clipCount: Int,
    onToggleStudioMode: () -> Unit,
    onToggleScreenCapture: () -> Unit,
    onTogglePrivacyShield: () -> Unit,
    onToggleFullscreenPreview: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenClips: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenShortcuts: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioDock)
            .verticalScroll(rememberScrollState())
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
                title = "Diagnostics & Markers",
                subtitle = "Logs & F8 markers",
                icon = Icons.Rounded.Terminal,
                active = false,
                onClick = onOpenLogs,
                modifier = Modifier.weight(1f)
            )
            ToolActionCard(
                title = "JSON Backup",
                subtitle = "Export / Import scenes",
                icon = Icons.Rounded.SaveAlt,
                active = false,
                onClick = onOpenBackup,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolActionCard(
                title = "Fullscreen Stage",
                subtitle = "Inspect layout cleanly",
                icon = Icons.Rounded.Fullscreen,
                active = false,
                onClick = onToggleFullscreenPreview,
                modifier = Modifier.weight(1f)
            )
            ToolActionCard(
                title = "Hotkeys",
                subtitle = "Keyboard shortcuts",
                icon = Icons.Rounded.Keyboard,
                active = false,
                onClick = onOpenShortcuts,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolActionCard(
                title = "OBS Settings",
                subtitle = "Bitrate, FPS, Audio DSP",
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
fun BottomBroadcastControlBar(
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
    val healthColor = healthLevelColor(telemetry.healthLevel)

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(healthColor))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = String.format(Locale.US, "CPU %.1f%% • %.0f FPS • %d kbps", telemetry.cpuUsagePercent, telemetry.currentFps, telemetry.currentBitrateKbps),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(healthColor))
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
                        text = String.format(Locale.US, "%.1f FPS", telemetry.currentFps),
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
                        color = if (telemetry.droppedFrames > 0) RecordAmber else TextMuted
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
                        .size(40.dp)
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
                        .size(40.dp)
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
                    modifier = Modifier.height(40.dp).testTag("go_live_button")
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

private fun healthLevelColor(level: StreamHealthLevel): Color = when (level) {
    StreamHealthLevel.EXCELLENT -> MintAccentBright
    StreamHealthLevel.GOOD -> MintAccent
    StreamHealthLevel.FAIR -> RecordAmber
    StreamHealthLevel.POOR -> LiveCrimsonBright
    StreamHealthLevel.IDLE -> TextMuted
}
