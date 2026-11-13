package com.example.streaming

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import com.example.data.model.DonationGoalState
import com.example.data.model.LiveAlertEvent
import com.example.data.model.OutputOrientation
import com.example.data.model.SourceType
import com.example.data.model.StreamChatMessage
import com.example.data.model.StudioScene
import com.example.data.model.StudioSource

/**
 * Composites all visible sources of the active scene onto a hardware encoder Canvas
 * so the encoded RTMP stream and local MP4 recording contain the exact overlays,
 * webcam frames, alerts, goal bars, and chatbox seen in the studio preview.
 */
class FrameCompositor(private val context: Context) {

    private val imageCache = mutableMapOf<String, Bitmap?>()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFCBD5E1.toInt()
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    fun renderFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        orientation: OutputOrientation,
        scene: StudioScene?,
        sources: List<StudioSource>,
        activeAlert: LiveAlertEvent?,
        donationGoal: DonationGoalState,
        recentChat: List<StreamChatMessage>
    ) {
        val baseColor = scene?.backdropHex?.toInt() ?: 0xFF0C1520.toInt()
        canvas.drawColor(baseColor)

        // Subtle studio grid backdrop when no full-screen capture is active
        bgPaint.color = 0x1831C3A2
        bgPaint.strokeWidth = 1f
        val step = (width / 12f).coerceAtLeast(40f)
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

        // Draw bottom-to-top (highest sortOrder drawn first as background, index 0 on top)
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
            val dst = RectF(
                rectNorm.x * width,
                rectNorm.y * height,
                (rectNorm.x + rectNorm.width) * width,
                (rectNorm.y + rectNorm.height) * height
            )
            val corner = (src.cornerRadiusDp * (width / 720f)).coerceAtLeast(0f)

            when (src.type) {
                SourceType.SCREEN_CAPTURE -> {
                    val bmp = MediaProjectionHolder.getLatestScreenBitmap()
                    if (bmp != null && !bmp.isRecycled) {
                        canvas.drawBitmap(bmp, null, dst, null)
                    } else {
                        drawPlaceholderTile(canvas, dst, corner, 0xFF111B27.toInt(), "SCREEN CAPTURE STANDBY")
                    }
                }

                SourceType.CAMERA -> {
                    val camBmp = MediaProjectionHolder.getLatestCameraBitmap()
                    if (camBmp != null && !camBmp.isRecycled) {
                        canvas.save()
                        val path = android.graphics.Path().apply {
                            addRoundRect(dst, corner, corner, android.graphics.Path.Direction.CW)
                        }
                        canvas.clipPath(path)
                        canvas.drawBitmap(camBmp, null, dst, null)
                        canvas.restore()
                    } else {
                        drawPlaceholderTile(canvas, dst, corner, 0xFF1A142B.toInt(), "CAMERA FEED")
                    }
                }

                SourceType.WEBCAM_FRAME -> {
                    framePaint.style = Paint.Style.STROKE
                    framePaint.strokeWidth = (6f * (width / 720f)).coerceAtLeast(3f)
                    framePaint.color = src.accentColorHex.toInt()
                    canvas.drawRoundRect(dst, corner, corner, framePaint)
                    framePaint.style = Paint.Style.FILL
                }

                SourceType.COLOR_SOURCE -> {
                    framePaint.style = Paint.Style.FILL
                    framePaint.color = src.accentColorHex.toInt()
                    framePaint.alpha = (src.opacity * 255).toInt().coerceIn(0, 255)
                    canvas.drawRoundRect(dst, corner, corner, framePaint)
                    framePaint.alpha = 255
                }

                SourceType.IMAGE_OVERLAY -> {
                    val bmp = src.imageUri?.let { loadImageBitmap(it) }
                    if (bmp != null && !bmp.isRecycled) {
                        canvas.drawBitmap(bmp, null, dst, null)
                    } else {
                        drawPlaceholderTile(canvas, dst, corner, 0xFF1B2430.toInt(), src.name)
                    }
                }

                SourceType.TEXT_BANNER -> {
                    framePaint.style = Paint.Style.FILL
                    framePaint.color = 0xCC0D131B.toInt()
                    canvas.drawRoundRect(dst, corner, corner, framePaint)
                    framePaint.style = Paint.Style.STROKE
                    framePaint.strokeWidth = 2f
                    framePaint.color = src.accentColorHex.toInt()
                    canvas.drawRoundRect(dst, corner, corner, framePaint)
                    framePaint.style = Paint.Style.FILL

                    val lines = (src.textContent.ifBlank { src.name }).split("\n")
                    textPaint.textSize = (dst.height() * 0.24f).coerceIn(18f, 44f)
                    textPaint.textAlign = Paint.Align.CENTER
                    val centerX = dst.centerX()
                    val firstY = dst.centerY() - ((lines.size - 1) * textPaint.textSize * 0.65f)
                    lines.forEachIndexed { idx, line ->
                        canvas.drawText(line, centerX, firstY + idx * textPaint.textSize * 1.25f, textPaint)
                    }
                }

                SourceType.ALERTBOX -> {
                    if (activeAlert != null) {
                        framePaint.style = Paint.Style.FILL
                        framePaint.color = 0xEA111822.toInt()
                        canvas.drawRoundRect(dst, corner, corner, framePaint)
                        framePaint.style = Paint.Style.STROKE
                        framePaint.strokeWidth = 3f
                        framePaint.color = activeAlert.accentHex.toInt()
                        canvas.drawRoundRect(dst, corner, corner, framePaint)
                        framePaint.style = Paint.Style.FILL

                        textPaint.textAlign = Paint.Align.CENTER
                        textPaint.textSize = (dst.height() * 0.30f).coerceIn(16f, 34f)
                        canvas.drawText(activeAlert.title, dst.centerX(), dst.centerY() - 4f, textPaint)
                        subTextPaint.textAlign = Paint.Align.CENTER
                        subTextPaint.textSize = (dst.height() * 0.20f).coerceIn(12f, 22f)
                        canvas.drawText(activeAlert.subtitle, dst.centerX(), dst.centerY() + subTextPaint.textSize + 6f, subTextPaint)
                    }
                }

                SourceType.DONATION_GOAL -> {
                    framePaint.style = Paint.Style.FILL
                    framePaint.color = 0xD90E151E.toInt()
                    canvas.drawRoundRect(dst, corner, corner, framePaint)

                    val pct = (donationGoal.currentAmount / donationGoal.targetAmount.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val barRect = RectF(
                        dst.left + 12f,
                        dst.centerY() + 4f,
                        dst.right - 12f,
                        dst.bottom - 10f
                    )
                    framePaint.color = 0xFF1E293B.toInt()
                    canvas.drawRoundRect(barRect, 8f, 8f, framePaint)

                    val fillRect = RectF(
                        barRect.left,
                        barRect.top,
                        barRect.left + barRect.width() * pct,
                        barRect.bottom
                    )
                    framePaint.shader = LinearGradient(
                        fillRect.left, fillRect.top, fillRect.right, fillRect.bottom,
                        src.accentColorHex.toInt(), 0xFF56E3C2.toInt(), Shader.TileMode.CLAMP
                    )
                    canvas.drawRoundRect(fillRect, 8f, 8f, framePaint)
                    framePaint.shader = null

                    textPaint.textAlign = Paint.Align.LEFT
                    textPaint.textSize = (dst.height() * 0.28f).coerceIn(13f, 24f)
                    canvas.drawText(
                        "${donationGoal.title} — $${donationGoal.currentAmount.toInt()}/$${donationGoal.targetAmount.toInt()}",
                        dst.left + 14f,
                        dst.centerY() - 4f,
                        textPaint
                    )
                }

                SourceType.CHATBOX -> {
                    framePaint.style = Paint.Style.FILL
                    framePaint.color = 0xB80B1016.toInt()
                    canvas.drawRoundRect(dst, corner, corner, framePaint)
                    subTextPaint.textAlign = Paint.Align.LEFT
                    val lineSize = (dst.height() * 0.085f).coerceIn(12f, 20f)
                    subTextPaint.textSize = lineSize
                    val lines = recentChat.takeLast(6)
                    var y = dst.top + lineSize + 10f
                    for (msg in lines) {
                        if (y > dst.bottom - 8f) break
                        subTextPaint.color = msg.nameColorHex.toInt()
                        val prefix = "${msg.username}: "
                        canvas.drawText(prefix, dst.left + 10f, y, subTextPaint)
                        val prefixW = subTextPaint.measureText(prefix)
                        subTextPaint.color = Color.WHITE
                        val maxChars = ((dst.width() - prefixW - 20f) / (lineSize * 0.55f)).toInt().coerceAtLeast(8)
                        val body = if (msg.message.length > maxChars) msg.message.take(maxChars - 1) + "…" else msg.message
                        canvas.drawText(body, dst.left + 10f + prefixW, y, subTextPaint)
                        y += lineSize * 1.45f
                    }
                    subTextPaint.color = 0xFFCBD5E1.toInt()
                }

                SourceType.FOLDER -> Unit
            }
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
        if (imageCache.containsKey(uriString)) return imageCache[uriString]
        val bmp = runCatching {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
        imageCache[uriString] = bmp
        return bmp
    }
}
