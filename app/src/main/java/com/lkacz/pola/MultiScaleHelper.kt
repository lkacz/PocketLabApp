// Filename: MultiScaleHelper.kt
package com.lkacz.pola

import kotlin.random.Random

/**
 * Handles expansion for lines that start with "SCALE" or "SCALE[RANDOMIZED]".
 *
 * - If [SCALE;...] is detected with multiple bracketed items, it expands into multiple lines,
 *   each line containing one of the bracketed items, in the same order.
 *
 * - If [SCALE[RANDOMIZED];...] is detected, it also expands bracketed items into multiple lines,
 *   but randomizes the order of those bracketed items first.
 */
object MultiScaleHelper {
    /**
     * Examines [originalLine]. If it starts with "SCALE" or "SCALE[RANDOMIZED]", it processes bracketed items.
     * - If "SCALE[RANDOMIZED]", shuffle multiple bracketed items and return multiple lines
     *   (each item forming a single scale line).
     * - If just "SCALE" and multiple bracketed items, expand them into multiple lines in the typed order.
     */
    fun expandScaleLine(originalLine: String, rnd: Random = Random): List<String> {
        val trimmed = originalLine.trim()

        // Only proceed if it starts with "SCALE", ignoring case
        if (!trimmed.uppercase().startsWith("SCALE")) {
            return listOf(originalLine)
        }

        // Check for randomized
        val isRandom = trimmed.uppercase().startsWith("SCALE[RANDOMIZED]")

        // Use custom splitting to preserve bracketed semicolons
        val parts = ParsingUtils.customSplitSemicolons(trimmed).map { it.trim() }
        if (parts.size < 4) {
            // Not enough parts to treat as a standard SCALE line
            return listOf(originalLine)
        }

        // The bracketed portion is typically at index 3
        val itemCandidate = parts[3]
        // If there's no bracket, treat as a single-scale line
        if (!itemCandidate.startsWith("[") || !itemCandidate.endsWith("]")) {
            return listOf(originalLine)
        }

        // Extract items inside the brackets
        val bracketContent = itemCandidate.substring(1, itemCandidate.length - 1).trim()
        val items = bracketContent.split(";").map { it.trim() }.filter { it.isNotEmpty() }

        // If only 0 or 1 item found, no special expansion needed
        if (items.size <= 1) {
            return listOf(originalLine)
        }

        // Decide the directive
        val newDirective = if (isRandom) "SCALE[RANDOMIZED]" else "SCALE"

        // The prefix is everything up to index=3 (directive + header + body)
        // The suffix is everything after index=3, i.e. the responses
        val prefix = parts.take(3).joinToString(";")
        val suffix = if (parts.size > 4) parts.drop(4).joinToString(";") else ""

        return if (isRandom) {
            /*
             * For SCALE[RANDOMIZED] with multiple bracketed items, shuffle them
             * and produce multiple lines (one item per line).
             */
            val shuffledItems = items.toMutableList().also { it.shuffle(rnd) }
            shuffledItems.map { singleItem ->
                if (suffix.isNotBlank()) {
                    "$newDirective;${parts[1]};${parts[2]};$singleItem;$suffix"
                } else {
                    "$newDirective;${parts[1]};${parts[2]};$singleItem"
                }
            }
        } else {
            /*
             * For a normal SCALE with multiple bracketed items, expand them
             * into multiple lines (one item per line) in the typed order.
             */
            items.map { singleItem ->
                if (suffix.isNotBlank()) {
                    "$newDirective;${parts[1]};${parts[2]};$singleItem;$suffix"
                } else {
                    "$newDirective;${parts[1]};${parts[2]};$singleItem"
                }
            }
        }
    }
}
