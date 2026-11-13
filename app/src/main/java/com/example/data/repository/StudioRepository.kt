package com.example.data.repository

import com.example.data.local.AudioChannelEntity
import com.example.data.local.SceneCollectionEntity
import com.example.data.local.StreamDestinationEntity
import com.example.data.local.StudioDao
import com.example.data.local.StudioPreferencesRepository
import com.example.data.local.StudioSceneEntity
import com.example.data.local.StudioSourceEntity
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.AudioChannelConfig
import com.example.data.model.AudioChannelId
import com.example.data.model.OutputOrientation
import com.example.data.model.RecordingClip
import com.example.data.model.SourceType
import com.example.data.model.StreamDestination
import com.example.data.model.StreamPlatform
import com.example.data.model.StudioPreferences
import com.example.data.model.StudioScene
import com.example.data.model.StudioSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudioRepository(
    private val dao: StudioDao,
    private val prefsRepo: StudioPreferencesRepository
) {
    val collections: Flow<List<SceneCollectionEntity>> = dao.observeCollections()
    val audioChannels: Flow<List<AudioChannelConfig>> = dao.observeAudioChannels().map { list ->
        list.map { it.toDomain() }
    }
    val destinations: Flow<List<StreamDestination>> = dao.observeDestinations().map { list ->
        list.map { it.toDomain() }
    }
    val clips: Flow<List<RecordingClip>> = dao.observeClips().map { list ->
        list.map { it.toDomain() }
    }
    val preferences: Flow<StudioPreferences> = prefsRepo.preferencesFlow

    fun observeScenes(collectionId: Long): Flow<List<StudioScene>> =
        dao.observeScenes(collectionId).map { list -> list.map { it.toDomain() } }

    fun observeSources(sceneId: Long): Flow<List<StudioSource>> =
        dao.observeSources(sceneId).map { list -> list.map { it.toDomain() } }

    suspend fun ensureSeeded() {
        if (dao.getCollectionCount() == 0) {
            seedDefaultStudio()
        }
        if (dao.getAudioChannelCount() == 0) {
            dao.insertAudioChannel(
                AudioChannelEntity(
                    id = AudioChannelId.DEVICE_AUDIO.name,
                    name = "Desktop / Game Audio",
                    gainDb = 0f,
                    isMuted = false,
                    noiseGateEnabled = false,
                    compressorEnabled = false
                )
            )
            dao.insertAudioChannel(
                AudioChannelEntity(
                    id = AudioChannelId.MIC_AUX.name,
                    name = "Mic/Aux",
                    gainDb = 0f,
                    isMuted = false,
                    noiseGateEnabled = true,
                    noiseGateThresholdDb = -45f,
                    compressorEnabled = true
                )
            )
            dao.insertAudioChannel(
                AudioChannelEntity(
                    id = AudioChannelId.ALERTS_MEDIA.name,
                    name = "Alerts & Media",
                    gainDb = -4f,
                    isMuted = false,
                    noiseGateEnabled = false,
                    compressorEnabled = false
                )
            )
        }
        if (dao.getDestinationCount() == 0) {
            dao.insertDestination(
                StreamDestinationEntity(
                    name = "Twitch Main (16:9)",
                    platformName = StreamPlatform.TWITCH.name,
                    rtmpUrl = StreamPlatform.TWITCH.defaultIngestUrl,
                    streamKey = "",
                    orientationName = OutputOrientation.HORIZONTAL.name,
                    isEnabled = true
                )
            )
            dao.insertDestination(
                StreamDestinationEntity(
                    name = "YouTube Vertical (9:16)",
                    platformName = StreamPlatform.YOUTUBE.name,
                    rtmpUrl = StreamPlatform.YOUTUBE.defaultIngestUrl,
                    streamKey = "",
                    orientationName = OutputOrientation.VERTICAL.name,
                    isEnabled = false
                )
            )
        }
    }

    private suspend fun seedDefaultStudio() {
        val gamingId = dao.insertCollection(
            SceneCollectionEntity(name = "Gaming Scene", isActive = true)
        )
        dao.insertCollection(
            SceneCollectionEntity(name = "IRL & Just Chatting", isActive = false)
        )

        // Gaming Scene Collection — matches OBS / Streamlabs Desktop scene list
        val liveSceneId = dao.insertScene(
            StudioSceneEntity(collectionId = gamingId, name = "Live Scene", sortOrder = 0, backdropHex = 0xFF0C1520)
        )
        val startingId = dao.insertScene(
            StudioSceneEntity(collectionId = gamingId, name = "Starting Soon", sortOrder = 1, backdropHex = 0xFF0E1B24)
        )
        val brbId = dao.insertScene(
            StudioSceneEntity(collectionId = gamingId, name = "Be Right Back", sortOrder = 2, backdropHex = 0xFF141824)
        )
        val thanksId = dao.insertScene(
            StudioSceneEntity(collectionId = gamingId, name = "Thanks For Watching", sortOrder = 3, backdropHex = 0xFF101D1B)
        )
        val offlineId = dao.insertScene(
            StudioSceneEntity(collectionId = gamingId, name = "Offline", sortOrder = 4, backdropHex = 0xFF0B0E14)
        )
        val chatCamId = dao.insertScene(
            StudioSceneEntity(collectionId = gamingId, name = "Chat And Cam", sortOrder = 5, backdropHex = 0xFF121822)
        )
        val chatOnlyId = dao.insertScene(
            StudioSceneEntity(collectionId = gamingId, name = "Chat Only Screen", sortOrder = 6, backdropHex = 0xFF0E131B)
        )

        // Seed Live Scene sources (matching exact tree: Alertbox, Your Cam folder -> Webcam Frame + Video Capture Device, Donation Goal, Chatbox, Screen Capture)
        dao.insertSource(
            StudioSourceEntity(
                sceneId = liveSceneId,
                name = "Alertbox",
                typeName = SourceType.ALERTBOX.name,
                sortOrder = 0,
                hX = 0.25f, hY = 0.04f, hW = 0.50f, hH = 0.14f,
                vX = 0.08f, vY = 0.36f, vW = 0.84f, vH = 0.11f,
                textContent = "New Follower & Sub Alerts",
                accentColorHex = 0xFF31C3A2
            )
        )
        val camFolderId = dao.insertSource(
            StudioSourceEntity(
                sceneId = liveSceneId,
                name = "Your Cam",
                typeName = SourceType.FOLDER.name,
                sortOrder = 1,
                hX = 0.04f, hY = 0.56f, hW = 0.28f, hH = 0.36f,
                vX = 0.06f, vY = 0.04f, vW = 0.88f, vH = 0.30f
            )
        )
        dao.insertSource(
            StudioSourceEntity(
                sceneId = liveSceneId,
                parentFolderId = camFolderId,
                name = "Webcam Frame",
                typeName = SourceType.WEBCAM_FRAME.name,
                sortOrder = 2,
                hX = 0.04f, hY = 0.56f, hW = 0.28f, hH = 0.36f,
                vX = 0.06f, vY = 0.04f, vW = 0.88f, vH = 0.30f,
                accentColorHex = 0xFF31C3A2,
                cornerRadiusDp = 12
            )
        )
        dao.insertSource(
            StudioSourceEntity(
                sceneId = liveSceneId,
                parentFolderId = camFolderId,
                name = "Video Capture Device",
                typeName = SourceType.CAMERA.name,
                sortOrder = 3,
                hX = 0.045f, hY = 0.57f, hW = 0.27f, hH = 0.34f,
                vX = 0.07f, vY = 0.046f, vW = 0.86f, vH = 0.288f,
                cameraFacingFront = true,
                cornerRadiusDp = 10
            )
        )
        dao.insertSource(
            StudioSourceEntity(
                sceneId = liveSceneId,
                name = "Donation Goal",
                typeName = SourceType.DONATION_GOAL.name,
                sortOrder = 4,
                hX = 0.36f, hY = 0.87f, hW = 0.36f, hH = 0.09f,
                vX = 0.08f, vY = 0.38f, vW = 0.84f, vH = 0.08f,
                textContent = "Stream Setup Upgrade Goal",
                accentColorHex = 0xFF31C3A2
            )
        )
        dao.insertSource(
            StudioSourceEntity(
                sceneId = liveSceneId,
                name = "Chatbox",
                typeName = SourceType.CHATBOX.name,
                sortOrder = 5,
                isVisibleVertical = false,
                hX = 0.72f, hY = 0.44f, hW = 0.25f, hH = 0.48f,
                vX = 0.06f, vY = 0.74f, vW = 0.88f, vH = 0.22f,
                accentColorHex = 0xFF9146FF
            )
        )
        dao.insertSource(
            StudioSourceEntity(
                sceneId = liveSceneId,
                name = "Screen / Game Capture",
                typeName = SourceType.SCREEN_CAPTURE.name,
                sortOrder = 6,
                hX = 0f, hY = 0f, hW = 1f, hH = 1f,
                vX = 0f, vY = 0.48f, vW = 1f, vH = 0.52f
            )
        )

        // Seed Starting Soon
        dao.insertSource(
            StudioSourceEntity(
                sceneId = startingId,
                name = "Starting Banner",
                typeName = SourceType.TEXT_BANNER.name,
                sortOrder = 0,
                hX = 0.14f, hY = 0.34f, hW = 0.72f, hH = 0.28f,
                vX = 0.08f, vY = 0.32f, vW = 0.84f, vH = 0.22f,
                textContent = "STREAM STARTING SOON\nGrab snacks — going live momentarily",
                accentColorHex = 0xFF31C3A2
            )
        )
        dao.insertSource(
            StudioSourceEntity(
                sceneId = startingId,
                name = "Chatbox",
                typeName = SourceType.CHATBOX.name,
                sortOrder = 1,
                hX = 0.68f, hY = 0.12f, hW = 0.28f, hH = 0.74f,
                vX = 0.08f, vY = 0.58f, vW = 0.84f, vH = 0.34f
            )
        )

        // Seed Be Right Back
        dao.insertSource(
            StudioSourceEntity(
                sceneId = brbId,
                name = "BRB Banner",
                typeName = SourceType.TEXT_BANNER.name,
                sortOrder = 0,
                hX = 0.18f, hY = 0.35f, hW = 0.64f, hH = 0.26f,
                vX = 0.08f, vY = 0.36f, vW = 0.84f, vH = 0.20f,
                textContent = "BE RIGHT BACK\nDon't go anywhere!",
                accentColorHex = 0xFFF59E0B
            )
        )

        // Seed Thanks For Watching
        dao.insertSource(
            StudioSourceEntity(
                sceneId = thanksId,
                name = "Outro Banner",
                typeName = SourceType.TEXT_BANNER.name,
                sortOrder = 0,
                hX = 0.16f, hY = 0.34f, hW = 0.68f, hH = 0.28f,
                vX = 0.08f, vY = 0.35f, vW = 0.84f, vH = 0.22f,
                textContent = "THANKS FOR WATCHING!\nFollow & drop a clip from tonight",
                accentColorHex = 0xFF31C3A2
            )
        )

        // Seed Offline
        dao.insertSource(
            StudioSourceEntity(
                sceneId = offlineId,
                name = "Offline Card",
                typeName = SourceType.TEXT_BANNER.name,
                sortOrder = 0,
                hX = 0.2f, hY = 0.36f, hW = 0.6f, hH = 0.24f,
                vX = 0.1f, vY = 0.38f, vW = 0.8f, vH = 0.18f,
                textContent = "STREAM IS OFFLINE\nCheck schedule for next broadcast",
                accentColorHex = 0xFF64748B
            )
        )

        // Seed Chat And Cam
        dao.insertSource(
            StudioSourceEntity(
                sceneId = chatCamId,
                name = "Video Capture Device",
                typeName = SourceType.CAMERA.name,
                sortOrder = 0,
                hX = 0.03f, hY = 0.05f, hW = 0.62f, hH = 0.90f,
                vX = 0.04f, vY = 0.04f, vW = 0.92f, vH = 0.48f,
                cameraFacingFront = true
            )
        )
        dao.insertSource(
            StudioSourceEntity(
                sceneId = chatCamId,
                name = "Webcam Frame",
                typeName = SourceType.WEBCAM_FRAME.name,
                sortOrder = 1,
                hX = 0.03f, hY = 0.05f, hW = 0.62f, hH = 0.90f,
                vX = 0.04f, vY = 0.04f, vW = 0.92f, vH = 0.48f,
                accentColorHex = 0xFF31C3A2
            )
        )
        dao.insertSource(
            StudioSourceEntity(
                sceneId = chatCamId,
                name = "Chatbox",
                typeName = SourceType.CHATBOX.name,
                sortOrder = 2,
                hX = 0.68f, hY = 0.05f, hW = 0.29f, hH = 0.90f,
                vX = 0.04f, vY = 0.55f, vW = 0.92f, vH = 0.40f
            )
        )

        // Seed Chat Only Screen
        dao.insertSource(
            StudioSourceEntity(
                sceneId = chatOnlyId,
                name = "Chatbox",
                typeName = SourceType.CHATBOX.name,
                sortOrder = 0,
                hX = 0.15f, hY = 0.08f, hW = 0.70f, hH = 0.84f,
                vX = 0.06f, vY = 0.08f, vW = 0.88f, vH = 0.84f
            )
        )
    }

    suspend fun switchCollection(collectionId: Long) = dao.setActiveCollection(collectionId)

    suspend fun addCollection(name: String): Long {
        val id = dao.insertCollection(SceneCollectionEntity(name = name, isActive = true))
        dao.setActiveCollection(id)
        val sceneId = dao.insertScene(StudioSceneEntity(collectionId = id, name = "Main Scene", sortOrder = 0))
        dao.insertSource(
            StudioSourceEntity(
                sceneId = sceneId,
                name = "Video Capture Device",
                typeName = SourceType.CAMERA.name,
                sortOrder = 0,
                hX = 0f, hY = 0f, hW = 1f, hH = 1f,
                vX = 0f, vY = 0f, vW = 1f, vH = 1f
            )
        )
        return id
    }

    suspend fun addScene(collectionId: Long, name: String, order: Int): Long {
        val sceneId = dao.insertScene(
            StudioSceneEntity(collectionId = collectionId, name = name, sortOrder = order)
        )
        dao.insertSource(
            StudioSourceEntity(
                sceneId = sceneId,
                name = "Screen / Game Capture",
                typeName = SourceType.SCREEN_CAPTURE.name,
                sortOrder = 0
            )
        )
        return sceneId
    }

    suspend fun renameScene(scene: StudioScene, newName: String) {
        dao.updateScene(scene.copy(name = newName).toEntity())
    }

    suspend fun updateSceneBackdrop(scene: StudioScene, colorHex: Long) {
        dao.updateScene(scene.copy(backdropHex = colorHex).toEntity())
    }

    suspend fun duplicateScene(scene: StudioScene, newOrder: Int): Long {
        val newSceneId = dao.insertScene(
            StudioSceneEntity(
                collectionId = scene.collectionId,
                name = "${scene.name} (Copy)",
                sortOrder = newOrder,
                backdropHex = scene.backdropHex
            )
        )
        val sources = dao.getSourcesOnce(scene.id)
        val idMap = mutableMapOf<Long, Long>()
        sources.filter { it.parentFolderId == null }.forEach { src ->
            val newId = dao.insertSource(src.copy(id = 0, sceneId = newSceneId))
            idMap[src.id] = newId
        }
        sources.filter { it.parentFolderId != null }.forEach { src ->
            val newParent = idMap[src.parentFolderId]
            dao.insertSource(src.copy(id = 0, sceneId = newSceneId, parentFolderId = newParent))
        }
        return newSceneId
    }

    suspend fun deleteScene(sceneId: Long) {
        dao.deleteSourcesForScene(sceneId)
        dao.deleteScene(sceneId)
    }

    suspend fun addSource(source: StudioSource): Long = dao.insertSource(source.toEntity())

    suspend fun duplicateSource(source: StudioSource): Long {
        val shiftedH = source.hRect.copy(
            x = (source.hRect.x + 0.03f).coerceIn(0f, (1f - source.hRect.width).coerceAtLeast(0f)),
            y = (source.hRect.y + 0.03f).coerceIn(0f, (1f - source.hRect.height).coerceAtLeast(0f))
        )
        val shiftedV = source.vRect.copy(
            x = (source.vRect.x + 0.03f).coerceIn(0f, (1f - source.vRect.width).coerceAtLeast(0f)),
            y = (source.vRect.y + 0.03f).coerceIn(0f, (1f - source.vRect.height).coerceAtLeast(0f))
        )
        return dao.insertSource(
            source.copy(
                id = 0,
                name = "${source.name} Copy",
                hRect = shiftedH,
                vRect = shiftedV,
                sortOrder = source.sortOrder + 1
            ).toEntity()
        )
    }

    suspend fun updateSource(source: StudioSource) = dao.updateSource(source.toEntity())

    suspend fun deleteSource(sourceId: Long) = dao.deleteSourceAndChildren(sourceId)

    suspend fun moveSourceOrder(sources: List<StudioSource>, sourceId: Long, moveUp: Boolean) {
        val idx = sources.indexOfFirst { it.id == sourceId }
        if (idx == -1) return
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx !in sources.indices) return
        val a = sources[idx]
        val b = sources[targetIdx]
        dao.updateSource(a.copy(sortOrder = b.sortOrder).toEntity())
        dao.updateSource(b.copy(sortOrder = a.sortOrder).toEntity())
    }

    suspend fun updateAudioChannel(channel: AudioChannelConfig) =
        dao.updateAudioChannel(channel.toEntity())

    suspend fun saveDestination(destination: StreamDestination): Long =
        dao.insertDestination(destination.toEntity())

    suspend fun updateDestination(destination: StreamDestination) =
        dao.updateDestination(destination.toEntity())

    suspend fun deleteDestination(id: Long) = dao.deleteDestination(id)

    suspend fun addRecordingClip(clip: RecordingClip): Long = dao.insertClip(clip.toEntity())

    suspend fun deleteClip(id: Long) = dao.deleteClip(id)

    suspend fun updatePreferences(transform: (StudioPreferences) -> StudioPreferences) =
        prefsRepo.updatePreferences(transform)

    suspend fun exportSceneCollectionJson(collectionId: Long, collectionName: String, scenes: List<StudioScene>): String {
        val root = org.json.JSONObject()
        root.put("name", collectionName)
        root.put("version", 3)
        val scenesArr = org.json.JSONArray()
        for (sc in scenes) {
            val scObj = org.json.JSONObject()
            scObj.put("name", sc.name)
            scObj.put("sortOrder", sc.sortOrder)
            scObj.put("backdropHex", sc.backdropHex)
            val srcArr = org.json.JSONArray()
            for (s in dao.getSourcesOnce(sc.id)) {
                srcArr.put(org.json.JSONObject().apply {
                    put("name", s.name)
                    put("typeName", s.typeName)
                    put("sortOrder", s.sortOrder)
                    put("isVisibleHorizontal", s.isVisibleHorizontal)
                    put("isVisibleVertical", s.isVisibleVertical)
                    put("hX", s.hX.toDouble()); put("hY", s.hY.toDouble())
                    put("hW", s.hW.toDouble()); put("hH", s.hH.toDouble())
                    put("vX", s.vX.toDouble()); put("vY", s.vY.toDouble())
                    put("vW", s.vW.toDouble()); put("vH", s.vH.toDouble())
                    put("textContent", s.textContent)
                    put("accentColorHex", s.accentColorHex)
                    put("cameraFacingFront", s.cameraFacingFront)
                    put("cameraMirror", s.cameraMirror)
                    put("cameraZoom", s.cameraZoom.toDouble())
                    put("cropToFill", s.cropToFill)
                    put("opacity", s.opacity.toDouble())
                    put("cornerRadiusDp", s.cornerRadiusDp)
                    put("chromaKeyEnabled", s.chromaKeyEnabled)
                })
            }
            scObj.put("sources", srcArr)
            scenesArr.put(scObj)
        }
        root.put("scenes", scenesArr)
        return root.toString(2)
    }

    suspend fun importSceneCollectionJson(rawJson: String): Result<Long> = runCatching {
        val root = org.json.JSONObject(rawJson)
        val baseName = root.optString("name", "Imported Scene").ifBlank { "Imported Scene" }
        val colId = dao.insertCollection(SceneCollectionEntity(name = "$baseName (Imported)", isActive = true))
        dao.setActiveCollection(colId)
        val scenesArr = root.getJSONArray("scenes")
        for (i in 0 until scenesArr.length()) {
            val scObj = scenesArr.getJSONObject(i)
            val sceneId = dao.insertScene(
                StudioSceneEntity(
                    collectionId = colId,
                    name = scObj.optString("name", "Scene ${i + 1}"),
                    sortOrder = scObj.optInt("sortOrder", i),
                    backdropHex = scObj.optLong("backdropHex", 0xFF0C1520)
                )
            )
            val srcArr = scObj.optJSONArray("sources") ?: continue
            for (j in 0 until srcArr.length()) {
                val s = srcArr.getJSONObject(j)
                dao.insertSource(
                    StudioSourceEntity(
                        sceneId = sceneId,
                        name = s.optString("name", "Source"),
                        typeName = s.optString("typeName", SourceType.TEXT_BANNER.name),
                        sortOrder = s.optInt("sortOrder", j),
                        isVisibleHorizontal = s.optBoolean("isVisibleHorizontal", true),
                        isVisibleVertical = s.optBoolean("isVisibleVertical", true),
                        hX = s.optDouble("hX", 0.0).toFloat(),
                        hY = s.optDouble("hY", 0.0).toFloat(),
                        hW = s.optDouble("hW", 1.0).toFloat(),
                        hH = s.optDouble("hH", 1.0).toFloat(),
                        vX = s.optDouble("vX", 0.0).toFloat(),
                        vY = s.optDouble("vY", 0.0).toFloat(),
                        vW = s.optDouble("vW", 1.0).toFloat(),
                        vH = s.optDouble("vH", 1.0).toFloat(),
                        textContent = s.optString("textContent", ""),
                        accentColorHex = s.optLong("accentColorHex", 0xFF31C3A2),
                        cameraFacingFront = s.optBoolean("cameraFacingFront", true),
                        cameraMirror = s.optBoolean("cameraMirror", true),
                        cameraZoom = s.optDouble("cameraZoom", 1.0).toFloat(),
                        cropToFill = s.optBoolean("cropToFill", true),
                        opacity = s.optDouble("opacity", 1.0).toFloat(),
                        cornerRadiusDp = s.optInt("cornerRadiusDp", 10),
                        chromaKeyEnabled = s.optBoolean("chromaKeyEnabled", false)
                    )
                )
            }
        }
        colId
    }
}
