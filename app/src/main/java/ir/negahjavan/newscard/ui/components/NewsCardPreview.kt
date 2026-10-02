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
import ir.negahjavan.newscard.model.SmartPage
import ir.negahjavan.newscard.ui.theme.BrandRed

private const val BRAND_SOCIAL = "negahjavaan"
private const val BRAND_SITE = "negahjavan.ir"
private const val BRAND_TITLE = "پایگاه خبری نگاه جوان"

@Composable
fun NewsCardPreview(
    analysis: SmartAnalysis,
    page: SmartPage,
    imageUri: Uri?,
    format: CardFormat,
    modifier: Modifier = Modifier
) {
    val ratio = format.width.toFloat() / format.height

    Box(
        modifier = modifier
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0B0B0E))
    ) {
        Background(analysis.category)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            BrandLockup()
            Spacer(Modifier.height(16.dp))

            if (page.showImage && imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(
                            when (format) {
                                CardFormat.STORY -> .255f
                                CardFormat.PORTRAIT -> .245f
                                CardFormat.SQUARE -> .225f
                                CardFormat.LANDSCAPE -> .29f
                            }
                        )
                        .clip(RoundedCornerShape(18.dp))
                )
                Spacer(Modifier.height(14.dp))
            }

            Badge(page.badge.ifBlank {
                if (page.isContinuation) "ادامه خبر" else analysis.badge
            })
            Spacer(Modifier.height(12.dp))

            if (page.isFirst && page.title.isNotBlank()) {
                Text(
                    text = page.title,
                    color = Color.White,
                    fontSize = when {
                        page.title.length < 44 -> 27.sp
                        page.title.length < 78 -> 24.sp
                        page.title.length < 118 -> 21.sp
                        else -> 18.sp
                    },
                    lineHeight = when {
                        page.title.length < 44 -> 35.sp
                        page.title.length < 78 -> 31.sp
                        page.title.length < 118 -> 27.sp
                        else -> 24.sp
                    },
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                if (page.body.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                }
            }

            if (page.body.isNotBlank()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = .055f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = page.body,
                        color = Color(0xFFEDEDF1),
                        fontSize = when {
                            page.bodyTextSizePx >= 32f -> 14.sp
                            page.bodyTextSizePx >= 29f -> 13.sp
                            else -> 12.sp
                        },
                        lineHeight = when {
                            page.bodyTextSizePx >= 32f -> 22.sp
                            page.bodyTextSizePx >= 29f -> 21.sp
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
private fun BrandLockup() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                BRAND_SOCIAL,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                BRAND_SITE,
                color = Color(0xFFBFC0C6),
                fontSize = 9.sp
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(42.dp)
                    .height(2.dp)
                    .background(BrandRed, RoundedCornerShape(50))
            )
        }

        Column(
            modifier = Modifier.weight(1.25f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                BRAND_TITLE,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right
            )
            Text(
                "نگاه جوان",
                color = Color(0xFFC3C3C9),
                fontSize = 9.sp,
                textAlign = TextAlign.Right
            )
        }
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
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun Background(category: NewsCategory) {
    val colors = when (category) {
        NewsCategory.JUDICIAL ->
            listOf(Color(0xFF1D2029), Color(0xFF0D0E12), Color(0xFF30070C))
        NewsCategory.POLITICS ->
            listOf(Color(0xFF20232E), Color(0xFF0D0E12), Color(0xFF30070C))
        NewsCategory.ECONOMY ->
            listOf(Color(0xFF172822), Color(0xFF0B100E), Color(0xFF2E070B))
        NewsCategory.SPORTS ->
            listOf(Color(0xFF152B21), Color(0xFF0A110D), Color(0xFF2E070B))
        NewsCategory.CULTURE ->
            listOf(Color(0xFF2C202E), Color(0xFF100D11), Color(0xFF31070C))
        NewsCategory.TECHNOLOGY ->
            listOf(Color(0xFF192C3A), Color(0xFF0B1015), Color(0xFF2F070B))
        else ->
            listOf(Color(0xFF252832), Color(0xFF0D0E12), Color(0xFF30070C))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors))
    )
}
