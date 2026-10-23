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
data class ReadMark(
    val sourceId: String,
    val mangaUrl: String,
    val mangaTitle: String = "",
    val mangaThumbnail: String? = null,
    val chapterUrl: String,
    val chapterName: String = "",
    val lastPage: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

class ReadMarkRepository(context: Context) {

    private val file = File(context.filesDir, "reads.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<List<ReadMark>> = _state.asStateFlow()

    fun find(sourceId: String, mangaUrl: String, chapterUrl: String): ReadMark? =
        _state.value.firstOrNull {
            it.sourceId == sourceId && it.mangaUrl == mangaUrl && it.chapterUrl == chapterUrl
        }

    fun isChapterRead(sourceId: String, mangaUrl: String, chapterUrl: String): Boolean =
        find(sourceId, mangaUrl, chapterUrl) != null

    suspend fun save(
        sourceId: String,
        mangaUrl: String,
        mangaTitle: String,
        mangaThumbnail: String?,
        chapterUrl: String,
        chapterName: String,
        lastPage: Int,
    ) = withContext(Dispatchers.IO) {
        val filtered = _state.value.filterNot {
            it.sourceId == sourceId && it.mangaUrl == mangaUrl && it.chapterUrl == chapterUrl
        }
        _state.value = filtered + ReadMark(
            sourceId = sourceId,
            mangaUrl = mangaUrl,
            mangaTitle = mangaTitle,
            mangaThumbnail = mangaThumbnail,
            chapterUrl = chapterUrl,
            chapterName = chapterName,
            lastPage = lastPage,
            updatedAt = System.currentTimeMillis(),
        )
        persist()
    }

    private fun load(): List<ReadMark> = runCatching {
        if (!file.exists()) emptyList()
        else json.decodeFromString<List<ReadMark>>(file.readText())
    }.getOrDefault(emptyList())

    private fun persist() {
        file.writeText(json.encodeToString(_state.value))
    }
}