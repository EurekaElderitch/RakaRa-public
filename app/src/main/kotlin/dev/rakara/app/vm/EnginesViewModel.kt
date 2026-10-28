package dev.rakara.app.vm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.rakara.app.RakaraApp
import dev.rakara.app.data.EngineRepository
import dev.rakara.engine.InstalledEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class EnginesState(
    val loading: Boolean = true,
    val engines: List<InstalledEngine> = emptyList(),
    val status: String? = null,
    val busy: Boolean = false,
    val refreshing: Boolean = false,
)

class EnginesViewModel(
    private val repo: EngineRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(EnginesState())
    val state: StateFlow<EnginesState> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repo.installed() }
            _state.value = _state.value.copy(loading = false, engines = list)
        }
    }

    fun setStatus(s: String?) { _state.value = _state.value.copy(status = s) }

    
    fun refreshEngines() {
        if (_state.value.busy) return
        _state.value = _state.value.copy(
            busy = true,
            refreshing = true,
            status = "Memeriksa update engine...",
        )
        viewModelScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) {
                    RakaraApp.from(appContext).engineUpdater.checkAndUpdate()
                }
                val msg = if (updated.isEmpty()) {
                    "Tidak ada update engine"
                } else {
                    "Berhasil update: ${updated.size} engine"
                }
                _state.value = _state.value.copy(
                    busy = false,
                    refreshing = false,
                    status = msg,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    busy = false,
                    refreshing = false,
                    status = "Gagal: ${e.message}",
                )
            }
            reload()
        }
    }

    fun installFromUri(uri: Uri) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, status = "Memasang…")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val tmp = File(appContext.cacheDir, "incoming.rkx")
                    appContext.contentResolver.openInputStream(uri)!!.use { input ->
                        tmp.outputStream().use { input.copyTo(it) }
                    }
                    repo.install(tmp)
                }
            }.onSuccess { r ->
                val name = r.getOrNull()?.name ?: "?"
                _state.value = _state.value.copy(busy = false, status = "Terpasang: $name")
            }.onFailure {
                _state.value = _state.value.copy(busy = false, status = "Gagal: ${it.message}")
            }
            reload()
        }
    }

    fun toggle(engine: InstalledEngine) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (engine.enabled) repo.disable(engine.manifest.id)
                else repo.enable(engine.manifest.id)
            }
            reload()
        }
    }

    fun uninstall(engine: InstalledEngine) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.uninstall(engine.manifest.id) }
            _state.value = _state.value.copy(status = "Dihapus: ${engine.manifest.name}")
            reload()
        }
    }
}