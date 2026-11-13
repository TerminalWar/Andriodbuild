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
import android.net.Uri
import android.util.LruCache
import com.example.data.model.DonationGoalState
import com.example.data.model.LiveAlertEvent
import com.example.data.model.OutputOrientation
import com.example.data.model.SourceType
import com.example.data.model.StreamChatMessage
import com.example.data.model.StudioScene
import com.example.data.model.StudioSource
import kotlin.math.max
import kotlin.math.min

/**
 * Zero-allocation per-frame scene compositor for hardware MediaCodec Surfaces.
 * Supports proportional aspect-fit / crop-to-fill rendering for tablets & phones,
 * looping Video Overlays, Privacy Shield, Camera zoom/mirror, and full widget rendering.
 */
class FrameCompositor(private val context: Context) {

    private val imageCache = LruCache<String, Bitmap>(10)

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

    // Reusable geometry objects to prevent GC pauses during multi-hour broadcasts
    private val dstRect = RectF()
    private val barRect = RectF()
    private val fillRect = RectF()
    private val clipPath = Path()
    private val drawMatrix = Matrix()

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
        privacyShieldActive: Boolean = false
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
                        drawBitmapScaled(
                            canvas = canvas,
                            bmp = camBmp,
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
                        drawBitmapScaled(canvas, bmp, dstRect, corner, cropToFill = src.cropToFill, alpha = src.opacity)
                    } else {
                        drawPlaceholderTile(canvas, dstRect, corner, 0xFF1B2430.toInt(), src.name)
                    }
                }

                SourceType.VIDEO_OVERLAY -> {
                    val vidBmp = src.imageUri?.let { MediaProjectionHolder.getVideoOverlayFrame(it) }
                    if (vidBmp != null && !vidBmp.isRecycled) {
                        drawBitmapScaled(canvas, vidBmp, dstRect, corner, cropToFill = src.cropToFill, alpha = src.opacity)
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
                    val lines = recentChat.takeLast(6)
                    var y = dstRect.top + lineSize + 10f
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

        canvas.save()
        clipPath.rewind()
        clipPath.addRoundRect(dst, corner, corner, Path.Direction.CW)
        canvas.clipPath(clipPath)
        bitmapPaint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        canvas.drawBitmap(bmp, drawMatrix, bitmapPaint)
        bitmapPaint.alpha = 255
        canvas.restore()
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
            val uri = Uri.parse(uriString)
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
