package com.disheveled.bills.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disheveled.bills.ai.AiProvider
import com.disheveled.bills.ai.ReceiptScanner
import com.disheveled.bills.db.BillsStore
import com.disheveled.bills.model.Category
import com.disheveled.bills.model.Expense
import com.disheveled.bills.model.ExpenseSource
import com.disheveled.bills.model.Receipt
import com.disheveled.bills.model.SavedSplit
import com.disheveled.bills.spend.CategoryGuesser
import com.disheveled.bills.spend.MonthFacts
import com.disheveled.bills.spend.MonthlyReport
import com.disheveled.bills.spend.SpendCalculator
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth

/** Where a draft came from, shown as a card at the top of the editor. */
internal sealed interface DraftOrigin {
    /** A scanned receipt the user paid alone. */
    data class Receipt(val merchant: String, val itemCount: Int) : DraftOrigin

    /** The user's share of a saved split; the card opens it. */
    data class Split(val splitId: String, val label: String) : DraftOrigin
}

/** What the expense editor is holding before it's saved. */
internal data class ExpenseDraft(
    /** The entry being edited, or null for a new one. */
    val original: Expense?,
    val source: ExpenseSource,
    val origin: DraftOrigin?,
    val amount: Long,
    val currency: String,
    val title: String,
    val category: Category,
    /** The model's pick for a scanned receipt; its chip keeps the AI label. */
    val suggested: Category?,
    /** Set once the person taps a chip, so typing the title stops re-guessing over them. */
    val categoryPicked: Boolean,
    /** [category] came from the title keyword guess, which the editor says under the chips. */
    val guessed: Boolean,
    val date: LocalDate,
    /** The date was read off the receipt, which the field's label says. */
    val dateFromReceipt: Boolean,
    val note: String,
) {
    val canSave: Boolean get() = amount > 0 && title.isNotBlank()

    /**
     * A split share follows the split; a fresh scan follows the receipt, which is fixed on the
     * review screen one step back. A scan already logged has no receipt left to fix it in, so
     * its amount opens up again.
     */
    val amountLocked: Boolean
        get() = source == ExpenseSource.Split || (source == ExpenseSource.Scan && original == null)
}

internal sealed interface RecapNote {
    data object Hidden : RecapNote
    data object Loading : RecapNote

    /** Figures the note highlights are wrapped in `**`. */
    data class Ready(val text: String) : RecapNote
}

internal data class SpendState(
    val today: LocalDate,
    val month: YearMonth,
    val report: MonthlyReport? = null,
    /** Null until known, so the first-use page never flashes for someone who has entries. */
    val hasExpenses: Boolean? = null,
    val draft: ExpenseDraft? = null,
    val note: RecapNote = RecapNote.Hidden,
) {
    val canGoNext: Boolean get() = month < today.yearMonth
}

/** The Pengeluaran tab: the month on screen, the expense editor and the monthly recap. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SpendViewModel(
    aiProviders: List<AiProvider>,
    private val store: BillsStore,
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : ViewModel() {

    private val scannerHolder = lazy { ReceiptScanner(aiProviders) }
    private val scanner by scannerHolder

    private val month = MutableStateFlow(today().yearMonth)

    private val _state = MutableStateFlow(SpendState(today = today(), month = month.value))
    val state: StateFlow<SpendState> = _state.asStateFlow()

    /** The entries behind [SpendState.report], which the recap note's facts are built from. */
    private var entries: List<Expense> = emptyList()

    /** Notes already written, by the facts they were written from, so revisiting costs nothing. */
    private val notes = mutableMapOf<String, String>()
    private var noteJob: Job? = null

    init {
        // The month before comes along because the report compares against it.
        month
            .flatMapLatest { m -> store.expenses(m.minusMonth().firstDay, m.lastDay).map { m to it } }
            .onEach { (m, list) ->
                entries = list
                _state.update {
                    it.copy(today = today(), month = m, report = SpendCalculator.monthlyReport(list, m))
                }
            }
            .launchIn(viewModelScope)

        store.hasExpenses()
            .onEach { has -> _state.update { it.copy(hasExpenses = has) } }
            .launchIn(viewModelScope)
    }

    fun previousMonth() = month.update { it.minusMonth() }

    fun nextMonth() = month.update { if (it < today().yearMonth) it.plusMonth() else it }

    // --- editor -----------------------------------------------------------------------

    fun startNew() = _state.update {
        it.copy(
            draft = ExpenseDraft(
                original = null,
                source = ExpenseSource.Manual,
                origin = null,
                amount = 0,
                currency = "IDR",
                title = "",
                category = Category.Lainnya,
                suggested = null,
                categoryPicked = false,
                guessed = false,
                date = today(),
                dateFromReceipt = false,
                note = "",
            ),
        )
    }

    /** "Aku bayar sendiri": the whole reviewed receipt, logged as the user's own spend. */
    fun startFromReceipt(receipt: Receipt, category: Category) = _state.update {
        it.copy(
            draft = ExpenseDraft(
                original = null,
                source = ExpenseSource.Scan,
                origin = DraftOrigin.Receipt(receipt.merchant.ifBlank { "Struk" }, receipt.items.size),
                amount = receipt.total,
                currency = receipt.currency,
                title = receipt.merchant.ifBlank { "Struk" },
                category = category,
                suggested = receipt.suggestedCategory,
                categoryPicked = true,
                guessed = false,
                date = receipt.date ?: today(),
                dateFromReceipt = receipt.date != null,
                note = "",
            ),
        )
    }

    /** [split] is the one a split share belongs to, for the card that leads back to it. */
    fun edit(expense: Expense, split: SavedSplit? = null) = _state.update {
        val origin = expense.splitId?.let { id ->
            DraftOrigin.Split(
                splitId = id,
                label = split?.let { s -> "${s.receipt.merchant.ifBlank { "Struk" }} · ${formatDate(s.date)}" }
                    ?: "${expense.title} · ${formatDate(expense.date)}",
            )
        }
        it.copy(
            draft = ExpenseDraft(
                original = expense,
                source = expense.source,
                origin = origin,
                amount = expense.amount,
                currency = expense.currency,
                title = expense.title,
                category = expense.category,
                suggested = expense.suggestedCategory,
                categoryPicked = true,
                guessed = false,
                date = expense.date,
                dateFromReceipt = false,
                note = expense.note.orEmpty(),
            ),
        )
    }

    fun setAmount(amount: Long) = updateDraft { if (it.amountLocked) it else it.copy(amount = amount) }

    fun setTitle(title: String) = updateDraft { draft ->
        if (draft.categoryPicked) return@updateDraft draft.copy(title = title)
        val guess = CategoryGuesser.guess(title)
        draft.copy(title = title, category = guess ?: Category.Lainnya, guessed = guess != null)
    }

    fun pickCategory(category: Category) = updateDraft {
        it.copy(category = category, categoryPicked = true, guessed = false)
    }

    fun setDate(date: LocalDate) = updateDraft { it.copy(date = date) }

    fun setNote(note: String) = updateDraft { it.copy(note = note) }

    /** Saves and shows the month the entry landed in, so it doesn't vanish into another one. */
    fun saveDraft() {
        val draft = _state.value.draft?.takeIf { it.canSave } ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        val note = draft.note.trim().ifEmpty { null }
        val expense = draft.original?.let { original ->
            original.copy(
                title = draft.title.trim(),
                amount = if (draft.amountLocked) original.amount else draft.amount,
                category = draft.category,
                date = draft.date,
                note = note,
                updatedAtMillis = now,
            )
        } ?: Expense(
            id = Uuid.random().toString(),
            title = draft.title.trim(),
            amount = draft.amount,
            currency = draft.currency,
            category = draft.category,
            suggestedCategory = draft.suggested,
            date = draft.date,
            note = note,
            source = draft.source,
            splitId = null,
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        _state.update { it.copy(draft = null) }
        month.value = draft.date.yearMonth
        viewModelScope.launch { store.saveExpense(expense) }
    }

    fun deleteDraft() {
        val original = _state.value.draft?.original ?: return
        _state.update { it.copy(draft = null) }
        viewModelScope.launch {
            store.deleteExpense(original.id, atMillis = Clock.System.now().toEpochMilliseconds())
        }
    }

    private fun updateDraft(transform: (ExpenseDraft) -> ExpenseDraft) =
        _state.update { state -> state.copy(draft = state.draft?.let(transform)) }

    // --- recap ------------------------------------------------------------------------

    /** Asks for the month's AI note unless it's already written for exactly these numbers. */
    fun loadRecapNote() {
        val report = _state.value.report
        if (report == null || report.total == 0L) {
            noteJob?.cancel()
            _state.update { it.copy(note = RecapNote.Hidden) }
            return
        }
        val facts = MonthFacts.describe(entries, report.month)
        notes[facts]?.let { text ->
            _state.update { it.copy(note = RecapNote.Ready(text)) }
            return
        }

        noteJob?.cancel()
        _state.update { it.copy(note = RecapNote.Loading) }
        noteJob = viewModelScope.launch {
            // A failure isn't remembered, so the next visit tries again.
            val text = scanner.monthNote(facts)
            if (text != null) notes[facts] = text
            _state.update { it.copy(note = text?.let(RecapNote::Ready) ?: RecapNote.Hidden) }
        }
    }

    override fun onCleared() {
        if (scannerHolder.isInitialized()) scanner.close()
    }
}
