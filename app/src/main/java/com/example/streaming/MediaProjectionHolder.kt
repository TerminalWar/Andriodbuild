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
import java.util.concurrent.atomic.AtomicReference

object MediaProjectionHolder {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null

    private val latestScreenBitmap = AtomicReference<Bitmap?>(null)
    private val latestCameraBitmap = AtomicReference<Bitmap?>(null)

    private val _isScreenCapturing = MutableStateFlow(false)
    val isScreenCapturing: StateFlow<Boolean> = _isScreenCapturing.asStateFlow()

    fun getProjection(): MediaProjection? = mediaProjection

    fun getLatestScreenBitmap(): Bitmap? = latestScreenBitmap.get()

    fun updateCameraBitmap(bitmap: Bitmap?) {
        latestCameraBitmap.set(bitmap)
    }

    fun getLatestCameraBitmap(): Bitmap? = latestCameraBitmap.get()

    @SuppressLint("WrongConstant")
    fun attachProjection(projection: MediaProjection, width: Int, height: Int, densityDpi: Int) {
        release()
        mediaProjection = projection

        val thread = HandlerThread("MobileLabsScreenCapture").apply { start() }
        handlerThread = thread
        val handler = Handler(thread.looper)

        val captureW = width.coerceIn(480, 1280)
        val captureH = height.coerceIn(480, 1280)

        val reader = ImageReader.newInstance(captureW, captureH, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        reader.setOnImageAvailableListener({ r ->
            val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * captureW

                val rawBitmap = Bitmap.createBitmap(
                    captureW + rowPadding / pixelStride,
                    captureH,
                    Bitmap.Config.ARGB_8888
                )
                rawBitmap.copyPixelsFromBuffer(buffer)
                val cropped = if (rowPadding > 0) {
                    Bitmap.createBitmap(rawBitmap, 0, 0, captureW, captureH).also {
                        rawBitmap.recycle()
                    }
                } else {
                    rawBitmap
                }
                latestScreenBitmap.set(cropped)
            } catch (_: Throwable) {
                // Ignore transient buffer frame drops
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
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        runCatching { handlerThread?.quitSafely() }
        handlerThread = null
        latestScreenBitmap.set(null)
    }
}
