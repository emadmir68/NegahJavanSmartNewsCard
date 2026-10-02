package ir.negahjavan.newscard.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ir.negahjavan.newscard.model.CardFormat
import ir.negahjavan.newscard.model.NewsCategory
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.model.SmartSlide
import ir.negahjavan.newscard.ui.theme.BrandRed

private const val BRAND_SOCIAL = "negahjavaan"
private const val BRAND_SITE = "negahjavan.ir"
private const val BRAND_TITLE = "پایگاه خبری نگاه جوان"

@Composable
fun NewsCardPreview(
    analysis: SmartAnalysis,
    slide: SmartSlide,
    imageUri: Uri?,
    format: CardFormat,
    slideIndex: Int,
    modifier: Modifier = Modifier
) {
    val ratio = format.width.toFloat() / format.height
    val hasImage = imageUri != null

    Box(
        modifier = modifier
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0B0B0E))
    ) {
        if (hasImage) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = .20f),
                                Color.Black.copy(alpha = .25f),
                                Color.Black.copy(alpha = .48f),
                                Color.Black.copy(alpha = .82f)
                            )
                        )
                    )
            )
        } else {
            TextOnlyBackground(analysis.category)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            BrandHeader()
            Spacer(Modifier.height(34.dp))

            Badge(slide.badge.ifBlank { analysis.badge })
            Spacer(Modifier.height(12.dp))

            Text(
                text = slide.title,
                color = Color.White,
                fontSize = when {
                    slide.title.length < 42 -> 28.sp
                    slide.title.length < 76 -> 24.sp
                    slide.title.length < 112 -> 21.sp
                    else -> 18.sp
                },
                lineHeight = when {
                    slide.title.length < 42 -> 36.sp
                    slide.title.length < 76 -> 31.sp
                    slide.title.length < 112 -> 27.sp
                    else -> 24.sp
                },
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            val bodyVisible = slide.body.isNotBlank() &&
                compactKey(slide.body) != compactKey(slide.title)

            if (bodyVisible) {
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (hasImage) Color.Black.copy(alpha = .38f)
                            else Color.White.copy(alpha = .055f)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = slide.body,
                        color = Color(0xFFEDEDF0),
                        fontSize = when {
                            slide.body.length < 170 -> 14.sp
                            slide.body.length < 300 -> 13.sp
                            else -> 12.sp
                        },
                        lineHeight = when {
                            slide.body.length < 170 -> 22.sp
                            slide.body.length < 300 -> 21.sp
                            else -> 19.sp
                        },
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            BRAND_SITE,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Left,
            maxLines = 1
        )
        Text(
            BRAND_SOCIAL,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            BRAND_TITLE,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.35f),
            textAlign = TextAlign.Right,
            maxLines = 1
        )
    }
}

@Composable
private fun Badge(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(BrandRed)
            .padding(horizontal = 13.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun TextOnlyBackground(category: NewsCategory) {
    val colors = when (category) {
        NewsCategory.JUDICIAL ->
            listOf(Color(0xFF262C3A), Color(0xFF101218), Color(0xFF31080E))
        NewsCategory.POLITICS ->
            listOf(Color(0xFF2A2F42), Color(0xFF101219), Color(0xFF31080E))
        NewsCategory.ECONOMY ->
            listOf(Color(0xFF1F352E), Color(0xFF0E1512), Color(0xFF2F080D))
        NewsCategory.SPORTS ->
            listOf(Color(0xFF18392C), Color(0xFF0B1510), Color(0xFF2F080D))
        NewsCategory.CULTURE ->
            listOf(Color(0xFF3D2C40), Color(0xFF151016), Color(0xFF33080E))
        NewsCategory.TECHNOLOGY ->
            listOf(Color(0xFF1D354B), Color(0xFF0E151D), Color(0xFF30080D))
        else ->
            listOf(Color(0xFF313645), Color(0xFF111218), Color(0xFF31080E))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors))
    )
}

private fun compactKey(value: String): String =
    value.lowercase()
        .replace(Regex("[^آ-یa-z0-9 ]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(80)
