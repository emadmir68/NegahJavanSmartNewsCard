package ir.negahjavan.newscard.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
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
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import ir.negahjavan.newscard.model.CardFormat
import ir.negahjavan.newscard.model.NewsCategory
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.model.SmartPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object CardBitmapRenderer {

    private val accentRed = Color.rgb(231, 22, 34)
    private const val brandSocial = "negahjavaan"
    private const val brandSite = "negahjavan.ir"
    private const val brandTitle = "پایگاه خبری نگاه جوان"

    suspend fun renderPage(
        context: Context,
        analysis: SmartAnalysis,
        page: SmartPage,
        imageUri: Uri?,
        format: CardFormat
    ): Bitmap = withContext(Dispatchers.Default) {
        val w = format.width
        val h = format.height
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawPremiumBackground(canvas, paint, analysis.category, w, h)
        drawBrandLockup(canvas, paint, w, h)

        val photo = if (page.showImage) {
            imageUri?.let { loadBitmap(context, it) }
        } else {
            null
        }

        val pad = w * .060f
        val gap = max(18f, h * .018f)
        val headerBottom = h * .115f
        val imageTop = h * .135f
        val imageHeight = imageHeight(format, h)
        val safeBottom = h * .945f
        val badgeHeight = h * .045f

        var contentTop = headerBottom + gap

        if (page.showImage && photo != null) {
            val focus = SmartImageFocus.detect(photo)
            val imageRect = RectF(
                pad,
                imageTop,
                w - pad,
                imageTop + imageHeight
            )
            drawRoundedFocalImage(
                canvas = canvas,
                bitmap = photo,
                dest = imageRect,
                paint = paint,
                focusX = focus.x,
                focusY = focus.y,
                radius = w * .032f
            )

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = max(2f, w * .0022f)
            paint.color = Color.argb(120, 255, 255, 255)
            canvas.drawRoundRect(
                imageRect,
                w * .032f,
                w * .032f,
                paint
            )
            paint.style = Paint.Style.FILL

            contentTop = imageTop + imageHeight + gap
        }

        drawBadge(
            canvas = canvas,
            paint = paint,
            text = page.badge.ifBlank {
                if (page.isContinuation) "ادامه خبر" else analysis.badge
            },
            right = w - pad,
            top = contentTop,
            height = badgeHeight,
            textSize = w * .025f
        )

        if (page.isFirst) {
            val titleTop = contentTop + badgeHeight + gap
            val titleLayout = makeRtlLayout(
                text = page.title,
                width = (w - 2 * pad).toInt(),
                size = titleSize(format, page.title.length),
                color = Color.WHITE,
                spacing = 1.08f,
                bold = true
            )

            drawLayout(canvas, titleLayout, pad, titleTop)

            if (page.body.isNotBlank()) {
                val bodyTop = titleTop + titleLayout.height + gap
                drawBody(
                    canvas = canvas,
                    paint = paint,
                    body = page.body,
                    x = pad,
                    top = bodyTop,
                    width = (w - 2 * pad).toInt(),
                    textSize = page.bodyTextSizePx,
                    safeBottom = safeBottom,
                    w = w,
                    h = h
                )
            }
        } else {
            val bodyTop = contentTop + badgeHeight + gap
            drawBody(
                canvas = canvas,
                paint = paint,
                body = page.body,
                x = pad,
                top = bodyTop,
                width = (w - 2 * pad).toInt(),
                textSize = page.bodyTextSizePx,
                safeBottom = safeBottom,
                w = w,
                h = h
            )
        }

        bitmap
    }

    private fun drawPremiumBackground(
        canvas: Canvas,
        paint: Paint,
        category: NewsCategory,
        w: Int,
        h: Int
    ) {
        val colors = when (category) {
            NewsCategory.JUDICIAL ->
                intArrayOf(Color.rgb(28, 31, 40), Color.rgb(12, 13, 17), Color.rgb(48, 7, 12))
            NewsCategory.POLITICS ->
                intArrayOf(Color.rgb(31, 33, 45), Color.rgb(12, 13, 17), Color.rgb(48, 7, 12))
            NewsCategory.ECONOMY ->
                intArrayOf(Color.rgb(23, 40, 35), Color.rgb(11, 16, 14), Color.rgb(46, 7, 11))
            NewsCategory.SPORTS ->
                intArrayOf(Color.rgb(20, 44, 33), Color.rgb(10, 17, 13), Color.rgb(46, 7, 11))
            NewsCategory.CULTURE ->
                intArrayOf(Color.rgb(45, 32, 47), Color.rgb(16, 12, 17), Color.rgb(49, 7, 12))
            NewsCategory.TECHNOLOGY ->
                intArrayOf(Color.rgb(24, 43, 57), Color.rgb(11, 16, 21), Color.rgb(47, 7, 12))
            else ->
                intArrayOf(Color.rgb(35, 38, 49), Color.rgb(12, 13, 17), Color.rgb(48, 7, 12))
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

        paint.color = Color.argb(14, 255, 255, 255)
        canvas.drawCircle(w * .92f, h * .15f, w * .36f, paint)
        paint.color = Color.argb(10, 229, 22, 34)
        canvas.drawCircle(w * .10f, h * .88f, w * .44f, paint)

        paint.color = Color.argb(12, 255, 255, 255)
        canvas.drawRoundRect(
            w * .055f,
            h * .105f,
            w * .945f,
            h * .108f,
            3f,
            3f,
            paint
        )
    }

    private fun drawBrandLockup(
        canvas: Canvas,
        paint: Paint,
        w: Int,
        h: Int
    ) {
        val pad = w * .060f

        drawText(
            canvas,
            brandSocial,
            pad,
            h * .028f,
            w * .033f,
            Color.WHITE,
            true,
            Paint.Align.LEFT
        )
        drawText(
            canvas,
            brandSite,
            pad,
            h * .064f,
            w * .0205f,
            Color.rgb(191, 191, 198),
            false,
            Paint.Align.LEFT
        )

        paint.color = accentRed
        canvas.drawRoundRect(
            pad,
            h * .091f,
            pad + w * .13f,
            h * .095f,
            3f,
            3f,
            paint
        )

        drawText(
            canvas,
            brandTitle,
            w - pad,
            h * .035f,
            w * .027f,
            Color.WHITE,
            true,
            Paint.Align.RIGHT
        )
        drawText(
            canvas,
            "نگاه جوان",
            w - pad,
            h * .070f,
            w * .019f,
            Color.rgb(194, 194, 201),
            false,
            Paint.Align.RIGHT
        )
    }

    private fun drawBadge(
        canvas: Canvas,
        paint: Paint,
        text: String,
        right: Float,
        top: Float,
        height: Float,
        textSize: Float
    ) {
        val measure = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = Typeface.create("sans", Typeface.BOLD)
        }
        val padX = textSize * .80f
        val width = measure.measureText(text) + padX * 2

        paint.color = accentRed
        canvas.drawRoundRect(
            right - width,
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
            top + height * .16f,
            textSize,
            Color.WHITE,
            true,
            Paint.Align.RIGHT
        )
    }

    private fun drawBody(
        canvas: Canvas,
        paint: Paint,
        body: String,
        x: Float,
        top: Float,
        width: Int,
        textSize: Float,
        safeBottom: Float,
        w: Int,
        h: Int
    ) {
        val layout = makeRtlLayout(
            text = body,
            width = width - (w * .050f).toInt(),
            size = textSize,
            color = Color.rgb(238, 238, 242),
            spacing = 1.28f,
            bold = false
        )

        val panelBottom = minOf(
            safeBottom,
            top + layout.height + h * .034f
        )

        paint.color = Color.argb(22, 255, 255, 255)
        canvas.drawRoundRect(
            x,
            top - h * .010f,
            x + width,
            panelBottom,
            w * .025f,
            w * .025f,
            paint
        )

        drawLayout(
            canvas,
            layout,
            x + w * .025f,
            top + h * .007f
        )
    }

    private fun imageHeight(
        format: CardFormat,
        h: Int
    ): Float =
        when (format) {
            CardFormat.STORY -> h * .255f
            CardFormat.PORTRAIT -> h * .245f
            CardFormat.SQUARE -> h * .225f
            CardFormat.LANDSCAPE -> h * .29f
        }

    private fun titleSize(
        format: CardFormat,
        length: Int
    ): Float {
        val w = format.width.toFloat()
        val base = when {
            length < 44 -> w * .068f
            length < 78 -> w * .058f
            length < 118 -> w * .049f
            else -> w * .043f
        }
        return when (format) {
            CardFormat.LANDSCAPE -> base * .82f
            CardFormat.SQUARE -> base * .92f
            else -> base
        }
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

    private fun drawRoundedFocalImage(
        canvas: Canvas,
        bitmap: Bitmap,
        dest: RectF,
        paint: Paint,
        focusX: Float,
        focusY: Float,
        radius: Float
    ) {
        val destW = dest.width()
        val destH = dest.height()
        val scale = max(
            destW / bitmap.width,
            destH / bitmap.height
        )
        val srcW = destW / scale
        val srcH = destH / scale

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

        val path = Path().apply {
            addRoundRect(dest, radius, radius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, src, dest, paint)

        paint.shader = LinearGradient(
            dest.left,
            dest.top,
            dest.left,
            dest.bottom,
            intArrayOf(
                Color.argb(5, 0, 0, 0),
                Color.argb(55, 0, 0, 0)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(dest, paint)
        paint.shader = null
        canvas.restore()
    }

    private fun makeRtlLayout(
        text: String,
        width: Int,
        size: Float,
        color: Int,
        spacing: Float,
        bold: Boolean
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
}
