package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MobileLabsDatabase
import com.example.data.local.StudioPreferencesRepository
import com.example.data.repository.StudioRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app_name matches mobileLabs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("mobileLabs", appName)
    }

    @Test
    fun `studio repository seeds default OBS scenes, sources, and audio channels`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MobileLabsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = StudioRepository(db.studioDao(), StudioPreferencesRepository(context))
            repo.ensureSeeded()

            val collections = repo.collections.first()
            assertTrue(collections.any { it.name == "Gaming Scene" })

            val activeCol = collections.first { it.isActive }
            val scenes = repo.observeScenes(activeCol.id).first()
            assertEquals(7, scenes.size)
            assertEquals("Live Scene", scenes.first().name)

            val liveSources = repo.observeSources(scenes.first().id).first()
            assertTrue(liveSources.any { it.name == "Alertbox" })
            assertTrue(liveSources.any { it.name == "Video Capture Device" })
            assertTrue(liveSources.any { it.name == "Screen / Game Capture" })

            val channels = repo.audioChannels.first()
            assertEquals(3, channels.size)
        } finally {
            db.close()
        }
    }
}
