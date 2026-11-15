package dev.rakara.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile


class EngineInstaller(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val enginesDir = File(context.filesDir, "engines").apply { mkdirs() }
    private val disabledFile = File(enginesDir, ".disabled.json")
    private val mutex = Mutex()

    

    
    suspend fun install(rkx: File): Result<EngineManifest> = withContext(Dispatchers.IO) {
        runCatching {
            require(rkx.exists()) { "file .rkx tidak ditemukan: ${rkx.absolutePath}" }

            val manifest = readAndVerify(rkx)
            require(manifest.apiVersion == EngineManifest.CURRENT_API) {
                "Engine API tidak cocok: engine=${manifest.apiVersion}, host=${EngineManifest.CURRENT_API}. " +
                "Rebuild engine dengan source-api terbaru."
            }

            mutex.withLock {
                val target = File(enginesDir, "${manifest.id}/${manifest.version}")
                if (target.exists() && File(target, "classes.dex").exists()) {
                    return@withLock manifest
                }

                val staging = File(enginesDir, "${manifest.id}/.staging-${manifest.version}")
                staging.deleteRecursively(); staging.mkdirs()
                try {
                    ZipFile(rkx).use { zip -> extract(zip, staging) }
                    File(staging, "classes.dex").let {
                        require(it.exists()) { "classes.dex hilang setelah ekstrak" }
                        it.setReadOnly()   
                    }
                    target.parentFile?.mkdirs()
                    require(staging.renameTo(target)) {
                        "gagal rename staging → target (target: $target)"
                    }
                } catch (t: Throwable) {
                    staging.deleteRecursively()
                    throw t
                }
                manifest
            }
        }
    }

    

    
    suspend fun uninstall(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dir = File(enginesDir, id)
            if (!dir.exists()) return@withLock false

            val tomb = File(context.cacheDir, "engine_tomb/${id}_${System.currentTimeMillis()}")
            tomb.parentFile?.mkdirs()
            val renamed = dir.renameTo(tomb)
            if (renamed) tomb.deleteRecursively()

            
            val set = readDisabled().toMutableSet().apply { remove(id) }
            writeDisabled(set)
            renamed
        }
    }

    
    fun cleanupTombstones() {
        File(context.cacheDir, "engine_tomb").deleteRecursively()
    }

    

    fun disable(id: String) {
        val set = readDisabled().toMutableSet().apply { add(id) }
        writeDisabled(set)
    }

    fun enable(id: String) {
        val set = readDisabled().toMutableSet().apply { remove(id) }
        writeDisabled(set)
    }

    fun isEnabled(id: String): Boolean = id !in readDisabled()

    

    
    fun installed(): List<InstalledEngine> {
        val disabled = readDisabled()
        val result = mutableListOf<InstalledEngine>()

        for (dir in enginesDir.listFiles() ?: emptyArray()) {
            if (!dir.isDirectory || dir.name.startsWith(".")) continue
            val versionDir = latestVersionDir(dir.name) ?: continue
            val manifest = runCatching { readManifest(File(versionDir, "manifest.json")) }
                .getOrNull() ?: continue
            result += InstalledEngine(
                manifest = manifest,
                dir = versionDir,
                enabled = manifest.id !in disabled,
            )
        }
        return result.sortedBy { it.manifest.name.lowercase() }
    }

    
    fun find(id: String): InstalledEngine? =
        installed().firstOrNull { it.manifest.id == id }

    

    private fun readAndVerify(rkx: File): EngineManifest {
        ZipFile(rkx).use { zip ->
            val mEntry = zip.getEntry("manifest.json")
                ?: error("manifest.json tidak ada di .rkx")
            val mText = zip.getInputStream(mEntry).bufferedReader().use { it.readText() }
            val manifest = json.decodeFromString<EngineManifest>(mText)

            val dexEntry = zip.getEntry("classes.dex")
                ?: error("classes.dex tidak ada di .rkx")
            val bytes = zip.getInputStream(dexEntry).readBytes()
            val actual = sha256(bytes)
            require(actual == manifest.sha256) {
                "checksum mismatch: expected=${manifest.sha256} got=$actual"
            }
            return manifest
        }
    }

    private fun extract(zip: ZipFile, to: File) {
        for (e in zip.entries()) {
            val t = File(to, e.name)
            if (e.isDirectory) { t.mkdirs(); continue }
            t.parentFile?.mkdirs()
            zip.getInputStream(e).use { i -> t.outputStream().use { i.copyTo(it) } }
        }
    }

    private fun latestVersionDir(id: String): File? {
        val base = File(enginesDir, id)
        if (!base.exists() || !base.isDirectory) return null
        return base.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") && File(it, "classes.dex").exists() }
            ?.maxByOrNull { it.name.toIntOrNull() ?: 0 }
    }

    private fun readManifest(file: File): EngineManifest =
        json.decodeFromString(file.readText())

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun readDisabled(): Set<String> = runCatching {
        if (!disabledFile.exists()) emptySet()
        else json.decodeFromString(SetSerializer(String.serializer()), disabledFile.readText())
    }.getOrDefault(emptySet())

    private fun writeDisabled(set: Set<String>) {
        disabledFile.parentFile?.mkdirs()
        disabledFile.writeText(json.encodeToString(SetSerializer(String.serializer()), set))
    }
}