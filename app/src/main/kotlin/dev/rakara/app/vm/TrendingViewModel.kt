package dev.rakara.app.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rakara.app.data.EngineRepository
import dev.rakara.source.SManga
import kotlinx.coroutines.launch

data class TrendingUiState(
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val items: List<SManga> = emptyList(),
    val error: String? = null,
    val hasNext: Boolean = false,
)

class TrendingViewModel(
    private val engineRepo: EngineRepository,
) : ViewModel() {

    var state by mutableStateOf(TrendingUiState())
        private set

    private var page = 0

    init { loadInitial() }

    fun loadInitial() {
        page = 0
        state = TrendingUiState(loading = true)
        viewModelScope.launch {
            try {
                val source = engineRepo.loadAll().firstOrNull()?.source
                if (source == null) {
                    state = TrendingUiState(loading = false, error = "Engine belum siap")
                    return@launch
                }
                val res = source.getPopular(1)
                state = TrendingUiState(
                    loading = false,
                    items = res.mangas,
                    hasNext = res.hasNextPage,
                )
                page = 1
            } catch (e: Exception) {
                state = TrendingUiState(loading = false, error = e.message ?: "Gagal memuat data")
            }
        }
    }

    fun loadMore() {
        if (state.loading || state.loadingMore || !state.hasNext) return
        state = state.copy(loadingMore = true)
        viewModelScope.launch {
            try {
                val source = engineRepo.loadAll().firstOrNull()?.source
                if (source == null) {
                    state = state.copy(loadingMore = false)
                    return@launch
                }
                val res = source.getPopular(page + 1)
                state = state.copy(
                    loadingMore = false,
                    items = state.items + res.mangas,
                    hasNext = res.hasNextPage,
                )
                page += 1
            } catch (_: Exception) {
                state = state.copy(loadingMore = false)
            }
        }
    }
}