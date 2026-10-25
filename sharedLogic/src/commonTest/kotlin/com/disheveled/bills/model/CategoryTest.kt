package com.disheveled.bills.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoryTest {

    @Test
    fun keysAreUniqueAndRoundTrip() {
        assertEquals(Category.entries.size, Category.entries.map { it.key }.toSet().size)
        Category.entries.forEach { assertEquals(it, Category.fromKey(it.key)) }
    }

    @Test
    fun unknownKeyIsNull() {
        assertNull(Category.fromKey("Makan"))
        assertNull(Category.fromKey("fashion"))
    }

    @Test
    fun lainnyaIsLast() {
        assertEquals(Category.Lainnya, Category.entries.last())
    }
}
