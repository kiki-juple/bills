package com.disheveled.bills.ai

import com.disheveled.bills.model.Category
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val today = LocalDate(2026, 10, 25)

private fun decode(date: String = "", category: String = "") = json.decodeFromString<ReceiptDto>(
    """
    {"merchant":"Kopi Kenangan","date":"$date","currency":"IDR",
     "items":[{"name":"Kopi Kenangan Mantan","qty":1,"unit_price":24000,"total_price":24000}],
     "subtotal":24000,"tax":0,"service_charge":0,"discount":0,"total":24000,
     "category":"$category"}
    """,
).toDomain(today)

class ReceiptDtoTest {

    @Test
    fun readsDateAndCategory() {
        val receipt = decode(date = "2026-10-05", category = "kopi")

        assertEquals(LocalDate(2026, 10, 5), receipt.date)
        assertEquals(Category.Kopi, receipt.suggestedCategory)
    }

    @Test
    fun anUnreadableDateIsLeftEmpty() {
        assertNull(decode(date = "").date)
        assertNull(decode(date = "05/10/2026").date)
        assertNull(decode(date = "2026-13-01").date)
    }

    @Test
    fun onlyDatesFromThePastYearAreTrusted() {
        assertEquals(today, decode(date = "2026-10-25").date)
        assertEquals(LocalDate(2025, 10, 25), decode(date = "2025-10-25").date)
        assertNull(decode(date = "2025-10-24").date)
        // "26" misread as 2016, and "11/10/26" read month-first into the future.
        assertNull(decode(date = "2016-10-11").date)
        assertNull(decode(date = "2026-11-10").date)
    }

    @Test
    fun anUnknownCategoryIsNoSuggestion() {
        assertNull(decode(category = "").suggestedCategory)
        assertNull(decode(category = "fashion").suggestedCategory)
    }

    @Test
    fun aReplyWithoutTheNewFieldsStillDecodes() {
        val receipt = json.decodeFromString<ReceiptDto>("""{"merchant":"Warung","total":10000}""")
            .toDomain(today)

        assertNull(receipt.date)
        assertNull(receipt.suggestedCategory)
    }

    @Test
    fun schemaIsStrictAllTheWayDown() {
        assertStrict(receiptSchema)
    }

    @Test
    fun schemaOffersExactlyTheCategoryKeys() {
        val offered = receiptSchema["properties"]!!.jsonObject["category"]!!.jsonObject["enum"]!!
            .jsonArray.map { it.jsonPrimitive.content }

        assertEquals(Category.entries.map { it.key }, offered)
    }

    /** `strict: true` rejects any object that allows extra keys or leaves one optional. */
    private fun assertStrict(node: JsonElement, path: String = "$") {
        if (node !is JsonObject) return
        node["properties"]?.jsonObject?.let { properties ->
            assertEquals(false, node["additionalProperties"]?.jsonPrimitive?.boolean, "$path allows extra keys")
            val required = node["required"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
            assertEquals(properties.keys, required.toSet(), "$path must require every property")
            properties.forEach { (name, child) -> assertStrict(child, "$path.$name") }
        }
        node["items"]?.let { assertStrict(it, "$path[]") }
    }
}
