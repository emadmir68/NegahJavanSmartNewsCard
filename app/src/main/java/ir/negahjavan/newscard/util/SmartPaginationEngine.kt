package ir.negahjavan.newscard.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import ir.negahjavan.newscard.model.CardFormat
import ir.negahjavan.newscard.model.NewsLength
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
        val metrics = metrics(format, analysis.length, hasImage)
        val title = analysis.title.trim()
        val body = analysis.bodyText.trim()

        if (body.isBlank()) {
            return listOf(
                SmartPage(
                    title = title,
                    body = "",
                    badge = analysis.badge,
                    showImage = hasImage,
                    isFirst = true,
                    bodyTextSizePx = preferredBodySize(format, analysis.length)
                )
            )
        }

        val candidates = bodyCandidates(format)

        // Try one premium card first. We reduce font only to the readability floor;
        // after that we paginate instead of making tiny text.
        candidates.forEach { candidate ->
            val capacity = firstPageBodyHeight(
                title = title,
                format = format,
                hasImage = hasImage,
                bodyTextSize = candidate,
                metrics = metrics
            )

            if (
                capacity > 0 &&
                textHeight(body, bodyWidth(metrics), candidate) <= capacity
            ) {
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

        val bodySize = preferredBodySize(format, analysis.length)
        val pages = mutableListOf<SmartPage>()
        var remaining = body

        val firstCapacity = firstPageBodyHeight(
            title = title,
            format = format,
            hasImage = hasImage,
            bodyTextSize = bodySize,
            metrics = metrics
        ).coerceAtLeast((bodySize * 3.8f).toInt())

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
            metrics.safeBottom -
                metrics.headerBottom -
                metrics.badgeHeight -
                metrics.gap * 2

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
                pages[pages.lastIndex] = pages.last().copy(body = remaining)
                remaining = ""
            } else {
                remaining = split.second
            }
        }

        return pages
    }

    private fun metrics(
        format: CardFormat,
        length: NewsLength,
        hasImage: Boolean
    ): Metrics {
        val w = format.width
        val h = format.height

        val imageRatio = if (!hasImage) {
            0f
        } else {
            when (length) {
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
        }

        return Metrics(
            width = w,
            height = h,
            horizontalPad = (w * .060f).toInt(),
            headerBottom = (h * .118f).toInt(),
            imageTop = (h * .137f).toInt(),
            imageHeight = (h * imageRatio).toInt(),
            safeBottom = (h * .948f).toInt(),
            badgeHeight = (h * .043f).toInt(),
            gap = max(18, (h * .017f).toInt())
        )
    }

    private fun bodyCandidates(format: CardFormat): List<Float> =
        when (format) {
            CardFormat.STORY -> listOf(42f, 40f, 38f, 36f)
            CardFormat.PORTRAIT -> listOf(38f, 36f, 34f, 32f)
            CardFormat.SQUARE -> listOf(34f, 32f, 30f, 28f)
            CardFormat.LANDSCAPE -> listOf(36f, 34f, 32f, 30f)
        }

    private fun preferredBodySize(
        format: CardFormat,
        length: NewsLength
    ): Float =
        when (format) {
            CardFormat.STORY -> when (length) {
                NewsLength.SHORT -> 42f
                NewsLength.MEDIUM -> 40f
                NewsLength.LONG -> 38f
            }
            CardFormat.PORTRAIT -> when (length) {
                NewsLength.SHORT -> 38f
                NewsLength.MEDIUM -> 36f
                NewsLength.LONG -> 34f
            }
            CardFormat.SQUARE -> when (length) {
                NewsLength.SHORT -> 34f
                NewsLength.MEDIUM -> 32f
                NewsLength.LONG -> 30f
            }
            CardFormat.LANDSCAPE -> when (length) {
                NewsLength.SHORT -> 36f
                NewsLength.MEDIUM -> 34f
                NewsLength.LONG -> 32f
            }
        }

    private fun firstPageBodyHeight(
        title: String,
        format: CardFormat,
        hasImage: Boolean,
        bodyTextSize: Float,
        metrics: Metrics
    ): Int {
        val titleHeight = textHeight(
            text = title,
            width = contentWidth(metrics),
            textSize = titleSize(format, title.length),
            bold = true
        )

        val contentStart = if (hasImage && metrics.imageHeight > 0) {
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

    private fun contentWidth(metrics: Metrics): Int =
        metrics.width - metrics.horizontalPad * 2

    private fun bodyWidth(metrics: Metrics): Int =
        contentWidth(metrics) - (metrics.width * .054f).toInt()

    fun titleSize(
        format: CardFormat,
        length: Int
    ): Float {
        val w = format.width.toFloat()
        val base = when {
            length < 38 -> w * .076f
            length < 68 -> w * .066f
            length < 105 -> w * .056f
            else -> w * .047f
        }

        return when (format) {
            CardFormat.LANDSCAPE -> base * .82f
            CardFormat.SQUARE -> base * .91f
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

        val minimumUsefulCut = (best * .60f).toInt().coerceAtLeast(1)
        var cut = best.coerceAtMost(text.length)

        // Prefer paragraph and sentence boundaries. This keeps cards feeling
        // editorial instead of mechanically cut in the middle of a thought.
        val boundaryCandidates = listOf(
            text.lastIndexOf("\n\n", cut - 1),
            text.lastIndexOf('؟', cut - 1),
            text.lastIndexOf('.', cut - 1),
            text.lastIndexOf('!', cut - 1),
            text.lastIndexOf('؛', cut - 1),
            text.lastIndexOf('،', cut - 1),
            text.lastIndexOf(' ', cut - 1)
        ).filter { it >= minimumUsefulCut }

        boundaryCandidates.maxOrNull()?.let {
            cut = if (text.startsWith("\n\n", it)) it + 2 else it + 1
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
            .setLineSpacing(0f, if (bold) 1.07f else 1.30f)
            .build()
            .height
    }
}
