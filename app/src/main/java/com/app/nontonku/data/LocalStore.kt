package com.app.nontonku.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Riwayat tontonan + daftar Tandai, disimpan lokal sebagai JSON. */
class LocalStore(context: Context) {
    private val file = File(context.filesDir, "library.json")
    private val json = Json { ignoreUnknownKeys = true }

    @kotlinx.serialization.Serializable
    private data class Library(
        val history: List<VideoItem> = emptyList(),
        val bookmarks: List<VideoItem> = emptyList(),
    )

    private var library: Library = load()

    private fun load(): Library = try {
        if (file.exists()) json.decodeFromString<Library>(file.readText()) else Library()
    } catch (e: Exception) {
        Library()
    }

    private fun save() {
        try { file.writeText(json.encodeToString(library)) } catch (e: Exception) { }
    }

    fun history(): List<VideoItem> = library.history
    fun bookmarks(): List<VideoItem> = library.bookmarks
    fun isBookmarked(id: String): Boolean = library.bookmarks.any { it.id == id }

    fun addHistory(item: VideoItem) {
        val rest = library.history.filter { it.id != item.id }
        library = library.copy(history = (listOf(item) + rest).take(100))
        save()
    }

    fun toggleBookmark(item: VideoItem): Boolean {
        return if (isBookmarked(item.id)) {
            library = library.copy(bookmarks = library.bookmarks.filter { it.id != item.id })
            save()
            false
        } else {
            library = library.copy(bookmarks = listOf(item) + library.bookmarks)
            save()
            true
        }
    }

    fun clearHistory() {
        library = library.copy(history = emptyList())
        save()
    }
}
