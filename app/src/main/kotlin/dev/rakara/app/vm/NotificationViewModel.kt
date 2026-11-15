package dev.rakara.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rakara.app.data.EngineRepository
import dev.rakara.app.data.NotificationRepository
import dev.rakara.app.data.NotificationSub
import dev.rakara.source.SManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationUiState(
    val checking: Boolean = false,
    val subs: List<NotificationSub> = emptyList(),
    val error: String? = null,
)

class NotificationViewModel(
    private val engineRepo: EngineRepository,
    private val notifications: NotificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            notifications.state.collect { subs ->
                _state.value = _state.value.copy(subs = subs)
            }
        }
    }

    fun checkForUpdates() {
        if (_state.value.checking) return
        val subs = notifications.state.value
        if (subs.isEmpty()) return

        _state.value = _state.value.copy(checking = true, error = null)
        viewModelScope.launch {
            var lastError: String? = null
            subs.forEach { sub ->
                try {
                    val source = engineRepo.load(sub.sourceId).source
                    val chapters = source.getChapters(SManga(url = sub.mangaUrl))
                    val latest = chapters.maxByOrNull { it.chapterNumber }
                    if (latest != null) {
                        
                        val hasNew = sub.lastKnownChapterUrl != null &&
                                latest.url != sub.lastKnownChapterUrl
                        notifications.updateAfterCheck(
                            sourceId = sub.sourceId,
                            mangaUrl = sub.mangaUrl,
                            latestChapterUrl = latest.url,
                            latestChapterName = latest.name,
                            hasNew = hasNew,
                        )
                    }
                } catch (e: Exception) {
                    lastError = e.message
                }
            }
            _state.value = _state.value.copy(checking = false, error = lastError)
        }
    }

    fun markAllRead() {
        viewModelScope.launch { notifications.markAllRead() }
    }

    fun markRead(sourceId: String, mangaUrl: String) {
        viewModelScope.launch { notifications.markRead(sourceId, mangaUrl) }
    }

    fun unsubscribe(sourceId: String, mangaUrl: String) {
        viewModelScope.launch { notifications.unsubscribe(sourceId, mangaUrl) }
    }
}