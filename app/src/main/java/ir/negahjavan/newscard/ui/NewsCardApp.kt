package ir.negahjavan.newscard.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.negahjavan.newscard.model.CardFormat
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
    var exportedUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    Scaffold(
        containerColor = Color(0xFF0B0B0E),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("کارت‌ساز نگاه جوان", fontWeight = FontWeight.Black)
                        Text("Smart News Card", style = MaterialTheme.typography.labelSmall, color = BrandRed)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0B0B0E), titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("خبر را بده؛ طراحی را اپ انجام می‌دهد.", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("متن کامل خبر") },
                minLines = 6,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandRed, focusedLabelColor = BrandRed)
            )

            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF393941))
            ) {
                Icon(Icons.Outlined.Image, null)
                Spacer(Modifier.width(8.dp))
                Text(if (imageUri == null) "افزودن عکس خبر (اختیاری)" else "تغییر عکس")
            }

            Button(
                onClick = { vm.analyze(text) },
                enabled = text.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                else Icon(Icons.Outlined.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "در حال تحلیل هوشمند…" else "ساخت هوشمند کارت", fontWeight = FontWeight.Bold)
            }

            analysis?.let { result ->
                Text("ابعاد خروجی", color = Color.White, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CardFormat.entries.forEach { item ->
                        FilterChip(
                            selected = format == item,
                            onClick = { format = item },
                            label = { Text(item.fa, fontSize = MaterialTheme.typography.labelSmall.fontSize) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandRed, selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                NewsCardPreview(
                    analysis = result,
                    imageUri = imageUri,
                    format = format,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val bitmap = CardBitmapRenderer.render(context, result, imageUri, format)
                                exportedUri = CardBitmapRenderer.save(context, bitmap, format)
                                Toast.makeText(context, if (exportedUri != null) "کارت ذخیره شد" else "ذخیره ناموفق بود", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                    ) {
                        Icon(Icons.Outlined.Download, null)
                        Spacer(Modifier.width(6.dp))
                        Text("ذخیره PNG")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val uri = exportedUri ?: CardBitmapRenderer.save(context, CardBitmapRenderer.render(context, result, imageUri, format), format)
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Share, null)
                        Spacer(Modifier.width(6.dp))
                        Text("اشتراک")
                    }
                }
                Text(
                    if (result.source == "online") "تحلیل این کارت با سرویس هوش مصنوعی آنلاین انجام شد." else "سرویس آنلاین تنظیم نشده/در دسترس نبود؛ موتور هوشمند داخلی استفاده شد.",
                    color = Color(0xFF9FA0A8),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
