package dev.rakara.app.data

import dev.rakara.engine.EngineManager
import dev.rakara.engine.EngineManifest
import dev.rakara.engine.InstalledEngine
import dev.rakara.engine.LoadedEngine
import java.io.File

class EngineRepository(private val manager: EngineManager) {

    suspend fun installed(): List<InstalledEngine> = manager.installed()

    suspend fun load(id: String): LoadedEngine = manager.load(id)

    suspend fun loadAll(): List<LoadedEngine> = manager.loadAll()

    suspend fun install(file: File): Result<EngineManifest> = manager.install(file)

    suspend fun uninstall(id: String) = manager.uninstall(id)

    suspend fun enable(id: String) = manager.enable(id)

    suspend fun disable(id: String) = manager.disable(id)
}