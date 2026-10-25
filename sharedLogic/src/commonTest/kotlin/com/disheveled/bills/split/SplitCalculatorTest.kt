package com.disheveled.bills.split

import com.disheveled.bills.model.Person
import com.disheveled.bills.model.Receipt
import com.disheveled.bills.model.ReceiptItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun item(id: String, name: String, qty: Int, total: Long) =
    ReceiptItem(id = id, name = name, qty = qty, unitPrice = total / qty, totalPrice = total)

/** Warung Sederhana: 173.000 + PB1 10% + service 5% = 198.950. */
private val warung = Receipt(
    merchant = "Warung Sederhana",
    currency = "IDR",
    items = listOf(
        item("i1", "Nasi Goreng Spesial", 2, 56_000),
        item("i2", "Ayam Bakar Madu", 1, 45_000),
        item("i3", "Es Teh Manis", 3, 18_000),
        item("i4", "Sate Ayam (10 tusuk)", 1, 42_000),
        item("i5", "Nasi Putih", 2, 12_000),
    ),
    subtotal = 173_000,
    tax = 17_300,
    serviceCharge = 8_650,
    discount = 0,
    total = 198_950,
)

private val four = listOf(
    Person("p1", "Kiki"),
    Person("p2", "Dimas"),
    Person("p3", "Rara"),
    Person("p4", "Bagas"),
)

class SplitCalculatorTest {

    @Test
    fun evenSplitLosesNothingWhenItDoesNotDivideCleanly() {
        val result = SplitCalculator.calculate(warung, four)

        assertEquals(198_950, result.total, "shares must add up to the bill")
        assertTrue(result.matchesReceipt)
        assertEquals(0, result.discrepancy)
        assertEquals(198_950, result.shares.sumOf { it.total })
    }

    @Test
    fun evenSplitSpreadsTheLeftoverRupiahRatherThanDroppingIt() {
        // 198.950 / 4 = 49.737,5 — two people carry an extra rupiah, nobody loses one.
        val totals = SplitCalculator.calculate(warung, four).shares.map { it.total }

        assertEquals(198_950, totals.sum())
        assertTrue(totals.max() - totals.min() <= 1, "shares differ by at most one rupiah: $totals")
    }

    @Test
    fun assignedItemsOnlyChargeThePeopleWhoAteThem() {
        // Only Kiki had the sate; everything else is shared four ways.
        val result = SplitCalculator.calculate(
            warung, four, assignments = mapOf("i4" to setOf("p1")),
        )

        val kiki = result.shares.first { it.person.id == "p1" }
        val dimas = result.shares.first { it.person.id == "p2" }

        // (173.000 - 42.000) / 4 = 32.750 shared, + 42.000 sate for Kiki.
        assertEquals(32_750 + 42_000, kiki.itemsSubtotal)
        assertEquals(32_750, dimas.itemsSubtotal)
        assertTrue(kiki.total > dimas.total)
        assertEquals(198_950, result.total, "assignment must not change the bill total")
    }

    @Test
    fun taxAndServiceFollowWhatEachPersonOrdered() {
        val result = SplitCalculator.calculate(
            warung, four, assignments = mapOf("i4" to setOf("p1")),
        )
        val kiki = result.shares.first { it.person.id == "p1" }
        val dimas = result.shares.first { it.person.id == "p2" }

        assertTrue(
            kiki.taxShare > dimas.taxShare,
            "the person who ordered more pays more tax: ${kiki.taxShare} vs ${dimas.taxShare}",
        )
        assertEquals(17_300, result.shares.sumOf { it.taxShare })
        assertEquals(8_650, result.shares.sumOf { it.serviceShare })
    }

    @Test
    fun discountIsSubtractedAndFullyDistributed() {
        val discounted = warung.copy(discount = 20_000, total = 178_950)
        val result = SplitCalculator.calculate(discounted, four)

        assertEquals(20_000, result.shares.sumOf { it.discountShare })
        assertEquals(178_950, result.total)
        assertTrue(result.matchesReceipt)
    }

    @Test
    fun oneItemSoloStillBalances() {
        val result = SplitCalculator.calculate(
            warung, four,
            assignments = mapOf(
                "i1" to setOf("p1"), "i2" to setOf("p2"),
                "i3" to setOf("p3"), "i4" to setOf("p4"), "i5" to setOf("p1", "p2"),
            ),
        )
        assertEquals(198_950, result.total)
        assertEquals(198_950, result.shares.sumOf { it.total })
    }

    @Test
    fun aReceiptThatDoesNotTieOutIsReportedNotHidden() {
        // Extraction misread the total — we must surface it, not silently "fix" it.
        val wrong = warung.copy(total = 200_000)
        val result = SplitCalculator.calculate(wrong, four)

        assertEquals(198_950, result.total, "we split what the items actually say")
        assertEquals(200_000, result.receiptTotal)
        assertTrue(!result.matchesReceipt)
        assertEquals(-1_050, result.discrepancy)
    }

    @Test
    fun splitBetweenOnePersonGivesThemEverything() {
        val result = SplitCalculator.calculate(warung, listOf(four[0]))
        assertEquals(198_950, result.shares.single().total)
    }

    @Test
    fun sharelinesRecordHowManyWaysEachItemWasSplit() {
        val result = SplitCalculator.calculate(
            warung, four, assignments = mapOf("i4" to setOf("p1", "p2")),
        )
        val sate = result.shares.first { it.person.id == "p1" }
            .lines.first { it.itemName == "Sate Ayam (10 tusuk)" }

        assertEquals(2, sate.sharedBetween)
        assertEquals(21_000, sate.amount)
    }
}

class AllocationTest {

    @Test
    fun splitEvenlyAlwaysSumsToTheInput() {
        for (amount in listOf(0L, 1L, 7L, 100L, 99_999L, 198_950L, 1_000_003L)) {
            for (n in 1..9) {
                assertEquals(amount, splitEvenly(amount, n).sum(), "amount=$amount n=$n")
            }
        }
    }

    @Test
    fun allocateAlwaysSumsToTheInput() {
        val weightSets = listOf(
            listOf(1L, 1L, 1L),
            listOf(0L, 0L, 0L),
            listOf(5L, 0L, 0L),
            listOf(56_000L, 45_000L, 18_000L, 42_000L),
            listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
        )
        for (weights in weightSets) {
            for (amount in listOf(0L, 1L, 2L, 17_300L, 8_650L, 999_999L)) {
                val parts = allocate(amount, weights)
                assertEquals(amount, parts.sum(), "amount=$amount weights=$weights")
                assertEquals(weights.size, parts.size)
                assertTrue(parts.all { it >= 0 }, "no negative shares: $parts")
            }
        }
    }

    @Test
    fun portionsAddBackUpToTheLineTotal() {
        val portions = SplitCalculator.portions(item("x", "Bebek Goreng", 2, 45_001))

        assertEquals(listOf(22_501L, 22_500L), portions.map { it.totalPrice })
        assertEquals(listOf(22_501L, 22_500L), portions.map { it.unitPrice })
        assertEquals(listOf(1, 1), portions.map { it.qty })
        assertEquals(listOf("x-1", "x-2"), portions.map { it.id })
        assertTrue(portions.all { it.name == "Bebek Goreng" })
    }

    @Test
    fun aSinglePortionIsLeftAsItIs() {
        val single = item("x", "Ayam Bakar Madu", 1, 45_000)
        assertEquals(listOf(single), SplitCalculator.portions(single))
    }

    @Test
    fun portionsOfOneDishCanGoToDifferentPeople() {
        // Two plates of nasi goreng between three: one person eats a whole plate, the other
        // two halve the second. Shared by all three it would be 18.667 each.
        val nasiGoreng = item("i1", "Nasi Goreng Spesial", 2, 56_000)
        val (whole, halved) = SplitCalculator.portions(nasiGoreng)
        val receipt = Receipt(
            merchant = "Warung Sederhana",
            currency = "IDR",
            items = listOf(whole, halved),
            subtotal = 56_000,
            tax = 0,
            serviceCharge = 0,
            discount = 0,
            total = 56_000,
        )
        val result = SplitCalculator.calculate(
            receipt,
            four.take(3),
            assignments = mapOf(whole.id to setOf("p1"), halved.id to setOf("p2", "p3")),
        )

        assertEquals(listOf(28_000L, 14_000L, 14_000L), result.shares.map { it.total })
        assertTrue(result.matchesReceipt)
    }

    @Test
    fun allocateIsProportionalAndDeterministic() {
        val parts = allocate(100L, listOf(50L, 30L, 20L))
        assertEquals(listOf(50L, 30L, 20L), parts)
        assertEquals(parts, allocate(100L, listOf(50L, 30L, 20L)), "same input, same output")
    }

    @Test
    fun allocateFallsBackToAnEvenSplitWhenEverythingIsFree() {
        assertEquals(listOf(4L, 3L, 3L), allocate(10L, listOf(0L, 0L, 0L)))
    }
}
