package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BroadcastLogEntry
import com.example.data.model.StreamMarker
import com.example.ui.theme.LiveCrimson
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

@Composable
fun BroadcastLogsDialog(
    logs: List<BroadcastLogEntry>,
    markers: List<StreamMarker>,
    onDismiss: () -> Unit,
    onClearLogs: () -> Unit,
    onDropMarker: () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }

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
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioHeader)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Terminal, contentDescription = null, tint = MintAccentBright)
                        Spacer(Modifier.width(8.dp))
                        Text("Broadcast Diagnostics & Markers", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = StudioHeader,
                    contentColor = MintAccentBright,
                    indicator = { positions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(positions[tabIndex]),
                            color = MintAccent
                        )
                    }
                ) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Engine Logs (${logs.size})") })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Stream Markers (${markers.size})") })
                }

                if (tabIndex == 0) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(StudioVoid.copy(alpha = 0.7f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs.asReversed(), key = { it.id }) { entry ->
                            val levelColor = when (entry.level) {
                                "ERR" -> LiveCrimsonBright
                                "WARN" -> RecordAmber
                                else -> MintAccentBright
                            }
                            Text(
                                text = "[${entry.timeText}] [${entry.tag}] ${entry.message}",
                                color = levelColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = onClearLogs) {
                            Text("Clear Logs", color = TextSecondary)
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                        if (markers.isEmpty()) {
                            Text(
                                "No highlight markers dropped yet. Tap the Flag button in Scenes or press F8 during a stream.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(markers, key = { it.id }) { marker ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(StudioElevated)
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(marker.note, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("Scene: ${marker.sceneName}", color = TextMuted, fontSize = 11.sp)
                                        }
                                        Text(formatDuration(marker.elapsedSec), color = MintAccentBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onDropMarker,
                            colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = StudioVoid),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Drop Highlight Marker Now (F8)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SceneCollectionBackupDialog(
    exportedJson: String,
    onDismiss: () -> Unit,
    onImportJson: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var jsonText by remember(exportedJson) { mutableStateOf(exportedJson) }
    var copiedNotice by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = StudioDock,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 600.dp)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Scene Collection JSON Backup", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Export or paste OBS Scene Collection JSON across devices", color = MintAccent, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text("Collection JSON") },
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )

                if (copiedNotice) {
                    Text("Copied JSON to clipboard!", color = MintAccentBright, style = MaterialTheme.typography.labelMedium)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(jsonText))
                            copiedNotice = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy JSON", color = MintAccentBright)
                    }
                    Button(
                        onClick = {
                            onImportJson(jsonText)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintAccent, contentColor = StudioVoid),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import JSON", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmEndStreamDialog(
    elapsedSec: Long,
    onDismiss: () -> Unit,
    onConfirmEnd: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioDock,
        icon = {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = RecordAmber)
        },
        title = { Text("End Live Broadcast?", color = TextPrimary) },
        text = {
            Text(
                "You have been live for ${formatDuration(elapsedSec)}. Ending will disconnect all active RTMP destinations.",
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmEnd()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson, contentColor = Color.White)
            ) {
                Text("End Stream", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Stay Live", color = MintAccentBright) }
        }
    )
}
