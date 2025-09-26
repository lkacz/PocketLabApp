// Filename: FragmentLoader.kt
package com.lkacz.pola

import android.content.Context
import androidx.fragment.app.Fragment

class FragmentLoader(
    bufferedReader: java.io.BufferedReader,
    private val logger: Logger,
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

    fun getCurrentCommandIndex(): Int = currentIndex

    fun prepareForResume(resumeIndex: Int) {
        currentIndex = if (resumeIndex >= 0) resumeIndex - 1 else -1
    }

    fun loadNextFragment(): Fragment? {
        while (true) {
            currentIndex++
            if (currentIndex >= lines.size) {
                return null
            }
            val line = lines[currentIndex]
            if (line.isBlank()) continue

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
                    getContext().getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
                        .edit().putString("TRANSITION_MODE", mode).apply()
                    continue
                }
                "TIMER_SOUND" -> {
                    val filename = parts.getOrNull(1)?.trim().orEmpty()
                    getContext().getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
                        .edit().putString("CUSTOM_TIMER_SOUND", filename).apply()
                    continue
                }
                "HEADER_ALIGNMENT" -> {
                    val alignValue = parts.getOrNull(1)?.uppercase() ?: "CENTER"
                    getContext().getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
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
                    getContext().getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
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
                    val arg1 = parts.getOrNull(1)?.uppercase()
                    val arg2 = parts.getOrNull(2)?.uppercase()

                    var horizontal = "RIGHT"
                    var vertical = "BOTTOM"
                    val possibleHoriz = setOf("LEFT", "CENTER", "RIGHT")
                    val possibleVert = setOf("TOP", "BOTTOM")

                    if (!arg1.isNullOrEmpty()) {
                        if (arg1 in possibleHoriz) {
                            horizontal = arg1
                        } else if (arg1 in possibleVert) {
                            vertical = arg1
                        }
                    }
                    if (!arg2.isNullOrEmpty()) {
                        if (arg2 in possibleHoriz) {
                            horizontal = arg2
                        } else if (arg2 in possibleVert) {
                            vertical = arg2
                        }
                    }

                    getContext().getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString("CONTINUE_ALIGNMENT_HORIZONTAL", horizontal)
                        .putString("CONTINUE_ALIGNMENT_VERTICAL", vertical)
                        .apply()
                    continue
                }
                "RESPONSE_SPACING" -> {
                    val spacingVal = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
                    SpacingManager.setResponseSpacing(getContext(), spacingVal)
                    continue
                }
                "TIMER_SIZE" -> {
                    val sizeValue = parts.getOrNull(1)?.toFloatOrNull() ?: 18f
                    FontSizeManager.setTimerSize(getContext(), sizeValue)
                    continue
                }
                "TIMER_COLOR" -> {
                    val colorStr = parts.getOrNull(1)?.trim().orEmpty()
                    val colorInt = safeParseColor(colorStr)
                    ColorManager.setTimerTextColor(getContext(), colorInt)
                    continue
                }
                "TIMER_ALIGNMENT" -> {
                    val alignValue = parts.getOrNull(1)?.uppercase() ?: "CENTER"
                    getContext().getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
                        .edit().putString("TIMER_ALIGNMENT", alignValue).apply()
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
                "SCREEN_BACKGROUND_COLOR" -> {
                    val colorStr = parts.getOrNull(1)?.trim().orEmpty()
                    val colorInt = safeParseColor(colorStr)
                    ColorManager.setScreenBackgroundColor(getContext(), colorInt)
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
                "INPUTFIELD" -> {
                    return createInputFieldFragment(parts, isRandom = false)
                }
                "INPUTFIELD[RANDOMIZED]" -> {
                    return createInputFieldFragment(parts, isRandom = true)
                }
                "HTML" -> {
                    return createHtmlFragment(parts)
                }
                "END" -> {
                    return null
                }
                else -> {
                    continue
                }
            }
        }
    }

    fun jumpToLabelAndLoad(label: String): Fragment? {
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

    private fun createScaleFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val itemCandidate = parts.getOrNull(3)?.trim()
        val rawResponses = parts.drop(4)
        val hasLabels = rawResponses.any { it.contains('[') && it.contains(']') }

        return if (hasLabels) {
            val branchPairs = makeBranchPairs(rawResponses)
            ScaleFragment.newBranchInstance(header, body, itemCandidate.orEmpty(), branchPairs)
        } else {
            ScaleFragment.newInstance(header, body, itemCandidate.orEmpty(), rawResponses)
        }
    }

    private fun createInstructionFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonText = parts.getOrNull(3)
        return InstructionFragment.newInstance(header, body, buttonText)
    }

    private fun createTimerFragment(parts: List<String>): Fragment {
        // TIMER;HEADER;BODY;TIME_IN_SECONDS;CONTINUE_TEXT
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val timeSeconds = parts.getOrNull(3)?.toIntOrNull() ?: 0
        val buttonText = parts.getOrNull(4)
        return TimerFragment.newInstance(header, body, timeSeconds, buttonText)
    }

    private fun createInputFieldFragment(
        parts: List<String>,
        isRandom: Boolean,
    ): Fragment? {
        if (parts.size < 4) {
            return null
        }
        val heading = parts[1]
        val body = parts[2]
        val continueText = parts[parts.size - 1]
        val rawFieldsRange = parts.subList(3, parts.size - 1)
        val combined = rawFieldsRange.joinToString(";").trim()

        val fields: List<String> =
            if (
                combined.startsWith("[") && combined.endsWith("]") && combined.length > 2
            ) {
                combined
                    .substring(1, combined.length - 1)
                    .split(";")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            } else {
                rawFieldsRange.map { it.trim() }.filter { it.isNotEmpty() }
            }

        return InputFieldFragment.newInstance(heading, body, continueText, fields, isRandom)
    }

    private fun createHtmlFragment(parts: List<String>): Fragment {
        val fileName = parts.getOrNull(1).orEmpty()
        val buttonText = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Continue"
        return HtmlFragment.newInstance(fileName, buttonText)
    }

    private fun safeParseColor(colorStr: String): Int {
        return try {
            android.graphics.Color.parseColor(colorStr)
        } catch (_: Exception) {
            android.graphics.Color.BLACK
        }
    }

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

    private fun getContext() =
        logger.javaClass
            .getDeclaredField("context")
            .apply { isAccessible = true }
            .get(logger) as Context
}
