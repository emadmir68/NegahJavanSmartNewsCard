package ir.negahjavan.newscard.util

import ir.negahjavan.newscard.model.CardFormat
import ir.negahjavan.newscard.model.NewsCategory
import ir.negahjavan.newscard.model.NewsLength
import ir.negahjavan.newscard.model.PremiumTemplate
import ir.negahjavan.newscard.model.SmartAnalysis
import kotlin.math.max

object SmartCompositionEngine {

    enum class Density {
        AIRY,
        BALANCED,
        DENSE
    }

    data class Profile(
        val horizontalPadRatio: Float,
        val gapRatio: Float,
        val headerBottomRatio: Float,
        val imageTopRatio: Float,
        val imageHeightRatio: Float,
        val safeBottomRatio: Float,
        val badgeHeightRatio: Float,
        val titleSizePx: Float,
        val titleLineSpacing: Float,
        val preferredBodySizePx: Float,
        val minimumBodySizePx: Float,
        val bodyLineSpacing: Float,
        val shortContentStartRatio: Float,
        val density: Density
    )

    fun profile(
        analysis: SmartAnalysis,
        format: CardFormat,
        hasImage: Boolean
    ): Profile {
        val title = analysis.title.trim()
        val body = analysis.bodyText.trim()
        val bodyChars = body.length
        val paragraphCount = body
            .split(Regex("""\n\s*\n"""))
            .count { it.isNotBlank() }
            .coerceAtLeast(1)

        val density = when {
            bodyChars <= 250 && paragraphCount <= 2 -> Density.AIRY
            bodyChars <= 900 && paragraphCount <= 6 -> Density.BALANCED
            else -> Density.DENSE
        }

        val titleSize = smartTitleSize(format, title)
        val preferredBody = preferredBodySize(format, density, hasImage)

        val minBody = when (format) {
            CardFormat.STORY -> 36f
            CardFormat.PORTRAIT -> 31f
            CardFormat.SQUARE -> 28f
            CardFormat.LANDSCAPE -> 29f
        }

        return Profile(
            horizontalPadRatio = when (format) {
                CardFormat.LANDSCAPE -> .050f
                CardFormat.SQUARE -> .058f
                else -> .060f
            },
            gapRatio = when (density) {
                Density.AIRY -> .019f
                Density.BALANCED -> .016f
                Density.DENSE -> .014f
            },
            headerBottomRatio = when (format) {
                CardFormat.STORY -> .118f
                CardFormat.PORTRAIT -> .125f
                CardFormat.SQUARE -> .132f
                CardFormat.LANDSCAPE -> .145f
            },
            imageTopRatio = when (format) {
                CardFormat.STORY -> .137f
                CardFormat.PORTRAIT -> .150f
                CardFormat.SQUARE -> .158f
                CardFormat.LANDSCAPE -> .165f
            },
            imageHeightRatio = smartImageRatio(
                format = format,
                analysis = analysis,
                hasImage = hasImage,
                density = density
            ),
            safeBottomRatio = when (format) {
                CardFormat.STORY -> .930f
                CardFormat.PORTRAIT -> .925f
                CardFormat.SQUARE -> .920f
                CardFormat.LANDSCAPE -> .900f
            },
            badgeHeightRatio = when (format) {
                CardFormat.STORY -> .040f
                CardFormat.PORTRAIT -> .046f
                CardFormat.SQUARE -> .050f
                CardFormat.LANDSCAPE -> .060f
            },
            titleSizePx = titleSize,
            titleLineSpacing = when {
                title.length <= 45 -> 1.03f
                title.length <= 90 -> 1.06f
                else -> 1.09f
            },
            preferredBodySizePx = preferredBody,
            minimumBodySizePx = minBody,
            bodyLineSpacing = when (density) {
                Density.AIRY -> 1.36f
                Density.BALANCED -> 1.30f
                Density.DENSE -> 1.25f
            },
            shortContentStartRatio = when {
                hasImage -> 0f
                density == Density.AIRY -> .205f
                density == Density.BALANCED -> .145f
                else -> .128f
            },
            density = density
        )
    }

    fun bodyCandidates(profile: Profile): List<Float> {
        val start = profile.preferredBodySizePx
        val min = profile.minimumBodySizePx

        return listOf(
            start,
            start - 2f,
            start - 4f,
            start - 6f,
            min
        )
            .map { max(min, it) }
            .distinct()
            .sortedDescending()
    }

    fun recommendTemplate(
        analysis: SmartAnalysis,
        hasImage: Boolean
    ): PremiumTemplate {
        analysis.recommendedPremiumTemplate?.let { return it }
        return when {
            hasImage && analysis.length == NewsLength.SHORT ->
                PremiumTemplate.PHOTO_PREMIUM

            analysis.category in setOf(
                NewsCategory.JUDICIAL,
                NewsCategory.POLITICS,
                NewsCategory.ECONOMY
            ) || analysis.length == NewsLength.LONG ->
                PremiumTemplate.LUXURY_MINIMAL

            analysis.category == NewsCategory.BREAKING ||
                analysis.category == NewsCategory.SOCIAL ->
                PremiumTemplate.EDITORIAL_GLASS

            hasImage ->
                PremiumTemplate.PHOTO_PREMIUM

            else ->
                PremiumTemplate.LUXURY_MINIMAL
        }
    }

    fun smartTitleSize(
        format: CardFormat,
        title: String
    ): Float {
        val width = format.width.toFloat()
        val chars = title.length
        val words = title
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
        val longestWord = words.maxOfOrNull { it.length } ?: 0

        val base = when {
            chars <= 30 -> width * .082f
            chars <= 48 -> width * .075f
            chars <= 72 -> width * .066f
            chars <= 105 -> width * .057f
            chars <= 150 -> width * .050f
            else -> width * .044f
        }

        val wordPenalty = when {
            longestWord >= 18 -> .90f
            longestWord >= 14 -> .95f
            else -> 1f
        }

        val wordCountPenalty = when {
            words.size >= 18 -> .92f
            words.size >= 13 -> .96f
            else -> 1f
        }

        val formatScale = when (format) {
            CardFormat.LANDSCAPE -> .82f
            CardFormat.SQUARE -> .91f
            else -> 1f
        }

        return base * wordPenalty * wordCountPenalty * formatScale
    }

    private fun preferredBodySize(
        format: CardFormat,
        density: Density,
        hasImage: Boolean
    ): Float {
        val base = when (format) {
            CardFormat.STORY -> when (density) {
                Density.AIRY -> 48f
                Density.BALANCED -> 42f
                Density.DENSE -> 39f
            }
            CardFormat.PORTRAIT -> when (density) {
                Density.AIRY -> 42f
                Density.BALANCED -> 37f
                Density.DENSE -> 34f
            }
            CardFormat.SQUARE -> when (density) {
                Density.AIRY -> 38f
                Density.BALANCED -> 33f
                Density.DENSE -> 30f
            }
            CardFormat.LANDSCAPE -> when (density) {
                Density.AIRY -> 40f
                Density.BALANCED -> 35f
                Density.DENSE -> 32f
            }
        }

        return if (hasImage && density != Density.AIRY) {
            max(base - 1f, 30f)
        } else {
            base
        }
    }

    private fun smartImageRatio(
        format: CardFormat,
        analysis: SmartAnalysis,
        hasImage: Boolean,
        density: Density
    ): Float {
        if (!hasImage) return 0f

        val base = when (format) {
            CardFormat.STORY -> when (density) {
                Density.AIRY -> .34f
                Density.BALANCED -> .255f
                Density.DENSE -> .195f
            }
            CardFormat.PORTRAIT -> when (density) {
                Density.AIRY -> .30f
                Density.BALANCED -> .235f
                Density.DENSE -> .180f
            }
            CardFormat.SQUARE -> when (density) {
                Density.AIRY -> .27f
                Density.BALANCED -> .215f
                Density.DENSE -> .165f
            }
            CardFormat.LANDSCAPE -> when (density) {
                Density.AIRY -> .33f
                Density.BALANCED -> .270f
                Density.DENSE -> .220f
            }
        }

        val titlePenalty = when {
            analysis.title.length >= 130 -> .035f
            analysis.title.length >= 90 -> .020f
            else -> 0f
        }

        return (base - titlePenalty).coerceIn(.15f, .36f)
    }
}
