package com.docopener.universal.engine

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

class TextEngine(private val context: Context) {

    suspend fun readText(uri: Uri, maxLines: Int = 5000): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val sb = java.lang.StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                // Try UTF-8 first, then fallback to ISO-8859-1
                val reader = BufferedReader(InputStreamReader(stream, Charset.forName("UTF-8")))
                var lineCount = 0
                reader.useLines { lines ->
                    for (line in lines) {
                        sb.append(line).append("\n")
                        lineCount++
                        if (lineCount >= maxLines) {
                            sb.append("\n... [Preview truncated after $maxLines lines]")
                            break
                        }
                    }
                }
            }
            sb.toString()
        }
    }
}
