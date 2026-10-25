package com.disheveled.bills.split

import com.disheveled.bills.model.Person
import com.disheveled.bills.model.Receipt

/** One line of a person's bill: their cut of a single item. */
data class ShareLine(
    val itemName: String,
    /** This person's portion, not the item's full price. */
    val amount: Long,
    /** How many people split this item. 1 means they had it to themselves. */
    val sharedBetween: Int,
)

/** What one person owes, and why. */
data class PersonShare(
    val person: Person,
    val lines: List<ShareLine>,
    val itemsSubtotal: Long,
    val taxShare: Long,
    val serviceShare: Long,
    val discountShare: Long,
    val total: Long,
)

/**
 * The finished split.
 *
 * [total] is what the shares actually add up to, computed from the line items. [receiptTotal]
 * is what the receipt claims. They can disagree when extraction misread something, which is
 * why both are kept — the Summary screen shows the reconciliation instead of quietly
 * presenting numbers that don't tie out.
 */
data class SplitResult(
    val receipt: Receipt,
    val shares: List<PersonShare>,
    val total: Long,
    val receiptTotal: Long,
) {
    val matchesReceipt: Boolean get() = total == receiptTotal

    /** Signed gap, for showing "Rp 500 lebih" / "Rp 500 kurang" when it doesn't tie out. */
    val discrepancy: Long get() = total - receiptTotal
}
