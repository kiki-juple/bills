package com.disheveled.bills.ai

import com.disheveled.bills.model.Person
import com.disheveled.bills.model.Receipt
import com.disheveled.bills.model.ReceiptItem
import com.disheveled.bills.split.SplitCalculator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Warung Sederhana split evenly four ways: 49.738, 49.738, 49.737, 49.737. */
private val evenSplit = SplitCalculator.calculate(
    Receipt(
        merchant = "Warung Sederhana",
        currency = "IDR",
        items = listOf(
            ReceiptItem("i1", "Nasi Goreng Spesial", 2, 28_000, 56_000),
            ReceiptItem("i2", "Ayam Bakar Madu", 1, 45_000, 45_000),
            ReceiptItem("i3", "Es Teh Manis", 3, 6_000, 18_000),
            ReceiptItem("i4", "Sate Ayam (10 tusuk)", 1, 42_000, 42_000),
            ReceiptItem("i5", "Nasi Putih", 2, 6_000, 12_000),
        ),
        subtotal = 173_000,
        tax = 17_300,
        serviceCharge = 8_650,
        discount = 0,
        total = 198_950,
    ),
    listOf(Person("p1", "Kiki"), Person("p2", "Dimas"), Person("p3", "Rara"), Person("p4", "Bagas")),
)

class NoteGroundingTest {

    @Test
    fun aNoteQuotingTheFactsIsGrounded() {
        val facts = "Total bulan ini: Rp 486.000\nKopi & Jajan: naik 40%\nBulan: Oktober 2026"

        assertTrue(noteIsGrounded("Jajan kopi kamu **naik 40%**, total Rp 486.000 di Oktober 2026.", facts))
        assertTrue(noteIsGrounded("Bulan ini adem-adem aja.", facts))
    }

    @Test
    fun aNoteWithANumberOfItsOwnIsNot() {
        val facts = "Total bulan ini: Rp 486.000\nKopi & Jajan: naik 40%"

        // 486.000 / 30 days — arithmetic the app never did.
        assertFalse(noteIsGrounded("Rata-rata Rp 16.200 sehari.", facts))
        assertFalse(noteIsGrounded("Kopi naik 41%.", facts))
    }

    @Test
    fun theSplitRecapCanQuoteSharesAndItems() {
        val facts = splitFacts(evenSplit)

        assertTrue(noteIsGrounded("Kiki sama Dimas bayar Rp 49.738, Rara sama Bagas Rp 49.737.", facts))
        assertTrue(noteIsGrounded("Sate Ayam (10 tusuk) dibagi rata berempat.", facts))
    }

    @Test
    fun theSplitRecapCannotInventTheRoundingGap() {
        // What the model actually wrote for this split: the gap is one rupiah, not a thousand.
        assertFalse(
            noteIsGrounded(
                "Kiki, Dimas, Rara, dan Bagas membayar hampir sama, perbedaan kecil Rp 1.000 hanya karena pembulatan.",
                splitFacts(evenSplit),
            ),
        )
    }
}
