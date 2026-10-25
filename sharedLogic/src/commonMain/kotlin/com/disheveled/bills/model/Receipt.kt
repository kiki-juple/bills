package com.disheveled.bills.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * A receipt after extraction and (possibly) manual correction by the user.
 *
 * Every amount is a whole number of minor currency units. For IDR that is rupiah —
 * the currency has no practical subunit, so 198_950L means Rp 198.950. Money is never
 * a Double here: rounding drift on a split bill is a bug nobody notices until someone
 * is short 500 rupiah.
 *
 * Nothing guarantees this is internally consistent: [items] may not add up to [subtotal],
 * and [total] may not equal subtotal + tax + service - discount, because a vision model
 * can misread a blurry line. [com.disheveled.bills.split.SplitResult] surfaces that
 * mismatch rather than hiding it.
 */
@Serializable
data class Receipt(
    val merchant: String,
    val currency: String,
    val items: List<ReceiptItem>,
    val subtotal: Long,
    val tax: Long,
    val serviceCharge: Long,
    val discount: Long,
    val total: Long,
    /** The date printed on the receipt; null when there is none or it couldn't be trusted. */
    val date: LocalDate? = null,
    /** What the model thinks the money went on; null for receipts typed in by hand. */
    val suggestedCategory: Category? = null,
) {
    /** What the line items actually add up to, regardless of what [subtotal] claims. */
    val itemsTotal: Long get() = items.sumOf { it.totalPrice }
}

/**
 * One line on the receipt.
 *
 * [id] is generated on our side, not read off the receipt — item assignment needs a
 * stable handle that survives the user renaming or reordering things.
 */
@Serializable
data class ReceiptItem(
    val id: String,
    val name: String,
    val qty: Int,
    val unitPrice: Long,
    val totalPrice: Long,
)
