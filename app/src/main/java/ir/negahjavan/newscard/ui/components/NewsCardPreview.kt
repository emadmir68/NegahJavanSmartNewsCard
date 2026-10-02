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

    Box(
        modifier = modifier
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0B0B0E))
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x26000000),
                            Color(0xD9000000),
                            Color(0xF508080A)
                        ),
                        startY = 120f
                    )
                )
            )
        } else {
            TextOnlyBackground(analysis.category)
        }

        NewsContent(
            analysis = analysis,
            slide = slide,
            hasImage = imageUri != null
        )
    }
}

@Composable
private fun TextOnlyBackground(category: NewsCategory) {
    val colors = when (category) {
        NewsCategory.JUDICIAL -> listOf(Color(0xFF242936), Color(0xFF101217), Color(0xFF2C080C))
        NewsCategory.ECONOMY -> listOf(Color(0xFF20352F), Color(0xFF0F1714), Color(0xFF2C080C))
        NewsCategory.SPORTS -> listOf(Color(0xFF17372A), Color(0xFF0C1611), Color(0xFF2C080C))
        NewsCategory.TECHNOLOGY -> listOf(Color(0xFF1D3549), Color(0xFF0E161D), Color(0xFF2C080C))
        else -> listOf(Color(0xFF303542), Color(0xFF121318), Color(0xFF2C080C))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors))
    )
}

@Composable
private fun NewsContent(
    analysis: SmartAnalysis,
    slide: SmartSlide,
    hasImage: Boolean
) {
    val bodyVisible = slide.body.isNotBlank() && slide.body.trim() != slide.title.trim()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = if (bodyVisible) Arrangement.Bottom else Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = slide.badge.ifBlank { analysis.badge },
            color = BrandRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Right
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = slide.title,
            color = Color.White,
            fontSize = when {
                slide.title.length < 45 -> 30.sp
                slide.title.length < 80 -> 26.sp
                slide.title.length < 115 -> 22.sp
                else -> 19.sp
            },
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Right,
            lineHeight = when {
                slide.title.length < 45 -> 39.sp
                slide.title.length < 80 -> 34.sp
                slide.title.length < 115 -> 29.sp
                else -> 25.sp
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (bodyVisible) {
            Spacer(Modifier.height(18.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (hasImage) Color.Black.copy(alpha = .36f)
                        else Color.White.copy(alpha = .055f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                Text(
                    text = slide.body,
                    color = Color(0xFFE9E9ED),
                    fontSize = 14.sp,
                    lineHeight = 23.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(if (bodyVisible) 18.dp else 0.dp))
    }
}
