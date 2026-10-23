package dev.rakara.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rakara.app.data.EngineRepository
import dev.rakara.source.SManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrowseState(
    val loading: Boolean = true,
    val error: String? = null,
    val engineName: String = "",
    val items: List<SManga> = emptyList(),
    val query: String = "",
    val mode: Mode = Mode.POPULAR,
) {
    enum class Mode { POPULAR, SEARCH }
}

class BrowseViewModel(
    private val repo: EngineRepository,
    private val sourceId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseState())
    val state: StateFlow<BrowseState> = _state.asStateFlow()

    init { loadPopular() }

    fun setQuery(q: String) { _state.value = _state.value.copy(query = q) }

    fun loadPopular() = run { _state.value = _state.value.copy(mode = BrowseState.Mode.POPULAR); fetch() }

    fun search() = run { _state.value = _state.value.copy(mode = BrowseState.Mode.SEARCH); fetch() }

    private fun fetch() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val le = repo.load(sourceId)
                val page = when (_state.value.mode) {
                    BrowseState.Mode.POPULAR -> le.source.getPopular(1)
                    BrowseState.Mode.SEARCH -> le.source.search(_state.value.query, 1)
                }
                _state.value = _state.value.copy(
                    loading = false,
                    engineName = le.manifest.name,
                    items = page.mangas,
                )
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.message ?: "Gagal")
            }
        }
    }
}