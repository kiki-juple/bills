package com.disheveled.bills.spend

import com.disheveled.bills.model.Expense
import com.disheveled.bills.model.ExpenseSource
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.yearMonth

private val monthNames = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember",
)

/** "Oktober". */
fun Month.indonesianName(): String = monthNames[ordinal]

/** "Oktober 2026". */
fun YearMonth.indonesianName(): String = "${month.indonesianName()} $year"

/**
 * Everything the monthly AI note is allowed to talk about, as text for the model.
 *
 * Every number in here was computed by [SpendCalculator]; the model only picks what to say
 * about them. Amounts are already written the way the note should write them ("Rp 38.000"),
 * because left to format bare integers the model copied "38000" as is. A month with nothing
 * to compare against says so in words: told "baru", the model reported the amount as a rise.
 */
object MonthFacts {

    /** Facts for [month], from entries covering at least that month and the one before. */
    fun describe(expenses: List<Expense>, month: YearMonth, currency: String = "IDR"): String {
        val report = SpendCalculator.monthlyReport(expenses, month, currency)
        val previousName = month.minusMonth().indonesianName()
        val live = expenses.filter { !it.isDeleted && it.currency == currency }
        val current = live.filter { it.date.yearMonth == month }
        val previous = live.filter { it.date.yearMonth == month.minusMonth() }

        return buildString {
            appendLine("Bulan: ${month.indonesianName()}")
            appendLine("Total bulan ini: ${rupiah(report.total)}")
            appendLine("Total $previousName: ${rupiah(report.previousTotal)}")
            appendLine("Total dibanding $previousName: ${describe(report.change, previousName)}")
            appendLine()
            appendLine("Per kategori:")
            for (category in report.categories) {
                append("- ${category.category.label}: ${rupiah(category.amount)}, ${category.percent}% dari total bulan ini, ")
                append(describe(category.change, previousName))
                if (category.previousAmount > 0) append(" (${rupiah(category.previousAmount)})")
                topPlace(current.filter { it.category == category.category })?.let { (title, amount) ->
                    append("; paling banyak di $title (${rupiah(amount)})")
                }
                appendLine()
            }

            val gone = previous.groupBy { it.category }
                .filterKeys { category -> report.categories.none { it.category == category } }
            if (gone.isNotEmpty()) {
                appendLine()
                appendLine("Kosong bulan ini, padahal ada di $previousName:")
                gone.entries.sortedBy { it.key.ordinal }.forEach { (category, list) ->
                    appendLine("- ${category.label}: ${rupiah(list.sumOf { it.amount })}")
                }
            }

            appendLine()
            appendLine(
                "Bagian kamu dari split bareng orang lain: ${rupiah(current.splitTotal())} " +
                    "($previousName: ${rupiah(previous.splitTotal())})",
            )
        }.trimEnd()
    }

    private fun describe(change: Change, previousName: String): String = when (change) {
        Change.New -> "$previousName kosong, jadi belum bisa dibandingin"
        Change.Same -> "sama dengan $previousName"
        is Change.Percent -> when {
            change.value > 0 -> "naik ${change.value}% dari $previousName"
            change.value < 0 -> "turun ${-change.value}% dari $previousName"
            else -> "hampir sama dengan $previousName"
        }
    }

    /** Where most of a category's money went, by title, ignoring case. */
    private fun topPlace(expenses: List<Expense>): Pair<String, Long>? = expenses
        .groupBy { it.title.trim().lowercase() }
        .map { (_, list) -> list.first().title.trim() to list.sumOf { it.amount } }
        .maxByOrNull { it.second }

    private fun List<Expense>.splitTotal(): Long =
        filter { it.source == ExpenseSource.Split }.sumOf { it.amount }

    // The app's formatMoney lives in the UI module; this is the same "Rp 1.245.000".
    private fun rupiah(amount: Long): String =
        "Rp " + amount.toString().reversed().chunked(3).joinToString(".").reversed()
}
