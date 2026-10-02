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
            .background(backgroundBrush(template))
            .border(
                1.dp,
                when (template) {
                    PremiumTemplate.LUXURY_MINIMAL -> Color(0xFFD6B168).copy(alpha = .55f)
                    PremiumTemplate.PHOTO_PREMIUM -> Color(0xFF745BE0).copy(alpha = .62f)
                    PremiumTemplate.EDITORIAL_GLASS -> Color.White.copy(alpha = .16f)
                },
                RoundedCornerShape(24.dp)
            )
    ) {
        TemplateChrome(template)

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

        FooterBrand(
            template = template,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 13.dp, start = 18.dp, end = 18.dp)
        )
    }
}

@Composable
private fun BrandLockup(template: PremiumTemplate) {
    when (template) {
        PremiumTemplate.LUXURY_MINIMAL -> {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        BRAND_SOCIAL,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        BRAND_SITE,
                        color = Color(0xFFBFC1C8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier.weight(1.45f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        BRAND_TITLE,
                        color = Color(0xFFD6B168),
                        fontSize = 16.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Right
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .width(70.dp)
                            .height(2.dp)
                            .background(Color(0xFFD6B168), RoundedCornerShape(50))
                    )
                }
            }
        }

        PremiumTemplate.PHOTO_PREMIUM -> {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .border(
                        1.dp,
                        Color(0xFF745BE0).copy(alpha = .48f),
                        RoundedCornerShape(13.dp)
                    )
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "NEGAH JAVAAN • NEWSROOM",
                        color = Color(0xFFBDB0F7),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        BRAND_SOCIAL,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(
                    modifier = Modifier.weight(1.25f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        BRAND_TITLE,
                        color = Color(0xFFF7F2E9),
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        BRAND_SITE,
                        color = Color(0xFFC5BCEB),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        PremiumTemplate.EDITORIAL_GLASS -> {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(5.dp)
                        .height(48.dp)
                        .background(BrandRed, RoundedCornerShape(50))
                )
                Spacer(Modifier.width(9.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        BRAND_SOCIAL,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        BRAND_SITE,
                        color = Color(0xFFC8C8CE),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    BRAND_TITLE,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1.35f)
                )
            }
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

@Composable
private fun BoxScope.TemplateChrome(template: PremiumTemplate) {
    when (template) {
        PremiumTemplate.LUXURY_MINIMAL -> {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 14.dp)
                    .width(86.dp)
                    .height(2.dp)
                    .background(Color(0xFFD6B168).copy(alpha = .55f))
            )
        }

        PremiumTemplate.PHOTO_PREMIUM -> {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFF745BE0))
            )
            Column(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(.88f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                repeat(5) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF745BE0).copy(alpha = .08f))
                    )
                }
            }
        }

        PremiumTemplate.EDITORIAL_GLASS -> {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .width(3.dp)
                    .fillMaxHeight(.52f)
                    .background(BrandRed.copy(alpha = .78f))
            )
        }
    }
}

@Composable
private fun FooterBrand(
    template: PremiumTemplate,
    modifier: Modifier = Modifier
) {
    when (template) {
        PremiumTemplate.LUXURY_MINIMAL -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    BRAND_SITE.uppercase(),
                    color = Color(0xFFA6A8AE),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .width(82.dp)
                        .height(2.dp)
                        .background(Color(0xFFD6B168))
                )
            }
        }

        PremiumTemplate.PHOTO_PREMIUM -> {
            Text(
                BRAND_SITE.uppercase(),
                color = Color(0xFFBDB3E6),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                modifier = modifier
            )
        }

        PremiumTemplate.EDITORIAL_GLASS -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    BRAND_SOCIAL,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    BRAND_SITE,
                    color = Color(0xFFC7C7CD),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun backgroundBrush(
    template: PremiumTemplate
): Brush {
    val colors = when (template) {
        PremiumTemplate.LUXURY_MINIMAL ->
            listOf(
                Color(0xFF09121D),
                Color(0xFF07101A),
                Color(0xFF0B0E13)
            )

        PremiumTemplate.PHOTO_PREMIUM ->
            listOf(
                Color(0xFF161334),
                Color(0xFF0B0C19),
                Color(0xFF0A0D14)
            )

        PremiumTemplate.EDITORIAL_GLASS ->
            listOf(
                Color(0xFF121317),
                Color(0xFF08090C),
                Color(0xFF15080B)
            )
    }

    return Brush.linearGradient(colors)
}
