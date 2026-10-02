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
import ir.negahjavan.newscard.model.SmartAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object CardBitmapRenderer {
    private val brandRed = Color.rgb(227, 18, 28)

    suspend fun render(
        context: Context,
        analysis: SmartAnalysis,
        imageUri: Uri?,
        format: CardFormat,
        template: CardTemplate
    ): Bitmap = withContext(Dispatchers.Default) {
        val w = format.width
        val h = format.height
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val pad = w * .05f

        drawBackground(canvas, paint, context, imageUri, w, h, template)

        when (template) {
            CardTemplate.SIGNATURE -> drawSignature(canvas, paint, analysis, format, w, h, pad)
            CardTemplate.BREAKING_PLUS -> drawBreaking(canvas, paint, analysis, format, w, h, pad)
            CardTemplate.EDITORIAL -> drawEditorial(canvas, paint, analysis, format, w, h, pad)
        }

        bitmap
    }

    private suspend fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        context: Context,
        imageUri: Uri?,
        w: Int,
        h: Int,
        template: CardTemplate
    ) {
        canvas.drawColor(Color.rgb(11, 11, 14))
        val photo = imageUri?.let { loadBitmap(context, it) }

        if (photo != null) {
            drawCenterCrop(canvas, photo, Rect(0, 0, w, h), paint)
        } else {
            val colors = when (template) {
                CardTemplate.SIGNATURE -> intArrayOf(
                    Color.rgb(54, 57, 72), Color.rgb(18, 18, 23), Color.rgb(104, 9, 17)
                )
                CardTemplate.BREAKING_PLUS -> intArrayOf(
                    Color.rgb(88, 7, 12), Color.rgb(25, 25, 29), Color.rgb(8, 8, 10)
                )
                CardTemplate.EDITORIAL -> intArrayOf(
                    Color.rgb(68, 72, 80), Color.rgb(25, 27, 32), Color.rgb(14, 15, 18)
                )
            }
            paint.shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(), colors, null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            paint.shader = null
        }

        val overlay = when (template) {
            CardTemplate.SIGNATURE -> intArrayOf(
                Color.TRANSPARENT, Color.argb(70, 0, 0, 0), Color.argb(240, 6, 6, 9)
            )
            CardTemplate.BREAKING_PLUS -> intArrayOf(
                Color.argb(35, 0, 0, 0), Color.argb(110, 0, 0, 0), Color.argb(245, 5, 5, 7)
            )
            CardTemplate.EDITORIAL -> intArrayOf(
                Color.argb(25, 0, 0, 0), Color.argb(60, 0, 0, 0), Color.argb(150, 0, 0, 0)
            )
        }

        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(), overlay, null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
    }

    private fun drawSignature(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        format: CardFormat,
        w: Int,
        h: Int,
        pad: Float
    ) {
        paint.color = brandRed
        canvas.drawRoundRect(
            pad, pad, pad + w * .29f, pad + h * .062f, 24f, 24f, paint
        )
        drawText(
            canvas, "NEGAH JAVAN", pad + w * .145f, pad + h * .012f,
            w * .027f, Color.WHITE, true, Paint.Align.CENTER
        )
        drawText(
            canvas, "پایگاه خبری نگاه جوان", w - pad, pad + h * .013f,
            w * .024f, Color.WHITE, true, Paint.Align.RIGHT
        )

        val catY = if (format == CardFormat.LANDSCAPE) h * .22f else h * .49f
        paint.color = brandRed
        canvas.drawRoundRect(
            w - pad - w * .22f, catY, w - pad, catY + h * .052f, 18f, 18f, paint
        )
        drawText(
            canvas, analysis.category.fa, w - pad - w * .11f, catY + h * .009f,
            w * .035f, Color.WHITE, true, Paint.Align.CENTER
        )

        val titleTop = catY + h * .075f
        drawRtlBlock(
            canvas,
            analysis.title,
            pad,
            titleTop,
            (w - 2 * pad).toInt(),
            smartTitleSize(w, analysis.title.length),
            Color.WHITE,
            1.08f,
            true,
            4
        )

        val leadTop = if (format == CardFormat.LANDSCAPE) h * .70f else h * .77f
        drawRtlBlock(
            canvas,
            analysis.lead,
            pad,
            leadTop,
            (w - 2 * pad).toInt(),
            w * .031f,
            Color.rgb(232, 232, 236),
            1.22f,
            false,
            if (format == CardFormat.LANDSCAPE) 2 else 3
        )

        drawFooter(canvas, paint, w, h, pad, Color.LTGRAY)
    }

    private fun drawBreaking(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        format: CardFormat,
        w: Int,
        h: Int,
        pad: Float
    ) {
        paint.color = brandRed
        canvas.drawRect(0f, 0f, w.toFloat(), h * .105f, paint)

        drawText(
            canvas, "فوری", w - pad, h * .024f,
            w * .052f, Color.WHITE, true, Paint.Align.RIGHT
        )
        drawText(
            canvas, "NEGAH JAVAN • BREAKING", pad, h * .032f,
            w * .022f, Color.WHITE, true, Paint.Align.LEFT
        )

        paint.color = brandRed
        canvas.drawRoundRect(
            w - pad - w * .012f, h * .38f, w - pad, h * .73f, 10f, 10f, paint
        )

        val titleTop = if (format == CardFormat.LANDSCAPE) h * .31f else h * .50f
        drawText(
            canvas, "●  خبر فوری", w - pad - w * .03f, titleTop - h * .055f,
            w * .025f, Color.rgb(255, 64, 74), true, Paint.Align.RIGHT
        )

        val titleSize = if (analysis.title.length < 52) {
            w * .074f
        } else {
            smartTitleSize(w, analysis.title.length)
        }

        drawRtlBlock(
            canvas,
            analysis.title,
            pad,
            titleTop,
            (w - 2 * pad - w * .025f).toInt(),
            titleSize,
            Color.WHITE,
            1.05f,
            true,
            4
        )

        val panelTop = if (format == CardFormat.LANDSCAPE) h * .68f else h * .77f
        paint.color = Color.argb(190, 18, 18, 22)
        canvas.drawRoundRect(
            pad, panelTop, w - pad, panelTop + h * .125f, 22f, 22f, paint
        )

        drawRtlBlock(
            canvas,
            analysis.lead,
            pad + w * .025f,
            panelTop + h * .022f,
            (w - 2 * pad - w * .05f).toInt(),
            w * .029f,
            Color.WHITE,
            1.18f,
            false,
            2
        )

        drawFooter(canvas, paint, w, h, pad, Color.WHITE)
    }

    private fun drawEditorial(
        canvas: Canvas,
        paint: Paint,
        analysis: SmartAnalysis,
        format: CardFormat,
        w: Int,
        h: Int,
        pad: Float
    ) {
        drawText(
            canvas, "NEGAH / JAVAN", pad, pad,
            w * .025f, Color.WHITE, true, Paint.Align.LEFT
        )
        drawText(
            canvas, "پایگاه خبری نگاه جوان", w - pad, pad,
            w * .024f, Color.WHITE, false, Paint.Align.RIGHT
        )

        val panelTop = if (format == CardFormat.LANDSCAPE) h * .30f else h * .47f
        val panelBottom = h - pad

        paint.color = Color.argb(238, 246, 243, 238)
        canvas.drawRoundRect(
            pad, panelTop, w - pad, panelBottom, 28f, 28f, paint
        )

        drawText(
            canvas, analysis.category.fa, w - pad - w * .03f, panelTop + h * .035f,
            w * .026f, brandRed, true, Paint.Align.RIGHT
        )

        drawRtlBlock(
            canvas,
            analysis.title,
            pad + w * .035f,
            panelTop + h * .09f,
            (w - 2 * pad - w * .07f).toInt(),
            smartTitleSize(w, analysis.title.length) * .90f,
            Color.rgb(20, 20, 23),
            1.08f,
            true,
            4
        )

        val leadTop = if (format == CardFormat.LANDSCAPE) {
            panelTop + h * .33f
        } else {
            panelTop + h * .31f
        }

        drawRtlBlock(
            canvas,
            analysis.lead,
            pad + w * .035f,
            leadTop,
            (w - 2 * pad - w * .07f).toInt(),
            w * .029f,
            Color.rgb(74, 74, 80),
            1.22f,
            false,
            3
        )

        paint.color = brandRed
        canvas.drawRoundRect(
            pad + w * .035f,
            panelBottom - h * .09f,
            w - pad - w * .035f,
            panelBottom - h * .086f,
            3f,
            3f,
            paint
        )

        drawText(
            canvas, JalaliDate.today(), pad + w * .035f, panelBottom - h * .065f,
            w * .021f, Color.DKGRAY, false, Paint.Align.LEFT
        )
        drawText(
            canvas, "@negahjavan", w - pad - w * .035f, panelBottom - h * .065f,
            w * .021f, brandRed, true, Paint.Align.RIGHT
        )
    }

    private fun smartTitleSize(w: Int, length: Int): Float = when {
        length < 35 -> w * .074f
        length < 70 -> w * .060f
        else -> w * .050f
    }

    private fun drawFooter(
        canvas: Canvas,
        paint: Paint,
        w: Int,
        h: Int,
        pad: Float,
        textColor: Int
    ) {
        paint.color = brandRed
        canvas.drawRoundRect(
            pad, h * .935f, w - pad, h * .939f, 3f, 3f, paint
        )
        drawText(
            canvas, JalaliDate.today(), pad, h * .951f,
            w * .022f, textColor, false, Paint.Align.LEFT
        )
        drawText(
            canvas, "@negahjavan", w / 2f, h * .951f,
            w * .022f, textColor, false, Paint.Align.CENTER
        )
        drawText(
            canvas, "NEGAH JAVAN", w - pad, h * .951f,
            w * .020f, Color.WHITE, true, Paint.Align.RIGHT
        )
    }

    suspend fun save(
        context: Context,
        bitmap: Bitmap,
        format: CardFormat
    ): Uri? = withContext(Dispatchers.IO) {
        val name = "NegahJavan_" + System.currentTimeMillis() + "_" + format.name + ".png"
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

    private fun drawCenterCrop(
        canvas: Canvas,
        bitmap: Bitmap,
        dest: Rect,
        paint: Paint
    ) {
        val scale = max(
            dest.width().toFloat() / bitmap.width,
            dest.height().toFloat() / bitmap.height
        )
        val srcW = dest.width() / scale
        val srcH = dest.height() / scale
        val left = (bitmap.width - srcW) / 2f
        val top = (bitmap.height - srcH) / 2f
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
        maxLines: Int = 4
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
