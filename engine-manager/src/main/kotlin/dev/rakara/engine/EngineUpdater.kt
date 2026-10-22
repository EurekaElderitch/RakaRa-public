package dev.rakara.engine

import android.content.Context
import android.util.Log
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
    private val TAG = "EngineUpdater"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    private fun resolveUrl(entry: EngineEntry): String {
    if (entry.url.startsWith("http://") || entry.url.startsWith("https://")) {
        return entry.url
    }
    val base = indexUrl.substringBeforeLast("/")
    return "$base/${entry.url}"
}
    suspend fun checkAndUpdate(): List<EngineManifest> = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== checkAndUpdate START ===")
        Log.d(TAG, "indexUrl = $indexUrl")

        if (indexUrl.isBlank()) {
            Log.e(TAG, "indexUrl kosong, abort")
            return@withContext emptyList()
        }

        val index = fetchIndex()
        if (index == null) {
            Log.e(TAG, "fetchIndex GAGAL, abort")
            return@withContext emptyList()
        }
        Log.d(TAG, "Index OK: ${index.engines.size} engine(s)")

        val updated = mutableListOf<EngineManifest>()
        for (entry in index.engines) {
            Log.d(TAG, "--- Cek ${entry.id} v${entry.version} ---")
            val current = installer.find(entry.id)
            val needsInstall = current == null
            val needsUpgrade = current != null && current.manifest.version < entry.version

            if (!needsInstall && !needsUpgrade) {
                Log.d(TAG, "  Skip: lokal sudah v${current?.manifest?.version}")
                continue
            }
            if (entry.apiVersion != EngineManifest.CURRENT_API) {
                Log.w(TAG, "  Skip: apiVersion ${entry.apiVersion} != ${EngineManifest.CURRENT_API}")
                continue
            }

            Log.d(TAG, "  Install/upgrade ke v${entry.version}")
            val manifest = downloadAndInstall(entry)
            if (manifest != null) {
                Log.d(TAG, "  ✓ OK: ${entry.id} v${manifest.version}")
                updated += manifest
            } else {
                Log.e(TAG, "  ✗ GAGAL: ${entry.id}")
            }
        }
        Log.d(TAG, "=== checkAndUpdate END: ${updated.size} updated ===")
        updated
    }

    private fun fetchIndex(): EngineIndex? = runCatching {
        val req = Request.Builder()
            .url(indexUrl)
            .header("User-Agent", "RAKARA/updater")
            .build()

        Log.d(TAG, "GET $indexUrl")
        http.newCall(req).execute().use { resp ->
            Log.d(TAG, "HTTP ${resp.code} ${resp.message}")
            if (!resp.isSuccessful) {
                Log.e(TAG, "Response not successful")
                return null
            }
            val body = resp.body?.string().orEmpty()
            Log.d(TAG, "Body ${body.length} chars, prefix: ${body.take(120)}")

            try {
                json.decodeFromString<EngineIndex>(body).also {
                    Log.d(TAG, "Parse OK, ${it.engines.size} engines")
                }
            } catch (e: Exception) {
                Log.e(TAG, "JSON parse FAILED", e)
                null
            }
        }
    }.onFailure {
        Log.e(TAG, "fetchIndex exception", it)
    }.getOrNull()

    private suspend fun downloadAndInstall(entry: EngineEntry): EngineManifest? = runCatching {
        val resolvedUrl = resolveUrl(entry)
        Log.d(TAG, "  GET $resolvedUrl")
        val req = Request.Builder()
            .url(resolvedUrl)
            .header("User-Agent", "RAKARA/updater")
            .build()
        val tmp = File(context.cacheDir, "upd-${entry.id}.rkx")
        Log.d(TAG, "  GET ${entry.url}")
        Log.d(TAG, "  -> ${tmp.absolutePath}")

        http.newCall(req).execute().use { resp ->
            Log.d(TAG, "  HTTP ${resp.code} ${resp.message}")
            if (!resp.isSuccessful) {
                Log.e(TAG, "  Download not successful")
                return null
            }
            tmp.outputStream().use { out ->
                resp.body?.byteStream()?.copyTo(out) ?: return null
            }
        }
        Log.d(TAG, "  Downloaded ${tmp.length()} bytes")

        val result = installer.install(tmp)
        tmp.delete()

        if (result.isSuccess) {
            Log.d(TAG, "  install() OK")
            result.getOrNull()
        } else {
            Log.e(TAG, "  install() FAILED", result.exceptionOrNull())
            null
        }
    }.onFailure {
        Log.e(TAG, "  downloadAndInstall exception", it)
    }.getOrNull()
}