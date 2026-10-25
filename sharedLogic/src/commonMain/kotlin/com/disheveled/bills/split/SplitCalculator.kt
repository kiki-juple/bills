package com.disheveled.bills.split

import com.disheveled.bills.model.Person
import com.disheveled.bills.model.Receipt
import com.disheveled.bills.model.ReceiptItem

/**
 * Turns a receipt plus "who ate what" into what each person owes.
 *
 * Deliberately pure and deliberately not done by the model: an LLM that drops 500 rupiah
 * produces an answer nobody can check. Every division here uses largest-remainder so the
 * shares add up to the input exactly, with no rupiah created or destroyed.
 */
object SplitCalculator {

    /**
     * @param assignments item id -> ids of the people sharing it. An item with no entry
     *   (or an entry naming nobody who is still in [people]) is split across everyone.
     */
    fun calculate(
        receipt: Receipt,
        people: List<Person>,
        assignments: Map<String, Set<String>> = emptyMap(),
    ): SplitResult {
        require(people.isNotEmpty()) { "Need at least one person to split between" }

        val order = people.map { it.id }
        val lines = order.associateWith { mutableListOf<ShareLine>() }
        val subtotals = order.associateWith { 0L }.toMutableMap()

        for (item in receipt.items) {
            val named = assignments[item.id].orEmpty()
            // Keep `order`'s sequence rather than the Set's so the leftover rupiah always
            // lands on the same person for the same input.
            val sharers = order.filter { it in named }.ifEmpty { order }

            val parts = splitEvenly(item.totalPrice, sharers.size)
            sharers.forEachIndexed { i, id ->
                subtotals[id] = subtotals.getValue(id) + parts[i]
                lines.getValue(id) += ShareLine(item.name, parts[i], sharers.size)
            }
        }

        // Tax, service and discount follow what each person actually ordered.
        val weights = order.map { subtotals.getValue(it) }
        val taxParts = allocate(receipt.tax, weights)
        val serviceParts = allocate(receipt.serviceCharge, weights)
        val discountParts = allocate(receipt.discount, weights)

        val shares = people.mapIndexed { i, person ->
            val subtotal = subtotals.getValue(person.id)
            PersonShare(
                person = person,
                lines = lines.getValue(person.id).toList(),
                itemsSubtotal = subtotal,
                taxShare = taxParts[i],
                serviceShare = serviceParts[i],
                discountShare = discountParts[i],
                total = subtotal + taxParts[i] + serviceParts[i] - discountParts[i],
            )
        }

        return SplitResult(
            receipt = receipt,
            shares = shares,
            total = shares.sumOf { it.total },
            receiptTotal = receipt.total,
        )
    }

    /**
     * One 1× line per portion of [item], for when its portions went to different people:
     * "2× Nasi Goreng 56.000" becomes two "1× Nasi Goreng 28.000" lines that can each be
     * assigned on their own. Priced with [splitEvenly], so an odd total still adds back up to
     * the rupiah. Ids are the original's with "-1", "-2", … appended; a single portion comes
     * back unchanged.
     */
    fun portions(item: ReceiptItem): List<ReceiptItem> {
        if (item.qty <= 1) return listOf(item)
        return splitEvenly(item.totalPrice, item.qty).mapIndexed { i, price ->
            item.copy(id = "${item.id}-${i + 1}", qty = 1, unitPrice = price, totalPrice = price)
        }
    }
}

/**
 * Split [amount] into [n] parts that sum to exactly [amount].
 *
 * 100.000 across 3 people is 33.333,33 each; flooring everyone loses a rupiah. The first
 * `amount % n` people absorb one extra unit instead.
 */
internal fun splitEvenly(amount: Long, n: Int): List<Long> {
    if (n <= 0) return emptyList()
    val base = amount / n
    val remainder = (amount - base * n).toInt()
    return List(n) { base + if (it < remainder) 1L else 0L }
}

/**
 * Split [amount] proportionally to [weights], summing to exactly [amount].
 *
 * Largest-remainder: floor every share, then hand the leftover units to whoever was
 * truncated hardest. Ties go to the lower index, so the same input always gives the
 * same answer. When every weight is zero (a fully discounted bill, say) there is nothing
 * to be proportional to, so it falls back to an even split.
 */
internal fun allocate(amount: Long, weights: List<Long>): List<Long> {
    if (weights.isEmpty()) return emptyList()
    if (amount == 0L) return List(weights.size) { 0L }

    val totalWeight = weights.sum()
    if (totalWeight <= 0L) return splitEvenly(amount, weights.size)

    val shares = LongArray(weights.size)
    var distributed = 0L
    for (i in weights.indices) {
        shares[i] = amount * weights[i] / totalWeight
        distributed += shares[i]
    }

    var leftover = amount - distributed
    val byRemainder = weights.indices.sortedWith(
        compareByDescending<Int> { amount * weights[it] % totalWeight }.thenBy { it }
    )
    var k = 0
    while (leftover > 0 && k < byRemainder.size) {
        shares[byRemainder[k]]++
        leftover--
        k++
    }
    return shares.toList()
}
