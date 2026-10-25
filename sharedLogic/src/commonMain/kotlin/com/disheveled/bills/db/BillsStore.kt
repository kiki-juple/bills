package com.disheveled.bills.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.db.SqlDriver
import com.disheveled.bills.model.Category
import com.disheveled.bills.model.Expense
import com.disheveled.bills.model.ExpenseSource
import com.disheveled.bills.model.Person
import com.disheveled.bills.model.Receipt
import com.disheveled.bills.model.SavedSplit
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Everything kept on the device: saved splits, the spending log and a few settings.
 *
 * The driver is handed in because building one needs a platform handle (a Context on
 * Android). Reads are Flows that emit again whenever their table changes, so a screen
 * showing them never has to refresh by hand.
 */
class BillsStore(
    driver: SqlDriver,
    private val io: CoroutineContext = Dispatchers.IO,
) {
    private val db = BillsDatabase(driver)

    // --- spending log -----------------------------------------------------------------

    /** Entries dated [from] through [to], newest first, without deleted ones. */
    fun expenses(from: LocalDate, to: LocalDate): Flow<List<Expense>> =
        db.expensesQueries.between(from.toString(), to.toString(), ::toExpense).asFlow().mapToList(io)

    /** False until the first entry is saved, and again once every entry is deleted. */
    fun hasExpenses(): Flow<Boolean> = db.expensesQueries.anyLive().asFlow().mapToOne(io)

    suspend fun expense(id: String): Expense? = withContext(io) {
        db.expensesQueries.byId(id, ::toExpense).executeAsOneOrNull()
    }

    /** Inserts, or replaces the entry with the same id. */
    suspend fun saveExpense(expense: Expense): Unit = withContext(io) {
        with(expense) {
            db.expensesQueries.upsert(
                id = id,
                title = title,
                amount = amount,
                currency = currency,
                category = category.key,
                suggested_category = suggestedCategory?.key,
                date = date.toString(),
                note = note,
                source = source.key,
                split_id = splitId,
                created_at = createdAtMillis,
                updated_at = updatedAtMillis,
                deleted_at = deletedAtMillis,
            )
        }
    }

    /** The user's shares of splits, keyed by split id; deleted ones drop out. */
    fun splitShares(): Flow<Map<String, Expense>> =
        db.expensesQueries.splitShares(::toExpense).asFlow().mapToList(io)
            .map { shares -> shares.associateBy { it.splitId!! } }

    suspend fun deleteExpense(id: String, atMillis: Long): Unit = withContext(io) {
        db.expensesQueries.markDeleted(at = atMillis, id = id)
    }

    // --- splits -----------------------------------------------------------------------

    /** Newest first, without deleted ones. */
    fun splits(): Flow<List<SavedSplit>> = db.splitsQueries.selectAll(::toSplit).asFlow().mapToList(io)

    suspend fun split(id: String): SavedSplit? = withContext(io) {
        db.splitsQueries.byId(id, ::toSplit).executeAsOneOrNull()
    }

    /** Inserts, or replaces the split with the same id. */
    suspend fun saveSplit(split: SavedSplit): Unit = withContext(io) {
        val inputs = SplitInputs(split.receipt, split.people, split.assignments, split.selfPersonId, split.category)
        db.splitsQueries.upsert(
            id = split.id,
            date = split.date.toString(),
            data_ = storageJson.encodeToString(inputs),
            created_at = split.createdAtMillis,
            updated_at = split.updatedAtMillis,
            deleted_at = null,
        )
    }

    // --- settings ---------------------------------------------------------------------

    /** The name the user goes by in splits, so they can be found among the people next time. */
    fun selfName(): Flow<String?> = db.settingsQueries.valueOf(SELF_NAME).asFlow().mapToOneOrNull(io)

    suspend fun setSelfName(name: String): Unit = withContext(io) {
        db.settingsQueries.put(SELF_NAME, name)
    }

    private companion object {
        const val SELF_NAME = "self_name"
    }
}

/**
 * The JSON kept in `splits.data`. Its field names are the stored format: renaming one here,
 * or in [Receipt], [com.disheveled.bills.model.ReceiptItem] or [Person], orphans every split
 * already saved. `BillsStoreTest` decodes a fixed document to catch exactly that.
 */
@Serializable
private data class SplitInputs(
    val receipt: Receipt,
    val people: List<Person>,
    val assignments: Map<String, Set<String>>,
    val selfPersonId: String?,
    // Added after the first saved splits; those decode with no category.
    val category: Category? = null,
)

// Unknown keys are skipped so a split saved by a newer build still opens in an older one.
private val storageJson = Json { ignoreUnknownKeys = true }

private val ExpenseSource.key: String
    get() = when (this) {
        ExpenseSource.Split -> "split"
        ExpenseSource.Scan -> "scan"
        ExpenseSource.Manual -> "manual"
    }

private fun toExpense(
    id: String,
    title: String,
    amount: Long,
    currency: String,
    category: String,
    suggestedCategory: String?,
    date: String,
    note: String?,
    source: String,
    splitId: String?,
    createdAt: Long,
    updatedAt: Long,
    deletedAt: Long?,
) = Expense(
    id = id,
    title = title,
    amount = amount,
    currency = currency,
    // Keys this build doesn't know can only come from a newer one; keep the entry visible.
    category = Category.fromKey(category) ?: Category.Lainnya,
    suggestedCategory = suggestedCategory?.let(Category::fromKey),
    date = LocalDate.parse(date),
    note = note,
    source = ExpenseSource.entries.firstOrNull { it.key == source } ?: ExpenseSource.Manual,
    splitId = splitId,
    createdAtMillis = createdAt,
    updatedAtMillis = updatedAt,
    deletedAtMillis = deletedAt,
)

private fun toSplit(
    id: String,
    date: String,
    data: String,
    createdAt: Long,
    updatedAt: Long,
    deletedAt: Long?,
): SavedSplit {
    val inputs = storageJson.decodeFromString<SplitInputs>(data)
    return SavedSplit(
        id = id,
        receipt = inputs.receipt,
        date = LocalDate.parse(date),
        people = inputs.people,
        assignments = inputs.assignments,
        selfPersonId = inputs.selfPersonId,
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt,
        category = inputs.category,
    )
}
