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
    var studyId: String? = null

    private var sharedPref: SharedPreferences =
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
        val lines = originalProtocol?.lines()
        val newLines = mutableListOf<String>()
        var randomize = false
        val randomSection = mutableListOf<String>()

        lines?.forEach { line ->
            when {
                line.trim() == "RANDOMIZE_ON" -> {
                    randomize = true
                }
                line.trim() == "RANDOMIZE_OFF" -> {
                    randomize = false
                    randomSection.shuffle(Random)
                    newLines.addAll(randomSection)
                    randomSection.clear()
                }
                line.trim().startsWith("MULTISCALE") || line.trim().startsWith("RANDOMIZED_MULTISCALE") -> {
                    val expanded = MultiScaleHelper.expandMultiScaleLine(line)
                    if (randomize) {
                        randomSection.addAll(expanded)
                    } else {
                        newLines.addAll(expanded)
                    }
                }
                else -> {
                    if (randomize) {
                        randomSection.add(line)
                    } else {
                        newLines.add(line)
                    }
                }
            }
        }

        if (randomize) {
            randomSection.shuffle(Random)
            newLines.addAll(randomSection)
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
