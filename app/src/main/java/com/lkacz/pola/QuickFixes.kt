package com.lkacz.pola

/**
 * Collection of pure quick-fix transformations operating on protocol lines.
 * Each method returns a result describing how many changes were applied.
 */
object QuickFixes {
    data class Result(val lines: List<String>, val changedCount: Int)

    fun removeStraySemicolons(lines: List<String>): Result {
        var changed = 0
        val updated = lines.map { raw ->
            val trimmed = raw.trimEnd()
            if (trimmed.endsWith(";") && trimmed != ";") {
                val new = raw.replace(Regex(";\\s*$"), "")
                if (new != raw) changed++
                new
            } else raw
        }
        return Result(updated, changed)
    }

    fun removeDuplicateStudyIds(lines: List<String>): Result {
        var seen = false
        var removed = 0
        val updated = lines.filter { line ->
            val isStudy = line.trim().uppercase().startsWith("STUDY_ID;")
            if (!isStudy) return@filter true
            if (!seen) { seen = true; true } else { removed++; false }
        }
        return Result(updated, removed)
    }

    fun removeDuplicateLabels(lines: List<String>): Result {
        val seen = mutableSetOf<String>()
        var removed = 0
        val updated = lines.filter { line ->
            val t = line.trim().uppercase()
            if (!t.startsWith("LABEL;")) return@filter true
            val name = line.split(';').getOrNull(1)?.trim().orEmpty()
            if (name.isEmpty()) return@filter true
            if (seen.add(name)) true else { removed++; false }
        }
        return Result(updated, removed)
    }

    fun insertMissingGotoLabels(lines: List<String>): Result {
        // Map labels
        val labelSet = mutableSetOf<String>()
        lines.forEach { l ->
            val t = l.trim()
            if (t.uppercase().startsWith("LABEL;")) {
                val name = t.split(';').getOrNull(1)?.trim().orEmpty()
                if (name.isNotEmpty()) labelSet.add(name)
            }
        }
        // Gather missing targets with first occurrence index
        data class Missing(val target: String, val firstIndex: Int)
        val missingMap = mutableMapOf<String, Int>()
        lines.forEachIndexed { idx, l ->
            val t = l.trim().uppercase()
            if (t.startsWith("GOTO")) {
                val parts = l.trim().split(';')
                val target = parts.getOrNull(1)?.trim().orEmpty()
                if (target.isNotEmpty() && !labelSet.contains(target) && !missingMap.containsKey(target)) {
                    missingMap[target] = idx // zero-based index of GOTO line
                }
            }
        }
        if (missingMap.isEmpty()) return Result(lines, 0)
        val insertions = missingMap.entries.sortedBy { it.value }
        val mutable = lines.toMutableList()
        var offset = 0
        insertions.forEach { (target, gotoIdx) ->
            val insertionPos = gotoIdx + 1 + offset
            if (insertionPos <= mutable.size) mutable.add(insertionPos, "LABEL;$target") else mutable.add("LABEL;$target")
            offset++
        }
        return Result(mutable, insertions.size)
    }

    fun normalizeTimerLines(lines: List<String>): Result {
        var changed = 0
        val updated = lines.map { raw ->
            val t = raw.trim()
            if (!t.uppercase().startsWith("TIMER")) return@map raw
            val parts = t.split(';').toMutableList()
            if (parts.first().uppercase() != "TIMER") return@map raw
            var modified = false
            while (parts.size < 5) { parts.add(""); modified = true }
            val header = parts.getOrNull(1).takeUnless { it.isNullOrBlank() } ?: run { modified = true; "Header" }
            val body = parts.getOrNull(2).takeUnless { it.isNullOrBlank() } ?: run { modified = true; "Body" }
            val timeVal = parts.getOrNull(3)?.trim()?.toIntOrNull()?.takeIf { it >= 0 } ?: run { modified = true; 60 }
            val cont = parts.getOrNull(4).takeUnless { it.isNullOrBlank() } ?: run { modified = true; "Continue" }
            val normalized = "TIMER;$header;$body;$timeVal;$cont"
            if (modified || normalized != raw) { changed++; normalized } else raw
        }
        return Result(updated, changed)
    }

    fun normalizeColors(lines: List<String>): Result {
        val colorCommands = setOf(
            "HEADER_COLOR", "BODY_COLOR", "RESPONSE_TEXT_COLOR", "RESPONSE_BACKGROUND_COLOR",
            "SCREEN_BACKGROUND_COLOR", "CONTINUE_TEXT_COLOR", "CONTINUE_BACKGROUND_COLOR", "TIMER_COLOR"
        )
        var changed = 0
        val updated = lines.map { raw ->
            val t = raw.trim()
            val upper = t.uppercase()
            val cmd = colorCommands.firstOrNull { upper.startsWith(it + ";") }
            if (cmd == null) return@map raw
            val parts = t.split(';')
            val value = parts.getOrNull(1)?.trim().orEmpty()
            val normalized = ColorUtils.normalizeColorValue(value)
            if (normalized != null && normalized != value) {
                changed++
                "$cmd;$normalized"
            } else raw
        }
        return Result(updated, changed)
    }

    fun normalizeContentCommands(lines: List<String>): Result {
        var changed = 0
        val updated = lines.map { raw ->
            val norm = ContentCommandNormalizer.normalize(raw)
            if (norm != null) { changed++; norm } else raw
        }
        return Result(updated, changed)
    }
}
