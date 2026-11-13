package com.example.streaming

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Loops user-selected MP4/WebM video overlays smoothly and publishes frames into
 * MediaProjectionHolder so both the Studio Preview and the H.264 encoder render animated overlays.
 */
object VideoOverlayManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun ensurePlaying(context: Context, uriString: String) {
        if (uriString.isBlank() || activeJobs.containsKey(uriString)) return
        val job = scope.launch {
            val retriever = MediaMetadataRetriever()
            var durationMs = 0L
            val ready = runCatching {
                retriever.setDataSource(context, Uri.parse(uriString))
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.coerceAtLeast(1000L) ?: 5000L
            }.isSuccess

            if (!ready) {
                runCatching { retriever.release() }
                activeJobs.remove(uriString)
                return@launch
            }

            var positionMs = 0L
            val stepMs = 80L // ~12.5 FPS smooth overlay frame update without starving encoder
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
                    positionMs = (positionMs + stepMs) % durationMs
                    delay(stepMs)
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
        return Bitmap.createScaledBitmap(src, nw, nh, true).also {
            if (it !== src) src.recycle()
        }
    }
}
