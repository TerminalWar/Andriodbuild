package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SceneCollectionEntity::class,
        StudioSceneEntity::class,
        StudioSourceEntity::class,
        AudioChannelEntity::class,
        StreamDestinationEntity::class,
        RecordingClipEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MobileLabsDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: MobileLabsDatabase? = null

        fun getInstance(context: Context): MobileLabsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MobileLabsDatabase::class.java,
                    "mobilelabs_studio.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
