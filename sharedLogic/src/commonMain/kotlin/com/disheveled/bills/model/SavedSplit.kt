package com.disheveled.bills.model

import kotlinx.datetime.LocalDate

/**
 * A finished split as it is kept: the inputs, not the result. Running them through
 * [com.disheveled.bills.split.SplitCalculator] again gives the same shares, so there is one
 * source of truth for the numbers.
 *
 * [assignments] has the same meaning as during the split: item id -> ids of the people
 * sharing it, with a missing entry meaning everyone.
 */
data class SavedSplit(
    val id: String,
    val receipt: Receipt,
    val date: LocalDate,
    val people: List<Person>,
    val assignments: Map<String, Set<String>>,
    /** Which of [people] is the user, or null when the user wasn't part of this split. */
    val selfPersonId: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /** What the bill was for, as picked on the review screen; the user's share is filed under it. */
    val category: Category? = null,
)
