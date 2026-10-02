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
        val pad = w * .05f

        drawBackground(
            canvas = canvas,
            paint = paint,
            context = context,
            imageUri = imageUri,
            analysis = analysis,
            w = w,
            h = h
        )

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

    private suspend fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        context: Context,
        imageUri: Uri?,
        analysis: SmartAnalysis,
        w: Int,
        h: Int
    ) {
        canvas.drawColor(Color.rgb(10, 10, 13))

        val photo = imageUri?.let { loadBitmap(context, it) }

        if (photo != null) {
            val focus = SmartImageFocus.detect(photo)
            drawFocalCrop(
                canvas = canvas,
                bitmap = photo,
                dest = Rect(0, 0, w, h),
                paint = paint,
                focusX = focus.x,
                focusY = focus.y
            )
        } else {
            drawSmartCover(canvas, paint, analysis, w, h)
        }

        val overlay = when (analysis.recommendedTemplate) {
            CardTemplate.SIGNATURE ->
                intArrayOf(
                    Color.argb(20, 0, 0, 0),
                    Color.argb(75, 0, 0, 0),
                    Color.argb(242, 5, 5, 8)
                )
            CardTemplate.BREAKING_PLUS ->
                intArrayOf(
                    Color.argb(40, 0, 0, 0),
                    Color.argb(115, 0, 0, 0),
                    Color.argb(248, 4, 4, 6)
                )
            CardTemplate.EDITORIAL ->
                intArrayOf(
                    Color.argb(28, 0, 0, 0),
                    Color.argb(70, 0, 0, 0),
                    Color.argb(160, 0, 0, 0)
                )
        }

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            overlay,
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
    }

    private fun drawSmartCover(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        w: Int,
        h: Int
    ) {
        val colors = when (analysis.category) {
            NewsCategory.JUDICIAL ->
                intArrayOf(Color.rgb(45, 52, 66), Color.rgb(17, 19, 24), Color.rgb(92, 13, 21))
            NewsCategory.POLITICS ->
                intArrayOf(Color.rgb(49, 57, 82), Color.rgb(18, 20, 28), Color.rgb(86, 11, 18))
            NewsCategory.ECONOMY ->
                intArrayOf(Color.rgb(35, 61, 53), Color.rgb(15, 22, 19), Color.rgb(76, 15, 20))
            NewsCategory.SPORTS ->
                intArrayOf(Color.rgb(28, 69, 51), Color.rgb(12, 22, 18), Color.rgb(78, 12, 18))
            NewsCategory.CULTURE ->
                intArrayOf(Color.rgb(69, 53, 72), Color.rgb(23, 17, 23), Color.rgb(90, 11, 19))
            NewsCategory.TECHNOLOGY ->
                intArrayOf(Color.rgb(36, 59, 82), Color.rgb(15, 21, 29), Color.rgb(78, 11, 18))
            else ->
                intArrayOf(Color.rgb(57, 62, 83), Color.rgb(17, 18, 23), Color.rgb(90, 8, 16))
        }

        paint.shader = LinearGradient(
            0f,
            0f,
            w.toFloat(),
            h.toFloat(),
            colors,
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * .006f
        paint.color = Color.argb(28, 255, 255, 255)

        canvas.drawCircle(
            w * .18f,
            h * .20f,
            w * .30f,
            paint
        )
        canvas.drawCircle(
            w * .88f,
            h * .70f,
            w * .42f,
            paint
        )

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(16, 255, 255, 255)
        canvas.drawRoundRect(
            w * .08f,
            h * .16f,
            w * .92f,
            h * .22f,
            40f,
            40f,
            paint
        )

        drawText(
            canvas,
            "N/J",
            w * .50f,
            h * .33f,
            w * .20f,
            Color.argb(18, 255, 255, 255),
            true,
            Paint.Align.CENTER
        )

        drawText(
            canvas,
            analysis.category.fa.uppercase(),
            w * .50f,
            h * .43f,
            w * .035f,
            Color.argb(80, 255, 255, 255),
            true,
            Paint.Align.CENTER
        )
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
        paint.color = brandRed
        canvas.drawRoundRect(
            pad,
            pad,
            pad + w * .29f,
            pad + h * .062f,
            24f,
            24f,
            paint
        )
        drawText(
            canvas,
            "NEGAH JAVAN",
            pad + w * .145f,
            pad + h * .012f,
            w * .027f,
            Color.WHITE,
            true,
            Paint.Align.CENTER
        )
        drawText(
            canvas,
            "پایگاه خبری نگاه جوان",
            w - pad,
            pad + h * .013f,
            w * .024f,
            Color.WHITE,
            true,
            Paint.Align.RIGHT
        )

        val badge = slide.badge.ifBlank { analysis.badge }
        val badgeY = if (format == CardFormat.LANDSCAPE) h * .24f else h * .50f

        paint.color = brandRed
        canvas.drawRoundRect(
            w - pad - w * .28f,
            badgeY,
            w - pad,
            badgeY + h * .052f,
            18f,
            18f,
            paint
        )
        drawText(
            canvas,
            badge,
            w - pad - w * .14f,
            badgeY + h * .009f,
            w * .032f,
            Color.WHITE,
            true,
            Paint.Align.CENTER
        )

        val titleTop = badgeY + h * .075f
        drawRtlBlock(
            canvas,
            slide.title,
            pad,
            titleTop,
            (w - 2 * pad).toInt(),
            smartTitleSize(w, slide.title.length, slideIndex == 0),
            Color.WHITE,
            1.08f,
            true,
            if (format == CardFormat.LANDSCAPE) 3 else 4
        )

        val bodyTop = if (format == CardFormat.LANDSCAPE) h * .69f else h * .77f
        drawRtlBlock(
            canvas,
            slide.body,
            pad,
            bodyTop,
            (w - 2 * pad).toInt(),
            w * .030f,
            Color.rgb(235, 235, 239),
            1.22f,
            false,
            if (format == CardFormat.LANDSCAPE) 2 else 4
        )

        drawFooter(
            canvas,
            paint,
            analysis,
            slideIndex,
            w,
            h,
            pad,
            Color.LTGRAY
        )
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
        canvas.drawRect(
            0f,
            0f,
            w.toFloat(),
            h * .10f,
            paint
        )

        drawText(
            canvas,
            "فوری",
            w - pad,
            h * .021f,
            w * .052f,
            Color.WHITE,
            true,
            Paint.Align.RIGHT
        )
        drawText(
            canvas,
            "NEGAH JAVAN • BREAKING",
            pad,
            h * .032f,
            w * .022f,
            Color.WHITE,
            true,
            Paint.Align.LEFT
        )

        val titleTop = if (format == CardFormat.LANDSCAPE) h * .34f else h * .53f

        drawText(
            canvas,
            "●  " + slide.badge.ifBlank { "خبر فوری" },
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
            smartTitleSize(w, slide.title.length, true),
            Color.WHITE,
            1.06f,
            true,
            4
        )

        val bodyTop = if (format == CardFormat.LANDSCAPE) h * .70f else h * .78f
        paint.color = Color.argb(205, 17, 17, 22)
        canvas.drawRoundRect(
            pad,
            bodyTop - h * .02f,
            w - pad,
            bodyTop + h * .13f,
            24f,
            24f,
            paint
        )

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

        drawFooter(
            canvas,
            paint,
            analysis,
            slideIndex,
            w,
            h,
            pad,
            Color.WHITE
        )
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
        drawText(
            canvas,
            "NEGAH / JAVAN",
            pad,
            pad,
            w * .025f,
            Color.WHITE,
            true,
            Paint.Align.LEFT
        )
        drawText(
            canvas,
            "پایگاه خبری نگاه جوان",
            w - pad,
            pad,
            w * .024f,
            Color.WHITE,
            false,
            Paint.Align.RIGHT
        )

        val panelTop = when (format) {
            CardFormat.LANDSCAPE -> h * .31f
            CardFormat.SQUARE -> h * .39f
            CardFormat.PORTRAIT -> h * .43f
            CardFormat.STORY -> h * .47f
        }
        val panelBottom = h - pad

        paint.color = Color.argb(242, 247, 244, 239)
        canvas.drawRoundRect(
            pad,
            panelTop,
            w - pad,
            panelBottom,
            30f,
            30f,
            paint
        )

        val inner = pad + w * .035f
        val badge = slide.badge.ifBlank { analysis.badge }

        drawText(
            canvas,
            badge,
            w - inner,
            panelTop + h * .028f,
            w * .025f,
            brandRed,
            true,
            Paint.Align.RIGHT
        )

        val titleTop = panelTop + h * .085f
        drawRtlBlock(
            canvas,
            slide.title,
            inner,
            titleTop,
            (w - 2 * inner).toInt(),
            smartTitleSize(w, slide.title.length, slideIndex == 0) * .90f,
            Color.rgb(20, 20, 23),
            1.08f,
            true,
            if (format == CardFormat.LANDSCAPE) 3 else 4
        )

        val bodyTop = if (format == CardFormat.LANDSCAPE) {
            panelTop + h * .32f
        } else {
            panelTop + h * .29f
        }

        drawRtlBlock(
            canvas,
            slide.body,
            inner,
            bodyTop,
            (w - 2 * inner).toInt(),
            w * .029f,
            Color.rgb(70, 71, 76),
            1.22f,
            false,
            if (format == CardFormat.STORY) 6 else 5
        )

        paint.color = brandRed
        canvas.drawRoundRect(
            inner,
            panelBottom - h * .085f,
            w - inner,
            panelBottom - h * .081f,
            3f,
            3f,
            paint
        )

        drawText(
            canvas,
            JalaliDate.today(),
            inner,
            panelBottom - h * .060f,
            w * .021f,
            Color.DKGRAY,
            false,
            Paint.Align.LEFT
        )
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
        drawText(
            canvas,
            "@negahjavan",
            w - inner,
            panelBottom - h * .060f,
            w * .021f,
            brandRed,
            true,
            Paint.Align.RIGHT
        )
    }

    private fun smartTitleSize(
        w: Int,
        length: Int,
        cover: Boolean
    ): Float = when {
        cover && length < 42 -> w * .076f
        length < 68 -> w * .061f
        length < 100 -> w * .052f
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
        canvas.drawRoundRect(
            pad,
            h * .935f,
            w - pad,
            h * .939f,
            3f,
            3f,
            paint
        )

        drawText(
            canvas,
            JalaliDate.today(),
            pad,
            h * .951f,
            w * .022f,
            textColor,
            false,
            Paint.Align.LEFT
        )
        drawText(
            canvas,
            (slideIndex + 1).toString() + "/" + analysis.slides.size,
            w * .40f,
            h * .951f,
            w * .021f,
            textColor,
            true,
            Paint.Align.CENTER
        )
        drawText(
            canvas,
            "@negahjavan",
            w * .65f,
            h * .951f,
            w * .021f,
            textColor,
            false,
            Paint.Align.CENTER
        )
        drawText(
            canvas,
            "NEGAH JAVAN",
            w - pad,
            h * .951f,
            w * .020f,
            Color.WHITE,
            true,
            Paint.Align.RIGHT
        )
    }

    suspend fun save(
        context: Context,
        bitmap: Bitmap,
        format: CardFormat,
        index: Int,
        total: Int
    ): Uri? = withContext(Dispatchers.IO) {
        val suffix = if (total > 1) {
            "_" + (index + 1).toString().padStart(2, '0')
        } else {
            ""
        }

        val name =
            "NegahJavan_" +
                System.currentTimeMillis() +
                "_" +
                format.name +
                suffix +
                ".png"

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

    private suspend fun loadBitmap(
        context: Context,
        uri: Uri
    ): Bitmap? = withContext(Dispatchers.IO) {
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

        val wantedLeft = bitmap.width * focusX - srcW / 2f
        val wantedTop = bitmap.height * focusY - srcH / 2f

        val left = wantedLeft.coerceIn(
            0f,
            (bitmap.width - srcW).coerceAtLeast(0f)
        )
        val top = wantedTop.coerceIn(
            0f,
            (bitmap.height - srcH).coerceAtLeast(0f)
        )

        val src = Rect(
            left.toInt(),
            top.toInt(),
            (left + srcW).toInt(),
            (top + srcH).toInt()
        )

        canvas.drawBitmap(
            bitmap,
            src,
            RectF(dest),
            paint
        )
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
