package com.disheveled.bills.spend

import com.disheveled.bills.model.Category
import com.disheveled.bills.model.Expense
import com.disheveled.bills.model.ExpenseSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

private val october = YearMonth(2026, 10)

private fun expense(
    amount: Long,
    category: Category,
    date: LocalDate = LocalDate(2026, 10, 15),
    createdAt: Long = 0,
    currency: String = "IDR",
    deletedAt: Long? = null,
) = Expense(
    id = "$category-$amount-$date-$createdAt",
    title = category.name,
    amount = amount,
    currency = currency,
    category = category,
    suggestedCategory = null,
    date = date,
    note = null,
    source = ExpenseSource.Manual,
    splitId = null,
    createdAtMillis = createdAt,
    updatedAtMillis = createdAt,
    deletedAtMillis = deletedAt,
)

/** October 2026 from the design canvas: Rp 2.674.900 across all eight categories. */
private val octoberSpending = listOf(
    expense(1_245_000, Category.Makan),
    expense(486_000, Category.Kopi),
    expense(412_500, Category.Transport),
    expense(298_000, Category.Belanja),
    expense(120_000, Category.Tagihan),
    expense(60_000, Category.Hiburan),
    expense(38_400, Category.Kesehatan),
    expense(15_000, Category.Lainnya),
)

class SpendCalculatorTest {

    @Test
    fun percentagesAddUpToExactly100() {
        // Rounding each share on its own gives 47+18+15+11+4+2+1+1 = 99.
        val report = SpendCalculator.monthlyReport(octoberSpending, october)

        assertEquals(2_674_900, report.total)
        assertEquals(listOf(47L, 18L, 15L, 11L, 5L, 2L, 1L, 1L), report.categories.map { it.percent })
        assertEquals(100, report.categories.sumOf { it.percent })
    }

    @Test
    fun categoriesAreLargestFirstAndSkipEmptyOnes() {
        val report = SpendCalculator.monthlyReport(
            listOf(expense(10_000, Category.Kopi), expense(30_000, Category.Makan)),
            october,
        )

        assertEquals(listOf(Category.Makan, Category.Kopi), report.categories.map { it.category })
    }

    @Test
    fun otherMonthsAndDeletedEntriesStayOutOfTheTotal() {
        val report = SpendCalculator.monthlyReport(
            listOf(
                expense(50_000, Category.Makan),
                expense(20_000, Category.Makan, deletedAt = 1),
                expense(70_000, Category.Makan, date = LocalDate(2026, 11, 1)),
            ),
            october,
        )

        assertEquals(50_000, report.total)
        assertEquals(1, report.days.single().expenses.size)
    }

    @Test
    fun monthTotalComparesWithThePreviousMonth() {
        val september = expense(2_611_000, Category.Makan, date = LocalDate(2026, 9, 20))
        val report = SpendCalculator.monthlyReport(octoberSpending + september, october)

        // 63.900 / 2.611.000 = 2,45%
        assertEquals(2_611_000, report.previousTotal)
        assertEquals(Change.Percent(2), report.change)
        assertEquals(Change.New, report.categories.first { it.category == Category.Kopi }.change)
    }

    @Test
    fun percentChangeRoundsHalfAwayFromZero() {
        assertEquals(Change.Percent(-5), changeBetween(previous = 100, current = 95))
        assertEquals(Change.Percent(33), changeBetween(previous = 3, current = 4))
        assertEquals(Change.Percent(-13), changeBetween(previous = 8, current = 7))
        assertEquals(Change.Percent(1), changeBetween(previous = 200, current = 201))
        assertEquals(Change.Percent(-1), changeBetween(previous = 200, current = 199))
        assertEquals(Change.New, changeBetween(previous = 0, current = 5))
        assertEquals(Change.Same, changeBetween(previous = 5, current = 5))
        assertEquals(Change.Same, changeBetween(previous = 0, current = 0))
    }

    @Test
    fun daysAreNewestFirstWithTheirOwnTotals() {
        val today = LocalDate(2026, 10, 24)
        val yesterday = LocalDate(2026, 10, 23)
        val report = SpendCalculator.monthlyReport(
            listOf(
                expense(86_250, Category.Makan, date = yesterday, createdAt = 1),
                expense(38_000, Category.Kopi, date = today, createdAt = 2),
                expense(67_400, Category.Belanja, date = yesterday, createdAt = 3),
                expense(24_500, Category.Transport, date = today, createdAt = 5),
            ),
            october,
        )

        assertEquals(listOf(today, yesterday), report.days.map { it.date })
        assertEquals(62_500, report.days[0].total)
        assertEquals(153_650, report.days[1].total)
        assertEquals(listOf(24_500L, 38_000L), report.days[0].expenses.map { it.amount })
    }

    @Test
    fun otherCurrenciesAreCountedNotAdded() {
        val report = SpendCalculator.monthlyReport(
            listOf(expense(50_000, Category.Makan), expense(12, Category.Makan, currency = "USD")),
            october,
        )

        assertEquals(50_000, report.total)
        assertEquals(1, report.otherCurrencyCount)
    }

    @Test
    fun emptyMonthHasNoCategoriesAndNoChange() {
        val report = SpendCalculator.monthlyReport(emptyList(), october)

        assertEquals(0, report.total)
        assertTrue(report.categories.isEmpty())
        assertTrue(report.days.isEmpty())
        assertEquals(Change.Same, report.change)
    }

    @Test
    fun topCategoriesFoldTheRestIntoLainnya() {
        val top = SpendCalculator.monthlyReport(octoberSpending, october).topCategories(4)

        assertEquals(
            listOf(Category.Makan, Category.Kopi, Category.Transport, Category.Belanja, Category.Lainnya),
            top.map { it.category },
        )
        // Tagihan + Hiburan + Kesehatan + Lainnya
        assertEquals(233_400, top.last().amount)
        assertEquals(2_674_900, top.sumOf { it.amount })
        assertEquals(100, top.sumOf { it.percent })
    }

    @Test
    fun lainnyaNeverTakesATopSlot() {
        val top = SpendCalculator.monthlyReport(
            listOf(
                expense(500_000, Category.Lainnya),
                expense(100_000, Category.Makan),
                expense(50_000, Category.Kopi),
            ),
            october,
        ).topCategories(1)

        assertEquals(listOf(Category.Makan, Category.Lainnya), top.map { it.category })
        assertEquals(550_000, top.last().amount)
    }
}
