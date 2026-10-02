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
import ir.negahjavan.newscard.ui.theme.BrandRed
import ir.negahjavan.newscard.util.JalaliDate

@Composable
fun NewsCardPreview(
    analysis: SmartAnalysis,
    imageUri: Uri?,
    format: CardFormat,
    template: CardTemplate,
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
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    when (template) {
                        CardTemplate.SIGNATURE -> Brush.linearGradient(
                            listOf(Color(0xFF3B3F55), Color(0xFF121217), Color(0xFF5A0810))
                        )
                        CardTemplate.BREAKING_PLUS -> Brush.linearGradient(
                            listOf(Color(0xFF4E080C), Color(0xFF151519), Color(0xFF09090B))
                        )
                        CardTemplate.EDITORIAL -> Brush.linearGradient(
                            listOf(Color(0xFF41454C), Color(0xFF191B20), Color(0xFF0F1013))
                        )
                    }
                )
            )
        }

        when (template) {
            CardTemplate.SIGNATURE -> SignatureTemplate(analysis)
            CardTemplate.BREAKING_PLUS -> BreakingTemplate(analysis)
            CardTemplate.EDITORIAL -> EditorialTemplate(analysis)
        }
    }
}

@Composable
private fun SignatureTemplate(analysis: SmartAnalysis) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color.Transparent, Color.Transparent, Color(0xE908080B)),
                startY = 150f
            )
        )
    )

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        BrandHeader()

        Column(horizontalAlignment = Alignment.End) {
            CategoryPill(analysis.category.fa)
            Spacer(Modifier.height(12.dp))
            Headline(analysis.title, Color.White)
            Spacer(Modifier.height(10.dp))
            Lead(analysis.lead, Color(0xFFE7E7EB))
        }

        Footer()
    }
}

@Composable
private fun BreakingTemplate(analysis: SmartAnalysis) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0x33000000), Color(0x55000000), Color(0xF0080809)),
                startY = 90f
            )
        )
    )

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(BrandRed).padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NEGAH JAVAN • BREAKING", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("فوری", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.weight(1f))

        Column(
            Modifier.padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text("●  خبر فوری", color = Color(0xFFFF3944), fontSize = 12.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Headline(analysis.title, Color.White, forceLarge = true)
            Spacer(Modifier.height(9.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xB817171B))
                    .padding(12.dp)
            ) {
                Lead(analysis.lead, Color(0xFFF0F0F2))
            }
        }

        Footer(Modifier.padding(18.dp))
    }
}

@Composable
private fun EditorialTemplate(analysis: SmartAnalysis) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0x22000000), Color(0x55000000), Color(0xAA000000))
            )
        )
    )

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NEGAH / JAVAN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text("پایگاه خبری نگاه جوان", color = Color.White, fontSize = 12.sp)
        }

        Spacer(Modifier.weight(1f))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xECF4F1EC))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(analysis.category.fa, color = BrandRed, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Headline(analysis.title, Color(0xFF141416))
                Spacer(Modifier.height(8.dp))
                Lead(analysis.lead, Color(0xFF4D4D52))
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(JalaliDate.today(), color = Color(0xFF66666B), fontSize = 10.sp)
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
private fun CategoryPill(text: String) {
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
private fun Headline(text: String, color: Color, forceLarge: Boolean = false) {
    val size = when {
        forceLarge && text.length < 55 -> 30.sp
        text.length < 35 -> 29.sp
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
private fun Lead(text: String, color: Color) {
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
private fun Footer(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(JalaliDate.today(), color = Color.LightGray, fontSize = 10.sp)
        Text("@negahjavan", color = Color.LightGray, fontSize = 10.sp)
        Text("NEGAH JAVAN", color = BrandRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}
