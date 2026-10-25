package com.disheveled.bills.spend

import com.disheveled.bills.model.Category
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoryGuesserTest {

    @Test
    fun commonTitlesLandInTheirCategory() {
        assertEquals(Category.Kopi, CategoryGuesser.guess("Kopi Kenangan"))
        assertEquals(Category.Transport, CategoryGuesser.guess("Gojek ke kantor"))
        assertEquals(Category.Tagihan, CategoryGuesser.guess("Token listrik"))
        assertEquals(Category.Belanja, CategoryGuesser.guess("Indomaret"))
        assertEquals(Category.Makan, CategoryGuesser.guess("Nasi Padang"))
        assertEquals(Category.Hiburan, CategoryGuesser.guess("Nonton XXI"))
        assertEquals(Category.Kesehatan, CategoryGuesser.guess("Apotek K24"))
    }

    @Test
    fun matchesWholeWordsOnly() {
        // "grabfood" is food, not a ride; "grab food" is too, because Makan wins the tie.
        assertEquals(Category.Makan, CategoryGuesser.guess("GrabFood"))
        assertEquals(Category.Makan, CategoryGuesser.guess("Grab Food"))
        assertEquals(Category.Transport, CategoryGuesser.guess("Grab ke stasiun"))
    }

    @Test
    fun phrasesOutweighTheirParts() {
        assertEquals(Category.Kopi, CategoryGuesser.guess("Warung kopi"))
        assertEquals(Category.Makan, CategoryGuesser.guess("Warung Bu Tini"))
    }

    @Test
    fun tiesGoToTheCategoryDeclaredFirst() {
        assertEquals(Category.Makan, CategoryGuesser.guess("Nasi goreng + es teh"))
        assertEquals(Category.Transport, CategoryGuesser.guess("Tiket kereta"))
    }

    @Test
    fun ignoresCaseAndPunctuation() {
        assertEquals(Category.Kopi, CategoryGuesser.guess("KOPI!!"))
        assertEquals(Category.Transport, CategoryGuesser.guess("e-toll"))
    }

    @Test
    fun noMatchLeavesTheChoiceToTheUser() {
        assertNull(CategoryGuesser.guess("Transfer ke Budi"))
        assertNull(CategoryGuesser.guess("   "))
    }
}
