package com.docopener.universal.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.docopener.universal.domain.DocumentItem
import com.docopener.universal.domain.DocumentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class RecentDocsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("omni_docs_prefs", Context.MODE_PRIVATE)

    private val _recentDocs = MutableStateFlow<List<DocumentItem>>(emptyList())
    val recentDocs: StateFlow<List<DocumentItem>> = _recentDocs.asStateFlow()

    private val _favoriteUris = MutableStateFlow<Set<String>>(emptySet())
    val favoriteUris: StateFlow<Set<String>> = _favoriteUris.asStateFlow()

    init {
        loadRecents()
        loadFavorites()
    }

    private fun loadFavorites() {
        val set = prefs.getStringSet("favorite_uris", emptySet()) ?: emptySet()
        _favoriteUris.value = set
    }

    fun toggleFavorite(uri: Uri) {
        val uriStr = uri.toString()
        val current = _favoriteUris.value.toMutableSet()
        if (current.contains(uriStr)) {
            current.remove(uriStr)
        } else {
            current.add(uriStr)
        }
        prefs.edit().putStringSet("favorite_uris", current).apply()
        _favoriteUris.value = current
    }

    fun isFavorite(uri: Uri): Boolean {
        return _favoriteUris.value.contains(uri.toString())
    }

    private fun loadRecents() {
        val jsonStr = prefs.getString("recent_documents", "[]") ?: "[]"
        val list = mutableListOf<DocumentItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    DocumentItem(
                        uri = Uri.parse(obj.getString("uri")),
                        name = obj.getString("name"),
                        sizeBytes = obj.optLong("size", 0L),
                        lastModified = obj.optLong("date", System.currentTimeMillis()),
                        type = DocumentType.valueOf(obj.optString("type", DocumentType.UNKNOWN.name)),
                        path = obj.optString("path", null),
                        lastReadPosition = obj.optInt("lastPage", 0)
                    )
                )
            }
        } catch (_: Exception) {}
        _recentDocs.value = list
    }

    fun addRecentDocument(item: DocumentItem) {
        val current = _recentDocs.value.filterNot { it.uri == item.uri }.toMutableList()
        current.add(0, item)
        // Keep max 50 recent documents
        val trimmed = if (current.size > 50) current.take(50) else current
        _recentDocs.value = trimmed

        val jsonArray = JSONArray()
        trimmed.forEach { doc ->
            val obj = JSONObject().apply {
                put("uri", doc.uri.toString())
                put("name", doc.name)
                put("size", doc.sizeBytes)
                put("date", doc.lastModified)
                put("type", doc.type.name)
                put("path", doc.path)
                put("lastPage", doc.lastReadPosition)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("recent_documents", jsonArray.toString()).apply()
    }
}
