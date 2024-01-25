package com.example.frags

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ExcelOperations {

    fun createXlsxBackup(backupFileXlsx: File, logFile: File, originalProtocol: BufferedReader, finalProtocol: BufferedReader) {
        try {
            val workbook = XSSFWorkbook()

            addSheetFromLogFile(workbook, "Log", logFile)
            addSheetFromBufferedReader(workbook, "Original Protocol", originalProtocol)
            addSheetFromBufferedReader(workbook, "Used Protocol", finalProtocol)

            FileOutputStream(backupFileXlsx).use { fos ->
                workbook.write(fos)
            }

            workbook.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun addSheetFromLogFile(workbook: XSSFWorkbook, sheetName: String, logFile: File) {
        val sheet = workbook.createSheet(sheetName)
        val inputStream = logFile.inputStream()
        val lines = inputStream.bufferedReader().readLines()

        lines.forEachIndexed { index, line ->
            val row = sheet.createRow(index)
            line.split("\t").forEachIndexed { cellIndex, cellContent ->
                val cell = row.createCell(cellIndex)
                cell.setCellValue(cellContent)
            }
        }
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
}
