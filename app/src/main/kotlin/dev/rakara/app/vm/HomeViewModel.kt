package dev.rakara.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rakara.app.data.EngineRepository
import dev.rakara.app.data.ReadMark
import dev.rakara.app.data.ReadMarkRepository
import dev.rakara.source.SManga
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
    val selectedFilter: String = "Semua"
)

class HomeViewModel(
    private val engineRepo: EngineRepository,
    private val readMarks: ReadMarkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    private var rawTop10: List<SManga> = emptyList()
    private var rawUpdates: List<SManga> = emptyList()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val engines = engineRepo.loadAll()
                val popular = mutableListOf<SManga>()
                val latest = mutableListOf<SManga>()
                
                
                val source = engines.firstOrNull()?.source
                if (source != null) {
                    val popPage = source.getPopular(1)
                    val latPage = source.getLatest(1)
                    popular.addAll(popPage.mangas)
                    latest.addAll(latPage.mangas)
                }

                rawTop10 = popular
                rawUpdates = latest

                _state.value = _state.value.copy(
                    loading = false,
                    continueReading = readMarks.state.value,
                    top10 = popular,
                    updates = latest
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun updateSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilterAndSearch()
    }

    fun updateFilter(filter: String) {
        _state.value = _state.value.copy(selectedFilter = filter)
        applyFilterAndSearch()
    }

    private fun applyFilterAndSearch() {
        val query = _state.value.searchQuery.lowercase()
        val filter = _state.value.selectedFilter

        fun filterList(list: List<SManga>): List<SManga> {
            return list.filter { manga ->
                val matchQuery = manga.title.lowercase().contains(query)
                val matchFilter = if (filter == "Semua") true else {
                    manga.genre?.any { it.equals(filter, ignoreCase = true) } == true
                }
                matchQuery && matchFilter
            }
        }

        _state.value = _state.value.copy(
            top10 = filterList(rawTop10),
            updates = filterList(rawUpdates)
        )
    }
}