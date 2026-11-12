package dev.rakara.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rakara.app.data.EngineRepository
import dev.rakara.app.data.ReadMarkRepository
import dev.rakara.source.SManga
import dev.rakara.source.SChapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReaderState(
    val loading: Boolean = true,
    val error: String? = null,
    val mangaTitle: String = "",
    val chapterName: String = "",
    val urls: List<String> = emptyList(),
    val initialPage: Int = 0,
    val nextChapterUrl: String? = null,
    val nextChapterName: String? = null,
    val prevChapterUrl: String? = null,
    val prevChapterName: String? = null,
    val chapters: List<SChapter> = emptyList(), 
)

class ReaderViewModel(
    private val engineRepo: EngineRepository,
    private val readMarks: ReadMarkRepository,
    private val sourceId: String,
    private val mangaUrl: String,
    private val chapterUrl: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val loadedEngine = engineRepo.load(sourceId)
                val source = loadedEngine.source
                
                val manga = source.getDetails(SManga(url = mangaUrl))
                val chapters = source.getChapters(manga)
                
                val currentIndex = chapters.indexOfFirst { it.url == chapterUrl }
                
                var prevUrl: String? = null
                var prevName: String? = null
                var nextUrl: String? = null
                var nextName: String? = null

                if (currentIndex != -1) {
                    if (currentIndex + 1 < chapters.size) {
                        prevUrl = chapters[currentIndex + 1].url
                        prevName = chapters[currentIndex + 1].name
                    }
                    if (currentIndex - 1 >= 0) {
                        nextUrl = chapters[currentIndex - 1].url
                        nextName = chapters[currentIndex - 1].name
                    }
                }

                val chapter = chapters.firstOrNull { it.url == chapterUrl } 
                    ?: error("Chapter tidak ditemukan di daftar")
                val pages = source.getPages(chapter)
                
                val safeTitle = manga.title.orEmpty()
                val safeChapterName = chapter.name.orEmpty()

                readMarks.save(
                    sourceId = sourceId,
                    mangaUrl = mangaUrl,
                    mangaTitle = safeTitle,
                    mangaThumbnail = manga.thumbnailUrl,
                    chapterUrl = chapterUrl,
                    chapterName = safeChapterName,
                    lastPage = 0
                )

                _state.value = ReaderState(
                    loading = false,
                    mangaTitle = safeTitle,
                    chapterName = safeChapterName,
                    urls = pages.mapNotNull { it.imageUrl },
                    nextChapterUrl = nextUrl,
                    nextChapterName = nextName,
                    prevChapterUrl = prevUrl,
                    prevChapterName = prevName,
                    chapters = chapters 
                )
            } catch (e: Exception) {
                _state.value = ReaderState(loading = false, error = e.message ?: "Gagal memuat")
            }
        }
    }

    fun onPageChanged(page: Int) {}
}