package com.disheveled.bills.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disheveled.bills.model.Expense
import com.disheveled.bills.spend.MonthlyReport
import com.disheveled.bills.ui.AddButton
import com.disheveled.bills.ui.CategoryLine
import com.disheveled.bills.ui.CategoryStackBar
import com.disheveled.bills.ui.DashedDivider
import com.disheveled.bills.ui.DayCard
import com.disheveled.bills.ui.EmptyState
import com.disheveled.bills.ui.Glyph
import com.disheveled.bills.ui.MonthSwitcher
import com.disheveled.bills.ui.NotaGlyph
import com.disheveled.bills.ui.PrimaryButton
import com.disheveled.bills.ui.SecondaryButton
import com.disheveled.bills.ui.NotaSheet
import com.disheveled.bills.ui.SheetRow
import com.disheveled.bills.ui.SheetTitle
import com.disheveled.bills.ui.SpendState
import com.disheveled.bills.ui.SurfaceCard
import com.disheveled.bills.ui.changeLine
import com.disheveled.bills.ui.pressable
import com.disheveled.bills.ui.topBarInsets

/** The Pengeluaran tab: first-use page, or the month on screen with the add button. */
@Composable
internal fun SpendScreen(
    state: SpendState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenRecap: () -> Unit,
    onOpenExpense: (Expense) -> Unit,
    onScan: () -> Unit,
    onPickFromGallery: () -> Unit,
    onAddManual: () -> Unit,
) {
    when (state.hasExpenses) {
        // Not known yet: one database read away, not worth a spinner.
        null -> Unit
        false -> FirstUse(onScan = onScan, onAddManual = onAddManual)
        true -> {
            var sheetOpen by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxSize()) {
                MonthLog(state, onPreviousMonth, onNextMonth, onOpenRecap, onOpenExpense, onAddManual)
                // Hidden while the sheet is up: the sheet is the only other floating thing.
                if (!sheetOpen) {
                    AddButton(
                        onClick = { sheetOpen = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp),
                    )
                }
            }
            if (sheetOpen) {
                AddSheet(
                    onDismiss = { sheetOpen = false },
                    onScan = onScan,
                    onPickFromGallery = onPickFromGallery,
                    onAddManual = onAddManual,
                )
            }
        }
    }
}

@Composable
private fun MonthLog(
    state: SpendState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenRecap: () -> Unit,
    onOpenExpense: (Expense) -> Unit,
    onAddManual: () -> Unit,
) {
    val report = state.report ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize().topBarInsets(),
        // Room under the last day for the add button (56dp) plus a gap, so it never covers a row.
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 56.dp + 24.dp + 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(contentType = "month") {
            MonthSwitcher(
                month = report.month,
                total = report.total,
                changeLine = changeLine(report),
                canGoNext = state.canGoNext,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
            )
        }
        if (report.days.isEmpty()) {
            item(contentType = "empty") {
                EmptyState(
                    glyph = NotaGlyph.Chart,
                    title = "Belum ada pengeluaran bulan ini",
                    body = "Kalau ada yang lupa dicatat, tambahin manual aja. Tanggalnya bisa diatur.",
                    action = { SecondaryButton(label = "Catat manual", onClick = onAddManual) },
                )
            }
        } else {
            item(contentType = "categories") { CategoryCard(report, onOpenRecap) }
            items(report.days, key = { it.date.toEpochDays() }, contentType = { "day" }) { day ->
                DayCard(day, state.today, onOpenExpense)
            }
        }
    }
}

@Composable
private fun CategoryCard(report: MonthlyReport, onOpenRecap: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val top = report.topCategories(4)
    SurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("per kategori", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(
                    "Lihat rekap",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                    color = scheme.primary,
                    modifier = Modifier.pressable(onClick = onOpenRecap).padding(4.dp),
                )
            }
            CategoryStackBar(top)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                top.forEach { CategoryLine(it) }
            }
        }
    }
}

@Composable
private fun AddSheet(
    onDismiss: () -> Unit,
    onScan: () -> Unit,
    onPickFromGallery: () -> Unit,
    onAddManual: () -> Unit,
) {
    NotaSheet(onDismiss) { close ->
        SheetTitle("Tambah pengeluaran")
        Column {
            SheetRow(NotaGlyph.Camera, "Scan struk", "Total, tanggal, dan kategori dibaca otomatis") { close(onScan) }
            SheetRow(NotaGlyph.Gallery, "Pilih dari galeri", "Pakai foto struk yang udah ada") { close(onPickFromGallery) }
            SheetRow(NotaGlyph.Plus, "Catat manual", "Buat yang nggak ada struknya") { close(onAddManual) }
        }
    }
}

/** Before anything has been logged: where entries come from, and the ways to log the first one. */
@Composable
private fun FirstUse(onScan: () -> Unit, onAddManual: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .topBarInsets()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Pengeluaran", style = MaterialTheme.typography.displaySmall, color = scheme.onBackground)
            Text(
                "Catat ke mana aja duit kamu tiap bulan.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }

        SurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "masuknya dari tiga tempat",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                SourceRow(NotaGlyph.Receipt, "Bagian kamu dari split", "Abis split, tinggal simpan")
                DashedDivider()
                SourceRow(NotaGlyph.Camera, "Struk yang kamu bayar sendiri", "Total, tanggal, dan kategori kebaca")
                DashedDivider()
                SourceRow(NotaGlyph.Plus, "Catat manual", "Buat yang nggak ada struknya")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                label = "Scan struk",
                icon = { Glyph(NotaGlyph.Camera, scheme.onPrimary) },
                onClick = onScan,
            )
            SecondaryButton(
                label = "Catat manual",
                icon = { Glyph(NotaGlyph.Plus, scheme.onSurface) },
                onClick = onAddManual,
            )
        }

        Text(
            "AI cuma nyaranin kategori. Semua angka diitung sama app.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SourceRow(glyph: NotaGlyph, title: String, caption: String) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Glyph(glyph, scheme.primary) }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}
