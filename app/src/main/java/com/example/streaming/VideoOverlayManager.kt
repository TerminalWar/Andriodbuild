package com.example.streaming

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.core.graphics.scale
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/**
 * Loops user-selected MP4/WebM video overlays smoothly with configurable playback speed
 * and publishes non-recycled frames into MediaProjectionHolder for Studio Preview & H.264 encoder.
 */
object VideoOverlayManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val speedMap = ConcurrentHashMap<String, Float>()

    fun ensurePlaying(context: Context, uriString: String, speed: Float = 1.0f) {
        if (uriString.isBlank()) return
        speedMap[uriString] = speed.coerceIn(0.25f, 2.5f)
        if (activeJobs.containsKey(uriString)) return

        val job = scope.launch {
            val retriever = MediaMetadataRetriever()
            var durationMs = 0L
            val ready = runCatching {
                retriever.setDataSource(context, uriString.toUri())
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.coerceAtLeast(500L) ?: 5000L
            }.isSuccess

            if (!ready) {
                runCatching { retriever.release() }
                activeJobs.remove(uriString)
                return@launch
            }

            var positionMs = 0L
            val baseStepMs = 66L // ~15 FPS smooth overlay decoding
            try {
                while (isActive) {
                    val frame = runCatching {
                        retriever.getFrameAtTime(
                            positionMs * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                    }.getOrNull()
                    if (frame != null) {
                        val scaled = scaleDownIfNeeded(frame, 640)
                        MediaProjectionHolder.updateVideoOverlayFrame(uriString, scaled)
                    }
                    val spd = speedMap[uriString] ?: 1.0f
                    positionMs = (positionMs + (baseStepMs * spd).roundToLong()) % durationMs
                    delay(baseStepMs)
                }
            } finally {
                runCatching { retriever.release() }
            }
        }
        activeJobs[uriString] = job
    }

    fun stopUnused(activeUris: Set<String>) {
        val keys = activeJobs.keys.toList()
        for (key in keys) {
            if (!activeUris.contains(key)) {
                activeJobs.remove(key)?.cancel()
                speedMap.remove(key)
            }
        }
    }

    private fun scaleDownIfNeeded(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return src.scale(nw, nh)
    }
}
