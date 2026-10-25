package com.disheveled.bills.spend

import com.disheveled.bills.model.Category
import com.disheveled.bills.model.Expense
import com.disheveled.bills.model.ExpenseSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

private var nextId = 0

private fun entry(
    title: String,
    amount: Long,
    category: Category,
    date: LocalDate = LocalDate(2026, 10, 10),
    source: ExpenseSource = ExpenseSource.Manual,
) = Expense(
    id = "e${nextId++}",
    title = title,
    amount = amount,
    currency = "IDR",
    category = category,
    suggestedCategory = null,
    date = date,
    note = null,
    source = source,
    splitId = null,
    createdAtMillis = 0,
    updatedAtMillis = 0,
)

private val september = LocalDate(2026, 9, 12)

class MonthFactsTest {

    @Test
    fun namesMonthsInIndonesian() {
        assertEquals("Oktober 2026", YearMonth(2026, 10).indonesianName())
        assertEquals("Mei 2027", YearMonth(2027, 5).indonesianName())
    }

    @Test
    fun spellsOutTheMonthTheAppComputed() {
        val facts = MonthFacts.describe(
            listOf(
                entry("Kopi Kenangan", 100_000, Category.Kopi),
                entry("kopi kenangan ", 52_000, Category.Kopi),
                entry("Fore", 90_000, Category.Kopi),
                entry("Warung Sederhana", 51_175, Category.Makan, source = ExpenseSource.Split),
                entry("Warung Sederhana", 60_000, Category.Makan, date = september, source = ExpenseSource.Split),
                entry("XXI", 50_000, Category.Hiburan, date = september),
            ),
            YearMonth(2026, 10),
        )

        assertEquals(
            """
            Bulan: Oktober 2026
            Total bulan ini: Rp 293.175
            Total September 2026: Rp 110.000
            Total dibanding September 2026: naik 167% dari September 2026

            Per kategori:
            - Kopi & Jajan: Rp 242.000, 83% dari total bulan ini, September 2026 kosong, jadi belum bisa dibandingin; paling banyak di Kopi Kenangan (Rp 152.000)
            - Makan: Rp 51.175, 17% dari total bulan ini, turun 15% dari September 2026 (Rp 60.000); paling banyak di Warung Sederhana (Rp 51.175)

            Kosong bulan ini, padahal ada di September 2026:
            - Hiburan: Rp 50.000

            Bagian kamu dari split bareng orang lain: Rp 51.175 (September 2026: Rp 60.000)
            """.trimIndent(),
            facts,
        )
    }
}
