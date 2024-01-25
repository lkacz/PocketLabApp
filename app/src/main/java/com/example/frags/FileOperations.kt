package com.example.frags

import android.os.Build
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class FileOperations(
    private val mainFolder: File,
    private val file: File
) {

    fun createFileAndFolder() {
        try {
            if (!mainFolder.exists()) {
                mainFolder.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val identifier = Build.ID
            val newFileName = "${identifier}_output_$timestamp.csv"
            val newFile = File(mainFolder, newFileName)

            if (!newFile.exists()) {
                newFile.createNewFile()
                writeHeader(newFile)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun writeHeader(newFile: File) {
        val identifier = Build.ID
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val header = "IDENTIFIER: $identifier, MANUFACTURER: $manufacturer, MODEL: $model\nDATE\tTIME\tHEADER\tINTRODUCTION\tITEM\tITEM RESPONSE (number)\tITEM RESPONSE (text)\tWAITING TIME\tOTHER\n"
        FileOutputStream(file, true).use { fos ->
            fos.write(header.toByteArray(Charsets.UTF_8))
        }
    }

    fun writeToCSV(logMessage: String) {
        try {
            FileOutputStream(file, true).use { fos ->
                fos.write(logMessage.toByteArray(Charsets.UTF_8))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}