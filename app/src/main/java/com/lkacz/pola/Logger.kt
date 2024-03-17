package com.lkacz.pola

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class Logger private constructor(private val context: Context) {
    private lateinit var fileOperations: FileOperations
    private var isBackupCreated = false
    private val excelOperations = ExcelOperations()
    private val sharedPref: SharedPreferences = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

    private lateinit var studyId: String
    private lateinit var fileName: String
    private lateinit var mainFolder: File
    private lateinit var file: File

    fun setupLogger() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        studyId = sharedPref.getString("STUDY_ID", "") ?: ""

        fileName = if (studyId.isEmpty()) {
            "output_$timeStamp.csv"
        } else {
            "${studyId}_output_$timeStamp.csv"
        }

        val publicStorage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        mainFolder = File(publicStorage, "PoLA_Data")
        file = File(mainFolder, fileName)
        fileOperations = FileOperations(mainFolder, file)
        fileOperations.createFileAndFolder()
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun logToCSV(cells: Array<String?>) {
        if (!this::fileOperations.isInitialized) {
            return
        }

        val dateTime = getCurrentDateTime().split(" ")
        val date = dateTime[0]
        val time = dateTime[1]
        val sanitizedCells = cells.map { it ?: "" }
        val logMessage = "$date\t$time\t${sanitizedCells.joinToString("\t")}\n"
        fileOperations.writeToCSV(logMessage)
    }

    fun logInstructionFragment(header: String, body: String) {
        logToCSV(arrayOf(header, body, null, null, null, null, null, null))
    }

    fun logTimerFragment(header: String, body: String, timeInSeconds: Int, other: String? = null) {
        val cells = arrayOf(header, body, null, null, null, timeInSeconds.toString(), null, other)
        logToCSV(cells)
    }

    fun logScaleFragment(header: String, intro: String, item: String, responseNumber: Int, responseText: String) {
        logToCSV(arrayOf(header, intro, item, responseNumber.toString(), responseText, null, null, null))
    }

    fun logInputFieldFragment(header: String, intro: String, item: String, response: String, isNumeric: Boolean) {
        val responseNumber = if (isNumeric) response else null
        val responseText = if (!isNumeric) response else null
        logToCSV(arrayOf(header, intro, item, responseNumber, responseText, null, null, null))
    }

    fun backupLogFile() {
        if (!file.exists()) {
            return
        }

        try {
            val mainFolderXlsxFileName = fileName.replace(".csv", ".xlsx")
            val mainFolderXlsxFile = File(mainFolder, mainFolderXlsxFileName)
            excelOperations.createXlsxBackup(mainFolderXlsxFile, file, BufferedReader(StringReader(ProtocolManager.originalProtocol ?: "")), BufferedReader(StringReader(ProtocolManager.finalProtocol ?: "")))

            if (isBackupCreated) return

            val backupFolder = File(mainFolder.parentFile, backupFolderName)
            if (!backupFolder.exists()) {
                backupFolder.mkdirs()
            }

            val backupFileNameCsv = fileName.replace(".csv", "_backup.csv")
            val backupFileCsv = File(backupFolder, backupFileNameCsv)
            file.copyTo(backupFileCsv, overwrite = true)

            val backupFileNameXlsx = fileName.replace(".csv", "_backup.xlsx")
            val backupFileXlsx = File(backupFolder, backupFileNameXlsx)
            excelOperations.createXlsxBackup(backupFileXlsx, file, BufferedReader(StringReader(ProtocolManager.originalProtocol ?: "")), BufferedReader(StringReader(ProtocolManager.finalProtocol ?: "")))

            isBackupCreated = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: Logger? = null
        private const val backupFolderName = "PoLA_Backup"

        fun getInstance(context: Context): Logger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Logger(context).also { INSTANCE = it }
            }
        }

        fun resetInstance() {
            INSTANCE = null
        }
    }
}
