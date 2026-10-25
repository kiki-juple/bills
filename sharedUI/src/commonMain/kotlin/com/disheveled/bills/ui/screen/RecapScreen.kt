package com.disheveled.bills.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.disheveled.bills.spend.indonesianName
import com.disheveled.bills.ui.AiNoteCard
import com.disheveled.bills.ui.CategoryStackBar
import com.disheveled.bills.ui.EmptyState
import com.disheveled.bills.ui.MonthSwitcher
import com.disheveled.bills.ui.NotaGlyph
import com.disheveled.bills.ui.RecapCategoryRow
import com.disheveled.bills.ui.ScreenTopBar
import com.disheveled.bills.ui.SpendState
import com.disheveled.bills.ui.SurfaceCard
import com.disheveled.bills.ui.changeLine
import com.disheveled.bills.ui.formatMoney
import kotlinx.datetime.minusMonth

/** Every category of a month against the one before, with the AI's note on what moved. */
@Composable
internal fun RecapScreen(
    state: SpendState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(scheme.background)) {
        ScreenTopBar(title = "Rekap bulanan", onBack = onBack)
        val report = state.report ?: return@Column
        val previous = report.month.minusMonth().month.indonesianName().lowercase()

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MonthSwitcher(
                month = report.month,
                total = report.total,
                changeLine = changeLine(report) +
                    if (report.total > 0 && report.previousTotal > 0) " (${formatMoney(report.previousTotal).lowercase()})" else "",
                canGoNext = state.canGoNext,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
            )

            if (report.categories.isEmpty()) {
                EmptyState(
                    glyph = NotaGlyph.Chart,
                    title = "Belum ada pengeluaran bulan ini",
                    body = "Rekapnya muncul begitu ada yang dicatat di bulan ini.",
                )
                return@Column
            }

            AiNoteCard(state.note, caption = "catatan bulan ini")

            SurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("semua kategori", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("vs $previous", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    }
                    CategoryStackBar(report.categories)
                    Column { report.categories.forEach { RecapCategoryRow(it) } }
                }
            }
        }
    }
}
