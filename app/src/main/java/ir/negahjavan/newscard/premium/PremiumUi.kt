package ir.negahjavan.newscard.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.negahjavan.newscard.R

object Pm {
    val Bg = Color(0xFF0B0A09)
    val Surface = Color(0xFF12100D)
    val Border = Color(0xFF3A3224)
    val Hair = Color(0x47C9A86A)
    val Gold = Color(0xFFC9A86A)
    val Ivory = Color(0xFFF3EDE0)
    val Muted = Color(0xFFA59C89)
    val OnGold = Color(0xFF17130A)
    val GoldTint = Color(0xFF17130A)
}

val UiFont = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_bold, FontWeight.ExtraBold)
)

fun fontFamilyFor(f: PremiumCardKit.HeadlineFont): FontFamily = when (f) {
    PremiumCardKit.HeadlineFont.LALEZAR ->
        FontFamily(Font(R.font.lalezar))
    PremiumCardKit.HeadlineFont.VAZIRMATN ->
        FontFamily(Font(R.font.vazirmatn_bold, FontWeight.ExtraBold))
    PremiumCardKit.HeadlineFont.CAIRO,
    PremiumCardKit.HeadlineFont.KUFI,
    PremiumCardKit.HeadlineFont.AMIRI,
    PremiumCardKit.HeadlineFont.AREF_RUQAA ->
        FontFamily(Font(R.font.vazirmatn_bold, FontWeight.ExtraBold))
}

@Composable
fun PremiumTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = Pm.Bg,
                surface = Pm.Surface,
                primary = Pm.Gold,
                onPrimary = Pm.OnGold,
                onBackground = Pm.Ivory,
                onSurface = Pm.Ivory
            ),
            content = content
        )
    }
}

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .then(
                if (filled) Modifier.background(Pm.Gold)
                else Modifier.border(1.dp, Pm.Border, shape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = UiFont,
            fontSize = 15.sp,
            fontWeight = if (filled) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (filled) Pm.OnGold else Pm.Ivory
        )
    }
}

@Composable
fun ModeTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        options.forEachIndexed { i, label ->
            Column(
                Modifier
                    .width(IntrinsicSize.Max)
                    .heightIn(min = 48.dp)
                    .clickable { onSelect(i) },
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    label,
                    fontFamily = UiFont,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (i == selected) Pm.Ivory else Pm.Muted
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (i == selected) Pm.Gold else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun PremiumField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(label, fontFamily = UiFont, fontSize = 13.sp, color = Pm.Muted)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp),
            shape = RoundedCornerShape(18.dp),
            textStyle = TextStyle(
                fontFamily = UiFont,
                fontSize = 16.sp,
                lineHeight = 30.sp,
                color = Pm.Ivory
            ),
            placeholder = {
                Text(
                    "متن خبر را اینجا بچسبانید…",
                    fontFamily = UiFont,
                    color = Pm.Muted
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Pm.Surface,
                unfocusedContainerColor = Pm.Surface,
                focusedBorderColor = Pm.Gold,
                unfocusedBorderColor = Pm.Hair,
                focusedTextColor = Pm.Ivory,
                unfocusedTextColor = Pm.Ivory,
                cursorColor = Pm.Gold
            )
        )
    }
}

enum class Ratio(
    val label: String,
    val w: Int,
    val h: Int,
    val outW: Int,
    val outH: Int
) {
    STORY("استوری ۹:۱۶", 9, 16, 1080, 1920),
    POST("پست ۴:۵", 4, 5, 1080, 1350),
    SQUARE("مربع ۱:۱", 1, 1, 1080, 1080),
    LANDSCAPE("افقی ۱۶:۹", 16, 9, 1920, 1080)
}

@Composable
fun RatioChooser(
    selected: Ratio,
    onSelect: (Ratio) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Ratio.entries.forEach { r ->
            val on = r == selected
            val shape = RoundedCornerShape(14.dp)
            val k = 34f / maxOf(r.w, r.h)
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 88.dp)
                    .clip(shape)
                    .background(if (on) Pm.GoldTint else Pm.Surface)
                    .border(1.dp, if (on) Pm.Gold else Pm.Border, shape)
                    .clickable { onSelect(r) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .size((r.w * k).dp, (r.h * k).dp)
                        .border(
                            1.5.dp,
                            if (on) Pm.Gold else Pm.Muted,
                            RoundedCornerShape(3.dp)
                        )
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    r.label,
                    fontFamily = UiFont,
                    fontSize = 12.sp,
                    color = Pm.Ivory
                )
            }
        }
    }
}

@Composable
fun TemplateRow(
    title: String,
    subtitle: String,
    swatch: Color,
    swatchBorder: Color,
    selected: Boolean,
    tag: String? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(shape)
            .background(if (selected) Pm.GoldTint else Pm.Surface)
            .border(1.dp, if (selected) Pm.Gold else Pm.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(40.dp, 50.dp)
                .background(swatch, RoundedCornerShape(4.dp))
                .border(1.dp, swatchBorder, RoundedCornerShape(4.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontFamily = UiFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Pm.Ivory
            )
            Text(
                subtitle,
                fontFamily = UiFont,
                fontSize = 12.sp,
                color = Pm.Muted
            )
        }
        if (tag != null) {
            Text(
                tag,
                fontFamily = UiFont,
                fontSize = 11.sp,
                color = Pm.Gold,
                modifier = Modifier
                    .border(1.dp, Pm.Gold, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Pm.Border, shape)
    ) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(if (i == selected) Pm.Gold else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontFamily = UiFont,
                    fontSize = 14.sp,
                    fontWeight = if (i == selected) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (i == selected) Pm.OnGold else Pm.Ivory
                )
            }
        }
    }
}

@Composable
fun FontRow(
    font: PremiumCardKit.HeadlineFont,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(shape)
            .background(if (selected) Pm.GoldTint else Pm.Surface)
            .border(1.dp, if (selected) Pm.Gold else Pm.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "خبر امروز نگاه جوان",
            fontFamily = fontFamilyFor(font),
            fontSize = 21.sp,
            color = Pm.Ivory
        )
        Text(
            font.label + if (selected) " ✓" else "",
            fontFamily = UiFont,
            fontSize = 11.sp,
            color = if (selected) Pm.Gold else Pm.Muted
        )
    }
}

@Composable
fun FontAndTextSection(
    headline: String,
    selected: PremiumCardKit.HeadlineFont,
    onSelect: (PremiumCardKit.HeadlineFont) -> Unit,
    autoFit: Boolean,
    onAutoFit: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Pm.Hair, shape)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                "پیش‌نمایش زنده",
                fontFamily = UiFont,
                fontSize = 12.sp,
                color = Pm.Muted
            )
            Spacer(Modifier.height(6.dp))
            Text(
                headline,
                fontFamily = fontFamilyFor(selected),
                fontSize = 28.sp,
                lineHeight = 40.sp,
                color = Pm.Ivory
            )
        }

        PremiumCardKit.HeadlineFont.entries.forEach { f ->
            FontRow(f, f == selected) { onSelect(f) }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Pm.Surface)
                .border(1.dp, Pm.Border, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "اندازهٔ خودکار تیتر",
                    fontFamily = UiFont,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Pm.Ivory
                )
                Text(
                    "با بلندتر شدن متن، اندازه کم می‌شود؛ تیتر هیچ‌وقت از وسط کلمه بریده نمی‌شود.",
                    fontFamily = UiFont,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    color = Pm.Muted
                )
            }
            Switch(
                checked = autoFit,
                onCheckedChange = onAutoFit,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Pm.OnGold,
                    checkedTrackColor = Pm.Gold,
                    uncheckedThumbColor = Pm.Muted,
                    uncheckedTrackColor = Pm.Surface,
                    uncheckedBorderColor = Pm.Border
                )
            )
        }
    }
}
