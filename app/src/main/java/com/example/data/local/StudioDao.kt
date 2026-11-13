package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDao {

    // Scene Collections
    @Query("SELECT * FROM scene_collections ORDER BY id ASC")
    fun observeCollections(): Flow<List<SceneCollectionEntity>>

    @Query("SELECT COUNT(*) FROM scene_collections")
    suspend fun getCollectionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: SceneCollectionEntity): Long

    @Query("UPDATE scene_collections SET isActive = (id = :collectionId)")
    suspend fun setActiveCollection(collectionId: Long)

    // Scenes
    @Query("SELECT * FROM studio_scenes WHERE collectionId = :collectionId ORDER BY sortOrder ASC, id ASC")
    fun observeScenes(collectionId: Long): Flow<List<StudioSceneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: StudioSceneEntity): Long

    @Update
    suspend fun updateScene(scene: StudioSceneEntity)

    @Query("DELETE FROM studio_scenes WHERE id = :sceneId")
    suspend fun deleteScene(sceneId: Long)

    // Sources
    @Query("SELECT * FROM studio_sources WHERE sceneId = :sceneId ORDER BY sortOrder ASC, id ASC")
    fun observeSources(sceneId: Long): Flow<List<StudioSourceEntity>>

    @Query("SELECT * FROM studio_sources WHERE sceneId = :sceneId ORDER BY sortOrder ASC, id ASC")
    suspend fun getSourcesOnce(sceneId: Long): List<StudioSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: StudioSourceEntity): Long

    @Update
    suspend fun updateSource(source: StudioSourceEntity)

    @Query("DELETE FROM studio_sources WHERE id = :sourceId OR parentFolderId = :sourceId")
    suspend fun deleteSourceAndChildren(sourceId: Long)

    @Query("DELETE FROM studio_sources WHERE sceneId = :sceneId")
    suspend fun deleteSourcesForScene(sceneId: Long)

    // Audio Channels
    @Query("SELECT * FROM audio_channels ORDER BY id ASC")
    fun observeAudioChannels(): Flow<List<AudioChannelEntity>>

    @Query("SELECT COUNT(*) FROM audio_channels")
    suspend fun getAudioChannelCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioChannel(channel: AudioChannelEntity)

    @Update
    suspend fun updateAudioChannel(channel: AudioChannelEntity)

    // Stream Destinations
    @Query("SELECT * FROM stream_destinations ORDER BY id ASC")
    fun observeDestinations(): Flow<List<StreamDestinationEntity>>

    @Query("SELECT COUNT(*) FROM stream_destinations")
    suspend fun getDestinationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestination(destination: StreamDestinationEntity): Long

    @Update
    suspend fun updateDestination(destination: StreamDestinationEntity)

    @Query("DELETE FROM stream_destinations WHERE id = :id")
    suspend fun deleteDestination(id: Long)

    // Recording Clips
    @Query("SELECT * FROM recording_clips ORDER BY createdAtMs DESC")
    fun observeClips(): Flow<List<RecordingClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: RecordingClipEntity): Long

    @Query("DELETE FROM recording_clips WHERE id = :id")
    suspend fun deleteClip(id: Long)
}
