package dev.rakara.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rakara.app.data.EngineRepository
import dev.rakara.app.data.ReadMark
import dev.rakara.app.data.ReadMarkRepository
import dev.rakara.source.FilterList
import dev.rakara.source.SManga
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val continueReading: List<ReadMark> = emptyList(),
    val top10: List<SManga> = emptyList(),
    val updates: List<SManga> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<SManga> = emptyList(),
    val isSearching: Boolean = false
)

class HomeViewModel(
    private val engineRepo: EngineRepository,
    private val readMarks: ReadMarkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state
    private var searchJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val engines = engineRepo.loadAll()
                val source = engines.firstOrNull()?.source
                if (source != null) {
                    val popular = source.getPopular(1).mangas
                    val latest = source.getLatest(1).mangas
                    
                    _state.value = _state.value.copy(
                        loading = false,
                        continueReading = readMarks.state.value,
                        top10 = popular,
                        updates = latest
                    )
                } else {
                    _state.value = _state.value.copy(loading = false, error = "Engine belum siap")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    
    fun performSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            delay(500) 
            try {
                val source = engineRepo.loadAll().firstOrNull()?.source
                if (source != null) {
                    
                    val results = source.search(query, 1, FilterList(emptyList())).mangas
                    _state.value = _state.value.copy(searchResults = results, isSearching = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSearching = false, error = e.message)
            }
        }
    }
}