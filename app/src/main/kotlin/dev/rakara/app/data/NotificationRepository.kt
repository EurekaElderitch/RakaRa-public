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
data class NotificationSub(
    val sourceId: String,
    val mangaUrl: String,
    val mangaTitle: String = "",
    val mangaThumbnail: String? = null,
    val lastKnownChapterUrl: String? = null,
    val lastKnownChapterName: String? = null,
    val lastCheckedAt: Long = 0L,
    val unreadCount: Int = 0,
)

class NotificationRepository(context: Context) {

    private val file = File(context.filesDir, "notifications.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<List<NotificationSub>> = _state.asStateFlow()

    fun isSubscribed(sourceId: String, mangaUrl: String): Boolean =
        _state.value.any { it.sourceId == sourceId && it.mangaUrl == mangaUrl }

    fun find(sourceId: String, mangaUrl: String): NotificationSub? =
        _state.value.firstOrNull { it.sourceId == sourceId && it.mangaUrl == mangaUrl }

    suspend fun subscribe(
        sourceId: String,
        mangaUrl: String,
        mangaTitle: String,
        mangaThumbnail: String?,
        lastKnownChapterUrl: String?,
        lastKnownChapterName: String?,
    ) = withContext(Dispatchers.IO) {
        if (isSubscribed(sourceId, mangaUrl)) return@withContext
        _state.value = _state.value + NotificationSub(
            sourceId = sourceId,
            mangaUrl = mangaUrl,
            mangaTitle = mangaTitle,
            mangaThumbnail = mangaThumbnail,
            lastKnownChapterUrl = lastKnownChapterUrl,
            lastKnownChapterName = lastKnownChapterName,
            lastCheckedAt = System.currentTimeMillis(),
        )
        persist()
    }

    suspend fun unsubscribe(sourceId: String, mangaUrl: String) = withContext(Dispatchers.IO) {
        _state.value = _state.value.filterNot {
            it.sourceId == sourceId && it.mangaUrl == mangaUrl
        }
        persist()
    }

    suspend fun updateAfterCheck(
        sourceId: String,
        mangaUrl: String,
        latestChapterUrl: String?,
        latestChapterName: String?,
        hasNew: Boolean,
    ) = withContext(Dispatchers.IO) {
        _state.value = _state.value.map { sub ->
            if (sub.sourceId == sourceId && sub.mangaUrl == mangaUrl) {
                sub.copy(
                    lastKnownChapterUrl = latestChapterUrl ?: sub.lastKnownChapterUrl,
                    lastKnownChapterName = latestChapterName ?: sub.lastKnownChapterName,
                    lastCheckedAt = System.currentTimeMillis(),
                    unreadCount = if (hasNew) sub.unreadCount + 1 else sub.unreadCount,
                )
            } else sub
        }
        persist()
    }

    suspend fun markRead(sourceId: String, mangaUrl: String) = withContext(Dispatchers.IO) {
        _state.value = _state.value.map {
            if (it.sourceId == sourceId && it.mangaUrl == mangaUrl) it.copy(unreadCount = 0)
            else it
        }
        persist()
    }

    suspend fun markAllRead() = withContext(Dispatchers.IO) {
        _state.value = _state.value.map { it.copy(unreadCount = 0) }
        persist()
    }

    private fun load(): List<NotificationSub> = runCatching {
        if (!file.exists()) emptyList()
        else json.decodeFromString<List<NotificationSub>>(file.readText())
    }.getOrDefault(emptyList())

    private fun persist() {
        file.writeText(json.encodeToString(_state.value))
    }
}