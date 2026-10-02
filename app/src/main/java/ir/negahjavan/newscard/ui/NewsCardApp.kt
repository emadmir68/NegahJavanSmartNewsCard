package ir.negahjavan.newscard.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.negahjavan.newscard.model.CardFormat
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.model.SmartPage
import ir.negahjavan.newscard.ui.components.NewsCardPreview
import ir.negahjavan.newscard.ui.theme.BrandRed
import ir.negahjavan.newscard.util.CardBitmapRenderer
import ir.negahjavan.newscard.util.SmartPaginationEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsCardApp(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val analysis by vm.analysis.collectAsState()
    val loading by vm.loading.collectAsState()

    var text by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var previewIndex by remember { mutableStateOf(0) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) imageUri = uri
    }

    Scaffold(
        containerColor = Color(0xFF09090C),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("نگاه جوان", fontWeight = FontWeight.Black)
                        Text(
                            "SMART PAGINATION • 0.5",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF09090C),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IntroCard()

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    vm.clear()
                },
                label = { Text("متن کامل خبر") },
                placeholder = {
                    Text("کل خبر را وارد کن؛ متن حذف یا خلاصه نمی‌شود و اگر جا نشود خودکار به کارت بعد می‌رود.")
                },
                minLines = 8,
                maxLines = 16,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandRed,
                    focusedLabelColor = BrandRed,
                    unfocusedBorderColor = Color(0xFF34343B)
                )
            )

            if (imageUri == null) {
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF34343B))
                ) {
                    Icon(Icons.Outlined.Image, null)
                    Spacer(Modifier.width(8.dp))
                    Text("افزودن عکس خبر — اختیاری")
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Image, null)
                        Spacer(Modifier.width(6.dp))
                        Text("تغییر عکس")
                    }
                    OutlinedButton(
                        onClick = { imageUri = null },
                        modifier = Modifier.weight(.72f).height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Close, null)
                        Spacer(Modifier.width(5.dp))
                        Text("حذف عکس")
                    }
                }
            }

            Button(
                onClick = { vm.analyze(text) },
                enabled = text.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Outlined.AutoAwesome, null)
                }

                Spacer(Modifier.width(8.dp))
                Text(
                    if (loading) "در حال صفحه‌بندی کامل خبر…" else "ساخت هوشمند متن کامل",
                    fontWeight = FontWeight.Black
                )
            }

            analysis?.let { result ->
                val storyPages = remember(
                    result.title,
                    result.bodyText,
                    result.badge,
                    imageUri
                ) {
                    SmartPaginationEngine.paginate(
                        analysis = result,
                        format = CardFormat.STORY,
                        hasImage = imageUri != null
                    )
                }

                LaunchedEffect(storyPages.size, result.title) {
                    previewIndex = 0
                }

                PaginationStatus(
                    result = result,
                    storyPageCount = storyPages.size,
                    hasImage = imageUri != null
                )

                if (storyPages.size > 1) {
                    PageSelector(
                        count = storyPages.size,
                        selected = previewIndex,
                        onSelect = { previewIndex = it }
                    )
                }

                Text(
                    "پیش‌نمایش استوری",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                NewsCardPreview(
                    analysis = result,
                    page = storyPages[previewIndex.coerceIn(0, storyPages.lastIndex)],
                    imageUri = imageUri,
                    format = CardFormat.STORY,
                    modifier = Modifier.fillMaxWidth()
                )

                OutputActions(
                    result = result,
                    imageUri = imageUri,
                    context = context,
                    storyCount = storyPages.size,
                    onLaunch = { action ->
                        scope.launch { action() }
                    }
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun IntroCard() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF35090F), Color(0xFF17171D), Color(0xFF101014))
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(17.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "متن کامل؛ بدون حذف.",
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "اپ اول تلاش می‌کند همه خبر را در یک کارت جا بدهد. اگر خوانایی حفظ نشود، ادامه متن خودکار به کارت‌های بعد منتقل می‌شود.",
                color = Color(0xFFB7B7C0),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Right
            )
        }
    }
}

@Composable
private fun PaginationStatus(
    result: SmartAnalysis,
    storyPageCount: Int,
    hasImage: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF131318)
    ) {
        Column(
            Modifier.padding(14.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                StatusTag("متن کامل", Modifier.weight(1f))
                StatusTag(result.badge, Modifier.weight(1f))
                StatusTag(
                    if (storyPageCount == 1) "۱ کارت" else "$storyPageCount کارت",
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                if (storyPageCount == 1) {
                    "کل متن با خوانایی مناسب روی یک کارت جا شد." +
                        if (hasImage) " عکس در باکس بالای کارت قرار گرفت." else ""
                } else {
                    "هیچ بخشی از متن حذف نشده؛ ادامه خبر بین $storyPageCount کارت تقسیم شده است." +
                        if (hasImage) " عکس فقط روی کارت اول نمایش داده می‌شود." else ""
                },
                color = Color(0xFFB0B1B9),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatusTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .background(Color(0xFF202027), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun PageSelector(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "کارت‌های خبر",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )

        val rows = (0 until count).chunked(4)
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { index ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(index) },
                        shape = RoundedCornerShape(11.dp),
                        color = if (selected == index) BrandRed else Color(0xFF18181E)
                    ) {
                        Text(
                            text = "کارت " + (index + 1),
                            modifier = Modifier.padding(vertical = 9.dp),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OutputActions(
    result: SmartAnalysis,
    imageUri: Uri?,
    context: Context,
    storyCount: Int,
    onLaunch: (suspend () -> Unit) -> Unit
) {
    val portraitCount = remember(
        result.title,
        result.bodyText,
        result.badge,
        imageUri
    ) {
        SmartPaginationEngine.paginate(
            analysis = result,
            format = CardFormat.PORTRAIT,
            hasImage = imageUri != null
        ).size
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    onLaunch {
                        val uris = renderAndSaveSeries(
                            context = context,
                            result = result,
                            imageUri = imageUri,
                            format = CardFormat.STORY
                        )
                        shareImages(context, uris, "اشتراک استوری")
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
            ) {
                Icon(Icons.Outlined.Share, null)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (storyCount == 1) "استوری آماده" else "$storyCount استوری",
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    onLaunch {
                        val uris = renderAndSaveSeries(
                            context = context,
                            result = result,
                            imageUri = imageUri,
                            format = CardFormat.PORTRAIT
                        )
                        shareImages(context, uris, "اشتراک کاروسل")
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Share, null)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (portraitCount == 1) "پست آماده" else "$portraitCount کاروسل"
                )
            }
        }

        OutlinedButton(
            onClick = {
                onLaunch {
                    val storyUris = renderAndSaveSeries(
                        context,
                        result,
                        imageUri,
                        CardFormat.STORY
                    )
                    val postUris = renderAndSaveSeries(
                        context,
                        result,
                        imageUri,
                        CardFormat.PORTRAIT
                    )

                    Toast.makeText(
                        context,
                        (storyUris.size + postUris.size).toString() +
                            " فایل کامل در گالری ذخیره شد",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Outlined.Download, null)
            Spacer(Modifier.width(7.dp))
            Text("ذخیره همه کارت‌های متن کامل")
        }
    }
}

private suspend fun renderAndSaveSeries(
    context: Context,
    result: SmartAnalysis,
    imageUri: Uri?,
    format: CardFormat
): List<Uri> {
    val pages = SmartPaginationEngine.paginate(
        analysis = result,
        format = format,
        hasImage = imageUri != null
    )

    val uris = mutableListOf<Uri>()

    pages.forEachIndexed { index, page ->
        val bitmap = CardBitmapRenderer.renderPage(
            context = context,
            analysis = result,
            page = page,
            imageUri = imageUri,
            format = format
        )

        CardBitmapRenderer.save(
            context = context,
            bitmap = bitmap,
            format = format,
            index = index,
            total = pages.size
        )?.let { uris += it }
    }

    return uris
}

private fun shareImages(
    context: Context,
    uris: List<Uri>,
    title: String
) {
    if (uris.isEmpty()) {
        Toast.makeText(context, "خروجی ساخته نشد", Toast.LENGTH_SHORT).show()
        return
    }

    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/png"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    context.startActivity(Intent.createChooser(intent, title))
}
