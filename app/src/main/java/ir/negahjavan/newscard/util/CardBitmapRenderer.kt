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
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.graphics.text.LineBreaker
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
import ir.negahjavan.newscard.model.PremiumTemplate
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.model.SmartPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object CardBitmapRenderer {

    private val accentRed = Color.rgb(232, 21, 35)
    private val accentGold = Color.rgb(214, 177, 104)
    private val accentViolet = Color.rgb(116, 91, 224)
    private val warmIvory = Color.rgb(247, 242, 233)
    private const val brandSocial = "negahjavaan"
    private const val brandSite = "negahjavan.ir"
    private const val brandTitle = "پایگاه خبری نگاه جوان"

    suspend fun renderPage(
        context: Context,
        analysis: SmartAnalysis,
        page: SmartPage,
        imageUri: Uri?,
        format: CardFormat,
        template: PremiumTemplate = PremiumTemplate.LUXURY_MINIMAL
    ): Bitmap = withContext(Dispatchers.Default) {
        PremiumTypography.initialize(context)
        val w = format.width
        val h = format.height
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val composition = SmartCompositionEngine.profile(
            analysis = analysis,
            format = format,
            hasImage = page.showImage && imageUri != null
        )

        drawPremiumBackground(
            canvas = canvas,
            paint = paint,
            analysis = analysis,
            template = template,
            w = w,
            h = h,
            shortCover = page.isFirst &&
                analysis.length == NewsLength.SHORT &&
                !page.showImage
        )

        drawBrandLockup(canvas, paint, w, h, template)

        val pad = w * composition.horizontalPadRatio
        val gap = max(18f, h * composition.gapRatio)
        val headerBottom = h * composition.headerBottomRatio
        val imageTop = h * composition.imageTopRatio
        val imageHeight = h * composition.imageHeightRatio
        val safeBottom = h * composition.safeBottomRatio
        val badgeHeight = h * composition.badgeHeightRatio

        var contentTop = if (
            !page.showImage &&
            composition.shortContentStartRatio > 0f
        ) {
            max(
                headerBottom + gap,
                h * composition.shortContentStartRatio
            )
        } else {
            headerBottom + gap
        }

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
                radius = w * .032f,
                template = template
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
            textSize = when {
                page.badge.length >= 18 -> w * .0205f
                page.badge.length >= 12 -> w * .0225f
                else -> w * .0245f
            },
            outlined = shortNoImage,
            template = template
        )

        if (page.isFirst) {
            val titleTop = contentTop + badgeHeight + gap
            val titleLayout = makeRtlLayout(
                text = highlightedTitle(page.title, analysis.accentWord, template),
                width = (w - 2 * pad).toInt(),
                size = composition.titleSizePx,
                color = when (template) {
                    PremiumTemplate.LUXURY_MINIMAL -> warmIvory
                    PremiumTemplate.PHOTO_PREMIUM -> Color.WHITE
                    PremiumTemplate.EDITORIAL_GLASS -> Color.WHITE
                },
                spacing = composition.titleLineSpacing,
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
                    shortCover = shortNoImage,
                    template = template,
                    lineSpacing = composition.bodyLineSpacing,
                    density = composition.density
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
                shortCover = false,
                template = template,
                lineSpacing = composition.bodyLineSpacing,
                density = composition.density
            )
        }

        drawBrandFooter(
            canvas = canvas,
            paint = paint,
            w = w,
            h = h,
            template = template
        )

        bitmap
    }

    private suspend fun drawSmartImagePanel(
        canvas: Canvas,
        paint: Paint,
        bitmap: Bitmap,
        dest: RectF,
        radius: Float,
        template: PremiumTemplate
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
        paint.color = when (template) {
            PremiumTemplate.LUXURY_MINIMAL -> Color.argb(190, 214, 177, 104)
            PremiumTemplate.PHOTO_PREMIUM -> Color.argb(190, 116, 91, 224)
            PremiumTemplate.EDITORIAL_GLASS -> Color.argb(145, 255, 255, 255)
        }
        canvas.drawRoundRect(dest, radius, radius, paint)

        if (template == PremiumTemplate.LUXURY_MINIMAL) {
            val inner = RectF(
                dest.left + dest.width() * .018f,
                dest.top + dest.height() * .025f,
                dest.right - dest.width() * .018f,
                dest.bottom - dest.height() * .025f
            )
            paint.strokeWidth = max(1.5f, dest.width() * .0014f)
            paint.color = Color.argb(70, 214, 177, 104)
            canvas.drawRoundRect(inner, radius * .72f, radius * .72f, paint)
        } else if (template == PremiumTemplate.EDITORIAL_GLASS) {
            paint.strokeWidth = max(5f, dest.width() * .004f)
            paint.color = accentRed
            canvas.drawLine(
                dest.right - dest.width() * .16f,
                dest.top,
                dest.right,
                dest.top,
                paint
            )
        }

        paint.style = Paint.Style.FILL
    }

    private fun accent(template: PremiumTemplate): Int = when (template) {
        PremiumTemplate.LUXURY_MINIMAL -> accentGold
        PremiumTemplate.PHOTO_PREMIUM -> Color.rgb(190, 170, 255)
        PremiumTemplate.EDITORIAL_GLASS -> Color.rgb(255, 101, 113)
    }

    // Color-only emphasis keeps the headline's measured geometry unchanged.
    private fun highlightedTitle(title: String, word: String, template: PremiumTemplate): CharSequence {
        val start = if (word.isNotBlank()) title.indexOf(word) else -1
        return SpannableString(title).apply {
            if (start >= 0) setSpan(ForegroundColorSpan(accent(template)), start,
                start + word.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun drawPremiumBackground(
        canvas: Canvas, paint: Paint, analysis: SmartAnalysis,
        template: PremiumTemplate, w: Int, h: Int, shortCover: Boolean
    ) {
        val colors = when (template) {
            PremiumTemplate.LUXURY_MINIMAL -> intArrayOf(Color.rgb(25, 28, 29), Color.rgb(10, 14, 17), Color.rgb(20, 23, 24))
            PremiumTemplate.PHOTO_PREMIUM -> intArrayOf(Color.rgb(34, 27, 52), Color.rgb(14, 14, 25), Color.rgb(28, 23, 43))
            PremiumTemplate.EDITORIAL_GLASS -> intArrayOf(Color.rgb(34, 22, 28), Color.rgb(16, 17, 22), Color.rgb(25, 17, 23))
        }
        paint.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
        val tint = accent(template)
        // Fine architectural frame outside the reading area.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, w * .001f)
        paint.color = Color.argb(65, Color.red(tint), Color.green(tint), Color.blue(tint))
        canvas.drawRoundRect(w * .031f, h * .018f, w * .969f, h * .980f,
            w * .025f, w * .025f, paint)
        paint.style = Paint.Style.FILL
        // A subtle foil rule, never behind the text.
        paint.shader = LinearGradient(w * .075f, 0f, w * .925f, 0f,
            intArrayOf(Color.TRANSPARENT, tint, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        canvas.drawRect(w * .075f, h * .111f, w * .925f, h * .112f, paint)
        canvas.drawRect(w * .075f, h * .944f, w * .925f, h * .945f, paint)
        paint.shader = null
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

    private fun drawBrandLockup(canvas: Canvas, paint: Paint, w: Int, h: Int, template: PremiumTemplate) {
        val pad = w * .075f
        val tint = accent(template)
        drawText(canvas, "نگاه جوان", w - pad, h * .027f, w * .040f, warmIvory, true, Paint.Align.RIGHT)
        drawText(canvas, "پایگاه خبری", w - pad, h * .074f, w * .017f, tint, false, Paint.Align.RIGHT)
        drawText(canvas, "NEGAH JAVAAN", pad, h * .032f, w * .019f, tint, true, Paint.Align.LEFT)
        drawText(canvas, brandSite, pad, h * .068f, w * .017f, Color.rgb(178, 181, 190), false, Paint.Align.LEFT)
    }

    private fun drawBadge(
        canvas: Canvas,
        paint: Paint,
        text: String,
        right: Float,
        top: Float,
        height: Float,
        textSize: Float,
        outlined: Boolean,
        template: PremiumTemplate
    ) {
        val measure = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = PremiumTypography.bold
        }
        val padX = textSize * .82f
        val width = measure.measureText(text) + padX * 2
        val left = right - width

        when (template) {
            PremiumTemplate.LUXURY_MINIMAL -> {
                paint.color = Color.rgb(120, 23, 55)
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
            PremiumTemplate.PHOTO_PREMIUM -> {
                paint.color = accentViolet
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
            PremiumTemplate.EDITORIAL_GLASS -> {
                if (outlined) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = max(2f, textSize * .08f)
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
            }
        }

        drawText(
            canvas = canvas,
            text = text,
            x = right - padX,
            y = top + height * .16f,
            size = textSize,
            color = when (template) {
                PremiumTemplate.LUXURY_MINIMAL -> warmIvory
                PremiumTemplate.PHOTO_PREMIUM -> Color.WHITE
                PremiumTemplate.EDITORIAL_GLASS -> Color.WHITE
            },
            bold = true,
            align = Paint.Align.RIGHT
        )
    }

    private fun drawBrandFooter(
        canvas: Canvas,
        paint: Paint,
        w: Int,
        h: Int,
        template: PremiumTemplate
    ) {
        val y = h * .944f

        when (template) {
            PremiumTemplate.LUXURY_MINIMAL -> {
                drawText(
                    canvas = canvas,
                    text = brandSite.uppercase(),
                    x = w * .50f,
                    y = y,
                    size = w * .023f,
                    color = Color.rgb(164, 166, 173),
                    bold = true,
                    align = Paint.Align.CENTER
                )
                paint.color = accentGold
                canvas.drawRoundRect(
                    w * .37f,
                    h * .976f,
                    w * .63f,
                    h * .979f,
                    3f,
                    3f,
                    paint
                )
            }

            PremiumTemplate.PHOTO_PREMIUM -> {
                drawText(
                    canvas = canvas,
                    text = brandSite.uppercase(),
                    x = w * .50f,
                    y = y,
                    size = w * .023f,
                    color = Color.rgb(188, 180, 228),
                    bold = true,
                    align = Paint.Align.CENTER
                )
                paint.color = accentViolet
                canvas.drawRoundRect(
                    w * .43f,
                    h * .976f,
                    w * .57f,
                    h * .979f,
                    3f,
                    3f,
                    paint
                )
            }

            PremiumTemplate.EDITORIAL_GLASS -> {
                drawText(
                    canvas = canvas,
                    text = brandSite,
                    x = w - w * .060f,
                    y = y,
                    size = w * .024f,
                    color = Color.rgb(208, 208, 214),
                    bold = true,
                    align = Paint.Align.RIGHT
                )
                drawText(
                    canvas = canvas,
                    text = brandSocial,
                    x = w * .060f,
                    y = y,
                    size = w * .024f,
                    color = Color.WHITE,
                    bold = true,
                    align = Paint.Align.LEFT
                )
            }
        }
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
        shortCover: Boolean,
        template: PremiumTemplate,
        lineSpacing: Float,
        density: SmartCompositionEngine.Density
    ) {
        val innerInset = when (density) {
            SmartCompositionEngine.Density.AIRY -> w * .018f
            SmartCompositionEngine.Density.BALANCED -> w * .024f
            SmartCompositionEngine.Density.DENSE -> w * .027f
        }
        val layout = makeRtlLayout(
            text = body,
            width = width - (w * .054f).toInt(),
            size = textSize,
            color = Color.rgb(239, 239, 243),
            spacing = lineSpacing,
            bold = false
        )

        if (shortCover) {
            paint.color = when (template) {
                PremiumTemplate.LUXURY_MINIMAL -> accentGold
                PremiumTemplate.PHOTO_PREMIUM -> accentViolet
                PremiumTemplate.EDITORIAL_GLASS -> accentRed
            }
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
            top + layout.height + when (density) {
                SmartCompositionEngine.Density.AIRY -> h * .026f
                SmartCompositionEngine.Density.BALANCED -> h * .030f
                SmartCompositionEngine.Density.DENSE -> h * .034f
            }
        )

        val panelRadius = when (template) {
            PremiumTemplate.LUXURY_MINIMAL -> w * .023f
            PremiumTemplate.PHOTO_PREMIUM -> w * .032f
            PremiumTemplate.EDITORIAL_GLASS -> w * .030f
        }

        paint.color = when (template) {
            PremiumTemplate.LUXURY_MINIMAL -> when (density) {
                SmartCompositionEngine.Density.AIRY -> Color.argb(8, 255, 255, 255)
                SmartCompositionEngine.Density.BALANCED -> Color.argb(16, 255, 255, 255)
                SmartCompositionEngine.Density.DENSE -> Color.argb(23, 255, 255, 255)
            }
            PremiumTemplate.PHOTO_PREMIUM -> when (density) {
                SmartCompositionEngine.Density.AIRY -> Color.argb(62, 0, 0, 0)
                SmartCompositionEngine.Density.BALANCED -> Color.argb(82, 0, 0, 0)
                SmartCompositionEngine.Density.DENSE -> Color.argb(96, 0, 0, 0)
            }
            PremiumTemplate.EDITORIAL_GLASS -> when (density) {
                SmartCompositionEngine.Density.AIRY -> Color.argb(22, 255, 255, 255)
                SmartCompositionEngine.Density.BALANCED -> Color.argb(31, 255, 255, 255)
                SmartCompositionEngine.Density.DENSE -> Color.argb(39, 255, 255, 255)
            }
        }
        canvas.drawRoundRect(
            x,
            top - h * .009f,
            x + width,
            panelBottom,
            panelRadius,
            panelRadius,
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1.5f, w * .0015f)
        paint.color = when (template) {
            PremiumTemplate.LUXURY_MINIMAL -> Color.argb(24, 255, 255, 255)
            PremiumTemplate.PHOTO_PREMIUM -> Color.argb(54, 190, 170, 255)
            PremiumTemplate.EDITORIAL_GLASS -> Color.argb(48, 255, 255, 255)
        }
        canvas.drawRoundRect(
            x,
            top - h * .009f,
            x + width,
            panelBottom,
            panelRadius,
            panelRadius,
            paint
        )
        paint.style = Paint.Style.FILL

        drawLayout(
            canvas = canvas,
            layout = layout,
            x = x + innerInset,
            y = top
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
        text: CharSequence,
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
                PremiumTypography.headline
            } else {
                PremiumTypography.body
            }
        }

        return StaticLayout.Builder
            .obtain(text, 0, text.length, tp, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
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
                PremiumTypography.bold
            } else {
                PremiumTypography.body
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

