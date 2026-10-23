package dev.rakara.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ApkUpdateInfo(
    val version: String,
    val downloadUrl: String,
    val size: Long,
    val changelog: String?,
)

class ApkUpdater(
    private val ctx: Context,
    private val repo: String = "EurekaElderitch/RakaRa-public",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun checkForUpdate(): ApkUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$repo/releases?per_page=50"
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "RakaRa-Updater")
                .header("Cache-Control", "no-cache")
                .build()
                
            val body = client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) {
                    Log.e("ApkUpdater", "GitHub API Error: ${r.code} - ${r.message}")
                    return@withContext null
                }
                r.body?.string() ?: return@withContext null
            }
            
            val arr = json.parseToJsonElement(body) as? JsonArray ?: return@withContext null

            data class Cand(val ver: String, val url: String, val size: Long, val body: String?)

            val candidates = mutableListOf<Cand>()
            for (el in arr) {
                val obj = el as? JsonObject ?: continue
                val tag = (obj["tag_name"] as? JsonPrimitive)?.contentOrNull ?: continue
                if (!tag.startsWith("app-v")) continue
                val ver = tag.removePrefix("app-v").trim()
                val assets = obj["assets"] as? JsonArray ?: continue
                for (a in assets) {
                    val aObj = a as? JsonObject ?: continue
                    val name = (aObj["name"] as? JsonPrimitive)?.contentOrNull ?: continue
                    if (!name.endsWith(".apk")) continue
                    val dlUrl = (aObj["browser_download_url"] as? JsonPrimitive)?.contentOrNull ?: continue
                    val size = (aObj["size"] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L
                    val bodyText = (obj["body"] as? JsonPrimitive)?.contentOrNull
                    candidates += Cand(ver, dlUrl, size, bodyText)
                    break
                }
            }
            
            if (candidates.isEmpty()) {
                Log.d("ApkUpdater", "Tidak ada rilis .apk yang ditemukan.")
                return@withContext null
            }

            
            val best = candidates.maxByOrNull { versionToLong(it.ver) } ?: return@withContext null
            
            val localVer = try {
                ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0.0.0"
            } catch (e: Exception) {
                "0.0.0"
            }
            
            Log.d("ApkUpdater", "Versi lokal: $localVer | Versi remote: ${best.ver}")

            if (versionToLong(best.ver) <= versionToLong(localVer)) {
                Log.d("ApkUpdater", "Aplikasi sudah versi terbaru.")
                return@withContext null
            }

            ApkUpdateInfo(
                version = best.ver,
                downloadUrl = best.url,
                size = best.size,
                changelog = best.body,
            )
        } catch (e: Exception) {
            Log.e("ApkUpdater", "Gagal cek update: ${e.message}")
            null
        }
    }

    suspend fun downloadApk(
        info: ApkUpdateInfo,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
            val file = File(dir, "RakaRa-${info.version}.apk")
            if (file.exists() && file.length() == info.size && info.size > 0) {
                return@withContext file
            }

            val req = Request.Builder()
                .url(info.downloadUrl)
                .header("User-Agent", "RakaRa-Updater")
                .header("Cache-Control", "no-cache")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body ?: return@withContext null
                val total = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        val buf = ByteArray(8192)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            output.write(buf, 0, read)
                            downloaded += read
                            withContext(Dispatchers.Main) { onProgress(downloaded, total) }
                        }
                    }
                }
            }
            file
        } catch (e: Exception) {
            Log.e("ApkUpdater", "Gagal download: ${e.message}")
            null
        }
    }

    fun installApk(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!ctx.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                return
            }
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }

    
    private fun versionToLong(v: String): Long {
        val parts = v.split('.').map { it.toLongOrNull() ?: 0L }
        return (parts.getOrElse(0) { 0L } * 1000000L) + 
               (parts.getOrElse(1) { 0L } * 1000L) + 
               parts.getOrElse(2) { 0L }
    }
}