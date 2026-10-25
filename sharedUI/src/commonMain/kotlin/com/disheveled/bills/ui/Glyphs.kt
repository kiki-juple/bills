package com.disheveled.bills.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disheveled.bills.model.Category

/*
 * Every glyph is [NotaGlyph]: the design system's SVG paths, drawn here instead of pulling in
 * the material-icons artifact.
 */

/**
 * Glyphs from the Nota design system, as its 24-unit SVG paths, so a new one is copied from
 * the canvas rather than redrawn. Rects and circles are spelled as paths because that is all
 * [PathParser] reads.
 */
internal enum class NotaGlyph(vararg pathData: String) {
    Chart("M5 20v-8", "M12 20V5", "M19 20v-5"),
    Receipt("M6 3h12v18l-2-1.5-2 1.5-2-1.5-2 1.5-2-1.5L6 21z", "M9 8h6", "M9 12h6", "M9 16h3"),
    Camera(
        "M5 7h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z",
        "M8.5 7l1.5-3h4l1.5 3",
        "M8.5 13.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0",
    ),
    Plus("M12 5v14", "M5 12h14"),
    Gallery(
        "M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        "M7.5 9.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0",
        "M21 15.5l-5-5L6 20",
    ),
    Close("M6 6l12 12", "M18 6 6 18"),
    ChevronLeft("M15 5l-7 7 7 7"),
    ChevronRight("M9 5l7 7-7 7"),
    ChevronDown("M6 9l6 6 6-6"),
    Check("M5 12.5l4.5 4.5L19 7.5"),
    Share("M12 15V4", "M8 8l4-4 4 4", "M5 13v7h14v-7"),

    // Categories.
    Makan("M3 11h18", "M4.5 11a7.5 7.5 0 0 0 15 0", "M9 21h6", "M10 4v3", "M14 4v3"),
    Kopi(
        "M6 9h8a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z",
        "M16 12h1.5a2.5 2.5 0 0 1 0 5H16",
        "M8 3.5V6",
        "M12 3.5V6",
    ),
    Belanja(
        "M6 8h12a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2z",
        "M8.5 8V6.5a3.5 3.5 0 0 1 7 0V8",
    ),
    Transport(
        "M7 4h10a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3z",
        "M4 11h16",
        "M6.5 20a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0",
        "M14.5 20a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0",
    ),
    Tagihan("M13 2.5 5.5 13.5H11l-1 8 7.5-11H12l1-8z"),
    Hiburan(
        "M6 5h12a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z",
        "M10 9.5v5l4.5-2.5z",
    ),
    Kesehatan(
        "M8 4h8a4 4 0 0 1 4 4v8a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4V8a4 4 0 0 1 4-4z",
        "M12 8.5v7",
        "M8.5 12h7",
    ),
    Lainnya(
        "M5.5 4h3a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1-1.5 1.5h-3a1.5 1.5 0 0 1-1.5-1.5v-3a1.5 1.5 0 0 1 1.5-1.5z",
        "M15.5 4h3a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1-1.5 1.5h-3a1.5 1.5 0 0 1-1.5-1.5v-3a1.5 1.5 0 0 1 1.5-1.5z",
        "M5.5 14h3a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1-1.5 1.5h-3a1.5 1.5 0 0 1-1.5-1.5v-3a1.5 1.5 0 0 1 1.5-1.5z",
        "M15.5 14h3a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1-1.5 1.5h-3a1.5 1.5 0 0 1-1.5-1.5v-3a1.5 1.5 0 0 1 1.5-1.5z",
    );

    val paths: List<Path> = pathData.map { PathParser().parsePathString(it).toPath() }
}

internal val Category.glyph: NotaGlyph
    get() = when (this) {
        Category.Makan -> NotaGlyph.Makan
        Category.Kopi -> NotaGlyph.Kopi
        Category.Belanja -> NotaGlyph.Belanja
        Category.Transport -> NotaGlyph.Transport
        Category.Tagihan -> NotaGlyph.Tagihan
        Category.Hiburan -> NotaGlyph.Hiburan
        Category.Kesehatan -> NotaGlyph.Kesehatan
        Category.Lainnya -> NotaGlyph.Lainnya
    }

/** Nota strokes every glyph at 1.75 of its 24 units, with round ends and joins. */
private val GlyphStroke = Stroke(width = 1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Heavier stroke for glyphs drawn small inside a filled badge, like the check in a seal. */
private val BadgeStroke = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
internal fun Glyph(
    glyph: NotaGlyph,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    heavy: Boolean = false,
) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 24f, pivot = Offset.Zero) {
            glyph.paths.forEach { drawPath(it, tint, style = if (heavy) BadgeStroke else GlyphStroke) }
        }
    }
}

/**
 * Nota's light ink and paper, fixed rather than themed: the camera and the photo being read
 * stay dark in both themes, so their marks need the same contrast either way.
 */
internal val ScannerOverlay = Color(0xFF1D1F22)
internal val ScannerPaper = Color(0xFFFCFCF9)

/**
 * Four corner brackets marking where the receipt should sit.
 *
 * Two strokes per corner rather than a full border: boxing the frame in would read as a
 * crop tool, and the point is to suggest an aim, not a boundary. Shared by the live camera
 * and the screen that shows the photo being read, so the two stay the same shape.
 */
@Composable
internal fun FrameCorners(modifier: Modifier, tint: Color = ScannerPaper, arm: Dp = 34.dp) {
    Box(
        modifier.drawBehind {
            val armPx = arm.toPx()
            val w = 3.dp.toPx()
            val inset = w / 2f
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(tint, Offset(x, y + dy * inset), Offset(x + dx * armPx, y + dy * inset), w, StrokeCap.Round)
                drawLine(tint, Offset(x + dx * inset, y), Offset(x + dx * inset, y + dy * armPx), w, StrokeCap.Round)
            }
            corner(0f, 0f, 1f, 1f)
            corner(size.width, 0f, -1f, 1f)
            corner(0f, size.height, 1f, -1f)
            corner(size.width, size.height, -1f, -1f)
        },
    )
}

/**
 * Room for the status bar and any display cutout above a screen's header.
 *
 * The root composable reserves only the left and right insets, so each screen decides for
 * itself whether to sit below the system bars or run underneath them — Scanning wants the
 * photo full-bleed, everything else wants its header pushed clear.
 */
@Composable
internal fun Modifier.topBarInsets(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))

/**
 * Room for the navigation bar, and for the keyboard, below a screen's footer.
 *
 * safeDrawing rather than navigationBars because these footers carry the primary button: on
 * the screens with a text field it has to ride above the IME, not hide behind it.
 */
@Composable
internal fun Modifier.bottomBarInsets(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))

/**
 * Back chevron plus title — the header every inner screen shares. [close] swaps the chevron
 * for a cross, for screens that dismiss a draft rather than step back.
 */
@Composable
internal fun ScreenTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    close: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().topBarInsets()
            .height(56.dp).padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp)
                .then(if (onBack != null) Modifier.pressable(onClick = onBack, onClickLabel = if (close) "Tutup" else "Kembali") else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (onBack != null) Glyph(if (close) NotaGlyph.Close else NotaGlyph.ChevronLeft, scheme.onSurface)
        }
        if (subtitle == null) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
        } else {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = scheme.onSurfaceVariant)
            }
        }
    }
}

/** Bottom action bar: hairline rule, then the primary call to action. */
@Composable
internal fun BottomBar(
    modifier: Modifier = Modifier,
    divider: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().background(scheme.background)) {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

/**
 * The two-tab bar under the tab roots, hidden everywhere else. It owns the bottom inset, so
 * a screen inside a tab doesn't pad for the navigation bar again.
 */
@Composable
internal fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(scheme.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.onSurface))
        Row(Modifier.fillMaxWidth().height(72.dp).selectableGroup()) {
            NavItem("Pengeluaran", NotaGlyph.Chart, selected = current == Tab.Spend) { onSelect(Tab.Spend) }
            NavItem("Split", NotaGlyph.Receipt, selected = current == Tab.Splits) { onSelect(Tab.Splits) }
        }
        // navigationBars, not safeDrawing: a keyboard should cover the bar, not lift it.
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun RowScope.NavItem(label: String, glyph: NotaGlyph, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier.weight(1f).fillMaxHeight().selectable(
            selected = selected,
            interactionSource = interaction,
            indication = null,
            role = Role.Tab,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        // The whole tab takes the touch; only the pill shrinks.
        Box(
            modifier = Modifier.pressFeedback(interaction).size(width = 64.dp, height = 32.dp).clip(CircleShape)
                .background(if (selected) scheme.primaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(glyph, if (selected) scheme.primary else scheme.onSurfaceVariant)
        }
        Text(
            text = label,
            // Sans, unlike Nota's mono `label` that sits in labelMedium.
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
        )
    }
}
