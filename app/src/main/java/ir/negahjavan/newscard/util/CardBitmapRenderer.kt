package ir.negahjavan.newscard.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import ir.negahjavan.newscard.model.CardFormat
import ir.negahjavan.newscard.model.NewsCategory
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.model.SmartSlide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object CardBitmapRenderer {

    private val accentRed = Color.rgb(227, 18, 28)

    suspend fun renderSlide(
        context: Context,
        analysis: SmartAnalysis,
        slide: SmartSlide,
        imageUri: Uri?,
        format: CardFormat,
        slideIndex: Int
    ): Bitmap = withContext(Dispatchers.Default) {
        val w = format.width
        val h = format.height
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val pad = w * .055f

        val photo = imageUri?.let { loadBitmap(context, it) }

        if (photo != null) {
            val focus = SmartImageFocus.detect(photo)
            drawFocalCrop(canvas, photo, Rect(0, 0, w, h), paint, focus.x, focus.y)

            paint.shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(35, 0, 0, 0),
                    Color.argb(205, 0, 0, 0),
                    Color.argb(248, 6, 6, 8)
                ),
                floatArrayOf(0f, .36f, .70f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            paint.shader = null
        } else {
            drawTextOnlyBackground(canvas, paint, analysis.category, w, h)
        }

        val bodyVisible = slide.body.isNotBlank() && slide.body.trim() != slide.title.trim()

        if (bodyVisible) {
            drawBottomNewsLayout(
                canvas, paint, analysis, slide, w, h, pad, photo != null
            )
        } else {
            drawCenteredNewsLayout(
                canvas, analysis, slide, w, h, pad
            )
        }

        bitmap
    }

    private fun drawCenteredNewsLayout(
        canvas: Canvas,
        analysis: SmartAnalysis,
        slide: SmartSlide,
        w: Int,
        h: Int,
        pad: Float
    ) {
        drawText(
            canvas,
            slide.badge.ifBlank { analysis.badge },
            w - pad,
            h * .31f,
            w * .028f,
            accentRed,
            true,
            Paint.Align.RIGHT
        )

        drawRtlBlock(
            canvas,
            slide.title,
            pad,
            h * .38f,
            (w - 2 * pad).toInt(),
            titleSize(w, slide.title.length),
            Color.WHITE,
            1.10f,
            true,
            if (slide.title.length < 75) 4 else 5
        )
    }

    private fun drawBottomNewsLayout(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        slide: SmartSlide,
        w: Int,
        h: Int,
        pad: Float,
        hasImage: Boolean
    ) {
        val titleTop = if (slide.title.length < 75) h * .55f else h * .50f

        drawText(
            canvas,
            slide.badge.ifBlank { analysis.badge },
            w - pad,
            titleTop - h * .055f,
            w * .028f,
            accentRed,
            true,
            Paint.Align.RIGHT
        )

        drawRtlBlock(
            canvas,
            slide.title,
            pad,
            titleTop,
            (w - 2 * pad).toInt(),
            titleSize(w, slide.title.length),
            Color.WHITE,
            1.08f,
            true,
            4
        )

        val panelTop = if (slide.title.length < 75) h * .74f else h * .72f
        paint.color = if (hasImage) Color.argb(92, 0, 0, 0) else Color.argb(16, 255, 255, 255)
        canvas.drawRoundRect(
            pad,
            panelTop,
            w - pad,
            h * .91f,
            28f,
            28f,
            paint
        )

        drawRtlBlock(
            canvas,
            slide.body,
            pad + w * .035f,
            panelTop + h * .025f,
            (w - 2 * pad - w * .07f).toInt(),
            w * .031f,
            Color.rgb(235, 235, 239),
            1.22f,
            false,
            5
        )
    }

    private fun drawTextOnlyBackground(
        canvas: Canvas,
        paint: Paint,
        category: NewsCategory,
        w: Int,
        h: Int
    ) {
        val colors = when (category) {
            NewsCategory.JUDICIAL ->
                intArrayOf(Color.rgb(36, 42, 55), Color.rgb(15, 17, 22), Color.rgb(45, 7, 12))
            NewsCategory.ECONOMY ->
                intArrayOf(Color.rgb(31, 53, 46), Color.rgb(14, 21, 18), Color.rgb(45, 7, 12))
            NewsCategory.SPORTS ->
                intArrayOf(Color.rgb(23, 55, 43), Color.rgb(11, 20, 16), Color.rgb(45, 7, 12))
            NewsCategory.TECHNOLOGY ->
                intArrayOf(Color.rgb(29, 53, 74), Color.rgb(14, 21, 28), Color.rgb(45, 7, 12))
            else ->
                intArrayOf(Color.rgb(47, 52, 65), Color.rgb(17, 18, 23), Color.rgb(45, 7, 12))
        }

        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            colors, null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
    }

    private fun titleSize(w: Int, length: Int): Float = when {
        length < 45 -> w * .073f
        length < 80 -> w * .061f
        length < 115 -> w * .052f
        else -> w * .046f
    }

    suspend fun save(
        context: Context,
        bitmap: Bitmap,
        format: CardFormat,
        index: Int,
        total: Int
    ): Uri? = withContext(Dispatchers.IO) {
        val suffix = if (total > 1) "_" + (index + 1).toString().padStart(2, '0') else ""
        val name = "NegahJavan_" + System.currentTimeMillis() + "_" + format.name + suffix + ".png"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NegahJavan")
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        uri?.let { outUri ->
            context.contentResolver.openOutputStream(outUri)?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }

        uri
    }

    private suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .allowHardware(false)
                    .build()
                loader.execute(request).image?.toBitmap()
            }.getOrNull()
        }

    private fun drawFocalCrop(
        canvas: Canvas,
        bitmap: Bitmap,
        dest: Rect,
        paint: Paint,
        focusX: Float,
        focusY: Float
    ) {
        val scale = max(
            dest.width().toFloat() / bitmap.width,
            dest.height().toFloat() / bitmap.height
        )
        val srcW = dest.width() / scale
        val srcH = dest.height() / scale
        val left = (bitmap.width * focusX - srcW / 2f)
            .coerceIn(0f, (bitmap.width - srcW).coerceAtLeast(0f))
        val top = (bitmap.height * focusY - srcH / 2f)
            .coerceIn(0f, (bitmap.height - srcH).coerceAtLeast(0f))

        val src = Rect(
            left.toInt(),
            top.toInt(),
            (left + srcW).toInt(),
            (top + srcH).toInt()
        )
        canvas.drawBitmap(bitmap, src, RectF(dest), paint)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean,
        align: Paint.Align
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = if (bold) {
                Typeface.create("sans", Typeface.BOLD)
            } else {
                Typeface.create("sans", Typeface.NORMAL)
            }
        }
        canvas.drawText(text, x, y - p.ascent(), p)
    }

    private fun drawRtlBlock(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        size: Float,
        color: Int,
        spacing: Float,
        bold: Boolean,
        maxLines: Int
    ) {
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) {
                Typeface.create("sans", Typeface.BOLD)
            } else {
                Typeface.create("sans", Typeface.NORMAL)
            }
        }

        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, tp, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, spacing)
            .setMaxLines(maxLines)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}
