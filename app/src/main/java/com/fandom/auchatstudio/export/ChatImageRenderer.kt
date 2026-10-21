package com.fandom.auchatstudio.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.fandom.auchatstudio.core.ColorMath
import com.fandom.auchatstudio.core.ExportPaging
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.NameRules
import com.fandom.auchatstudio.core.VoiceFormat
import com.fandom.auchatstudio.core.Watermark
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.ui.chat.ChatItem
import com.fandom.auchatstudio.ui.chat.ChatStyle
import com.fandom.auchatstudio.ui.chat.WaveformHeights
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a chat with plain android.graphics so it can run off-screen on a background thread.
 * Layout mirrors the Compose bubbles (sizes in dp, rendered at [WIDTH_PX] wide).
 */
class ChatImageRenderer(
    private val context: Context,
    private val chat: ChatStyle,
    private val showNames: Boolean,
) {
    private val s = WIDTH_PX / WIDTH_DP // px per dp
    private fun dp(v: Float) = v * s
    private fun Long.argb() = this.toInt()

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = dp(15f) }
    private val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = dp(12f) }
    private val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(12f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = chat.senderName.argb()
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.STROKE }

    private val avatarSize = dp(30f)
    private val sidePad = dp(12f)
    private val avatarGap = dp(8f)
    private val maxBubbleWidth = dp(WIDTH_DP - 12f * 2 - 30f - 8f - 48f)
    private val avatarCache = HashMap<Long, Bitmap?>()

    private interface Block {
        val height: Float
        fun draw(canvas: Canvas, top: Float)
    }

    fun render(headerTitle: String, partners: List<CharacterEntity>, items: List<ChatItem>): List<Bitmap> {
        val blocks = items.map(::measure)
        val header = HeaderBlock(headerTitle, partners)
        val footer = FooterBlock()
        val budget = (MAX_PAGE_HEIGHT_PX - header.height - footer.height - dp(24f)).toInt()
        val pages = ExportPaging.split(blocks.map { ceil(it.height).toInt() }, budget)
        try {
            return pages.map { range ->
                val content = range.sumOf { blocks[it].height.toDouble() }.toFloat()
                val height = ceil(header.height + dp(12f) + content + dp(12f) + footer.height).toInt()
                val bitmap = Bitmap.createBitmap(WIDTH_PX.toInt(), height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(chat.background.argb())
                header.draw(canvas, 0f)
                var y = header.height + dp(12f)
                for (i in range) {
                    blocks[i].draw(canvas, y)
                    y += blocks[i].height
                }
                footer.draw(canvas, height - footer.height)
                bitmap
            }
        } finally {
            avatarCache.values.forEach { it?.recycle() }
            avatarCache.clear()
        }
    }

    // region Measuring

    private fun measure(item: ChatItem): Block {
        if (item.isNarration) return NarrationBlock(item.message.text)
        val nameHeight = if (showNames && !item.outgoing && item.firstInRun) dp(16f) else 0f
        val timeHeight = if (item.message.timeLabel != null && item.lastInRun) dp(16f) else 0f
        val topGap = if (item.firstInRun) dp(8f) else dp(2f)
        val content: Content = when (item.message.type) {
            MessageType.VOICE -> VoiceContent(item)
            MessageType.IMAGE -> ImageContent(item.message.imagePath)
            else -> TextContent(item)
        }
        return BubbleBlock(item, content, topGap, nameHeight, timeHeight)
    }

    private interface Content {
        val width: Float
        val height: Float
        fun draw(canvas: Canvas, left: Float, top: Float)
    }

    private inner class TextContent(private val item: ChatItem) : Content {
        private val paint = TextPaint(textPaint).apply {
            color = (if (item.outgoing) chat.outgoingText else chat.incomingText).argb()
        }
        private val padH = dp(14f)
        private val padV = dp(9f)
        private val layout: StaticLayout = buildLayout(item.message.text, paint, (maxBubbleWidth - padH * 2).toInt())
        private val textWidth = (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: 0f
        override val width = ceil(textWidth) + padH * 2
        override val height = layout.height + padV * 2

        override fun draw(canvas: Canvas, left: Float, top: Float) {
            drawBubbleBackground(canvas, RectF(left, top, left + width, top + height), item)
            canvas.save()
            canvas.translate(left + padH, top + padV)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    private inner class VoiceContent(private val item: ChatItem) : Content {
        override val width = dp(208f)
        override val height = dp(48f)

        override fun draw(canvas: Canvas, left: Float, top: Float) {
            drawBubbleBackground(canvas, RectF(left, top, left + width, top + height), item)
            val color = (if (item.outgoing) chat.outgoingText else chat.incomingText).argb()
            val cy = top + height / 2
            fill.shader = null
            fill.color = color
            fill.alpha = 46
            canvas.drawCircle(left + dp(26f), cy, dp(14f), fill)
            fill.alpha = 255
            val tri = Path().apply {
                moveTo(left + dp(22f), cy - dp(6f))
                lineTo(left + dp(32f), cy)
                lineTo(left + dp(22f), cy + dp(6f))
                close()
            }
            canvas.drawPath(tri, fill)
            val waveLeft = left + dp(48f)
            val waveWidth = dp(110f)
            val waveHeight = dp(26f)
            val barWidth = waveWidth / (WaveformHeights.size * 1.6f)
            stroke.shader = null
            stroke.color = color
            stroke.strokeWidth = barWidth
            stroke.strokeCap = Paint.Cap.ROUND
            WaveformHeights.forEachIndexed { i, h ->
                val x = waveLeft + i * barWidth * 1.6f + barWidth / 2
                val bh = waveHeight * h
                canvas.drawLine(x, cy - bh / 2, x, cy + bh / 2, stroke)
            }
            val label = TextPaint(smallPaint).apply { this.color = color }
            canvas.drawText(VoiceFormat.format(item.message.voiceSeconds), waveLeft + waveWidth + dp(8f), cy + dp(4f), label)
        }
    }

    private inner class ImageContent(path: String?) : Content {
        private val bitmap: Bitmap? = path?.let { decodeScaled(it, dp(220f).toInt()) }
        private val scale = bitmap?.let { min(dp(220f) / it.width, dp(280f) / it.height).coerceAtMost(1f) } ?: 1f
        override val width = bitmap?.let { max(dp(120f), it.width * scale) } ?: dp(160f)
        override val height = bitmap?.let { max(dp(120f), it.height * scale) } ?: dp(120f)

        override fun draw(canvas: Canvas, left: Float, top: Float) {
            val rect = RectF(left, top, left + width, top + height)
            val r = dp(chat.cornerDp)
            val bmp = bitmap
            if (bmp == null) {
                fill.shader = null
                fill.color = chat.incomingBubble.argb()
                canvas.drawRoundRect(rect, r, r, fill)
                return
            }
            val shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val sx = width / bmp.width
            val sy = height / bmp.height
            val sc = max(sx, sy)
            shader.setLocalMatrix(Matrix().apply {
                setScale(sc, sc)
                postTranslate(left + (width - bmp.width * sc) / 2, top + (height - bmp.height * sc) / 2)
            })
            fill.shader = shader
            canvas.drawRoundRect(rect, r, r, fill)
            fill.shader = null
            bmp.recycle()
        }
    }

    // endregion

    // region Blocks

    private inner class BubbleBlock(
        private val item: ChatItem,
        private val content: Content,
        private val topGap: Float,
        private val nameHeight: Float,
        private val timeHeight: Float,
    ) : Block {
        override val height = topGap + nameHeight + content.height + timeHeight

        override fun draw(canvas: Canvas, top: Float) {
            var y = top + topGap
            val incomingLeft = sidePad + avatarSize + avatarGap
            if (nameHeight > 0f) {
                canvas.drawText(item.speaker?.name ?: "Không rõ", incomingLeft + dp(4f), y + dp(12f), namePaint)
                y += nameHeight
            }
            val left = if (item.outgoing) WIDTH_PX - sidePad - content.width else incomingLeft
            content.draw(canvas, left, y)
            if (!item.outgoing && item.lastInRun) {
                drawAvatar(canvas, item.speaker, sidePad, y + content.height - avatarSize, avatarSize)
            }
            y += content.height
            val time = item.message.timeLabel
            if (timeHeight > 0f && time != null) {
                val p = TextPaint(smallPaint).apply { color = chat.timeText.argb(); textSize = dp(11f) }
                val x = if (item.outgoing) WIDTH_PX - sidePad - dp(4f) - p.measureText(time) else incomingLeft + dp(4f)
                canvas.drawText(time, x, y + dp(13f), p)
            }
        }
    }

    private inner class NarrationBlock(text: String) : Block {
        private val paint = TextPaint(smallPaint).apply { color = chat.narrationText.argb() }
        private val layout = buildLayout(text, paint, dp(280f).toInt(), Layout.Alignment.ALIGN_CENTER)
        private val textWidth = (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: 0f
        override val height = layout.height + dp(8f) + dp(16f)

        override fun draw(canvas: Canvas, top: Float) {
            val pillW = textWidth + dp(24f)
            val pillH = layout.height + dp(8f)
            val left = (WIDTH_PX - pillW) / 2
            val y = top + dp(8f)
            fill.shader = null
            fill.color = chat.narrationBackground.argb()
            canvas.drawRoundRect(RectF(left, y, left + pillW, y + pillH), pillH / 2, pillH / 2, fill)
            canvas.save()
            canvas.translate((WIDTH_PX - layout.width) / 2, y + dp(4f))
            layout.draw(canvas)
            canvas.restore()
        }
    }

    private inner class HeaderBlock(private val title: String, private val partners: List<CharacterEntity>) : Block {
        override val height = dp(64f)

        override fun draw(canvas: Canvas, top: Float) {
            fill.shader = null
            fill.color = chat.headerBackground.argb()
            canvas.drawRect(0f, top, WIDTH_PX, top + height, fill)
            val size = dp(38f)
            val avatarTop = top + (height - size) / 2
            drawAvatar(canvas, partners.firstOrNull(), dp(16f), avatarTop, size)
            val titlePaint = TextPaint(textPaint).apply {
                color = chat.headerContent.argb()
                textSize = dp(16f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textLeft = dp(16f) + size + dp(12f)
            val maxW = WIDTH_PX - textLeft - dp(16f)
            val shown = android.text.TextUtils.ellipsize(title, titlePaint, maxW, android.text.TextUtils.TruncateAt.END).toString()
            canvas.drawText(shown, textLeft, top + dp(29f), titlePaint)
            val sub = TextPaint(smallPaint).apply { color = chat.headerSubtle.argb() }
            canvas.drawText(chat.preset.displayName, textLeft, top + dp(46f), sub)
        }
    }

    private inner class FooterBlock : Block {
        override val height = dp(36f)

        override fun draw(canvas: Canvas, top: Float) {
            val p = TextPaint(smallPaint).apply {
                color = chat.timeText.argb()
                textSize = dp(11f)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("⚠ ${Watermark.TEXT}", WIDTH_PX / 2, top + dp(22f), p)
        }
    }

    // endregion

    // region Drawing helpers

    private fun drawBubbleBackground(canvas: Canvas, rect: RectF, item: ChatItem) {
        val r = dp(chat.cornerDp)
        val t = dp(5f)
        // Corner order: top-left, top-right, bottom-right, bottom-left.
        val radii = when {
            !item.lastInRun -> floatArrayOf(r, r, r, r, r, r, r, r)
            item.outgoing -> floatArrayOf(r, r, r, r, t, t, r, r)
            else -> floatArrayOf(r, r, r, r, r, r, t, t)
        }
        val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
        fill.reset()
        fill.isAntiAlias = true
        if (item.outgoing) {
            val colors = chat.outgoingGradient.map { it.argb() }.toIntArray()
            if (colors.size == 1) {
                fill.shader = null
                fill.color = colors[0]
            } else {
                fill.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, colors, null, Shader.TileMode.CLAMP)
            }
        } else {
            fill.shader = null
            fill.color = chat.incomingBubble.argb()
        }
        if (chat.shadow) fill.setShadowLayer(dp(2f), 0f, dp(1f), 0x337CB8EB)
        canvas.drawPath(path, fill)
        fill.clearShadowLayer()
        fill.shader = null
        val border = chat.bubbleBorder
        if (border != null && !item.outgoing) {
            stroke.shader = null
            stroke.color = border.argb()
            stroke.strokeWidth = dp(chat.bubbleBorderWidthDp)
            canvas.drawPath(path, stroke)
        }
    }

    private fun drawAvatar(canvas: Canvas, character: CharacterEntity?, left: Float, top: Float, size: Float) {
        val cx = left + size / 2
        val cy = top + size / 2
        val color = character?.roleColorHex ?: 0xFF9E9E9E
        val bitmap = character?.let { c -> avatarCache.getOrPut(c.id) { c.avatarPath?.let { decodeScaled(it, 160) } } }
        fill.reset()
        fill.isAntiAlias = true
        if (bitmap != null) {
            val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val sc = size / min(bitmap.width, bitmap.height)
            shader.setLocalMatrix(Matrix().apply {
                setScale(sc, sc)
                postTranslate(left + (size - bitmap.width * sc) / 2, top + (size - bitmap.height * sc) / 2)
            })
            fill.shader = shader
            canvas.drawCircle(cx, cy, size / 2, fill)
            fill.shader = null
            return
        }
        fill.color = color.argb()
        canvas.drawCircle(cx, cy, size / 2, fill)
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size * 0.38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            this.color = if (ColorMath.prefersDarkText(color)) 0xFF1C1B22.toInt() else 0xFFFFFFFF.toInt()
        }
        val initials = NameRules.initialsOf(character?.name ?: "?")
        canvas.drawText(initials, cx, cy - (p.descent() + p.ascent()) / 2, p)
    }

    private fun buildLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    ): StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
        .setAlignment(align)
        .setLineSpacing(0f, 1.1f)
        .setIncludePad(false)
        .build()

    private fun decodeScaled(path: String, target: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (e: Exception) {
        null
    }

    // endregion

    companion object {
        const val WIDTH_PX = 1080f
        const val WIDTH_DP = 360f
        const val MAX_PAGE_HEIGHT_PX = 6000
    }
}
