package com.example.gstbillingapp.utils

/**
 * Indicative GST rate table used ONLY to pre-fill a suggestion while reviewing a scanned bill.
 *
 * !!! UNVERIFIED !!!
 * These rates were written from memory (slabs after the GST Council 56th meeting, Sept 2025).
 * They have NOT been checked against the CBIC notifications or by a CA. Rates and HSN codes
 * change, and a product's rate can depend on brand, packing, and description. The user must
 * confirm every rate before an invoice is saved. Verify at https://cbic-gst.gov.in .
 *
 * An item that matches no keyword (or an ambiguous one) returns null: the app does not guess.
 */
object GstRates {

    const val UNVERIFIED_NOTE =
        "Suggested GST rates are indicative and NOT verified. Confirm each rate with your CA " +
            "or at cbic-gst.gov.in before saving the invoice."

    val SLABS: List<Double> = listOf(0.0, 3.0, 5.0, 18.0, 40.0)

    data class Entry(val keywords: List<String>, val rate: Double?, val hsn: String)

    data class Suggestion(val rate: Double, val hsn: String, val matchedKeyword: String)

    // rate == null means "known to be ambiguous, ask the user" (no suggestion is shown).
    internal val ENTRIES: List<Entry> = listOf(
        Entry(listOf("milk", "doodh", "dudh"), 0.0, "0401"),
        Entry(listOf("paneer", "chhena"), 0.0, "0406"),
        Entry(listOf("salt", "namak"), 0.0, "2501"),
        Entry(listOf("curd", "dahi", "lassi", "chaas"), 5.0, "0403"),
        Entry(listOf("butter", "makhan"), 5.0, "0405"),
        Entry(listOf("ghee"), 5.0, "0405"),
        Entry(listOf("cheese"), 5.0, "0406"),
        Entry(listOf("sugar", "cheeni", "chini"), 5.0, "1701"),
        Entry(listOf("tea", "chai"), 5.0, "0902"),
        Entry(listOf("coffee"), 5.0, "0901"),
        Entry(listOf("atta", "maida", "besan", "suji", "rava"), 0.0, "1101"),
        Entry(listOf("rice", "chawal"), 0.0, "1006"),
        Entry(listOf("dal", "daal", "moong", "chana", "toor", "masoor", "urad"), 0.0, "0713"),
        Entry(listOf("oil", "tel"), 5.0, "1507"),
        Entry(listOf("namkeen", "bhujia", "mixture"), 5.0, "2106"),
        Entry(listOf("noodles", "maggi"), 5.0, "1902"),
        Entry(listOf("soap", "sabun"), 5.0, "3401"),
        Entry(listOf("toothpaste", "colgate"), 5.0, "3306"),
        Entry(listOf("hair oil"), 5.0, "3305"),
        Entry(listOf("shampoo"), 5.0, "3305"),
        Entry(listOf("pencil", "notebook", "copy", "eraser"), 0.0, "4820"),
        Entry(listOf("cold drink", "cola", "soft drink", "pepsi", "sprite"), 40.0, "2202"),
        Entry(listOf("mobile", "charger", "earphone", "headphone"), 18.0, "8517"),
        // Biscuits: rate depends on the product, so we deliberately give no suggestion.
        Entry(listOf("biscuit", "parle"), null, "1905"),
    )

    /** Returns a suggestion, or null when the item is unknown or ambiguous. */
    fun suggest(itemName: String): Suggestion? {
        val name = itemName.lowercase().trim()
        if (name.isEmpty()) return null
        for (entry in ENTRIES) {
            for (kw in entry.keywords) {
                // keyword must start at a word boundary ("tea" matches "green tea", not "steak")
                if (Regex("(^|[^a-z])" + Regex.escape(kw)).containsMatchIn(name)) {
                    return entry.rate?.let { Suggestion(it, entry.hsn, kw) }
                }
            }
        }
        return null
    }
}
