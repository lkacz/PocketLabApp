// Filename: MultiScaleHelper.kt
package com.lkacz.pola

import kotlin.random.Random

/**
 * Handles expansion for lines that start with "SCALE" or "SCALE[RANDOMIZED]"
 * and have a bracketed list. If multiple bracketed items exist, it returns
 * multiple lines (one item each). If [RANDOMIZED], it shuffles them first.
 */
object MultiScaleHelper {

    /**
     * If [originalLine] is "SCALE" (or "SCALE[RANDOMIZED]") with multiple
     * bracketed items, expand them into multiple single-item lines. Otherwise
     * return the line as-is.
     *
     * For example:
     *   SCALE[RANDOMIZED];Header;Body;[ItemA;ItemB];RespX;RespY
     * can be expanded into multiple lines (one per bracketed item).
     */
    fun expandScaleLine(originalLine: String): List<String> {
        val trimmed = originalLine.trim()

        // Check if line starts with "SCALE" ignoring case
        if (!trimmed.uppercase().startsWith("SCALE")) {
            return listOf(originalLine)
        }

        // Determine if it's a randomized scale
        val isRandom = trimmed.uppercase().startsWith("SCALE[RANDOMIZED]")

        // Use custom splitting to preserve bracketed semicolons
        val parts = ParsingUtils.customSplitSemicolons(trimmed).map { it.trim() }
        if (parts.size < 4) {
            // Not enough parts to treat as a standard SCALE line
            return listOf(originalLine)
        }

        // The bracketed portion is typically at index 3
        val itemCandidate = parts[3]
        if (!itemCandidate.startsWith("[") || !itemCandidate.endsWith("]")) {
            // No bracket => single-scale
            return listOf(originalLine)
        }

        // Extract items inside the brackets
        val bracketContent = itemCandidate.substring(1, itemCandidate.length - 1).trim()
        val items = bracketContent.split(";").map { it.trim() }.filter { it.isNotEmpty() }

        // If only 0 or 1 item found, no expansion needed
        if (items.size <= 1) {
            return listOf(originalLine)
        }

        // We'll proceed with expansion
        // Shuffle if needed
        val itemList = items.toMutableList()
        if (isRandom) {
            itemList.shuffle(Random)
        }

        // Recreate lines. For example, everything up to index=3 is directive+header+body
        val prefix = parts.take(3).joinToString(";")
        // Everything beyond index=3 is the responses
        val suffix = if (parts.size > 4) parts.drop(4).joinToString(";") else ""
        // Updated directive for each expanded line
        val newDirective = if (isRandom) "SCALE[RANDOMIZED]" else "SCALE"

        // Build lines for each item
        return itemList.map { singleItem ->
            if (suffix.isNotBlank()) {
                "$newDirective;${parts[1]};${parts[2]};$singleItem;$suffix"
            } else {
                "$newDirective;${parts[1]};${parts[2]};$singleItem"
            }
        }
    }
}
