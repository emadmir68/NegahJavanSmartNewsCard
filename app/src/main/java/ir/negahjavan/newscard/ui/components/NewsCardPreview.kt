package ir.negahjavan.newscard.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.negahjavan.newscard.model.CardFormat
import ir.negahjavan.newscard.model.PremiumTemplate
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.model.SmartPage
import ir.negahjavan.newscard.ui.theme.BrandRed
import ir.negahjavan.newscard.util.CardBitmapRenderer

@Composable
fun NewsCardPreview(
    analysis: SmartAnalysis,
    page: SmartPage,
    imageUri: Uri?,
    format: CardFormat,
    template: PremiumTemplate = PremiumTemplate.LUXURY_MINIMAL,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var rendered by remember(
        analysis.title,
        analysis.bodyText,
        page,
        imageUri,
        format,
        template
    ) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(
        analysis.title,
        analysis.bodyText,
        page,
        imageUri,
        format,
        template
    ) {
        rendered = null
        rendered = CardBitmapRenderer.renderPage(
            context = context,
            analysis = analysis,
            page = page,
            imageUri = imageUri,
            format = format,
            template = template
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(format.width.toFloat() / format.height)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF101014)),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = rendered
        if (bitmap == null) {
            CircularProgressIndicator(color = BrandRed)
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
