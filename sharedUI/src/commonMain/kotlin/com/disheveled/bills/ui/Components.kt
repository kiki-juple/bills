package com.disheveled.bills.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disheveled.bills.model.Person
import kotlin.math.roundToInt

/** The one main action on a screen: stamp fill, full width. */
@Composable
internal fun PrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .pressable(onClick = onClick, enabled = enabled)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) scheme.primary else scheme.surfaceVariant)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null && enabled) icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) scheme.onPrimary else scheme.onSurfaceVariant,
        )
    }
}

/** The alternative action: paper with an ink outline. */
@Composable
internal fun SecondaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val ink = if (enabled) scheme.onSurface else scheme.onSurfaceVariant
    Row(
        modifier = modifier
            .pressable(onClick = onClick, enabled = enabled)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, if (enabled) scheme.onSurface else scheme.outline, shape)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) icon()
        Text(label, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

/** A text-only action: "Selesai", "Aku nggak ikut di sini", or a delete in [color]. */
@Composable
internal fun PlainButton(
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        Modifier.pressable(onClick = onClick).fillMaxWidth().heightIn(min = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/**
 * A person's initial in an ink-ruled circle. Neutral on purpose: people are told apart by name
 * and initial, never by colour, so avatars don't compete with category tints.
 */
@Composable
internal fun Avatar(
    person: Person,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.surfaceVariant)
            .border(1.dp, scheme.onSurface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = person.initial,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.4f).sp,
                lineHeight = (size.value * 0.4f).sp,
            ),
            color = scheme.onSurface,
        )
    }
}

/** Overlapping avatars, tucked 8dp under each other. */
@Composable
internal fun AvatarStack(
    people: List<Person>,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    max: Int = 5,
) {
    val scheme = MaterialTheme.colorScheme
    val shown = people.take(max)
    Row(modifier) {
        shown.forEachIndexed { i, person ->
            Avatar(
                person = person,
                size = size,
                modifier = if (i == 0) Modifier else Modifier.offset(x = (-8).dp * i),
            )
        }
        if (people.size > shown.size) {
            Box(
                modifier = Modifier
                    .offset(x = (-8).dp * shown.size)
                    .size(size)
                    .clip(CircleShape)
                    .background(scheme.surfaceVariant)
                    .border(1.dp, scheme.onSurface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+${people.size - shown.size}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A receipt: paper with an ink hairline on three sides and a torn zigzag edge at the bottom.
 * Bills and lists sit on these; [onClick] makes the whole slip a button that shrinks a little.
 */
@Composable
internal fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, press = Press.Large) else Modifier)
            .fillMaxWidth(),
    ) {
        val ink = scheme.onSurface
        Box(
            Modifier.fillMaxWidth()
                .background(scheme.surface, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                // Paths and strokes built once per size, not on every draw of every receipt.
                .drawWithCache {
                    val stroke = Stroke(width = 1.dp.toPx())
                    val border = openTopBorder(size, radius = 4.dp.toPx(), stroke = stroke.width)
                    onDrawBehind { drawPath(border, ink, style = stroke) }
                }
                .padding(contentPadding),
        ) { content() }
        TornEdge()
    }
}

/** Left, top and right sides with rounded top corners; the bottom is left to the torn edge. */
private fun openTopBorder(size: Size, radius: Float, stroke: Float): Path {
    val half = stroke / 2f
    return Path().apply {
        moveTo(half, size.height)
        lineTo(half, radius)
        quadraticTo(half, half, radius, half)
        lineTo(size.width - radius, half)
        quadraticTo(size.width - half, half, size.width - half, radius)
        lineTo(size.width - half, size.height)
    }
}

/** The bottom of a receipt torn off the roll: 16dp teeth, 8dp deep. */
@Composable
internal fun TornEdge() {
    val scheme = MaterialTheme.colorScheme
    val paper = scheme.surface
    val ink = scheme.onSurface
    Spacer(
        Modifier.fillMaxWidth().height(8.dp).drawWithCache {
            val teeth = (size.width / 16.dp.toPx()).roundToInt().coerceAtLeast(1)
            val tooth = size.width / teeth
            val half = 0.5.dp.toPx()
            val zigzag = Path().apply {
                moveTo(half, 0f)
                for (i in 0 until teeth) {
                    lineTo(i * tooth + tooth / 2f, size.height - half)
                    lineTo(if (i == teeth - 1) size.width - half else (i + 1) * tooth, 0f)
                }
            }
            val torn = Path().apply { addPath(zigzag); close() }
            val stroke = Stroke(width = 1.dp.toPx())
            onDrawBehind {
                drawPath(torn, paper)
                drawPath(zigzag, ink, style = stroke)
            }
        },
    )
}

/** Selected: a stamp seal with a check. Not: an empty ring. */
@Composable
internal fun CheckDot(selected: Boolean, size: Dp = 22.dp) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(size).clip(CircleShape).then(
            if (selected) Modifier.background(scheme.primary)
            else Modifier.border(1.dp, scheme.outline, CircleShape),
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Glyph(NotaGlyph.Check, scheme.onPrimary, size = size * 0.64f, heavy = true)
    }
}

/** A 1dp dashed rule between groups inside a card. */
@Composable
internal fun DashedDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier.fillMaxWidth().height(1.dp).drawWithCache {
            val dashes = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
            onDrawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = size.height,
                    pathEffect = dashes,
                )
            }
        },
    )
}

@Composable
internal fun MoneyText(
    amount: Long,
    currency: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = formatMoney(amount, currency),
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
