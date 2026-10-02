package ir.negahjavan.newscard.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import ir.negahjavan.newscard.model.NewsLength
import ir.negahjavan.newscard.model.PremiumTemplate
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
    template: PremiumTemplate = PremiumTemplate.LUXURY_MINIMAL,
    modifier: Modifier = Modifier
) {
    val ratio = format.width.toFloat() / format.height
    val shortCover =
        page.isFirst &&
            analysis.length == NewsLength.SHORT &&
            !page.showImage

    Box(
        modifier = modifier
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundBrush(analysis.category, template))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            BrandLockup(template)
            Spacer(Modifier.height(14.dp))

            if (page.showImage && imageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(imageFraction(format, analysis.length))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF101014))
                        .border(
                            width = 1.dp,
                            color = when (template) {
                                PremiumTemplate.LUXURY_MINIMAL -> Color.White.copy(alpha = .16f)
                                PremiumTemplate.PHOTO_PREMIUM -> BrandRed.copy(alpha = .70f)
                                PremiumTemplate.EDITORIAL_GLASS -> Color.White.copy(alpha = .24f)
                            },
                            shape = RoundedCornerShape(
                                when (template) {
                                    PremiumTemplate.LUXURY_MINIMAL -> 18.dp
                                    PremiumTemplate.PHOTO_PREMIUM -> 24.dp
                                    PremiumTemplate.EDITORIAL_GLASS -> 14.dp
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            Badge(
                text = page.badge.ifBlank {
                    if (page.isContinuation) "ادامه خبر" else analysis.badge
                },
                outlined = shortCover,
                template = template
            )
            Spacer(Modifier.height(11.dp))

            if (page.isFirst && page.title.isNotBlank()) {
                Text(
                    text = page.title,
                    color = Color.White,
                    fontSize = when {
                        page.title.length < 38 -> 30.sp
                        page.title.length < 68 -> 26.sp
                        page.title.length < 105 -> 22.sp
                        else -> 19.sp
                    },
                    lineHeight = when {
                        page.title.length < 38 -> 38.sp
                        page.title.length < 68 -> 33.sp
                        page.title.length < 105 -> 28.sp
                        else -> 25.sp
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
                if (shortCover) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = page.body,
                            color = Color(0xFFEDEDF1),
                            fontSize = 14.sp,
                            lineHeight = 23.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier
                                .width(3.dp)
                                .heightIn(min = 56.dp)
                                .background(
                                    BrandRed,
                                    RoundedCornerShape(50)
                                )
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when (template) {
                                    PremiumTemplate.LUXURY_MINIMAL -> Color.White.copy(alpha = .045f)
                                    PremiumTemplate.PHOTO_PREMIUM -> Color.Black.copy(alpha = .34f)
                                    PremiumTemplate.EDITORIAL_GLASS -> Color.White.copy(alpha = .10f)
                                }
                            )
                            .border(
                                1.dp,
                                when (template) {
                                    PremiumTemplate.LUXURY_MINIMAL -> Color.White.copy(alpha = .07f)
                                    PremiumTemplate.PHOTO_PREMIUM -> BrandRed.copy(alpha = .28f)
                                    PremiumTemplate.EDITORIAL_GLASS -> Color.White.copy(alpha = .20f)
                                },
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = page.body,
                            color = Color(0xFFEDEDF1),
                            fontSize = when {
                                page.bodyTextSizePx >= 40f -> 14.sp
                                page.bodyTextSizePx >= 34f -> 13.sp
                                else -> 12.sp
                            },
                            lineHeight = when {
                                page.bodyTextSizePx >= 40f -> 23.sp
                                page.bodyTextSizePx >= 34f -> 21.sp
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
}

@Composable
private fun BrandLockup(template: PremiumTemplate) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                when (template) {
                    PremiumTemplate.LUXURY_MINIMAL -> Color.Black.copy(alpha = .38f)
                    PremiumTemplate.PHOTO_PREMIUM -> Color(0xFF121317).copy(alpha = .96f)
                    PremiumTemplate.EDITORIAL_GLASS -> Color.White.copy(alpha = .09f)
                }
            )
            .border(
                1.dp,
                Color.White.copy(alpha = .08f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                BRAND_SOCIAL,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(2.dp))
            Text(
                BRAND_SITE,
                color = Color(0xFFE3E3E8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(5.dp))
            Box(
                Modifier
                    .width(42.dp)
                    .height(3.dp)
                    .background(BrandRed, RoundedCornerShape(50))
            )
        }

        Spacer(Modifier.width(10.dp))

        Row(
            modifier = Modifier.weight(1.55f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                BRAND_TITLE,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Right
            )
            Spacer(Modifier.width(9.dp))
            Box(
                Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .background(BrandRed, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun Badge(
    text: String,
    outlined: Boolean,
    template: PremiumTemplate
) {
    val shape = RoundedCornerShape(50)

    Box(
        Modifier
            .then(
                when (template) {
                    PremiumTemplate.LUXURY_MINIMAL ->
                        if (outlined) {
                            Modifier.border(1.dp, BrandRed, shape)
                        } else {
                            Modifier.background(BrandRed, shape)
                        }
                    PremiumTemplate.PHOTO_PREMIUM ->
                        Modifier.background(Color.White, shape)
                    PremiumTemplate.EDITORIAL_GLASS ->
                        Modifier
                            .background(Color.White.copy(alpha = .10f), shape)
                            .border(1.dp, BrandRed.copy(alpha = .75f), shape)
                }
            )
            .padding(horizontal = 13.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (template == PremiumTemplate.PHOTO_PREMIUM) Color(0xFF121317) else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}

private fun imageFraction(
    format: CardFormat,
    length: NewsLength
): Float =
    when (length) {
        NewsLength.SHORT -> when (format) {
            CardFormat.STORY -> .30f
            CardFormat.PORTRAIT -> .28f
            CardFormat.SQUARE -> .25f
            CardFormat.LANDSCAPE -> .31f
        }
        NewsLength.MEDIUM -> when (format) {
            CardFormat.STORY -> .245f
            CardFormat.PORTRAIT -> .23f
            CardFormat.SQUARE -> .21f
            CardFormat.LANDSCAPE -> .27f
        }
        NewsLength.LONG -> when (format) {
            CardFormat.STORY -> .205f
            CardFormat.PORTRAIT -> .19f
            CardFormat.SQUARE -> .17f
            CardFormat.LANDSCAPE -> .23f
        }
    }

private fun backgroundBrush(
    category: NewsCategory,
    template: PremiumTemplate
): Brush {
    val colors = when (template) {
        PremiumTemplate.LUXURY_MINIMAL -> when (category) {
            NewsCategory.JUDICIAL -> listOf(Color(0xFF171A21), Color(0xFF08090C), Color(0xFF21070B))
            NewsCategory.ECONOMY -> listOf(Color(0xFF14241D), Color(0xFF080B0A), Color(0xFF21070B))
            NewsCategory.SPORTS -> listOf(Color(0xFF12251B), Color(0xFF080B09), Color(0xFF21070B))
            else -> listOf(Color(0xFF191C24), Color(0xFF08090C), Color(0xFF25070C))
        }

        PremiumTemplate.PHOTO_PREMIUM -> when (category) {
            NewsCategory.TECHNOLOGY -> listOf(Color(0xFF172E41), Color(0xFF0A1018), Color(0xFF430A13))
            NewsCategory.CULTURE -> listOf(Color(0xFF33223B), Color(0xFF100C14), Color(0xFF430A13))
            else -> listOf(Color(0xFF272C39), Color(0xFF0B0F16), Color(0xFF470A13))
        }

        PremiumTemplate.EDITORIAL_GLASS -> when (category) {
            NewsCategory.ECONOMY -> listOf(Color(0xFF173129), Color(0xFF0D1512), Color(0xFF251018))
            NewsCategory.JUDICIAL -> listOf(Color(0xFF202635), Color(0xFF0D1118), Color(0xFF251018))
            else -> listOf(Color(0xFF222737), Color(0xFF0D1119), Color(0xFF291019))
        }
    }

    return Brush.linearGradient(colors)
}
