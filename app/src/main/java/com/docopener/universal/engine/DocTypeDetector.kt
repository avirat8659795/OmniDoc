package com.docopener.universal.engine

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.docopener.universal.domain.DocumentType
import java.io.File
import java.io.InputStream

object DocTypeDetector {

    fun detectType(context: Context, uri: Uri): DocumentType {
        // 1. Try file name extension from URI or ContentResolver
        val fileName = getFileName(context, uri)
        val extFromName = fileName.substringAfterLast('.', "").lowercase()
        if (extFromName.isNotEmpty()) {
            val type = DocumentType.fromExtension(extFromName)
            if (type != DocumentType.UNKNOWN) return type
        }

        // 2. Try MimeType from ContentResolver
        val mime = context.contentResolver.getType(uri)
        if (mime != null) {
            val typeFromMime = fromMimeType(mime)
            if (typeFromMime != DocumentType.UNKNOWN) return typeFromMime
        }

        // 3. Fallback: Sniff Magic Bytes
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                return detectFromMagicBytes(stream)
            }
        } catch (_: Exception) {}

        return DocumentType.UNKNOWN
    }

    fun getFileName(context: Context, uri: Uri): String {
        var result = ""
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index) ?: ""
                    }
                }
            }
        }
        if (result.isEmpty()) {
            result = uri.lastPathSegment ?: "document"
            result = result.substringAfterLast('/')
        }
        return result
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (it.moveToFirst() && sizeIndex != -1) {
                        return it.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) {}
        } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            uri.path?.let {
                val f = File(it)
                if (f.exists()) return f.length()
            }
        }
        return 0L
    }

    private fun fromMimeType(mime: String): DocumentType {
        return when {
            mime.contains("pdf") -> DocumentType.PDF
            mime.contains("wordprocessingml") || mime.contains("msword") -> DocumentType.DOCX
            mime.contains("spreadsheetml") || mime.contains("ms-excel") -> DocumentType.XLSX
            mime.contains("presentationml") || mime.contains("powerpoint") -> DocumentType.PPTX
            mime.contains("epub") -> DocumentType.EPUB
            mime.contains("rtf") -> DocumentType.RTF
            mime.contains("csv") -> DocumentType.CSV
            mime.startsWith("text/") -> DocumentType.TXT
            mime.contains("json") || mime.contains("xml") -> DocumentType.CODE
            else -> DocumentType.UNKNOWN
        }
    }

    private fun detectFromMagicBytes(stream: InputStream): DocumentType {
        val header = ByteArray(8)
        val read = stream.read(header, 0, 8)
        if (read < 4) return DocumentType.UNKNOWN

        // PDF: %PDF (0x25, 0x50, 0x44, 0x46)
        if (header[0] == 0x25.toByte() && header[1] == 0x50.toByte() && header[2] == 0x44.toByte() && header[3] == 0x46.toByte()) {
            return DocumentType.PDF
        }

        // RTF: {\rtf (0x7B, 0x5C, 0x72, 0x74, 0x66)
        if (header[0] == 0x7B.toByte() && header[1] == 0x5C.toByte() && header[2] == 0x72.toByte() && header[3] == 0x74.toByte()) {
            return DocumentType.RTF
        }

        // ZIP based formats (DOCX, XLSX, PPTX, EPUB): PK.. (0x50, 0x4B, 0x03, 0x04)
        if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) {
            // Default to DOCX or handle within ZIP analyzer
            return DocumentType.DOCX
        }

        return DocumentType.UNKNOWN
    }
}
