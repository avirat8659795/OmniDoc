package com.docopener.universal.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.docopener.universal.domain.DocumentCategory
import com.docopener.universal.domain.DocumentItem
import com.docopener.universal.domain.DocumentType

@Composable
fun UniversalViewerContainer(
    doc: DocumentItem,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val onShare: () -> Unit = {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, doc.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
        } catch (_: Exception) {}
    }

    when (doc.type.category) {
        DocumentCategory.PDF -> {
            PdfViewerScreen(
                uri = doc.uri,
                title = doc.name,
                onBack = onBack,
                onShare = onShare
            )
        }
        DocumentCategory.WORD -> {
            if (doc.type == DocumentType.RTF) {
                TextViewerScreen(
                    uri = doc.uri,
                    title = doc.name,
                    type = doc.type,
                    onBack = onBack,
                    onShare = onShare
                )
            } else {
                DocxViewerScreen(
                    uri = doc.uri,
                    title = doc.name,
                    onBack = onBack,
                    onShare = onShare
                )
            }
        }
        DocumentCategory.EXCEL -> {
            ExcelViewerScreen(
                uri = doc.uri,
                title = doc.name,
                type = doc.type,
                onBack = onBack,
                onShare = onShare
            )
        }
        DocumentCategory.PPT -> {
            PptxViewerScreen(
                uri = doc.uri,
                title = doc.name,
                onBack = onBack,
                onShare = onShare
            )
        }
        DocumentCategory.EPUB -> {
            EpubViewerScreen(
                uri = doc.uri,
                title = doc.name,
                onBack = onBack,
                onShare = onShare
            )
        }
        DocumentCategory.TEXT, DocumentCategory.OTHER -> {
            TextViewerScreen(
                uri = doc.uri,
                title = doc.name,
                type = doc.type,
                onBack = onBack,
                onShare = onShare
            )
        }
        DocumentCategory.ALL -> {
            TextViewerScreen(
                uri = doc.uri,
                title = doc.name,
                type = doc.type,
                onBack = onBack,
                onShare = onShare
            )
        }
    }
}
