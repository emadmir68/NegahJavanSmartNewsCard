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
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.ui.theme.BrandRed
import ir.negahjavan.newscard.util.JalaliDate

@Composable
fun NewsCardPreview(analysis: SmartAnalysis, imageUri: Uri?, format: CardFormat, modifier: Modifier = Modifier) {
    val ratio = format.width.toFloat() / format.height
    Box(
        modifier = modifier
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(22.dp))
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
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF3B3F55), Color(0xFF121217), Color(0xFF5A0810)))))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xE508080B)), startY = 180f)))

        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("پایگاه خبری", color = Color.White, fontSize = 11.sp)
                    Text("نگاه جوان", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(BrandRed).padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("NEGAH JAVAN", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(BrandRed).padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Text(analysis.category.fa, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    analysis.title,
                    color = Color.White,
                    fontSize = when { analysis.title.length < 35 -> 29.sp; analysis.title.length < 70 -> 24.sp; else -> 21.sp },
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Right,
                    lineHeight = 36.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(analysis.lead, color = Color(0xFFE6E6EA), fontSize = 14.sp, textAlign = TextAlign.Right, lineHeight = 22.sp)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(JalaliDate.today(), color = Color.LightGray, fontSize = 11.sp)
                Text("@negahjavan", color = Color.LightGray, fontSize = 11.sp)
                Text(if (analysis.source == "online") "AI ONLINE" else "SMART LOCAL", color = BrandRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
