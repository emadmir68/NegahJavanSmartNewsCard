package ir.negahjavan.newscard.studio

import ir.negahjavan.newscard.util.VideoExportPreset
import ir.negahjavan.newscard.util.VideoPostFormat
import androidx.media3.effect.Crop

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

@OptIn(UnstableApi::class)
object StudioVideo {
    /**
     * Preserve source quality while keeping file sizes practical for social publishing.
     * Media3 uses VBR by default, so we request a strong target bitrate without forcing
     * a bitrate mode that may be unsupported by a device encoder.
     */
    internal fun targetVideoBitrate(sourceBitrate: Int?, preset: VideoExportPreset): Int {
        val source = sourceBitrate?.takeIf { it > 0 }?.toLong()
        return when (preset) {
            VideoExportPreset.FAST ->
                ((source?.times(3)?.div(4)) ?: 6_000_000L)
                    .coerceIn(4_500_000L, 8_000_000L)
                    .toInt()
            VideoExportPreset.FULL_HD ->
                ((source?.times(5)?.div(4)) ?: 16_000_000L)
                    .coerceIn(12_000_000L, 24_000_000L)
                    .toInt()
        }
    }

    private fun sourceBitrate(context: Context, input: Uri): Int? {
        val metadata = MediaMetadataRetriever()
        return try {
            metadata.setDataSource(context, input)
            metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
        } catch (_: Exception) {
            null
        } finally {
            metadata.release()
        }
    }

    data class Info(val width:Int,val height:Int,val duration:Long)
    fun info(context:Context,input:Uri):Info {
        val m=MediaMetadataRetriever()
        try {
            m.setDataSource(context,input)
            val w=m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()?:0
            val h=m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()?:0
            val rotation=m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()?:0
            val duration=m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:0
            require(w>0&&h>0&&duration>0){"ویدئو قابل خواندن نیست."}
            return if(rotation%180==0)Info(w,h,duration)else Info(h,w,duration)
        }finally{m.release()}
    }
    private fun readingTime(page:Page)=maxOf(3000L,(page.title.length.toLong()+page.body.length+page.byline.length)*65L)
    fun pageAt(timeUs:Long,duration:Long,pages:List<Page>):Int {
        val total=pages.sumOf(::readingTime)
        val position=(timeUs/1000.0/duration.coerceAtLeast(1))*total
        var cumulative=0L
        pages.forEachIndexed {index,page->cumulative+=readingTime(page);if(position<cumulative)return index}
        return pages.lastIndex
    }
    fun validate(plan:Plan,info:Info) {
        if(plan.pages.size>1) require(info.duration>=plan.pages.sumOf(::readingTime)) {"زمان ویدئو برای خواندن همهٔ صفحات کافی نیست؛ متن را کوتاه کنید یا ویدئوی بلندتر انتخاب کنید."}
    }
    private class Pages(val context:Context,val plan:Plan,val duration:Long):BitmapOverlay() {
        private var current=-1
        private val retained=java.util.ArrayDeque<Bitmap>()
        override fun getBitmap(presentationTimeUs:Long):Bitmap {
            val index=pageAt(presentationTimeUs,duration,plan.pages)
            if(index!=current){retained.addLast(StudioRenderer.draw(context,plan,index));current=index;while(retained.size>2)retained.removeFirst().recycle()}
            return retained.last
        }
        override fun release(){super.release();retained.forEach {it.recycle()};retained.clear()}
    }
    fun effects(context:Context,plan:Plan,info:Info):List<Effect> {
        val p=plan.project
        val list=mutableListOf<Effect>()
        if(p.videoCrop){val r=StudioRenderer.cropRect(info.width,info.height,plan.width.toFloat(),plan.height.toFloat(),p.vx,p.vy,p.vz);list+=Crop(2*r.left/info.width-1,2*r.right/info.width-1,1-2*r.bottom/info.height,1-2*r.top/info.height)}
        list+=Presentation.createForWidthAndHeight(plan.width,plan.height,Presentation.LAYOUT_SCALE_TO_FIT)
        list+=OverlayEffect(listOf(Pages(context,plan,info.duration)))
        return list
    }

    /** Must finish successfully before a MediaStore item is made visible or shared. */
    suspend fun export(
        context: Context,
        input: Uri,
        plan:Plan,
        onProgress: (Int) -> Unit
    ): File {
        val metadata=withContext(Dispatchers.IO){info(context,input)}
        validate(plan,metadata)
        val preset=if(plan.project.fullHd)VideoExportPreset.FULL_HD else VideoExportPreset.FAST
        val requestedBitrate = withContext(Dispatchers.IO) {
            targetVideoBitrate(sourceBitrate(context, input), preset)
        }
        val output = File.createTempFile("negahjavan-video-", ".mp4", context.cacheDir)
        output.delete() // Transformer requires a new path.

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
                        val encoderFactory = DefaultEncoderFactory.Builder(context)
                            .setRequestedVideoEncoderSettings(
                                VideoEncoderSettings.Builder()
                                    .setBitrate(requestedBitrate)
                                    .build()
                            )
                            .build()
                        transformer = Transformer.Builder(context)
                            .setEncoderFactory(encoderFactory)
                            .setVideoMimeType(MimeTypes.VIDEO_H264)
                            .setAudioMimeType(MimeTypes.AUDIO_AAC)
                            .addListener(listener)
                            .build()
                        val item = EditedMediaItem.Builder(MediaItem.fromUri(input))
                            .setEffects(Effects(emptyList(), effects(context,plan,metadata))).build()
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
