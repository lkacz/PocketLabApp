// Filename: FragmentLoader.kt
package com.lkacz.pola

import android.content.Context

/**
 * Updated to unify SCALE, and to prevent the item from being included
 * in responses. If the bracketed portion has multiple items, we expand
 * them inside [MultiScaleHelper], then parse normally here.
 */
class FragmentLoader(
    bufferedReader: java.io.BufferedReader,
    private val logger: Logger
) {
    private val lines = mutableListOf<String>()
    private val labelMap = mutableMapOf<String, Int>()
    private var currentIndex = -1

    init {
        val rawLines = bufferedReader.readLines()
        rawLines.forEach { line -> lines.add(line.trim()) }
        lines.forEachIndexed { index, line ->
            if (line.startsWith("LABEL;")) {
                val labelName = line.split(";").getOrNull(1)?.trim().orEmpty()
                if (labelName.isNotEmpty()) labelMap[labelName] = index
            }
        }
    }

    fun loadNextFragment(): androidx.fragment.app.Fragment {
        while (true) {
            currentIndex++
            if (currentIndex >= lines.size) {
                return EndFragment()
            }
            val line = lines[currentIndex]
            if (line.isBlank()) continue

            // Use custom splitter that respects bracketed content
            val parts = ParsingUtils.customSplitSemicolons(line).map { it.trim('"') }
            val directive = parts.firstOrNull()?.uppercase() ?: ""

            when (directive) {
                "LABEL" -> continue
                "GOTO" -> {
                    jumpToLabel(parts.getOrNull(1).orEmpty())
                    continue
                }
                "TRANSITIONS" -> {
                    val mode = parts.getOrNull(1)?.lowercase() ?: "off"
                    getContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
                        .edit().putString("TRANSITION_MODE", mode).apply()
                    continue
                }
                "TIMER_SOUND" -> {
                    val filename = parts.getOrNull(1)?.trim().orEmpty()
                    getContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
                        .edit().putString("CUSTOM_TIMER_SOUND", filename).apply()
                    continue
                }
                "HEADER_ALIGNMENT" -> {
                    val alignValue = parts.getOrNull(1)?.uppercase() ?: "CENTER"
                    getContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
                        .edit().putString("HEADER_ALIGNMENT", alignValue).apply()
                    continue
                }
                "HEADER_COLOR" -> {
                    val colorStr = parts.getOrNull(1)?.trim().orEmpty()
                    val colorInt = safeParseColor(colorStr)
                    ColorManager.setHeaderTextColor(getContext(), colorInt)
                    continue
                }
                "BODY_COLOR" -> {
                    val colorStr = parts.getOrNull(1)?.trim().orEmpty()
                    val colorInt = safeParseColor(colorStr)
                    ColorManager.setBodyTextColor(getContext(), colorInt)
                    continue
                }
                "BODY_ALIGNMENT" -> {
                    val alignValue = parts.getOrNull(1)?.uppercase() ?: "CENTER"
                    getContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
                        .edit().putString("BODY_ALIGNMENT", alignValue).apply()
                    continue
                }
                "CONTINUE_TEXT_COLOR" -> {
                    val colorStr = parts.getOrNull(1)?.trim().orEmpty()
                    val colorInt = safeParseColor(colorStr)
                    ColorManager.setContinueTextColor(getContext(), colorInt)
                    continue
                }
                "CONTINUE_BACKGROUND_COLOR" -> {
                    val colorStr = parts.getOrNull(1)?.trim().orEmpty()
                    val colorInt = safeParseColor(colorStr)
                    ColorManager.setContinueBackgroundColor(getContext(), colorInt)
                    continue
                }
                "CONTINUE_ALIGNMENT" -> {
                    val alignValue = parts.getOrNull(1)?.uppercase() ?: "CENTER"
                    getContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
                        .edit().putString("CONTINUE_ALIGNMENT", alignValue).apply()
                    continue
                }
                "RESPONSE_SPACING" -> {
                    val spacingVal = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
                    SpacingManager.setResponseSpacing(getContext(), spacingVal)
                    continue
                }
                "HEADER_SIZE", "BODY_SIZE", "ITEM_SIZE", "RESPONSE_SIZE", "CONTINUE_SIZE" -> {
                    val sizeValue = parts.getOrNull(1)?.toFloatOrNull()
                    if (sizeValue != null) {
                        when (directive) {
                            "HEADER_SIZE" -> FontSizeManager.setHeaderSize(getContext(), sizeValue)
                            "BODY_SIZE" -> FontSizeManager.setBodySize(getContext(), sizeValue)
                            "ITEM_SIZE" -> FontSizeManager.setItemSize(getContext(), sizeValue)
                            "RESPONSE_SIZE" -> FontSizeManager.setResponseSize(getContext(), sizeValue)
                            "CONTINUE_SIZE" -> FontSizeManager.setContinueSize(getContext(), sizeValue)
                        }
                    }
                    continue
                }
                "RESPONSE_TEXT_COLOR" -> {
                    val colorStr = parts.getOrNull(1)?.trim().orEmpty()
                    val colorInt = safeParseColor(colorStr)
                    ColorManager.setResponseTextColor(getContext(), colorInt)
                    continue
                }
                "RESPONSE_BACKGROUND_COLOR" -> {
                    val colorStr = parts.getOrNull(1)?.trim().orEmpty()
                    val colorInt = safeParseColor(colorStr)
                    ColorManager.setButtonBackgroundColor(getContext(), colorInt)
                    continue
                }
                "SCALE", "SCALE[RANDOMIZED]" -> {
                    return createScaleFragment(parts)
                }
                "INSTRUCTION" -> {
                    return createInstructionFragment(parts)
                }
                "TIMER" -> {
                    return createTimerFragment(parts)
                }
                "TAP_INSTRUCTION" -> {
                    return createTapInstructionFragment(parts)
                }
                "INPUTFIELD" -> {
                    return createInputFieldFragment(parts)
                }
                "CUSTOM_HTML" -> {
                    return createCustomHtmlFragment(parts)
                }
                "END" -> {
                    return EndFragment()
                }
                else -> {
                    // Possibly unknown or empty directive; skip
                    continue
                }
            }
        }
    }

    fun jumpToLabelAndLoad(label: String): androidx.fragment.app.Fragment {
        jumpToLabel(label)
        return loadNextFragment()
    }

    private fun jumpToLabel(label: String) {
        val targetIndex = labelMap[label] ?: -1
        if (targetIndex == -1) {
            currentIndex = lines.size
            return
        }
        currentIndex = targetIndex
    }

    private fun createScaleFragment(parts: List<String>): androidx.fragment.app.Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val itemCandidate = parts.getOrNull(3)?.trim()

        // Everything beyond index 3 is considered possible responses
        val rawResponses = parts.drop(4)

        // If itemCandidate is bracketed
        if (itemCandidate != null && itemCandidate.startsWith("[") && itemCandidate.endsWith("]")) {
            val stripped = itemCandidate.substring(1, itemCandidate.length - 1)
            val splitted = stripped.split(";").map { it.trim() }.filter { it.isNotEmpty() }

            return when {
                splitted.size > 1 -> {
                    // multiple bracketed items
                    val hasLabels = rawResponses.any { it.contains('[') && it.contains(']') }
                    return if (hasLabels) {
                        val branchPairs = makeBranchPairs(rawResponses)
                        ScaleFragment.newBranchInstance(header, body, splitted.joinToString(" / "), branchPairs)
                    } else {
                        ScaleFragment.newInstance(header, body, splitted.joinToString(" / "), rawResponses)
                    }
                }
                splitted.size == 1 -> {
                    // Single bracketed item => single scale
                    val singleItem = splitted[0]
                    val hasLabels = rawResponses.any { it.contains('[') && it.contains(']') }
                    return if (hasLabels) {
                        val branchPairs = makeBranchPairs(rawResponses)
                        ScaleFragment.newBranchInstance(header, body, singleItem, branchPairs)
                    } else {
                        ScaleFragment.newInstance(header, body, singleItem, rawResponses)
                    }
                }
                else -> {
                    // Empty bracket => treat as single scale with no real item
                    val hasLabels = rawResponses.any { it.contains('[') && it.contains(']') }
                    return if (hasLabels) {
                        val branchPairs = makeBranchPairs(rawResponses)
                        ScaleFragment.newBranchInstance(header, body, "", branchPairs)
                    } else {
                        ScaleFragment.newInstance(header, body, "", rawResponses)
                    }
                }
            }
        } else {
            // normal single-scale (no bracket or bracket is malformed)
            val hasLabels = rawResponses.any { it.contains('[') && it.contains(']') }
            return if (hasLabels) {
                val branchPairs = makeBranchPairs(rawResponses)
                ScaleFragment.newBranchInstance(header, body, itemCandidate.orEmpty(), branchPairs)
            } else {
                ScaleFragment.newInstance(header, body, itemCandidate.orEmpty(), rawResponses)
            }
        }
    }

    private fun createInstructionFragment(parts: List<String>): androidx.fragment.app.Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonText = parts.getOrNull(3)
        return InstructionFragment.newInstance(header, body, buttonText)
    }

    private fun createTimerFragment(parts: List<String>): androidx.fragment.app.Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonText = parts.getOrNull(3)
        val timeSeconds = parts.getOrNull(4)?.toIntOrNull() ?: 0
        return TimerFragment.newInstance(header, body, buttonText, timeSeconds)
    }

    private fun createTapInstructionFragment(parts: List<String>): androidx.fragment.app.Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonText = parts.getOrNull(3)
        return TapInstructionFragment.newInstance(header, body, buttonText)
    }

    private fun createInputFieldFragment(parts: List<String>): androidx.fragment.app.Fragment {
        val heading = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonName = parts.getOrNull(3)
        val fields = parts.drop(4)
        return InputFieldFragment.newInstance(heading, body, buttonName, fields)
    }

    private fun createCustomHtmlFragment(parts: List<String>): androidx.fragment.app.Fragment {
        val fileName = parts.getOrNull(1).orEmpty()
        return CustomHtmlFragment.newInstance(fileName)
    }

    private fun safeParseColor(colorStr: String): Int {
        return try {
            android.graphics.Color.parseColor(colorStr)
        } catch (_: Exception) {
            android.graphics.Color.BLACK
        }
    }

    /**
     * Converts each response from "displayText[SomeLabel]" into Pair("displayText", "SomeLabel").
     * Otherwise, it becomes Pair("displayText", null).
     */
    private fun makeBranchPairs(raw: List<String>): List<Pair<String, String?>> {
        val branchResponses = mutableListOf<Pair<String, String?>>()
        for (resp in raw) {
            val bracketStart = resp.indexOf('[')
            val bracketEnd = resp.indexOf(']')
            if (bracketStart in 0 until bracketEnd) {
                val displayText = resp.substring(0, bracketStart).trim()
                val label = resp.substring(bracketStart + 1, bracketEnd).trim()
                branchResponses.add(displayText to label)
            } else {
                branchResponses.add(resp to null)
            }
        }
        return branchResponses
    }

    private fun getContext() = logger.javaClass
        .getDeclaredField("context")
        .apply { isAccessible = true }
        .get(logger) as android.content.Context
}
