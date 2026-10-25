package com.disheveled.bills.ui

/**
 * Formats a minor-unit amount the way Indonesian receipts do: "Rp 198.950".
 *
 * Written by hand because `java.text.NumberFormat` doesn't exist in common code, and the
 * one format this app shows isn't worth a date/number library.
 */
fun formatMoney(amount: Long, currency: String = "IDR"): String {
    val sign = if (amount < 0) "-" else ""
    val prefix = if (currency.equals("IDR", ignoreCase = true)) "Rp" else currency
    return "$sign$prefix ${formatAmount(if (amount < 0) -amount else amount)}"
}

/** Just the grouped digits, "198.950", for rows where the currency is understood. */
fun formatAmount(amount: Long): String {
    val sign = if (amount < 0) "-" else ""
    val digits = if (amount < 0) (-amount).toString() else amount.toString()
    return sign + digits.reversed().chunked(3).joinToString(".").reversed()
}
