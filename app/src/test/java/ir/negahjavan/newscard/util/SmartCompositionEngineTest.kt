package ir.negahjavan.newscard.util

import ir.negahjavan.newscard.model.CardFormat
import ir.negahjavan.newscard.model.CardTemplate
import ir.negahjavan.newscard.model.NewsCategory
import ir.negahjavan.newscard.model.NewsLength
import ir.negahjavan.newscard.model.PremiumTemplate
import ir.negahjavan.newscard.model.SmartAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartCompositionEngineTest {
    private fun analysis(
        category: NewsCategory = NewsCategory.GENERAL,
        length: NewsLength = NewsLength.MEDIUM,
        premium: PremiumTemplate? = null
    ) = SmartAnalysis(
        title = "تیتر آزمایشی برای کارت خبری نگاه جوان",
        lead = "",
        category = category,
        badge = category.fa,
        length = length,
        recommendedTemplate = CardTemplate.SIGNATURE,
        recommendedPremiumTemplate = premium,
        fullText = "متن آزمایشی",
        bodyText = "این یک متن آزمایشی برای بررسی موتور ترکیب‌بندی هوشمند است."
    )

    @Test
    fun explicitPremiumRecommendationWins() {
        val result = SmartCompositionEngine.recommendTemplate(
            analysis(premium = PremiumTemplate.EDITORIAL_GLASS),
            hasImage = true
        )
        assertEquals(PremiumTemplate.EDITORIAL_GLASS, result)
    }

    @Test
    fun longNewsKeepsReadableMinimum() {
        val profile = SmartCompositionEngine.profile(
            analysis(length = NewsLength.LONG).copy(bodyText = "متن ".repeat(500)),
            format = CardFormat.STORY,
            hasImage = true
        )
        assertTrue(profile.minimumBodySizePx >= 36f)
        assertTrue(profile.imageHeightRatio in .15f..36f)
    }
}
