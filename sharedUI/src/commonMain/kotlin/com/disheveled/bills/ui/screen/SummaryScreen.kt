package com.disheveled.bills.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disheveled.bills.model.Person
import com.disheveled.bills.split.PersonShare
import com.disheveled.bills.split.SplitResult
import com.disheveled.bills.ui.AiNote
import com.disheveled.bills.ui.Avatar
import com.disheveled.bills.ui.DashedDivider
import com.disheveled.bills.ui.Glyph
import com.disheveled.bills.ui.Leader
import com.disheveled.bills.ui.LinkCard
import com.disheveled.bills.ui.NotaGlyph
import com.disheveled.bills.ui.PlainButton
import com.disheveled.bills.ui.Press
import com.disheveled.bills.ui.PrimaryButton
import com.disheveled.bills.ui.ReconLine
import com.disheveled.bills.ui.ScreenTopBar
import com.disheveled.bills.ui.SelfPickerSheet
import com.disheveled.bills.ui.SourceTag
import com.disheveled.bills.ui.Stamp
import com.disheveled.bills.ui.SurfaceCard
import com.disheveled.bills.ui.bottomBarInsets
import com.disheveled.bills.ui.formatAmount
import com.disheveled.bills.ui.formatMoney
import com.disheveled.bills.ui.pressable

/** Whether the user's share of this split is in Pengeluaran, or can be put there. */
internal sealed interface ShareState {
    /** The user said they weren't in it. */
    data object Hidden : ShareState

    /** Not logged yet; [month] is where it would land, "September". */
    data class Offer(val month: String) : ShareState

    data class Logged(val personId: String?, val amount: Long) : ShareState
}

@Composable
internal fun SummaryScreen(
    split: SplitResult,
    dateLabel: String,
    summary: String?,
    summaryLoading: Boolean,
    shareState: ShareState,
    /** The user, when their name from last time is among the people; saves without asking. */
    knownSelf: Person?,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDone: () -> Unit,
    onLogShare: (Person) -> Unit,
    onNotInSplit: () -> Unit,
    onOpenShare: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val currency = split.receipt.currency
    var expanded by remember { mutableStateOf<String?>(null) }
    var askingWho by remember { mutableStateOf(false) }
    val selfId = (shareState as? ShareState.Logged)?.personId

    Column(Modifier.fillMaxSize().background(scheme.background)) {
        ScreenTopBar("Hasilnya", onBack)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    split.receipt.merchant.ifBlank { "Struk" },
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                if (shareState is ShareState.Logged) {
                    Stamp("tercatat")
                    Spacer(Modifier.width(10.dp))
                }
                Text(dateLabel.lowercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = scheme.onSurfaceVariant)
            }

            // One receipt for the whole table: each person's line, their breakdown on tap, the total.
            SurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    split.shares.forEachIndexed { index, share ->
                        if (index > 0) DashedDivider()
                        ShareLine(
                            share = share,
                            currency = currency,
                            isSelf = share.person.id == selfId,
                            expanded = expanded == share.person.id,
                            onToggle = { expanded = if (expanded == share.person.id) null else share.person.id },
                        )
                    }
                    DashedDivider()
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Total", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = scheme.onSurface)
                        Leader()
                        Text(formatMoney(split.total, currency), style = ShareAmount, color = scheme.onSurface)
                    }
                }
            }

            Reconciliation(split)

            if (summaryLoading || summary != null) {
                AiNote(text = summary, loading = summaryLoading, caption = "kenapa beda-beda")
            }

            when (shareState) {
                ShareState.Hidden -> Unit
                is ShareState.Offer -> LinkCard(
                    glyph = NotaGlyph.Plus,
                    title = "Simpan bagian kamu",
                    subtitle = "Masuk ke Pengeluaran ${shareState.month}",
                    outlined = true,
                    onClick = { if (knownSelf != null) onLogShare(knownSelf) else askingWho = true },
                )
                is ShareState.Logged -> ReconLine(
                    ok = true,
                    text = AnnotatedString("${formatMoney(shareState.amount, currency)} udah masuk Pengeluaran"),
                    action = "Lihat",
                    onAction = onOpenShare,
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().bottomBarInsets()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PrimaryButton(
                label = "Share ke WhatsApp",
                icon = { Glyph(NotaGlyph.Share, scheme.onPrimary) },
                onClick = onShare,
            )
            PlainButton("Selesai", onClick = onDone)
        }
    }

    if (askingWho) {
        SelfPickerSheet(
            shares = split.shares,
            currency = currency,
            onSave = onLogShare,
            onNotMe = onNotInSplit,
            onDismiss = { askingWho = false },
        )
    }
}

private val ShareAmount: androidx.compose.ui.text.TextStyle
    @Composable get() = MaterialTheme.typography.labelMedium.copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.sp)

/** A person's line on the receipt; tapping it unfolds what their amount is made of. */
@Composable
private fun ShareLine(
    share: PersonShare,
    currency: String,
    isSelf: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.pressable(onClick = onToggle, press = Press.Row, onClickLabel = "Lihat rinciannya"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(share.person, size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Text(share.person.name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            if (isSelf) {
                Spacer(Modifier.width(6.dp))
                SourceTag("kamu")
            }
            Leader()
            Text(formatAmount(share.total), style = ShareAmount, color = scheme.onSurface)
        }

        AnimatedVisibility(expanded) {
            Column(Modifier.fillMaxWidth().padding(start = 36.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                share.lines.forEach { line ->
                    BreakdownRow("${line.itemName} · ${shareLabel(line.sharedBetween)}", line.amount)
                }
                val charges = share.taxShare + share.serviceShare
                if (charges != 0L) BreakdownRow("Pajak & service", charges)
                if (share.discountShare != 0L) BreakdownRow("Diskon", -share.discountShare)
            }
        }
    }
}

private fun shareLabel(sharedBetween: Int) = when (sharedBetween) {
    1 -> "sendiri"
    2 -> "½ porsi"
    else -> "dibagi $sharedBetween"
}

@Composable
private fun BreakdownRow(label: String, amount: Long) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Leader()
        Text(
            formatAmount(amount),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, letterSpacing = 0.sp),
            color = scheme.onSurfaceVariant,
        )
    }
}

/** Proof the split ties out — or a plain statement of the gap when it doesn't. */
@Composable
private fun Reconciliation(split: SplitResult) {
    val currency = split.receipt.currency
    val text = if (split.matchesReceipt) {
        "Total ${formatMoney(split.total, currency)}, cocok sama struk"
    } else {
        val gap = formatMoney(kotlin.math.abs(split.discrepancy), currency)
        val direction = if (split.discrepancy > 0) "lebih" else "kurang"
        "Total ${formatMoney(split.total, currency)}, $gap $direction dari struk"
    }
    ReconLine(ok = split.matchesReceipt, text = AnnotatedString(text))
}
