package ir.negahjavan.newscard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ir.negahjavan.newscard.R

private val PersianBody = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)
private val PersianHeadline = FontFamily(Font(R.font.lalezar))
private val BaseTypography = Typography()
private val PremiumType = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = PersianHeadline),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = PersianHeadline),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = PersianHeadline),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = PersianHeadline),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = PersianHeadline),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = PersianHeadline),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = PersianBody, fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = PersianBody),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = PersianBody),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = PersianBody),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = PersianBody),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = PersianBody),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = PersianBody),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = PersianBody),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = PersianBody)
)

val BrandRed = Color(0xFFE3121C)
val BrandBlack = Color(0xFF0B0B0E)
val SoftBlack = Color(0xFF15151B)

private val Scheme = darkColorScheme(
    primary = Color(0xFFD3B77D),
    secondary = Color(0xFF9AC7B7),
    background = BrandBlack,
    surface = SoftBlack,
    onPrimary = Color(0xFF182722),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun NegahJavanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = PremiumType, content = content)
}

