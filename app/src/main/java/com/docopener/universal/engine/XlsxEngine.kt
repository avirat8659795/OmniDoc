package com.docopener.universal.engine

import android.content.Context
import android.net.Uri
import com.docopener.universal.domain.ExcelSheet
import com.docopener.universal.domain.ParsedExcelDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class XlsxEngine(private val context: Context) {

    suspend fun parseXlsx(uri: Uri, title: String): Result<ParsedExcelDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val sharedStrings = mutableListOf<String>()
            val sheetEntries = mutableListOf<Pair<String, ByteArray>>() // sheetName -> xml bytes
            val sheetNames = mutableListOf<String>()

            // First pass: Read sharedStrings and buffer worksheet xmls
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val zip = ZipInputStream(stream)
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "xl/sharedStrings.xml" -> {
                            parseSharedStrings(zip, sharedStrings)
                        }
                        entry.name == "xl/workbook.xml" -> {
                            parseWorkbookSheetNames(zip, sheetNames)
                        }
                        entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml") -> {
                            sheetEntries.add(entry.name to zip.readBytes())
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // Second pass: Parse worksheets
            val sheets = mutableListOf<ExcelSheet>()
            sheetEntries.forEachIndexed { index, pair ->
                val name = sheetNames.getOrNull(index) ?: "Sheet ${index + 1}"
                val rows = parseSheetXml(pair.second.inputStream(), sharedStrings)
                sheets.add(ExcelSheet(name = name, rows = rows))
            }

            if (sheets.isEmpty()) {
                sheets.add(ExcelSheet("Sheet 1", emptyList()))
            }

            ParsedExcelDocument(title = title, sheets = sheets)
        }
    }

    suspend fun parseCsv(uri: Uri, title: String): Result<ParsedExcelDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = mutableListOf<List<String>>()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).useLines { lines ->
                    lines.forEach { line ->
                        // Standard CSV split with quotes handling
                        val tokens = parseCsvLine(line)
                        if (tokens.isNotEmpty()) {
                            rows.add(tokens)
                        }
                    }
                }
            }
            ParsedExcelDocument(title = title, sheets = listOf(ExcelSheet("CSV Data", rows)))
        }
    }

    private fun parseSharedStrings(stream: InputStream, list: MutableList<String>) {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var currentText = StringBuilder()
        var inStringItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") {
                        inStringItem = true
                        currentText = StringBuilder()
                    } else if (parser.name == "t" && inStringItem) {
                        currentText.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si") {
                        inStringItem = false
                        list.add(currentText.toString())
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseWorkbookSheetNames(stream: InputStream, sheetNames: MutableList<String>) {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                val name = parser.getAttributeValue(null, "name")
                if (name != null) sheetNames.add(name)
            }
            eventType = parser.next()
        }
    }

    private fun parseSheetXml(stream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        val resultRows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        var cellType: String? = null
        var cellValue = ""
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableListOf()
                        }
                        "c" -> {
                            cellType = parser.getAttributeValue(null, "t")
                            cellValue = ""
                        }
                        "v" -> {
                            cellValue = parser.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            val resolvedText = if (cellType == "s") {
                                val sIndex = cellValue.toIntOrNull() ?: -1
                                if (sIndex in sharedStrings.indices) sharedStrings[sIndex] else cellValue
                            } else {
                                cellValue
                            }
                            currentRow.add(resolvedText)
                        }
                        "row" -> {
                            if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                                resultRows.add(currentRow.toList())
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return resultRows
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }
}
