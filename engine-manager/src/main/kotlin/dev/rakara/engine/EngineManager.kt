package dev.rakara.engine

import android.content.Context
import dalvik.system.DexClassLoader
import dev.rakara.source.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File


class EngineManager(context: Context) {

    private val appContext = context.applicationContext
    val installer = EngineInstaller(appContext)

    private val dexCacheDir = File(appContext.codeCacheDir, "engine_dex").apply { mkdirs() }
    private val loaded = mutableMapOf<String, LoadedEngine>()
    private val loadMutex = Mutex()

    

    suspend fun install(rkx: File): Result<EngineManifest> = installer.install(rkx)

    suspend fun uninstall(id: String): Boolean {
        unload(id)
        return installer.uninstall(id)
    }

    fun disable(id: String) {
        installer.disable(id)
        unload(id)
    }

    fun enable(id: String) = installer.enable(id)

    fun cleanupTombstones() = installer.cleanupTombstones()

    

    fun installed(): List<InstalledEngine> = installer.installed()

    fun find(id: String): InstalledEngine? = installer.find(id)

    

    
    suspend fun load(id: String): LoadedEngine = loadMutex.withLock {
        loaded[id]?.let { return it }

        val inst = installer.find(id)
            ?: error("Engine '$id' tidak terpasang")
        require(inst.enabled) { "Engine '$id' sedang dinonaktifkan" }

        val source = withContext(Dispatchers.IO) { instantiate(inst.dir, inst.manifest) }
        val le = LoadedEngine(inst.manifest, inst.dir, source)
        loaded[id] = le
        le
    }

    
    suspend fun loadAll(): List<LoadedEngine> {
        val engines = installer.installed().filter { it.enabled }
        val out = mutableListOf<LoadedEngine>()
        for (e in engines) {
            runCatching { out += load(e.manifest.id) }
                .onFailure { android.util.Log.w("RakaRa", "load ${e.manifest.id} gagal", it) }
        }
        return out
    }

    fun unload(id: String) {
        loaded.remove(id)?.source?.let { runCatching { it.close() } }
    }

    fun unloadAll() {
        loaded.keys.toList().forEach { unload(it) }
    }

    

    private fun instantiate(dir: File, manifest: EngineManifest): Source {
        val dex = File(dir, "classes.dex").apply { setReadOnly() }
        val opt = File(dexCacheDir, "${manifest.id}_${manifest.version}").apply { mkdirs() }

        
        
        val loader = DexClassLoader(
            dex.absolutePath,
            opt.absolutePath,
            null,
            Source::class.java.classLoader,
        )

        val cls = loader.loadClass(manifest.className)
        val inst = cls.getDeclaredConstructor().newInstance() as? Source
            ?: error("Kelas ${manifest.className} bukan implementasi Source")

        
        runCatching {
            val f = inst.javaClass.getDeclaredField("id").apply { isAccessible = true }
            if (f.getLong(inst) == 0L) f.setLong(inst, manifest.id.hashCode().toLong())
        }

        return inst
    }
}
