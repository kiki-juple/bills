package com.disheveled.bills.ui

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.minus

private val shortMonths = MonthNames("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

/** "25 Okt 2026" — what `SimpleDateFormat("d MMM yyyy")` gives for id-ID, in common code. */
internal fun formatDate(date: LocalDate): String = dateFormat.format(date)

private val dateFormat = LocalDate.Format {
    day(padding = Padding.NONE)
    char(' ')
    monthName(shortMonths)
    char(' ')
    year()
}

/** A date field's value: "Hari ini, 24 Okt 2026", "Kemarin, 23 Okt 2026" or "12 Sep 2026". */
internal fun dateFieldLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Hari ini, ${formatDate(date)}"
    today.minus(1, DateTimeUnit.DAY) -> "Kemarin, ${formatDate(date)}"
    else -> formatDate(date)
}

/** A day header in the log, lowercase like every section label: "hari ini", "kemarin", "22 okt". */
internal fun dayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "hari ini"
    date == today.minus(1, DateTimeUnit.DAY) -> "kemarin"
    date.year == today.year -> "${date.day} ${shortMonths.names[date.month.ordinal].lowercase()}"
    else -> formatDate(date).lowercase()
}
