package ir.negahjavan.newscard.util

import android.content.Context
import android.graphics.Bitmap
import ir.negahjavan.newscard.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PremiumRenderingTest {
    private fun news(body: String) = SmartAnalysis(
        title = "روایتی تازه از زندگی شهر",
        lead = "", category = NewsCategory.CULTURE, badge = "فرهنگ و زندگی",
        length = NewsLength.SHORT, recommendedTemplate = CardTemplate.SIGNATURE,
        bodyText = body, fullText = body, accentWord = "زندگی"
    )

    @Test fun bundledFontsRenderEveryTemplateAndFormat() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        PremiumTypography.initialize(context)
        val item = news("شهر با حضور مردم جان می‌گیرد؛ هر خیابان، قصه‌ای برای شنیدن دارد.\n\nنگاهی به لحظه‌هایی که زندگی روزمره را زیباتر می‌کنند.")
        val output = File("build/reports/premium-cards").apply { mkdirs() }
        for (format in CardFormat.entries) {
            val pages = SmartPaginationEngine.paginate(item, format, false)
            assertTrue(pages.isNotEmpty())
            for (template in PremiumTemplate.entries) {
                val bitmap = CardBitmapRenderer.renderPage(context, item, pages.first(), null, format, template)
                assertEquals(format.width, bitmap.width)
                assertEquals(format.height, bitmap.height)
                File(output, "${template.name}-${format.name}.png").outputStream().use {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
                bitmap.recycle()
            }
        }
    }

    @Test fun longPersianNewsSurvivesFontAwarePagination() {
        PremiumTypography.initialize(RuntimeEnvironment.getApplication())
        val body = (1..45).joinToString("\n\n") { "بند $it: این متن فارسی برای بررسی خوانایی، نیم‌فاصله و حفظ کامل خبر روی کارت‌های چندصفحه‌ای است." }
        val item = news(body).copy(length = NewsLength.LONG)
        for (format in CardFormat.entries) for (hasImage in listOf(false, true)) {
            val pages = SmartPaginationEngine.paginate(item, format, hasImage)
            assertTrue(pages.size > 1)
            fun compact(text: String) = text.replace(Regex("\\s+"), "")
            assertEquals(compact(body), compact(pages.joinToString("") { it.body }))
            assertTrue(pages.all { it.bodyTextSizePx >= SmartCompositionEngine.profile(item, format, hasImage).minimumBodySizePx })
        }
    }
}
