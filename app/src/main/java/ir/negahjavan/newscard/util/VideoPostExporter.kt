package ir.negahjavan.newscard.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.*
import ir.negahjavan.newscard.model.SmartAnalysis
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Both preview and export use this exact overlay and crop, including Persian font shaping.
enum class VideoPostFormat(val fa: String, val height: Int) {
    POST("پست ۴:۵", 1350), REEL("ریلز ۹:۱۶", 1920)
}

enum class VideoExportPreset(val fa: String, val width: Int) {
    FAST("سریع 720p", 720),
    FULL_HD("کیفیت کامل 1080p", 1080);

    fun height(format: VideoPostFormat): Int =
        if (format == VideoPostFormat.POST) width * 5 / 4 else width * 16 / 9
}

@OptIn(UnstableApi::class)
object VideoPostExporter {
    fun overlay(
        context: Context,
        news: SmartAnalysis,
        template: InstagramTemplate,
        format: VideoPostFormat,
        layout: InstagramPostLayout = InstagramPostLayout(),
        preset: VideoExportPreset = VideoExportPreset.FULL_HD
    ): Bitmap {
        PremiumTypography.initialize(context)
        val portrait = InstagramPostRenderer.draw(news, null, template, videoOverlay = true, layout = layout)
        val full = if (format == VideoPostFormat.POST) {
            portrait
        } else {
            Bitmap.createBitmap(1080, format.height, Bitmap.Config.ARGB_8888).also { tall ->
                Canvas(tall).apply {
                    drawBitmap(portrait, Rect(0, 0, 1080, 180), Rect(0, 0, 1080, 180), null)
                    drawBitmap(portrait, Rect(0, 180, 1080, 1350), Rect(0, format.height - 1170, 1080, format.height), null)
                }
                portrait.recycle()
            }
        }

        if (preset == VideoExportPreset.FULL_HD) return full
        return Bitmap.createScaledBitmap(full, preset.width, preset.height(format), true).also {
            if (it !== full) full.recycle()
        }
    }

    fun effects(
        overlay: Bitmap,
        format: VideoPostFormat,
        preset: VideoExportPreset = VideoExportPreset.FULL_HD
    ): List<Effect> = listOf(
        Presentation.createForWidthAndHeight(
            preset.width,
            preset.height(format),
            Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
        ),
        OverlayEffect(listOf(BitmapOverlay.createStaticBitmapOverlay(overlay)))
    )

    /** Must finish successfully before a MediaStore item is made visible or shared. */
    suspend fun export(
        context: Context,
        input: Uri,
        news: SmartAnalysis,
        template: InstagramTemplate,
        format: VideoPostFormat,
        layout: InstagramPostLayout = InstagramPostLayout(),
        preset: VideoExportPreset = VideoExportPreset.FULL_HD,
        onProgress: (Int) -> Unit
    ): File {
        val output = File.createTempFile("negahjavan-video-", ".mp4", context.cacheDir)
        output.delete() // Transformer requires a new path.
        val art = overlay(context, news, template, format, layout, preset)
        try {
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val handler = Handler(Looper.getMainLooper())
                    var transformer: Transformer? = null
                    val ticker = object : Runnable {
                        override fun run() {
                            if (!continuation.isActive) return
                            val holder = ProgressHolder()
                            if (transformer?.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress)
                            handler.postDelayed(this, 400)
                        }
                    }
                    val listener = object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, result: ExportResult) {
                            handler.removeCallbacks(ticker)
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                        override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                            handler.removeCallbacks(ticker)
                            if (continuation.isActive) continuation.resumeWithException(exception)
                        }
                    }
                    try {
                        transformer = Transformer.Builder(context)
                            .setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
                            .addListener(listener).build()
                        val item = EditedMediaItem.Builder(MediaItem.fromUri(input))
                            .setEffects(Effects(emptyList(), effects(art, format, preset))).build()
                        transformer!!.start(item, output.absolutePath)
                        handler.post(ticker)
                    } catch (failure: Exception) {
                        handler.removeCallbacks(ticker)
                        transformer?.cancel()
                        if (continuation.isActive) continuation.resumeWithException(failure)
                    }
                    continuation.invokeOnCancellation {
                        // All Transformer access, including cancellation, stays on its application thread.
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            handler.removeCallbacks(ticker); transformer?.cancel(); output.delete()
                        } else handler.post { handler.removeCallbacks(ticker); transformer?.cancel(); output.delete() }
                    }
                }
            }
            withContext(Dispatchers.IO) {
                check(output.length() > 0) { "خروجی ویدیو خالی است" }
                val source = MediaMetadataRetriever()
                val result = MediaMetadataRetriever()
                try {
                    source.setDataSource(context, input); result.setDataSource(output.absolutePath)
                    val inputDuration = source.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    val outputDuration = result.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    check(outputDuration > 0 && outputDuration >= inputDuration - maxOf(500L, inputDuration / 20)) { "ویدیوی خروجی کامل نیست" }
                    check(result.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes")
                    if (source.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes") {
                        check(result.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes") { "صدای ویدیو ذخیره نشد" }
                    }
                } finally { source.release(); result.release() }
            }
            onProgress(100)
            return output
        } catch (failure: Exception) {
            output.delete()
            throw failure
        } finally {
            withContext(NonCancellable + Dispatchers.Main.immediate) { art.recycle() }
        }
    }

    suspend fun save(context: Context, file: File): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "NegahJavan_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/NegahJavan")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values))
        try {
            requireNotNull(resolver.openOutputStream(uri)).use { out -> file.inputStream().use { it.copyTo(out) } }
            ensureActive()
            check(resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null) == 1)
            uri
        } catch (failure: Exception) { resolver.delete(uri, null, null); throw failure }
    }
}
