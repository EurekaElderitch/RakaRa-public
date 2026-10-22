package dev.rakara.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable
data class EngineIndex(val engines: List<EngineEntry> = emptyList())

@Serializable
data class EngineEntry(
    val id: String,
    val name: String,
    val version: Int,
    val apiVersion: Int,
    val lang: String = "id",
    val url: String,
    val sha256: String,
    val size: Long = 0,
)


class EngineUpdater(
    private val context: Context,
    private val installer: EngineInstaller,
    private val indexUrl: String,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    
    suspend fun checkAndUpdate(): List<EngineManifest> = withContext(Dispatchers.IO) {
        if (indexUrl.isBlank()) return@withContext emptyList()

        val index = fetchIndex() ?: return@withContext emptyList()
        val updated = mutableListOf<EngineManifest>()

        for (entry in index.engines) {
            val current = installer.find(entry.id)
            val needsInstall = current == null
            val needsUpgrade = current != null && current.manifest.version < entry.version
            if (!needsInstall && !needsUpgrade) continue

            if (entry.apiVersion != EngineManifest.CURRENT_API) continue

            val manifest = downloadAndInstall(entry)
            if (manifest != null) updated += manifest
        }
        updated
    }

    

    private fun fetchIndex(): EngineIndex? = runCatching {
        val req = Request.Builder()
            .url(indexUrl)
            .header("User-Agent", "RAKARA/updater")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            json.decodeFromString<EngineIndex>(body)
        }
    }.getOrNull()

    private suspend fun downloadAndInstall(entry: EngineEntry): EngineManifest? = runCatching {
        val req = Request.Builder()
            .url(entry.url)
            .header("User-Agent", "RAKARA/updater")
            .build()

        val tmp = File(context.cacheDir, "upd-${entry.id}.rkx")
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            tmp.outputStream().use { out ->
                resp.body?.byteStream()?.copyTo(out) ?: return null
            }
        }

        
        val result = installer.install(tmp)
        tmp.delete()
        result.getOrNull()
    }.getOrNull()
}