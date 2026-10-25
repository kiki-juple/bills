package com.disheveled.bills.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disheveled.bills.model.Category
import com.disheveled.bills.ui.NotaGlyph
import com.disheveled.bills.ui.Glyph
import com.disheveled.bills.ui.AiLabel
import com.disheveled.bills.ui.DashedDivider
import com.disheveled.bills.ui.Press
import com.disheveled.bills.ui.SurfaceCard
import com.disheveled.bills.ui.formatAmount
import com.disheveled.bills.ui.pressable
import com.disheveled.bills.ui.theme.MoneyMedium
import com.disheveled.bills.ui.CategoryButton
import com.disheveled.bills.ui.CategorySheet
import com.disheveled.bills.ui.SecondaryButton
import com.disheveled.bills.ui.Tab
import com.disheveled.bills.ui.formatDate
import kotlinx.datetime.LocalDate
import com.disheveled.bills.model.Receipt
import com.disheveled.bills.model.ReceiptItem
import com.disheveled.bills.ui.PrimaryButton
import com.disheveled.bills.ui.ScreenTopBar
import com.disheveled.bills.ui.bottomBarInsets
import com.disheveled.bills.ui.formatMoney
import com.disheveled.bills.ui.theme.MoneyLarge


@Composable
internal fun ReviewScreen(
    receipt: Receipt,
    date: LocalDate,
    category: Category,
    origin: Tab,
    canContinue: Boolean,
    showExtractionWarning: Boolean,
    onBack: () -> Unit,
    onPickCategory: (Category) -> Unit,
    onPaidAlone: () -> Unit,
    onMerchantChange: (String) -> Unit,
    onItemChange: (id: String, name: String, qty: Int, total: Long) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onChargesChange: (tax: Long, service: Long, discount: Long) -> Unit,
    onContinue: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var editingId by remember { mutableStateOf<String?>(null) }
    var editingMerchant by remember { mutableStateOf(false) }
    var editingCharges by remember { mutableStateOf(false) }
    var pickingCategory by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(scheme.background)) {
        ScreenTopBar("Cek struk", onBack)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (editingMerchant) {
                        InlineField(
                            value = receipt.merchant,
                            onValueChange = onMerchantChange,
                            textStyle = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp, lineHeight = 28.sp),
                            placeholder = "Nama tempat",
                        )
                    } else {
                        Text(
                            text = receipt.merchant.ifBlank { "Struk tanpa nama" },
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp, lineHeight = 28.sp),
                            color = scheme.onBackground,
                        )
                    }
                    Text(
                        formatDate(date).lowercase() + " · " +
                            if (showExtractionWarning) "${receipt.items.size} item kebaca" else "${receipt.items.size} item",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = scheme.onSurfaceVariant,
                    )
                }
                Pill(
                    label = if (editingMerchant) "Selesai" else "Ubah",
                    onClick = { editingMerchant = !editingMerchant },
                )
            }

            CategoryButton(category, receipt.suggestedCategory, onClick = { pickingCategory = true })

            if (showExtractionWarning) Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    .background(scheme.surfaceVariant).padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AiLabel()
                Text(
                    "Cek lagi ya, AI kadang salah baca",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = scheme.onSurface,
                )
            }

            SurfaceCard(contentPadding = PaddingValues(start = 2.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)) {
              Column {
                receipt.items.forEachIndexed { index, item ->
                    if (index > 0) DashedDivider(Modifier.padding(horizontal = 14.dp))
                    ItemRow(
                        item = item,
                        currency = receipt.currency,
                        editing = editingId == item.id,
                        onToggleEdit = { editingId = if (editingId == item.id) null else item.id },
                        onChange = { name, qty, total -> onItemChange(item.id, name, qty, total) },
                        onRemove = {
                            editingId = null
                            onRemoveItem(item.id)
                        },
                    )
                }

                DashedDivider(Modifier.padding(horizontal = 14.dp))
                Row(
                    modifier = Modifier.pressable(onClick = {
                        onAddItem()
                        editingId = null
                    }).fillMaxWidth().height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Glyph(NotaGlyph.Plus, scheme.primary)
                    Text(
                        "Tambah item",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                    )
                }
              }
            }

            TotalsCard(
                receipt = receipt,
                editing = editingCharges,
                onToggleEdit = { editingCharges = !editingCharges },
                onChargesChange = onChargesChange,
            )
        }

        Column(Modifier.fillMaxWidth().bottomBarInsets()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outline))
            // The way in decides which is the main action: a scan from Pengeluaran is most
            // likely the user's own spend, one from Split most likely a shared bill.
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (origin == Tab.Spend) {
                    PrimaryButton(label = "Aku bayar sendiri", enabled = canContinue, onClick = onPaidAlone)
                    SecondaryButton(label = "Split bareng temen", onClick = { if (canContinue) onContinue() })
                } else {
                    PrimaryButton(label = "Lanjut, pilih orangnya", enabled = canContinue, onClick = onContinue)
                    SecondaryButton(label = "Aku bayar sendiri", onClick = { if (canContinue) onPaidAlone() })
                }
            }
        }
    }

    if (pickingCategory) {
        CategorySheet(
            selected = category,
            suggested = receipt.suggestedCategory,
            explanation = if (receipt.suggestedCategory != null) {
                "AI nebak dari isinya: ${receipt.items.take(3).joinToString(", ") { it.name.lowercase() }}. Ganti kalau kurang pas."
            } else {
                "Pilih yang paling pas, biar masuk ke kategori yang bener di Pengeluaran."
            },
            onApply = onPickCategory,
            onDismiss = { pickingCategory = false },
        )
    }
}

@Composable
private fun ItemRow(
    item: ReceiptItem,
    currency: String,
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onChange: (name: String, qty: Int, total: Long) -> Unit,
    onRemove: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.pressable(onClick = onToggleEdit, press = Press.Row).fillMaxWidth()
            .background(if (editing) scheme.surfaceVariant else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(width = 38.dp, height = 32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (editing) scheme.surface else Color.Transparent)
                .then(if (editing) Modifier.border(1.dp, scheme.outline, RoundedCornerShape(8.dp)) else Modifier),
            contentAlignment = if (editing) Alignment.Center else Alignment.CenterStart,
        ) {
            if (editing) {
                NumberField(
                    value = item.qty.toLong(),
                    onValueChange = { onChange(item.name, it.toInt().coerceAtLeast(1), item.totalPrice) },
                    textStyle = qtyStyle(scheme.onSurface),
                    modifier = Modifier.widthIn(min = 24.dp),
                    align = TextAlign.Center,
                )
            } else {
                Text("${item.qty}×", style = qtyStyle(scheme.onSurfaceVariant))
            }
        }

        if (editing) {
            InlineField(
                value = item.name,
                onValueChange = { onChange(it, item.qty, item.totalPrice) },
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = "Nama item",
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(
                text = item.name.ifBlank { "Item tanpa nama" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.name.isBlank()) scheme.onSurfaceVariant else scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }

        if (editing) {
            Box(
                modifier = Modifier.height(38.dp).widthIn(min = 104.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surface)
                    .border(2.dp, scheme.primary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                NumberField(
                    value = item.totalPrice,
                    onValueChange = { onChange(item.name, item.qty, it) },
                    textStyle = moneyStyle(scheme.onSurface),
                    align = TextAlign.End,
                )
            }
            Box(
                Modifier.pressable(onClick = onRemove, onClickLabel = "Hapus item").size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Glyph(NotaGlyph.Close, scheme.onSurfaceVariant, size = 20.dp)
            }
        } else {
            Text(formatAmount(item.totalPrice), style = moneyStyle(scheme.onSurface))
        }
    }
}

@Composable
private fun TotalsCard(
    receipt: Receipt,
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onChargesChange: (Long, Long, Long) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    SurfaceCard {
      Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        ChargeRow("Subtotal", receipt.subtotal, receipt.currency, editable = false, onChange = {})
        ChargeRow("Pajak", receipt.tax, receipt.currency, editing) {
            onChargesChange(it, receipt.serviceCharge, receipt.discount)
        }
        ChargeRow("Service", receipt.serviceCharge, receipt.currency, editing) {
            onChargesChange(receipt.tax, it, receipt.discount)
        }
        if (editing || receipt.discount != 0L) {
            ChargeRow("Diskon", receipt.discount, receipt.currency, editing) {
                onChargesChange(receipt.tax, receipt.serviceCharge, it)
            }
        }

        DashedDivider(Modifier.padding(vertical = 3.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("Total", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(
                formatMoney(receipt.total, receipt.currency),
                style = MoneyMedium,
                color = scheme.onSurface,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                if (editing) "Selesai ubah" else "Pajak/service salah?",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = scheme.primary,
                modifier = Modifier.pressable(onClick = onToggleEdit).padding(4.dp),
            )
        }
      }
    }
}

@Composable
private fun ChargeRow(
    label: String,
    amount: Long,
    currency: String,
    editable: Boolean,
    onChange: (Long) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = scheme.onSurfaceVariant)
        if (editable) {
            Box(
                modifier = Modifier.height(32.dp).widthIn(min = 96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, scheme.primary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                NumberField(
                    value = amount,
                    onValueChange = onChange,
                    textStyle = moneyStyle(scheme.onSurface).copy(fontSize = 15.sp),
                    align = TextAlign.End,
                )
            }
        } else {
            Text(formatAmount(amount), style = moneyStyle(scheme.onSurfaceVariant).copy(fontSize = 15.sp))
        }
    }
}

@Composable
private fun Pill(label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.pressable(onClick = onClick).height(36.dp).clip(RoundedCornerShape(8.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.onSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
            color = scheme.onSurface,
        )
    }
}

@Composable
private fun qtyStyle(color: Color) = MaterialTheme.typography.labelMedium.copy(
    fontSize = 14.sp, letterSpacing = 0.sp, color = color,
)

@Composable
private fun moneyStyle(color: Color) = MaterialTheme.typography.labelMedium.copy(
    fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp, color = color,
)

/** Text field styled as the canvas's dashed-underline inline edit. */
@Composable
internal fun InlineField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier) {
        if (value.isEmpty()) {
            Text(placeholder, style = textStyle, color = scheme.onSurfaceVariant)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.primary),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Digits-only field bound to a Long.
 *
 * Rupiah has no subunit, so anything that isn't a digit is simply dropped rather than
 * fought with — separators and "Rp" typed by habit just disappear.
 */
@Composable
internal fun NumberField(
    value: Long,
    onValueChange: (Long) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
) {
    val scheme = MaterialTheme.colorScheme
    BasicTextField(
        value = if (value == 0L) "" else value.toString(),
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(12)
            onValueChange(digits.toLongOrNull() ?: 0L)
        },
        textStyle = textStyle.merge(LocalTextStyle.current.copy(textAlign = align)).copy(
            color = scheme.onSurface, textAlign = align,
        ),
        cursorBrush = SolidColor(scheme.primary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
