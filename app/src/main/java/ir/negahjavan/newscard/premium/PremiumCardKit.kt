package ir.negahjavan.newscard.premium

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat

private fun argb(v: Long): Int = v.toInt()

/**
 * رندرر کارت خبری لوکس نگاه جوان.
 * یک Bitmap برمی‌گرداند؛ Preview و PNG Export از همین خروجی استفاده می‌کنند.
 */
object PremiumCardKit {
    enum class Template(
        val bg: Int, val frame: Int, val headline: Int, val body: Int,
        val muted: Int, val accent: Int, val topBar: Boolean
    ) {
        GOLD(argb(0xFF0B0A09), argb(0x66C9A86A), argb(0xFFF3EDE0), argb(0xFFBDB4A1), argb(0xFFA59C89), argb(0xFFC9A86A), false),
        IVORY(argb(0xFFF3EDE0), argb(0xFFB89556), argb(0xFF17130A), argb(0xFF4A4233), argb(0xFF4A4233), argb(0xFF7A5C22), false),
        BREAKING(argb(0xFF0B0A09), argb(0xFF2E2819), argb(0xFFF3EDE0), argb(0xFFBDB4A1), argb(0xFFA59C89), argb(0xFFE5534B), true)
    }

    enum class HeadlineFont(val resName: String, val label: String) {
        LALEZAR("lalezar", "Lalezar"),
        VAZIRMATN("vazirmatn_bold", "Vazirmatn"),
        CAIRO("cairo_extrabold", "Cairo"),
        KUFI("noto_kufi_arabic_bold", "Noto Kufi Arabic"),
        AMIRI("amiri_bold", "Amiri"),
        AREF_RUQAA("aref_ruqaa_bold", "Aref Ruqaa")
    }

    private val cache = HashMap<String, Typeface>()

    private fun face(ctx: Context, res: String): Typeface = cache.getOrPut(res) {
        val id = ctx.resources.getIdentifier(res, "font", ctx.packageName)
        (if (id != 0) ResourcesCompat.getFont(ctx, id) else null) ?: Typeface.DEFAULT_BOLD
    }

    private fun paint(tf: Typeface, color: Int, size: Float, align: Paint.Align = Paint.Align.LEFT) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            this.color = color
            textSize = size
            textAlign = align
        }

    private fun layout(text: CharSequence, p: TextPaint, w: Int, mult: Float): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, w)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, mult)
            .setIncludePad(false)
            .build()

    private fun fit(
        text: String, p: TextPaint, w: Int, maxH: Int,
        maxSize: Float, minSize: Float, mult: Float
    ): StaticLayout {
        var size = maxSize
        while (size >= minSize) {
            p.textSize = size
            val l = layout(text, p, w, mult)
            if (l.height <= maxH) return l
            size -= maxSize * 0.04f
        }

        p.textSize = minSize
        val words = text.trim().split(" ").filter { it.isNotBlank() }.toMutableList()
        var l = layout(text, p, w, mult)
        while (words.size > 1) {
            words.removeAt(words.lastIndex)
            l = layout(words.joinToString(" ") + "…", p, w, mult)
            if (l.height <= maxH) return l
        }
        return l
    }

    private fun trimToLines(
        text: String, p: TextPaint, w: Int, mult: Float, maxLines: Int
    ): StaticLayout {
        var l = layout(text, p, w, mult)
        if (l.lineCount <= maxLines) return l

        val words = text.trim().split(" ").filter { it.isNotBlank() }.toMutableList()
        while (words.size > 1) {
            words.removeAt(words.lastIndex)
            l = layout(words.joinToString(" ") + "…", p, w, mult)
            if (l.lineCount <= maxLines) return l
        }
        return l
    }

    private fun drawLayout(cv: Canvas, l: StaticLayout, x: Float, y: Float) {
        cv.save()
        cv.translate(x, y)
        l.draw(cv)
        cv.restore()
    }

    fun render(
        ctx: Context,
        w: Int,
        h: Int,
        template: Template,
        font: HeadlineFont,
        headline: String,
        summary: String,
        label: String = "خبر",
        brand: String = "نگاه جوان",
        url: String = "negahjavan.ir",
        background: Bitmap? = null,
        autoFit: Boolean = true
    ): Bitmap {
        require(w > 0 && h > 0) { "Output dimensions must be positive." }

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val u = minOf(w.toFloat(), h * 0.75f) / 360f
        val story = h.toFloat() / w > 1.6f

        cv.drawColor(template.bg)

        if (background != null && background.width > 0 && background.height > 0) {
            val s = maxOf(w.toFloat() / background.width, h.toFloat() / background.height)
            val sw = (w / s).toInt().coerceIn(1, background.width)
            val sh = (h / s).toInt().coerceIn(1, background.height)
            val sx = ((background.width - sw) / 2).coerceAtLeast(0)
            val sy = ((background.height - sh) / 2).coerceAtLeast(0)

            cv.drawBitmap(
                background,
                Rect(sx, sy, sx + sw, sy + sh),
                Rect(0, 0, w, h),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )

            val base = template.bg and 0x00FFFFFF
            val scrim = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h.toFloat(),
                    intArrayOf(
                        (0x8C shl 24) or base,
                        base,
                        base,
                        (0xF0 shl 24) or base
                    ),
                    floatArrayOf(0f, 0.22f, 0.38f, 0.8f),
                    Shader.TileMode.CLAMP
                )
            }
            cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), scrim)
        }

        val inset = 14f * u
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * u
            color = template.frame
        }
        cv.drawRect(RectF(inset, inset, w - inset, h - inset), line)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = template.accent }
        if (template.topBar) {
            cv.drawRect(inset, inset, w - inset, inset + 3f * u, fill)
        }

        val left = inset + 26f * u
        val right = w - left
        val cw = (right - left).toInt().coerceAtLeast(1)
        val top = if (story) h * 0.12f else inset + 26f * u
        val bottom = h - if (story) h * 0.15f else inset + 20f * u

        val headFace = face(ctx, font.resName)
        cv.drawText(
            brand, right, top + 22f * u,
            paint(headFace, template.headline, 22f * u, Paint.Align.RIGHT)
        )
        cv.drawText(
            label, left, top + 20f * u,
            paint(face(ctx, "vazirmatn_bold"), template.accent, 13f * u)
        )

        val bodyFace = face(ctx, "vazirmatn_regular")
        val fp = paint(bodyFace, template.muted, 12f * u, Paint.Align.RIGHT)
        cv.drawText("ادامهٔ خبر در کپشن", right, bottom, fp)
        fp.textAlign = Paint.Align.LEFT
        fp.color = template.accent
        fp.letterSpacing = 0.08f
        cv.drawText(url, left, bottom, fp)

        val lineY = bottom - 26f * u
        line.strokeWidth = 1f * u
        cv.drawLine(left, lineY, right, lineY, line)

        val midTop = top + 42f * u
        val midBottom = lineY - 16f * u
        val midH = (midBottom - midTop).toInt().coerceAtLeast(1)
        val gap = 18f * u
        val ruleH = 2f * u

        val sl = if (summary.isNotBlank()) {
            trimToLines(
                summary.trim(),
                paint(bodyFace, template.body, 15f * u),
                cw, 1.5f, 3
            )
        } else null

        val extra = if (sl != null) gap + ruleH + gap + sl.height else 0f
        val maxHead = (midH - extra).toInt().coerceAtLeast((40f * u).toInt())
        val maxSize = (if (story) 44f else 38f) * u
        val minSize = if (autoFit) 22f * u else maxSize

        val hl = fit(
            headline.trim(),
            paint(headFace, template.headline, maxSize),
            cw,
            maxHead,
            maxSize,
            minSize,
            1.4f
        )

        val total = hl.height + extra
        val centered = story && background == null
        var y = if (centered) midTop + (midH - total) / 2f else midBottom - total
        y = maxOf(y, midTop)

        drawLayout(cv, hl, left, y)

        if (sl != null) {
            y += hl.height + gap
            cv.drawRect(right - 44f * u, y, right, y + ruleH, fill)
            y += ruleH + gap
            drawLayout(cv, sl, left, y)
        }

        return bmp
    }
}
