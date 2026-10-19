package ir.negahjavan.newscard.util

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.negahjavan.newscard.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class VideoPostExportTest {
    private val news = SmartAnalysis(
        "گزارش تصویری نگاه جوان",
        "خلاصه خبر برای بررسی خروجی ویدیویی",
        NewsCategory.CULTURE,
        "فرهنگ",
        NewsLength.SHORT,
        CardTemplate.SIGNATURE,
        fullText = "متن کامل خبر"
    )

    @Test
    fun exportsFullAndFastPresetsWithExactDimensionsAndAudio() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        data class Case(
            val format: VideoPostFormat,
            val template: InstagramTemplate,
            val preset: VideoExportPreset,
            val withAudio: Boolean,
            val layout: InstagramPostLayout = InstagramPostLayout()
        )

        val cases = listOf(
            Case(
                VideoPostFormat.POST,
                InstagramTemplate.PHOTO_GOLD,
                VideoExportPreset.FULL_HD,
                true
            ),
            Case(
                VideoPostFormat.REEL,
                InstagramTemplate.EDITORIAL_IVORY,
                VideoExportPreset.FULL_HD,
                false
            ),
            Case(
                VideoPostFormat.POST,
                InstagramTemplate.BREAKING_RED,
                VideoExportPreset.FAST,
                true,
                InstagramPostLayout(PostContentPosition.TOP, PostBrandPosition.LEFT)
            ),
            Case(
                VideoPostFormat.REEL,
                InstagramTemplate.MIDNIGHT_BLUE,
                VideoExportPreset.FAST,
                false,
                InstagramPostLayout(PostContentPosition.CENTER, PostBrandPosition.RIGHT)
            )
        )

        for (case in cases) {
            val asset = if (case.withAudio) "video-with-audio.mp4" else "video-silent.mp4"
            val source = File(context.cacheDir, asset)
            instrumentation.context.assets.open(asset).use { input ->
                source.outputStream().use { input.copyTo(it) }
            }

            val output = withTimeout(180_000) {
                VideoPostExporter.export(
                    context,
                    Uri.fromFile(source),
                    news,
                    case.template,
                    case.format,
                    case.layout,
                    case.preset
                ) {}
            }

            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(output.absolutePath)
                assertTrue(output.length() > 1000)
                assertTrue(
                    metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
                        .toLong() >= 1800
                )
                assertEquals(
                    case.withAudio,
                    metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
                )

                val encodedWidth = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
                val encodedHeight = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
                val rotation = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull()
                    ?.mod(360)
                    ?: 0
                val displayWidth = if (rotation == 90 || rotation == 270) encodedHeight else encodedWidth
                val displayHeight = if (rotation == 90 || rotation == 270) encodedWidth else encodedHeight
                assertEquals(case.preset.width, displayWidth)
                assertEquals(case.preset.height(case.format), displayHeight)

                val frame = requireNotNull(metadata.getFrameAtTime(500_000))
                assertTrue(frame.width > 0 && frame.height > 0)
                frame.recycle()

                val saved = VideoPostExporter.save(context, output)
                try {
                    assertEquals("video/mp4", context.contentResolver.getType(saved))
                    assertTrue(
                        context.contentResolver.openAssetFileDescriptor(saved, "r")!!
                            .use { it.length > 1000 }
                    )
                } finally {
                    context.contentResolver.delete(saved, null, null)
                }
            } finally {
                metadata.release()
                output.delete()
                source.delete()
            }
        }
    }

    @Test
    fun invalidInputFailsWithoutLeavingPartialExport() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val before = context.cacheDir.listFiles()!!
            .filter { it.name.startsWith("negahjavan-video-") }
            .map { it.name }
            .toSet()

        val failure = runCatching {
            withTimeout(30_000) {
                VideoPostExporter.export(
                    context,
                    Uri.fromFile(File(context.cacheDir, "missing.mp4")),
                    news,
                    InstagramTemplate.PHOTO_GOLD,
                    VideoPostFormat.POST
                ) {}
            }
        }

        assertTrue(failure.isFailure)
        assertEquals(
            before,
            context.cacheDir.listFiles()!!
                .filter { it.name.startsWith("negahjavan-video-") }
                .map { it.name }
                .toSet()
        )
    }
}
