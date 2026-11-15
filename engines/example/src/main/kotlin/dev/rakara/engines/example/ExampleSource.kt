package dev.rakara.engines.example

import dev.rakara.source.FilterList
import dev.rakara.source.MangasPage
import dev.rakara.source.SChapter
import dev.rakara.source.SManga
import dev.rakara.source.SPage
import dev.rakara.source.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.util.concurrent.TimeUnit


class ExampleSource : Source {

    override val id: Long = 0L            
    override val name: String = "Example"
    override val lang: String = "en"
    override val versionId: Int = 1

    override val supportsLatest: Boolean = true

    private val baseUrl = "https://example.com"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    

    override suspend fun getPopular(page: Int): MangasPage =
        fetchList("$baseUrl/popular?page=$page")

    override suspend fun getLatest(page: Int): MangasPage =
        fetchList("$baseUrl/latest?page=$page")

    

    override suspend fun search(query: String, page: Int, filters: FilterList): MangasPage {
        val q = URLEncoder.encode(query, "UTF-8")
        return fetchList("$baseUrl/search?q=$q&page=$page")
    }

    

    override suspend fun getDetails(manga: SManga): SManga {
        val doc = fetch(baseUrl + manga.url)
        return manga.copy(
            title = doc.selectFirst("h1.title")?.text()?.ifBlank { manga.title } ?: manga.title,
            author = doc.selectFirst(".author")?.text()?.ifBlank { null },
            artist = doc.selectFirst(".artist")?.text()?.ifBlank { null },
            description = doc.selectFirst(".description")?.text()?.ifBlank { null },
            genre = doc.select(".genre a").map { it.text() },
            thumbnailUrl = doc.selectFirst("img.cover")?.attr("abs:src")?.ifBlank { null },
            initialized = true,
        )
    }

    

    override suspend fun getChapters(manga: SManga): List<SChapter> {
        val doc = fetch(baseUrl + manga.url)
        return doc.select("#chapters li a").mapNotNull { a ->
            val href = a.attr("href").ifBlank { return@mapNotNull null }
            val text = a.text().ifBlank { return@mapNotNull null }
            val number = Regex("""([\d.]+)""")
                .find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: -1f
            SChapter(
                url = href,
                name = text,
                chapterNumber = number,
            )
        }
    }

    

    override suspend fun getPages(chapter: SChapter): List<SPage> {
        val doc = fetch(baseUrl + chapter.url)
        return doc.select("#reader img").mapIndexedNotNull { i, img ->
            val src = img.attr("abs:src").ifBlank { return@mapIndexedNotNull null }
            SPage(index = i, url = chapter.url, imageUrl = src)
        }
    }

    

    override fun close() {
        runCatching {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            client.cache?.close()
        }
    }

    

    private suspend fun fetchList(url: String): MangasPage = withContext(Dispatchers.IO) {
        val doc = fetch(url)
        val items = doc.select(".manga-item").mapNotNull { el ->
            val href = el.selectFirst("a")?.attr("href")?.ifBlank { return@mapNotNull null }
                ?: return@mapNotNull null
            SManga(
                url = href,
                title = el.selectFirst(".title")?.text() ?: href,
                thumbnailUrl = el.selectFirst("img")?.attr("abs:src")?.ifBlank { null },
            )
        }
        MangasPage(
            mangas = items,
            hasNextPage = doc.selectFirst("a.next, .pagination .next") != null,
        )
    }

    private suspend fun fetch(path: String): Document = withContext(Dispatchers.IO) {
        val url = if (path.startsWith("http")) path else baseUrl + path
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible; RakaRa/0.1)")
            .header("Accept-Language", "en,id;q=0.8")
            .header("Referer", baseUrl)
            .build()
        val body = client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} untuk $url")
            resp.body?.string().orEmpty()
        }
        Jsoup.parse(body, url)
    }
}
