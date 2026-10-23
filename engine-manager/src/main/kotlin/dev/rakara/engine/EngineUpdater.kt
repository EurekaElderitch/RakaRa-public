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
    
    private fun log(msg: String) = println("EngineUpdater: $msg")
    private fun logErr(msg: String, e: Throwable? = null) {
        println("EngineUpdater [ERROR]: $msg${e?.let { " -> ${it.message}" } ?: ""}")
        e?.printStackTrace()
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
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
        log("=== checkAndUpdate START ===")
        log("indexUrl = $indexUrl")

        if (indexUrl.isBlank()) {
            logErr("indexUrl kosong, abort")
            return@withContext emptyList()
        }

        val index = fetchIndex()
        if (index == null) {
            logErr("fetchIndex GAGAL, abort")
            return@withContext emptyList()
        }
        log("Index OK: ${index.engines.size} engine(s)")

        val updated = mutableListOf<EngineManifest>()
        for (entry in index.engines) {
            log("--- Cek ${entry.id} v${entry.version} ---")
            val current = installer.find(entry.id)
            val needsInstall = current == null
            val needsUpgrade = current != null && current.manifest.version < entry.version

            if (!needsInstall && !needsUpgrade) {
                log("  Skip: lokal sudah v${current?.manifest?.version}")
                continue
            }
            if (entry.apiVersion != EngineManifest.CURRENT_API) {
                log("  Skip: apiVersion ${entry.apiVersion} != ${EngineManifest.CURRENT_API}")
                continue
            }

            log("  Install/upgrade ke v${entry.version}")
            val manifest = downloadAndInstall(entry)
            if (manifest != null) {
                log("  OK: ${entry.id} v${manifest.version}")
                updated += manifest
            } else {
                logErr("  GAGAL: ${entry.id}")
            }
        }
        log("=== checkAndUpdate END: ${updated.size} updated ===")
        updated
    }

    private fun fetchIndex(): EngineIndex? {
        return try {
            val req = Request.Builder()
                .url(indexUrl)
                .header("User-Agent", "RakaRa/updater")
                .header("Cache-Control", "no-cache")
                .build()

            log("GET $indexUrl")
            http.newCall(req).execute().use { resp ->
                log("HTTP ${resp.code} ${resp.message}")
                if (!resp.isSuccessful) {
                    logErr("Response not successful: HTTP ${resp.code}")
                    return null
                }
                val body = resp.body?.string().orEmpty()
                log("Body ${body.length} chars, prefix: ${body.take(120)}")

                try {
                    val parsed = json.decodeFromString<EngineIndex>(body)
                    log("Parse OK, ${parsed.engines.size} engines")
                    parsed
                } catch (e: Exception) {
                    logErr("JSON parse FAILED", e)
                    null
                }
            }
        } catch (e: Exception) {
            logErr("fetchIndex exception", e)
            null
        }
    }

    private suspend fun downloadAndInstall(entry: EngineEntry): EngineManifest? {
        return try {
            val resolvedUrl = resolveUrl(entry)
            log("  GET $resolvedUrl")
            val req = Request.Builder()
                .url(resolvedUrl)
                .header("User-Agent", "RakaRa/updater")
                .header("Cache-Control", "no-cache")
                .build()

            val tmp = File(context.cacheDir, "upd-${entry.id}.rkx")
            log("  -> ${tmp.absolutePath}")

            var downloaded = false
            http.newCall(req).execute().use { resp ->
                log("  HTTP ${resp.code} ${resp.message}")
                if (!resp.isSuccessful) {
                    logErr("  Download not successful")
                    return null
                }
                val body = resp.body
                if (body == null) {
                    logErr("  Response body null")
                    return null
                }
                tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                downloaded = true
            }

            if (!downloaded) {
                logErr("  Download failed, no data written")
                return null
            }
            log("  Downloaded ${tmp.length()} bytes (expected ${entry.size})")

            val result = installer.install(tmp)
            tmp.delete()

            if (result.isSuccess) {
                log("  install() OK")
                result.getOrNull()
            } else {
                logErr("  install() FAILED", result.exceptionOrNull())
                null
            }
        } catch (e: Exception) {
            logErr("  downloadAndInstall exception", e)
            null
        }
    }
}