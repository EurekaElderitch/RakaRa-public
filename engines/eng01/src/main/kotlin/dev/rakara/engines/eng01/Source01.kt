package dev.rakara.engines.eng01

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

class Source01 : Source {
    override val id: Long = 0L
    override val name: String = "Engine 1"
    override val lang: String = "id"
    override val versionId: Int = 12   
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
            val params = mutableListOf<String>()
            if (query.isNotBlank()) params.add("q=${URLEncoder.encode(query, "UTF-8")}")
            if (page > 1) params.add("page=$page")

            filters.filters.forEach { filter ->
                when (filter) {
                    is Filter.Select -> {
                        val selectedValue = filter.values.getOrNull(filter.selected)
                        if (!selectedValue.isNullOrBlank()) {
                            when (filter.name.lowercase()) {
                                "tipe", "type" -> params.add("ti=$selectedValue")
                                "genre" -> params.add("ge=$selectedValue")
                                "status" -> params.add("o=$selectedValue")
                                else -> params.add("${filter.name.lowercase()}=$selectedValue")
                            }
                        }
                    }
                    is Filter.Text -> {
                        if (filter.state.isNotBlank()) {
                            params.add("${filter.name.lowercase()}=${URLEncoder.encode(filter.state, "UTF-8")}")
                        }
                    }
                    else -> {}
                }
            }

            val url = if (params.isEmpty()) {
                "$baseUrl/"
            } else {
                "$baseUrl/browse/?${params.joinToString("&")}"
            }

            val html = httpGet(url)
            val doc = Jsoup.parse(html)

            val items = normalizeCatalog(html)
            val hasNext = doc.select("a[href*='?page=${page + 1}']").isNotEmpty() ||
                          doc.select("nav[aria-label='Pagination'] a:contains(Next)").isNotEmpty() ||
                          doc.select("a:contains(Next)").isNotEmpty()

            MangasPage(items.map { it.toSManga() }, hasNextPage = hasNext)
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

        
        fun addChWithTimestamp(numStr: String, timestamp: Long) {
            val num = numStr.toFloatOrNull() ?: return
            if (num <= 0f) return
            val chUrl = "$slug-chapter-$numStr"
            val existing = allChapters[chUrl]
            when {
                existing == null -> allChapters[chUrl] = SChapter(
                    url = chUrl,
                    name = "Chapter $numStr",
                    chapterNumber = num,
                    dateUpload = timestamp,
                )
                existing.dateUpload == 0L && timestamp > 0L ->
                    allChapters[chUrl] = existing.copy(dateUpload = timestamp)
            }
        }

        
        val jsonLdDates: Map<String, Long> = parseJsonLdChapters(html)
        println("RAKARA eng01 [$slug]: JSON-LD=${jsonLdDates.size} chapters")

        
        doc.select("a[href^=/komik/${slug}/chapter-]").forEach { a ->
            val m = Regex("/komik/${Regex.escape(slug)}/chapter-([^/]+)/").find(a.attr("href"))
            val numStr = m?.groupValues?.get(1) ?: return@forEach

            
            val ts = jsonLdDates[numStr] ?: run {
                val article = a.parents().firstOrNull { it.tagName() == "article" }
                    ?: a.parent()
                val dateText = article.selectFirst("p.text-xs")?.text()
                    ?: article.selectFirst("p[class*=text-gray]")?.text()
                    ?: ""
                parseRelativeDate(dateText)
            }

            addChWithTimestamp(numStr, ts)
        }

        val domCount = allChapters.size
        val domWithDate = allChapters.values.count { it.dateUpload > 0L }
        val domMax = allChapters.values.maxOfOrNull { it.chapterNumber } ?: 0f

        
        val CHAPTER_GAP_THRESHOLD = 20f
        val qjsonRegex = Regex("<script type=\"qwik/json\">(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
        qjsonRegex.find(html)?.groupValues?.get(1)?.let { qjson ->
            val pattern = Regex(
                "\"error_reason\":\"[^\"]*\"\\s*\\}\\s*,\\s*\\d+\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*(?:,\\s*\\d+\\s*)?,\\s*\"([^\"]+)\""
            )
            pattern.findAll(qjson).forEach { m ->
                val num = m.groupValues[1]
                val numFloat = num.toFloatOrNull() ?: return@forEach

                
                if (domMax > 0f && (numFloat - domMax) > CHAPTER_GAP_THRESHOLD) {
                    println(
                        "RAKARA eng01 [$slug]: drop Qwik chapter $num " +
                        "(gap ${numFloat - domMax} > $CHAPTER_GAP_THRESHOLD dari DOM max $domMax)"
                    )
                    return@forEach
                }

                val ts = parseIsoDate(m.groupValues[2])
                addChWithTimestamp(num, ts)
            }
        }

        val withDate = allChapters.values.count { it.dateUpload > 0L }
        println(
            "RAKARA eng01 [$slug]: DOM=$domCount (withDate=$domWithDate), " +
            "DomMax=$domMax, Total=${allChapters.size}, WithDate=$withDate"
        )

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
        val url = if (page > 1) "$baseUrl/browse/?page=$page" else "$baseUrl/"
        val html = httpGet(url)
        val doc = Jsoup.parse(html)

        val items = normalizeCatalog(html)
        val hasNext = doc.select("a[href*='?page=${page + 1}']").isNotEmpty() ||
                      doc.select("nav[aria-label='Pagination'] a:contains(Next)").isNotEmpty() ||
                      doc.select("a:contains(Next)").isNotEmpty()

        MangasPage(items.map { it.toSManga() }, hasNextPage = hasNext)
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

    
    private fun parseJsonLdChapters(html: String): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        val blockRegex = Regex(
            "\\{[^{}]*?\"@type\"\\s*:\\s*\"ComicIssue\"[^{}]*?\\}",
            RegexOption.DOT_MATCHES_ALL
        )
        val numRegex = Regex("\"issueNumber\"\\s*:\\s*\"([^\"]+)\"")
        val dateRegex = Regex("\"datePublished\"\\s*:\\s*\"([^\"]+)\"")

        blockRegex.findAll(html).forEach { blockMatch ->
            val block = blockMatch.value
            val num = numRegex.find(block)?.groupValues?.get(1)?.trim()
            val dateStr = dateRegex.find(block)?.groupValues?.get(1)?.trim()
            if (!num.isNullOrBlank() && !dateStr.isNullOrBlank()) {
                val ts = parseIsoDate(dateStr)
                if (ts > 0) result[num] = ts
            }
        }
        return result
    }

    
    private fun parseIsoDate(iso: String): Long {
        if (iso.isBlank()) return 0L
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
        )
        for (fmt in formats) {
            try {
                val parser = SimpleDateFormat(fmt, Locale.US)
                if (fmt.endsWith("'Z'")) {
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                }
                val d = parser.parse(iso)
                if (d != null) return d.time
            } catch (_: Exception) { }
        }
        return 0L
    }

    
    private fun parseRelativeDate(text: String): Long {
        val t = text.trim().lowercase()
        if (t.isBlank()) return 0L
        val now = System.currentTimeMillis()

        Regex("(\\d+)\\s*menit\\s*lalu").find(t)?.let {
            return now - it.groupValues[1].toLong() * 60_000L
        }
        Regex("(\\d+)\\s*jam\\s*lalu").find(t)?.let {
            return now - it.groupValues[1].toLong() * 3_600_000L
        }
        Regex("(\\d+)\\s*hari\\s*lalu").find(t)?.let {
            return now - it.groupValues[1].toLong() * 86_400_000L
        }
        Regex("(\\d+)\\s*minggu\\s*lalu").find(t)?.let {
            return now - it.groupValues[1].toLong() * 7 * 86_400_000L
        }
        Regex("(\\d+)\\s*bulan\\s*lalu").find(t)?.let {
            return now - it.groupValues[1].toLong() * 30 * 86_400_000L
        }
        Regex("(\\d+)\\s*tahun\\s*lalu").find(t)?.let {
            return now - it.groupValues[1].toLong() * 365 * 86_400_000L
        }
        if (t.contains("baru saja") || t.contains("just now")) return now - 60_000L
        if (t.contains("kemarin")) return now - 86_400_000L
        return 0L
    }
}