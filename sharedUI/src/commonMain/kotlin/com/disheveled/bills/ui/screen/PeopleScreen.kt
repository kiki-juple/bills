package com.disheveled.bills.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disheveled.bills.model.Person
import com.disheveled.bills.model.Receipt
import com.disheveled.bills.ui.NotaGlyph
import com.disheveled.bills.ui.Glyph
import com.disheveled.bills.ui.pressable
import com.disheveled.bills.ui.Avatar
import com.disheveled.bills.ui.PrimaryButton
import com.disheveled.bills.ui.ScreenTopBar
import com.disheveled.bills.ui.bottomBarInsets
import com.disheveled.bills.ui.formatMoney

private const val MAX_PEOPLE = 12

@Composable
internal fun PeopleScreen(
    receipt: Receipt,
    people: List<Person>,
    suggestions: List<Person>,
    canContinue: Boolean,
    onBack: () -> Unit,
    onAddPerson: (String) -> Unit,
    onRemovePerson: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var draft by remember { mutableStateOf("") }
    val atCapacity = people.size >= MAX_PEOPLE

    fun commit() {
        if (draft.isNotBlank() && !atCapacity) {
            onAddPerson(draft)
            draft = ""
        }
    }

    Column(Modifier.fillMaxSize().background(scheme.background)) {
        ScreenTopBar(
            title = receipt.merchant.ifBlank { "Struk" },
            subtitle = "${formatMoney(receipt.total, receipt.currency)} · ${receipt.items.size} item",
            onBack = onBack,
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outline))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Siapa aja yang ikut?",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp, lineHeight = 30.sp),
                    color = scheme.onBackground,
                )
                Text(
                    "Nanti tinggal colek siapa makan apa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            if (people.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    people.forEach { person ->
                        PersonChip(person) { onRemovePerson(person.id) }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(scheme.surface)
                        .border(2.dp, scheme.primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape)
                            .background(scheme.surfaceVariant)
                            .border(1.dp, scheme.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (draft.isNotBlank()) {
                            Text(
                                draft.trim().take(1).uppercase(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        InlineField(
                            value = draft,
                            onValueChange = { draft = it },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            placeholder = "Nama temen",
                        )
                    }
                    Box(
                        modifier = Modifier.pressable(onClick = { commit() }, enabled = draft.isNotBlank() && !atCapacity)
                            .height(36.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (draft.isBlank() || atCapacity) scheme.surfaceVariant else scheme.primary)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Tambah",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            color = if (draft.isBlank() || atCapacity) scheme.onSurfaceVariant else scheme.onPrimary,
                        )
                    }
                }
                Text(
                    text = if (atCapacity) "Udah $MAX_PEOPLE orang, itu maksimalnya."
                    else "Ketik nama, terus tekan Tambah. Bisa sampai $MAX_PEOPLE orang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            if (suggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("sering bareng", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        suggestions.forEach { person ->
                            SuggestionChip(person, enabled = !atCapacity) { onAddPerson(person.name) }
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().bottomBarInsets()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))
            Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryButton("Lanjut, pilih orangnya", enabled = canContinue, onClick = onContinue)
            }
        }
    }
}

@Composable
private fun PersonChip(person: Person, onRemove: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.height(40.dp).clip(RoundedCornerShape(8.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outline, RoundedCornerShape(8.dp))
            .padding(start = 6.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Avatar(person, size = 32.dp)
        Text(
            person.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurface,
        )
        Box(
            Modifier.pressable(onClick = onRemove, onClickLabel = "Hapus ${person.name}").size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(NotaGlyph.Close, scheme.onSurfaceVariant, size = 16.dp)
        }
    }
}

@Composable
private fun SuggestionChip(person: Person, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.pressable(onClick = onClick, enabled = enabled).height(40.dp).clip(RoundedCornerShape(8.dp))
            .border(1.dp, scheme.outline, RoundedCornerShape(8.dp))
            .padding(start = 6.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Avatar(person, size = 28.dp)
        Text(
            person.name,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
            color = scheme.onSurfaceVariant,
        )
    }
}
