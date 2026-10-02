package ir.negahjavan.newscard.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextDirectionHeuristics
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import ir.negahjavan.newscard.model.CardFormat
import ir.negahjavan.newscard.model.SmartAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object CardBitmapRenderer {
    suspend fun render(context: Context, analysis: SmartAnalysis, imageUri: Uri?, format: CardFormat): Bitmap = withContext(Dispatchers.Default) {
        val w = format.width
        val h = format.height
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(Color.rgb(11, 11, 14))

        val photo = imageUri?.let { loadBitmap(context, it) }
        val imageHeight = when (format) {
            CardFormat.STORY -> (h * .54f).toInt()
            CardFormat.PORTRAIT -> (h * .48f).toInt()
            CardFormat.SQUARE -> (h * .46f).toInt()
            CardFormat.LANDSCAPE -> h
        }

        if (photo != null) {
            drawCenterCrop(canvas, photo, Rect(0, 0, w, imageHeight), paint)
        } else {
            paint.shader = LinearGradient(0f, 0f, w.toFloat(), imageHeight.toFloat(),
                intArrayOf(Color.rgb(52, 54, 68), Color.rgb(17, 17, 23), Color.rgb(120, 10, 18)), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w.toFloat(), imageHeight.toFloat(), paint)
            paint.shader = null
        }

        val gradientTop = if (format == CardFormat.LANDSCAPE) 0f else imageHeight * .58f
        paint.shader = LinearGradient(0f, gradientTop, 0f, h.toFloat(),
            intArrayOf(Color.TRANSPARENT, Color.argb(210, 5, 5, 8), Color.rgb(8, 8, 11)),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, gradientTop, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Brand block
        val pad = (w * .05f)
        paint.color = Color.rgb(227, 18, 28)
        val brandTop = pad
        canvas.drawRoundRect(pad, brandTop, pad + w * .28f, brandTop + h * .065f, 24f, 24f, paint)
        drawText(canvas, "نگاه جوان", pad + w * .14f, brandTop + h * .014f, w * .035f, Color.WHITE, true, Paint.Align.CENTER)
        drawText(canvas, "پایگاه خبری", w - pad, brandTop + h * .016f, w * .022f, Color.WHITE, false, Paint.Align.RIGHT)

        // Category
        val catY = if (format == CardFormat.LANDSCAPE) h * .18f else max(h * .43f, imageHeight * .80f)
        paint.color = Color.rgb(227, 18, 28)
        canvas.drawRoundRect(pad, catY, pad + w * .22f, catY + h * .058f, 18f, 18f, paint)
        drawText(canvas, analysis.category.fa, pad + w * .11f, catY + h * .012f, w * .038f, Color.WHITE, true, Paint.Align.CENTER)

        val titleTop = catY + h * .08f
        val titleWidth = if (format == CardFormat.LANDSCAPE) (w * .54f).toInt() else (w - 2 * pad).toInt()
        val titleX = if (format == CardFormat.LANDSCAPE) w * .40f else pad
        val titleSize = when {
            analysis.title.length < 35 -> w * .078f
            analysis.title.length < 65 -> w * .064f
            else -> w * .053f
        }
        drawRtlBlock(canvas, analysis.title, titleX, titleTop, titleWidth, titleSize, Color.WHITE, 1.08f, true)

        val leadTop = if (format == CardFormat.LANDSCAPE) h * .62f else h * .76f
        drawRtlBlock(canvas, analysis.lead, pad, leadTop, (w - 2 * pad).toInt(), w * .032f, Color.rgb(225,225,230), 1.24f, false, maxLines = if (format == CardFormat.LANDSCAPE) 2 else 3)

        // Footer divider + meta
        paint.color = Color.rgb(227,18,28)
        canvas.drawRoundRect(pad, h * .935f, w - pad, h * .939f, 3f, 3f, paint)
        drawText(canvas, JalaliDate.today(), pad, h * .955f, w * .024f, Color.LTGRAY, false, Paint.Align.LEFT)
        drawText(canvas, "@negahjavan", w / 2f, h * .955f, w * .024f, Color.LTGRAY, false, Paint.Align.CENTER)
        drawText(canvas, "NEGAH JAVAN", w - pad, h * .955f, w * .022f, Color.WHITE, true, Paint.Align.RIGHT)

        bitmap
    }

    suspend fun save(context: Context, bitmap: Bitmap, format: CardFormat): Uri? = withContext(Dispatchers.IO) {
        val name = "NegahJavan_${System.currentTimeMillis()}_${format.name}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NegahJavan")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let { outUri ->
            context.contentResolver.openOutputStream(outUri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        uri
    }

    private suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context).data(uri).allowHardware(false).build()
                loader.execute(request).image?.toBitmap()
            }.getOrNull()
        }
    }

    private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap, dest: Rect, paint: Paint) {
        val scale = max(dest.width().toFloat() / bitmap.width, dest.height().toFloat() / bitmap.height)
        val srcW = dest.width() / scale
        val srcH = dest.height() / scale
        val left = (bitmap.width - srcW) / 2f
        val top = (bitmap.height - srcH) / 2f
        val src = RectF(left, top, left + srcW, top + srcH)
        canvas.drawBitmap(bitmap, src, RectF(dest), paint)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean, align: Paint.Align) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = if (bold) Typeface.create("sans", Typeface.BOLD) else Typeface.create("sans", Typeface.NORMAL)
        }
        canvas.drawText(text, x, y - p.ascent(), p)
    }

    private fun drawRtlBlock(canvas: Canvas, text: String, x: Float, y: Float, width: Int, size: Float, color: Int, spacing: Float, bold: Boolean, maxLines: Int = 4) {
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) Typeface.create("sans", Typeface.BOLD) else Typeface.create("sans", Typeface.NORMAL)
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, tp, width)
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
