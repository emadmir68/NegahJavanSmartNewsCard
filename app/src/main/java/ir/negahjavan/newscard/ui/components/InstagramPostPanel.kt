package ir.negahjavan.newscard.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.negahjavan.newscard.model.CardFormat
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.util.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun InstagramPostPanel(news: SmartAnalysis, imageUri: Uri?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by rememberSaveable { mutableStateOf(true) }
    var template by rememberSaveable { mutableStateOf(InstagramTemplate.PHOTO_GOLD) }
    var contentPosition by rememberSaveable { mutableStateOf(PostContentPosition.BOTTOM) }
    var brandPosition by rememberSaveable { mutableStateOf(PostBrandPosition.RIGHT) }
    var title by rememberSaveable(news.fullText, news.title) { mutableStateOf(news.title) }
    var summary by rememberSaveable(news.fullText, news.lead) { mutableStateOf(news.lead) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val draft = remember(news, title, summary) {
        news.copy(
            title = title.trim().ifBlank { news.title },
            lead = summary.trim()
        )
    }
    val layout = remember(contentPosition, brandPosition) {
        InstagramPostLayout(contentPosition, brandPosition)
    }
    val caption = remember(draft) { InstagramPostRenderer.caption(draft) }
    var bitmap by remember(draft, imageUri, template, layout) { mutableStateOf<Bitmap?>(null) }
    var error by remember(draft, imageUri, template, layout) { mutableStateOf("") }

    fun copyCaption() {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("کپشن خبر", caption))
    }

    OutlinedButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (expanded) "بستن استودیوی پست" else "استودیوی پست اینستاگرام · ۴ قالب + ادیت")
    }

    if (!expanded) return

    LaunchedEffect(draft, imageUri, template, layout) {
        error = ""
        bitmap = null
        try {
            bitmap = InstagramPostRenderer.render(context, draft, imageUri, template, layout)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "ساخت پیش‌نمایش انجام نشد"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("ویرایش مستقیم خروجی", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(220) },
            enabled = !busy,
            label = { Text("تیتر روی پست") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )
        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it.take(360) },
            enabled = !busy,
            label = { Text("خلاصه روی پست") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )

        Text("قالب", style = MaterialTheme.typography.labelLarge)
        InstagramTemplate.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    FilterChip(
                        selected = template == option,
                        onClick = { template = option },
                        enabled = !busy,
                        label = { Text(option.fa) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Text("جای متن", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            PostContentPosition.entries.forEach { option ->
                FilterChip(
                    selected = contentPosition == option,
                    onClick = { contentPosition = option },
                    enabled = !busy,
                    label = { Text(option.fa) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("جای لوگو", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PostBrandPosition.entries.forEach { option ->
                FilterChip(
                    selected = brandPosition == option,
                    onClick = { brandPosition = option },
                    enabled = !busy,
                    label = { Text(option.fa) }
                )
            }
        }

        Text("پست ۴:۵ · ۱۰۸۰ × ۱۳۵۰", style = MaterialTheme.typography.bodySmall)
        val preview = bitmap
        if (preview != null) {
            Image(
                preview.asImageBitmap(),
                "پیش‌نمایش دقیق پست اینستاگرام",
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f)
            )
            Text(
                "همین چیدمان در فایل نهایی ذخیره می‌شود.",
                style = MaterialTheme.typography.labelSmall
            )
        } else if (error.isNotBlank()) {
            Text(error)
        } else {
            CircularProgressIndicator()
        }

        OutlinedButton(
            onClick = {
                copyCaption()
                status = "کپشن کامل کپی شد"
            }
        ) {
            Text("کپی کپشن کامل")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (share in listOf(false, true)) {
                Button(
                    enabled = preview != null && !busy,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val ready = preview ?: return@Button
                        busy = true
                        status = ""
                        scope.launch {
                            try {
                                val uri = requireNotNull(
                                    CardBitmapRenderer.save(
                                        context,
                                        ready,
                                        CardFormat.PORTRAIT,
                                        0,
                                        1
                                    )
                                )
                                if (share) {
                                    copyCaption()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, caption)
                                        clipData = ClipData.newUri(
                                            context.contentResolver,
                                            "پست اینستاگرام",
                                            uri
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "اشتراک پست اینستاگرام"))
                                    status = "پست ذخیره و کپشن کپی شد؛ در اینستاگرام کپشن را جای‌گذاری کنید."
                                } else {
                                    status = "پست در گالری ذخیره شد"
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                status = "ذخیره یا اشتراک انجام نشد؛ دوباره تلاش کنید."
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) {
                    Text(if (share) "ارسال پست" else "ذخیره پست")
                }
            }
        }

        if (status.isNotBlank()) Text(status)
    }
}
