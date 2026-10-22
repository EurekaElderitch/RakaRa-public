package dev.rakara.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rakara.app.data.EngineRepository
import dev.rakara.app.data.ReadMarkRepository
import dev.rakara.source.SChapter
import dev.rakara.source.SManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReaderState(
    val loading: Boolean = true,
    val error: String? = null,
    val urls: List<String> = emptyList(),
    val initialPage: Int = 0,
    val currentPage: Int = 0,
    val mangaTitle: String = "",
    val chapterName: String = "",
    val prevChapterUrl: String? = null,
    val nextChapterUrl: String? = null,
    val nextChapterName: String? = null,
)

class ReaderViewModel(
    private val engineRepo: EngineRepository,
    private val readMarks: ReadMarkRepository,
    private val sourceId: String,
    private val mangaUrl: String,
    private val chapterUrl: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val le = engineRepo.load(sourceId)
                val source = le.source

                
                val pages = source.getPages(SChapter(url = chapterUrl))
                val urls = pages.mapNotNull { it.imageUrl }

                
                val manga = source.getDetails(SManga(url = mangaUrl))
                val chapters = runCatching { source.getChapters(manga) }
                    .getOrDefault(emptyList())
                    .sortedBy { it.chapterNumber }

                val idx = chapters.indexOfFirst { it.url == chapterUrl }
                val prev = chapters.getOrNull(idx - 1)
                val next = chapters.getOrNull(idx + 1)
                val current = chapters.getOrNull(idx)

                
                val mark = readMarks.find(sourceId, mangaUrl, chapterUrl)
                val start = (mark?.lastPage ?: 0)
                    .coerceIn(0, (urls.size - 1).coerceAtLeast(0))

                _state.value = _state.value.copy(
                    loading = false,
                    urls = urls,
                    initialPage = start,
                    currentPage = start,
                    mangaTitle = manga.title,
                    chapterName = current?.name ?: "",
                    prevChapterUrl = prev?.url,
                    nextChapterUrl = next?.url,
                    nextChapterName = next?.name,
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    loading = false,
                    error = it.message ?: "Gagal memuat"
                )
            }
        }
    }

    fun onPageChanged(page: Int) {
        if (_state.value.currentPage == page) return
        _state.value = _state.value.copy(currentPage = page)
        viewModelScope.launch {
            readMarks.save(sourceId, mangaUrl, chapterUrl, page)
        }
    }

    
    val navigationContext get() = Pair(sourceId, mangaUrl)
}