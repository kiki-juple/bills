package com.disheveled.bills.ui

import com.disheveled.bills.model.Category
import com.disheveled.bills.model.Expense
import com.disheveled.bills.model.ExpenseSource
import com.disheveled.bills.model.Person
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

private val people = listOf(Person("p1", "Dimas"), Person("p2", "Kiki "), Person("p3", "Rara"))

private fun draft(source: ExpenseSource, original: Expense?) = ExpenseDraft(
    original = original,
    source = source,
    origin = null,
    amount = 51_175,
    currency = "IDR",
    title = "Warung Sederhana",
    category = Category.Makan,
    suggested = null,
    categoryPicked = true,
    guessed = false,
    date = LocalDate(2026, 9, 12),
    dateFromReceipt = false,
    note = "",
)

private val logged = Expense(
    id = "e1", title = "Warung", amount = 51_175, currency = "IDR", category = Category.Makan,
    suggestedCategory = null, date = LocalDate(2026, 9, 12), note = null, source = ExpenseSource.Scan,
    splitId = null, createdAtMillis = 0, updatedAtMillis = 0,
)

class ShareRulesTest {

    @Test
    fun theUserIsFoundByNameIgnoringCaseAndSpaces() {
        assertEquals("p2", BillsState(selfName = "kiki").selfAmong(people)?.id)
        assertEquals("p2", BillsState(selfName = " KIKI").selfAmong(people)?.id)
    }

    @Test
    fun noRememberedNameMeansAsking() {
        assertNull(BillsState(selfName = null).selfAmong(people))
        assertNull(BillsState(selfName = "  ").selfAmong(people))
        assertNull(BillsState(selfName = "Bagas").selfAmong(people))
    }

    @Test
    fun aSplitShareAlwaysFollowsTheSplit() {
        assertTrue(draft(ExpenseSource.Split, original = null).amountLocked)
        assertTrue(draft(ExpenseSource.Split, original = logged.copy(source = ExpenseSource.Split)).amountLocked)
    }

    @Test
    fun aScanIsLockedOnlyUntilItsReceiptIsGone() {
        // Fresh from review: fix it there. Already logged: there's no receipt left, so here.
        assertTrue(draft(ExpenseSource.Scan, original = null).amountLocked)
        assertFalse(draft(ExpenseSource.Scan, original = logged).amountLocked)
        assertFalse(draft(ExpenseSource.Manual, original = null).amountLocked)
    }
}
