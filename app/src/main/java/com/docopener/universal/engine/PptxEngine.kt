package com.docopener.universal.engine

import android.content.Context
import android.net.Uri
import com.docopener.universal.domain.ParsedPptxDocument
import com.docopener.universal.domain.PptxSlide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class PptxEngine(private val context: Context) {

    suspend fun parse(uri: Uri, title: String): Result<ParsedPptxDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val slideEntries = mutableListOf<Pair<String, ByteArray>>()

            context.contentResolver.openInputStream(uri)?.use { stream ->
                val zip = ZipInputStream(stream)
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml")) {
                        slideEntries.add(entry.name to zip.readBytes())
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // Sort slides numerically by slide number (e.g. slide1, slide2, slide10)
            slideEntries.sortBy { pair ->
                pair.first.filter { it.isDigit() }.toIntOrNull() ?: 0
            }

            val slides = mutableListOf<PptxSlide>()
            slideEntries.forEachIndexed { index, pair ->
                val slide = parseSlideXml(pair.second.inputStream(), index + 1)
                slides.add(slide)
            }

            if (slides.isEmpty()) {
                slides.add(PptxSlide(1, "Empty Presentation", listOf("No slide content found.")))
            }

            ParsedPptxDocument(title = title, slides = slides)
        }
    }

    private fun parseSlideXml(stream: InputStream, slideNumber: Int): PptxSlide {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var title = ""
        val bulletPoints = mutableListOf<String>()
        var currentParagraph = StringBuilder()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "a:p") {
                        currentParagraph = StringBuilder()
                    } else if (parser.name == "a:t") {
                        val text = parser.nextText()
                        currentParagraph.append(text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "a:p") {
                        val text = currentParagraph.toString().trim()
                        if (text.isNotEmpty()) {
                            if (title.isEmpty()) {
                                title = text
                            } else {
                                bulletPoints.add(text)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return PptxSlide(
            slideNumber = slideNumber,
            title = if (title.isNotEmpty()) title else "Slide $slideNumber",
            bulletPoints = bulletPoints
        )
    }
}
