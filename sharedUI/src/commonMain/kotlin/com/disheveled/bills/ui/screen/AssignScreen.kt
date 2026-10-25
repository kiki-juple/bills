package com.disheveled.bills.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disheveled.bills.model.Person
import com.disheveled.bills.model.Receipt
import com.disheveled.bills.model.ReceiptItem
import com.disheveled.bills.split.SplitCalculator
import com.disheveled.bills.ui.NotaGlyph
import com.disheveled.bills.ui.Glyph
import com.disheveled.bills.ui.NotaSheet
import com.disheveled.bills.ui.PlainButton
import com.disheveled.bills.ui.Press
import com.disheveled.bills.ui.SecondaryButton
import com.disheveled.bills.ui.pressable
import com.disheveled.bills.ui.Avatar
import com.disheveled.bills.ui.AvatarStack
import com.disheveled.bills.ui.CheckDot
import com.disheveled.bills.ui.PrimaryButton
import com.disheveled.bills.ui.ScreenTopBar
import com.disheveled.bills.ui.bottomBarInsets
import com.disheveled.bills.ui.formatMoney
import com.disheveled.bills.ui.theme.MoneySmall

@Composable
internal fun AssignScreen(
    receipt: Receipt,
    people: List<Person>,
    assignments: Map<String, Set<String>>,
    onBack: () -> Unit,
    onToggle: (itemId: String, personId: String) -> Unit,
    onShareWithEveryone: (itemId: String) -> Unit,
    onSplitPortions: (itemId: String) -> Unit,
    onContinue: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var sheetItem by remember { mutableStateOf<ReceiptItem?>(null) }

    // Recomputed on every assignment change so the footer totals are never stale.
    val running = remember(receipt, people, assignments) {
        SplitCalculator.calculate(receipt, people, assignments)
    }

    fun sharersOf(item: ReceiptItem): List<Person> {
        val ids = assignments[item.id] ?: return people
        return people.filter { it.id in ids }.ifEmpty { people }
    }

    Column(Modifier.fillMaxSize().background(scheme.background)) {
        ScreenTopBar("Siapa makan apa", onBack)

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(contentType = "hint") {
                Text(
                    "Semua item default-nya dibagi rata. Tap barisnya kalau ada yang cuma dimakan sebagian orang.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(receipt.items, key = { it.id }, contentType = { "item" }) { item ->
                val sharers = sharersOf(item)
                AssignRow(
                    item = item,
                    currency = receipt.currency,
                    sharers = sharers,
                    everyone = sharers.size == people.size,
                    allPeople = people,
                    onClick = { sheetItem = item },
                )
            }
            item(contentType = "end") { Box(Modifier.height(4.dp)) }
        }

        Column(
            // Background before the inset so the raised footer runs to the bottom edge.
            Modifier.fillMaxWidth().background(scheme.surface).bottomBarInsets(),
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    running.shares.forEach { share ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Avatar(share.person, size = 30.dp)
                            Text(
                                formatMoney(share.total, receipt.currency).removePrefix("Rp "),
                                style = MoneySmall,
                                color = scheme.onSurface,
                            )
                            Text(
                                share.person.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                PrimaryButton("Lihat hasilnya", onClick = onContinue)
            }
        }
    }

    val openItem = sheetItem
    if (openItem != null) {
        NotaSheet(onDismiss = { sheetItem = null }) { close ->
            AssignSheet(
                item = openItem,
                currency = receipt.currency,
                people = people,
                selected = sharersOf(openItem).map { it.id }.toSet(),
                onToggle = { personId -> onToggle(openItem.id, personId) },
                onEveryone = { onShareWithEveryone(openItem.id) },
                // After the sheet is gone: the line it shows is about to stop existing.
                onSplitPortions = { close { onSplitPortions(openItem.id) } },
                onDone = { close {} },
            )
        }
    }
}

@Composable
private fun AssignRow(
    item: ReceiptItem,
    currency: String,
    sharers: List<Person>,
    everyone: Boolean,
    allPeople: List<Person>,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val perHead = if (sharers.isEmpty()) 0L else item.totalPrice / sharers.size
    val detail = when {
        sharers.size == 1 -> "${formatMoney(item.totalPrice, currency)} · ${sharers.first().name} sendiri"
        else -> "${formatMoney(item.totalPrice, currency)} · ${formatMoney(perHead, currency)} /orang"
    }

    Row(
        modifier = Modifier.pressable(onClick = onClick, press = Press.Large).fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "${item.qty}× ${item.name.ifBlank { "Item tanpa nama" }}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurface,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (everyone && allPeople.size > 1) {
                Box(
                    Modifier.height(20.dp).clip(RoundedCornerShape(2.dp))
                        .border(1.dp, scheme.outline, RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("semua", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }
            AvatarStack(sharers, size = 26.dp, max = 4)
            Glyph(NotaGlyph.ChevronRight, scheme.onSurfaceVariant, size = 20.dp)
        }
    }
}

@Composable
private fun AssignSheet(
    item: ReceiptItem,
    currency: String,
    people: List<Person>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onEveryone: () -> Unit,
    onSplitPortions: () -> Unit,
    onDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val perHead = if (selected.isEmpty()) 0L else item.totalPrice / selected.size

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "Siapa yang makan ${item.name.ifBlank { "item ini" }}?",
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onBackground,
            )
            Text(
                "${item.qty}× · ${formatMoney(item.totalPrice, currency)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
                color = scheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            people.forEach { person ->
                val isOn = person.id in selected
                Row(
                    modifier = Modifier.pressable(onClick = { onToggle(person.id) }, role = Role.Checkbox)
                        .fillMaxWidth().height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isOn) scheme.primaryContainer else scheme.surface)
                        .border(if (isOn) 2.dp else 1.dp, if (isOn) scheme.primary else scheme.outline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(person, size = 40.dp)
                    Text(
                        person.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (isOn) formatMoney(perHead, currency) else "nggak ikut",
                        style = if (isOn) MoneySmall else MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = scheme.onSurfaceVariant,
                    )
                    CheckDot(selected = isOn)
                }
            }
        }

        // "2× Nasi Goreng" where one person ate a whole plate and two others halved the
        // second: dividing the line evenly can't say that, its portions can.
        if (item.qty > 1) {
            PlainButton("Pecah per porsi", onClick = onSplitPortions, color = scheme.primary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Bagi rata semua", onClick = onEveryone, modifier = Modifier.weight(1f))
            PrimaryButton("Simpan", onClick = onDone, modifier = Modifier.weight(1f))
        }
    }
}

