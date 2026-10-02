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
import android.graphics.RenderEffect
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
import ir.negahjavan.newscard.model.NewsLength
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.model.SmartPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object CardBitmapRenderer {

    private val accentRed = Color.rgb(232, 21, 35)
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
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        drawPremiumBackground(
            canvas = canvas,
            paint = paint,
            analysis = analysis,
            w = w,
            h = h,
            shortCover = page.isFirst &&
                analysis.length == NewsLength.SHORT &&
                !page.showImage
        )

        drawBrandLockup(canvas, paint, w, h)

        val pad = w * .060f
        val gap = max(18f, h * .017f)
        val headerBottom = h * .118f
        val imageTop = h * .137f
        val imageHeight = imageHeight(
            format = format,
            h = h,
            length = analysis.length,
            hasImage = page.showImage
        )
        val safeBottom = h * .948f
        val badgeHeight = h * .043f

        var contentTop = headerBottom + gap

        val photo = if (page.showImage) {
            imageUri?.let { loadBitmap(context, it) }
        } else {
            null
        }

        if (photo != null && imageHeight > 0f) {
            val imageRect = RectF(
                pad,
                imageTop,
                w - pad,
                imageTop + imageHeight
            )

            drawSmartImagePanel(
                canvas = canvas,
                paint = paint,
                bitmap = photo,
                dest = imageRect,
                radius = w * .032f
            )

            contentTop = imageRect.bottom + gap
        }

        val shortNoImage =
            page.isFirst &&
                analysis.length == NewsLength.SHORT &&
                photo == null

        drawBadge(
            canvas = canvas,
            paint = paint,
            text = page.badge.ifBlank {
                if (page.isContinuation) "ادامه خبر" else analysis.badge
            },
            right = w - pad,
            top = contentTop,
            height = badgeHeight,
            textSize = w * .0245f,
            outlined = shortNoImage
        )

        if (page.isFirst) {
            val titleTop = contentTop + badgeHeight + gap
            val titleLayout = makeRtlLayout(
                text = page.title,
                width = (w - 2 * pad).toInt(),
                size = SmartPaginationEngine.titleSize(
                    format = format,
                    length = page.title.length
                ),
                color = Color.WHITE,
                spacing = 1.07f,
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
                    h = h,
                    shortCover = shortNoImage
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
                h = h,
                shortCover = false
            )
        }

        bitmap
    }

    private suspend fun drawSmartImagePanel(
        canvas: Canvas,
        paint: Paint,
        bitmap: Bitmap,
        dest: RectF,
        radius: Float
    ) {
        val plan = SmartImageEngine.plan(
            bitmap = bitmap,
            destinationAspect = dest.width() / dest.height()
        )

        val clipPath = Path().apply {
            addRoundRect(dest, radius, radius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)

        when (plan.mode) {
            SmartImageEngine.Mode.SMART_CROP -> {
                drawFocalCrop(
                    canvas = canvas,
                    bitmap = bitmap,
                    dest = dest,
                    paint = paint,
                    focusX = plan.focusX,
                    focusY = plan.focusY
                )
            }

            SmartImageEngine.Mode.FIT_BLUR -> {
                val softenedWidth = 72
                val softenedHeight = max(
                    40,
                    (72f * bitmap.height / bitmap.width).toInt()
                )
                val softened = Bitmap.createScaledBitmap(
                    bitmap,
                    softenedWidth,
                    softenedHeight,
                    true
                )

                drawFocalCrop(
                    canvas = canvas,
                    bitmap = softened,
                    dest = dest,
                    paint = paint,
                    focusX = .5f,
                    focusY = .5f
                )
                paint.color = Color.argb(105, 0, 0, 0)
                canvas.drawRect(dest, paint)

                val innerPad = dest.width() * .018f
                val fitBounds = RectF(
                    dest.left + innerPad,
                    dest.top + innerPad,
                    dest.right - innerPad,
                    dest.bottom - innerPad
                )
                val fitRect = fitInside(
                    bitmap = bitmap,
                    bounds = fitBounds
                )

                paint.color = Color.BLACK
                canvas.drawRoundRect(
                    fitRect,
                    radius * .58f,
                    radius * .58f,
                    paint
                )

                canvas.drawBitmap(
                    bitmap,
                    null,
                    fitRect,
                    paint
                )
            }
        }
paint.shader = LinearGradient(
            dest.left,
            dest.top,
            dest.left,
            dest.bottom,
            intArrayOf(
                Color.argb(0, 0, 0, 0),
                Color.argb(48, 0, 0, 0)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(dest, paint)
        paint.shader = null

        canvas.restore()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(2f, dest.width() * .0022f)
        paint.color = Color.argb(88, 255, 255, 255)
        canvas.drawRoundRect(dest, radius, radius, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawPremiumBackground(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        w: Int,
        h: Int,
        shortCover: Boolean
    ) {
        val colors = categoryColors(analysis.category)

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

        paint.color = Color.argb(if (shortCover) 19 else 11, 255, 255, 255)
        canvas.drawCircle(w * .93f, h * .17f, w * .40f, paint)

        paint.color = Color.argb(if (shortCover) 17 else 9, 232, 21, 35)
        canvas.drawCircle(w * .08f, h * .86f, w * .48f, paint)

        if (shortCover) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = w * .004f
            paint.color = Color.argb(28, 255, 255, 255)
            canvas.drawCircle(
                w * .12f,
                h * .44f,
                w * .30f,
                paint
            )
            paint.style = Paint.Style.FILL

            val watermark = analysis.accentWord
                .takeIf { it.length >= 4 }
                ?: analysis.category.fa

            drawText(
                canvas = canvas,
                text = watermark,
                x = w * .50f,
                y = h * .72f,
                size = w * .095f,
                color = Color.argb(18, 255, 255, 255),
                bold = true,
                align = Paint.Align.CENTER
            )
        }

        paint.color = Color.argb(15, 255, 255, 255)
        canvas.drawRoundRect(
            w * .060f,
            h * .109f,
            w * .940f,
            h * .112f,
            3f,
            3f,
            paint
        )
    }

    private fun categoryColors(
        category: NewsCategory
    ): IntArray =
        when (category) {
            NewsCategory.JUDICIAL ->
                intArrayOf(
                    Color.rgb(31, 34, 44),
                    Color.rgb(11, 12, 16),
                    Color.rgb(52, 7, 13)
                )
            NewsCategory.POLITICS ->
                intArrayOf(
                    Color.rgb(35, 37, 50),
                    Color.rgb(11, 12, 17),
                    Color.rgb(52, 7, 13)
                )
            NewsCategory.ECONOMY ->
                intArrayOf(
                    Color.rgb(22, 43, 36),
                    Color.rgb(9, 16, 13),
                    Color.rgb(49, 7, 12)
                )
            NewsCategory.SPORTS ->
                intArrayOf(
                    Color.rgb(19, 47, 35),
                    Color.rgb(8, 17, 12),
                    Color.rgb(49, 7, 12)
                )
            NewsCategory.CULTURE ->
                intArrayOf(
                    Color.rgb(49, 33, 51),
                    Color.rgb(14, 10, 15),
                    Color.rgb(53, 7, 13)
                )
            NewsCategory.TECHNOLOGY ->
                intArrayOf(
                    Color.rgb(23, 47, 63),
                    Color.rgb(9, 15, 21),
                    Color.rgb(50, 7, 12)
                )
            else ->
                intArrayOf(
                    Color.rgb(38, 41, 53),
                    Color.rgb(11, 12, 16),
                    Color.rgb(52, 7, 13)
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
            canvas = canvas,
            text = brandSocial,
            x = pad,
            y = h * .023f,
            size = w * .036f,
            color = Color.WHITE,
            bold = true,
            align = Paint.Align.LEFT
        )

        val sitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = w * .019f
            typeface = Typeface.create("sans", Typeface.BOLD)
        }
        val siteWidth = sitePaint.measureText(brandSite)
        val pillTop = h * .069f
        val pillHeight = h * .025f
        val pillRight = pad + siteWidth + w * .030f

        paint.color = Color.argb(24, 255, 255, 255)
        canvas.drawRoundRect(
            pad,
            pillTop,
            pillRight,
            pillTop + pillHeight,
            pillHeight / 2f,
            pillHeight / 2f,
            paint
        )

        drawText(
            canvas = canvas,
            text = brandSite,
            x = pad + w * .015f,
            y = pillTop + h * .001f,
            size = w * .019f,
            color = Color.rgb(205, 205, 211),
            bold = true,
            align = Paint.Align.LEFT
        )

        paint.color = accentRed
        canvas.drawRoundRect(
            w - pad - w * .014f,
            h * .021f,
            w - pad,
            h * .078f,
            w * .007f,
            w * .007f,
            paint
        )

        drawText(
            canvas = canvas,
            text = brandTitle,
            x = w - pad - w * .032f,
            y = h * .031f,
            size = w * .029f,
            color = Color.WHITE,
            bold = true,
            align = Paint.Align.RIGHT
        )
    }

    private fun drawBadge(
        canvas: Canvas,
        paint: Paint,
        text: String,
        right: Float,
        top: Float,
        height: Float,
        textSize: Float,
        outlined: Boolean
    ) {
        val measure = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = Typeface.create("sans", Typeface.BOLD)
        }
        val padX = textSize * .82f
        val width = measure.measureText(text) + padX * 2
        val left = right - width

        if (outlined) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = max(2f, textSize * .08f)
            paint.color = Color.argb(210, 232, 21, 35)
            canvas.drawRoundRect(
                left,
                top,
                right,
                top + height,
                height / 2f,
                height / 2f,
                paint
            )
            paint.style = Paint.Style.FILL
        } else {
            paint.color = accentRed
            canvas.drawRoundRect(
                left,
                top,
                right,
                top + height,
                height / 2f,
                height / 2f,
                paint
            )
        }

        drawText(
            canvas = canvas,
            text = text,
            x = right - padX,
            y = top + height * .16f,
            size = textSize,
            color = Color.WHITE,
            bold = true,
            align = Paint.Align.RIGHT
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
        h: Int,
        shortCover: Boolean
    ) {
        val innerInset = w * .027f
        val layout = makeRtlLayout(
            text = body,
            width = width - (w * .054f).toInt(),
            size = textSize,
            color = Color.rgb(239, 239, 243),
            spacing = 1.30f,
            bold = false
        )

        if (shortCover) {
            paint.color = accentRed
            canvas.drawRoundRect(
                w - x - w * .010f,
                top,
                w - x,
                minOf(
                    safeBottom,
                    top + layout.height
                ),
                w * .005f,
                w * .005f,
                paint
            )

            drawLayout(
                canvas = canvas,
                layout = layout,
                x = x + innerInset,
                y = top
            )
            return
        }

        val panelBottom = minOf(
            safeBottom,
            top + layout.height + h * .032f
        )

        paint.color = Color.argb(21, 255, 255, 255)
        canvas.drawRoundRect(
            x,
            top - h * .009f,
            x + width,
            panelBottom,
            w * .023f,
            w * .023f,
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1.5f, w * .0015f)
        paint.color = Color.argb(28, 255, 255, 255)
        canvas.drawRoundRect(
            x,
            top - h * .009f,
            x + width,
            panelBottom,
            w * .023f,
            w * .023f,
            paint
        )
        paint.style = Paint.Style.FILL

        drawLayout(
            canvas = canvas,
            layout = layout,
            x = x + innerInset,
            y = top + h * .006f
        )
    }

    private fun imageHeight(
        format: CardFormat,
        h: Int,
        length: NewsLength,
        hasImage: Boolean
    ): Float {
        if (!hasImage) return 0f

        val ratio = when (length) {
            NewsLength.SHORT -> when (format) {
                CardFormat.STORY -> .30f
                CardFormat.PORTRAIT -> .28f
                CardFormat.SQUARE -> .25f
                CardFormat.LANDSCAPE -> .31f
            }
            NewsLength.MEDIUM -> when (format) {
                CardFormat.STORY -> .245f
                CardFormat.PORTRAIT -> .23f
                CardFormat.SQUARE -> .21f
                CardFormat.LANDSCAPE -> .27f
            }
            NewsLength.LONG -> when (format) {
                CardFormat.STORY -> .205f
                CardFormat.PORTRAIT -> .19f
                CardFormat.SQUARE -> .17f
                CardFormat.LANDSCAPE -> .23f
            }
        }

        return h * ratio
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
        dest: RectF,
        paint: Paint,
        focusX: Float,
        focusY: Float
    ) {
        val scale = max(
            dest.width() / bitmap.width,
            dest.height() / bitmap.height
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

        canvas.drawBitmap(
            bitmap,
            src,
            dest,
            paint
        )
    }

    private fun fitInside(
        bitmap: Bitmap,
        bounds: RectF
    ): RectF {
        val sourceAspect =
            bitmap.width.toFloat() / bitmap.height.toFloat()
        val destAspect =
            bounds.width() / bounds.height()

        return if (sourceAspect > destAspect) {
            val height = bounds.width() / sourceAspect
            val top = bounds.centerY() - height / 2f
            RectF(
                bounds.left,
                top,
                bounds.right,
                top + height
            )
        } else {
            val width = bounds.height() * sourceAspect
            val left = bounds.centerX() - width / 2f
            RectF(
                left,
                bounds.top,
                left + width,
                bounds.bottom
            )
        }
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

        canvas.drawText(
            text,
            x,
            y - p.ascent(),
            p
        )
    }
}
