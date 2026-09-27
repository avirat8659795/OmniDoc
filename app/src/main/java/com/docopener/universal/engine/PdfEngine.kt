package com.docopener.universal.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfEngine(private val context: Context) : AutoCloseable {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var tempFile: File? = null

    var pageCount: Int = 0
        private set

    suspend fun open(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            close() // Clean up any previous session

            val pfd: ParcelFileDescriptor = if (uri.scheme == "content") {
                // For content:// URIs, copy to a temp cache file for random-access seeking required by PdfRenderer
                val temp = File.createTempFile("view_doc_", ".pdf", context.cacheDir)
                tempFile = temp
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(temp).use { output ->
                        input.copyTo(output)
                    }
                }
                ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                val file = File(uri.path ?: throw IllegalArgumentException("Invalid file URI"))
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }

            fileDescriptor = pfd
            val renderer = PdfRenderer(pfd)
            pdfRenderer = renderer
            pageCount = renderer.pageCount
            pageCount
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext null

        synchronized(renderer) {
            val page = renderer.openPage(pageIndex)
            val scale = targetWidth.toFloat() / page.width.toFloat()
            val targetHeight = (page.height * scale).toInt()

            val bitmap = Bitmap.createBitmap(
                targetWidth.coerceAtLeast(100),
                targetHeight.coerceAtLeast(100),
                Bitmap.Config.ARGB_8888
            )
            // Fill background white before rendering PDF page content
            bitmap.eraseColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        }
    }

    override fun close() {
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        pdfRenderer = null

        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}
        fileDescriptor = null

        try {
            tempFile?.delete()
        } catch (_: Exception) {}
        tempFile = null
        pageCount = 0
    }
}
