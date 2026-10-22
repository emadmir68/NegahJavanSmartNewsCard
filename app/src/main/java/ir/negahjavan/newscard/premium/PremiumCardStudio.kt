package ir.negahjavan.newscard.premium

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PremiumCardStudio(
    headline: String,
    summary: String,
    imageUri: Uri?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var ratio by remember { mutableStateOf(Ratio.STORY) }
    var template by remember { mutableStateOf(PremiumCardKit.Template.GOLD) }
    var font by remember { mutableStateOf(PremiumCardKit.HeadlineFont.VAZIRMATN) }
    var autoFit by remember { mutableStateOf(true) }

    val background by produceState<Bitmap?>(initialValue = null, imageUri) {
        value = if (imageUri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(imageUri)
                        ?.use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
        }
    }

    val cardBitmap = remember(
        headline,
        summary,
        ratio,
        template,
        font,
        autoFit,
        background
    ) {
        PremiumCardKit.render(
            ctx = context,
            w = ratio.outW,
            h = ratio.outH,
            template = template,
            font = font,
            headline = headline,
            summary = summary,
            background = background,
            autoFit = autoFit
        )
    }

    PremiumTheme {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Pm.Bg
        ) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "طراحی پریمیوم جدید",
                    fontFamily = UiFont,
                    fontSize = 16.sp,
                    color = Pm.Ivory
                )

                RatioChooser(
                    selected = ratio,
                    onSelect = { ratio = it }
                )

                TemplateRow(
                    title = "Gold",
                    subtitle = "مشکی لوکس با تأکید طلایی",
                    swatch = Pm.Bg,
                    swatchBorder = Pm.Gold,
                    selected = template == PremiumCardKit.Template.GOLD,
                    tag = "پیشنهادی",
                    onClick = { template = PremiumCardKit.Template.GOLD }
                )

                TemplateRow(
                    title = "Ivory",
                    subtitle = "روشن و سردبیری",
                    swatch = Pm.Ivory,
                    swatchBorder = Pm.Gold,
                    selected = template == PremiumCardKit.Template.IVORY,
                    onClick = { template = PremiumCardKit.Template.IVORY }
                )

                TemplateRow(
                    title = "Breaking",
                    subtitle = "خبر فوری با نوار قرمز",
                    swatch = Pm.Bg,
                    swatchBorder = androidx.compose.ui.graphics.Color(0xFFE5534B),
                    selected = template == PremiumCardKit.Template.BREAKING,
                    onClick = { template = PremiumCardKit.Template.BREAKING }
                )

                FontAndTextSection(
                    headline = headline,
                    selected = font,
                    onSelect = { font = it },
                    autoFit = autoFit,
                    onAutoFit = { autoFit = it }
                )

                Spacer(Modifier.height(2.dp))

                Image(
                    bitmap = cardBitmap.asImageBitmap(),
                    contentDescription = "پیش‌نمایش کارت پریمیوم",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio.w.toFloat() / ratio.h.toFloat()),
                    contentScale = ContentScale.Fit
                )

                PremiumButton(
                    text = "ذخیره PNG پریمیوم",
                    onClick = {
                        val ok = savePremiumPng(context, cardBitmap)
                        Toast.makeText(
                            context,
                            if (ok) "کارت پریمیوم در گالری ذخیره شد"
                            else "ذخیره کارت انجام نشد",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun savePremiumPng(
    context: Context,
    bitmap: Bitmap
): Boolean {
    val resolver = context.contentResolver
    val name = "NegahJavan-Premium-${System.currentTimeMillis()}.png"

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NegahJavan")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val uri = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    ) ?: return false

    return runCatching {
        resolver.openOutputStream(uri)?.use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        } ?: error("Output stream unavailable")

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        true
    }.getOrElse {
        resolver.delete(uri, null, null)
        false
    }
}
