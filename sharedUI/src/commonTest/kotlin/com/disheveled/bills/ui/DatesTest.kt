package com.disheveled.bills.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class DatesTest {

    @Test
    fun matchesTheIndonesianShortFormat() {
        assertEquals("25 Okt 2026", formatDate(LocalDate(2026, 10, 25)))
        assertEquals("5 Agu 2026", formatDate(LocalDate(2026, 8, 5)))
        assertEquals("1 Mei 2027", formatDate(LocalDate(2027, 5, 1)))
    }
}
