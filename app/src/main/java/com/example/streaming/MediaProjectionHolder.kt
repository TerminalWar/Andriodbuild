package com.example.streaming

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.roundToInt

object MediaProjectionHolder {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null

    // Double-buffered reusable screen bitmaps to eliminate GC churn over multi-hour streams
    private var screenBackBitmap: Bitmap? = null
    private var screenFrontBitmap: Bitmap? = null
    private var rawCopyBitmap: Bitmap? = null
    private val screenCanvas = android.graphics.Canvas()
    private val captureRect = android.graphics.Rect()
    private val latestScreenBitmap = AtomicReference<Bitmap?>(null)

    private val latestCameraBitmap = AtomicReference<Bitmap?>(null)
    private val videoOverlayFrames = ConcurrentHashMap<String, Bitmap>()

    private val _isScreenCapturing = MutableStateFlow(false)
    val isScreenCapturing: StateFlow<Boolean> = _isScreenCapturing.asStateFlow()

    fun getProjection(): MediaProjection? = mediaProjection

    fun getLatestScreenBitmap(): Bitmap? = latestScreenBitmap.get()

    fun updateCameraBitmap(bitmap: Bitmap?) {
        val prev = latestCameraBitmap.getAndSet(bitmap)
        if (prev != null && prev !== bitmap && !prev.isRecycled) {
            // GC handles small camera previews safely while UI may still reference prev for 1 frame
        }
    }

    fun getLatestCameraBitmap(): Bitmap? = latestCameraBitmap.get()

    fun updateVideoOverlayFrame(uri: String, bitmap: Bitmap) {
        videoOverlayFrames[uri] = bitmap
    }

    fun getVideoOverlayFrame(uri: String): Bitmap? = videoOverlayFrames[uri]

    internal fun computeCaptureSize(width: Int, height: Int, maxEdge: Int = 1280): Pair<Int, Int> {
        val safeW = width.coerceAtLeast(320)
        val safeH = height.coerceAtLeast(320)
        val maxDim = max(safeW, safeH).toFloat()
        val scale = (maxEdge.toFloat() / maxDim).coerceAtMost(1f)
        val captureW = ((safeW * scale).roundToInt().coerceAtLeast(320)) and 1.inv()
        val captureH = ((safeH * scale).roundToInt().coerceAtLeast(320)) and 1.inv()
        return captureW to captureH
    }

    @SuppressLint("WrongConstant")
    fun attachProjection(projection: MediaProjection, width: Int, height: Int, densityDpi: Int) {
        release()
        mediaProjection = projection

        val thread = HandlerThread("MobileLabsScreenCapture").apply { start() }
        handlerThread = thread
        val handler = Handler(thread.looper)

        // Proportional scaling preserving exact tablet/phone aspect ratio (even dimensions for H.264)
        val (captureW, captureH) = computeCaptureSize(width, height)
        captureRect.set(0, 0, captureW, captureH)

        val reader = ImageReader.newInstance(captureW, captureH, PixelFormat.RGBA_8888, 3)
        imageReader = reader

        reader.setOnImageAvailableListener({ r ->
            val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val paddedWidth = rowStride / pixelStride

                if (rawCopyBitmap == null || rawCopyBitmap?.width != paddedWidth || rawCopyBitmap?.height != captureH) {
                    rawCopyBitmap?.recycle()
                    rawCopyBitmap = Bitmap.createBitmap(paddedWidth, captureH, Bitmap.Config.ARGB_8888)
                }
                rawCopyBitmap?.copyPixelsFromBuffer(buffer)

                if (screenBackBitmap == null || screenBackBitmap?.width != captureW || screenBackBitmap?.height != captureH) {
                    screenBackBitmap?.recycle()
                    screenBackBitmap = Bitmap.createBitmap(captureW, captureH, Bitmap.Config.ARGB_8888)
                }
                if (screenFrontBitmap == null || screenFrontBitmap?.width != captureW || screenFrontBitmap?.height != captureH) {
                    screenFrontBitmap?.recycle()
                    screenFrontBitmap = Bitmap.createBitmap(captureW, captureH, Bitmap.Config.ARGB_8888)
                }

                val target = screenBackBitmap!!
                val src = rawCopyBitmap!!
                screenCanvas.setBitmap(target)
                screenCanvas.drawBitmap(src, captureRect, captureRect, null)
                // Swap back & front pointers cleanly
                screenBackBitmap = screenFrontBitmap
                screenFrontBitmap = target
                latestScreenBitmap.set(target)
            } catch (_: Throwable) {
                // Non-fatal transient frame drop
            } finally {
                image.close()
            }
        }, handler)

        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                release()
            }
        }, handler)

        virtualDisplay = projection.createVirtualDisplay(
            "mobileLabsScreenCapture",
            captureW,
            captureH,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )
        _isScreenCapturing.value = true
    }

    fun release() {
        _isScreenCapturing.value = false
        latestScreenBitmap.set(null)
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        runCatching { handlerThread?.quitSafely() }
        handlerThread = null
        runCatching { rawCopyBitmap?.recycle() }
        runCatching { screenBackBitmap?.recycle() }
        runCatching { screenFrontBitmap?.recycle() }
        rawCopyBitmap = null
        screenBackBitmap = null
        screenFrontBitmap = null
    }
}
