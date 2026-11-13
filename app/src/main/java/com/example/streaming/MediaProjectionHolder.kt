package com.example.streaming

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import androidx.core.graphics.createBitmap
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
    private var lastCaptureW = 0
    private var lastCaptureH = 0

    // Triple-buffered reusable screen bitmaps to eliminate GC churn & recycled-bitmap races
    private var screenBackBitmap: Bitmap? = null
    private var screenFrontBitmap: Bitmap? = null
    private var screenSpareBitmap: Bitmap? = null
    private var rawCopyBitmap: Bitmap? = null
    private val screenCanvas = Canvas()
    private val captureRect = Rect()
    private val latestScreenBitmap = AtomicReference<Bitmap?>(null)

    private val latestCameraBitmap = AtomicReference<Bitmap?>(null)
    private val videoOverlayFrames = ConcurrentHashMap<String, Bitmap>()

    private val _isScreenCapturing = MutableStateFlow(false)
    val isScreenCapturing: StateFlow<Boolean> = _isScreenCapturing.asStateFlow()

    fun getProjection(): MediaProjection? = mediaProjection

    fun getLatestScreenBitmap(): Bitmap? = latestScreenBitmap.get()

    fun updateCameraBitmap(bitmap: Bitmap?) {
        latestCameraBitmap.set(bitmap)
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

        val (captureW, captureH) = computeCaptureSize(width, height)
        lastCaptureW = captureW
        lastCaptureH = captureH
        captureRect.set(0, 0, captureW, captureH)

        val reader = createImageReader(captureW, captureH, handler)
        imageReader = reader

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

    @SuppressLint("WrongConstant")
    fun resizeVirtualDisplay(width: Int, height: Int, densityDpi: Int) {
        val display = virtualDisplay ?: return
        val thread = handlerThread ?: return
        if (!thread.isAlive || !_isScreenCapturing.value) return

        val (captureW, captureH) = computeCaptureSize(width, height)
        if (captureW == lastCaptureW && captureH == lastCaptureH) return

        lastCaptureW = captureW
        lastCaptureH = captureH
        captureRect.set(0, 0, captureW, captureH)

        val handler = Handler(thread.looper)
        val oldReader = imageReader
        val newReader = createImageReader(captureW, captureH, handler)
        imageReader = newReader
        latestScreenBitmap.set(null)

        runCatching {
            display.resize(captureW, captureH, densityDpi)
            display.surface = newReader.surface
        }
        runCatching { oldReader?.close() }
    }

    @SuppressLint("WrongConstant")
    private fun createImageReader(captureW: Int, captureH: Int, handler: Handler): ImageReader {
        return ImageReader.newInstance(captureW, captureH, PixelFormat.RGBA_8888, 3).apply {
            setOnImageAvailableListener({ r ->
                val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
                try {
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride.coerceAtLeast(4)
                    val rowStride = plane.rowStride
                    val paddedWidth = rowStride / pixelStride

                    if (screenBackBitmap == null || screenBackBitmap?.width != captureW || screenBackBitmap?.height != captureH) {
                        screenBackBitmap = createBitmap(captureW, captureH)
                    }
                    if (screenFrontBitmap == null || screenFrontBitmap?.width != captureW || screenFrontBitmap?.height != captureH) {
                        screenFrontBitmap = createBitmap(captureW, captureH)
                    }
                    if (screenSpareBitmap == null || screenSpareBitmap?.width != captureW || screenSpareBitmap?.height != captureH) {
                        screenSpareBitmap = createBitmap(captureW, captureH)
                    }

                    val target = screenBackBitmap!!
                    if (paddedWidth == captureW) {
                        // Zero-copy fast path directly into pooled back bitmap
                        target.copyPixelsFromBuffer(buffer)
                    } else {
                        if (rawCopyBitmap == null || rawCopyBitmap?.width != paddedWidth || rawCopyBitmap?.height != captureH) {
                            rawCopyBitmap = createBitmap(paddedWidth, captureH)
                        }
                        val src = rawCopyBitmap!!
                        src.copyPixelsFromBuffer(buffer)
                        screenCanvas.setBitmap(target)
                        screenCanvas.drawBitmap(src, captureRect, captureRect, null)
                    }

                    // Rotate 3 buffers: back -> latest(front) -> spare -> back (prevents recycled-bitmap race)
                    val oldFront = screenFrontBitmap
                    screenFrontBitmap = target
                    screenBackBitmap = screenSpareBitmap
                    screenSpareBitmap = oldFront
                    latestScreenBitmap.set(target)
                } catch (_: Throwable) {
                    // Transient frame drop
                } finally {
                    image.close()
                }
            }, handler)
        }
    }

    fun release() {
        _isScreenCapturing.value = false
        latestScreenBitmap.set(null)
        lastCaptureW = 0
        lastCaptureH = 0
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        runCatching { handlerThread?.quitSafely() }
        handlerThread = null
        rawCopyBitmap = null
        screenBackBitmap = null
        screenFrontBitmap = null
        screenSpareBitmap = null
    }
}
