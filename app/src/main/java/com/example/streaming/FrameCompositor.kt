package com.example.streaming

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withSave
import androidx.core.net.toUri
import com.example.data.model.DonationGoalState
import com.example.data.model.LiveAlertEvent
import com.example.data.model.OutputOrientation
import com.example.data.model.SourceType
import com.example.data.model.StreamChatMessage
import com.example.data.model.StudioScene
import com.example.data.model.StudioSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Zero-allocation per-frame scene compositor for hardware MediaCodec Surfaces.
 * Supports proportional aspect-fit / crop-to-fill rendering for tablets & phones,
 * looping Video Overlays, Privacy Shield, Camera zoom/mirror, real Chroma Key
 * alpha keying, Stream Uptime & Clock widget, pinned chat banner, and full widget rendering.
 */
class FrameCompositor(private val context: Context) {

    private val imageCache = LruCache<String, Bitmap>(12)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFCBD5E1.toInt()
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    // Reusable geometry & chroma-key buffers to prevent GC pauses during multi-hour broadcasts
    private val dstRect = RectF()
    private val barRect = RectF()
    private val fillRect = RectF()
    private val badgeRect = RectF()
    private val clipPath = Path()
    private val drawMatrix = Matrix()

    private var chromaScratchBitmap: Bitmap? = null
    private var chromaPixelBuffer: IntArray = IntArray(0)
    private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private var cachedGoalKey = ""
    private var cachedGoalShader: LinearGradient? = null

    fun renderFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        orientation: OutputOrientation,
        scene: StudioScene?,
        sources: List<StudioSource>,
        activeAlert: LiveAlertEvent?,
        donationGoal: DonationGoalState,
        recentChat: List<StreamChatMessage>,
        privacyShieldActive: Boolean = false,
        liveElapsedSec: Long = 0L,
        currentFpsTelemetry: Float = 0f,
        pinnedChatMessage: StreamChatMessage? = null
    ) {
        val baseColor = scene?.backdropHex?.toInt() ?: 0xFF0C1520.toInt()
        canvas.drawColor(baseColor)

        bgPaint.color = 0x1631C3A2
        bgPaint.strokeWidth = 1f
        val step = (width / 12f).coerceAtLeast(48f)
        var gx = 0f
        while (gx < width) {
            canvas.drawLine(gx, 0f, gx, height.toFloat(), bgPaint)
            gx += step
        }
        var gy = 0f
        while (gy < height) {
            canvas.drawLine(0f, gy, width.toFloat(), gy, bgPaint)
            gy += step
        }

        val ordered = sources.sortedByDescending { it.sortOrder }
        val folderVisibility = sources
            .filter { it.type == SourceType.FOLDER }
            .associate {
                it.id to if (orientation == OutputOrientation.HORIZONTAL) it.isVisibleHorizontal else it.isVisibleVertical
            }

        for (src in ordered) {
            if (src.type == SourceType.FOLDER) continue
            val visible = if (orientation == OutputOrientation.HORIZONTAL) {
                src.isVisibleHorizontal
            } else {
                src.isVisibleVertical
            }
            if (!visible) continue
            if (src.parentFolderId != null && folderVisibility[src.parentFolderId] == false) continue

            val rectNorm = if (orientation == OutputOrientation.HORIZONTAL) src.hRect else src.vRect
            dstRect.set(
                rectNorm.x * width,
                rectNorm.y * height,
                (rectNorm.x + rectNorm.width) * width,
                (rectNorm.y + rectNorm.height) * height
            )
            val corner = (src.cornerRadiusDp * (width / 720f)).coerceAtLeast(0f)

            when (src.type) {
                SourceType.SCREEN_CAPTURE -> {
                    if (privacyShieldActive) {
                        drawPlaceholderTile(canvas, dstRect, corner, 0xFF0D141D.toInt(), "PRIVACY SHIELD ACTIVE")
                    } else {
                        val bmp = MediaProjectionHolder.getLatestScreenBitmap()
                        if (bmp != null && !bmp.isRecycled) {
                            drawBitmapScaled(canvas, bmp, dstRect, corner, cropToFill = src.cropToFill, alpha = src.opacity)
                        } else {
                            drawPlaceholderTile(canvas, dstRect, corner, 0xFF111B27.toInt(), "SCREEN CAPTURE STANDBY")
                        }
                    }
                }

                SourceType.CAMERA -> {
                    val camBmp = MediaProjectionHolder.getLatestCameraBitmap()
                    if (camBmp != null && !camBmp.isRecycled) {
                        val drawBmp = if (src.chromaKeyEnabled) {
                            applyChromaKey(camBmp, src.chromaKeyColorHex.toInt(), src.chromaKeyTolerance)
                        } else {
                            camBmp
                        }
                        drawBitmapScaled(
                            canvas = canvas,
                            bmp = drawBmp,
                            dst = dstRect,
                            corner = corner,
                            cropToFill = src.cropToFill,
                            alpha = src.opacity,
                            zoom = src.cameraZoom.coerceIn(1f, 4f)
                        )
                    } else {
                        drawPlaceholderTile(canvas, dstRect, corner, 0xFF1A142B.toInt(), "CAMERA FEED")
                    }
                }

                SourceType.WEBCAM_FRAME -> {
                    framePaint.style = Paint.Style.STROKE
                    framePaint.strokeWidth = (6f * (width / 720f)).coerceAtLeast(3f)
                    framePaint.color = src.accentColorHex.toInt()
                    canvas.drawRoundRect(dstRect, corner, corner, framePaint)
                    framePaint.style = Paint.Style.FILL
                }

                SourceType.COLOR_SOURCE -> {
                    framePaint.style = Paint.Style.FILL
                    framePaint.color = src.accentColorHex.toInt()
                    framePaint.alpha = (src.opacity * 255).toInt().coerceIn(0, 255)
                    canvas.drawRoundRect(dstRect, corner, corner, framePaint)
                    framePaint.alpha = 255
                }

                SourceType.IMAGE_OVERLAY -> {
                    val bmp = src.imageUri?.let { loadImageBitmap(it) }
                    if (bmp != null && !bmp.isRecycled) {
                        val drawBmp = if (src.chromaKeyEnabled) {
                            applyChromaKey(bmp, src.chromaKeyColorHex.toInt(), src.chromaKeyTolerance)
                        } else {
                            bmp
                        }
                        drawBitmapScaled(canvas, drawBmp, dstRect, corner, cropToFill = src.cropToFill, alpha = src.opacity)
                    } else {
                        drawPlaceholderTile(canvas, dstRect, corner, 0xFF1B2430.toInt(), src.name)
                    }
                }

                SourceType.VIDEO_OVERLAY -> {
                    val vidBmp = src.imageUri?.let { MediaProjectionHolder.getVideoOverlayFrame(it) }
                    if (vidBmp != null && !vidBmp.isRecycled) {
                        val drawBmp = if (src.chromaKeyEnabled) {
                            applyChromaKey(vidBmp, src.chromaKeyColorHex.toInt(), src.chromaKeyTolerance)
                        } else {
                            vidBmp
                        }
                        drawBitmapScaled(canvas, drawBmp, dstRect, corner, cropToFill = src.cropToFill, alpha = src.opacity)
                    } else {
                        drawPlaceholderTile(canvas, dstRect, corner, 0xFF14212E.toInt(), "VIDEO OVERLAY: ${src.name}")
                    }
                }

                SourceType.TEXT_BANNER -> {
                    framePaint.style = Paint.Style.FILL
                    framePaint.color = 0xCC0D131B.toInt()
                    canvas.drawRoundRect(dstRect, corner, corner, framePaint)
                    framePaint.style = Paint.Style.STROKE
                    framePaint.strokeWidth = 2f
                    framePaint.color = src.accentColorHex.toInt()
                    canvas.drawRoundRect(dstRect, corner, corner, framePaint)
                    framePaint.style = Paint.Style.FILL

                    val lines = (src.textContent.ifBlank { src.name }).split("\n")
                    textPaint.textSize = (dstRect.height() * 0.24f).coerceIn(18f, 44f)
                    textPaint.textAlign = Paint.Align.CENTER
                    val centerX = dstRect.centerX()
                    val firstY = dstRect.centerY() - ((lines.size - 1) * textPaint.textSize * 0.65f)
                    lines.forEachIndexed { idx, line ->
                        canvas.drawText(line, centerX, firstY + idx * textPaint.textSize * 1.25f, textPaint)
                    }
                }

                SourceType.STREAM_TIMER -> {
                    framePaint.style = Paint.Style.FILL
                    framePaint.color = 0xD40D131B.toInt()
                    canvas.drawRoundRect(dstRect, corner, corner, framePaint)
                    framePaint.style = Paint.Style.STROKE
                    framePaint.strokeWidth = 2f
                    framePaint.color = src.accentColorHex.toInt()
                    canvas.drawRoundRect(dstRect, corner, corner, framePaint)
                    framePaint.style = Paint.Style.FILL

                    // Status dot + elapsed uptime + local clock
                    val dotRadius = (dstRect.height() * 0.14f).coerceIn(5f, 11f)
                    framePaint.color = if (liveElapsedSec > 0L) 0xFFFF4557.toInt() else src.accentColorHex.toInt()
                    canvas.drawCircle(dstRect.left + dotRadius * 2f + 6f, dstRect.centerY(), dotRadius, framePaint)

                    val h = liveElapsedSec / 3600
                    val m = (liveElapsedSec % 3600) / 60
                    val s = liveElapsedSec % 60
                    val uptime = String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
                    val prefix = src.textContent.ifBlank { if (liveElapsedSec > 0L) "LIVE" else "UPTIME" }

                    textPaint.textAlign = Paint.Align.LEFT
                    textPaint.textSize = (dstRect.height() * 0.34f).coerceIn(14f, 30f)
                    canvas.drawText(
                        "$prefix  $uptime",
                        dstRect.left + dotRadius * 3.8f + 10f,
                        dstRect.centerY() + textPaint.textSize * 0.32f,
                        textPaint
                    )

                    subTextPaint.textAlign = Paint.Align.RIGHT
                    subTextPaint.textSize = (dstRect.height() * 0.24f).coerceIn(11f, 20f)
                    val rightInfo = if (currentFpsTelemetry > 0f) {
                        "${currentFpsTelemetry.toInt()} FPS • ${clockFormat.format(Date())}"
                    } else {
                        clockFormat.format(Date())
                    }
                    canvas.drawText(rightInfo, dstRect.right - 14f, dstRect.centerY() + subTextPaint.textSize * 0.32f, subTextPaint)
                }

                SourceType.ALERTBOX -> {
                    if (activeAlert != null) {
                        framePaint.style = Paint.Style.FILL
                        framePaint.color = 0xEA111822.toInt()
                        canvas.drawRoundRect(dstRect, corner, corner, framePaint)
                        framePaint.style = Paint.Style.STROKE
                        framePaint.strokeWidth = 3f
                        framePaint.color = activeAlert.accentHex.toInt()
                        canvas.drawRoundRect(dstRect, corner, corner, framePaint)
                        framePaint.style = Paint.Style.FILL

                        textPaint.textAlign = Paint.Align.CENTER
                        textPaint.textSize = (dstRect.height() * 0.30f).coerceIn(16f, 34f)
                        canvas.drawText(activeAlert.title, dstRect.centerX(), dstRect.centerY() - 4f, textPaint)
                        subTextPaint.textAlign = Paint.Align.CENTER
                        subTextPaint.textSize = (dstRect.height() * 0.20f).coerceIn(12f, 22f)
                        canvas.drawText(
                            activeAlert.subtitle,
                            dstRect.centerX(),
                            dstRect.centerY() + subTextPaint.textSize + 6f,
                            subTextPaint
                        )
                    }
                }

                SourceType.DONATION_GOAL -> {
                    framePaint.style = Paint.Style.FILL
                    framePaint.color = 0xD90E151E.toInt()
                    canvas.drawRoundRect(dstRect, corner, corner, framePaint)

                    val pct = (donationGoal.currentAmount / donationGoal.targetAmount.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    barRect.set(
                        dstRect.left + 12f,
                        dstRect.centerY() + 4f,
                        dstRect.right - 12f,
                        dstRect.bottom - 10f
                    )
                    framePaint.color = 0xFF1E293B.toInt()
                    canvas.drawRoundRect(barRect, 8f, 8f, framePaint)

                    fillRect.set(
                        barRect.left,
                        barRect.top,
                        barRect.left + barRect.width() * pct,
                        barRect.bottom
                    )
                    val key = "${fillRect.left.toInt()}_${fillRect.right.toInt()}_${src.accentColorHex}"
                    if (key != cachedGoalKey || cachedGoalShader == null) {
                        cachedGoalKey = key
                        cachedGoalShader = LinearGradient(
                            fillRect.left, fillRect.top, fillRect.right.coerceAtLeast(fillRect.left + 1f), fillRect.bottom,
                            src.accentColorHex.toInt(), 0xFF56E3C2.toInt(), Shader.TileMode.CLAMP
                        )
                    }
                    framePaint.shader = cachedGoalShader
                    canvas.drawRoundRect(fillRect, 8f, 8f, framePaint)
                    framePaint.shader = null

                    textPaint.textAlign = Paint.Align.LEFT
                    textPaint.textSize = (dstRect.height() * 0.28f).coerceIn(13f, 24f)
                    canvas.drawText(
                        "${donationGoal.title} — $${donationGoal.currentAmount.toInt()}/$${donationGoal.targetAmount.toInt()}",
                        dstRect.left + 14f,
                        dstRect.centerY() - 4f,
                        textPaint
                    )
                }

                SourceType.CHATBOX -> {
                    framePaint.style = Paint.Style.FILL
                    framePaint.color = 0xB80B1016.toInt()
                    canvas.drawRoundRect(dstRect, corner, corner, framePaint)
                    subTextPaint.textAlign = Paint.Align.LEFT
                    val lineSize = (dstRect.height() * 0.085f).coerceIn(12f, 20f)
                    subTextPaint.textSize = lineSize
                    var y = dstRect.top + lineSize + 10f

                    if (pinnedChatMessage != null && dstRect.height() > lineSize * 3f) {
                        badgeRect.set(dstRect.left + 6f, dstRect.top + 6f, dstRect.right - 6f, dstRect.top + lineSize * 1.9f)
                        framePaint.color = 0x3331C3A2
                        canvas.drawRoundRect(badgeRect, 6f, 6f, framePaint)
                        subTextPaint.color = 0xFF56E3C2.toInt()
                        canvas.drawText(
                            "PIN ${pinnedChatMessage.username}: ${pinnedChatMessage.message.take(36)}",
                            dstRect.left + 12f,
                            dstRect.top + lineSize * 1.3f,
                            subTextPaint
                        )
                        y = badgeRect.bottom + lineSize + 4f
                    }

                    val lines = recentChat.takeLast(6)
                    for (msg in lines) {
                        if (y > dstRect.bottom - 8f) break
                        subTextPaint.color = msg.nameColorHex.toInt()
                        val prefix = "${msg.username}: "
                        canvas.drawText(prefix, dstRect.left + 10f, y, subTextPaint)
                        val prefixW = subTextPaint.measureText(prefix)
                        subTextPaint.color = Color.WHITE
                        val maxChars = ((dstRect.width() - prefixW - 20f) / (lineSize * 0.55f)).toInt().coerceAtLeast(8)
                        val body = if (msg.message.length > maxChars) msg.message.take(maxChars - 1) + "…" else msg.message
                        canvas.drawText(body, dstRect.left + 10f + prefixW, y, subTextPaint)
                        y += lineSize * 1.45f
                    }
                    subTextPaint.color = 0xFFCBD5E1.toInt()
                }

                SourceType.FOLDER -> Unit
            }
        }
    }

    /**
     * Real-time Chroma Key processor reusing a single scratch bitmap & pixel array.
     * Supports standard broadcast Green Screen (#00B140) or Blue Screen (#0047AB).
     */
    private fun applyChromaKey(source: Bitmap, keyColor: Int, tolerance: Float): Bitmap {
        val sw = source.width
        val sh = source.height
        if (sw <= 1 || sh <= 1) return source

        // Downscale if huge to guarantee <2ms frame budgets
        val scale = if (sw > 480) 480f / sw.toFloat() else 1f
        val tw = max(2, (sw * scale).toInt())
        val th = max(2, (sh * scale).toInt())

        val scratch = chromaScratchBitmap?.takeIf {
            !it.isRecycled && it.width == tw && it.height == th
        } ?: createBitmap(tw, th).also {
            chromaScratchBitmap = it
        }

        val scaledInput = if (tw != sw || th != sh) {
            source.scale(tw, th, false)
        } else {
            source
        }

        val total = tw * th
        if (chromaPixelBuffer.size < total) {
            chromaPixelBuffer = IntArray(total)
        }
        scaledInput.getPixels(chromaPixelBuffer, 0, tw, 0, 0, tw, th)

        val kr = Color.red(keyColor)
        val kg = Color.green(keyColor)
        val kb = Color.blue(keyColor)
        val tolPx = (tolerance.coerceIn(0.05f, 0.7f) * 255f).toInt()
        val featherPx = tolPx + 38

        for (i in 0 until total) {
            val c = chromaPixelBuffer[i]
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val dist = (abs(r - kr) + abs(g - kg) + abs(b - kb)) / 3
            if (dist < tolPx) {
                chromaPixelBuffer[i] = 0x00000000
            } else if (dist < featherPx) {
                val origA = (c ushr 24) and 0xFF
                val alphaFactor = (dist - tolPx).toFloat() / 38f
                val newA = (origA * alphaFactor).toInt().coerceIn(0, 255)
                chromaPixelBuffer[i] = (c and 0x00FFFFFF) or (newA shl 24)
            }
        }
        scratch.setPixels(chromaPixelBuffer, 0, tw, 0, 0, tw, th)
        return scratch
    }

    private fun drawBitmapScaled(
        canvas: Canvas,
        bmp: Bitmap,
        dst: RectF,
        corner: Float,
        cropToFill: Boolean,
        alpha: Float,
        zoom: Float = 1f
    ) {
        val bw = bmp.width.toFloat().coerceAtLeast(1f)
        val bh = bmp.height.toFloat().coerceAtLeast(1f)
        val dw = dst.width().coerceAtLeast(1f)
        val dh = dst.height().coerceAtLeast(1f)

        val baseScale = if (cropToFill) {
            max(dw / bw, dh / bh)
        } else {
            min(dw / bw, dh / bh)
        } * zoom

        val scaledW = bw * baseScale
        val scaledH = bh * baseScale
        val dx = dst.left + (dw - scaledW) * 0.5f
        val dy = dst.top + (dh - scaledH) * 0.5f

        drawMatrix.reset()
        drawMatrix.postScale(baseScale, baseScale)
        drawMatrix.postTranslate(dx, dy)

        canvas.withSave {
            clipPath.rewind()
            clipPath.addRoundRect(dst, corner, corner, Path.Direction.CW)
            clipPath(clipPath)
            bitmapPaint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
            drawBitmap(bmp, drawMatrix, bitmapPaint)
            bitmapPaint.alpha = 255
        }
    }

    private fun drawPlaceholderTile(
        canvas: Canvas,
        dst: RectF,
        corner: Float,
        bgColor: Int,
        label: String
    ) {
        framePaint.style = Paint.Style.FILL
        framePaint.color = bgColor
        canvas.drawRoundRect(dst, corner, corner, framePaint)
        framePaint.style = Paint.Style.STROKE
        framePaint.strokeWidth = 2f
        framePaint.color = 0x5531C3A2
        canvas.drawRoundRect(dst, corner, corner, framePaint)
        framePaint.style = Paint.Style.FILL
        subTextPaint.textAlign = Paint.Align.CENTER
        subTextPaint.textSize = (dst.height() * 0.09f).coerceIn(14f, 24f)
        canvas.drawText(label, dst.centerX(), dst.centerY(), subTextPaint)
    }

    private fun loadImageBitmap(uriString: String): Bitmap? {
        imageCache.get(uriString)?.let { if (!it.isRecycled) return it }
        val bmp = runCatching {
            val uri = uriString.toUri()
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            val maxDim = max(opts.outWidth, opts.outHeight)
            var sample = 1
            while (maxDim / sample > 1280) sample *= 2
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
        }.getOrNull()
        if (bmp != null) imageCache.put(uriString, bmp)
        return bmp
    }
}
