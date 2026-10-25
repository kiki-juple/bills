package com.disheveled.bills.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.disheveled.bills.model.Category
import com.disheveled.bills.model.ExpenseSource
import com.disheveled.bills.ui.DraftOrigin
import com.disheveled.bills.ui.LinkCard
import com.disheveled.bills.ui.NotaGlyph
import com.disheveled.bills.ui.AmountField
import com.disheveled.bills.ui.CategoryGrid
import com.disheveled.bills.ui.ExpenseDraft
import com.disheveled.bills.ui.NotaTextField
import com.disheveled.bills.ui.PickerField
import com.disheveled.bills.ui.PlainButton
import com.disheveled.bills.ui.PrimaryButton
import com.disheveled.bills.ui.ScreenTopBar
import com.disheveled.bills.ui.bottomBarInsets
import com.disheveled.bills.ui.dateFieldLabel
import kotlinx.datetime.LocalDate

/** Logging an expense by hand, or changing one already logged. */
@Composable
internal fun ExpenseEditorScreen(
    draft: ExpenseDraft,
    today: LocalDate,
    onClose: () -> Unit,
    onAmountChange: (Long) -> Unit,
    onTitleChange: (String) -> Unit,
    onPickCategory: (Category) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onOpenSplit: (splitId: String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isNew = draft.original == null
    val fromReceipt = draft.origin is DraftOrigin.Receipt
    var pickingDate by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(scheme.background)) {
        ScreenTopBar(
            title = when {
                !isNew -> "Edit pengeluaran"
                fromReceipt -> "Simpan pengeluaran"
                else -> "Tambah pengeluaran"
            },
            onBack = onClose,
            // A fresh manual entry is a draft to throw away; a scan steps back to its review.
            close = isNew && !fromReceipt,
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            when (val origin = draft.origin) {
                is DraftOrigin.Receipt -> OriginCard(
                    title = "Struk yang kamu scan",
                    subtitle = "${origin.merchant} · ${origin.itemCount} item",
                    caption = "Kamu bayar sendiri, jadi langsung dicatat full. Nggak perlu pilih orang.",
                    onClick = null,
                )
                is DraftOrigin.Split -> OriginCard(
                    title = "Bagian kamu dari split",
                    subtitle = origin.label,
                    caption = "Jumlahnya ngikut hasil split. Buka split-nya buat lihat rinciannya.",
                    onClick = { onOpenSplit(origin.splitId) },
                )
                null -> Unit
            }
            AmountField(
                value = draft.amount,
                onValueChange = onAmountChange,
                label = when {
                    !draft.amountLocked -> "Jumlah"
                    draft.source == ExpenseSource.Split -> "Jumlah · ngikut split"
                    else -> "Jumlah · dari struk"
                },
                locked = draft.amountLocked,
            )
            NotaTextField(label = "Judul", value = draft.title, onValueChange = onTitleChange)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("kategori", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                // Only scanned receipts carry the model's pick; manual entries never show the label.
                CategoryGrid(selected = draft.category, suggested = draft.suggested, onPick = onPickCategory)
                if (draft.guessed) {
                    Text(
                        "Ditebak dari judulnya. Tap lainnya buat ganti.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            PickerField(
                label = if (draft.dateFromReceipt) "Tanggal · dari struk" else "Tanggal",
                value = dateFieldLabel(draft.date, today),
                onClick = { pickingDate = true },
            )
            NotaTextField(label = "Catatan", value = draft.note, onValueChange = onNoteChange, placeholder = "Opsional")
        }

        Column(Modifier.fillMaxWidth().bottomBarInsets()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PrimaryButton(
                    label = when {
                        !isNew -> "Simpan perubahan"
                        fromReceipt -> "Simpan ke Pengeluaran"
                        else -> "Simpan"
                    },
                    enabled = draft.canSave,
                    onClick = onSave,
                )
                if (!isNew) PlainButton("Hapus pengeluaran", onClick = onDelete, color = scheme.error)
            }
        }
    }

    if (pickingDate) {
        ExpenseDatePicker(
            selected = draft.date,
            today = today,
            onPick = { onDateChange(it); pickingDate = false },
            onDismiss = { pickingDate = false },
        )
    }
}

@Composable
private fun OriginCard(title: String, subtitle: String, caption: String, onClick: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinkCard(glyph = NotaGlyph.Receipt, title = title, subtitle = subtitle, outlined = false, onClick = onClick)
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Material's date picker counts in UTC midnights, so dates cross over as whole epoch days —
 * converting through a time zone would shift them by one for anyone east of Greenwich.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDatePicker(
    selected: LocalDate,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val todayMillis = today.toEpochDays() * MILLIS_PER_DAY
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected.toEpochDays() * MILLIS_PER_DAY,
        // Spending hasn't happened yet in the future.
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            override fun isSelectableYear(year: Int) = year <= today.year
        },
    )
    val colors = DatePickerDefaults.colors(containerColor = scheme.surface)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(LocalDate.fromEpochDays(it / MILLIS_PER_DAY)) } ?: onDismiss()
            }) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        colors = colors,
    ) {
        DatePicker(
            state = state,
            colors = colors,
            title = {
                Text(
                    "Tanggal pengeluaran",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
        )
    }
}
