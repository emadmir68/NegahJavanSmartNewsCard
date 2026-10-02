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
import ir.negahjavan.newscard.model.CardTemplate
import ir.negahjavan.newscard.model.NewsCategory
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.model.SmartSlide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object CardBitmapRenderer {

    private val brandRed = Color.rgb(227, 18, 28)

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

        if (imageUri == null) {
            drawTextOnly(canvas, paint, analysis, slide, slideIndex, w, h, pad)
            return@withContext bitmap
        }

        val photo = loadBitmap(context, imageUri)
        if (photo != null) {
            val focus = SmartImageFocus.detect(photo)
            drawFocalCrop(canvas, photo, Rect(0, 0, w, h), paint, focus.x, focus.y)
        } else {
            drawPlainGradient(canvas, paint, analysis.category, w, h)
        }

        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.argb(15, 0, 0, 0),
                Color.argb(65, 0, 0, 0),
                Color.argb(245, 4, 4, 6)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        when (analysis.recommendedTemplate) {
            CardTemplate.SIGNATURE ->
                drawSignature(canvas, paint, analysis, slide, format, slideIndex, w, h, pad)
            CardTemplate.BREAKING_PLUS ->
                drawBreaking(canvas, paint, analysis, slide, format, slideIndex, w, h, pad)
            CardTemplate.EDITORIAL ->
                drawEditorial(canvas, paint, analysis, slide, format, slideIndex, w, h, pad)
        }

        bitmap
    }

    private fun drawTextOnly(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        slide: SmartSlide,
        slideIndex: Int,
        w: Int,
        h: Int,
        pad: Float
    ) {
        drawPlainGradient(canvas, paint, analysis.category, w, h)

        drawText(canvas, "NEGAH / JAVAN", pad, h * .045f, w * .024f, Color.WHITE, true, Paint.Align.LEFT)
        drawText(canvas, "پایگاه خبری نگاه جوان", w - pad, h * .045f, w * .024f, Color.WHITE, true, Paint.Align.RIGHT)

        val badge = slide.badge.ifBlank { analysis.badge }
        drawText(canvas, badge, w - pad, h * .195f, w * .028f, brandRed, true, Paint.Align.RIGHT)

        val titleTop = when {
            slide.body.isBlank() -> h * .34f
            slide.title.length < 55 -> h * .29f
            else -> h * .25f
        }

        drawRtlBlock(
            canvas,
            slide.title,
            pad,
            titleTop,
            (w - 2 * pad).toInt(),
            textOnlyTitleSize(w, slide.title.length),
            Color.WHITE,
            1.10f,
            true,
            if (slide.title.length < 70) 4 else 5
        )

        if (slide.body.isNotBlank() && slide.body.trim() != slide.title.trim()) {
            val panelTop = if (slide.title.length < 70) h * .58f else h * .61f
            paint.color = Color.argb(20, 255, 255, 255)
            canvas.drawRoundRect(
                pad,
                panelTop,
                w - pad,
                h * .82f,
                26f,
                26f,
                paint
            )

            drawRtlBlock(
                canvas,
                slide.body,
                pad + w * .035f,
                panelTop + h * .032f,
                (w - 2 * pad - w * .07f).toInt(),
                w * .031f,
                Color.rgb(230, 230, 235),
                1.24f,
                false,
                5
            )
        }

        paint.color = brandRed
        canvas.drawRoundRect(pad, h * .90f, w - pad, h * .904f, 3f, 3f, paint)

        drawText(canvas, JalaliDate.today(), pad, h * .925f, w * .021f, Color.LTGRAY, false, Paint.Align.LEFT)

        if (analysis.slides.size > 1) {
            drawText(
                canvas,
                (slideIndex + 1).toString() + "/" + analysis.slides.size,
                w / 2f,
                h * .925f,
                w * .021f,
                Color.LTGRAY,
                true,
                Paint.Align.CENTER
            )
        }

        drawText(canvas, "@negahjavan", w - pad, h * .925f, w * .021f, brandRed, true, Paint.Align.RIGHT)
    }

    private fun drawPlainGradient(
        canvas: Canvas,
        paint: Paint,
        category: NewsCategory,
        w: Int,
        h: Int
    ) {
        val colors = when (category) {
            NewsCategory.JUDICIAL ->
                intArrayOf(Color.rgb(35, 41, 53), Color.rgb(16, 18, 24), Color.rgb(57, 8, 13))
            NewsCategory.ECONOMY ->
                intArrayOf(Color.rgb(29, 48, 42), Color.rgb(15, 21, 18), Color.rgb(57, 8, 13))
            NewsCategory.SPORTS ->
                intArrayOf(Color.rgb(22, 53, 40), Color.rgb(12, 20, 16), Color.rgb(57, 8, 13))
            NewsCategory.TECHNOLOGY ->
                intArrayOf(Color.rgb(29, 48, 66), Color.rgb(15, 20, 27), Color.rgb(57, 8, 13))
            else ->
                intArrayOf(Color.rgb(43, 47, 60), Color.rgb(17, 18, 23), Color.rgb(57, 8, 13))
        }

        paint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            colors, null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
    }

    private fun drawSignature(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        slide: SmartSlide,
        format: CardFormat,
        slideIndex: Int,
        w: Int,
        h: Int,
        pad: Float
    ) {
        drawText(canvas, "NEGAH / JAVAN", pad, h * .04f, w * .024f, Color.WHITE, true, Paint.Align.LEFT)
        drawText(canvas, "پایگاه خبری نگاه جوان", w - pad, h * .04f, w * .024f, Color.WHITE, true, Paint.Align.RIGHT)

        val badgeY = if (format == CardFormat.LANDSCAPE) h * .24f else h * .50f
        drawText(
            canvas,
            slide.badge.ifBlank { analysis.badge },
            w - pad,
            badgeY,
            w * .029f,
            brandRed,
            true,
            Paint.Align.RIGHT
        )

        drawRtlBlock(
            canvas,
            slide.title,
            pad,
            badgeY + h * .06f,
            (w - 2 * pad).toInt(),
            textOnlyTitleSize(w, slide.title.length),
            Color.WHITE,
            1.08f,
            true,
            4
        )

        if (slide.body.isNotBlank()) {
            drawRtlBlock(
                canvas,
                slide.body,
                pad,
                if (format == CardFormat.LANDSCAPE) h * .70f else h * .77f,
                (w - 2 * pad).toInt(),
                w * .030f,
                Color.rgb(235, 235, 239),
                1.22f,
                false,
                4
            )
        }

        drawFooter(canvas, paint, analysis, slideIndex, w, h, pad, Color.LTGRAY)
    }

    private fun drawBreaking(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        slide: SmartSlide,
        format: CardFormat,
        slideIndex: Int,
        w: Int,
        h: Int,
        pad: Float
    ) {
        paint.color = brandRed
        canvas.drawRect(0f, 0f, w.toFloat(), h * .10f, paint)
        drawText(canvas, "فوری", w - pad, h * .022f, w * .052f, Color.WHITE, true, Paint.Align.RIGHT)
        drawText(canvas, "NEGAH JAVAN • BREAKING", pad, h * .032f, w * .022f, Color.WHITE, true, Paint.Align.LEFT)

        val titleTop = if (format == CardFormat.LANDSCAPE) h * .34f else h * .53f
        drawText(
            canvas,
            slide.badge.ifBlank { "خبر فوری" },
            w - pad,
            titleTop - h * .055f,
            w * .025f,
            Color.rgb(255, 70, 82),
            true,
            Paint.Align.RIGHT
        )

        drawRtlBlock(
            canvas,
            slide.title,
            pad,
            titleTop,
            (w - 2 * pad).toInt(),
            textOnlyTitleSize(w, slide.title.length),
            Color.WHITE,
            1.06f,
            true,
            4
        )

        if (slide.body.isNotBlank()) {
            val bodyTop = if (format == CardFormat.LANDSCAPE) h * .70f else h * .78f
            paint.color = Color.argb(205, 17, 17, 22)
            canvas.drawRoundRect(pad, bodyTop - h * .02f, w - pad, bodyTop + h * .13f, 24f, 24f, paint)
            drawRtlBlock(
                canvas,
                slide.body,
                pad + w * .025f,
                bodyTop,
                (w - 2 * pad - w * .05f).toInt(),
                w * .029f,
                Color.WHITE,
                1.18f,
                false,
                3
            )
        }

        drawFooter(canvas, paint, analysis, slideIndex, w, h, pad, Color.WHITE)
    }

    private fun drawEditorial(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        slide: SmartSlide,
        format: CardFormat,
        slideIndex: Int,
        w: Int,
        h: Int,
        pad: Float
    ) {
        drawText(canvas, "NEGAH / JAVAN", pad, h * .04f, w * .024f, Color.WHITE, true, Paint.Align.LEFT)
        drawText(canvas, "پایگاه خبری نگاه جوان", w - pad, h * .04f, w * .024f, Color.WHITE, false, Paint.Align.RIGHT)

        val panelTop = when (format) {
            CardFormat.LANDSCAPE -> h * .31f
            CardFormat.SQUARE -> h * .39f
            CardFormat.PORTRAIT -> h * .43f
            CardFormat.STORY -> h * .47f
        }
        val panelBottom = h - pad

        paint.color = Color.argb(242, 247, 244, 239)
        canvas.drawRoundRect(pad, panelTop, w - pad, panelBottom, 30f, 30f, paint)

        val inner = pad + w * .035f
        drawText(
            canvas,
            slide.badge.ifBlank { analysis.badge },
            w - inner,
            panelTop + h * .028f,
            w * .025f,
            brandRed,
            true,
            Paint.Align.RIGHT
        )

        drawRtlBlock(
            canvas,
            slide.title,
            inner,
            panelTop + h * .085f,
            (w - 2 * inner).toInt(),
            textOnlyTitleSize(w, slide.title.length) * .90f,
            Color.rgb(20, 20, 23),
            1.08f,
            true,
            4
        )

        if (slide.body.isNotBlank()) {
            drawRtlBlock(
                canvas,
                slide.body,
                inner,
                panelTop + h * .31f,
                (w - 2 * inner).toInt(),
                w * .029f,
                Color.rgb(70, 71, 76),
                1.22f,
                false,
                if (format == CardFormat.STORY) 6 else 5
            )
        }

        paint.color = brandRed
        canvas.drawRoundRect(inner, panelBottom - h * .085f, w - inner, panelBottom - h * .081f, 3f, 3f, paint)
        drawText(canvas, JalaliDate.today(), inner, panelBottom - h * .060f, w * .021f, Color.DKGRAY, false, Paint.Align.LEFT)
        if (analysis.slides.size > 1) {
            drawText(
                canvas,
                (slideIndex + 1).toString() + "/" + analysis.slides.size,
                w / 2f,
                panelBottom - h * .060f,
                w * .021f,
                Color.GRAY,
                true,
                Paint.Align.CENTER
            )
        }
        drawText(canvas, "@negahjavan", w - inner, panelBottom - h * .060f, w * .021f, brandRed, true, Paint.Align.RIGHT)
    }

    private fun textOnlyTitleSize(w: Int, length: Int): Float = when {
        length < 45 -> w * .073f
        length < 80 -> w * .061f
        length < 115 -> w * .052f
        else -> w * .046f
    }

    private fun drawFooter(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        slideIndex: Int,
        w: Int,
        h: Int,
        pad: Float,
        textColor: Int
    ) {
        paint.color = brandRed
        canvas.drawRoundRect(pad, h * .935f, w - pad, h * .939f, 3f, 3f, paint)
        drawText(canvas, JalaliDate.today(), pad, h * .951f, w * .022f, textColor, false, Paint.Align.LEFT)
        if (analysis.slides.size > 1) {
            drawText(
                canvas,
                (slideIndex + 1).toString() + "/" + analysis.slides.size,
                w / 2f,
                h * .951f,
                w * .021f,
                textColor,
                true,
                Paint.Align.CENTER
            )
        }
        drawText(canvas, "@negahjavan", w - pad, h * .951f, w * .021f, brandRed, true, Paint.Align.RIGHT)
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

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
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
        val scale = max(dest.width().toFloat() / bitmap.width, dest.height().toFloat() / bitmap.height)
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
            typeface = if (bold) Typeface.create("sans", Typeface.BOLD) else Typeface.create("sans", Typeface.NORMAL)
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
            typeface = if (bold) Typeface.create("sans", Typeface.BOLD) else Typeface.create("sans", Typeface.NORMAL)
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
