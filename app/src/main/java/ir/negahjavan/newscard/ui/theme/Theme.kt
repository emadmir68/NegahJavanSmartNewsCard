package ir.negahjavan.newscard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandRed = Color(0xFFE3121C)
val BrandBlack = Color(0xFF0B0B0E)
val SoftBlack = Color(0xFF15151B)

private val Scheme = darkColorScheme(
    primary = BrandRed,
    background = BrandBlack,
    surface = SoftBlack,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun NegahJavanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
