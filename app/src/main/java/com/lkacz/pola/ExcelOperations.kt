package com.lkacz.pola

import android.os.Build
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ExcelOperations {
    fun createXlsxBackup(
        backupFileXlsx: File,
        logFile: File,
        originalProtocol: BufferedReader,
        finalProtocol: BufferedReader,
    ) {
        try {
            XSSFWorkbook().use { workbook ->
                addSheetFromLogFile(workbook, "Log", logFile)
                addSheetFromBufferedReader(workbook, "Original Protocol", originalProtocol)
                addSheetFromBufferedReader(workbook, "Used Protocol", finalProtocol)
                addPhoneIdSheet(workbook)

                FileOutputStream(backupFileXlsx).use { fos ->
                    workbook.write(fos)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun addSheetFromLogFile(
        workbook: XSSFWorkbook,
        sheetName: String,
        logFile: File,
    ) {
        val sheet = workbook.createSheet(sheetName)
        logFile.inputStream().bufferedReader().use { reader ->
            var rowIndex = 0
            reader.forEachLine { line ->
                val row = sheet.createRow(rowIndex++)
                line.split("\t").forEachIndexed { cellIndex, cellContent ->
                    val cell = row.createCell(cellIndex)
                    cell.setCellValue(cellContent)
                }
            }
        }
    }

    private fun addSheetFromBufferedReader(
        workbook: XSSFWorkbook,
        sheetName: String,
        bufferedReader: BufferedReader,
    ) {
        val sheet = workbook.createSheet(sheetName)
        var rowIndex = 0
        bufferedReader.useLines { lines ->
            lines.forEach { line ->
                val row = sheet.createRow(rowIndex++)
                line.split("\t").forEachIndexed { cellIndex, cellContent ->
                    val cell = row.createCell(cellIndex)
                    cell.setCellValue(cellContent)
                }
            }
        }
    }

    private fun addPhoneIdSheet(workbook: XSSFWorkbook) {
        val sheet = workbook.createSheet("PhoneID")
        val row = sheet.createRow(0)
        row.createCell(0).setCellValue("IDENTIFIER")
        row.createCell(1).setCellValue(Build.ID)
        row.createCell(2).setCellValue("MANUFACTURER")
        row.createCell(3).setCellValue(Build.MANUFACTURER)
        row.createCell(4).setCellValue("MODEL")
        row.createCell(5).setCellValue(Build.MODEL)
    }
}
