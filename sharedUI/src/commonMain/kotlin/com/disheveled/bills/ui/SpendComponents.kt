package com.disheveled.bills.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disheveled.bills.model.Category
import com.disheveled.bills.model.Expense
import com.disheveled.bills.model.ExpenseSource
import com.disheveled.bills.spend.Change
import com.disheveled.bills.spend.CategoryTotal
import com.disheveled.bills.spend.DayGroup
import com.disheveled.bills.ui.theme.BillsTheme
import com.disheveled.bills.ui.theme.MoneyInput
import com.disheveled.bills.ui.theme.MoneyLarge
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/*
 * Nota components for the Pengeluaran tab, built to the design system's component specs.
 * Sizes and roles come from those specs; colours only from the theme.
 */

/** Month, its total and how it moved: the header of the tab and of the recap. */
@Composable
internal fun MonthSwitcher(
    month: YearMonth,
    total: Long,
    changeLine: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MonthButton(NotaGlyph.ChevronLeft, "Bulan sebelumnya", enabled = true, onClick = onPrevious)
            Text(
                text = monthLabel(month),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 150.dp),
            )
            // Unavailable on the current month: there is no future to look at.
            MonthButton(NotaGlyph.ChevronRight, "Bulan berikutnya", enabled = canGoNext, onClick = onNext)
        }
        Text(formatMoney(total), style = MoneyLarge, color = scheme.onBackground)
        Text(changeLine, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthButton(glyph: NotaGlyph, label: String, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.pressable(onClick = onClick, enabled = enabled, onClickLabel = label)
            .size(48.dp).clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) { Glyph(glyph, if (enabled) scheme.onSurface else scheme.outline) }
}

/** A category's glyph on its own tint. */
@Composable
internal fun CategoryTile(category: Category, size: Dp = 40.dp) {
    val tint = BillsTheme.colors.category(category)
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(tint.tile),
        contentAlignment = Alignment.Center,
    ) { Glyph(category.glyph, tint.glyph) }
}

/** Where an entry came from: "split", "scan", "manual". */
@Composable
internal fun SourceTag(text: String) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.height(20.dp)
            .border(1.dp, scheme.outline, RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant) }
}

/** On anything the model wrote or suggested, and nothing else. */
@Composable
internal fun AiLabel(modifier: Modifier = Modifier) {
    val colors = BillsTheme.colors
    Box(
        modifier = modifier.height(18.dp).clip(RoundedCornerShape(2.dp)).background(colors.marker)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "AI",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.66.sp),
            color = colors.onMarker,
        )
    }
}

/** One bar split by category, widths in proportion to the amounts. */
@Composable
internal fun CategoryStackBar(categories: List<CategoryTotal>) {
    val colors = BillsTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(2.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        categories.forEach { total ->
            Box(Modifier.weight(total.amount.toFloat()).height(10.dp).background(colors.category(total.category).glyph))
        }
    }
}

/** A dotted rule from a label to its amount. */
@Composable
internal fun RowScope.Leader() {
    val color = MaterialTheme.colorScheme.outline
    Box(
        Modifier.weight(1f).widthIn(min = 16.dp).padding(start = 6.dp, end = 6.dp, bottom = 5.dp).height(1.dp)
            .drawWithCache {
                val dots = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 2.dp.toPx()))
                onDrawBehind {
                    drawLine(
                        color = color,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = size.height,
                        pathEffect = dots,
                    )
                }
            },
    )
}

/** "Makan ······ 1.245.000", a row of the tab's category card. */
@Composable
internal fun CategoryLine(total: CategoryTotal) {
    val scheme = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp)
    Row(verticalAlignment = Alignment.Bottom) {
        Glyph(total.category.glyph, BillsTheme.colors.category(total.category).glyph, size = 20.dp)
        Spacer(Modifier.width(8.dp))
        Text(total.category.label, style = style, color = scheme.onSurface)
        Leader()
        Text(formatAmount(total.amount), style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp), color = scheme.onSurface)
    }
}

/** A row of the recap: share of the month and the move against last month. */
@Composable
internal fun RecapCategoryRow(total: CategoryTotal) {
    val scheme = MaterialTheme.colorScheme
    val colors = BillsTheme.colors
    val changeColor = when (val change = total.change) {
        is Change.Percent if change.value > 0 -> scheme.error
        is Change.Percent if change.value < 0 -> colors.ok
        else -> scheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryTile(total.category, size = 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(total.category.label, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text("${total.percent}%", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(formatAmount(total.amount), style = RowAmount, color = scheme.onSurface)
            Text(changeTag(total.change), style = MaterialTheme.typography.labelSmall, color = changeColor)
        }
    }
}

private val RowAmount: androidx.compose.ui.text.TextStyle
    @Composable get() = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp)

/** One day of the log: its label and total, then its entries on one receipt. */
@Composable
internal fun DayCard(group: DayGroup, today: LocalDate, onOpen: (Expense) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dayLabel(group.date, today), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Text(formatMoney(group.total).lowercase(), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }
        SurfaceCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
            Column {
                group.expenses.forEachIndexed { index, expense ->
                    if (index > 0) DashedDivider()
                    ExpenseRow(expense, onClick = { onOpen(expense) })
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        // Held, the row turns paper-sunk; it doesn't shrink inside its receipt.
        modifier = Modifier.pressable(onClick = onClick, press = Press.Row).fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryTile(expense.category)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                expense.title,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SourceTag(expense.source.tag)
                Text(
                    if (expense.source == ExpenseSource.Split) "bagian kamu" else expense.category.label.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Text(formatAmount(expense.amount), style = RowAmount, color = scheme.onSurface)
    }
}

/** The tab's one floating control. */
@Composable
internal fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier.pressable(onClick = onClick).height(56.dp).shadow(6.dp, shape).clip(shape)
            .background(scheme.primary).padding(start = 18.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Glyph(NotaGlyph.Plus, scheme.onPrimary)
        Text("Tambah", style = MaterialTheme.typography.labelLarge, color = scheme.onPrimary)
    }
}

/** Where a list would be: what the space is for and how to fill it. */
@Composable
internal fun EmptyState(
    glyph: NotaGlyph,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(4.dp)
    val outline = scheme.outline
    Column(
        modifier = Modifier.fillMaxWidth().clip(shape).background(scheme.surface)
            .drawWithCache {
                val dashed = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                )
                val corner = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                onDrawBehind { drawRoundRect(color = outline, style = dashed, cornerRadius = corner) }
            }
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(scheme.background),
            contentAlignment = Alignment.Center,
        ) { Glyph(glyph, scheme.primary) }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 250.dp),
        )
        if (action != null) action()
    }
}

/** The AI note on the recap, its loading state, or nothing when there's no note. */
@Composable
internal fun AiNoteCard(note: RecapNote, caption: String) = when (note) {
    RecapNote.Hidden -> Unit
    RecapNote.Loading -> AiNote(text = null, loading = true, caption = caption)
    is RecapNote.Ready -> AiNote(text = note.text, loading = false, caption = caption)
}

/**
 * What the model wrote: paper, ink hairline, the AI label and a caption, the text with up to
 * two highlighted figures. While [loading], "lagi nyusun catatan…" over skeleton lines.
 */
@Composable
internal fun AiNote(text: String?, loading: Boolean, caption: String) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier = Modifier.fillMaxWidth().clip(shape).background(scheme.surface)
            .border(1.dp, scheme.onSurface, shape).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiLabel()
            Text(
                if (loading) "lagi nyusun catatan…" else caption,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        }
        if (!loading && text != null) {
            Text(highlighted(text), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
        } else {
            listOf(1f, 0.92f, 0.6f).forEach { width ->
                Box(Modifier.fillMaxWidth(width).height(10.dp).clip(RoundedCornerShape(2.dp)).background(scheme.surfaceVariant))
            }
        }
    }
}

/** Turns the note's `**figure**` marks into marker highlights — at most two, as Nota allows. */
@Composable
private fun highlighted(text: String): AnnotatedString {
    val colors = BillsTheme.colors
    return remember(text, colors) {
        buildAnnotatedString {
            text.split("**").forEachIndexed { index, part ->
                val marked = index % 2 == 1 && index <= 3
                if (marked) withStyle(SpanStyle(background = colors.marker, color = colors.onMarker)) { append(part) }
                else append(part)
            }
        }
    }
}

/**
 * Amount entry: big mono digits with "." separators drawn in, the value stays a Long.
 * [locked] shows it read-only with a muted label that says why ("Jumlah · ngikut split").
 */
@Composable
internal fun AmountField(
    value: Long,
    onValueChange: (Long) -> Unit,
    label: String = "Jumlah",
    locked: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val rule = if (locked) scheme.outline else scheme.primary
    Column(
        modifier = Modifier.fillMaxWidth().drawBehind {
            val stroke = (if (locked) 1.dp else 2.dp).toPx()
            drawLine(rule, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
        }.padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = if (locked) scheme.onSurfaceVariant else scheme.primary,
        )
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Rp",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 24.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Box(Modifier.weight(1f)) {
                if (locked) {
                    Text(formatAmount(value), style = MoneyInput, color = scheme.onSurface)
                    return@Box
                }
                if (value == 0L) Text("0", style = MoneyInput, color = scheme.onSurfaceVariant)
                BasicTextField(
                    value = if (value == 0L) "" else value.toString(),
                    // Separators and "Rp" typed by habit are dropped; there are no decimals.
                    onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() }.take(12).toLongOrNull() ?: 0L) },
                    textStyle = MoneyInput.copy(color = scheme.onSurface),
                    cursorBrush = SolidColor(scheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparator,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Draws "38000" as "38.000" while the field itself only ever holds digits. */
private object ThousandsSeparator : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val out = StringBuilder()
        val digitAt = IntArray(digits.length + 1)
        digits.forEachIndexed { i, c ->
            if (i > 0 && (digits.length - i) % 3 == 0) out.append('.')
            digitAt[i] = out.length
            out.append(c)
        }
        digitAt[digits.length] = out.length
        val shown = out.toString()
        return TransformedText(
            AnnotatedString(shown),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = digitAt[offset]
                override fun transformedToOriginal(offset: Int) = shown.take(offset).count { it != '.' }
            },
        )
    }
}

/** Nota's outlined field: label on a notch in the border, 2dp stamp border while focused. */
@Composable
internal fun NotaTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FieldFrame(label, focused) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = FieldText, color = scheme.onSurfaceVariant)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = FieldText.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                interactionSource = interaction,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A read-only field that opens a picker: the date, the category on locked entries. */
@Composable
internal fun PickerField(label: String, value: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    FieldFrame(label, focused = false, onClick = onClick) {
        Text(value, style = FieldText, color = scheme.onSurface, modifier = Modifier.weight(1f))
        Glyph(NotaGlyph.ChevronDown, scheme.onSurfaceVariant, size = 20.dp)
    }
}

private val FieldText: androidx.compose.ui.text.TextStyle
    @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)

@Composable
private fun FieldFrame(
    label: String,
    focused: Boolean,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            // A picker field acts like a control; a text field never shrinks under the finger.
            modifier = Modifier.then(if (onClick != null) Modifier.pressable(onClick = onClick, press = Press.Large) else Modifier)
                // No fill: Nota's field is just its outline over whatever it sits on.
                .fillMaxWidth().heightIn(min = 56.dp)
                .border(if (focused) 2.dp else 1.dp, if (focused) scheme.primary else scheme.outline, shape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
        // Cut into the border with the page colour, the same colour that shows through the
        // field, so it reads as a gap in the line rather than a chip. Every field sits on the
        // page today; one inside a sheet would need the sheet's colour here instead.
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
            color = if (focused) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.offset(x = 10.dp, y = (-8).dp).background(scheme.background).padding(horizontal = 5.dp),
        )
    }
}

/** The eight categories, four to a row, exactly one selected. */
@Composable
internal fun CategoryGrid(selected: Category, suggested: Category?, onPick: (Category) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Category.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    CategoryChip(
                        category = category,
                        selected = category == selected,
                        suggested = category == suggested,
                        onClick = { onPick(category) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: Category, selected: Boolean, suggested: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val tint = BillsTheme.colors.category(category)
    val shape = RoundedCornerShape(8.dp)
    Box(modifier) {
        Column(
            modifier = Modifier.pressable(onClick = onClick, role = Role.RadioButton)
                .fillMaxWidth().height(72.dp).clip(shape)
                .background(if (selected) tint.tile else scheme.surface)
                .border(if (selected) 2.dp else 1.dp, if (selected) tint.glyph else scheme.outline, shape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Glyph(category.glyph, tint.glyph)
            Text(
                category.label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        // The label stays on the model's pick even after the person chooses another.
        if (suggested) AiLabel(Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = (-9).dp))
    }
}

/** A row of the add sheet: icon square, title, one helper line. */
@Composable
internal fun SheetRow(glyph: NotaGlyph, title: String, helper: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.pressable(onClick = onClick, press = Press.Row).fillMaxWidth().heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Glyph(glyph, scheme.primary) }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium), color = scheme.onSurface)
            Text(helper, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

/** The category of a receipt on the review screen; opens the category sheet. */
@Composable
internal fun CategoryButton(category: Category, suggested: Category?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tint = BillsTheme.colors.category(category)
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier.pressable(onClick = onClick).heightIn(min = 40.dp).clip(shape).background(scheme.surface)
            .border(1.dp, scheme.outline, shape).padding(start = 6.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(tint.tile),
            contentAlignment = Alignment.Center,
        ) { Glyph(category.glyph, tint.glyph, size = 20.dp) }
        Text(
            category.label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
            color = scheme.onSurface,
        )
        if (category == suggested) AiLabel()
        Glyph(NotaGlyph.ChevronDown, scheme.onSurfaceVariant, size = 20.dp)
    }
}

private const val STAMP_TILT = -6f

/** A rubber stamp: thick ring, thin ring around it, tilted a little. */
@Composable
internal fun Stamp(text: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier.tiltedBounds(STAMP_TILT).rotate(STAMP_TILT)
            .border(1.dp, scheme.primary, shape).padding(2.dp)
            .border(2.dp, scheme.primary, shape).padding(horizontal = 7.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.66.sp),
            color = scheme.primary,
        )
    }
}

/**
 * Lays the content out in the box it covers once rotated by [degrees]. `rotate` only turns the
 * drawing, so a tilted stamp's corners stuck out of its slot and got clipped by whatever scrolled
 * or clipped around it — the top of the summary cut the corner off "tercatat".
 */
private fun Modifier.tiltedBounds(degrees: Float) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val radians = degrees * PI.toFloat() / 180f
    val cos = abs(cos(radians))
    val sin = abs(sin(radians))
    val width = ceil(placeable.width * cos + placeable.height * sin).toInt()
    val height = ceil(placeable.width * sin + placeable.height * cos).toInt()
    layout(width, height) { placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2) }
}

/**
 * A reconciliation line: a green check when things tie out, a red "!" when they don't, always
 * with the sentence that says so, and maybe a link.
 */
@Composable
internal fun ReconLine(ok: Boolean, text: AnnotatedString, action: String? = null, onAction: () -> Unit = {}) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(scheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(if (ok) BillsTheme.colors.ok else scheme.error),
            contentAlignment = Alignment.Center,
        ) {
            if (ok) Glyph(NotaGlyph.Check, scheme.surface, size = 14.dp, heavy = true)
            else Text("!", style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = scheme.surface)
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                color = scheme.primary,
                modifier = Modifier.pressable(onClick = onAction).padding(4.dp),
            )
        }
    }
}

/** A tappable card that leads somewhere: tile, two lines, chevron. */
@Composable
internal fun LinkCard(
    glyph: NotaGlyph,
    title: String,
    subtitle: String,
    outlined: Boolean,
    onClick: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(if (outlined) 8.dp else 4.dp)
    Row(
        modifier = Modifier.then(if (onClick != null) Modifier.pressable(onClick = onClick, press = Press.Large) else Modifier)
            .fillMaxWidth().clip(shape)
            .background(if (outlined) scheme.surface else scheme.surfaceVariant)
            .then(if (outlined) Modifier.border(1.dp, scheme.primary, shape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (outlined) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(scheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Glyph(glyph, scheme.primary) }
        } else {
            Glyph(glyph, scheme.primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val first = MaterialTheme.typography.bodySmall
            val second = MaterialTheme.typography.titleMedium
            // The outlined card leads with its action; the quiet one leads with where it's from.
            if (outlined) {
                Text(title, style = second, color = scheme.onSurface)
                Text(subtitle, style = first, color = scheme.onSurfaceVariant)
            } else {
                Text(title, style = first, color = scheme.onSurfaceVariant)
                Text(subtitle, style = second, color = scheme.onSurface)
            }
        }
        if (onClick != null) Glyph(NotaGlyph.ChevronRight, scheme.onSurfaceVariant, size = 20.dp)
    }
}
