package com.docopener.universal.engine

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RtfEngine(private val context: Context) {

    suspend fun parseRtf(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            } ?: ""

            // Simple fast RTF control word stripping
            val sb = java.lang.StringBuilder()
            var inControlWord = false
            var i = 0
            while (i < raw.length) {
                val c = raw[i]
                when {
                    c == '\\' -> {
                        // Skip control word
                        i++
                        while (i < raw.length && (raw[i].isLetter() || raw[i].isDigit() || raw[i] == '-')) {
                            i++
                        }
                        if (i < raw.length && raw[i] == ' ') {
                            i++ // Skip trailing space after control word
                        }
                        continue
                    }
                    c == '{' || c == '}' -> {
                        // Grouping characters, skip
                    }
                    c == '\r' || c == '\n' -> {
                        sb.append('\n')
                    }
                    else -> {
                        sb.append(c)
                    }
                }
                i++
            }
            sb.toString().trim()
        }
    }
}
