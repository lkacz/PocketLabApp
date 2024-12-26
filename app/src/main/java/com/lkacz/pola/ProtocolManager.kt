package com.lkacz.pola

import android.content.Context
import android.net.Uri
import android.content.SharedPreferences
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.StringReader
import kotlin.random.Random

class ProtocolManager(private val context: Context) {
    var studyId: String? = null // Variable to store the STUDY_ID

    private var sharedPref: SharedPreferences =
        context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

    /**
     * Reads the original protocol from either the provided Uri or from default assets
     * (demo or tutorial). The originalProtocol is stored as a static field for later use.
     */
    fun readOriginalProtocol(uri: Uri? = null) {
        try {
            val inputStream = if (uri != null) {
                // Use ContentResolver to open InputStream from URI
                context.contentResolver.openInputStream(uri)
            } else {
                // Check the mode (demo or tutorial) and load the corresponding protocol
                val mode = sharedPref.getString("CURRENT_MODE", "demo")
                val fileName = if (mode == "tutorial") "tutorial_protocol.txt" else "demo_protocol.txt"
                context.assets.open(fileName)
            }

            inputStream?.bufferedReader().use { reader ->
                val lines = reader?.readLines()
                originalProtocol = lines?.joinToString("\n")

                // Extract STUDY_ID and save it in SharedPreferences
                lines?.firstOrNull { it.startsWith("STUDY_ID;") }?.let {
                    val studyId = it.split(";").getOrNull(1)
                    sharedPref.edit().putString("STUDY_ID", studyId).apply()
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    /**
     * Performs manipulations on the original protocol, including:
     * - Capturing font size directives (HEADER_SIZE;X, etc.) and persisting them
     * - Enabling/disabling randomization blocks
     * - Expanding MULTISCALE and RANDOMIZED_MULTISCALE lines into multiple SCALE lines
     * Then sets [finalProtocol] with the resulting lines.
     */
    private fun performManipulations() {
        val lines = originalProtocol?.lines()
        val newLines = mutableListOf<String>()
        var randomize = false
        val randomSection = mutableListOf<String>()

        lines?.forEach { line ->
            // 1) If it's a font size directive, update font sizes in SharedPreferences, skip it
            if (FontSizeUpdater.updateFontSizesFromLine(context, line)) {
                return@forEach
            }

            when {
                line.trim() == "RANDOMIZE_ON" -> {
                    randomize = true
                }
                line.trim() == "RANDOMIZE_OFF" -> {
                    randomize = false
                    // Shuffle the accumulated randomSection, then add to newLines
                    randomSection.shuffle(Random)
                    newLines.addAll(randomSection)
                    randomSection.clear()
                }
                line.trim().startsWith("MULTISCALE") || line.trim().startsWith("RANDOMIZED_MULTISCALE") -> {
                    // Expand using helper and add results
                    val expanded = MultiScaleHelper.expandMultiScaleLine(line)
                    if (randomize) {
                        randomSection.addAll(expanded)
                    } else {
                        newLines.addAll(expanded)
                    }
                }
                else -> {
                    // Normal line, either accumulate in randomSection or add to newLines
                    if (randomize) {
                        randomSection.add(line)
                    } else {
                        newLines.add(line)
                    }
                }
            }
        }

        // If randomize was still true at the end, flush what remains in randomSection
        if (randomize) {
            randomSection.shuffle(Random)
            newLines.addAll(randomSection)
            randomSection.clear()
        }

        finalProtocol = newLines.joinToString("\n")
    }

    /**
     * @return A [BufferedReader] containing the manipulated protocol.
     */
    fun getManipulatedProtocol(): BufferedReader {
        performManipulations()
        return BufferedReader(StringReader(finalProtocol))
    }

    companion object {
        var originalProtocol: String? = null
        var finalProtocol: String? = null
    }
}
