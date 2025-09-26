package com.lkacz.pola

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileOperations(
    private val mainFolder: File,
    initialFile: File,
) {
    @Volatile
    private var targetFile: File = initialFile

    fun createFileAndFolder() {
        try {
            if (!mainFolder.exists()) {
                mainFolder.mkdirs()
            }
            val current = targetFile
            val isFileNewlyCreated = if (!current.exists()) current.createNewFile() else false
            if (isFileNewlyCreated) {
                writeHeader(current)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun updateTargetFile(newFile: File) {
        targetFile = newFile
    }

    private fun writeHeader(file: File) {
        FileOutputStream(file, true)
            .bufferedWriter(Charsets.UTF_8)
            .use {
                it.write("DATE\tTIME\tHEADER\tBODY\tITEM\tITEM RESPONSE (number)\tITEM RESPONSE (text)\tWAITING TIME\tOTHER\n")
            }
    }

    fun writeToCSV(logMessage: String) {
        try {
            val current = targetFile
            if (!current.exists()) {
                current.parentFile?.mkdirs()
                writeHeader(current)
            }
            FileOutputStream(current, true).bufferedWriter(Charsets.UTF_8).use { it.write(logMessage) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
