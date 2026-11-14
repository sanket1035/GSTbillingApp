package com.example.gstbillingapp.utils

/** One line read from a bill. Every field is only a guess and must be reviewed by the user. */
data class ScannedItem(
    val name: String,
    val quantity: Int = 1,
    val price: Double = 0.0,
)

/**
 * Turns raw OCR text into candidate items. OCR on photos (and especially handwriting) is
 * unreliable, so this is intentionally simple and the UI makes every result editable.
 *
 * Understood line shapes (price = per-unit rate, before GST):
 *   "Parle-G 5 x 10"      -> name Parle-G, qty 5, price 10
 *   "Chawal 2 kg 60"      -> name "Chawal 2 kg", qty 1, price 60
 *   "Soap 45"             -> name Soap, qty 1, price 45
 */
object BillTextParser {

    private val timesPattern = Regex(
        """^(.*?)(\d+(?:\.\d+)?)\s*(?:x|\*|×)\s*(\d+(?:\.\d+)?)\s*(?:=\s*\d+(?:\.\d+)?)?\s*$""",
        RegexOption.IGNORE_CASE,
    )
    private val unitPattern = Regex(
        """^(.*?)(\d+(?:\.\d+)?)\s*(kg|g|gm|ltr|l|ml|pcs|pc|nos|dz)\b\.?\s*(\d+(?:\.\d+)?)\s*$""",
        RegexOption.IGNORE_CASE,
    )
    private val numberPattern = Regex("""\d+(?:\.\d+)?""")
    private val hasLetter = Regex("""[A-Za-zऀ-ॿ]""")

    fun parse(text: String): List<ScannedItem> =
        text.lines().mapNotNull { parseLine(it) }

    fun parseLine(raw: String): ScannedItem? {
        val line = raw.replace(Regex("""₹|Rs\.?""", RegexOption.IGNORE_CASE), " ")
            .replace('|', ' ').replace('_', ' ').trim()
        if (line.length < 2 || !hasLetter.containsMatchIn(line)) return null

        var name: String
        var qty = 1
        var price = 0.0

        val times = timesPattern.matchEntire(line)
        val unit = unitPattern.matchEntire(line)
        when {
            times != null -> {
                name = times.groupValues[1]
                val q = times.groupValues[2].toDoubleOrNull() ?: 1.0
                qty = if (q >= 1 && q == Math.floor(q)) q.toInt() else 1
                price = times.groupValues[3].toDoubleOrNull() ?: 0.0
            }
            unit != null -> {
                // Quantity column is Int in the database, so weights stay in the name.
                name = unit.groupValues[1] + unit.groupValues[2] + " " + unit.groupValues[3]
                price = unit.groupValues[4].toDoubleOrNull() ?: 0.0
            }
            else -> {
                val last = numberPattern.findAll(line).lastOrNull()
                if (last != null) {
                    price = last.value.toDoubleOrNull() ?: 0.0
                    name = line.substring(0, last.range.first)
                } else {
                    name = line
                }
            }
        }
        name = name.replace(Regex("""[-=:,.\s]+$"""), "")
            .replace(Regex("""^\s*\d+[.)]\s+"""), "")
            .trim()
        if (name.isEmpty()) return null
        return ScannedItem(name, qty, price)
    }
}
