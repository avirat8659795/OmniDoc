package com.docopener.universal.engine

import android.content.Context
import android.net.Uri
import com.docopener.universal.domain.DocxElement
import com.docopener.universal.domain.DocxParagraph
import com.docopener.universal.domain.DocxRun
import com.docopener.universal.domain.DocxTable
import com.docopener.universal.domain.ParsedDocxDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class DocxEngine(private val context: Context) {

    suspend fun parse(uri: Uri, title: String): Result<ParsedDocxDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open document stream")

            var documentXmlStream: InputStream? = null
            val zip = ZipInputStream(inputStream)
            var entry: ZipEntry? = zip.nextEntry
            val elements = mutableListOf<DocxElement>()

            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    parseDocumentXml(zip, elements)
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            zip.close()

            ParsedDocxDocument(title = title, elements = elements)
        }
    }

    private fun parseDocumentXml(stream: InputStream, elements: MutableList<DocxElement>) {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var inTable = false
        var currentTableRows = mutableListOf<List<String>>()
        var currentRowCells = mutableListOf<String>()
        var currentCellText = StringBuilder()

        var inParagraph = false
        var currentRuns = mutableListOf<DocxRun>()
        var currentRunText = StringBuilder()
        var isBold = false
        var isItalic = false
        var isUnderline = false
        var isHeading = false
        var isBullet = false
        var headingLevel = 0

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tag) {
                        "w:tbl" -> {
                            inTable = true
                            currentTableRows = mutableListOf()
                        }
                        "w:tr" -> {
                            currentRowCells = mutableListOf()
                        }
                        "w:tc" -> {
                            currentCellText = StringBuilder()
                        }
                        "w:p" -> {
                            inParagraph = true
                            currentRuns = mutableListOf()
                            isHeading = false
                            isBullet = false
                            headingLevel = 0
                        }
                        "w:pStyle" -> {
                            val valAttr = parser.getAttributeValue(null, "w:val") ?: ""
                            if (valAttr.startsWith("Heading", ignoreCase = true) || valAttr.startsWith("Title", ignoreCase = true)) {
                                isHeading = true
                                headingLevel = valAttr.filter { it.isDigit() }.toIntOrNull() ?: 1
                            }
                        }
                        "w:numPr" -> {
                            isBullet = true
                        }
                        "w:r" -> {
                            currentRunText = StringBuilder()
                            isBold = false
                            isItalic = false
                            isUnderline = false
                        }
                        "w:b" -> {
                            val valAttr = parser.getAttributeValue(null, "w:val")
                            isBold = valAttr == null || valAttr == "1" || valAttr.equals("true", true)
                        }
                        "w:i" -> {
                            val valAttr = parser.getAttributeValue(null, "w:val")
                            isItalic = valAttr == null || valAttr == "1" || valAttr.equals("true", true)
                        }
                        "w:u" -> {
                            isUnderline = true
                        }
                        "w:t" -> {
                            val text = parser.nextText()
                            if (text.isNotEmpty()) {
                                currentRunText.append(text)
                                if (inTable) {
                                    currentCellText.append(text).append(" ")
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tag) {
                        "w:r" -> {
                            if (currentRunText.isNotEmpty()) {
                                currentRuns.add(
                                    DocxRun(
                                        text = currentRunText.toString(),
                                        isBold = isBold,
                                        isItalic = isItalic,
                                        isUnderline = isUnderline
                                    )
                                )
                            }
                        }
                        "w:p" -> {
                            inParagraph = false
                            val fullText = currentRuns.joinToString("") { it.text }
                            if (fullText.isNotBlank() && !inTable) {
                                elements.add(
                                    DocxElement.ParagraphElement(
                                        DocxParagraph(
                                            text = fullText,
                                            isHeading = isHeading,
                                            headingLevel = headingLevel,
                                            isBold = isBold,
                                            isItalic = isItalic,
                                            isBullet = isBullet,
                                            runs = currentRuns.toList()
                                        )
                                    )
                                )
                            }
                        }
                        "w:tc" -> {
                            currentRowCells.add(currentCellText.toString().trim())
                        }
                        "w:tr" -> {
                            if (currentRowCells.isNotEmpty()) {
                                currentTableRows.add(currentRowCells.toList())
                            }
                        }
                        "w:tbl" -> {
                            inTable = false
                            if (currentTableRows.isNotEmpty()) {
                                elements.add(DocxElement.TableElement(DocxTable(currentTableRows.toList())))
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }
}
