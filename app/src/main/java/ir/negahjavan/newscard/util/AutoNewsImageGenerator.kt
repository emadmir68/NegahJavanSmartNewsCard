package ir.negahjavan.newscard.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import ir.negahjavan.newscard.model.NewsCategory
import ir.negahjavan.newscard.model.SmartAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

object AutoNewsImageGenerator {

    private enum class Scene {
        ROAD, WEATHER, JUSTICE, CITY, ECONOMY, SPORT, TECH, GENERAL
    }

    suspend fun generate(
        context: Context,
        analysis: SmartAnalysis
    ): Uri = withContext(Dispatchers.Default) {
        val width = 1600
        val height = 1000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            palette(analysis.category),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        val seed = analysis.fullText.hashCode()
        drawAmbient(canvas, paint, width, height, seed)

        when (detectScene(analysis.fullText, analysis.category)) {
            Scene.ROAD -> drawRoad(canvas, paint, width, height)
            Scene.WEATHER -> drawWeather(canvas, paint, width, height)
            Scene.JUSTICE -> drawJustice(canvas, paint, width, height)
            Scene.CITY -> drawCity(canvas, paint, width, height, seed)
            Scene.ECONOMY -> drawEconomy(canvas, paint, width, height)
            Scene.SPORT -> drawSport(canvas, paint, width, height)
            Scene.TECH -> drawTech(canvas, paint, width, height, seed)
            Scene.GENERAL -> drawGeneral(canvas, paint, width, height, seed)
        }

        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                Color.argb(12, 0, 0, 0),
                Color.argb(0, 0, 0, 0),
                Color.argb(115, 0, 0, 0)
            ),
            floatArrayOf(0f, .48f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        withContext(Dispatchers.IO) {
            val file = File(
                context.cacheDir,
                "negahjavan_auto_${System.currentTimeMillis()}.png"
            )
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            Uri.fromFile(file)
        }
    }

    private fun detectScene(text: String, category: NewsCategory): Scene {
        val t = text.lowercase()
        return when {
            listOf("جاده", "محور", "راه", "ترافیک", "تصادف", "پل").any(t::contains) -> Scene.ROAD
            listOf("باران", "بارندگی", "هوا", "طوفان", "برف", "گرد و غبار", "هواشناسی").any(t::contains) -> Scene.WEATHER
            category == NewsCategory.JUDICIAL ||
                listOf("دادگاه", "دادگستری", "قضایی", "دادستان").any(t::contains) -> Scene.JUSTICE
            category == NewsCategory.ECONOMY ||
                listOf("قیمت", "بازار", "دلار", "طلا", "بورس").any(t::contains) -> Scene.ECONOMY
            category == NewsCategory.SPORTS -> Scene.SPORT
            category == NewsCategory.TECHNOLOGY -> Scene.TECH
            listOf("شهرداری", "شهر", "روستا", "شهرستان", "ساختمان").any(t::contains) -> Scene.CITY
            else -> Scene.GENERAL
        }
    }

    private fun palette(category: NewsCategory): IntArray =
        when (category) {
            NewsCategory.JUDICIAL ->
                intArrayOf(Color.rgb(28, 34, 46), Color.rgb(10, 13, 18), Color.rgb(70, 16, 22))
            NewsCategory.ECONOMY ->
                intArrayOf(Color.rgb(18, 52, 42), Color.rgb(8, 20, 18), Color.rgb(58, 14, 19))
            NewsCategory.SPORTS ->
                intArrayOf(Color.rgb(19, 62, 43), Color.rgb(8, 22, 16), Color.rgb(62, 14, 20))
            NewsCategory.TECHNOLOGY ->
                intArrayOf(Color.rgb(18, 47, 69), Color.rgb(8, 17, 27), Color.rgb(61, 14, 20))
            NewsCategory.CULTURE ->
                intArrayOf(Color.rgb(53, 32, 61), Color.rgb(18, 10, 22), Color.rgb(67, 15, 21))
            else ->
                intArrayOf(Color.rgb(35, 40, 53), Color.rgb(10, 12, 17), Color.rgb(70, 14, 20))
        }

    private fun drawAmbient(canvas: Canvas, paint: Paint, w: Int, h: Int, seed: Int) {
        val sx = abs(seed % 430)
        paint.color = Color.argb(22, 255, 255, 255)
        canvas.drawCircle(w * .78f, h * .18f, 330f + sx * .10f, paint)
        paint.color = Color.argb(25, 232, 21, 35)
        canvas.drawCircle(w * .11f, h * .92f, 420f, paint)
    }

    private fun drawRoad(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val road = Path().apply {
            moveTo(w * .38f, h.toFloat())
            lineTo(w * .49f, h * .38f)
            lineTo(w * .58f, h * .38f)
            lineTo(w * .83f, h.toFloat())
            close()
        }
        paint.color = Color.argb(210, 18, 19, 23)
        canvas.drawPath(road, paint)

        paint.color = Color.argb(215, 245, 245, 247)
        val centerX = w * .535f
        repeat(5) { index ->
            val y = h * (.48f + index * .105f)
            val half = 7f + index * 4.5f
            canvas.drawRoundRect(
                centerX - half,
                y,
                centerX + half,
                y + 46f + index * 9f,
                9f,
                9f,
                paint
            )
        }

        paint.color = Color.argb(70, 255, 255, 255)
        canvas.drawRect(0f, h * .36f, w.toFloat(), h * .37f, paint)
    }

    private fun drawWeather(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        paint.color = Color.argb(170, 220, 229, 238)
        val cy = h * .40f
        canvas.drawCircle(w * .43f, cy, 150f, paint)
        canvas.drawCircle(w * .55f, cy - 55f, 190f, paint)
        canvas.drawCircle(w * .67f, cy + 15f, 135f, paint)
        canvas.drawRoundRect(w * .34f, cy, w * .75f, cy + 145f, 75f, 75f, paint)

        paint.strokeWidth = 12f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.argb(145, 185, 213, 232)
        repeat(9) { i ->
            val x = w * (.31f + i * .055f)
            val y = h * (.63f + (i % 3) * .025f)
            canvas.drawLine(x, y, x - 42f, y + 95f, paint)
        }
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawJustice(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val cx = w * .55f
        val top = h * .23f
        paint.color = Color.argb(220, 234, 235, 238)
        paint.strokeWidth = 18f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        canvas.drawLine(cx, top, cx, h * .72f, paint)
        canvas.drawLine(cx - 265f, top + 110f, cx + 265f, top + 110f, paint)
        canvas.drawLine(cx - 230f, top + 110f, cx - 315f, top + 285f, paint)
        canvas.drawLine(cx - 230f, top + 110f, cx - 145f, top + 285f, paint)
        canvas.drawLine(cx + 230f, top + 110f, cx + 145f, top + 285f, paint)
        canvas.drawLine(cx + 230f, top + 110f, cx + 315f, top + 285f, paint)

        paint.style = Paint.Style.FILL
        canvas.drawOval(RectF(cx - 340f, top + 270f, cx - 120f, top + 355f), paint)
        canvas.drawOval(RectF(cx + 120f, top + 270f, cx + 340f, top + 355f), paint)
        canvas.drawRoundRect(cx - 190f, h * .70f, cx + 190f, h * .76f, 30f, 30f, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawCity(canvas: Canvas, paint: Paint, w: Int, h: Int, seed: Int) {
        val baseline = h * .82f
        var x = 80f
        var index = 0
        while (x < w - 40f) {
            val v = abs(seed + index * 97)
            val bw = 100f + (v % 95)
            val bh = 180f + (v % 330)
            paint.color = Color.argb(190, 27 + v % 18, 31 + v % 20, 40 + v % 26)
            canvas.drawRoundRect(x, baseline - bh, x + bw, baseline, 10f, 10f, paint)

            paint.color = Color.argb(90, 238, 238, 230)
            var wy = baseline - bh + 42f
            while (wy < baseline - 25f) {
                canvas.drawRoundRect(x + 22f, wy, x + 42f, wy + 18f, 4f, 4f, paint)
                if (bw > 145f) {
                    canvas.drawRoundRect(x + 70f, wy, x + 90f, wy + 18f, 4f, 4f, paint)
                }
                wy += 58f
            }

            x += bw + 24f
            index++
        }
    }

    private fun drawEconomy(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val left = w * .24f
        val bottom = h * .76f
        paint.color = Color.argb(175, 238, 239, 241)
        repeat(5) { i ->
            val x = left + i * 190f
            val barH = 120f + i * 90f
            canvas.drawRoundRect(x, bottom - barH, x + 105f, bottom, 24f, 24f, paint)
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 19f
        paint.color = Color.argb(230, 232, 21, 35)
        val p = Path().apply {
            moveTo(left - 15f, bottom - 80f)
            cubicTo(
                left + 240f, bottom - 120f,
                left + 450f, bottom - 410f,
                left + 860f, bottom - 520f
            )
        }
        canvas.drawPath(p, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawSport(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val cx = w * .56f
        val cy = h * .48f
        val r = 280f
        paint.color = Color.argb(210, 234, 236, 239)
        canvas.drawCircle(cx, cy, r, paint)
        paint.color = Color.argb(210, 31, 34, 42)
        canvas.drawCircle(cx, cy, 72f, paint)
        repeat(6) { i ->
            val angle = i * Math.PI / 3.0
            val x = cx + kotlin.math.cos(angle).toFloat() * r * .63f
            val y = cy + kotlin.math.sin(angle).toFloat() * r * .63f
            canvas.drawCircle(x, y, 48f, paint)
        }
    }

    private fun drawTech(canvas: Canvas, paint: Paint, w: Int, h: Int, seed: Int) {
        val nodes = List(13) { i ->
            val x = 180f + abs(seed + i * 163) % (w - 360)
            val y = 150f + abs(seed * 3 + i * 211) % (h - 300)
            x to y
        }

        paint.strokeWidth = 7f
        paint.color = Color.argb(70, 212, 230, 245)
        nodes.zipWithNext().forEach { (a, b) ->
            canvas.drawLine(a.first, a.second, b.first, b.second, paint)
        }

        nodes.forEachIndexed { i, (x, y) ->
            paint.color = if (i % 4 == 0) {
                Color.argb(230, 232, 21, 35)
            } else {
                Color.argb(210, 221, 232, 242)
            }
            canvas.drawCircle(x, y, if (i % 4 == 0) 28f else 20f, paint)
        }
    }

    private fun drawGeneral(canvas: Canvas, paint: Paint, w: Int, h: Int, seed: Int) {
        val shift = abs(seed % 180)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 16f
        paint.color = Color.argb(95, 255, 255, 255)
        canvas.drawOval(
            RectF(
                w * .20f,
                h * .18f,
                w * .72f,
                h * .82f
            ),
            paint
        )
        paint.strokeWidth = 8f
        paint.color = Color.argb(130, 232, 21, 35)
        canvas.drawLine(
            w * .18f,
            h * .73f,
            w * .82f,
            h * (.27f + shift / 1400f),
            paint
        )
        paint.style = Paint.Style.FILL
    }
}
