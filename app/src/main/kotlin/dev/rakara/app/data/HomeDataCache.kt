package dev.rakara.app.data

import dev.rakara.source.SManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HomeDataCache {
    private const val DEFAULT_TTL_MS = 5 * 60 * 1000L  

    data class HomeCacheData(
        val popular: List<SManga>,
        val latest: List<SManga>,
        val fetchedAt: Long = System.currentTimeMillis(),
    )

    private val _data = MutableStateFlow<HomeCacheData?>(null)
    val data: StateFlow<HomeCacheData?> = _data.asStateFlow()

    fun set(popular: List<SManga>, latest: List<SManga>) {
        _data.value = HomeCacheData(popular, latest)
    }

    fun get(): HomeCacheData? = _data.value

    fun isFresh(maxAgeMs: Long = DEFAULT_TTL_MS): Boolean {
        val d = _data.value ?: return false
        return System.currentTimeMillis() - d.fetchedAt < maxAgeMs
    }

    fun clear() { _data.value = null }
}