package com.disheveled.bills.model

import kotlinx.datetime.LocalDate

/** Where an expense came from. Decides what the editor lets the user change. */
enum class ExpenseSource {
    /** The user's share of a split. The amount follows the split and can't be edited here. */
    Split,

    /** A scanned receipt the user paid alone. The amount is the receipt total. */
    Scan,

    /** Typed in by hand. */
    Manual,
}

/**
 * One entry in the personal spending log.
 *
 * [amount] is a snapshot taken when the entry was saved, not something recomputed from the
 * split: if the split maths ever changes, last month's log must not quietly change with it.
 *
 * Timestamps are epoch milliseconds. [deletedAtMillis] marks a deletion instead of removing
 * the entry, so a later sync can tell "deleted" apart from "never existed".
 */
data class Expense(
    val id: String,
    val title: String,
    val amount: Long,
    val currency: String,
    val category: Category,
    /**
     * What the model suggested when the receipt was scanned, null for anything it didn't see.
     * Kept separately so the AI label stays on the suggested category after the user picks
     * another one.
     */
    val suggestedCategory: Category?,
    val date: LocalDate,
    val note: String?,
    val source: ExpenseSource,
    /** The split this is the user's share of; only set when [source] is [ExpenseSource.Split]. */
    val splitId: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val deletedAtMillis: Long? = null,
) {
    val isDeleted: Boolean get() = deletedAtMillis != null
}
