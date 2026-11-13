package com.example

import android.app.Application
import com.example.data.local.MobileLabsDatabase
import com.example.data.local.StudioPreferencesRepository
import com.example.data.repository.StudioRepository
import com.example.streaming.AudioMixerEngine
import com.example.streaming.HardwareEncoderPipeline
import com.example.streaming.TwitchIrcChatClient

class MobileLabsApplication : Application() {

    val database: MobileLabsDatabase by lazy {
        MobileLabsDatabase.getInstance(this)
    }

    val preferencesRepository: StudioPreferencesRepository by lazy {
        StudioPreferencesRepository(this)
    }

    val studioRepository: StudioRepository by lazy {
        StudioRepository(database.studioDao(), preferencesRepository)
    }

    val audioMixerEngine: AudioMixerEngine by lazy {
        AudioMixerEngine()
    }

    val twitchChatClient: TwitchIrcChatClient by lazy {
        TwitchIrcChatClient()
    }

    val encoderPipeline: HardwareEncoderPipeline by lazy {
        HardwareEncoderPipeline(this, audioMixerEngine)
    }
}
