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
import ir.negahjavan.newscard.model.CardTemplate
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.model.SmartSlide
import ir.negahjavan.newscard.ui.theme.BrandRed
import ir.negahjavan.newscard.util.JalaliDate

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
        if (imageUri == null) {
            TextOnlyPreview(analysis, slide, slideIndex)
            return@Box
        }

        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        when (analysis.recommendedTemplate) {
            CardTemplate.SIGNATURE -> SignaturePreview(analysis, slide, slideIndex)
            CardTemplate.BREAKING_PLUS -> BreakingPreview(analysis, slide, slideIndex)
            CardTemplate.EDITORIAL -> EditorialPreview(analysis, slide, slideIndex)
        }
    }
}

@Composable
private fun TextOnlyPreview(
    analysis: SmartAnalysis,
    slide: SmartSlide,
    slideIndex: Int
) {
    val bg = when (analysis.category.name) {
        "JUDICIAL" -> listOf(Color(0xFF222833), Color(0xFF111319), Color(0xFF350A0E))
        "ECONOMY" -> listOf(Color(0xFF1E302B), Color(0xFF101613), Color(0xFF350A0E))
        "SPORTS" -> listOf(Color(0xFF18352A), Color(0xFF0D1511), Color(0xFF350A0E))
        "TECHNOLOGY" -> listOf(Color(0xFF1E3142), Color(0xFF10151B), Color(0xFF350A0E))
        else -> listOf(Color(0xFF2A2E3A), Color(0xFF121318), Color(0xFF350A0E))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bg))
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        BrandHeader()

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                slide.badge.ifBlank { analysis.badge },
                color = BrandRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(14.dp))

            Text(
                slide.title,
                color = Color.White,
                fontSize = when {
                    slide.title.length < 45 -> 30.sp
                    slide.title.length < 80 -> 26.sp
                    else -> 22.sp
                },
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Right,
                lineHeight = when {
                    slide.title.length < 45 -> 38.sp
                    slide.title.length < 80 -> 33.sp
                    else -> 29.sp
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (slide.body.isNotBlank() && slide.body.trim() != slide.title.trim()) {
                Spacer(Modifier.height(22.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = .06f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        slide.body,
                        color = Color(0xFFE7E7EB),
                        fontSize = 14.sp,
                        lineHeight = 23.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(JalaliDate.today(), color = Color(0xFF9C9DA5), fontSize = 10.sp)
            if (analysis.slides.size > 1) {
                Text(
                    (slideIndex + 1).toString() + "/" + analysis.slides.size,
                    color = Color(0xFF9C9DA5),
                    fontSize = 10.sp
                )
            }
            Text("@negahjavan", color = BrandRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SignaturePreview(
    analysis: SmartAnalysis,
    slide: SmartSlide,
    slideIndex: Int
) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color.Transparent, Color(0x22000000), Color(0xEF07070A)),
                startY = 100f
            )
        )
    )
    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        BrandHeader()
        Column(horizontalAlignment = Alignment.End) {
            Badge(slide.badge.ifBlank { analysis.badge })
            Spacer(Modifier.height(10.dp))
            Headline(slide.title, Color.White, slideIndex == 0)
            Spacer(Modifier.height(9.dp))
            Body(slide.body, Color(0xFFE7E7EB))
        }
        Footer(slideIndex, analysis.slides.size)
    }
}

@Composable
private fun BreakingPreview(
    analysis: SmartAnalysis,
    slide: SmartSlide,
    slideIndex: Int
) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0x33000000), Color(0x66000000), Color(0xF2070708))
            )
        )
    )
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(BrandRed).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("NEGAH JAVAN • BREAKING", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("فوری", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.weight(1f))
        Column(
            Modifier.padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "●  " + slide.badge.ifBlank { "خبر فوری" },
                color = Color(0xFFFF4A54),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Headline(slide.title, Color.White, true)
            Spacer(Modifier.height(10.dp))
            if (slide.body.isNotBlank()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xC016161B))
                        .padding(12.dp)
                ) {
                    Body(slide.body, Color.White)
                }
            }
        }
        Footer(slideIndex, analysis.slides.size, Modifier.padding(18.dp))
    }
}

@Composable
private fun EditorialPreview(
    analysis: SmartAnalysis,
    slide: SmartSlide,
    slideIndex: Int
) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0x22000000), Color(0x55000000), Color(0xA0000000))
            )
        )
    )
    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("NEGAH / JAVAN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text("پایگاه خبری نگاه جوان", color = Color.White, fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xF2F5F2ED))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    slide.badge.ifBlank { analysis.badge },
                    color = BrandRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                Headline(slide.title, Color(0xFF141416), slideIndex == 0)
                if (slide.body.isNotBlank()) {
                    Spacer(Modifier.height(9.dp))
                    Body(slide.body, Color(0xFF4C4D52))
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(JalaliDate.today(), color = Color(0xFF66666B), fontSize = 10.sp)
                    if (analysis.slides.size > 1) {
                        Text(
                            (slideIndex + 1).toString() + "/" + analysis.slides.size,
                            color = Color(0xFF77777D),
                            fontSize = 10.sp
                        )
                    }
                    Text("@negahjavan", color = BrandRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("NEGAH / JAVAN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text("پایگاه خبری نگاه جوان", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Badge(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BrandRed)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Headline(text: String, color: Color, cover: Boolean) {
    val size = when {
        cover && text.length < 45 -> 29.sp
        text.length < 70 -> 24.sp
        else -> 20.sp
    }
    Text(
        text,
        color = color,
        fontSize = size,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Right,
        lineHeight = when {
            size.value >= 29f -> 35.sp
            size.value >= 24f -> 30.sp
            else -> 26.sp
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Body(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 13.sp,
        textAlign = TextAlign.Right,
        lineHeight = 20.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Footer(
    slideIndex: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(JalaliDate.today(), color = Color.LightGray, fontSize = 10.sp)
        if (total > 1) {
            Text((slideIndex + 1).toString() + "/" + total, color = Color.LightGray, fontSize = 10.sp)
        }
        Text("@negahjavan", color = Color.LightGray, fontSize = 10.sp)
        Text("NEGAH JAVAN", color = BrandRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}
