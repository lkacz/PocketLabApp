package com.lkacz.pola

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileOperations(private val mainFolder: File, private val file: File) {
    fun createFileAndFolder() {
        try {
            if (!mainFolder.exists()) {
                mainFolder.mkdirs()
            }
            val isFileNewlyCreated = if (!file.exists()) file.createNewFile() else false
            if (isFileNewlyCreated) {
                writeHeader()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun writeHeader() {
        // Renamed INTRODUCTION -> BODY
        writeToCSV("DATE\tTIME\tHEADER\tBODY\tITEM\tITEM RESPONSE (number)\tITEM RESPONSE (text)\tWAITING TIME\tOTHER\n")
    }

    fun writeToCSV(logMessage: String) {
        try {
            FileOutputStream(file, true).bufferedWriter(Charsets.UTF_8).use { it.write(logMessage) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
