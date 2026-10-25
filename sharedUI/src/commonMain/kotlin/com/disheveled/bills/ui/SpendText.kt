package com.disheveled.bills.ui

import com.disheveled.bills.model.ExpenseSource
import com.disheveled.bills.spend.Change
import com.disheveled.bills.spend.MonthlyReport
import com.disheveled.bills.spend.indonesianName
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth

/** "oktober 2026" — section labels are always lowercase. */
internal fun monthLabel(month: YearMonth): String = month.indonesianName().lowercase()

/** The line under a month's total: "naik 2% dari september", "bulan pertama". */
internal fun changeLine(report: MonthlyReport): String {
    if (report.total == 0L) return "belum ada catatan"
    val previous = report.month.minusMonth().month.indonesianName().lowercase()
    return when (val change = report.change) {
        Change.New -> "bulan pertama"
        Change.Same -> "sama kayak $previous"
        is Change.Percent -> when {
            change.value > 0 -> "naik ${change.value}% dari $previous"
            change.value < 0 -> "turun ${-change.value}% dari $previous"
            else -> "hampir sama kayak $previous"
        }
    }
}

/** A category's move against last month, always as text: "+40%", "−8%", "sama", "baru". */
internal fun changeTag(change: Change): String = when (change) {
    Change.New -> "baru"
    Change.Same -> "sama"
    is Change.Percent -> when {
        change.value > 0 -> "+${change.value}%"
        // A real minus sign, so it lines up with the plus in the mono column.
        change.value < 0 -> "−${-change.value}%"
        else -> "sama"
    }
}

internal val ExpenseSource.tag: String
    get() = when (this) {
        ExpenseSource.Split -> "split"
        ExpenseSource.Scan -> "scan"
        ExpenseSource.Manual -> "manual"
    }
