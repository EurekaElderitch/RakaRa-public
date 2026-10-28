package dev.rakara.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class LibraryEntry(
    val sourceId: String,
    val mangaUrl: String,
    val title: String,
    val thumbnail: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
)

class LibraryRepository(context: Context) {

    private val file = File(context.filesDir, "library.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<List<LibraryEntry>> = _state.asStateFlow()

    fun contains(sourceId: String, mangaUrl: String): Boolean =
        _state.value.any { it.sourceId == sourceId && it.mangaUrl == mangaUrl }

    suspend fun add(entry: LibraryEntry) = withContext(Dispatchers.IO) {
        if (contains(entry.sourceId, entry.mangaUrl)) return@withContext
        _state.value = _state.value + entry
        persist()
    }

    suspend fun remove(sourceId: String, mangaUrl: String) = withContext(Dispatchers.IO) {
        _state.value = _state.value.filterNot {
            it.sourceId == sourceId && it.mangaUrl == mangaUrl
        }
        persist()
    }

    private fun load(): List<LibraryEntry> = runCatching {
        if (!file.exists()) emptyList()
        else json.decodeFromString<List<LibraryEntry>>(file.readText())
    }.getOrDefault(emptyList())

    private fun persist() {
        file.writeText(json.encodeToString(_state.value))
    }
}