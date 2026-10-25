package com.disheveled.bills.spend

import com.disheveled.bills.model.Category

/**
 * Guesses a category from what the user typed as a title, by keyword.
 *
 * This is deliberately not a model call: manual entry has to work offline and without an
 * API key, and a guess this cheap is right often enough to save a tap. Because it isn't the
 * model, the UI must not put the AI label on it.
 */
object CategoryGuesser {

    /**
     * The category with the most keyword hits, or null when nothing matches so the UI
     * leaves the choice to the user. Ties go to the category declared first, which is why
     * "nasi goreng + es teh" lands on Makan rather than Kopi.
     */
    fun guess(title: String): Category? {
        val text = normalize(title)
        if (text.isBlank()) return null

        return Category.entries
            .map { category -> category to (keywords[category] ?: emptyList()).count { " $it " in text } }
            .filter { (_, hits) -> hits > 0 }
            .maxWithOrNull(compareBy<Pair<Category, Int>> { it.second }.thenByDescending { it.first.ordinal })
            ?.first
    }

    /** Lowercase words separated by single spaces, padded so every keyword can match on whole words. */
    private fun normalize(title: String): String {
        val words = title.lowercase()
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.isNotEmpty() }
        return words.joinToString(" ", prefix = " ", postfix = " ")
    }

    // Whole words or phrases only: "grab" must not match inside "grabfood", "teh" not inside
    // a longer word. Multi-word entries ("warung kopi") outweigh their parts on purpose.
    private val keywords: Map<Category, List<String>> = mapOf(
        Category.Makan to listOf(
            "makan", "nasi", "ayam", "bebek", "ikan", "sate", "bakso", "mie", "mi", "soto", "rawon",
            "gado", "pecel", "warteg", "warung", "padang", "resto", "restoran", "rm", "lunch",
            "dinner", "sarapan", "food", "gofood", "grabfood", "shopeefood", "pizza", "burger",
            "mcd", "mcdonald", "kfc", "hokben", "sushi", "ramen", "seafood",
        ),
        Category.Kopi to listOf(
            "kopi", "coffee", "latte", "cappuccino", "americano", "espresso", "starbucks",
            "kenangan", "janji jiwa", "fore", "tomoro", "teh", "tea", "boba", "chatime", "mixue",
            "es krim", "jajan", "snack", "cemilan", "roti", "donat", "martabak", "warung kopi",
        ),
        Category.Belanja to listOf(
            "belanja", "indomaret", "alfamart", "alfamidi", "superindo", "hypermart", "lottemart",
            "transmart", "giant", "tokopedia", "shopee", "lazada", "blibli", "pasar", "groceries",
            "sabun", "sampo", "deterjen",
        ),
        Category.Transport to listOf(
            "gojek", "goride", "gocar", "grab", "grabbike", "grabcar", "maxim", "ojek", "ojol",
            "taksi", "taxi", "bluebird", "krl", "mrt", "lrt", "transjakarta", "busway", "bus",
            "kereta", "commuter", "whoosh", "bensin", "pertalite", "pertamax", "pertamina",
            "shell", "parkir", "tol", "toll", "flazz",
        ),
        Category.Tagihan to listOf(
            "tagihan", "listrik", "pln", "token", "pdam", "pulsa", "kuota", "paket data",
            "internet", "wifi", "indihome", "biznet", "bpjs", "cicilan", "kredit", "sewa", "kos",
            "kost", "kontrakan", "iuran", "asuransi",
        ),
        Category.Hiburan to listOf(
            "hiburan", "bioskop", "cinema", "xxi", "cgv", "cinepolis", "nonton", "netflix",
            "spotify", "youtube", "disney", "vidio", "game", "steam", "playstation", "konser",
            "karaoke", "tiket",
        ),
        Category.Kesehatan to listOf(
            "kesehatan", "apotek", "apotik", "obat", "dokter", "klinik", "rumah sakit", "rs",
            "vitamin", "kimia farma", "guardian", "halodoc", "alodokter", "k24", "gym", "fitness",
        ),
    )
}
