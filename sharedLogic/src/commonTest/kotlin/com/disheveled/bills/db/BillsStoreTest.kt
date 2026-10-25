package com.disheveled.bills.db

import app.cash.sqldelight.db.QueryResult
import com.disheveled.bills.model.Category
import com.disheveled.bills.model.Expense
import com.disheveled.bills.model.ExpenseSource
import com.disheveled.bills.model.Person
import com.disheveled.bills.model.Receipt
import com.disheveled.bills.model.ReceiptItem
import com.disheveled.bills.model.SavedSplit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

private fun expense(
    id: String,
    date: LocalDate = LocalDate(2026, 10, 24),
    createdAt: Long = 1,
    category: Category = Category.Kopi,
) = Expense(
    id = id,
    title = "Fore Coffee",
    amount = 38_000,
    currency = "IDR",
    category = category,
    suggestedCategory = Category.Kopi,
    date = date,
    note = "sama Dimas",
    source = ExpenseSource.Scan,
    splitId = null,
    createdAtMillis = createdAt,
    updatedAtMillis = createdAt,
)

private val warung = Receipt(
    merchant = "Warung Sederhana",
    currency = "IDR",
    items = listOf(
        ReceiptItem("item-0", "Nasi Goreng", qty = 2, unitPrice = 28_000, totalPrice = 56_000),
        ReceiptItem("item-1", "Es Teh Manis", qty = 3, unitPrice = 6_000, totalPrice = 18_000),
    ),
    subtotal = 74_000,
    tax = 7_400,
    serviceCharge = 0,
    discount = 0,
    total = 81_400,
    date = LocalDate(2026, 10, 5),
    suggestedCategory = Category.Makan,
)

private fun split(id: String, date: LocalDate = LocalDate(2026, 10, 5), createdAt: Long = 1) = SavedSplit(
    id = id,
    receipt = warung,
    date = date,
    people = listOf(Person("p1", "Kiki"), Person("p2", "Dimas")),
    assignments = mapOf("item-0" to setOf("p1")),
    selfPersonId = "p1",
    createdAtMillis = createdAt,
    updatedAtMillis = createdAt,
)

private fun categorized(split: SavedSplit) = split.copy(category = Category.Makan)

class BillsStoreTest {

    private val driver = testDriver()
    private val store = BillsStore(driver)
    private val raw = BillsDatabase(driver)

    @Test
    fun anExpenseComesBackExactlyAsSaved() = runBlocking {
        val saved = expense("e1")
        store.saveExpense(saved)

        assertEquals(saved, store.expense("e1"))
        assertEquals(listOf(saved), store.expenses(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31)).first())
    }

    @Test
    fun expensesAreFilteredByDateAndNewestFirst() = runBlocking {
        store.saveExpense(expense("sept", date = LocalDate(2026, 9, 30)))
        store.saveExpense(expense("first", date = LocalDate(2026, 10, 1)))
        store.saveExpense(expense("last-morning", date = LocalDate(2026, 10, 31), createdAt = 1))
        store.saveExpense(expense("last-evening", date = LocalDate(2026, 10, 31), createdAt = 2))
        store.saveExpense(expense("nov", date = LocalDate(2026, 11, 1)))

        val october = store.expenses(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31)).first()

        assertEquals(listOf("last-evening", "last-morning", "first"), october.map { it.id })
    }

    @Test
    fun savingTheSameIdReplacesTheEntry() = runBlocking {
        store.saveExpense(expense("e1", category = Category.Kopi))
        store.saveExpense(expense("e1", category = Category.Makan))

        val all = store.expenses(LocalDate(2026, 1, 1), LocalDate(2026, 12, 31)).first()
        assertEquals(listOf(Category.Makan), all.map { it.category })
    }

    @Test
    fun deletedExpensesDisappearButStayInTheTable() = runBlocking {
        store.saveExpense(expense("e1"))
        store.deleteExpense("e1", atMillis = 99)

        assertNull(store.expense("e1"))
        assertTrue(store.expenses(LocalDate(2026, 1, 1), LocalDate(2026, 12, 31)).first().isEmpty())
        // Kept for sync: the row is marked, not removed.
        val deletedAt = driver.executeQuery(
            identifier = null,
            sql = "SELECT deleted_at FROM expenses WHERE id = 'e1'",
            mapper = { cursor -> cursor.next(); QueryResult.Value(cursor.getLong(0)) },
            parameters = 0,
        ).value
        assertEquals(99, deletedAt)
    }

    @Test
    fun hasExpensesFollowsTheLiveEntries() = runBlocking {
        assertEquals(false, store.hasExpenses().first())

        store.saveExpense(expense("e1"))
        assertEquals(true, store.hasExpenses().first())

        store.deleteExpense("e1", atMillis = 2)
        assertEquals(false, store.hasExpenses().first())
    }

    @Test
    fun anUnknownCategoryKeyStillShowsTheEntry() = runBlocking {
        raw.expensesQueries.upsert(
            "e1", "Baju", 150_000, "IDR", "fashion", null, "2026-10-24", null, "manual", null, 1, 1, null,
        )

        assertEquals(Category.Lainnya, store.expense("e1")?.category)
    }

    @Test
    fun aSplitComesBackExactlyAsSaved() = runBlocking {
        val saved = categorized(split("s1"))
        store.saveSplit(saved)

        assertEquals(saved, store.split("s1"))
        assertEquals(listOf(saved), store.splits().first())
    }

    @Test
    fun categoriesAreStoredByKeyNotByName() = runBlocking {
        store.saveSplit(split("s1"))

        val data = raw.splitsQueries.byId("s1").executeAsOne().data_
        assertTrue("\"suggestedCategory\":\"makan\"" in data, data)
    }

    @Test
    fun resavingASplitUpdatesItInsteadOfAddingAnother() = runBlocking {
        store.saveSplit(split("s1"))
        store.saveSplit(split("s1").copy(people = listOf(Person("p1", "Kiki")), updatedAtMillis = 2))

        val all = store.splits().first()
        assertEquals(1, all.size)
        assertEquals(listOf("Kiki"), all.single().people.map { it.name })
    }

    @Test
    fun splitsAreNewestFirst() = runBlocking {
        store.saveSplit(split("old", date = LocalDate(2026, 10, 1)))
        store.saveSplit(split("new-later", date = LocalDate(2026, 10, 5), createdAt = 2))
        store.saveSplit(split("new-earlier", date = LocalDate(2026, 10, 5), createdAt = 1))

        assertEquals(listOf("new-later", "new-earlier", "old"), store.splits().first().map { it.id })
    }

    @Test
    fun aSplitSavedByThisVersionStillOpens() = runBlocking {
        // The stored format, frozen. If this fails, a field in Receipt, ReceiptItem or Person
        // was renamed and every split already on someone's phone would stop loading.
        raw.splitsQueries.upsert(
            id = "s1",
            date = "2026-10-05",
            data_ = """{"receipt":{"merchant":"Warung Sederhana","currency":"IDR","items":[""" +
                """{"id":"item-0","name":"Nasi Goreng","qty":2,"unitPrice":28000,"totalPrice":56000},""" +
                """{"id":"item-1","name":"Es Teh Manis","qty":3,"unitPrice":6000,"totalPrice":18000}],""" +
                """"subtotal":74000,"tax":7400,"serviceCharge":0,"discount":0,"total":81400,""" +
                """"date":"2026-10-05","suggestedCategory":"makan"},""" +
                """"people":[{"id":"p1","name":"Kiki"},{"id":"p2","name":"Dimas"}],""" +
                """"assignments":{"item-0":["p1"]},"selfPersonId":"p1"}""",
            created_at = 1,
            updated_at = 1,
            deleted_at = null,
        )

        assertEquals(split("s1"), store.split("s1"))
    }

    @Test
    fun splitSharesAreKeyedBySplitAndDropDeletedOnes() = runBlocking {
        store.saveExpense(expense("mine").copy(source = ExpenseSource.Split, splitId = "s1"))
        store.saveExpense(expense("other").copy(source = ExpenseSource.Split, splitId = "s2"))
        store.saveExpense(expense("manual"))
        store.deleteExpense("other", atMillis = 5)

        assertEquals(mapOf("s1" to "mine"), store.splitShares().first().mapValues { it.value.id })
    }

    @Test
    fun selfNameIsRememberedAndOverwritten() = runBlocking {
        assertNull(store.selfName().first())

        store.setSelfName("Kiki")
        store.setSelfName("Kiki Abdullah")

        assertEquals("Kiki Abdullah", store.selfName().first())
    }

    @Test
    fun readsEmitAgainWhenTheTableChanges() = runBlocking {
        val seen = Channel<List<SavedSplit>>(Channel.UNLIMITED)
        val watcher = launch { store.splits().collect { seen.send(it) } }

        assertTrue(seen.receive().isEmpty())
        store.saveSplit(split("s1"))
        assertEquals(listOf("s1"), seen.receive().map { it.id })

        watcher.cancel()
    }
}
