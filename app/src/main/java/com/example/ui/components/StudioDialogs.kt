package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AudioChannelConfig
import com.example.data.model.DestinationHealthState
import com.example.data.model.EncoderProfile
import com.example.data.model.NormalizedRect
import com.example.data.model.OutputOrientation
import com.example.data.model.OverlayThemePreset
import com.example.data.model.RateControlMode
import com.example.data.model.RecordingClip
import com.example.data.model.SourceType
import com.example.data.model.StreamDestination
import com.example.data.model.StreamPlatform
import com.example.data.model.StudioPreferences
import com.example.data.model.StudioSource
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
import java.util.Locale

private val StudioSwatchColors = listOf(
    0xFF31C3A2L,
    0xFF56E3C2L,
    0xFF9146FFL,
    0xFFFF4557L,
    0xFFF59E0BL,
    0xFF38BDF8L,
    0xFFE2E8F0L
)

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    confirmLabel: String = "Save",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioDock,
        title = { Text(title, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isNotBlank()) onConfirm(value.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = StudioVoid)
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAddSource: (SourceType, String, String, String?) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(SourceType.CAMERA) }
    var customName by remember { mutableStateOf(SourceType.CAMERA.displayName) }
    var textBody by remember { mutableStateOf("FOLLOW FOR MORE LIVE STREAMS") }
    var pickedMediaUri by remember { mutableStateOf<String?>(null) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            pickedMediaUri = uri.toString()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = StudioDock,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .heightIn(max = 600.dp)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Source", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(SourceType.entries) { type ->
                        val active = selectedType == type
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) StudioElevated else StudioVoid.copy(alpha = 0.5f))
                                .border(1.dp, if (active) MintAccent else StudioBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedType = type
                                    customName = type.displayName
                                    pickedMediaUri = null
                                }
                                .padding(10.dp)
                        ) {
                            Icon(
                                sourceTypeIcon(type),
                                contentDescription = null,
                                tint = if (active) MintAccentBright else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(type.displayName, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                Text(type.description, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Source Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == SourceType.TEXT_BANNER) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textBody,
                        onValueChange = { textBody = it },
                        label = { Text("Banner Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedType == SourceType.IMAGE_OVERLAY || selectedType == SourceType.VIDEO_OVERLAY) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val typeFilter = if (selectedType == SourceType.VIDEO_OVERLAY) {
                                ActivityResultContracts.PickVisualMedia.VideoOnly
                            } else {
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            }
                            mediaPickerLauncher.launch(PickVisualMediaRequest(typeFilter))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val label = if (selectedType == SourceType.VIDEO_OVERLAY) {
                            if (pickedMediaUri != null) "Looping Video Selected (Tap to Change)" else "Choose MP4/WebM Video Overlay"
                        } else {
                            if (pickedMediaUri != null) "Image Selected (Tap to Change)" else "Choose Image via Photo Picker"
                        }
                        Text(label, color = MintAccentBright)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        onAddSource(selectedType, customName, textBody, pickedMediaUri)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = StudioVoid),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_add_source_button")
                ) {
                    Text("Add Source to Scene", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SourcePropertiesDialog(
    source: StudioSource,
    orientation: OutputOrientation,
    onDismiss: () -> Unit,
    onSave: (StudioSource) -> Unit,
    onDelete: (Long) -> Unit
) {
    val rect = if (orientation == OutputOrientation.HORIZONTAL) source.hRect else source.vRect
    var name by remember { mutableStateOf(source.name) }
    var textContent by remember { mutableStateOf(source.textContent) }
    var frontCam by remember { mutableStateOf(source.cameraFacingFront) }
    var mirrorFront by remember { mutableStateOf(source.cameraMirror) }
    var cameraZoom by remember { mutableFloatStateOf(source.cameraZoom) }
    var cropToFill by remember { mutableStateOf(source.cropToFill) }
    var opacity by remember { mutableFloatStateOf(source.opacity) }
    var accentHex by remember { mutableLongStateOf(source.accentColorHex) }
    var posX by remember { mutableFloatStateOf(rect.x) }
    var posY by remember { mutableFloatStateOf(rect.y) }
    var widthPct by remember { mutableFloatStateOf(rect.width) }
    var heightPct by remember { mutableFloatStateOf(rect.height) }
    var cornerDp by remember { mutableIntStateOf(source.cornerRadiusDp) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = StudioDock,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Source Properties (${orientation.aspectLabel})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    IconButton(onClick = {
                        onDelete(source.id)
                        onDismiss()
                    }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete source", tint = LiveCrimsonBright)
                    }
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Source Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (source.type == SourceType.TEXT_BANNER || source.type == SourceType.DONATION_GOAL) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        label = { Text("Display Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (source.type == SourceType.CAMERA) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudioElevated)
                            .clickable { frontCam = !frontCam }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Cameraswitch, contentDescription = null, tint = MintAccentBright)
                            Spacer(Modifier.width(8.dp))
                            Text(if (frontCam) "Front Camera (Selfie)" else "Rear Camera (Main)", color = TextPrimary)
                        }
                        Text("Switch", color = MintAccent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mirror Front Camera", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = mirrorFront,
                            onCheckedChange = { mirrorFront = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MintAccent)
                        )
                    }
                    Text(
                        String.format(Locale.US, "Camera Zoom: %.1fx", cameraZoom),
                        color = MintAccentBright,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = cameraZoom,
                        onValueChange = { cameraZoom = it },
                        valueRange = 1.0f..4.0f,
                        colors = SliderDefaults.colors(thumbColor = MintAccentBright, activeTrackColor = MintAccent)
                    )
                }

                if (source.type == SourceType.SCREEN_CAPTURE || source.type == SourceType.CAMERA ||
                    source.type == SourceType.IMAGE_OVERLAY || source.type == SourceType.VIDEO_OVERLAY
                ) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Aspect Scaling", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (cropToFill) "Crop to Fill Box" else "Fit Proportional (No Crop)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = cropToFill,
                            onCheckedChange = { cropToFill = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MintAccent)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            posX = (1f - widthPct) * 0.5f
                            posY = (1f - heightPct) * 0.5f
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Center", color = MintAccentBright, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            posX = 0f; posY = 0f; widthPct = 1f; heightPct = 1f
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fit Screen", color = MintAccentBright, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Width: ${(widthPct * 100).toInt()}% • Height: ${(heightPct * 100).toInt()}%", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = widthPct,
                    onValueChange = { widthPct = it },
                    valueRange = 0.08f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = MintAccentBright, activeTrackColor = MintAccent)
                )
                Slider(
                    value = heightPct,
                    onValueChange = { heightPct = it },
                    valueRange = 0.06f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = MintAccentBright, activeTrackColor = MintAccent)
                )

                Text("Opacity: ${(opacity * 100).toInt()}% • Radius: ${cornerDp}dp", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = MintAccentBright, activeTrackColor = MintAccent)
                )
                Slider(
                    value = cornerDp.toFloat(),
                    onValueChange = { cornerDp = it.toInt() },
                    valueRange = 0f..32f,
                    colors = SliderDefaults.colors(thumbColor = MintAccentBright, activeTrackColor = MintAccent)
                )

                Text("Accent Color", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioSwatchColors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(2.dp, if (accentHex == hex) Color.White else Color.Transparent, CircleShape)
                                .clickable { accentHex = hex }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val clampedRect = NormalizedRect(
                                x = posX.coerceIn(0f, (1f - widthPct).coerceAtLeast(0f)),
                                y = posY.coerceIn(0f, (1f - heightPct).coerceAtLeast(0f)),
                                width = widthPct,
                                height = heightPct
                            )
                            val base = source.copy(
                                name = name.ifBlank { source.name },
                                textContent = textContent,
                                cameraFacingFront = frontCam,
                                cameraMirror = mirrorFront,
                                cameraZoom = cameraZoom,
                                cropToFill = cropToFill,
                                opacity = opacity,
                                accentColorHex = accentHex,
                                cornerRadiusDp = cornerDp
                            )
                            val updated = if (orientation == OutputOrientation.HORIZONTAL) {
                                base.copy(hRect = clampedRect)
                            } else {
                                base.copy(vRect = clampedRect)
                            }
                            onSave(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = StudioVoid)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AudioChannelSettingsDialog(
    channel: AudioChannelConfig,
    onDismiss: () -> Unit,
    onSave: (AudioChannelConfig) -> Unit
) {
    var gateEnabled by remember { mutableStateOf(channel.noiseGateEnabled) }
    var gateThreshold by remember { mutableFloatStateOf(channel.noiseGateThresholdDb) }
    var compressorEnabled by remember { mutableStateOf(channel.compressorEnabled) }
    var syncOffsetMs by remember { mutableIntStateOf(channel.syncOffsetMs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioDock,
        title = { Text("${channel.name} Filters & Sync", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Noise Gate", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                        Text("Silences background hum below threshold", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = gateEnabled,
                        onCheckedChange = { gateEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MintAccent)
                    )
                }
                if (gateEnabled) {
                    Text("Gate Threshold: ${gateThreshold.toInt()} dB", color = MintAccentBright, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = gateThreshold,
                        onValueChange = { gateThreshold = it },
                        valueRange = -60f..-15f,
                        colors = SliderDefaults.colors(thumbColor = MintAccentBright, activeTrackColor = MintAccent)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Voice Compressor", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                        Text("Prevents clipping during loud reactions", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = compressorEnabled,
                        onCheckedChange = { compressorEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MintAccent)
                    )
                }
                Column {
                    Text("Audio Sync Offset: $syncOffsetMs ms", color = RecordAmber, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = syncOffsetMs.toFloat(),
                        onValueChange = { syncOffsetMs = (it / 25f).toInt() * 25 },
                        valueRange = -500f..500f,
                        colors = SliderDefaults.colors(thumbColor = RecordAmber, activeTrackColor = RecordAmber)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        channel.copy(
                            noiseGateEnabled = gateEnabled,
                            noiseGateThresholdDb = gateThreshold,
                            compressorEnabled = compressorEnabled,
                            syncOffsetMs = syncOffsetMs
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = StudioVoid)
            ) {
                Text("Save Filters", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun DestinationsSheetDialog(
    destinations: List<StreamDestination>,
    healthMap: Map<Long, DestinationHealthState>,
    onDismiss: () -> Unit,
    onSaveDestination: (StreamDestination) -> Unit,
    onToggleEnabled: (StreamDestination) -> Unit,
    onDeleteDestination: (Long) -> Unit
) {
    var editingDest by remember { mutableStateOf<StreamDestination?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = StudioDock,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Multistream & Dual Output", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Text("Free unlimited RTMP/RTMPS targets — zero paywalls", color = MintAccent, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
                Spacer(Modifier.height(10.dp))

                if (editingDest != null) {
                    DestinationEditorForm(
                        initial = editingDest!!,
                        onCancel = { editingDest = null },
                        onSave = { saved ->
                            onSaveDestination(saved)
                            editingDest = null
                        }
                    )
                } else {
                    Button(
                        onClick = {
                            editingDest = StreamDestination(
                                name = "Twitch Stream",
                                platform = StreamPlatform.TWITCH,
                                rtmpUrl = StreamPlatform.TWITCH.defaultIngestUrl,
                                streamKey = ""
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = StudioVoid),
                        modifier = Modifier.fillMaxWidth().testTag("new_destination_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add RTMP / Platform Target", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(destinations, key = { it.id }) { dest ->
                            val hasKey = dest.streamKey.isNotBlank()
                            val health = healthMap[dest.id] ?: DestinationHealthState.IDLE
                            val healthBadgeColor = when (health) {
                                DestinationHealthState.LIVE -> MintAccentBright
                                DestinationHealthState.RECONNECTING -> RecordAmber
                                DestinationHealthState.FAILED -> LiveCrimsonBright
                                DestinationHealthState.CONNECTING -> RecordAmber
                                DestinationHealthState.IDLE -> Color(dest.platform.colorHex)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioVoid.copy(alpha = 0.65f))
                                    .border(1.dp, if (dest.isEnabled) healthBadgeColor else StudioBorder, RoundedCornerShape(8.dp))
                                    .clickable { editingDest = dest }
                                    .padding(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(healthBadgeColor)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dest.name, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                    val statusText = when {
                                        health != DestinationHealthState.IDLE -> health.name
                                        hasKey -> "Key Ready"
                                        else -> "Tap to add Stream Key"
                                    }
                                    Text(
                                        "${dest.platform.displayName} • ${dest.orientation.aspectLabel} • $statusText",
                                        color = if (hasKey) TextSecondary else RecordAmber,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Switch(
                                    checked = dest.isEnabled,
                                    onCheckedChange = { onToggleEnabled(dest) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = MintAccent)
                                )
                                IconButton(onClick = { onDeleteDestination(dest.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete destination", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DestinationEditorForm(
    initial: StreamDestination,
    onCancel: () -> Unit,
    onSave: (StreamDestination) -> Unit
) {
    var platform by remember { mutableStateOf(initial.platform) }
    var name by remember { mutableStateOf(initial.name) }
    var rtmpUrl by remember { mutableStateOf(initial.rtmpUrl) }
    var streamKey by remember { mutableStateOf(initial.streamKey) }
    var orientation by remember { mutableStateOf(initial.orientation) }
    var showKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Select Platform Preset", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StreamPlatform.entries.forEach { p ->
                FilterChip(
                    selected = platform == p,
                    onClick = {
                        platform = p
                        rtmpUrl = p.defaultIngestUrl
                        if (name == initial.name || StreamPlatform.entries.any { name.startsWith(it.displayName) }) {
                            name = "${p.displayName} (${orientation.aspectLabel})"
                        }
                    },
                    label = { Text(p.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(p.colorHex).copy(alpha = 0.25f),
                        selectedLabelColor = TextPrimary
                    )
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Label") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = rtmpUrl,
            onValueChange = { rtmpUrl = it },
            label = { Text("RTMP / RTMPS Server URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = streamKey,
            onValueChange = { streamKey = it },
            label = { Text("Stream Key") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(
                        if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = "Toggle key visibility"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutputOrientation.entries.forEach { orient ->
                FilterChip(
                    selected = orientation == orient,
                    onClick = { orientation = orient },
                    label = { Text("${orient.label} (${orient.aspectLabel})") }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Back", color = TextSecondary) }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.ifBlank { platform.displayName },
                            platform = platform,
                            rtmpUrl = rtmpUrl.trim(),
                            streamKey = streamKey.trim(),
                            orientation = orientation,
                            isEnabled = true
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = StudioVoid)
            ) {
                Text("Save Target", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioSettingsDialog(
    preferences: StudioPreferences,
    isScreenCaptureActive: Boolean,
    onDismiss: () -> Unit,
    onUpdatePreferences: ((StudioPreferences) -> StudioPreferences) -> Unit,
    onToggleScreenCapture: () -> Unit,
    onOpenKeyboardShortcuts: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Video & Stream", "Recording", "Audio", "Mobile Pro")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = StudioDock,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioHeader)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("OBS Studio Settings", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenKeyboardShortcuts) {
                            Icon(Icons.Rounded.Keyboard, contentDescription = "Keyboard Shortcuts", tint = MintAccentBright)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = StudioHeader,
                    contentColor = MintAccentBright,
                    indicator = { positions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                            color = MintAccent
                        )
                    }
                ) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(label, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            Text("Stream Video Bitrate: ${preferences.videoBitrateKbps} kbps", color = MintAccentBright, style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = preferences.videoBitrateKbps.toFloat(),
                                onValueChange = { val r = (it / 250f).toInt() * 250; onUpdatePreferences { p -> p.copy(videoBitrateKbps = r) } },
                                valueRange = 1000f..15000f,
                                colors = SliderDefaults.colors(thumbColor = MintAccentBright, activeTrackColor = MintAccent)
                            )

                            Text("Output Resolution", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(480 to "480p", 720 to "720p HD", 1080 to "1080p FHD").forEach { (res, label) ->
                                    FilterChip(
                                        selected = preferences.resolutionHeight == res,
                                        onClick = { onUpdatePreferences { it.copy(resolutionHeight = res) } },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Text("Frame Rate (FPS)", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(24, 30, 48, 60).forEach { fps ->
                                    FilterChip(
                                        selected = preferences.targetFps == fps,
                                        onClick = { onUpdatePreferences { it.copy(targetFps = fps) } },
                                        label = { Text("$fps FPS") }
                                    )
                                }
                            }

                            Text("Rate Control & H.264 Profile", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RateControlMode.entries.forEach { rc ->
                                    FilterChip(
                                        selected = preferences.rateControl == rc,
                                        onClick = { onUpdatePreferences { it.copy(rateControl = rc) } },
                                        label = { Text(rc.label) }
                                    )
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EncoderProfile.entries.forEach { prof ->
                                    FilterChip(
                                        selected = preferences.encoderProfile == prof,
                                        onClick = { onUpdatePreferences { it.copy(encoderProfile = prof) } },
                                        label = { Text(prof.label) }
                                    )
                                }
                            }

                            Text("Keyframe Interval: ${preferences.keyframeIntervalSec}s", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1, 2, 3, 4).forEach { sec ->
                                    FilterChip(
                                        selected = preferences.keyframeIntervalSec == sec,
                                        onClick = { onUpdatePreferences { it.copy(keyframeIntervalSec = sec) } },
                                        label = { Text("${sec}s") }
                                    )
                                }
                            }
                        }

                        1 -> {
                            Text("Local Recording Bitrate: ${preferences.recordingBitrateKbps} kbps", color = MintAccentBright, style = MaterialTheme.typography.labelMedium)
                            Text("Higher bitrate produces crisp local MP4 masters even if your live stream is 6,000 kbps.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = preferences.recordingBitrateKbps.toFloat(),
                                onValueChange = { val r = (it / 500f).toInt() * 500; onUpdatePreferences { p -> p.copy(recordingBitrateKbps = r) } },
                                valueRange = 4000f..25000f,
                                colors = SliderDefaults.colors(thumbColor = MintAccentBright, activeTrackColor = MintAccent)
                            )

                            Text("Replay Buffer Length: ${preferences.replayBufferSeconds}s", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(15, 30, 60, 90, 120).forEach { sec ->
                                    FilterChip(
                                        selected = preferences.replayBufferSeconds == sec,
                                        onClick = { onUpdatePreferences { it.copy(replayBufferSeconds = sec) } },
                                        label = { Text("${sec}s") }
                                    )
                                }
                            }

                            Button(
                                onClick = onToggleScreenCapture,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isScreenCaptureActive) LiveCrimsonBright else StudioElevated,
                                    contentColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isScreenCaptureActive) "Stop Screen / Game Capture" else "Start Android Screen / Game Capture")
                            }
                        }

                        2 -> {
                            Text("Audio Sample Rate", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(44100 to "44.1 kHz", 48000 to "48 kHz").forEach { (rate, label) ->
                                    FilterChip(
                                        selected = preferences.audioSampleRateHz == rate,
                                        onClick = { onUpdatePreferences { it.copy(audioSampleRateHz = rate) } },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            Text("AAC Audio Bitrate", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(96, 128, 160, 192, 320).forEach { kbps ->
                                    FilterChip(
                                        selected = preferences.audioBitrateKbps == kbps,
                                        onClick = { onUpdatePreferences { it.copy(audioBitrateKbps = kbps) } },
                                        label = { Text("$kbps kbps") }
                                    )
                                }
                            }
                        }

                        3 -> {
                            SettingsToggleRow(
                                title = "Auto-Reconnect Drop Watchdog",
                                subtitle = "Automatically reconnects RTMP drops during multi-hour streams",
                                checked = preferences.autoReconnectEnabled,
                                onCheckedChange = { v -> onUpdatePreferences { it.copy(autoReconnectEnabled = v) } }
                            )
                            SettingsToggleRow(
                                title = "Dynamic Bitrate Adaptation",
                                subtitle = "Adjusts bitrate smoothly on congested mobile networks",
                                checked = preferences.dynamicBitrateEnabled,
                                onCheckedChange = { v -> onUpdatePreferences { it.copy(dynamicBitrateEnabled = v) } }
                            )
                            SettingsToggleRow(
                                title = "Keep Tablet / Phone Screen Awake",
                                subtitle = "Prevents screen sleep during long live broadcasts",
                                checked = preferences.keepScreenAwake,
                                onCheckedChange = { v -> onUpdatePreferences { it.copy(keepScreenAwake = v) } }
                            )
                            SettingsToggleRow(
                                title = "Privacy Shield (Hide Screen Capture)",
                                subtitle = "Replaces game/screen feed with a safe shield banner instantly",
                                checked = preferences.privacyShieldActive,
                                onCheckedChange = { v -> onUpdatePreferences { it.copy(privacyShieldActive = v) } }
                            )
                            SettingsToggleRow(
                                title = "Canvas Center Magnetic Snap",
                                subtitle = "Snaps dragged overlays cleanly to canvas centerlines",
                                checked = preferences.gridSnapEnabled,
                                onCheckedChange = { v -> onUpdatePreferences { it.copy(gridSnapEnabled = v) } }
                            )
                            SettingsToggleRow(
                                title = "Studio Mode (Preview + Program)",
                                subtitle = "Stage scenes before transitioning live",
                                checked = preferences.studioModeEnabled,
                                onCheckedChange = { v -> onUpdatePreferences { it.copy(studioModeEnabled = v) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MintAccent)
        )
    }
}

@Composable
fun KeyboardShortcutsDialog(onDismiss: () -> Unit) {
    val shortcuts = listOf(
        "F1 – F7 / Ctrl + 1..7" to "Switch Scenes 1–7",
        "Space / Enter" to "Transition Preview -> Program (Studio Mode)",
        "Ctrl + L  or  F9" to "Go Live / End Stream",
        "Ctrl + R  or  F10" to "Start / Stop Local MP4 Recording",
        "Ctrl + B  or  F11" to "Save Instant Replay Clip",
        "Ctrl + M" to "Mute / Unmute Mic/Aux",
        "Ctrl + D" to "Mute / Unmute Desktop / Game Audio",
        "Ctrl + P" to "Toggle Privacy Shield",
        "Ctrl + T" to "Trigger Test Widgets (Alerts & Goal)",
        "Ctrl + Shift + S" to "Toggle Studio Mode",
        "Arrow Keys / Shift+Arrows" to "Nudge selected overlay (1% / 5%)",
        "Delete" to "Remove selected overlay",
        "F12  or  Ctrl + /" to "Open this Keyboard Shortcuts Guide"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioDock,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Keyboard, contentDescription = null, tint = MintAccentBright)
                Spacer(Modifier.width(8.dp))
                Text("Hardware Keyboard Hotkeys", color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shortcuts.forEach { (keys, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = StudioElevated,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(1.dp, StudioBorder, RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                keys,
                                color = MintAccentBright,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = StudioVoid)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun OverlayThemesDialog(
    presets: List<OverlayThemePreset>,
    activeThemeId: String,
    onDismiss: () -> Unit,
    onApplyTheme: (OverlayThemePreset) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioDock,
        title = {
            Column {
                Text("Free Pro Overlay Themes", color = TextPrimary)
                Text("100% unlocked — zero watermarks or subscriptions", color = MintAccent, style = MaterialTheme.typography.bodySmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    val selected = preset.id == activeThemeId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) StudioElevated else StudioVoid.copy(alpha = 0.5f))
                            .border(1.dp, if (selected) Color(preset.primaryHex) else StudioBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                onApplyTheme(preset)
                                onDismiss()
                            }
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(preset.frameCornerDp.dp))
                                .background(Color(preset.primaryHex))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.name, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                            Text(preset.subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        if (selected) {
                            Text("ACTIVE", color = MintAccentBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = MintAccentBright) }
        }
    )
}

@Composable
fun RecordingsClipsDialog(
    clips: List<RecordingClip>,
    onDismiss: () -> Unit,
    onDeleteClip: (Long) -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = StudioDock,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Recordings & Replay Clips", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Text("Saved as MP4 in Movies/mobileLabs", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
                Spacer(Modifier.height(10.dp))

                if (clips.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No local recordings yet.\nTap REC or Replay Clip on the studio bar to capture MP4 video.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(clips, key = { it.id }) { clip ->
                            val sizeMb = String.format(Locale.US, "%.1f MB", clip.sizeBytes / (1024f * 1024f))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioVoid.copy(alpha = 0.65f))
                                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(clip.title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${if (clip.isReplayClip) "REPLAY" else "REC"} • ${clip.orientation.aspectLabel} • ${formatDuration(clip.durationSec)} • $sizeMb",
                                        color = MintAccent,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                IconButton(onClick = {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(clip.fileUri), "video/mp4")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    }
                                }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play MP4", tint = MintAccentBright)
                                }
                                IconButton(onClick = {
                                    runCatching {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, Uri.parse(clip.fileUri))
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(share, "Share MP4 Clip"))
                                    }
                                }) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Share clip", tint = TextSecondary)
                                }
                                IconButton(onClick = { onDeleteClip(clip.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove from list", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
