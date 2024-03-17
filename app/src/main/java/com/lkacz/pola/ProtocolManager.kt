package com.lkacz.pola

import android.content.Context
import java.io.BufferedReader
import java.io.StringReader
import kotlin.random.Random
import android.net.Uri
import java.io.FileNotFoundException
import android.content.SharedPreferences


class ProtocolManager(private val context: Context) {

    private var sharedPref: SharedPreferences =
        context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

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

    // Method to randomize sections of the protocol.
    private fun performManipulations() {
        val lines = originalProtocol?.lines()
        val newLines = mutableListOf<String>()
        var randomize = false
        val randomSection = mutableListOf<String>()

        if (lines != null) {
            for (line in lines) {
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
                        val isRandom = line.trim().startsWith("RANDOMIZED_MULTISCALE")
                        val bracketStart = line.indexOf('[')
                        val bracketEnd = line.indexOf(']')

                        // Extract sections of the line before, within, and after brackets
                        val preBracket = line.substring(0, bracketStart).split(';')
                        val multiItems = line.substring(bracketStart + 1, bracketEnd).split(';')
                        val postBracket = line.substring(bracketEnd + 1).split(';').drop(1)  // drop 1 to remove the leading empty string

                        val header = preBracket[1]
                        val introduction = preBracket[2]
                        val responses = postBracket.joinToString(";")

                        val questionnaireLines = mutableListOf<String>()

                        for (item in multiItems) {
                            val newLine = "SCALE;$header;$introduction;$item;$responses"
                            questionnaireLines.add(newLine)
                        }

                        if (isRandom) {
                            questionnaireLines.shuffle(Random)
                        }

                        newLines.addAll(questionnaireLines)
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
        }

        finalProtocol = newLines.joinToString("\n")
    }

    // Method to provide the manipulated protocol as BufferedReader.
    fun getManipulatedProtocol(): BufferedReader {
        performManipulations()
        return BufferedReader(StringReader(finalProtocol))
    }

    companion object {
        var originalProtocol: String? = null
        var finalProtocol: String? = null
    }
}