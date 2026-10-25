package com.disheveled.bills.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disheveled.bills.model.Category
import com.disheveled.bills.model.Person
import com.disheveled.bills.split.PersonShare
import kotlinx.coroutines.launch

/**
 * Nota's bottom sheet: paper, a small handle, the ink scrim. [content] gets `close`, which
 * slides the sheet away before running what was chosen, so it isn't still on its way out
 * over the next screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotaSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(close: (then: () -> Unit) -> Unit) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { then ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            then()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = scheme.surface,
        scrimColor = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.6f) else scheme.onSurface.copy(alpha = 0.45f),
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(scheme.outline),
            )
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content(close) }
    }
}

@Composable
internal fun SheetTitle(title: String, body: String? = null) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** "Kategori struk ini": the grid, applied only when the button is pressed. */
@Composable
internal fun CategorySheet(
    selected: Category,
    suggested: Category?,
    explanation: String,
    onApply: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    var choice by remember { mutableStateOf(selected) }
    NotaSheet(onDismiss) { close ->
        SheetTitle("Kategori struk ini", explanation)
        CategoryGrid(selected = choice, suggested = suggested, onPick = { choice = it })
        PrimaryButton(label = "Pakai kategori ini", onClick = { close { onApply(choice) } })
    }
}

/** "Yang mana kamu?": asked once, then remembered by name for the next split. */
@Composable
internal fun SelfPickerSheet(
    shares: List<PersonShare>,
    currency: String,
    onSave: (Person) -> Unit,
    onNotMe: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var chosen by remember { mutableStateOf<Person?>(null) }
    NotaSheet(onDismiss) { close ->
        SheetTitle("Yang mana kamu?", "Cukup sekali. Split berikutnya kami inget, jadi tinggal simpan.")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            shares.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { share ->
                        PersonChoice(
                            share = share,
                            index = shares.indexOf(share),
                            selected = chosen?.id == share.person.id,
                            onClick = { chosen = share.person },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Keeps a lone last card half-width instead of stretching across.
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val amount = shares.firstOrNull { it.person.id == chosen?.id }?.total
            PrimaryButton(
                label = if (amount != null) "Simpan ${formatMoney(amount, currency)}" else "Simpan",
                enabled = chosen != null,
                onClick = { chosen?.let { person -> close { onSave(person) } } },
            )
            PlainButton("Aku nggak ikut di sini", onClick = { close(onNotMe) })
        }
    }
}

@Composable
private fun PersonChoice(
    share: PersonShare,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.pressable(onClick = onClick, role = Role.RadioButton).clip(shape)
            .background(if (selected) scheme.primaryContainer else scheme.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outline, shape),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Avatar(share.person, size = 40.dp)
            Text(share.person.name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(
                formatAmount(share.total),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, letterSpacing = 0.sp),
                color = scheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).clip(CircleShape).background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) { Glyph(NotaGlyph.Check, scheme.onPrimary, size = 14.dp, heavy = true) }
        }
    }
}
