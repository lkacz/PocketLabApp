package com.lkacz.pola

/**
 * Pure validation logic extracted from ProtocolValidationDialog for unit tests.
 */
class ProtocolValidator {
    companion object {
        val RECOGNIZED_COMMANDS =
            setOf(
                "BODY_ALIGNMENT",
                "BODY_COLOR",
                "BODY_SIZE",
                "CONTINUE_TEXT_COLOR",
                "CONTINUE_ALIGNMENT",
                "CONTINUE_BACKGROUND_COLOR",
                "CONTINUE_SIZE",
                "HTML",
                "END",
                "GOTO",
                "HEADER_ALIGNMENT",
                "HEADER_COLOR",
                "HEADER_SIZE",
                "INPUTFIELD",
                "INPUTFIELD[RANDOMIZED]",
                "INSTRUCTION",
                "ITEM_SIZE",
                "LABEL",
                "LOG",
                "RANDOMIZE_OFF",
                "RANDOMIZE_ON",
                "RESPONSE_BACKGROUND_COLOR",
                "RESPONSE_SIZE",
                "RESPONSE_TEXT_COLOR",
                "SCALE",
                "SCALE[RANDOMIZED]",
                "SCREEN_BACKGROUND_COLOR",
                "TOP_MARGIN",
                "BOTTOM_MARGIN",
                "STUDY_ID",
                "TIMER",
                "TIMER_SOUND",
                "TIMER_SIZE",
                "TIMER_COLOR",
                "TIMER_ALIGNMENT",
                "TRANSITIONS",
            )
    }

    data class LineResult(val lineNumber: Int, val raw: String, val error: String, val warning: String)

    private val recognizedCommands = RECOGNIZED_COMMANDS

    private val alignmentCommands =
        setOf(
            "BODY_ALIGNMENT",
            "CONTINUE_ALIGNMENT",
            "HEADER_ALIGNMENT",
            "TIMER_ALIGNMENT",
        )

    private val sizeCommands =
        setOf(
            "BODY_SIZE",
            "CONTINUE_SIZE",
            "HEADER_SIZE",
            "RESPONSE_SIZE",
            "ITEM_SIZE",
            "TIMER_SIZE",
        )

    private val colorCommands =
        setOf(
            "BODY_COLOR",
            "CONTINUE_TEXT_COLOR",
            "CONTINUE_BACKGROUND_COLOR",
            "HEADER_COLOR",
            "RESPONSE_TEXT_COLOR",
            "RESPONSE_BACKGROUND_COLOR",
            "SCREEN_BACKGROUND_COLOR",
            "TIMER_COLOR",
        )

    fun validate(lines: List<String>): List<LineResult> {
        val labelOccurrences = findLabelOccurrences(lines)
        var randomizationLevel = 0
        var lastCommand: String? = null
        var studyIdSeen = false
        val results = mutableListOf<LineResult>()
        lines.forEachIndexed { idx, raw ->
            val trimmed = raw.trim()
            var error = ""
            var warning = ""
            if (trimmed.startsWith("//") || trimmed.isEmpty()) {
                lastCommand = null
                results.add(LineResult(idx + 1, raw, error, warning))
                return@forEachIndexed
            }
            if (trimmed.endsWith(";")) error = append(error, "Line ends with stray semicolon")
            val parts = trimmed.split(";")
            val commandRaw = parts[0].uppercase()
            val recognized = recognizedCommands.contains(commandRaw)
            when (commandRaw) {
                "RANDOMIZE_ON" -> {
                    if (lastCommand == "RANDOMIZE_ON") error = append(error, "RANDOMIZE_ON cannot follow RANDOMIZE_ON")
                    randomizationLevel++
                }
                "RANDOMIZE_OFF" -> {
                    if (randomizationLevel <= 0) {
                        error = append(error, "RANDOMIZE_OFF without matching RANDOMIZE_ON")
                    } else {
                        randomizationLevel--
                    }
                }
                "STUDY_ID" -> {
                    if (studyIdSeen) {
                        error = append(error, "Duplicate STUDY_ID")
                    } else {
                        studyIdSeen = true
                        if (parts.getOrNull(1)?.trim().isNullOrEmpty()) {
                            error = append(error, "STUDY_ID missing required value")
                        }
                    }
                }
            }
            if (!recognized) {
                error = append(error, "Unrecognized command")
            } else {
                val (handledError, handledWarning) = handleKnown(commandRaw, parts, idx + 1, trimmed, labelOccurrences)
                if (handledError.isNotEmpty()) error = append(error, handledError)
                if (handledWarning.isNotEmpty()) warning = append(warning, handledWarning)
            }
            lastCommand = commandRaw
            results.add(LineResult(idx + 1, raw, error, warning))
        }
        if (randomizationLevel > 0) {
            results.add(
                LineResult(
                    lines.size + 1,
                    "<EOF>",
                    "RANDOMIZE_ON not closed by matching RANDOMIZE_OFF",
                    "",
                ),
            )
        }
        return results
    }

    private fun handleKnown(
        command: String,
        parts: List<String>,
        lineNumber: Int,
        line: String,
        labels: Map<String, List<Int>>,
    ): Pair<String, String> {
        var err = ""
        var warn = ""
        when (command) {
            "LABEL" -> {
                val labelName = parts.getOrNull(1)?.trim().orEmpty()
                val occ = labels[labelName] ?: emptyList()
                if (occ.size > 1 && lineNumber in occ) {
                    val others = occ.filter { it != lineNumber }
                    if (others.isNotEmpty()) err = append(err, "Label duplicated with line(s) ${others.joinToString(", ")}")
                }
            }
            "GOTO" -> {
                val target = parts.getOrNull(1)?.trim().orEmpty()
                if (target.isEmpty()) {
                    err = append(err, "GOTO missing target label")
                } else {
                    if (!labels.containsKey(target)) {
                        err = append(err, "GOTO target label '$target' not defined")
                    }
                }
            }
            "INSTRUCTION" -> if (line.count { it == ';' } != 3) err = append(err, "INSTRUCTION must have exactly 3 semicolons (4 segments)")
            "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> {
                if (parts.size < 4) err = append(err, "$command must have at least 4 segments")

                fun parseFieldTokens(raw: String): List<String> {
                    val trimmed = raw.trim()
                    if (trimmed.isEmpty()) return emptyList()
                    if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length >= 2) {
                        val inner = trimmed.substring(1, trimmed.length - 1)
                        return inner.split(';', ',').map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    return trimmed.split(';', ',').map { it.trim() }.filter { it.isNotEmpty() }
                }

                val tailSegments = parts.drop(3)
                val candidateSegments =
                    when {
                        tailSegments.isEmpty() -> emptyList()
                        tailSegments.size == 1 -> tailSegments
                        else -> {
                            val last = tailSegments.last().trim()
                            val looksLikeContinue =
                                last.contains("[HOLD]", ignoreCase = true) ||
                                    last.equals("continue", ignoreCase = true) ||
                                    last.equals("complete", ignoreCase = true) ||
                                    last.equals("next", ignoreCase = true)
                            val trimmed = if (looksLikeContinue) tailSegments.dropLast(1) else tailSegments
                            if (trimmed.isEmpty()) tailSegments else trimmed
                        }
                    }

                val definedFields = candidateSegments.flatMap { parseFieldTokens(it) }

                if (definedFields.isEmpty()) {
                    err = append(err, "$command needs at least one field definition in 4th segment")
                }
                if (command == "INPUTFIELD[RANDOMIZED]" && definedFields.size < 2) {
                    warn = append(warn, "Randomized input field has fewer than 2 fields")
                }
            }
            "SCALE", "SCALE[RANDOMIZED]" ->
                if (parts.size < 2) {
                    err = append(err, "$command must have at least one parameter after the command")
                }
            "TOP_MARGIN", "BOTTOM_MARGIN" -> {
                val rawValue = parts.getOrNull(1)?.trim().orEmpty()
                val numeric = rawValue.toIntOrNull()
                if (rawValue.isEmpty()) {
                    err = append(err, "$command requires a numeric value in dp")
                } else if (numeric == null) {
                    err = append(err, "$command value '$rawValue' is not a number")
                } else if (numeric < 0) {
                    err = append(err, "$command value must be >= 0")
                } else if (numeric > 200) {
                    warn = append(warn, "$command=$numeric is quite large; ensure this is intentional")
                }
            }
            "TIMER" -> {
                if (parts.size != 5) err = append(err, "TIMER must have exactly 4 semicolons (5 segments)")
                val timeVal = parts.getOrNull(3)?.trim()?.toIntOrNull()
                if (timeVal == null || timeVal < 0) {
                    err = append(err, "TIMER must have a non-negative integer in the 4th segment")
                } else if (timeVal > 3600) {
                    warn = append(warn, "TIMER = $timeVal (over 3600s, is that intentional?)")
                }
            }
        }
        // Generic validation for alignment
        if (command in alignmentCommands) {
            val valSegment = parts.getOrNull(1)?.trim()?.uppercase().orEmpty()
            if (valSegment.isEmpty()) {
                err = append(err, "$command requires a value")
            } else if (valSegment !in setOf("LEFT", "CENTER", "RIGHT")) {
                err = append(err, "$command invalid alignment '$valSegment'")
            }
        }
        // Generic size validation (numeric positive)
        if (command in sizeCommands) {
            val sizeVal = parts.getOrNull(1)?.trim()?.toFloatOrNull()
            if (sizeVal == null || sizeVal <= 0f) {
                err = append(err, "$command must be a positive number")
            } else if (sizeVal > 200f) {
                warn = append(warn, "$command=$sizeVal unusually large")
            }
        }
        // Generic color validation (#RRGGBB or #AARRGGBB)
        if (command in colorCommands) {
            val colorVal = parts.getOrNull(1)?.trim().orEmpty()
            val colorOk = Regex("^#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$").matches(colorVal)
            if (!colorOk) err = append(err, "$command expects hex color like #RRGGBB or #AARRGGBB")
        }
        return err to warn
    }

    private fun findLabelOccurrences(lines: List<String>): Map<String, List<Int>> {
        val map = mutableMapOf<String, MutableList<Int>>()
        lines.forEachIndexed { i, raw ->
            val trimmed = raw.trim()
            if (trimmed.uppercase().startsWith("LABEL;")) {
                val parts = trimmed.split(";")
                val name = parts.getOrNull(1)?.trim().orEmpty()
                if (name.isNotEmpty()) {
                    map.computeIfAbsent(name) { mutableListOf() }.add(i + 1)
                }
            }
        }
        return map
    }

    private fun append(
        current: String,
        add: String,
    ): String = if (current.isEmpty()) add else "$current; $add"
}
