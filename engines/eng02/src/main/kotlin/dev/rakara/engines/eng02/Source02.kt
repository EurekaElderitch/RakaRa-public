package dev.rakara.engines.eng02

import dev.rakara.source.Filter
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
import org.jsoup.parser.Parser
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class Source02 : Source {
    override val id: Long = 1L
    override val name: String = "Engine 2"
    override val lang: String = "id"
    override val versionId: Int = 1
    override val supportsLatest: Boolean = true

    private val baseUrl = K2.url
    private val userAgent = K2.ua

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    

    override suspend fun getPopular(page: Int): MangasPage = withContext(Dispatchers.IO) {
        
        val url = if (page > 1) "$baseUrl/p/ranking/?page=$page" else "$baseUrl/p/ranking/"
        val html = httpGet(url)
        val doc = Jsoup.parse(html)

        
        var articles = doc.select("#rank-harian article.ls4")
        if (articles.isEmpty()) articles = doc.select("article.ls4")

        val items = articles.mapNotNull { parseCatalogItem(it) }
        val hasNext = doc.selectFirst("a[href*='?page=${page + 1}']") != null ||
                      doc.selectFirst("a.next") != null

        println("RAKARA eng02 [popular p$page]: items=${items.size}, hasNext=$hasNext")
        MangasPage(items.map { it.toSManga() }, hasNextPage = hasNext)
    }

    override suspend fun getLatest(page: Int): MangasPage = withContext(Dispatchers.IO) {
        
        val url = if (page == 1) "$baseUrl/" else "$baseUrl/pustaka/?page=$page"
        val html = try {
            httpGet(url)
        } catch (e: Exception) {
            
            httpGet("https://api.komiku.org/manga/?orderby=modified&page=$page")
        }
        val doc = Jsoup.parse(html)

        
        var articles = doc.select("article.ls2")
        if (articles.isEmpty()) articles = doc.select("article.ls4")
        if (articles.isEmpty()) articles = doc.select("article:has(a[href^=/manga/])")

        val items = articles.mapNotNull { parseCatalogItem(it) }
        val hasNext = doc.selectFirst("a[href*='?page=${page + 1}']") != null ||
                      doc.selectFirst("a.next") != null

        println("RAKARA eng02 [latest p$page]: items=${items.size}, hasNext=$hasNext")
        MangasPage(items.map { it.toSManga() }, hasNextPage = hasNext)
    }

    override suspend fun search(query: String, page: Int, filters: FilterList): MangasPage =
        withContext(Dispatchers.IO) {
            val q = URLEncoder.encode(query, "UTF-8")
            val url = "$baseUrl/?post_type=manga&s=$q" + if (page > 1) "&paged=$page" else ""
            val html = httpGet(url)
            val doc = Jsoup.parse(html)

            var articles = doc.select("article.ls2")
            if (articles.isEmpty()) articles = doc.select("article.ls4")
            if (articles.isEmpty()) articles = doc.select("article:has(a[href^=/manga/])")

            val items = articles.mapNotNull { parseCatalogItem(it) }
            val hasNext = doc.selectFirst("a[href*='paged=${page + 1}']") != null ||
                          doc.selectFirst("a.next") != null

            println("RAKARA eng02 [search '$query' p$page]: items=${items.size}, hasNext=$hasNext")
            MangasPage(items.map { it.toSManga() }, hasNextPage = hasNext)
        }

    

    override suspend fun getDetails(manga: SManga): SManga = withContext(Dispatchers.IO) {
        val slug = cleanSlug(manga.url)
        val html = httpGet("$baseUrl/manga/$slug/")
        val doc = Jsoup.parse(html)

        var title = doc.selectFirst("h1")?.text().orEmpty()
            .ifBlank { doc.selectFirst("meta[property=og:title]")?.attr("content")
                ?.substringBefore(" - Komiku").orEmpty() }
            .ifBlank { doc.selectFirst("meta[property=og:title]")?.attr("content")
                ?.substringBefore(" Bahasa Indonesia").orEmpty() }

        var description = doc.selectFirst("p.desc")?.text().orEmpty()
            .ifBlank { doc.selectFirst("meta[property=og:description]")?.attr("content").orEmpty() }

        var image = doc.selectFirst("meta[property=og:image]")?.attr("content").orEmpty()
            .ifBlank { doc.selectFirst("div.ims img")?.attr("src").orEmpty() }

        val author = doc.select("table.inftable tr:contains(Author) td").last()?.text().orEmpty()
        val statusText = doc.select("table.inftable tr:contains(Status) td").last()?.text().orEmpty()
        val status = when {
            statusText.contains("Ongoing", true) -> SManga.Status.ONGOING
            statusText.contains("Tamat", true) -> SManga.Status.COMPLETED
            statusText.contains("Completed", true) -> SManga.Status.COMPLETED
            statusText.contains("Hiatus", true) -> SManga.Status.ON_HIATUS
            else -> SManga.Status.UNKNOWN
        }

        val genres = mutableListOf<String>()
        doc.select("ul.genre li.genre a span").forEach { genres.add(it.text()) }

        
        val typeText = doc.select("table.inftable tr:contains(Tipe) td").last()?.text().orEmpty()
        if (typeText.isNotBlank() && typeText !in genres) genres.add(typeText)

        manga.copy(
            title = title.ifBlank { slug.replace("-", " ").replaceFirstChar { it.uppercase() } },
            author = author.ifBlank { null },
            description = description.ifBlank { null },
            thumbnailUrl = image.ifBlank { null },
            genre = genres,
            status = status,
            initialized = true,
        )
    }

    

    override suspend fun getChapters(manga: SManga): List<SChapter> = withContext(Dispatchers.IO) {
        val slug = cleanSlug(manga.url)
        val html = httpGet("$baseUrl/manga/$slug/")
        val doc = Jsoup.parse(html)

        val chapters = mutableListOf<SChapter>()
        val rows = doc.select("table#Daftar_Chapter tbody tr")
        println("RAKARA eng02 [$slug]: chapter rows=${rows.size}")

        rows.forEach { row ->
            parseChapterRow(row)?.let { chapters.add(it) }
        }

        
        val unique = chapters.associateBy { it.url }.values.toList()
        println("RAKARA eng02 [$slug]: chapters=${unique.size}")
        unique.sortedByDescending { it.chapterNumber }
    }

    private fun parseChapterRow(row: org.jsoup.nodes.Element): SChapter? {
        val link = row.selectFirst("td.judulseries a[itemprop=url]")
            ?: row.selectFirst("td.judulseries a[href*='-chapter-']")
            ?: return null
        val href = link.attr("href")
        if (href.isBlank()) return null

        val title = link.text()
        val num = Regex("chapter-([\\d.]+)").find(href)?.groupValues?.get(1)?.toFloatOrNull()
            ?: Regex("Chapter\\s+([\\d.]+)", RegexOption.IGNORE_CASE).find(title)?.groupValues?.get(1)?.toFloatOrNull()
            ?: return null
        if (num <= 0f) return null

        val dateText = row.selectFirst("td.tanggalseries")?.text().orEmpty()
        var timestamp = 0L
        if (dateText.isNotBlank()) {
            timestamp = parseDate(dateText)
        }

        val cleanHref = href.trimStart('/').trimEnd('/')
        return SChapter(
            url = cleanHref,
            name = "Chapter $num",
            chapterNumber = num,
            dateUpload = timestamp,
        )
    }

    

    override suspend fun getPages(chapter: SChapter): List<SPage> = withContext(Dispatchers.IO) {
        val path = chapter.url.trimStart('/').trimEnd('/')
        val html = httpGet("$baseUrl/$path/")
        val doc = Jsoup.parse(html)

        val container = doc.selectFirst("div#Baca_Komik") ?: doc.selectFirst("#Baca_Komik")
            ?: throw IllegalStateException("Tidak ada gambar untuk chapter '${chapter.url}'")

        val images = container.select("img").mapNotNull { img ->
            val src = img.attr("src").ifBlank { img.attr("data-src") }
            if (src.startsWith("http")) src else null
        }

        if (images.isEmpty()) {
            throw IllegalStateException("Tidak ada gambar untuk chapter '${chapter.url}'")
        }

        println("RAKARA eng02 [pages]: chapter=${chapter.url}, images=${images.size}")
        images.mapIndexed { i, url -> SPage(index = i, url = chapter.url, imageUrl = url) }
    }

    override fun close() {
        runCatching {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

    

    private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8")
            .header("Referer", "$baseUrl/")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} untuk $url")
            resp.body?.string().orEmpty()
        }
    }

    

    private data class CatalogItem(
        val slug: String,
        val title: String,
        val type: String,
        val image: String,
    ) {
        fun toSManga(): SManga = SManga(
            url = slug,
            title = title,
            thumbnailUrl = image.ifBlank { null },
            genre = listOfNotNull(type.takeIf { it.isNotBlank() }),
        )
    }

    private fun parseCatalogItem(article: org.jsoup.nodes.Element): CatalogItem? {
        
        val link = article.selectFirst("a[href^=/manga/]") ?: return null
        val href = link.attr("href")
        val slug = href.removePrefix("/manga/").trimEnd('/')
        if (slug.isBlank()) return null

        
        var cover = article.selectFirst("img.lazy")?.attr("data-src").orEmpty()
        if (cover.isBlank()) cover = article.selectFirst("img[data-src]")?.attr("data-src").orEmpty()
        if (cover.isBlank()) cover = article.selectFirst("img")?.attr("src").orEmpty()
        cover = unescapeHtml(cover).trim()
        if (cover.isNotBlank() && !cover.startsWith("http")) cover = baseUrl + cover

        
        var title = article.selectFirst("h3 a")?.text().orEmpty()
        if (title.isBlank()) title = article.selectFirst("h4 a")?.text().orEmpty()
        title = unescapeHtml(title).trim()
        if (title.isBlank()) title = slug.replace("-", " ").replaceFirstChar { it.uppercase() }

        
        val flagSrc = article.selectFirst("img.flag")?.attr("src").orEmpty()
        val type = when {
            flagSrc.contains("/jp.png") -> "Manga"
            flagSrc.contains("/kr.png") -> "Manhwa"
            flagSrc.contains("/cn.png") -> "Manhua"
            else -> ""
        }

        return CatalogItem(slug, title, type, cover)
    }

    

    private fun cleanSlug(s: String): String =
        s.trim('/').removePrefix("manga/")

    private fun unescapeHtml(s: String): String = try {
        Parser.unescapeEntities(s, false)
    } catch (_: Exception) { s }

    private fun parseDate(text: String): Long {
        
        return try {
            val parser = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            parser.parse(text)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}