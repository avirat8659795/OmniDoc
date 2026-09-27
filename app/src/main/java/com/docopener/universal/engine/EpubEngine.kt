package com.docopener.universal.engine

import android.content.Context
import android.net.Uri
import android.text.Html
import com.docopener.universal.domain.EpubChapter
import com.docopener.universal.domain.ParsedEpubDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class EpubEngine(private val context: Context) {

    suspend fun parse(uri: Uri, title: String): Result<ParsedEpubDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val chapters = mutableListOf<EpubChapter>()
            var docTitle = title
            var author = ""

            val htmlEntries = mutableListOf<Pair<String, ByteArray>>()

            context.contentResolver.openInputStream(uri)?.use { stream ->
                val zip = ZipInputStream(stream)
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm")) {
                        htmlEntries.add(entry.name to zip.readBytes())
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            htmlEntries.sortBy { it.first }

            htmlEntries.forEachIndexed { idx, pair ->
                val html = String(pair.second, Charsets.UTF_8)
                val plain = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                if (plain.isNotBlank()) {
                    val chapterTitle = "Chapter ${idx + 1}"
                    chapters.add(EpubChapter(title = chapterTitle, htmlContent = html, plainText = plain))
                }
            }

            if (chapters.isEmpty()) {
                chapters.add(EpubChapter("Book Content", "", "No readable chapters found in this EPUB archive."))
            }

            ParsedEpubDocument(title = docTitle, author = author, chapters = chapters)
        }
    }
}
