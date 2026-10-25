package com.disheveled.bills.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disheveled.bills.ui.Glyph
import com.disheveled.bills.ui.topBarInsets
import com.disheveled.bills.ui.Avatar
import com.disheveled.bills.ui.AvatarStack
import com.disheveled.bills.ui.DashedDivider
import com.disheveled.bills.ui.EmptyState
import com.disheveled.bills.ui.Leader
import com.disheveled.bills.ui.Stamp
import com.disheveled.bills.ui.formatAmount
import com.disheveled.bills.ui.NotaGlyph
import com.disheveled.bills.ui.HistoryEntry
import com.disheveled.bills.ui.MoneyText
import com.disheveled.bills.ui.PrimaryButton
import com.disheveled.bills.ui.SecondaryButton
import com.disheveled.bills.ui.SurfaceCard
import com.disheveled.bills.ui.theme.MoneyMedium

/** The Split tab: start a split, and the history of saved ones. */
@Composable
internal fun SplitsScreen(
    history: List<HistoryEntry>,
    onScan: () -> Unit,
    onPickFromGallery: () -> Unit,
    onOpenSplit: (splitId: String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // Lazy because the history only grows: every split ever saved used to compose on each visit.
    LazyColumn(
        modifier = Modifier.fillMaxSize().topBarInsets(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 48.dp),
        // The cards' 12dp; the sections above add 12dp more below themselves to sit 24dp apart.
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(contentType = "header") {
            Column(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Bills", style = MaterialTheme.typography.displaySmall, color = scheme.onBackground)
                Text(
                    "Foto struknya, sisanya biar kami itung.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        item(contentType = "actions") {
            Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    label = "Scan struk",
                    icon = { Glyph(NotaGlyph.Camera, scheme.onPrimary) },
                    onClick = onScan,
                )
                SecondaryButton(
                    label = "Pilih dari galeri",
                    icon = { Glyph(NotaGlyph.Gallery, scheme.onSurface) },
                    onClick = onPickFromGallery,
                )
            }
        }

        item(contentType = "label") {
            Text("riwayat split", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }

        if (history.isEmpty()) {
            item(contentType = "empty") {
                EmptyState(
                    glyph = NotaGlyph.Receipt,
                    title = "Belum ada split",
                    body = "Struk pertama kamu bakal nongol di sini abis di-scan.",
                )
            }
        } else {
            items(history, key = { it.splitId }, contentType = { "split" }) { entry ->
                HistoryCard(entry, onClick = { onOpenSplit(entry.splitId) })
            }
        }
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    SurfaceCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(entry.merchant, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    Text(
                        "${entry.dateLabel} · ${entry.people.size} orang",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                MoneyText(entry.total, entry.currency, MoneyMedium, color = scheme.onSurface)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarStack(entry.people)
                Text(
                    entry.people.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (entry.loggedShare != null) Stamp("tercatat")
            }
            entry.loggedShare?.let { amount ->
                DashedDivider()
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("bagian kamu", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    Leader()
                    Text(
                        formatAmount(amount),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, letterSpacing = 0.sp),
                        color = scheme.onSurface,
                    )
                }
            }
        }
    }
}
