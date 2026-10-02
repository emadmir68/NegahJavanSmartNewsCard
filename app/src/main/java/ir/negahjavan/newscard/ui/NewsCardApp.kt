package ir.negahjavan.newscard.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
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
import ir.negahjavan.newscard.model.CardTemplate
import ir.negahjavan.newscard.model.NewsCategory
import ir.negahjavan.newscard.ui.components.NewsCardPreview
import ir.negahjavan.newscard.ui.theme.BrandRed
import ir.negahjavan.newscard.util.CardBitmapRenderer
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
    var format by remember { mutableStateOf(CardFormat.STORY) }
    var template by remember { mutableStateOf(CardTemplate.SIGNATURE) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
        exportedUri = null
    }

    Scaffold(
        containerColor = Color(0xFF09090C),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("نگاه جوان", fontWeight = FontWeight.Black)
                        Text(
                            "SMART NEWS STUDIO",
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
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2A080C), Color(0xFF15151A), Color(0xFF101014))
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Text("خبر بده، کارت آماده تحویل بگیر.", color = Color.White, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "تحلیل تیتر، انتخاب قالب و ساخت خروجی در یک مسیر",
                        color = Color(0xFFAAAAB2),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Right
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    exportedUri = null
                },
                label = { Text("متن کامل خبر") },
                placeholder = { Text("متن خبر را اینجا وارد کن…") },
                minLines = 6,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandRed,
                    focusedLabelColor = BrandRed,
                    unfocusedBorderColor = Color(0xFF34343B)
                )
            )

            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF34343B))
            ) {
                Icon(Icons.Outlined.Image, null)
                Spacer(Modifier.width(8.dp))
                Text(if (imageUri == null) "افزودن عکس خبر (اختیاری)" else "عکس انتخاب شد • تغییر عکس")
            }

            Button(
                onClick = { vm.analyze(text) },
                enabled = text.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
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
                    if (loading) "در حال ساخت پیشنهادها…" else "ساخت هوشمند کارت",
                    fontWeight = FontWeight.Black
                )
            }

            analysis?.let { result ->
                LaunchedEffect(result.title, result.category) {
                    template = if (result.category == NewsCategory.BREAKING) {
                        CardTemplate.BREAKING_PLUS
                    } else {
                        CardTemplate.SIGNATURE
                    }
                    exportedUri = null
                }

                LaunchedEffect(format, template, imageUri) {
                    exportedUri = null
                }

                SectionTitle("قالب کارت", "سه طراحی مستقل")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CardTemplate.entries.forEach { item ->
                        FilterChip(
                            selected = template == item,
                            onClick = { template = item },
                            label = {
                                Text(
                                    item.fa,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandRed,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF15151B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF121217)
                ) {
                    Text(
                        template.subtitle,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        color = Color(0xFFB9BAC2),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Right
                    )
                }

                SectionTitle("ابعاد خروجی", "استوری، پست و افقی")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CardFormat.entries.forEach { item ->
                        FilterChip(
                            selected = format == item,
                            onClick = { format = item },
                            label = {
                                Text(item.fa, style = MaterialTheme.typography.labelSmall)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandRed,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF15151B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                NewsCardPreview(
                    analysis = result,
                    imageUri = imageUri,
                    format = format,
                    template = template,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val bitmap = CardBitmapRenderer.render(
                                    context, result, imageUri, format, template
                                )
                                exportedUri = CardBitmapRenderer.save(context, bitmap, format)
                                Toast.makeText(
                                    context,
                                    if (exportedUri != null) "کارت با کیفیت اصلی ذخیره شد" else "ذخیره ناموفق بود",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                    ) {
                        Icon(Icons.Outlined.Download, null)
                        Spacer(Modifier.width(6.dp))
                        Text("ذخیره PNG", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val uri = exportedUri ?: CardBitmapRenderer.save(
                                    context,
                                    CardBitmapRenderer.render(
                                        context, result, imageUri, format, template
                                    ),
                                    format
                                )
                                uri?.let {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, it)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "اشتراک کارت"))
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Share, null)
                        Spacer(Modifier.width(6.dp))
                        Text("اشتراک")
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF101015)
                ) {
                    Text(
                        if (result.source == "online") {
                            "AI ONLINE • تحلیل خبر از سرویس آنلاین انجام شد."
                        } else {
                            "SMART LOCAL • اگر سرویس آنلاین در دسترس نباشد، موتور داخلی کارت را می‌سازد."
                        },
                        modifier = Modifier.padding(12.dp),
                        color = if (result.source == "online") Color(0xFF78D99B) else Color(0xFF9FA0A8),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Right
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(subtitle, color = Color(0xFF7F8089), style = MaterialTheme.typography.labelSmall)
        Text(title, color = Color.White, fontWeight = FontWeight.Black)
    }
}
