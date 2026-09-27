package com.docopener.universal.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

enum class PageOrientation {
    PORTRAIT,
    LANDSCAPE,
    AUTO_MATCH_IMAGE
}

enum class PageMargin(val valueDp: Int) {
    NONE(0),
    SMALL(16),
    STANDARD(32)
}

data class PdfCreationOptions(
    val title: String = "Converted_Document",
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val margin: PageMargin = PageMargin.SMALL,
    val compressionQuality: Int = 85 // 0-100
)

class PdfCreatorEngine(private val context: Context) {

    // Standard A4 dimensions in PostScript points (72 points per inch) -> 595 x 842
    companion object {
        const val A4_WIDTH = 595
        const val A4_HEIGHT = 842
    }

    /**
     * Converts a list of image URIs (JPG, PNG, WEBP, etc.) into a multi-page PDF document offline.
     */
    suspend fun createPdfFromImages(
        imageUris: List<Uri>,
        options: PdfCreationOptions
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            if (imageUris.isEmpty()) {
                throw IllegalArgumentException("At least one image is required to generate a PDF")
            }

            val pdfDocument = PdfDocument()

            imageUris.forEachIndexed { index, imageUri ->
                val bitmap = decodeSampledBitmapFromUri(imageUri)
                    ?: throw IllegalStateException("Could not load image ${index + 1}")

                val (pageWidth, pageHeight) = calculatePageDimensions(bitmap, options.orientation)

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Fill background white
                canvas.drawColor(Color.WHITE)

                val marginPts = options.margin.valueDp.toFloat()
                val availableWidth = pageWidth - (marginPts * 2)
                val availableHeight = pageHeight - (marginPts * 2)

                // Calculate aspect ratio fit
                val scale = minOf(
                    availableWidth / bitmap.width.toFloat(),
                    availableHeight / bitmap.height.toFloat()
                )

                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale

                val left = marginPts + (availableWidth - scaledWidth) / 2f
                val top = marginPts + (availableHeight - scaledHeight) / 2f

                val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
                val paint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                }

                canvas.drawBitmap(bitmap, null, destRect, paint)
                pdfDocument.finishPage(page)

                // Recycle bitmap memory
                bitmap.recycle()
            }

            // Save PDF to device storage
            val outputUri = savePdfToStorage(pdfDocument, options.title)
            pdfDocument.close()
            outputUri
        }
    }

    /**
     * Converts text/notes into a formatted multi-page PDF document offline.
     */
    suspend fun createPdfFromText(
        title: String,
        bodyText: String,
        options: PdfCreationOptions
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val pdfDocument = PdfDocument()
            val pageWidth = A4_WIDTH
            val pageHeight = A4_HEIGHT
            val margin = 40f

            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val textPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 12f
                isAntiAlias = true
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            var yPosition = margin + 30f

            // Draw Document Title on First Page
            if (title.isNotBlank()) {
                canvas.drawText(title, margin, yPosition, titlePaint)
                yPosition += 35f
                val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
                canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
                yPosition += 25f
            }

            val lineHeight = 18f
            val maxLineWidth = pageWidth - (margin * 2)

            val lines = bodyText.lines()
            for (line in lines) {
                // Handle text wrapping
                val words = line.split(" ")
                var currentLineText = StringBuilder()

                for (word in words) {
                    val candidate = if (currentLineText.isEmpty()) word else "$currentLineText $word"
                    val textWidth = textPaint.measureText(candidate)

                    if (textWidth <= maxLineWidth) {
                        currentLineText = StringBuilder(candidate)
                    } else {
                        if (yPosition + lineHeight > pageHeight - margin) {
                            // Finish current page and start a new page
                            pdfDocument.finishPage(page)
                            pageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            canvas.drawColor(Color.WHITE)
                            yPosition = margin + 20f
                        }

                        canvas.drawText(currentLineText.toString(), margin, yPosition, textPaint)
                        yPosition += lineHeight
                        currentLineText = StringBuilder(word)
                    }
                }

                if (currentLineText.isNotEmpty()) {
                    if (yPosition + lineHeight > pageHeight - margin) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        canvas.drawColor(Color.WHITE)
                        yPosition = margin + 20f
                    }
                    canvas.drawText(currentLineText.toString(), margin, yPosition, textPaint)
                    yPosition += lineHeight
                }
            }

            pdfDocument.finishPage(page)

            val finalTitle = if (title.isNotBlank()) title.replace(" ", "_") else "Document_${System.currentTimeMillis()}"
            val outputUri = savePdfToStorage(pdfDocument, finalTitle)
            pdfDocument.close()
            outputUri
        }
    }

    private fun calculatePageDimensions(bitmap: Bitmap, orientation: PageOrientation): Pair<Int, Int> {
        return when (orientation) {
            PageOrientation.PORTRAIT -> Pair(A4_WIDTH, A4_HEIGHT)
            PageOrientation.LANDSCAPE -> Pair(A4_HEIGHT, A4_WIDTH)
            PageOrientation.AUTO_MATCH_IMAGE -> {
                if (bitmap.width > bitmap.height) {
                    Pair(A4_HEIGHT, A4_WIDTH)
                } else {
                    Pair(A4_WIDTH, A4_HEIGHT)
                }
            }
        }
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, maxDimension: Int = 2048): Bitmap? {
        val stream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(stream, null, options)
        stream.close()

        var inSampleSize = 1
        if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
            val halfHeight: Int = options.outHeight / 2
            val halfWidth: Int = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }

        val decodeStream = context.contentResolver.openInputStream(uri) ?: return null
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
        decodeStream.close()
        return bitmap
    }

    private fun savePdfToStorage(pdfDoc: PdfDocument, filename: String): Uri {
        val safeName = filename.replace(Regex("[^a-zA-Z0-9_.-]"), "_") + ".pdf"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/OmniDoc")
            }

            val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                ?: throw IllegalStateException("Could not create MediaStore entry for PDF")

            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdfDoc.writeTo(out)
            }
            return uri
        } else {
            val docsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "OmniDoc")
            if (!docsDir.exists()) docsDir.mkdirs()
            val file = File(docsDir, safeName)
            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            return Uri.fromFile(file)
        }
    }
}
