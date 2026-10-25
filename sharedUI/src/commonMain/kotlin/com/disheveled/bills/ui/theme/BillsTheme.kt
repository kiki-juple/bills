package com.disheveled.bills.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bills.sharedui.generated.resources.Res
import bills.sharedui.generated.resources.ibm_plex_mono_medium
import bills.sharedui.generated.resources.ibm_plex_mono_regular
import bills.sharedui.generated.resources.ibm_plex_mono_semibold
import bills.sharedui.generated.resources.ibm_plex_sans_bold
import bills.sharedui.generated.resources.ibm_plex_sans_medium
import bills.sharedui.generated.resources.ibm_plex_sans_regular
import bills.sharedui.generated.resources.ibm_plex_sans_semibold
import com.disheveled.bills.model.Category
import org.jetbrains.compose.resources.Font

/*
 * Colours and type are copied from the Nota design system's tokens
 * (https://claude.ai/artifact/ABiHjVJwKivfwxYqi6ZZNG) rather than re-picked by eye. Its
 * README maps each token to a Material role; the tokens with no Material role are in
 * [NotaColors]. Token names are in the comments so the two can be checked side by side.
 */

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5B3FA8), // stamp
    onPrimary = Color(0xFFFFFFFF), // on-stamp
    primaryContainer = Color(0xFFECE8F6), // stamp-tint
    onPrimaryContainer = Color(0xFF5B3FA8), // stamp — Nota draws the stamp colour on its tint
    background = Color(0xFFF1F1EC), // ground
    onBackground = Color(0xFF1D1F22), // ink
    surface = Color(0xFFFCFCF9), // paper
    onSurface = Color(0xFF1D1F22), // ink
    surfaceVariant = Color(0xFFE7E8E2), // paper-sunk
    onSurfaceVariant = Color(0xFF5D6066), // ink-muted
    outline = Color(0xFF8A8D90), // rule-strong
    outlineVariant = Color(0xFFC9CAC3), // rule
    error = Color(0xFFB42318), // danger
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA996E8),
    onPrimary = Color(0xFF17131F),
    primaryContainer = Color(0xFF2C2640),
    onPrimaryContainer = Color(0xFFA996E8),
    background = Color(0xFF141518),
    onBackground = Color(0xFFECEDE8),
    surface = Color(0xFF1D1F23),
    onSurface = Color(0xFFECEDE8),
    surfaceVariant = Color(0xFF26282D),
    onSurfaceVariant = Color(0xFFA3A6AB),
    outline = Color(0xFF6E7178),
    outlineVariant = Color(0xFF3A3D43),
    error = Color(0xFFF28B82),
)

/** A category's glyph colour (`cat-<name>`) and the tile behind it (`cat-<name>-bg`). */
@Immutable
data class CategoryTint(val glyph: Color, val tile: Color)

/** Nota tokens that have no Material role. */
@Immutable
class NotaColors(
    /** The AI label and highlighted figures inside AI notes — nothing else. */
    val marker: Color,
    val onMarker: Color,
    /** "Tied out" and "saved". Always next to words, never on its own. */
    val ok: Color,
    private val categories: Map<Category, CategoryTint>,
) {
    /** Only for that category's glyph, tile, chip and bar. */
    fun category(category: Category): CategoryTint = categories.getValue(category)
}

private val LightNota = NotaColors(
    marker = Color(0xFFF2DC6B),
    onMarker = Color(0xFF1D1F22),
    ok = Color(0xFF2E6B45),
    categories = mapOf(
        Category.Makan to CategoryTint(Color(0xFFA5442C), Color(0xFFF3E4DE)),
        Category.Kopi to CategoryTint(Color(0xFF8A5A1F), Color(0xFFF1E8DA)),
        Category.Belanja to CategoryTint(Color(0xFF93406E), Color(0xFFF2E2EB)),
        Category.Transport to CategoryTint(Color(0xFF2B5F94), Color(0xFFE0E8F1)),
        Category.Tagihan to CategoryTint(Color(0xFF6A6219), Color(0xFFECEAD8)),
        Category.Hiburan to CategoryTint(Color(0xFF17767A), Color(0xFFDCEEEE)),
        Category.Kesehatan to CategoryTint(Color(0xFF2F7146), Color(0xFFE0EEE3)),
        // ink-muted on paper-sunk: "other" gets no colour of its own.
        Category.Lainnya to CategoryTint(Color(0xFF5D6066), Color(0xFFE7E8E2)),
    ),
)

private val DarkNota = NotaColors(
    marker = Color(0xFFE3CD5E),
    onMarker = Color(0xFF1D1F22),
    ok = Color(0xFF7FCB97),
    categories = mapOf(
        Category.Makan to CategoryTint(Color(0xFFF0A08A), Color(0xFF3A2620)),
        Category.Kopi to CategoryTint(Color(0xFFE2B57A), Color(0xFF362B1D)),
        Category.Belanja to CategoryTint(Color(0xFFE7A3C8), Color(0xFF392330)),
        Category.Transport to CategoryTint(Color(0xFF8DB8E8), Color(0xFF1F2C3B)),
        Category.Tagihan to CategoryTint(Color(0xFFD4C873), Color(0xFF33301A)),
        Category.Hiburan to CategoryTint(Color(0xFF7CCFD1), Color(0xFF173335)),
        Category.Kesehatan to CategoryTint(Color(0xFF8FCB9F), Color(0xFF1E3324)),
        Category.Lainnya to CategoryTint(Color(0xFFA3A6AB), Color(0xFF26282D)),
    ),
)

private val LocalNotaColors = staticCompositionLocalOf { LightNota }

/** Theme values Material has no slot for. */
object BillsTheme {
    val colors: NotaColors
        @Composable @ReadOnlyComposable get() = LocalNotaColors.current
}

// Nota's corners are tight like paper: xs 2, s 4, m 8, l 12. Material's extraLarge (dialogs,
// sheets) gets l too, the largest corner Nota draws.
private val BillsShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

/*
 * IBM Plex ships as static TTFs, one file per weight, so 500 and 600 render as drawn rather
 * than faux-bolded from a regular master. Sans for UI text; Mono for every amount, date,
 * quantity and small label, which also lines figures up without tabular-nums.
 */

@Composable
private fun plexSans() = FontFamily(
    Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(Res.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(Res.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
    Font(Res.font.ibm_plex_sans_bold, FontWeight.Bold),
)

@Composable
private fun plexMono() = FontFamily(
    Font(Res.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(Res.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(Res.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

/** Nota's type styles in Material's slots; the style name is in each comment. */
private fun typography(sans: FontFamily, mono: FontFamily): Typography {
    // Nota has a single body size, so bodyLarge and bodyMedium are the same style.
    val body = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    return Typography(
        displaySmall = TextStyle( // display
            fontFamily = sans, fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp,
        ),
        headlineSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp), // headline
        titleLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp), // title
        titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp), // subtitle
        bodyLarge = body,
        bodyMedium = body, // body
        bodySmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp), // body-sm
        labelLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp), // button
        labelMedium = TextStyle( // label
            fontFamily = mono, fontWeight = FontWeight.Medium,
            fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
        ),
        labelSmall = TextStyle( // caption
            fontFamily = mono, fontWeight = FontWeight.Normal,
            fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp,
        ),
    )
}

@Immutable
private class MoneyStyles(val input: TextStyle, val large: TextStyle, val medium: TextStyle, val small: TextStyle)

private fun moneyStyles(mono: FontFamily) = MoneyStyles(
    input = TextStyle( // money-input
        fontFamily = mono, fontWeight = FontWeight.Medium,
        fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1).sp,
    ),
    large = TextStyle( // money-hero
        fontFamily = mono, fontWeight = FontWeight.Medium,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp,
    ),
    medium = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp), // money-lg
    small = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp), // money
)

// Falls back to the platform monospace outside BillsTheme rather than failing.
private val LocalMoneyStyles = staticCompositionLocalOf { moneyStyles(FontFamily.Monospace) }

/** An amount while it's being typed. */
val MoneyInput: TextStyle
    @Composable @ReadOnlyComposable get() = LocalMoneyStyles.current.input

/** The month total and a receipt's total. */
val MoneyLarge: TextStyle
    @Composable @ReadOnlyComposable get() = LocalMoneyStyles.current.large

/** A person's share. */
val MoneyMedium: TextStyle
    @Composable @ReadOnlyComposable get() = LocalMoneyStyles.current.medium

/** Amounts in rows. */
val MoneySmall: TextStyle
    @Composable @ReadOnlyComposable get() = LocalMoneyStyles.current.small

@Composable
fun BillsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val sans = plexSans()
    val mono = plexMono()
    CompositionLocalProvider(
        LocalNotaColors provides if (darkTheme) DarkNota else LightNota,
        LocalMoneyStyles provides moneyStyles(mono),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = typography(sans, mono),
            shapes = BillsShapes,
            content = content,
        )
    }
}

internal val CenteredSmall = TextAlign.Center
