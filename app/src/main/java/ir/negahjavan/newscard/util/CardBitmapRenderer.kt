package ir.negahjavan.newscard.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
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

    private val accentRed = Color.rgb(229, 24, 36)
    private const val brandSocial = "negahjavaan"
    private const val brandSite = "negahjavan.ir"
    private const val brandTitle = "پایگاه خبری نگاه جوان"

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
            drawFocalCrop(
                canvas = canvas,
                bitmap = photo,
                dest = Rect(0, 0, w, h),
                paint = paint,
                focusX = focus.x,
                focusY = focus.y
            )
            drawPhotoScrim(canvas, paint, w, h)
        } else {
            drawTextBackground(canvas, paint, analysis.category, w, h)
        }

        drawBrandHeader(canvas, paint, w, h, pad, photo != null)
        drawNewsFromTop(
            canvas = canvas,
            paint = paint,
            analysis = analysis,
            slide = slide,
            format = format,
            w = w,
            h = h,
            pad = pad,
            hasImage = photo != null
        )

        bitmap
    }

    private fun drawBrandHeader(
        canvas: Canvas,
        paint: Paint,
        w: Int,
        h: Int,
        pad: Float,
        hasImage: Boolean
    ) {
        if (hasImage) {
            paint.shader = LinearGradient(
                0f,
                0f,
                0f,
                h * .13f,
                intArrayOf(Color.argb(145, 0, 0, 0), Color.argb(45, 0, 0, 0)),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w.toFloat(), h * .13f, paint)
            paint.shader = null
        }

        val sideSize = w * .0215f
        val centerSize = w * .0245f
        val y = h * .038f

        drawText(
            canvas,
            brandSite,
            pad,
            y,
            sideSize,
            Color.WHITE,
            true,
            Paint.Align.LEFT,
            shadow = hasImage
        )
        drawText(
            canvas,
            brandSocial,
            w / 2f,
            y,
            centerSize,
            Color.WHITE,
            true,
            Paint.Align.CENTER,
            shadow = hasImage
        )
        drawText(
            canvas,
            brandTitle,
            w - pad,
            y,
            sideSize,
            Color.WHITE,
            true,
            Paint.Align.RIGHT,
            shadow = hasImage
        )
    }

    private fun drawNewsFromTop(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        slide: SmartSlide,
        format: CardFormat,
        w: Int,
        h: Int,
        pad: Float,
        hasImage: Boolean
    ) {
        val bodyVisible = slide.body.isNotBlank() &&
            similarityKey(slide.body) != similarityKey(slide.title)

        val startY = when (format) {
            CardFormat.STORY -> h * .155f
            CardFormat.PORTRAIT -> h * .145f
            CardFormat.SQUARE -> h * .135f
            CardFormat.LANDSCAPE -> h * .16f
        }

        val badgeText = slide.badge.ifBlank { analysis.badge }
        val badgeSize = w * .027f
        drawBadge(
            canvas = canvas,
            paint = paint,
            text = badgeText,
            right = w - pad,
            top = startY,
            textSize = badgeSize,
            hasImage = hasImage
        )

        val titleTop = startY + h * .050f
        val titleSize = titleSize(w, slide.title.length, format)
        val titleMaxLines = when (format) {
            CardFormat.LANDSCAPE -> 3
            CardFormat.SQUARE -> 4
            else -> 5
        }

        val titleLayout = makeRtlLayout(
            text = slide.title,
            width = (w - 2 * pad).toInt(),
            size = titleSize,
            color = Color.WHITE,
            spacing = 1.09f,
            bold = true,
            maxLines = titleMaxLines
        )
        drawLayout(canvas, titleLayout, pad, titleTop)

        if (!bodyVisible) return

        val gap = h * .030f
        val bodyTop = titleTop + titleLayout.height + gap
        val safeBottom = when (format) {
            CardFormat.STORY -> h * .91f
            CardFormat.PORTRAIT -> h * .92f
            CardFormat.SQUARE -> h * .93f
            CardFormat.LANDSCAPE -> h * .91f
        }

        if (bodyTop >= safeBottom - h * .10f) return

        val bodySize = bodySize(w, slide.body.length, format)
        val availableHeight = (safeBottom - bodyTop - h * .02f).toInt()
        val estimatedLineHeight = (bodySize * 1.35f).toInt().coerceAtLeast(1)
        val maxLines = (availableHeight / estimatedLineHeight)
            .coerceIn(2, if (format == CardFormat.STORY) 9 else 7)

        val bodyWidth = (w - 2 * pad - w * .055f).toInt()
        val bodyLayout = makeRtlLayout(
            text = slide.body,
            width = bodyWidth,
            size = bodySize,
            color = Color.rgb(239, 239, 242),
            spacing = 1.24f,
            bold = false,
            maxLines = maxLines
        )

        val panelLeft = pad
        val panelRight = w - pad
        val panelTop = bodyTop - h * .010f
        val panelBottom = minOf(
            safeBottom,
            bodyTop + bodyLayout.height + h * .035f
        )

        paint.color = if (hasImage) {
            Color.argb(118, 4, 4, 7)
        } else {
            Color.argb(20, 255, 255, 255)
        }
        canvas.drawRoundRect(
            panelLeft,
            panelTop,
            panelRight,
            panelBottom,
            w * .025f,
            w * .025f,
            paint
        )

        drawLayout(
            canvas,
            bodyLayout,
            pad + w * .0275f,
            bodyTop + h * .010f
        )
    }

    private fun drawBadge(
        canvas: Canvas,
        paint: Paint,
        text: String,
        right: Float,
        top: Float,
        textSize: Float,
        hasImage: Boolean
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = Typeface.create("sans", Typeface.BOLD)
        }
        val width = p.measureText(text).coerceAtLeast(textSize * 2.2f)
        val padX = textSize * .75f
        val height = textSize * 1.85f

        paint.color = if (hasImage) {
            Color.argb(220, 214, 18, 30)
        } else {
            accentRed
        }
        canvas.drawRoundRect(
            right - width - padX * 2,
            top,
            right,
            top + height,
            height / 2f,
            height / 2f,
            paint
        )

        drawText(
            canvas,
            text,
            right - padX,
            top + height * .20f,
            textSize,
            Color.WHITE,
            true,
            Paint.Align.RIGHT,
            shadow = false
        )
    }

    private fun drawPhotoScrim(
        canvas: Canvas,
        paint: Paint,
        w: Int,
        h: Int
    ) {
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(
                Color.argb(55, 0, 0, 0),
                Color.argb(72, 0, 0, 0),
                Color.argb(112, 0, 0, 0),
                Color.argb(188, 0, 0, 0),
                Color.argb(230, 0, 0, 0)
            ),
            floatArrayOf(0f, .16f, .38f, .72f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        paint.shader = LinearGradient(
            0f,
            h * .12f,
            w.toFloat(),
            h * .75f,
            intArrayOf(Color.argb(45, 0, 0, 0), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, h * .10f, w.toFloat(), h * .80f, paint)
        paint.shader = null
    }

    private fun drawTextBackground(
        canvas: Canvas,
        paint: Paint,
        category: NewsCategory,
        w: Int,
        h: Int
    ) {
        val colors = when (category) {
            NewsCategory.JUDICIAL ->
                intArrayOf(Color.rgb(38, 44, 58), Color.rgb(16, 18, 24), Color.rgb(49, 8, 14))
            NewsCategory.POLITICS ->
                intArrayOf(Color.rgb(42, 47, 66), Color.rgb(16, 18, 25), Color.rgb(49, 8, 14))
            NewsCategory.ECONOMY ->
                intArrayOf(Color.rgb(31, 53, 46), Color.rgb(14, 21, 18), Color.rgb(47, 8, 13))
            NewsCategory.SPORTS ->
                intArrayOf(Color.rgb(24, 57, 44), Color.rgb(11, 21, 16), Color.rgb(47, 8, 13))
            NewsCategory.CULTURE ->
                intArrayOf(Color.rgb(61, 44, 63), Color.rgb(21, 16, 22), Color.rgb(51, 8, 14))
            NewsCategory.TECHNOLOGY ->
                intArrayOf(Color.rgb(29, 53, 75), Color.rgb(14, 21, 29), Color.rgb(48, 8, 13))
            else ->
                intArrayOf(Color.rgb(49, 54, 69), Color.rgb(17, 18, 24), Color.rgb(49, 8, 14))
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

        paint.color = Color.argb(12, 255, 255, 255)
        canvas.drawCircle(w * .88f, h * .18f, w * .42f, paint)
        paint.color = Color.argb(8, 255, 255, 255)
        canvas.drawCircle(w * .10f, h * .78f, w * .36f, paint)
    }

    private fun titleSize(
        w: Int,
        length: Int,
        format: CardFormat
    ): Float {
        val base = when {
            length < 42 -> w * .071f
            length < 76 -> w * .061f
            length < 112 -> w * .052f
            else -> w * .045f
        }

        return when (format) {
            CardFormat.LANDSCAPE -> base * .84f
            CardFormat.SQUARE -> base * .92f
            else -> base
        }
    }

    private fun bodySize(
        w: Int,
        length: Int,
        format: CardFormat
    ): Float {
        val base = when {
            length < 170 -> w * .0315f
            length < 300 -> w * .0285f
            else -> w * .026f
        }
        return if (format == CardFormat.LANDSCAPE) base * .88f else base
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

        val name = "NegahJavan_" +
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

    private fun makeRtlLayout(
        text: String,
        width: Int,
        size: Float,
        color: Int,
        spacing: Float,
        bold: Boolean,
        maxLines: Int
    ): StaticLayout {
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) {
                Typeface.create("sans", Typeface.BOLD)
            } else {
                Typeface.create("sans", Typeface.NORMAL)
            }
        }

        return StaticLayout.Builder
            .obtain(text, 0, text.length, tp, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, spacing)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
    }

    private fun drawLayout(
        canvas: Canvas,
        layout: StaticLayout,
        x: Float,
        y: Float
    ) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean,
        align: Paint.Align,
        shadow: Boolean
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
            if (shadow) {
                setShadowLayer(size * .22f, 0f, size * .07f, Color.argb(180, 0, 0, 0))
            }
        }
        canvas.drawText(text, x, y - p.ascent(), p)
    }

    private fun similarityKey(value: String): String =
        value.lowercase()
            .replace(Regex("[^آ-یa-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(80)
}
