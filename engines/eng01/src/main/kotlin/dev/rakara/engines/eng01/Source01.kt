package dev.rakara.engines.eng01

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
import java.util.concurrent.TimeUnit

class Source01 : Source {
    override val id: Long = 0L
    override val name: String = "Engine 1"
    override val lang: String = "id"
    override val versionId: Int = 6
    override val supportsLatest: Boolean = true

    private val baseUrl = K.url
    private val userAgent = K.ua

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    

    override suspend fun getPopular(page: Int): MangasPage = fetchCatalog(page)
    override suspend fun getLatest(page: Int): MangasPage = fetchCatalog(page)

    override suspend fun search(query: String, page: Int, filters: FilterList): MangasPage =
        withContext(Dispatchers.IO) {
            if (page > 1) return@withContext MangasPage(emptyList(), false)
            if (query.isBlank()) return@withContext fetchCatalog(1)
            val all = fetchCatalogRaw(1)
            val q = query.lowercase()
            MangasPage(all.filter { it.title.lowercase().contains(q) }.map { it.toSManga() }, false)
        }

    

    override suspend fun getDetails(manga: SManga): SManga = withContext(Dispatchers.IO) {
        val slug = cleanSlug(manga.url)
        val html = httpGet("$baseUrl/komik/$slug/")
        val doc = Jsoup.parse(html)

        var title = doc.selectFirst("h1")?.text().orEmpty()
        if (title.isBlank()) {
            title = doc.selectFirst("meta[property=og:title]")?.attr("content")
                ?.substringBefore(" Bahasa Indonesia").orEmpty()
        }

        var description = doc.selectFirst("div#content-detail p")?.text().orEmpty()
        if (description.isBlank()) {
            description = doc.selectFirst("meta[property=og:description]")?.attr("content").orEmpty()
        }

        var image = doc.selectFirst("section[aria-label^=Header] img")?.attr("src").orEmpty()
        if (image.isBlank()) {
            image = doc.selectFirst("meta[property=og:image]")?.attr("content").orEmpty()
        }

        val author = doc.select("ul li:contains(Author) a").joinToString(", ") { it.text() }
        val statusText = doc.selectFirst("ul li:contains(Status) a")?.text() ?: ""
        val status = when {
            statusText.contains("Ongoing", true) -> SManga.Status.ONGOING
            statusText.contains("Completed", true) -> SManga.Status.COMPLETED
            statusText.contains("Hiatus", true) -> SManga.Status.ON_HIATUS
            else -> SManga.Status.UNKNOWN
        }
        val type = doc.selectFirst("ul li:contains(Tipe) a")?.text() ?: ""

        manga.copy(
            title = title.ifBlank { slug.replace("-", " ").replaceFirstChar { it.uppercase() } },
            author = author.ifBlank { null },
            description = description.ifBlank { null },
            thumbnailUrl = image.ifBlank { null },
            genre = listOfNotNull(type.takeIf { it.isNotBlank() }),
            status = status,
            initialized = true,
        )
    }

    
    
    
    
    
    
    
    
    
    
    override suspend fun getChapters(manga: SManga): List<SChapter> = withContext(Dispatchers.IO) {
        val slug = cleanSlug(manga.url)
        val html = httpGet("$baseUrl/komik/$slug/")
        val doc = Jsoup.parse(html)
        val allChapters = mutableMapOf<String, SChapter>()

        fun addCh(numStr: String) {
            val num = numStr.toFloatOrNull() ?: return
            if (num <= 0f) return
            val chUrl = "$slug-chapter-$numStr"
            if (!allChapters.containsKey(chUrl)) {
                allChapters[chUrl] = SChapter(
                    url = chUrl,
                    name = "Chapter $numStr",
                    chapterNumber = num,
                    dateUpload = 0L,
                )
            }
        }

        
        doc.select("a[href^=/komik/${slug}/chapter-]").forEach { a ->
            val m = Regex("/komik/${Regex.escape(slug)}/chapter-([^/]+)/").find(a.attr("href"))
            m?.groupValues?.get(1)?.let { addCh(it) }
        }
        val domCount = allChapters.size

        
        val qjsonRegex = Regex("<script type=\"qwik/json\">(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
        qjsonRegex.find(html)?.groupValues?.get(1)?.let { qjson ->
            val pattern = Regex(
                "\"error_reason\":\"[^\"]*\"\\s*}\\s*,\\s*\\d+\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*(?:,\\s*\\d+\\s*)?,\\s*\"\\d{4}-\\d{2}-\\d{2}T"
            )
            pattern.findAll(qjson).forEach { m ->
                addCh(m.groupValues[1])
            }
        }

        println("RAKARA eng01 [$slug]: DOM=$domCount, Total=${allChapters.size}")

        allChapters.values.sortedByDescending { it.chapterNumber }
    }

    

    override suspend fun getPages(chapter: SChapter): List<SPage> = withContext(Dispatchers.IO) {
        val fullSlug = cleanSlug(chapter.url)
        val m = Regex("^(.*)-chapter-(\\d+(?:\\.\\d+)?)$").find(fullSlug)
            ?: error("format chapter tidak valid: $fullSlug")
        val series = m.groupValues[1]
        val num = m.groupValues[2]
        val html = httpGet("$baseUrl/komik/$series/chapter-$num/")
        val urls = extractImages(num, html)
        if (urls.isEmpty()) {
            throw IllegalStateException("Tidak ada gambar untuk chapter '$fullSlug'.")
        }
        urls.mapIndexed { i, url ->
            val abs = if (url.startsWith("http")) url else baseUrl + url
            SPage(index = i, url = chapter.url, imageUrl = abs)
        }
    }

    override fun close() {
        runCatching {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

    

    private suspend fun fetchCatalog(page: Int): MangasPage = withContext(Dispatchers.IO) {
        MangasPage(fetchCatalogRaw(page).map { it.toSManga() }, hasNextPage = false)
    }

    private suspend fun fetchCatalogRaw(page: Int): List<CatalogItem> {
        val url = if (page > 1) "$baseUrl/?page=$page" else "$baseUrl/"
        return normalizeCatalog(httpGet(url))
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
        val latest: String,
    ) {
        fun toSManga(): SManga = SManga(
            url = slug,
            title = title,
            thumbnailUrl = image.ifBlank { null },
            genre = listOfNotNull(type.takeIf { it.isNotBlank() }),
        )
    }

    private fun normalizeCatalog(html: String): List<CatalogItem> {
        val seen = mutableSetOf<String>()
        val out = mutableListOf<CatalogItem>()

        var blocks = Regex("<article\\b.*?</article>", RegexOption.DOT_MATCHES_ALL)
            .findAll(html).map { it.value }.toList()
        if (blocks.isEmpty()) blocks = listOf(html)

        for (block in blocks) {
            val slugMatch = Regex("href=\"/komik/([^/\"?#]+)/\"").find(block) ?: continue
            val slug = slugMatch.groupValues[1]
            if (!seen.add(slug)) continue

            var cover = Regex("<img[^>]+src=\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
                .find(block)?.groupValues?.get(1).orEmpty()
            cover = unescapeHtml(cover)
            if (cover.isNotBlank() && !cover.startsWith("http")) cover = baseUrl + cover

            
            var title = Regex("<h3[^>]*title=\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
                .find(block)?.groupValues?.get(1).orEmpty()
            if (title.isBlank()) {
                title = Regex("<h3[^>]*>(.*?)</h3>", RegexOption.DOT_MATCHES_ALL)
                    .find(block)?.groupValues?.get(1)?.let { cleanText(it) }.orEmpty()
            }
            title = unescapeHtml(title)
            if (title.isBlank()) title = slug.replace("-", " ").replaceFirstChar { it.uppercase() }

            val type = Regex(">(Manga|Manhwa|Manhua)<").find(block)?.groupValues?.get(1).orEmpty()
            val latest = Regex("Ch\\.\\s*(\\d+(?:\\.\\d+)?)").find(block)?.groupValues?.get(1) ?: ""

            out += CatalogItem(slug, title, type, cover, latest)
        }
        return out
    }

    

    private fun extractImages(chapterNum: String, html: String): List<String> {
        val doc = Jsoup.parse(html)
        val imgElements = doc.select("img[id^=page-img-]")
        if (imgElements.isNotEmpty()) {
            return imgElements.mapNotNull {
                val src = it.attr("src").trim()
                if (src.startsWith("http")) src else null
            }
        }
        val urls = LinkedHashSet<String>()
        val pattern = Regex(
            "https://[^\"\\s']+/img/[^\"\\s']+/[^/]+/\\d+-[a-zA-Z0-9]+\\.(?:jpg|jpeg|png|webp)",
            RegexOption.IGNORE_CASE
        )
        for (m in pattern.findAll(html)) urls += m.value
        if (urls.isNotEmpty()) return urls.toList()
        val oldPattern = Regex(
            "https://[^\"\\s']+/0*${Regex.escape(chapterNum)}/\\d+\\.(?:jpg|jpeg|png|webp)",
            RegexOption.IGNORE_CASE
        )
        for (m in oldPattern.findAll(html)) urls += m.value
        return urls.toList()
    }

    

    private fun cleanSlug(s: String): String = s.trim('/').removePrefix("komik/")

    private fun cleanText(s: String): String {
        var out = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL).replace(s, "")
        out = Regex("<[^>]+>").replace(out, "")
        out = unescapeHtml(out)
        return Regex("\\s+").replace(out, " ").trim()
    }

    private fun unescapeHtml(s: String): String = try {
        Parser.unescapeEntities(s, false)
    } catch (_: Exception) { s }
}