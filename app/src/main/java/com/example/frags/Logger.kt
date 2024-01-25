package com.example.frags

import android.content.Context
import android.os.Build
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class Logger private constructor(context: Context) {

    private val fileOperations: FileOperations = FileOperations(mainFolder, file)
    private var isBackupCreated = false
    private val excelOperations = ExcelOperations()

    init {
        fileOperations.createFileAndFolder()
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun logToCSV(cells: Array<String?>) {
        val dateTime = getCurrentDateTime().split(" ")
        val date = dateTime[0]
        val time = dateTime[1]
        val sanitizedCells = cells.map { it ?: "" }
        val logMessage = "$date\t$time\t${sanitizedCells.joinToString("\t")}\n"
        fileOperations.writeToCSV(logMessage)
    }

    private fun addSheetFromBufferedReader(workbook: XSSFWorkbook, sheetName: String, bufferedReader: BufferedReader) {
        val sheet = workbook.createSheet(sheetName)
        var rowIndex = 0
        bufferedReader.forEachLine { line ->
            val row = sheet.createRow(rowIndex++)
            line.split("\t").forEachIndexed { cellIndex, cellContent ->
                val cell = row.createCell(cellIndex)
                cell.setCellValue(cellContent)
            }
        }
    }

    fun backupLogFile() {
        if (isBackupCreated || !file.exists()) {
            return
        }

        if (ProtocolManager.originalProtocol == null || ProtocolManager.finalProtocol == null) {
            // Log an error or return
            return
        }

        try {
            val backupFolderName = "Backup"
            val backupFolder = File(mainFolder, backupFolderName)
            if (!backupFolder.exists()) {
                backupFolder.mkdirs()
            }

            val backupFileNameCsv = fileName.replace(".csv", "_backup.csv")
            val backupFileCsv = File(backupFolder, backupFileNameCsv)
            val backupFileNameXlsx = fileName.replace(".csv", "_backup.xlsx")
            val backupFileXlsx = File(backupFolder, backupFileNameXlsx)

            file.copyTo(backupFileCsv, overwrite = true)

            excelOperations.createXlsxBackup(
                backupFileXlsx,
                file,
                BufferedReader(StringReader(ProtocolManager.originalProtocol)),
                BufferedReader(StringReader(ProtocolManager.finalProtocol))
            )

            isBackupCreated = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun logInstructionFragment(header: String, body: String) {
        logToCSV(arrayOf(header, body, null, null, null, null, null, null))
    }

    fun logFixedInstructionFragment(header: String, body: String, timeInSeconds: Int, other: String? = null) {
        val cells = arrayOf(header, body, null, null, null, timeInSeconds.toString(), null, other)
        logToCSV(cells)
    }

    fun logQuestionnaireFragment(header: String, intro: String, item: String, responseNumber: Int, responseText: String) {
        logToCSV(arrayOf(header, intro, item, responseNumber.toString(), responseText, null, null, null))
    }

    fun logSettingsFragment(header: String, intro: String, item: String, response: String, isNumeric: Boolean) {
        val responseNumber = if (isNumeric) response else null
        val responseText = if (!isNumeric) response else null
        logToCSV(arrayOf(header, intro, item, responseNumber, responseText, null, null, null))
    }

    fun logOther(message: String) {
        logToCSV(arrayOf(null, null, null, null, null, null, null, message))
    }

    companion object {
        @Volatile
        private var INSTANCE: Logger? = null
        private const val mainFolderName = "Wearablelab"
        private lateinit var timeStamp: String
        private lateinit var identifier: String
        private lateinit var fileName: String
        private lateinit var mainFolderPath: File
        private lateinit var mainFolder: File
        private lateinit var file: File

        fun getInstance(context: Context): Logger {
            if (INSTANCE == null) {
                timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                identifier = Build.ID.take(4)
                fileName = "${identifier}_output_$timeStamp.csv"
                mainFolderPath = context.getExternalFilesDir(null)!!
                mainFolder = File(mainFolderPath, mainFolderName)
                file = File(mainFolder, fileName)
            }

            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Logger(context).also { INSTANCE = it }
            }
        }

        fun resetInstance() {
            INSTANCE = null
        }
    }
}
