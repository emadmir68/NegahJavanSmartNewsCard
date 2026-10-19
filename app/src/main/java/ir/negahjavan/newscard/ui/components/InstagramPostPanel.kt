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
import ir.negahjavan.newscard.util.CardBitmapRenderer
import ir.negahjavan.newscard.util.InstagramPostRenderer
import ir.negahjavan.newscard.util.InstagramTemplate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun InstagramPostPanel(news: SmartAnalysis, imageUri: Uri?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var template by rememberSaveable { mutableStateOf(InstagramTemplate.PHOTO_GOLD) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val caption = remember(news) { InstagramPostRenderer.caption(news) }
    var bitmap by remember(news, imageUri, template) { mutableStateOf<Bitmap?>(null) }
    var error by remember(news, imageUri, template) { mutableStateOf("") }
    fun copyCaption() {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("کپشن خبر", caption))
    }
    OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Text(if (expanded) "بستن پست اینستاگرام" else "پست اینستاگرام سایت · ۲ قالب")
    }
    if (!expanded) return
    LaunchedEffect(news, imageUri, template) {
        try { bitmap = InstagramPostRenderer.render(context, news, imageUri, template) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "ساخت پیش‌نمایش انجام نشد" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("پست ۴:۵ · ۱۰۸۰ × ۱۳۵۰", style = MaterialTheme.typography.titleMedium)
        InstagramTemplate.entries.forEach { option ->
            FilterChip(selected = template == option, onClick = { template = option },
                enabled = !busy, label = { Text(option.fa) })
        }
        val preview = bitmap
        if (preview != null) {
            Image(preview.asImageBitmap(), "پیش‌نمایش پست اینستاگرام",
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f))
        } else if (error.isNotBlank()) { Text(error) }
        else { CircularProgressIndicator() }
        Text("تیتر و خلاصه روی عکس؛ متن کامل در کپشن", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { copyCaption(); status = "کپشن کامل کپی شد" }) { Text("کپی کپشن کامل") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (share in listOf(false, true)) {
                Button(enabled = preview != null && !busy, modifier = Modifier.weight(1f), onClick = {
                    val ready = preview ?: return@Button
                    busy = true; status = ""
                    scope.launch {
                        try {
                            val uri = requireNotNull(CardBitmapRenderer.save(context, ready, CardFormat.PORTRAIT, 0, 1))
                            if (share) {
                                copyCaption()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_TEXT, caption)
                                    clipData = ClipData.newUri(context.contentResolver, "پست اینستاگرام", uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "اشتراک پست اینستاگرام"))
                                status = "پست ذخیره و کپشن کپی شد؛ در اینستاگرام کپشن را جای‌گذاری کنید."
                            } else status = "پست در گالری ذخیره شد"
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { status = "ذخیره یا اشتراک انجام نشد؛ دوباره تلاش کنید." }
                        finally { busy = false }
                    }
                }) { Text(if (share) "ارسال پست" else "ذخیره پست") }
            }
        }
        if (status.isNotBlank()) Text(status)
    }
}
