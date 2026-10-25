package com.disheveled.bills.ai

import com.disheveled.bills.model.Category
import com.disheveled.bills.model.Receipt
import com.disheveled.bills.model.ReceiptItem
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Exactly what the model is asked to return.
 *
 * Kept separate from [Receipt] so the domain model isn't shaped by the API: this side
 * carries the API's snake_case and has no item ids, because ids are ours to mint.
 */
@Serializable
internal data class ReceiptDto(
    val merchant: String = "",
    val date: String = "",
    val currency: String = "IDR",
    val items: List<ReceiptItemDto> = emptyList(),
    val subtotal: Long = 0,
    val tax: Long = 0,
    @SerialName("service_charge") val serviceCharge: Long = 0,
    val discount: Long = 0,
    val total: Long = 0,
    val category: String = "",
) {
    fun toDomain(today: LocalDate): Receipt = Receipt(
        merchant = merchant.ifBlank { "Struk" },
        currency = currency.ifBlank { "IDR" },
        items = items.mapIndexed { index, dto -> dto.toDomain(index) },
        subtotal = subtotal,
        tax = tax,
        serviceCharge = serviceCharge,
        discount = discount,
        // A receipt whose total the model never found is worse than useless for the
        // reconciliation row, so fall back to what the parts add up to.
        total = if (total != 0L) total else subtotal + tax + serviceCharge - discount,
        // A misread date files the spend under a month nobody is looking at: "26" read as
        // 2016, or day and month swapped into the future. Dropping it falls back to today,
        // which the review screen shows, and most receipts are scanned the day they print.
        date = LocalDate.Formats.ISO.parseOrNull(date.trim())
            ?.takeIf { it in today.minus(1, DateTimeUnit.YEAR)..today },
        suggestedCategory = Category.fromKey(category),
    )
}

@Serializable
internal data class ReceiptItemDto(
    val name: String = "",
    val qty: Int = 1,
    @SerialName("unit_price") val unitPrice: Long = 0,
    @SerialName("total_price") val totalPrice: Long = 0,
) {
    fun toDomain(index: Int): ReceiptItem {
        val safeQty = if (qty > 0) qty else 1
        val resolvedTotal = if (totalPrice != 0L) totalPrice else unitPrice * safeQty
        return ReceiptItem(
            id = "item-$index",
            name = name.ifBlank { "Item ${index + 1}" },
            qty = safeQty,
            unitPrice = if (unitPrice != 0L) unitPrice else resolvedTotal / safeQty,
            totalPrice = resolvedTotal,
        )
    }
}
