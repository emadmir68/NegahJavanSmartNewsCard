package ir.negahjavan.newscard.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.text.LineBreaker
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
        val gap: Int,
        val profile: SmartCompositionEngine.Profile
    )

    fun paginate(
        analysis: SmartAnalysis,
        format: CardFormat,
        hasImage: Boolean
    ): List<SmartPage> {
        val metrics = metrics(analysis, format, hasImage)
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
                    bodyTextSizePx = metrics.profile.preferredBodySizePx
                )
            )
        }

        SmartCompositionEngine.bodyCandidates(metrics.profile).forEach { candidate ->
            val capacity = firstPageBodyHeight(
                title = title,
                hasImage = hasImage,
                metrics = metrics
            )

            if (
                capacity > 0 &&
                textHeight(
                    text = body,
                    width = bodyWidth(metrics),
                    textSize = candidate,
                    lineSpacing = metrics.profile.bodyLineSpacing
                ) <= capacity
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

        val bodySize = metrics.profile.preferredBodySizePx
            .coerceAtLeast(metrics.profile.minimumBodySizePx)

        val pages = mutableListOf<SmartPage>()
        var remaining = body

        val firstCapacity = firstPageBodyHeight(
            title = title,
            hasImage = hasImage,
            metrics = metrics
        )

        val firstSplit = splitToFit(
            text = remaining,
            width = bodyWidth(metrics),
            textSize = bodySize,
            maxHeight = firstCapacity,
            lineSpacing = metrics.profile.bodyLineSpacing
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
                maxHeight = continuationHeight,
                lineSpacing = metrics.profile.bodyLineSpacing
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

        return rebalanceLastPage(
            pages = pages,
            width = bodyWidth(metrics),
            textSize = bodySize,
            continuationHeight = continuationHeight,
            lineSpacing = metrics.profile.bodyLineSpacing
        )
    }

    private fun metrics(
        analysis: SmartAnalysis,
        format: CardFormat,
        hasImage: Boolean
    ): Metrics {
        val profile = SmartCompositionEngine.profile(
            analysis = analysis,
            format = format,
            hasImage = hasImage
        )

        val w = format.width
        val h = format.height

        return Metrics(
            width = w,
            height = h,
            horizontalPad = (w * profile.horizontalPadRatio).toInt(),
            headerBottom = (h * profile.headerBottomRatio).toInt(),
            imageTop = (h * profile.imageTopRatio).toInt(),
            imageHeight = (h * profile.imageHeightRatio).toInt(),
            safeBottom = (h * profile.safeBottomRatio).toInt(),
            badgeHeight = (h * profile.badgeHeightRatio).toInt(),
            gap = max(18, (h * profile.gapRatio).toInt()),
            profile = profile
        )
    }

    private fun firstPageBodyHeight(
        title: String,
        hasImage: Boolean,
        metrics: Metrics
    ): Int {
        val titleHeight = textHeight(
            text = title,
            width = contentWidth(metrics),
            textSize = metrics.profile.titleSizePx,
            bold = true,
            lineSpacing = metrics.profile.titleLineSpacing
        )

        val contentStart = when {
            hasImage && metrics.imageHeight > 0 ->
                metrics.imageTop + metrics.imageHeight + metrics.gap

            metrics.profile.shortContentStartRatio > 0f ->
                max(
                    metrics.headerBottom + metrics.gap,
                    (metrics.height * metrics.profile.shortContentStartRatio).toInt()
                )

            else ->
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
        val placeholder = buildString {
            repeat(length.coerceAtLeast(1)) { append('ن') }
        }
        return SmartCompositionEngine.smartTitleSize(format, placeholder)
    }

    private fun splitToFit(
        text: String,
        width: Int,
        textSize: Float,
        maxHeight: Int,
        lineSpacing: Float
    ): Pair<String, String> {
        if (text.isBlank()) return "" to ""
        if (maxHeight <= 0) return "" to text.trimStart()

        if (
            textHeight(
                text = text,
                width = width,
                textSize = textSize,
                lineSpacing = lineSpacing
            ) <= maxHeight
        ) {
            return text.trim() to ""
        }

        var low = 1
        var high = text.length
        var best = 1

        while (low <= high) {
            val mid = (low + high) / 2
            val probe = text.substring(0, mid)

            if (
                textHeight(
                    text = probe,
                    width = width,
                    textSize = textSize,
                    lineSpacing = lineSpacing
                ) <= maxHeight
            ) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        val lowerBound = (best * .55f).toInt().coerceAtLeast(1)
        val candidateBoundaries = mutableListOf<Int>()

        fun addBoundary(index: Int) {
            if (index in lowerBound until best) {
                candidateBoundaries += index
            }
        }

        var searchFrom = best - 1
        repeat(8) {
            val paragraph = text.lastIndexOf("\n\n", searchFrom)
            val question = text.lastIndexOf('؟', searchFrom)
            val dot = text.lastIndexOf('.', searchFrom)
            val exclamation = text.lastIndexOf('!', searchFrom)
            val semicolon = text.lastIndexOf('؛', searchFrom)
            val comma = text.lastIndexOf('،', searchFrom)
            val space = text.lastIndexOf(' ', searchFrom)

            listOf(
                paragraph,
                question,
                dot,
                exclamation,
                semicolon,
                comma,
                space
            ).forEach(::addBoundary)

            val nearest = listOf(
                paragraph,
                question,
                dot,
                exclamation,
                semicolon,
                comma,
                space
            )
                .filter { it >= lowerBound }
                .maxOrNull()
                ?: return@repeat

            searchFrom = nearest - 1
        }

        var cut = candidateBoundaries.maxOrNull() ?: best

        val initialRest = text
            .substring(cut.coerceAtMost(text.length))
            .trimStart()

        if (initialRest.length in 1..170) {
            val desiredCut = (text.length - 220)
                .coerceAtLeast(lowerBound)

            val earlier = candidateBoundaries
                .filter { it in lowerBound..desiredCut }
                .maxOrNull()

            if (earlier != null) {
                cut = earlier
            }
        }

        if (text.startsWith("\n\n", cut)) {
            cut += 2
        } else if (
            cut < text.length &&
            text[cut] in listOf('؟', '.', '!', '؛', '،')
        ) {
            cut += 1
        }

        val safeCut = cut.coerceAtMost(text.length)
        return text.substring(0, safeCut).trimEnd() to
            text.substring(safeCut).trimStart()
    }

    private fun rebalanceLastPage(
        pages: List<SmartPage>,
        width: Int,
        textSize: Float,
        continuationHeight: Int,
        lineSpacing: Float
    ): List<SmartPage> {
        if (pages.size < 3) return pages

        val last = pages.last()
        if (last.body.length >= 170) return pages

        val previousIndex = pages.lastIndex - 1
        val previous = pages[previousIndex]

        val previousSentences = previous.body
            .split(Regex("""(?<=[.!؟؛])\s+"""))
            .filter { it.isNotBlank() }

        if (previousSentences.size < 2) return pages

        val moved = previousSentences.last()
        val shortenedPrevious = previousSentences
            .dropLast(1)
            .joinToString(" ")
            .trim()

        val enlargedLast = (moved + " " + last.body).trim()

        if (shortenedPrevious.length < 120) return pages

        val lastHeight = textHeight(
            text = enlargedLast,
            width = width,
            textSize = textSize,
            lineSpacing = lineSpacing
        )

        if (lastHeight > continuationHeight) return pages

        return pages.toMutableList().apply {
            this[previousIndex] = previous.copy(body = shortenedPrevious)
            this[lastIndex] = last.copy(body = enlargedLast)
        }
    }

    private fun textHeight(
        text: String,
        width: Int,
        textSize: Float,
        bold: Boolean = false,
        lineSpacing: Float = 1.30f
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
            .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setLineSpacing(0f, lineSpacing)
            .build()
            .height
    }
}
