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
    val items: List<SManga> = emptyList(),
    val error: String? = null,
)

class TrendingViewModel(
    private val engineRepo: EngineRepository,
) : ViewModel() {

    var state by mutableStateOf(TrendingUiState())
        private set

    init { load() }

    fun load() {
        state = TrendingUiState(loading = true)
        viewModelScope.launch {
            try {
                val source = engineRepo.loadAll().firstOrNull()?.source
                if (source == null) {
                    state = TrendingUiState(loading = false, error = "Engine belum siap")
                    return@launch
                }
                val items = source.getPopular(1).mangas.take(20)
                state = TrendingUiState(loading = false, items = items)
            } catch (e: Exception) {
                state = TrendingUiState(loading = false, error = e.message ?: "Gagal memuat data")
            }
        }
    }
}