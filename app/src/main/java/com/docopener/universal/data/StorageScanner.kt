package com.docopener.universal.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.docopener.universal.domain.DocumentCategory
import com.docopener.universal.domain.DocumentItem
import com.docopener.universal.domain.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StorageScanner(private val context: Context) {

    suspend fun scanDocuments(): List<DocumentItem> = withContext(Dispatchers.IO) {
        val documents = mutableListOf<DocumentItem>()
        val seenPaths = mutableSetOf<String>()

        // 1. Scan MediaStore Files
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA
        )

        // Supported document extensions
        val extensions = listOf(
            "pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt", "txt", "md", "csv", "epub", "rtf", "json", "xml"
        )
        val selectionArgs = extensions.map { "%.$it" }.toTypedArray()
        val selection = extensions.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DATA} LIKE ?" }

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Unknown"
                    val size = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol) * 1000 // Convert sec to ms
                    val path = cursor.getString(dataCol) ?: ""

                    if (path.isNotBlank()) seenPaths.add(path)

                    val uri = ContentUris.withAppendedId(collection, id)
                    val ext = name.substringAfterLast('.', "")
                    val docType = DocumentType.fromExtension(ext)

                    if (docType != DocumentType.UNKNOWN) {
                        documents.add(
                            DocumentItem(
                                uri = uri,
                                name = name,
                                sizeBytes = size,
                                lastModified = dateModified,
                                type = docType,
                                path = path
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Scan standard folders (Downloads, Documents) directly if accessible
        scanCommonDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), documents, seenPaths)
        scanCommonDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), documents, seenPaths)

        // Sort by newest modified first
        documents.sortedByDescending { it.lastModified }
    }

    private fun scanCommonDirectory(dir: File?, list: MutableList<DocumentItem>, seen: MutableSet<String>) {
        if (dir == null || !dir.exists() || !dir.isDirectory) return

        val supportedExts = setOf("pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt", "txt", "md", "csv", "epub", "rtf")
        dir.listFiles()?.forEach { file ->
            if (file.isFile && !seen.contains(file.absolutePath)) {
                val ext = file.extension.lowercase()
                if (supportedExts.contains(ext)) {
                    seen.add(file.absolutePath)
                    list.add(
                        DocumentItem(
                            uri = Uri.fromFile(file),
                            name = file.name,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                            type = DocumentType.fromExtension(ext),
                            path = file.absolutePath
                        )
                    )
                }
            }
        }
    }
}
