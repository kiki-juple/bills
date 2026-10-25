package com.disheveled.bills.ui

import com.disheveled.bills.spend.Change
import com.disheveled.bills.spend.MonthlyReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

private fun report(total: Long, change: Change) = MonthlyReport(
    month = YearMonth(2026, 10),
    currency = "IDR",
    total = total,
    previousTotal = 0,
    change = change,
    categories = emptyList(),
    days = emptyList(),
    otherCurrencyCount = 0,
)

private val today = LocalDate(2026, 10, 24)

class SpendTextTest {

    @Test
    fun theChangeLineSaysWhichWayAndAgainstWhichMonth() {
        assertEquals("naik 2% dari september", changeLine(report(100, Change.Percent(2))))
        assertEquals("turun 8% dari september", changeLine(report(100, Change.Percent(-8))))
        assertEquals("hampir sama kayak september", changeLine(report(100, Change.Percent(0))))
        assertEquals("sama kayak september", changeLine(report(100, Change.Same)))
        assertEquals("bulan pertama", changeLine(report(100, Change.New)))
        assertEquals("belum ada catatan", changeLine(report(0, Change.Same)))
    }

    @Test
    fun categoryMovesAreTextNotColourAlone() {
        assertEquals("+40%", changeTag(Change.Percent(40)))
        assertEquals("−8%", changeTag(Change.Percent(-8)))
        assertEquals("sama", changeTag(Change.Same))
        assertEquals("baru", changeTag(Change.New))
    }

    @Test
    fun dayHeadersAreRelativeThenShort() {
        assertEquals("hari ini", dayLabel(today, today))
        assertEquals("kemarin", dayLabel(LocalDate(2026, 10, 23), today))
        assertEquals("2 okt", dayLabel(LocalDate(2026, 10, 2), today))
        assertEquals("30 des 2025", dayLabel(LocalDate(2025, 12, 30), today))
    }

    @Test
    fun dateFieldsSpellOutTodayAndYesterday() {
        assertEquals("Hari ini, 24 Okt 2026", dateFieldLabel(today, today))
        assertEquals("Kemarin, 23 Okt 2026", dateFieldLabel(LocalDate(2026, 10, 23), today))
        assertEquals("12 Sep 2026", dateFieldLabel(LocalDate(2026, 9, 12), today))
    }

    @Test
    fun monthLabelsAreLowercase() {
        assertEquals("oktober 2026", monthLabel(YearMonth(2026, 10)))
    }

    @Test
    fun rowAmountsDropTheCurrency() {
        assertEquals("1.245.000", formatAmount(1_245_000))
        assertEquals("500", formatAmount(500))
        assertEquals("Rp 198.950", formatMoney(198_950))
    }
}
