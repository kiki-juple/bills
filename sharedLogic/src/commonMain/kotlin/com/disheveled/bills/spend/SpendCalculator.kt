package com.disheveled.bills.spend

import com.disheveled.bills.model.Category
import com.disheveled.bills.model.Expense
import com.disheveled.bills.split.allocate
import kotlin.math.abs
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.yearMonth

/** How an amount moved compared with the month before. */
sealed interface Change {
    /** Nothing to compare with: the previous month had none of it. */
    data object New : Change

    data object Same : Change

    /** Signed whole percent, rounded half away from zero. */
    data class Percent(val value: Long) : Change
}

data class CategoryTotal(
    val category: Category,
    val amount: Long,
    val previousAmount: Long,
    /** Share of the month in whole percent. A month's categories always add up to exactly 100. */
    val percent: Long,
    val change: Change,
)

data class DayGroup(
    val date: LocalDate,
    val total: Long,
    /** Newest first. */
    val expenses: List<Expense>,
)

data class MonthlyReport(
    val month: YearMonth,
    val currency: String,
    val total: Long,
    val previousTotal: Long,
    val change: Change,
    /** Largest first; categories with nothing spent are left out. */
    val categories: List<CategoryTotal>,
    /** Newest day first. */
    val days: List<DayGroup>,
    /** Entries this month in another currency, left out instead of being added to these totals. */
    val otherCurrencyCount: Int,
) {
    /**
     * The [limit] biggest categories, then everything else folded into one [Category.Lainnya]
     * row. Lainnya itself never takes one of the top slots, so it can't show up twice.
     */
    fun topCategories(limit: Int): List<CategoryTotal> {
        val (named, others) = categories.partition { it.category != Category.Lainnya }
        val top = named.take(limit)
        val rest = named.drop(limit) + others
        if (rest.isEmpty()) return top

        val amount = rest.sumOf { it.amount }
        val previousAmount = rest.sumOf { it.previousAmount }
        return top + CategoryTotal(
            category = Category.Lainnya,
            amount = amount,
            previousAmount = previousAmount,
            percent = rest.sumOf { it.percent },
            change = changeBetween(previousAmount, amount),
        )
    }
}

/**
 * Totals for the spending log. Like [com.disheveled.bills.split.SplitCalculator] this is
 * plain arithmetic on whole rupiah; the monthly AI note is written from these numbers and
 * never computes its own.
 */
object SpendCalculator {

    fun monthlyReport(
        expenses: List<Expense>,
        month: YearMonth,
        currency: String = "IDR",
    ): MonthlyReport {
        val live = expenses.filterNot { it.isDeleted }
        val inMonth = live.filter { it.date.yearMonth == month }
        val current = inMonth.filter { it.currency == currency }
        val previous = live.filter { it.date.yearMonth == month.minusMonth() && it.currency == currency }

        val amounts = current.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
        val previousAmounts = previous.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }

        // Ties keep declaration order, so the same month always lists the same way.
        val ranked = amounts.entries
            .filter { it.value > 0 }
            .sortedWith(compareByDescending<Map.Entry<Category, Long>> { it.value }.thenBy { it.key.ordinal })
        // Rounding each share on its own can total 99 or 101; allocating 100 can't.
        val percents = allocate(100, ranked.map { it.value })

        val categories = ranked.mapIndexed { i, (category, amount) ->
            val before = previousAmounts[category] ?: 0L
            CategoryTotal(category, amount, before, percents[i], changeBetween(before, amount))
        }

        val days = current
            .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAtMillis })
            .groupBy { it.date }
            .map { (date, list) -> DayGroup(date, list.sumOf { it.amount }, list) }

        val total = current.sumOf { it.amount }
        val previousTotal = previous.sumOf { it.amount }
        return MonthlyReport(
            month = month,
            currency = currency,
            total = total,
            previousTotal = previousTotal,
            change = changeBetween(previousTotal, total),
            categories = categories,
            days = days,
            otherCurrencyCount = inMonth.size - current.size,
        )
    }
}

internal fun changeBetween(previous: Long, current: Long): Change = when {
    previous == 0L -> if (current == 0L) Change.Same else Change.New
    current == previous -> Change.Same
    else -> Change.Percent(roundedPercent(current - previous, previous))
}

/** [part] / [whole] as a whole percent, rounded half away from zero, without leaving Long. */
private fun roundedPercent(part: Long, whole: Long): Long {
    val scaled = part * 100
    val quotient = scaled / whole
    val remainder = scaled % whole
    if (2 * abs(remainder) < whole) return quotient
    return if (scaled < 0) quotient - 1 else quotient + 1
}
