package dev.rakara.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rakara.app.data.EngineRepository
import dev.rakara.app.data.LibraryEntry
import dev.rakara.app.data.LibraryRepository
import dev.rakara.app.data.NotificationRepository
import dev.rakara.source.SChapter
import dev.rakara.source.SManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val manga: SManga? = null,
    val chapters: List<SChapter> = emptyList(),
    val inLibrary: Boolean = false,
    val subscribed: Boolean = false,
)

class MangaDetailViewModel(
    private val engineRepo: EngineRepository,
    private val library: LibraryRepository,
    private val notifications: NotificationRepository,
    private val sourceId: String,
    private val mangaUrl: String,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val le = engineRepo.load(sourceId)
                val m = le.source.getDetails(SManga(url = mangaUrl))
                val ch = le.source.getChapters(m)
                _state.value = _state.value.copy(
                    loading = false,
                    manga = m,
                    chapters = ch,
                    inLibrary = library.contains(sourceId, m.url),
                    subscribed = notifications.isSubscribed(sourceId, m.url),
                )
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.message ?: "Gagal")
            }
        }
    }

    fun toggleLibrary() {
        val m = _state.value.manga ?: return
        viewModelScope.launch {
            if (_state.value.inLibrary) {
                library.remove(sourceId, m.url)
                _state.value = _state.value.copy(inLibrary = false)
            } else {
                library.add(LibraryEntry(sourceId, m.url, m.title, m.thumbnailUrl))
                _state.value = _state.value.copy(inLibrary = true)
            }
        }
    }

    fun toggleSubscription() {
        val m = _state.value.manga ?: return
        viewModelScope.launch {
            if (_state.value.subscribed) {
                notifications.unsubscribe(sourceId, m.url)
                _state.value = _state.value.copy(subscribed = false)
            } else {
                
                val latest = _state.value.chapters.maxByOrNull { it.chapterNumber }
                notifications.subscribe(
                    sourceId = sourceId,
                    mangaUrl = m.url,
                    mangaTitle = m.title,
                    mangaThumbnail = m.thumbnailUrl,
                    lastKnownChapterUrl = latest?.url,
                    lastKnownChapterName = latest?.name,
                )
                _state.value = _state.value.copy(subscribed = true)
            }
        }
    }
}