package com.docopener.universal.domain

import android.net.Uri
import androidx.compose.ui.graphics.Color

enum class DocumentType(
    val extension: String,
    val displayName: String,
    val category: DocumentCategory,
    val color: Long
) {
    PDF("pdf", "PDF Document", DocumentCategory.PDF, 0xFFE53935),
    DOCX("docx", "Word Document", DocumentCategory.WORD, 0xFF1E88E5),
    DOC("doc", "Legacy Word Doc", DocumentCategory.WORD, 0xFF1976D2),
    XLSX("xlsx", "Excel Spreadsheet", DocumentCategory.EXCEL, 0xFF43A047),
    XLS("xls", "Legacy Excel Sheet", DocumentCategory.EXCEL, 0xFF388E3C),
    CSV("csv", "CSV Data", DocumentCategory.EXCEL, 0xFF2E7D32),
    PPTX("pptx", "PowerPoint Presentation", DocumentCategory.PPT, 0xFFFB8C00),
    PPT("ppt", "Legacy PowerPoint", DocumentCategory.PPT, 0xFFF57C00),
    TXT("txt", "Plain Text", DocumentCategory.TEXT, 0xFF546E7A),
    MD("md", "Markdown Document", DocumentCategory.TEXT, 0xFF37474F),
    EPUB("epub", "EPUB eBook", DocumentCategory.EPUB, 0xFF8E24AA),
    RTF("rtf", "Rich Text Format", DocumentCategory.WORD, 0xFF0288D1),
    CODE("code", "Code / Data", DocumentCategory.TEXT, 0xFF455A64),
    UNKNOWN("file", "Unknown File", DocumentCategory.OTHER, 0xFF757575);

    companion object {
        fun fromExtension(ext: String?): DocumentType {
            val lower = ext?.lowercase()?.removePrefix(".") ?: ""
            return when (lower) {
                "pdf" -> PDF
                "docx" -> DOCX
                "doc" -> DOC
                "xlsx" -> XLSX
                "xls" -> XLS
                "csv" -> CSV
                "pptx" -> PPTX
                "ppt" -> PPT
                "txt", "log" -> TXT
                "md", "markdown" -> MD
                "epub" -> EPUB
                "rtf" -> RTF
                "json", "xml", "html", "htm", "js", "ts", "py", "kt", "java", "cpp", "c", "css", "yaml", "yml" -> CODE
                else -> UNKNOWN
            }
        }
    }
}

enum class DocumentCategory(val label: String) {
    ALL("All"),
    PDF("PDF"),
    WORD("Word"),
    EXCEL("Excel"),
    PPT("PPT"),
    TEXT("Text"),
    EPUB("eBooks"),
    OTHER("Other")
}

data class DocumentItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val type: DocumentType,
    val path: String? = null,
    val isFavorite: Boolean = false,
    val lastReadPosition: Int = 0
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
            val df = java.text.DecimalFormat("#,##0.#")
            return "${df.format(sizeBytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
        }

    val formattedDate: String
        get() {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(lastModified))
        }
}

// DOCX Parsed Structure
data class DocxParagraph(
    val text: String,
    val isHeading: Boolean = false,
    val headingLevel: Int = 0,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isBullet: Boolean = false,
    val runs: List<DocxRun> = emptyList()
)

data class DocxRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontSizePt: Float = 14f,
    val colorHex: String? = null
)

data class DocxTable(
    val rows: List<List<String>>
)

sealed class DocxElement {
    data class ParagraphElement(val paragraph: DocxParagraph) : DocxElement()
    data class TableElement(val table: DocxTable) : DocxElement()
}

data class ParsedDocxDocument(
    val title: String,
    val elements: List<DocxElement>
)

// XLSX Parsed Structure
data class ExcelSheet(
    val name: String,
    val rows: List<List<String>>
)

data class ParsedExcelDocument(
    val title: String,
    val sheets: List<ExcelSheet>
)

// PPTX Parsed Structure
data class PptxSlide(
    val slideNumber: Int,
    val title: String,
    val bulletPoints: List<String>,
    val notes: String = ""
)

data class ParsedPptxDocument(
    val title: String,
    val slides: List<PptxSlide>
)

// EPUB Parsed Structure
data class EpubChapter(
    val title: String,
    val htmlContent: String,
    val plainText: String
)

data class ParsedEpubDocument(
    val title: String,
    val author: String = "",
    val chapters: List<EpubChapter>
)
