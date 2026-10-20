package ir.negahjavan.newscard.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import ir.negahjavan.newscard.model.SmartAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

enum class InstagramTemplate(val fa: String) {
    PHOTO_GOLD("عکس‌محور طلایی · وزیرمتن"),
    EDITORIAL_IVORY("ادیتوریال روشن · لاله‌زار"),
    BREAKING_RED("خبر فوری قرمز · بولد"),
    MIDNIGHT_BLUE("مینیمال شب · آبی")
}

enum class PostContentPosition(val fa: String) {
    TOP("متن بالا"),
    CENTER("متن وسط"),
    BOTTOM("متن پایین")
}

enum class PostBrandPosition(val fa: String) {
    LEFT("لوگو چپ"),
    RIGHT("لوگو راست")
}

data class InstagramPostLayout(
    val contentPosition: PostContentPosition = PostContentPosition.BOTTOM,
    val brandPosition: PostBrandPosition = PostBrandPosition.RIGHT
)

/** Native post renderer shared by still-image preview/export and video overlays.
 * The cover carries a short lead; the complete article is preserved in the caption.
 */
object InstagramPostRenderer {
    fun caption(news: SmartAnalysis): String {
        val body = news.fullText.ifBlank { news.bodyText.ifBlank { news.lead } }.trim()
        return listOf(
            news.title.trim().takeUnless { body.startsWith(it) }.orEmpty(),
            body,
            "پایگاه خبری نگاه جوان\nnegahjavan.ir",
            news.hashtags.joinToString(" ") { if (it.startsWith("#")) it else "#$it" }
        ).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    suspend fun render(
        context: Context,
        news: SmartAnalysis,
        imageUri: Uri?,
        template: InstagramTemplate,
        layout: InstagramPostLayout = InstagramPostLayout()
    ): Bitmap {
        PremiumTypography.initialize(context)
        val photo = if (imageUri == null) null else withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            try {
                requireNotNull(
                    loader.execute(
                        ImageRequest.Builder(context)
                            .data(imageUri)
                            .size(1080, 1350)
                            .allowHardware(false)
                            .build()
                    ).image?.toBitmap()
                ) { "تصویر باز نشد؛ دوباره آن را انتخاب کنید." }
            } finally {
                loader.shutdown()
            }
        }
        return withContext(Dispatchers.Default) { draw(news, photo, template, false, layout) }
    }

    internal fun draw(
        news: SmartAnalysis,
        photo: Bitmap?,
        template: InstagramTemplate,
        videoOverlay: Boolean = false,
        layout: InstagramPostLayout = InstagramPostLayout()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val darkGreen = Color.rgb(9, 39, 33)
        val ivory = Color.rgb(250, 247, 238)
        val nearBlack = Color.rgb(20, 15, 18)
        val midnight = Color.rgb(9, 21, 38)
        val gold = Color.rgb(224, 194, 129)
        val red = Color.rgb(232, 42, 55)
        val blue = Color.rgb(92, 171, 255)

        val background = when (template) {
            InstagramTemplate.PHOTO_GOLD -> darkGreen
            InstagramTemplate.EDITORIAL_IVORY -> ivory
            InstagramTemplate.BREAKING_RED -> nearBlack
            InstagramTemplate.MIDNIGHT_BLUE -> midnight
        }
        val accent = when (template) {
            InstagramTemplate.PHOTO_GOLD, InstagramTemplate.EDITORIAL_IVORY -> gold
            InstagramTemplate.BREAKING_RED -> red
            InstagramTemplate.MIDNIGHT_BLUE -> blue
        }
        val lightSurface = template == InstagramTemplate.EDITORIAL_IVORY
        val ink = if (lightSurface) darkGreen else ivory

        fun box(left: Float, top: Float, right: Float, bottom: Float, color: Int, radius: Float = 24f) {
            paint.shader = null
            paint.color = color
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, paint)
        }

        if (!videoOverlay) canvas.drawColor(background)

        if (photo != null) {
            val dest = if (lightSurface) RectF(28f, 150f, 1052f, 720f) else RectF(0f, 0f, 1080f, 1350f)
            val scale = max(dest.width() / photo.width, dest.height() / photo.height)
            canvas.save()
            canvas.clipRect(dest)
            canvas.drawBitmap(
                photo,
                null,
                RectF(
                    dest.centerX() - photo.width * scale / 2,
                    dest.centerY() - photo.height * scale / 2,
                    dest.centerX() + photo.width * scale / 2,
                    dest.centerY() + photo.height * scale / 2
                ),
                paint
            )
            canvas.restore()
        } else if (!videoOverlay) {
            val gradientEnd = when (template) {
                InstagramTemplate.PHOTO_GOLD -> Color.rgb(57, 96, 78)
                InstagramTemplate.EDITORIAL_IVORY -> Color.rgb(235, 226, 205)
                InstagramTemplate.BREAKING_RED -> Color.rgb(88, 19, 27)
                InstagramTemplate.MIDNIGHT_BLUE -> Color.rgb(20, 63, 102)
            }
            paint.shader = LinearGradient(
                0f, 150f, 1080f, 720f,
                intArrayOf(background, gradientEnd),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(28f, 150f, 1052f, 720f, paint)
            paint.shader = null
            text(
                canvas,
                when (template) {
                    InstagramTemplate.BREAKING_RED -> "خبر فوری"
                    InstagramTemplate.MIDNIGHT_BLUE -> "نگاه امروز"
                    else -> "روایت امروز"
                },
                100f, 340f, 880, 180, 100f, 70f, 1, accent, PremiumTypography.headline
            )
        }

        val panelTop = when (layout.contentPosition) {
            PostContentPosition.TOP -> 196f
            PostContentPosition.CENTER -> 450f
            PostContentPosition.BOTTOM -> 700f
        }
        val panelBottom = (panelTop + 610f).coerceAtMost(1320f)

        if (lightSurface && (videoOverlay || layout.contentPosition != PostContentPosition.BOTTOM)) {
            box(28f, panelTop, 1052f, panelBottom, Color.argb(245, 250, 247, 238), 30f)
        }

        if (!lightSurface) {
            val fadeStart = (panelTop - 160f).coerceAtLeast(0f)
            paint.shader = LinearGradient(
                0f, fadeStart, 0f, panelBottom + 40f,
                intArrayOf(Color.TRANSPARENT, Color.argb(235, Color.red(background), Color.green(background), Color.blue(background))),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, fadeStart, 1080f, 1350f, paint)
            paint.shader = null

            val panelColor = when (template) {
                InstagramTemplate.PHOTO_GOLD -> Color.argb(224, 4, 28, 25)
                InstagramTemplate.BREAKING_RED -> Color.argb(232, 28, 12, 17)
                InstagramTemplate.MIDNIGHT_BLUE -> Color.argb(230, 8, 26, 46)
                InstagramTemplate.EDITORIAL_IVORY -> Color.TRANSPARENT
            }
            box(28f, panelTop, 1052f, panelBottom, panelColor, 30f)
            paint.color = accent
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (template == InstagramTemplate.BREAKING_RED) 4f else 2f
            canvas.drawRoundRect(RectF(28f, panelTop, 1052f, panelBottom), 30f, 30f, paint)
            paint.style = Paint.Style.FILL
        }

        val brandLeft = if (layout.brandPosition == PostBrandPosition.RIGHT) 700f else 48f
        val brandRight = brandLeft + 332f
        val categoryLeft = if (layout.brandPosition == PostBrandPosition.RIGHT) 48f else 800f
        val categoryRight = categoryLeft + 232f

        box(brandLeft, 42f, brandRight, 150f, ivory)
        text(canvas, "نگاه جوان", brandLeft + 24f, 55f, 282, 82, 48f, 40f, 1, darkGreen, PremiumTypography.headline)
        box(categoryLeft, 60f, categoryRight, 138f, accent, 18f)
        text(canvas, news.category.fa, categoryLeft + 16f, 68f, 200, 60, 27f, 22f, 1, darkGreen, PremiumTypography.bold)

        val strapY = panelTop + 24f
        val titleY = panelTop + 75f
        val leadY = panelTop + 360f
        val dividerY = panelTop + 470f
        val moreY = panelTop + 486f
        val siteY = panelBottom - 30f

        text(
            canvas, "نگاه جوان • روایت خبر", 58f, strapY, 964, 42, 24f, 22f, 1,
            if (lightSurface) darkGreen else accent, PremiumTypography.body
        )
        text(
            canvas, news.title, 58f, titleY, 964, 275,
            when (template) {
                InstagramTemplate.EDITORIAL_IVORY -> 86f
                InstagramTemplate.BREAKING_RED -> 82f
                else -> 78f
            },
            42f, 3, ink,
            if (template == InstagramTemplate.EDITORIAL_IVORY || template == InstagramTemplate.BREAKING_RED) {
                PremiumTypography.headline
            } else {
                PremiumTypography.bold
            }
        )
        text(
            canvas,
            news.lead.ifBlank { news.bodyText },
            58f, leadY, 964, 90, 32f, 24f, 2,
            if (lightSurface) Color.rgb(65, 85, 76) else Color.rgb(220, 230, 226),
            PremiumTypography.body
        )
        paint.color = accent
        paint.strokeWidth = 2f
        canvas.drawLine(58f, dividerY, 1022f, dividerY, paint)
        text(
            canvas, "ادامه خبر در کپشن", 540f, moreY, 482, 52, 30f, 25f, 1,
            if (lightSurface) darkGreen else accent, PremiumTypography.bold
        )
        paint.color = ink
        paint.typeface = PremiumTypography.bold
        paint.textSize = 33f
        paint.isFakeBoldText = true
        canvas.drawText("negahjavan.ir", 58f, siteY, paint)

        return bitmap
    }

    private fun text(
        canvas: Canvas,
        value: String,
        x: Float,
        y: Float,
        width: Int,
        height: Int,
        size: Float,
        minSize: Float,
        lines: Int,
        color: Int,
        font: Typeface
    ) {
        if (value.isBlank()) return
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            typeface = font
        }

        fun layout(maxLines: Int): StaticLayout =
            StaticLayout.Builder.obtain(value, 0, value.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setIncludePad(false)
                .setLineSpacing(0f, 1.15f)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()

        paint.textSize = size
        var measured = layout(Int.MAX_VALUE)
        while ((measured.height > height || measured.lineCount > lines) && paint.textSize > minSize) {
            paint.textSize = max(minSize, paint.textSize - 2f)
            measured = layout(Int.MAX_VALUE)
        }

        var visibleLines = lines
        var final = layout(visibleLines)
        while (final.height > height && visibleLines > 1) {
            visibleLines--
            final = layout(visibleLines)
        }

        canvas.save()
        canvas.translate(x, y)
        canvas.clipRect(0, 0, width, height)
        final.draw(canvas)
        canvas.restore()
    }
}
