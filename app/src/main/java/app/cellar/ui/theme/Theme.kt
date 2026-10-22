package app.cellar.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.cellar.R

/**
 * A cellar by candlelight: dark stone, oak racks, amber flames, red sealing wax, chalk on the barrel
 * and labels written by hand. Only the candlelit look.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    /** Candle flame: the one thing to do. */
    val flame: Color,
    val flameSoft: Color,
    val onFlame: Color,
    /** Wine and sealing wax: opening. */
    val wine: Color,
    val wineSoft: Color,
    /** The barrel's water-clear spirit: pouring, saving. */
    val pour: Color,
    val pourSoft: Color,
    val chalk: Color,
    val label: Color,
    val labelInk: Color,
    val oak: Color,
    val oakDark: Color,
    val stone: Color,
    val stoneDark: Color,
)

val Candle = Palette(
    bg = Color(0xFF17120F),
    surface = Color(0xFF221A15),
    surface2 = Color(0xFF2B211B),
    line = Color(0xFF3A2D24),
    ink = Color(0xFFF2E6D2),
    muted = Color(0xFFBFAD93),
    faint = Color(0xFF85745F),
    flame = Color(0xFFE9A447),
    flameSoft = Color(0xFF3A2916),
    onFlame = Color(0xFF1E1408),
    wine = Color(0xFFC0444E),
    wineSoft = Color(0xFF3A1B1E),
    pour = Color(0xFF7FB3A6),
    pourSoft = Color(0xFF1C2C29),
    chalk = Color(0xFFE8E2D4),
    label = Color(0xFFEADCBE),
    labelInk = Color(0xFF3A2A1C),
    oak = Color(0xFF6E4B2E),
    oakDark = Color(0xFF3F2A19),
    stone = Color(0xFF3A3029),
    stoneDark = Color(0xFF231C17),
)

val LocalPalette = staticCompositionLocalOf { Candle }

/** Forum: the stencilled capitals on the racks. */
val Print = FontFamily(Font(R.font.forum_regular, FontWeight.Normal))

/** Lora: names and headings. */
val Serif = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Manrope: plain and clear, for everything around the racks. */
val Sans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

/** Caveat: labels written by hand, chalk on the barrel. */
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))

/** PT Mono: the ledger's figures. */
val Mono = FontFamily(Font(R.font.pt_mono, FontWeight.Normal))

object S {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(24.dp)
    val RLg = RoundedCornerShape(18.dp)
    val RMd = RoundedCornerShape(14.dp)
    val RSm = RoundedCornerShape(10.dp)
}

object SType {
    val Hero: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 38.sp, lineHeight = 44.sp, color = S.c.ink)
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 27.sp, lineHeight = 33.sp, color = S.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 25.sp, color = S.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = S.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Print, fontSize = 13.sp, letterSpacing = 0.2.em, color = S.c.flame)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = S.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = S.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp, color = S.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = S.c.faint)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val Hand: TextStyle @Composable get() = TextStyle(fontFamily = app.cellar.ui.theme.Hand, fontSize = 24.sp, lineHeight = 27.sp, color = S.c.ink)
    val Figures: TextStyle @Composable get() = TextStyle(fontFamily = Mono, fontSize = 14.sp, lineHeight = 19.sp, color = S.c.ink)
}

@Composable
fun CellarTheme(content: @Composable () -> Unit) {
    val p = Candle
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = p.flame,
                onPrimary = p.onFlame,
                primaryContainer = p.flameSoft,
                onPrimaryContainer = p.ink,
                secondary = p.wine,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface2,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = SType.Body,
                bodyMedium = SType.Body.copy(fontSize = 15.sp),
                labelLarge = SType.Button,
            ),
            content = content,
        )
    }
}
