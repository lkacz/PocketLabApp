// Filename: FragmentLoader.kt
package com.lkacz.pola

import android.content.Context
import androidx.fragment.app.Fragment
import java.io.BufferedReader

class FragmentLoader(
    private val bufferedReader: BufferedReader,
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

    fun loadNextFragment(): Fragment {
        while (true) {
            currentIndex++
            if (currentIndex >= lines.size) {
                return EndFragment()
            }
            val line = lines[currentIndex]
            if (line.isBlank()) continue

            val parts = line.split(";").map { it.trim('"') }
            val directive = parts.firstOrNull()?.uppercase() ?: ""

            when (directive) {
                "LABEL" -> continue
                "GOTO" -> {
                    jumpToLabel(parts.getOrNull(1).orEmpty())
                    continue
                }

                // Existing dynamic commands
                "TRANSITIONS" -> {
                    val mode = parts.getOrNull(1)?.lowercase() ?: "off"
                    TransitionManager.setTransitionMode(getContext(), mode)
                    continue
                }
                "TIMER_SOUND" -> {
                    val filename = parts.getOrNull(1)?.trim().orEmpty()
                    getContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
                        .edit().putString("CUSTOM_TIMER_SOUND", filename).apply()
                    continue
                }

                // New dynamic commands for appearance
                "HEADER_ALIGNMENT" -> {
                    val alignValue = parts.getOrNull(1)?.uppercase() ?: "CENTER"
                    getContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
                        .edit().putString("HEADER_ALIGNMENT", alignValue).apply()
                    continue
                }
                "HEADER_COLOR" -> {
                    // e.g. HEADER_COLOR;#FF0000
                    val colorStr = parts.getOrNull(1)?.trim().orEmpty()
                    val colorInt = safeParseColor(colorStr)
                    ColorManager.setHeaderTextColor(getContext(), colorInt)
                    continue
                }
                "RESPONSES_SPACING" -> {
                    // e.g. RESPONSES_SPACING;12
                    val spacingVal = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
                    SpacingManager.setResponsesSpacing(getContext(), spacingVal)
                    continue
                }

                "HEADER_SIZE", "BODY_SIZE", "BUTTON_SIZE", "ITEM_SIZE", "RESPONSE_SIZE" -> {
                    val sizeValue = parts.getOrNull(1)?.toFloatOrNull()
                    if (sizeValue != null) {
                        when (directive) {
                            "HEADER_SIZE" -> FontSizeManager.setHeaderSize(getContext(), sizeValue)
                            "BODY_SIZE" -> FontSizeManager.setBodySize(getContext(), sizeValue)
                            "BUTTON_SIZE" -> FontSizeManager.setButtonSize(getContext(), sizeValue)
                            "ITEM_SIZE" -> FontSizeManager.setItemSize(getContext(), sizeValue)
                            "RESPONSE_SIZE" -> FontSizeManager.setResponseSize(getContext(), sizeValue)
                        }
                    }
                    continue
                }

                // Fragments
                "BRANCH_SCALE" -> {
                    return createBranchScaleFragment(parts)
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
                "SCALE" -> {
                    return createScaleFragment(parts)
                }
                "INPUTFIELD" -> {
                    return createInputFieldFragment(parts)
                }
                "CUSTOM_HTML" -> {
                    return createCustomHtmlFragment(parts)
                }

                else -> continue
            }
        }
    }

    fun jumpToLabelAndLoad(label: String): Fragment {
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

    private fun createBranchScaleFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val item = parts.getOrNull(3)
        val rawResponses = parts.drop(4)
        val branchResponses = mutableListOf<Pair<String, String?>>()
        rawResponses.forEach { resp ->
            val bracketStart = resp.indexOf('[')
            val bracketEnd = resp.indexOf(']')
            if (bracketStart in 0 until bracketEnd) {
                val displayText = resp.substring(0, bracketStart).trim()
                val lbl = resp.substring(bracketStart + 1, bracketEnd).trim()
                branchResponses.add(displayText to lbl)
            } else {
                branchResponses.add(resp to null)
            }
        }
        return BranchScaleFragment.newInstance(header, body, item, branchResponses)
    }

    private fun createInstructionFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonText = parts.getOrNull(3)
        return InstructionFragment.newInstance(header, body, buttonText)
    }

    private fun createTimerFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonText = parts.getOrNull(3)
        val timeSeconds = parts.getOrNull(4)?.toIntOrNull() ?: 0
        return TimerFragment.newInstance(header, body, buttonText, timeSeconds)
    }

    private fun createTapInstructionFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonText = parts.getOrNull(3)
        return TapInstructionFragment.newInstance(header, body, buttonText)
    }

    private fun createScaleFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val item = parts.getOrNull(3)
        val responses = parts.drop(4)
        return ScaleFragment.newInstance(header, body, item, responses)
    }

    private fun createInputFieldFragment(parts: List<String>): Fragment {
        val heading = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonName = parts.getOrNull(3)
        val fields = parts.drop(4)
        return InputFieldFragment.newInstance(heading, body, buttonName, fields)
    }

    private fun createCustomHtmlFragment(parts: List<String>): Fragment {
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

    private fun getContext() = logger.javaClass
        .getDeclaredField("context")
        .apply { isAccessible = true }
        .get(logger) as android.content.Context
}
