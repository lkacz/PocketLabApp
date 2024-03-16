package com.lkacz.pola

import java.io.*

class FileOperations(private val mainFolder: File, private val file: File) {

    fun createFileAndFolder() {
        try {
            if (mainFolder.mkdirs() || file.createNewFile()) {
                writeHeader()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun writeHeader() {
        writeToCSV("DATE\tTIME\tHEADER\tINTRODUCTION\tITEM\tITEM RESPONSE (number)\tITEM RESPONSE (text)\tWAITING TIME\tOTHER\n")
    }

    fun writeToCSV(logMessage: String) {
        try {
            FileOutputStream(file, true).bufferedWriter(Charsets.UTF_8).use { it.write(logMessage) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}