package dev.rakara.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rakara.app.data.EngineRepository
import dev.rakara.app.data.ReadMark
import dev.rakara.app.data.ReadMarkRepository
import dev.rakara.source.SManga
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class HomeState(
    val loading: Boolean = true,
    val error: String? = null,
    val continueReading: List<ReadMark> = emptyList(),
    val top10: List<SManga> = emptyList(),
    val updates: List<SManga> = emptyList(),
)

class HomeViewModel(
    private val engineRepo: EngineRepository,
    private val readMarks: ReadMarkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val continueList = readMarks.state.value
                .sortedByDescending { it.updatedAt }
                .take(10)

            val engines = runCatching { engineRepo.loadAll() }.getOrDefault(emptyList())

            if (engines.isEmpty()) {
                _state.value = HomeState(
                    loading = false,
                    continueReading = continueList,
                    error = "Belum ada sumber terpasang.",
                )
                return@launch
            }

            
            val results = engines.map { le ->
                async {
                    runCatching {
                        withTimeoutOrNull(8_000) {
                            val popular = le.source.getPopular(1).mangas
                            val latest = le.source.getLatest(1).mangas
                            popular to latest
                        }
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()

            val top10 = interleave(results.map { it.first }).take(10)
            val updates = interleave(results.map { it.second }).take(20)

            _state.value = HomeState(
                loading = false,
                continueReading = continueList,
                top10 = top10,
                updates = updates,
            )
        }
    }

    
    private fun interleave(lists: List<List<SManga>>): List<SManga> {
        val out = mutableListOf<SManga>()
        val seen = mutableSetOf<String>()
        val maxLen = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until maxLen) {
            for (list in lists) {
                val m = list.getOrNull(i) ?: continue
                if (seen.add("${m.url}|${m.title}")) out += m
            }
        }
        return out
    }
}