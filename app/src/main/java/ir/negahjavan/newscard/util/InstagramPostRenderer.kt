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
    EDITORIAL_IVORY("ادیتوریال روشن · لاله‌زار")
}

/** Native port of NegahJavan/src/news-tools.js initializeInstagramPost.
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

    suspend fun render(context: Context, news: SmartAnalysis, imageUri: Uri?, template: InstagramTemplate): Bitmap {
        PremiumTypography.initialize(context)
        val photo = if (imageUri == null) null else withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            try {
                requireNotNull(loader.execute(ImageRequest.Builder(context).data(imageUri)
                    .size(1080, 1350).allowHardware(false).build()).image?.toBitmap()) {
                    "تصویر باز نشد؛ دوباره آن را انتخاب کنید."
                }
            } finally { loader.shutdown() }
        }
        return withContext(Dispatchers.Default) { draw(news, photo, template) }
    }

    internal fun draw(news: SmartAnalysis, photo: Bitmap?, template: InstagramTemplate, videoOverlay: Boolean = false): Bitmap {
        val bitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val dark = Color.rgb(9, 39, 33)
        val gold = Color.rgb(224, 194, 129)
        val ivory = Color.rgb(250, 247, 238)
        val editorial = template == InstagramTemplate.EDITORIAL_IVORY
        if (!videoOverlay) canvas.drawColor(if (editorial) ivory else dark)
        fun box(left: Float, top: Float, right: Float, bottom: Float, color: Int, radius: Float = 24f) {
            paint.shader = null; paint.color = color; paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, paint)
        }
        if (photo != null) {
            val dest = if (editorial) RectF(48f, 180f, 1032f, 720f) else RectF(0f, 0f, 1080f, 1350f)
            val scale = max(dest.width() / photo.width, dest.height() / photo.height)
            canvas.save()
            canvas.clipRect(dest)
            canvas.drawBitmap(photo, null, RectF(dest.centerX() - photo.width * scale / 2,
                dest.centerY() - photo.height * scale / 2, dest.centerX() + photo.width * scale / 2,
                dest.centerY() + photo.height * scale / 2), paint)
            canvas.restore()
        } else if (!videoOverlay) {
            paint.shader = LinearGradient(0f, 180f, 1080f, 720f,
                intArrayOf(dark, Color.rgb(57, 96, 78)), null, Shader.TileMode.CLAMP)
            canvas.drawRect(48f, 180f, 1032f, 720f, paint)
            paint.shader = null
            text(canvas, "روایت امروز", 100f, 340f, 880, 180, 100f, 70f, 1, gold, PremiumTypography.headline)
        }
        if (videoOverlay && editorial) box(48f, 742f, 1032f, 1272f, ivory, 30f)
        if (!editorial) {
            paint.shader = LinearGradient(0f, 450f, 0f, 1350f,
                intArrayOf(Color.TRANSPARENT, Color.argb(240, 2, 18, 16)), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, 1080f, 1350f, paint)
            paint.shader = null
            box(48f, 742f, 1032f, 1272f, Color.argb(224, 4, 28, 25), 30f)
            paint.color = gold; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
            canvas.drawRoundRect(RectF(48f, 742f, 1032f, 1272f), 30f, 30f, paint)
            paint.style = Paint.Style.FILL
        }
        box(724f, 52f, 1032f, 144f, ivory)
        text(canvas, "نگاه جوان", 750f, 65f, 255, 70, 44f, 36f, 1, dark, PremiumTypography.headline)
        box(48f, 66f, 280f, 130f, gold, 18f)
        text(canvas, news.category.fa, 64f, 74f, 200, 50, 25f, 20f, 1, dark, PremiumTypography.bold)
        val ink = if (editorial) dark else ivory
        text(canvas, "نگاه جوان • روایت خبر", 78f, 770f, 924, 42, 22f, 20f, 1,
            if (editorial) dark else gold, PremiumTypography.body)
        text(canvas, news.title, 78f, 825f, 924, 250, if (editorial) 76f else 64f, 38f, 3,
            ink, if (editorial) PremiumTypography.headline else PremiumTypography.bold)
        text(canvas, news.lead.ifBlank { news.bodyText }, 78f, 1080f, 924, 84, 29f, 23f, 2,
            if (editorial) Color.rgb(65, 85, 76) else Color.rgb(207, 221, 215), PremiumTypography.body)
        paint.color = gold; paint.strokeWidth = 2f
        canvas.drawLine(78f, 1184f, 1002f, 1184f, paint)
        text(canvas, "ادامه خبر در کپشن", 540f, 1200f, 462, 52, 28f, 24f, 1,
            if (editorial) dark else gold, PremiumTypography.bold)
        paint.color = ink; paint.typeface = PremiumTypography.bold; paint.textSize = 27f
        canvas.drawText("negahjavan.ir", 78f, 1237f, paint)
        return bitmap
    }

    private fun text(canvas: Canvas, value: String, x: Float, y: Float, width: Int, height: Int,
                     size: Float, minSize: Float, lines: Int, color: Int, font: Typeface) {
        if (value.isBlank()) return
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; typeface = font }
        fun layout(maxLines: Int): StaticLayout = StaticLayout.Builder.obtain(value, 0, value.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false).setLineSpacing(0f, 1.15f).setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END).build()
        paint.textSize = size
        var measured = layout(Int.MAX_VALUE)
        while ((measured.height > height || measured.lineCount > lines) && paint.textSize > minSize) {
            paint.textSize = max(minSize, paint.textSize - 2f)
            measured = layout(Int.MAX_VALUE)
        }
        var visibleLines = lines
        var final = layout(visibleLines)
        while (final.height > height && visibleLines > 1) { visibleLines--; final = layout(visibleLines) }
        canvas.save(); canvas.translate(x, y); canvas.clipRect(0, 0, width, height)
        final.draw(canvas); canvas.restore()
    }
}
