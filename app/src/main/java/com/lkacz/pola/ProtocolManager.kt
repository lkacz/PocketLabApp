// Filename: ProtocolManager.kt
package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.StringReader
import kotlin.random.Random

class ProtocolManager(private val context: Context) {

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

    fun readOriginalProtocol(uri: Uri? = null) {
        try {
            val inputStream = when {
                uri != null && isAssetUri(uri.toString()) -> {
                    val assetFileName = extractAssetFileName(uri.toString())
                    context.assets.open(assetFileName)
                }
                uri != null -> {
                    context.contentResolver.openInputStream(uri)
                }
                else -> {
                    val mode = sharedPref.getString("CURRENT_MODE", "demo")
                    val fileName =
                        if (mode == "tutorial") "tutorial_protocol.txt" else "demo_protocol.txt"
                    context.assets.open(fileName)
                }
            }
            inputStream?.bufferedReader().use { reader ->
                val lines = reader?.readLines()
                originalProtocol = lines?.joinToString("\n")
                lines?.firstOrNull { it.startsWith("STUDY_ID;") }?.let {
                    val studyId = it.split(";").getOrNull(1)
                    sharedPref.edit().putString("STUDY_ID", studyId).apply()
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Converts multi-line recognized commands to single-line format
     * for recognized commands. This ensures lines like:
     *
     *   INPUTFIELD;
     *       MyHeader;
     *       Some text;
     *       [Field1;Field2];
     *       Continue
     *
     * become:
     *
     *   INPUTFIELD;MyHeader;Some text;[Field1;Field2];Continue
     */
    private fun mergeMultiLineCommands(rawLines: List<String>): List<String> {
        val recognizedCommands = setOf(
            "BODY_ALIGNMENT",
            "BODY_COLOR",
            "BODY_SIZE",
            "CONTINUE_TEXT_COLOR",
            "CONTINUE_ALIGNMENT",
            "CONTINUE_BACKGROUND_COLOR",
            "CONTINUE_SIZE",
            "CUSTOM_HTML",
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
            "STUDY_ID",
            "TIMER",
            "TIMER_SOUND",
            "TIMER_SIZE",
            "TIMER_COLOR",
            "TIMER_ALIGNMENT",
            "TRANSITIONS"
        )

        val mergedLines = mutableListOf<String>()
        var tempBuffer = StringBuilder()
        var isMerging = false

        for (originalLine in rawLines) {
            val line = originalLine.trim()
            if (line.isEmpty()) {
                if (!isMerging) mergedLines.add(line)
                else {
                    tempBuffer.append(" ")
                }
                continue
            }

            val split = line.split(";").map { it.trim() }
            val firstToken = split.firstOrNull()?.uppercase()

            if (!isMerging) {
                // Check if line starts with recognized command
                if (firstToken != null && recognizedCommands.contains(firstToken)) {
                    // If there's exactly one token or (line ends in semicolon),
                    // we suspect multi-line command start
                    val hasTrailingSemicolon =
                        originalLine.trimEnd().endsWith(";") && line.count { it == ';' } == 1
                    // Or if the rest is blank (like "INPUTFIELD;")
                    if (line.endsWith(";") && split.size == 1) {
                        tempBuffer = StringBuilder(line.removeSuffix(";"))
                        isMerging = true
                        continue
                    } else if (hasTrailingSemicolon || lineEndsWithSemicolonButIncomplete(split)) {
                        // e.g. "INPUTFIELD;" or "BODY_ALIGNMENT;"
                        tempBuffer = StringBuilder(line.removeSuffix(";"))
                        isMerging = true
                        continue
                    } else {
                        mergedLines.add(line)
                    }
                } else {
                    // Not recognized or does not start a multi-line block
                    mergedLines.add(line)
                }
            } else {
                // We are already merging subsequent lines
                val endsWithSemicolon = line.endsWith(";")
                // Clean trailing semicolon for this sub-line
                val lineContent = if (endsWithSemicolon) line.removeSuffix(";") else line
                tempBuffer.append(";")
                tempBuffer.append(lineContent)

                // If line doesn't end with semicolon, merging ends
                if (!endsWithSemicolon) {
                    mergedLines.add(tempBuffer.toString())
                    isMerging = false
                }
            }
        }
        // If still merging by end of file:
        if (isMerging && tempBuffer.isNotEmpty()) {
            mergedLines.add(tempBuffer.toString())
        }
        return mergedLines
    }

    /**
     * Detects if line has an incomplete set of tokens but ends with semicolon,
     * implying user intends multi-line continuation. For example:
     * "INPUTFIELD;" with more lines expected.
     */
    private fun lineEndsWithSemicolonButIncomplete(split: List<String>): Boolean {
        // If the first token is recognized, user might break content across lines if the line ends with ";"
        // More advanced checks can be added here if needed.
        return split.size < 3
    }

    private fun performManipulations() {
        if (originalProtocol == null) {
            finalProtocol = ""
            return
        }

        // 1) Split into lines
        val rawLines = originalProtocol!!.lines()
        // 2) Merge multi-line recognized commands
        val singleLineCommands = mergeMultiLineCommands(rawLines)
        // 3) Handle randomization blocks
        val newLines = mutableListOf<String>()
        var randomize = false
        val randomSection = mutableListOf<String>()

        for (line in singleLineCommands) {
            val trimmed = line.trim()
            when {
                trimmed.uppercase() == "RANDOMIZE_ON" -> {
                    randomize = true
                }
                trimmed.uppercase() == "RANDOMIZE_OFF" -> {
                    randomize = false
                    randomSection.shuffle(Random)
                    randomSection.forEach { randomLine ->
                        newLines.addAll(MultiScaleHelper.expandScaleLine(randomLine))
                    }
                    randomSection.clear()
                }
                else -> {
                    if (randomize) {
                        randomSection.add(line)
                    } else {
                        val expanded = MultiScaleHelper.expandScaleLine(line)
                        newLines.addAll(expanded)
                    }
                }
            }
        }
        if (randomize) {
            randomSection.shuffle(Random)
            randomSection.forEach { randomLine ->
                newLines.addAll(MultiScaleHelper.expandScaleLine(randomLine))
            }
            randomSection.clear()
        }
        finalProtocol = newLines.joinToString("\n")
    }

    fun getManipulatedProtocol(): BufferedReader {
        performManipulations()
        return BufferedReader(StringReader(finalProtocol))
    }

    private fun isAssetUri(uriString: String): Boolean {
        return uriString.startsWith("file:///android_asset/")
    }

    private fun extractAssetFileName(uriString: String): String {
        return uriString.removePrefix("file:///android_asset/").trim()
    }

    companion object {
        var originalProtocol: String? = null
        var finalProtocol: String? = null
    }
}
