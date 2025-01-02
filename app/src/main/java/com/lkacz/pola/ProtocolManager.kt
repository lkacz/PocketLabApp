// Filename: ProtocolManager.kt
package com.lkacz.pola

import android.content.Context
import android.net.Uri
import android.content.SharedPreferences
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.StringReader
import kotlin.random.Random

class ProtocolManager(private val context: Context) {

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

    fun readOriginalProtocol(uri: Uri? = null) {
        try {
            val inputStream = if (uri != null) {
                context.contentResolver.openInputStream(uri)
            } else {
                val mode = sharedPref.getString("CURRENT_MODE", "demo")
                val fileName = if (mode == "tutorial") "tutorial_protocol.txt" else "demo_protocol.txt"
                context.assets.open(fileName)
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
        }
    }

    private fun performManipulations() {
        val rawLines = originalProtocol?.lines() ?: emptyList()
        val newLines = mutableListOf<String>()
        var randomize = false
        val randomSection = mutableListOf<String>()

        for (line in rawLines) {
            val trimmed = line.trim()
            when {
                trimmed == "RANDOMIZE_ON" -> {
                    randomize = true
                }
                trimmed == "RANDOMIZE_OFF" -> {
                    randomize = false
                    // finalize randomSection
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
                        // expand right away
                        val expanded = MultiScaleHelper.expandScaleLine(line)
                        newLines.addAll(expanded)
                    }
                }
            }
        }

        // If randomize was still on, do final flush
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

    companion object {
        var originalProtocol: String? = null
        var finalProtocol: String? = null
    }
}
