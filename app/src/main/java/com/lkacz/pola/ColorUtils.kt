package com.lkacz.pola

/**
 * Color utilities shared between dialog quick-fixes and (potentially) validator logic.
 */
object ColorUtils {
    /**
     * Attempts to normalize a color value:
     * - Expands #RGB -> #RRGGBB
     * - Expands #ARGB -> #AARRGGBB
     * - Accepts already valid #RRGGBB or #AARRGGBB (returns as uppercase)
     * - Translates limited named colors (RED, GREEN, BLUE, BLACK, WHITE, GRAY/GREY) to #RRGGBB
     * Returns null if input can't be normalized into an accepted hex form.
     */
    fun normalizeColorValue(input: String): String? {
        if (input.isBlank()) return null
        val v = input.trim()
        val shortHex = Regex("^#([0-9a-fA-F]{3})$")
        val shortHexA = Regex("^#([0-9a-fA-F]{4})$")
        if (shortHex.matches(v)) {
            val g = shortHex.find(v)!!.groupValues[1]
            val r = "" + g[0] + g[0]
            val gch = "" + g[1] + g[1]
            val b = "" + g[2] + g[2]
            return ("#$r$gch$b").uppercase()
        }
        if (shortHexA.matches(v)) {
            val g = shortHexA.find(v)!!.groupValues[1]
            val a = "" + g[0] + g[0]
            val r = "" + g[1] + g[1]
            val gch = "" + g[2] + g[2]
            val b = "" + g[3] + g[3]
            return ("#$a$r$gch$b").uppercase()
        }
        val named = mapOf(
            "RED" to "#FF0000",
            "GREEN" to "#00FF00",
            "BLUE" to "#0000FF",
            "BLACK" to "#000000",
            "WHITE" to "#FFFFFF",
            "GRAY" to "#808080",
            "GREY" to "#808080"
        )
        val upper = v.uppercase()
        if (named.containsKey(upper)) return named[upper]
        // Already valid long form? return uppercase version
        val longOk = Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")
        if (longOk.matches(v)) return v.uppercase()
        return null
    }
}
