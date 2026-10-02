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
    val template = analysis.recommendedTemplate

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
        } else {
            SmartCoverBackground(analysis)
        }

        when (template) {
            CardTemplate.SIGNATURE -> SignaturePreview(analysis, slide, slideIndex)
            CardTemplate.BREAKING_PLUS -> BreakingPreview(analysis, slide, slideIndex)
            CardTemplate.EDITORIAL -> EditorialPreview(analysis, slide, slideIndex)
        }
    }
}

@Composable
private fun SmartCoverBackground(analysis: SmartAnalysis) {
    val colors = when (analysis.category.name) {
        "JUDICIAL" -> listOf(Color(0xFF282E3A), Color(0xFF111318), Color(0xFF531016))
        "POLITICS" -> listOf(Color(0xFF343A50), Color(0xFF12141B), Color(0xFF4C0A10))
        "ECONOMY" -> listOf(Color(0xFF243B36), Color(0xFF101715), Color(0xFF4A1014))
        "SPORTS" -> listOf(Color(0xFF1D4333), Color(0xFF0D1713), Color(0xFF4B0D13))
        "CULTURE" -> listOf(Color(0xFF453647), Color(0xFF171117), Color(0xFF5A0B13))
        "TECHNOLOGY" -> listOf(Color(0xFF253B50), Color(0xFF10161D), Color(0xFF4D0B12))
        else -> listOf(Color(0xFF3B3F55), Color(0xFF121217), Color(0xFF5A0810))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors))
    ) {
        Text(
            "N/J",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 40.dp),
            color = Color.White.copy(alpha = .045f),
            fontSize = 96.sp,
            fontWeight = FontWeight.Black
        )
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
                Spacer(Modifier.height(9.dp))
                Body(slide.body, Color(0xFF4C4D52))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(JalaliDate.today(), color = Color(0xFF66666B), fontSize = 10.sp)
                    Text((slideIndex + 1).toString() + "/" + analysis.slides.size, color = Color(0xFF77777D), fontSize = 10.sp)
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
        Column(horizontalAlignment = Alignment.End) {
            Text("پایگاه خبری", color = Color.White, fontSize = 10.sp)
            Text("نگاه جوان", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(BrandRed)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("NEGAH JAVAN", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
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
        Text((slideIndex + 1).toString() + "/" + total, color = Color.LightGray, fontSize = 10.sp)
        Text("@negahjavan", color = Color.LightGray, fontSize = 10.sp)
        Text("NEGAH JAVAN", color = BrandRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}
