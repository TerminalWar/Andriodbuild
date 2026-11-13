package com.example.streaming

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import com.example.data.model.AudioChannelConfig
import com.example.data.model.AudioChannelId
import com.example.data.model.AudioMeterReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class AudioMixerEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val FRAME_SAMPLES = 1024
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var captureJob: Job? = null
    private var micRecord: AudioRecord? = null
    private var deviceRecord: AudioRecord? = null

    @Volatile
    private var channelConfigs: Map<String, AudioChannelConfig> = emptyMap()

    @Volatile
    var onMixedPcmFrame: ((ShortArray, Int) -> Unit)? = null

    private val _meterReadings = MutableStateFlow(
        mapOf(
            AudioChannelId.DEVICE_AUDIO.name to AudioMeterReading(AudioChannelId.DEVICE_AUDIO.name),
            AudioChannelId.MIC_AUX.name to AudioMeterReading(AudioChannelId.MIC_AUX.name),
            AudioChannelId.ALERTS_MEDIA.name to AudioMeterReading(AudioChannelId.ALERTS_MEDIA.name)
        )
    )
    val meterReadings: StateFlow<Map<String, AudioMeterReading>> = _meterReadings.asStateFlow()

    @Volatile
    private var alertPulseDb: Float = -60f

    fun updateConfigs(configs: List<AudioChannelConfig>) {
        channelConfigs = configs.associateBy { it.id }
    }

    fun triggerAlertAudioPulse() {
        alertPulseDb = -6f
    }

    @SuppressLint("MissingPermission")
    fun startCapture(hasMicPermission: Boolean, mediaProjection: MediaProjection? = null) {
        stopCapture()
        if (!hasMicPermission) {
            // Still run lightweight meter decay loop for alert bus
            captureJob = scope.launch {
                while (isActive) {
                    emitMeters(micRmsDb = -60f, micPeakDb = -60f, devRmsDb = -60f, devPeakDb = -60f)
                    kotlinx.coroutines.delay(50)
                }
            }
            return
        }

        val minBuf = max(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
            FRAME_SAMPLES * 4
        )

        micRecord = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBuf
            ).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
        }.getOrNull() ?: runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBuf
            ).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
        }.getOrNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
            deviceRecord = runCatching {
                val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .build()
                AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(playbackConfig)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AUDIO_FORMAT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL_CONFIG)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .build()
                    .takeIf { it.state == AudioRecord.STATE_INITIALIZED }
            }.getOrNull()
        }

        runCatching { micRecord?.startRecording() }
        runCatching { deviceRecord?.startRecording() }

        captureJob = scope.launch {
            val micBuf = ShortArray(FRAME_SAMPLES)
            val devBuf = ShortArray(FRAME_SAMPLES)
            val mixedBuf = ShortArray(FRAME_SAMPLES)

            while (isActive) {
                val micSamples = micRecord?.read(micBuf, 0, FRAME_SAMPLES) ?: 0
                val devSamples = if (deviceRecord != null) {
                    deviceRecord?.read(devBuf, 0, FRAME_SAMPLES, AudioRecord.READ_NON_BLOCKING) ?: 0
                } else 0

                val micCfg = channelConfigs[AudioChannelId.MIC_AUX.name]
                val devCfg = channelConfigs[AudioChannelId.DEVICE_AUDIO.name]

                val (micRms, micPeak) = if (micSamples > 0) {
                    processChannel(micBuf, micSamples, micCfg)
                } else {
                    -60f to -60f
                }

                val (devRms, devPeak) = if (devSamples > 0) {
                    processChannel(devBuf, devSamples, devCfg)
                } else {
                    -60f to -60f
                }

                // Mix processed channels for AAC encoder
                val outSamples = max(micSamples, devSamples).coerceAtMost(FRAME_SAMPLES)
                if (outSamples > 0) {
                    for (i in 0 until outSamples) {
                        val m = if (i < micSamples) micBuf[i].toInt() else 0
                        val d = if (i < devSamples) devBuf[i].toInt() else 0
                        mixedBuf[i] = (m + d).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    onMixedPcmFrame?.invoke(mixedBuf, outSamples)
                }

                emitMeters(micRms, micPeak, devRms, devPeak)

                if (micSamples <= 0) {
                    kotlinx.coroutines.delay(25)
                }
            }
        }
    }

    private fun processChannel(
        buffer: ShortArray,
        count: Int,
        config: AudioChannelConfig?
    ): Pair<Float, Float> {
        if (config?.isMuted == true) {
            for (i in 0 until count) buffer[i] = 0
            return -60f to -60f
        }

        val gainLinear = 10.0.pow(((config?.gainDb ?: 0f) / 20f).toDouble()).toFloat()
        var sumSq = 0.0
        var maxAbs = 0

        for (i in 0 until count) {
            val scaled = (buffer[i] * gainLinear).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = scaled.toShort()
            val absVal = kotlin.math.abs(scaled)
            if (absVal > maxAbs) maxAbs = absVal
            sumSq += scaled.toDouble() * scaled.toDouble()
        }

        val rms = sqrt(sumSq / count).toFloat()
        var rmsDb = if (rms > 1f) (20f * log10(rms / 32768f)).coerceIn(-60f, 0f) else -60f
        var peakDb = if (maxAbs > 1) (20f * log10(maxAbs / 32768f)).coerceIn(-60f, 0f) else -60f

        // Real Noise Gate DSP
        if (config?.noiseGateEnabled == true && rmsDb < config.noiseGateThresholdDb) {
            for (i in 0 until count) buffer[i] = 0
            rmsDb = -60f
            peakDb = -60f
        }

        return rmsDb to peakDb
    }

    private fun emitMeters(
        micRmsDb: Float,
        micPeakDb: Float,
        devRmsDb: Float,
        devPeakDb: Float
    ) {
        val alertCfg = channelConfigs[AudioChannelId.ALERTS_MEDIA.name]
        val currentAlert = if (alertCfg?.isMuted == true) -60f else alertPulseDb
        alertPulseDb = max(-60f, alertPulseDb - 2.2f)

        _meterReadings.value = mapOf(
            AudioChannelId.DEVICE_AUDIO.name to AudioMeterReading(
                channelId = AudioChannelId.DEVICE_AUDIO.name,
                rmsDb = devRmsDb,
                peakDb = devPeakDb,
                isClipping = devPeakDb > -1.0f
            ),
            AudioChannelId.MIC_AUX.name to AudioMeterReading(
                channelId = AudioChannelId.MIC_AUX.name,
                rmsDb = micRmsDb,
                peakDb = micPeakDb,
                isClipping = micPeakDb > -1.0f
            ),
            AudioChannelId.ALERTS_MEDIA.name to AudioMeterReading(
                channelId = AudioChannelId.ALERTS_MEDIA.name,
                rmsDb = currentAlert,
                peakDb = currentAlert,
                isClipping = false
            )
        )
    }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        runCatching { micRecord?.stop() }
        runCatching { micRecord?.release() }
        micRecord = null
        runCatching { deviceRecord?.stop() }
        runCatching { deviceRecord?.release() }
        deviceRecord = null
    }
}
