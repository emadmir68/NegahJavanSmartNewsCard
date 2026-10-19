package ir.negahjavan.newscard.util

import android.graphics.Bitmap
import android.graphics.Color
import ir.negahjavan.newscard.model.*
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
class InstagramPostRendererTest {
    private val news = SmartAnalysis(
        title = "روایتی تازه از زندگی شهر و آینده‌ای که با همراهی مردم ساخته می‌شود",
        lead = "شهر با حضور مردم جان می‌گیرد؛ هر خیابان قصه‌ای برای شنیدن دارد.",
        category = NewsCategory.CULTURE, badge = "فرهنگ", length = NewsLength.LONG,
        recommendedTemplate = CardTemplate.SIGNATURE,
        fullText = (1..80).joinToString("\n") { "بند $it: متن کامل خبر با نیم‌فاصله و جزئیات." },
        hashtags = listOf("نگاه_جوان", "#خبر")
    )

    @Test fun captionKeepsCompleteArticleAndDoesNotDuplicateTitle() {
        val caption = InstagramPostRenderer.caption(news)
        assertTrue(caption.contains(news.fullText))
        assertTrue(caption.contains("#نگاه_جوان #خبر"))
        val withTitle = news.copy(fullText = news.title + "\n" + news.fullText)
        assertEquals(caption, InstagramPostRenderer.caption(withTitle).replace(news.title + "\n", news.title + "\n\n"))
        assertTrue(InstagramPostRenderer.caption(news.copy(fullText = "", bodyText = "متن جایگزین")).contains("متن جایگزین"))
    }

    @Test fun videoOverlaysLeaveTheFootageVisibleAndKeepBrandAndText() {
        val context = RuntimeEnvironment.getApplication()
        for (template in InstagramTemplate.entries) for (format in VideoPostFormat.entries) {
            val bitmap = VideoPostExporter.overlay(context, news, template, format)
            assertEquals(format.height, bitmap.height)
            assertEquals(0, Color.alpha(bitmap.getPixel(540, 250)))
            assertEquals(255, Color.alpha(bitmap.getPixel(740, 80)))
            assertTrue(Color.alpha(bitmap.getPixel(70, format.height - 230)) > 200)
            bitmap.recycle()
        }
    }

    @Test fun twoDistinctTemplatesRenderPhotosFallbacksAndLongPersianTitles() {
        PremiumTypography.initialize(RuntimeEnvironment.getApplication())
        val photo = Bitmap.createBitmap(800, 500, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(90, 130, 150)) }
        val output = File("build/reports/premium-cards").apply { mkdirs() }
        for (template in InstagramTemplate.entries) for (hasPhoto in listOf(false, true)) {
            val bitmap = InstagramPostRenderer.draw(news, if (hasPhoto) photo else null, template)
            assertEquals(1080, bitmap.width); assertEquals(1350, bitmap.height)
            if (template == InstagramTemplate.EDITORIAL_IVORY) {
                assertEquals(Color.rgb(250, 247, 238), bitmap.getPixel(10, 10))
            } else if (hasPhoto) {
                assertEquals(photo.getPixel(10, 10), bitmap.getPixel(10, 10))
            } else {
                assertEquals(Color.rgb(9, 39, 33), bitmap.getPixel(10, 10))
            }
            File(output, "INSTAGRAM-${template.name}-$hasPhoto.png").outputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            bitmap.recycle()
            val long = InstagramPostRenderer.draw(news.copy(title = news.title.repeat(10)), null, template)
            assertEquals(1350, long.height)
            long.recycle()
        }
        photo.recycle()
    }
}
