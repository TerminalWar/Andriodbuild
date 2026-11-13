package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AudioChannelConfig
import com.example.data.model.NormalizedRect
import com.example.data.model.OutputOrientation
import com.example.data.model.RecordingClip
import com.example.data.model.SourceType
import com.example.data.model.StreamDestination
import com.example.data.model.StreamPlatform
import com.example.data.model.StudioScene
import com.example.data.model.StudioSource

@Entity(tableName = "scene_collections")
data class SceneCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = false
)

@Entity(tableName = "studio_scenes")
data class StudioSceneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: Long,
    val name: String,
    val sortOrder: Int,
    val backdropHex: Long = 0xFF0E1722
)

@Entity(tableName = "studio_sources")
data class StudioSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sceneId: Long,
    val parentFolderId: Long? = null,
    val name: String,
    val typeName: String,
    val sortOrder: Int,
    val isVisibleHorizontal: Boolean = true,
    val isVisibleVertical: Boolean = true,
    val isLocked: Boolean = false,
    val hX: Float = 0f,
    val hY: Float = 0f,
    val hW: Float = 1f,
    val hH: Float = 1f,
    val vX: Float = 0f,
    val vY: Float = 0f,
    val vW: Float = 1f,
    val vH: Float = 1f,
    val textContent: String = "",
    val accentColorHex: Long = 0xFF31C3A2,
    val imageUri: String? = null,
    val cameraFacingFront: Boolean = true,
    val opacity: Float = 1f,
    val cornerRadiusDp: Int = 10
)

@Entity(tableName = "audio_channels")
data class AudioChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val gainDb: Float = 0f,
    val isMuted: Boolean = false,
    val noiseGateEnabled: Boolean = true,
    val noiseGateThresholdDb: Float = -45f,
    val compressorEnabled: Boolean = true,
    val monitorEnabled: Boolean = false
)

@Entity(tableName = "stream_destinations")
data class StreamDestinationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val platformName: String,
    val rtmpUrl: String,
    val streamKey: String,
    val orientationName: String = OutputOrientation.HORIZONTAL.name,
    val isEnabled: Boolean = true
)

@Entity(tableName = "recording_clips")
data class RecordingClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fileUri: String,
    val durationSec: Long,
    val sizeBytes: Long,
    val isReplayClip: Boolean,
    val orientationName: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

fun StudioSceneEntity.toDomain() = StudioScene(
    id = id,
    collectionId = collectionId,
    name = name,
    sortOrder = sortOrder,
    backdropHex = backdropHex
)

fun StudioScene.toEntity() = StudioSceneEntity(
    id = id,
    collectionId = collectionId,
    name = name,
    sortOrder = sortOrder,
    backdropHex = backdropHex
)

fun StudioSourceEntity.toDomain() = StudioSource(
    id = id,
    sceneId = sceneId,
    parentFolderId = parentFolderId,
    name = name,
    type = runCatching { SourceType.valueOf(typeName) }.getOrDefault(SourceType.TEXT_BANNER),
    sortOrder = sortOrder,
    isVisibleHorizontal = isVisibleHorizontal,
    isVisibleVertical = isVisibleVertical,
    isLocked = isLocked,
    hRect = NormalizedRect(hX, hY, hW, hH),
    vRect = NormalizedRect(vX, vY, vW, vH),
    textContent = textContent,
    accentColorHex = accentColorHex,
    imageUri = imageUri,
    cameraFacingFront = cameraFacingFront,
    opacity = opacity,
    cornerRadiusDp = cornerRadiusDp
)

fun StudioSource.toEntity() = StudioSourceEntity(
    id = id,
    sceneId = sceneId,
    parentFolderId = parentFolderId,
    name = name,
    typeName = type.name,
    sortOrder = sortOrder,
    isVisibleHorizontal = isVisibleHorizontal,
    isVisibleVertical = isVisibleVertical,
    isLocked = isLocked,
    hX = hRect.x,
    hY = hRect.y,
    hW = hRect.width,
    hH = hRect.height,
    vX = vRect.x,
    vY = vRect.y,
    vW = vRect.width,
    vH = vRect.height,
    textContent = textContent,
    accentColorHex = accentColorHex,
    imageUri = imageUri,
    cameraFacingFront = cameraFacingFront,
    opacity = opacity,
    cornerRadiusDp = cornerRadiusDp
)

fun AudioChannelEntity.toDomain() = AudioChannelConfig(
    id = id,
    name = name,
    gainDb = gainDb,
    isMuted = isMuted,
    noiseGateEnabled = noiseGateEnabled,
    noiseGateThresholdDb = noiseGateThresholdDb,
    compressorEnabled = compressorEnabled,
    monitorEnabled = monitorEnabled
)

fun AudioChannelConfig.toEntity() = AudioChannelEntity(
    id = id,
    name = name,
    gainDb = gainDb,
    isMuted = isMuted,
    noiseGateEnabled = noiseGateEnabled,
    noiseGateThresholdDb = noiseGateThresholdDb,
    compressorEnabled = compressorEnabled,
    monitorEnabled = monitorEnabled
)

fun StreamDestinationEntity.toDomain() = StreamDestination(
    id = id,
    name = name,
    platform = runCatching { StreamPlatform.valueOf(platformName) }.getOrDefault(StreamPlatform.CUSTOM_RTMP),
    rtmpUrl = rtmpUrl,
    streamKey = streamKey,
    orientation = runCatching { OutputOrientation.valueOf(orientationName) }.getOrDefault(OutputOrientation.HORIZONTAL),
    isEnabled = isEnabled
)

fun StreamDestination.toEntity() = StreamDestinationEntity(
    id = id,
    name = name,
    platformName = platform.name,
    rtmpUrl = rtmpUrl,
    streamKey = streamKey,
    orientationName = orientation.name,
    isEnabled = isEnabled
)

fun RecordingClipEntity.toDomain() = RecordingClip(
    id = id,
    title = title,
    fileUri = fileUri,
    durationSec = durationSec,
    sizeBytes = sizeBytes,
    isReplayClip = isReplayClip,
    orientation = runCatching { OutputOrientation.valueOf(orientationName) }.getOrDefault(OutputOrientation.HORIZONTAL),
    createdAtMs = createdAtMs
)

fun RecordingClip.toEntity() = RecordingClipEntity(
    id = id,
    title = title,
    fileUri = fileUri,
    durationSec = durationSec,
    sizeBytes = sizeBytes,
    isReplayClip = isReplayClip,
    orientationName = orientation.name,
    createdAtMs = createdAtMs
)
