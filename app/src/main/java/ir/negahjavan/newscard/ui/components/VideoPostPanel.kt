package ir.negahjavan.newscard.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ir.negahjavan.newscard.R
import ir.negahjavan.newscard.model.*
import ir.negahjavan.newscard.util.*
import kotlinx.coroutines.*

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPostPanel(news: SmartAnalysis?, rawText: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var template by rememberSaveable { mutableStateOf(InstagramTemplate.PHOTO_GOLD) }
    var format by rememberSaveable { mutableStateOf(VideoPostFormat.POST) }
    var title by rememberSaveable(news?.title, rawText) { mutableStateOf(news?.title ?: rawText.lineSequence().firstOrNull().orEmpty().take(180)) }
    var summary by rememberSaveable(news?.lead) { mutableStateOf(news?.lead.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("") }
    var playError by remember(selected, template, format) { mutableStateOf("") }
    val draft = (news ?: SmartAnalysis(title, summary, NewsCategory.GENERAL, "خبر", NewsLength.SHORT, CardTemplate.SIGNATURE))
        .copy(title = title, lead = summary, fullText = rawText.ifBlank { news?.fullText.orEmpty().ifBlank { summary } })
    var exported by remember(selected, draft, template, format) { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            selected = uri.toString(); status = "فیلم انتخاب شد؛ قالب را انتخاب کن و «ارسال پست» را بزن."
        }
    }
    OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text(if (selected == null) "افزودن فیلم و ارسال پست" else "انتخاب فیلم دیگر")
    }
    val input = selected?.let(Uri::parse) ?: return
    DisposableEffect(busy) {
        val previous = view.keepScreenOn
        if (busy) view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("پست ویدیویی نگاه جوان", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = title, onValueChange = { title = it }, readOnly = busy,
            label = { Text("تیتر روی فیلم") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
        OutlinedTextField(value = summary, onValueChange = { summary = it }, readOnly = busy,
            label = { Text("خلاصه روی فیلم") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VideoPostFormat.entries.forEach { option -> FilterChip(selected = format == option,
                onClick = { format = option }, enabled = !busy, label = { Text(option.fa) }) }
        }
        InstagramTemplate.entries.forEach { option -> FilterChip(selected = template == option,
            onClick = { template = option }, enabled = !busy, label = { Text(option.fa) }) }
        // Release the decoder during export, freeing codecs on lower-memory phones.
        if (!busy) key(input, draft, template, format) {
            VideoPreview(input, draft, template, format) { playError = it }
        }
        if (playError.isNotBlank()) Text(playError)
        Text("فیلم با صدای اصلی و قالب خبری ذخیره می‌شود. قاب فیلم برای پرکردن پست از مرکز برش می‌خورد.",
            style = MaterialTheme.typography.bodySmall)
        if (busy) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            Text("در حال ساخت ویدیو · $progress٪ — تا پایان ساخت، برنامه را باز نگه دارید.")
            OutlinedButton(onClick = { job?.cancel() }) { Text("لغو ساخت") }
        }
        fun share(uri: Uri) {
            val caption = InstagramPostRenderer.caption(draft)
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("کپشن پست", caption))
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, caption)
                clipData = ClipData.newUri(context.contentResolver, "پست ویدیویی", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "ارسال پست"))
            status = "کپشن کپی شد؛ اینستاگرام را انتخاب کن و کپشن را جای‌گذاری کن."
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (send in listOf(false, true)) Button(
                modifier = Modifier.weight(1f), enabled = !busy && title.isNotBlank(),
                onClick = {
                    busy = true; progress = 0; status = ""
                    job = scope.launch {
                        try {
                            val uri = exported ?: run {
                                val file = VideoPostExporter.export(context, input, draft, template, format) { progress = it }
                                try { VideoPostExporter.save(context, file) } finally { file.delete() }
                            }.also { exported = it }
                            if (send) share(uri) else status = "ویدیو در پوشهٔ Movies/NegahJavan ذخیره شد."
                        } catch (cancelled: CancellationException) { status = "ساخت لغو شد."; throw cancelled }
                        catch (_: Exception) { status = "ساخت یا ارسال انجام نشد. فایل و فضای خالی گوشی را بررسی کن؛ می‌توانی فیلم MP4 دیگری انتخاب کنی." }
                        finally { busy = false; job = null }
                    }
                }) { Text(if (send) "ارسال پست" else "ذخیره ویدیو") }
        }
        TextButton(enabled = !busy, onClick = { selected = null; status = "" }) { Text("حذف فیلم") }
        if (status.isNotBlank()) Text(status)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPreview(uri: Uri, news: SmartAnalysis, template: InstagramTemplate,
                         format: VideoPostFormat, onError: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val overlay = remember { VideoPostExporter.overlay(context, news, template, format) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setVideoEffects(VideoPostExporter.effects(overlay, format))
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) { onError("این فیلم پخش نشد؛ فیلم دیگری انتخاب کن.") }
            })
            setMediaItem(MediaItem.fromUri(uri)); prepare()
        }
    }
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) player.pause() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); player.release(); overlay.recycle() }
    }
    AndroidView(factory = {
        (LayoutInflater.from(it).inflate(R.layout.video_post_preview, null) as PlayerView).apply { this.player = player }
    }, onRelease = { it.player = null }, modifier = Modifier.fillMaxWidth().aspectRatio(1080f / format.height))
}
