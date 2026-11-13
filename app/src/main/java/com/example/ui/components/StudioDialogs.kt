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
import com.example.data.model.NormalizedRect
import com.example.data.model.OutputOrientation
import com.example.data.model.OverlayThemePreset
import com.example.data.model.RecordingClip
import com.example.data.model.SourceType
import com.example.data.model.StreamDestination
import com.example.data.model.StreamPlatform
import com.example.data.model.StudioPreferences
import com.example.data.model.StudioSource
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
    var selectedType by remember { mutableStateOf(SourceType.CAMERA) }
    var customName by remember { mutableStateOf(SourceType.CAMERA.displayName) }
    var textBody by remember { mutableStateOf("FOLLOW FOR MORE LIVE STREAMS") }
    var pickedImageUri by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedImageUri = uri.toString()
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
                .fillMaxWidth(0.92f)
                .heightIn(max = 580.dp)
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

                if (selectedType == SourceType.IMAGE_OVERLAY) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (pickedImageUri != null) "Image Selected (Tap to Change)" else "Choose Image via Android Photo Picker",
                            color = MintAccentBright
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        onAddSource(selectedType, customName, textBody, pickedImageUri)
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
    var accentHex by remember { mutableLongStateOf(source.accentColorHex) }
    var widthPct by remember { mutableFloatStateOf(rect.width) }
    var heightPct by remember { mutableFloatStateOf(rect.height) }
    var cornerDp by remember { mutableIntStateOf(source.cornerRadiusDp) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = StudioDock,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
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
                }

                Spacer(Modifier.height(12.dp))
                Text("Width: ${(widthPct * 100).toInt()}%", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = widthPct,
                    onValueChange = { widthPct = it },
                    valueRange = 0.12f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = MintAccentBright, activeTrackColor = MintAccent)
                )

                Text("Height: ${(heightPct * 100).toInt()}%", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = heightPct,
                    onValueChange = { heightPct = it },
                    valueRange = 0.06f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = MintAccentBright, activeTrackColor = MintAccent)
                )

                Text("Corner Radius: ${cornerDp}dp", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
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
                                x = rect.x.coerceIn(0f, (1f - widthPct).coerceAtLeast(0f)),
                                y = rect.y.coerceIn(0f, (1f - heightPct).coerceAtLeast(0f)),
                                width = widthPct,
                                height = heightPct
                            )
                            val updated = if (orientation == OutputOrientation.HORIZONTAL) {
                                source.copy(
                                    name = name.ifBlank { source.name },
                                    textContent = textContent,
                                    cameraFacingFront = frontCam,
                                    accentColorHex = accentHex,
                                    cornerRadiusDp = cornerDp,
                                    hRect = clampedRect
                                )
                            } else {
                                source.copy(
                                    name = name.ifBlank { source.name },
                                    textContent = textContent,
                                    cameraFacingFront = frontCam,
                                    accentColorHex = accentHex,
                                    cornerRadiusDp = cornerDp,
                                    vRect = clampedRect
                                )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioDock,
        title = { Text("${channel.name} Filters", color = TextPrimary) },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        channel.copy(
                            noiseGateEnabled = gateEnabled,
                            noiseGateThresholdDb = gateThreshold,
                            compressorEnabled = compressorEnabled
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DestinationsSheetDialog(
    destinations: List<StreamDestination>,
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
                .fillMaxWidth(0.94f)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioVoid.copy(alpha = 0.65f))
                                    .border(1.dp, if (dest.isEnabled) Color(dest.platform.colorHex) else StudioBorder, RoundedCornerShape(8.dp))
                                    .clickable { editingDest = dest }
                                    .padding(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(dest.platform.colorHex))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dest.name, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${dest.platform.displayName} • ${dest.orientation.aspectLabel} • ${if (hasKey) "Key Set" else "Tap to add Stream Key"}",
                                        color = if (hasKey) TextSecondary else Color(0xFFF59E0B),
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

@Composable
fun StudioSettingsDialog(
    preferences: StudioPreferences,
    isScreenCaptureActive: Boolean,
    onDismiss: () -> Unit,
    onUpdatePreferences: ((StudioPreferences) -> StudioPreferences) -> Unit,
    onToggleScreenCapture: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = StudioDock,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Broadcast & Encoder Settings", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Video Bitrate
                Column {
                    Text("Video Bitrate: ${preferences.videoBitrateKbps} kbps", color = MintAccentBright, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = preferences.videoBitrateKbps.toFloat(),
                        onValueChange = { val rounded = (it / 250f).toInt() * 250; onUpdatePreferences { p -> p.copy(videoBitrateKbps = rounded) } },
                        valueRange = 1500f..10000f,
                        colors = SliderDefaults.colors(thumbColor = MintAccentBright, activeTrackColor = MintAccent)
                    )
                }

                // FPS Selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Frame Rate:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    listOf(30, 60).forEach { fps ->
                        FilterChip(
                            selected = preferences.targetFps == fps,
                            onClick = { onUpdatePreferences { it.copy(targetFps = fps) } },
                            label = { Text("$fps FPS") }
                        )
                    }
                }

                // Resolution Selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Output Res:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    listOf(720, 1080).forEach { res ->
                        FilterChip(
                            selected = preferences.resolutionHeight == res,
                            onClick = { onUpdatePreferences { it.copy(resolutionHeight = res) } },
                            label = { Text("${res}p") }
                        )
                    }
                }

                // Replay Buffer Duration
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Replay Buffer:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    listOf(15, 30, 60).forEach { sec ->
                        FilterChip(
                            selected = preferences.replayBufferSeconds == sec,
                            onClick = { onUpdatePreferences { it.copy(replayBufferSeconds = sec) } },
                            label = { Text("${sec}s") }
                        )
                    }
                }

                HorizontalDivider(color = StudioBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Studio Mode (Preview + Program)", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                        Text("Stage next scene before pushing live", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = preferences.studioModeEnabled,
                        onCheckedChange = { enabled -> onUpdatePreferences { it.copy(studioModeEnabled = enabled) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = MintAccent)
                    )
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
        }
    }
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
