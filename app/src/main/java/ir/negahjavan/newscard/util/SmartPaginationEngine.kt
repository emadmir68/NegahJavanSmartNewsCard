package ir.negahjavan.newscard.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import ir.negahjavan.newscard.model.CardFormat
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.model.SmartPage
import kotlin.math.max

object SmartPaginationEngine {

    private data class Metrics(
        val width: Int,
        val height: Int,
        val horizontalPad: Int,
        val headerBottom: Int,
        val imageTop: Int,
        val imageHeight: Int,
        val safeBottom: Int,
        val badgeHeight: Int,
        val gap: Int
    )

    fun paginate(
        analysis: SmartAnalysis,
        format: CardFormat,
        hasImage: Boolean
    ): List<SmartPage> {
        val metrics = metrics(format)
        val title = analysis.title.trim()
        val body = analysis.bodyText.trim()

        val bodyCandidates = when (format) {
            CardFormat.STORY -> listOf(34f, 32f, 30f, 28f)
            CardFormat.PORTRAIT -> listOf(31f, 29f, 27f, 25f)
            CardFormat.SQUARE -> listOf(29f, 27f, 25f, 24f)
            CardFormat.LANDSCAPE -> listOf(30f, 28f, 26f, 24f)
        }

        if (body.isBlank()) {
            return listOf(
                SmartPage(
                    title = title,
                    body = "",
                    badge = analysis.badge,
                    showImage = hasImage,
                    isFirst = true,
                    bodyTextSizePx = bodyCandidates.first()
                )
            )
        }

        // First try: keep the whole article on one card, but never shrink below
        // the premium readability floor.
        for (candidate in bodyCandidates) {
            val capacity = firstPageBodyHeight(
                title = title,
                format = format,
                hasImage = hasImage,
                bodyTextSize = candidate,
                metrics = metrics
            )
            if (capacity > 0 && textHeight(body, bodyWidth(metrics), candidate) <= capacity) {
                return listOf(
                    SmartPage(
                        title = title,
                        body = body,
                        badge = analysis.badge,
                        showImage = hasImage,
                        isFirst = true,
                        bodyTextSizePx = candidate
                    )
                )
            }
        }

        // If the full text does not fit, paginate instead of summarising.
        val bodySize = when (format) {
            CardFormat.STORY -> 30f
            CardFormat.PORTRAIT -> 27f
            CardFormat.SQUARE -> 25f
            CardFormat.LANDSCAPE -> 26f
        }

        val pages = mutableListOf<SmartPage>()
        var remaining = body

        val firstCapacity = firstPageBodyHeight(
            title = title,
            format = format,
            hasImage = hasImage,
            bodyTextSize = bodySize,
            metrics = metrics
        ).coerceAtLeast((bodySize * 3.2f).toInt())

        val firstSplit = splitToFit(
            text = remaining,
            width = bodyWidth(metrics),
            textSize = bodySize,
            maxHeight = firstCapacity
        )

        pages += SmartPage(
            title = title,
            body = firstSplit.first,
            badge = analysis.badge,
            showImage = hasImage,
            isFirst = true,
            bodyTextSizePx = bodySize
        )
        remaining = firstSplit.second

        val continuationHeight =
            metrics.safeBottom - metrics.headerBottom - metrics.badgeHeight - metrics.gap * 2

        while (remaining.isNotBlank()) {
            val split = splitToFit(
                text = remaining,
                width = bodyWidth(metrics),
                textSize = bodySize,
                maxHeight = continuationHeight
            )

            pages += SmartPage(
                title = "",
                body = split.first,
                badge = "ادامه خبر",
                showImage = false,
                isFirst = false,
                isContinuation = true,
                bodyTextSizePx = bodySize
            )

            if (split.second == remaining) {
                // Defensive escape; should never happen.
                pages[pages.lastIndex] = pages.last().copy(body = remaining)
                remaining = ""
            } else {
                remaining = split.second
            }
        }

        return pages
    }

    private fun metrics(format: CardFormat): Metrics {
        val w = format.width
        val h = format.height

        return Metrics(
            width = w,
            height = h,
            horizontalPad = (w * .060f).toInt(),
            headerBottom = (h * .115f).toInt(),
            imageTop = (h * .135f).toInt(),
            imageHeight = when (format) {
                CardFormat.STORY -> (h * .255f).toInt()
                CardFormat.PORTRAIT -> (h * .245f).toInt()
                CardFormat.SQUARE -> (h * .225f).toInt()
                CardFormat.LANDSCAPE -> (h * .29f).toInt()
            },
            safeBottom = (h * .945f).toInt(),
            badgeHeight = (h * .045f).toInt(),
            gap = max(18, (h * .018f).toInt())
        )
    }

    private fun firstPageBodyHeight(
        title: String,
        format: CardFormat,
        hasImage: Boolean,
        bodyTextSize: Float,
        metrics: Metrics
    ): Int {
        val titleSize = titleSize(format, title.length)
        val titleHeight = textHeight(
            title,
            metrics.width - metrics.horizontalPad * 2,
            titleSize,
            bold = true
        )

        val contentStart = if (hasImage) {
            metrics.imageTop + metrics.imageHeight + metrics.gap
        } else {
            metrics.headerBottom + metrics.gap
        }

        val bodyTop =
            contentStart +
                metrics.badgeHeight +
                metrics.gap +
                titleHeight +
                metrics.gap

        return metrics.safeBottom - bodyTop
    }

    private fun bodyWidth(metrics: Metrics): Int =
        metrics.width - metrics.horizontalPad * 2 - (metrics.width * .050f).toInt()

    private fun titleSize(format: CardFormat, length: Int): Float {
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

    private fun splitToFit(
        text: String,
        width: Int,
        textSize: Float,
        maxHeight: Int
    ): Pair<String, String> {
        if (text.isBlank()) return "" to ""
        if (textHeight(text, width, textSize) <= maxHeight) {
            return text.trim() to ""
        }

        var low = 1
        var high = text.length
        var best = 1

        while (low <= high) {
            val mid = (low + high) / 2
            val probe = text.substring(0, mid)
            if (textHeight(probe, width, textSize) <= maxHeight) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        val minSafe = (best * .62f).toInt().coerceAtLeast(1)
        var cut = best.coerceAtMost(text.length)

        val preferred = listOf(
            text.lastIndexOf('\n', cut - 1),
            text.lastIndexOf('؟', cut - 1),
            text.lastIndexOf('.', cut - 1),
            text.lastIndexOf('؛', cut - 1),
            text.lastIndexOf('!', cut - 1),
            text.lastIndexOf(' ', cut - 1)
        ).filter { it >= minSafe }.maxOrNull()

        if (preferred != null && preferred > 0) {
            cut = preferred + 1
        }

        val first = text.substring(0, cut).trimEnd()
        val rest = text.substring(cut).trimStart()

        return first to rest
    }

    private fun textHeight(
        text: String,
        width: Int,
        textSize: Float,
        bold: Boolean = false
    ): Int {
        if (text.isBlank()) return 0

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = if (bold) {
                Typeface.create("sans", Typeface.BOLD)
            } else {
                Typeface.create("sans", Typeface.NORMAL)
            }
        }

        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, if (bold) 1.08f else 1.28f)
            .build()
            .height
    }
}
