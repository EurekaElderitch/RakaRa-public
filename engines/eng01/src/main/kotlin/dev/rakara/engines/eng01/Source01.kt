package dev.rakara.engines.eng01

import dev.rakara.source.FilterList
import dev.rakara.source.MangasPage
import dev.rakara.source.SChapter
import dev.rakara.source.SManga
import dev.rakara.source.SPage
import dev.rakara.source.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.util.concurrent.TimeUnit

class Source01 : Source {
    override val id: Long = 0L
    override val name: String = "Engine 1"
    override val lang: String = "id"
    override val versionId: Int = 1
    override val supportsLatest: Boolean = true

    private val baseUrl = K.url
    private val userAgent = K.ua
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
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
        val parsed = normalizeComic(slug, html)
        manga.copy(
            title = parsed.title,
            author = parsed.author.ifBlank { null },
            description = parsed.description.ifBlank { null },
            thumbnailUrl = parsed.image.ifBlank { null },
            genre = listOfNotNull(parsed.type.takeIf { it.isNotBlank() }),
            status = parsed.status,
            initialized = true,
        )
    }

    override suspend fun getChapters(manga: SManga): List<SChapter> = withContext(Dispatchers.IO) {
        val slug = cleanSlug(manga.url)
        val html = httpGet("$baseUrl/komik/$slug/")
        val parsed = normalizeComic(slug, html)
        parsed.chapters.map { (num, _) ->
            SChapter(
                url = "$slug-chapter-$num",
                name = "Chapter $num",
                chapterNumber = num.toFloatOrNull() ?: -1f,
                dateUpload = 0L,
            )
        }
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
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
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

    private data class ComicData(
        val title: String,
        val type: String,
        val status: SManga.Status,
        val image: String,
        val description: String,
        val author: String,
        val chapters: List<Pair<String, Long>>,
    )

    private fun normalizeComic(slug: String, html: String): ComicData {
        val ldList = parseLdJson(html)
        val ld = findLd(ldList, "ComicSeries", "Book", "CreativeWork", "Article")

        var title = ""
        var description = ""
        var image = ""
        var author = ""
        var type = ""

        if (ld != null) {
            title = ld.stringOrNull("name").orEmpty()
            description = ld.stringOrNull("description").orEmpty()
            image = when (val img = ld["image"]) {
                is JsonPrimitive -> img.contentOrNull.orEmpty()
                is JsonObject -> img.stringOrNull("url").orEmpty()
                else -> ""
            }
            author = when (val a = ld["author"]) {
                is JsonPrimitive -> a.contentOrNull.orEmpty()
                is JsonObject -> a.stringOrNull("name").orEmpty()
                is JsonArray -> {
                    val first = a.firstOrNull()
                    when (first) {
                        is JsonPrimitive -> first.contentOrNull.orEmpty()
                        is JsonObject -> first.stringOrNull("name").orEmpty()
                        else -> ""
                    }
                }
                else -> ""
            }
            type = when (val g = ld["genre"]) {
                is JsonPrimitive -> g.contentOrNull.orEmpty()
                is JsonArray -> (g.firstOrNull() as? JsonPrimitive)?.contentOrNull.orEmpty()
                else -> ""
            }
        }

        if (title.isBlank()) {
            Regex("<title[^>]*>([^<]*)</title>").find(html)?.let {
                title = unescapeHtml(it.groupValues[1]).substringBefore("|").trim()
            }
        }
        if (image.isBlank()) {
            Regex("<meta property=\"og:image\" content=\"([^\"]+)\"").find(html)?.let {
                image = unescapeHtml(it.groupValues[1])
            }
        }
        if (image.isBlank()) {
            Regex("<link rel=\"preload\" href=\"(https://[^\"]+/cover/[^\"]+\\.(?:jpg|jpeg|png|webp))\"").find(html)?.let {
                image = unescapeHtml(it.groupValues[1])
            }
        }
        if (description.isBlank()) {
            Regex("<meta name=\"description\" content=\"([^\"]*)\"").find(html)?.let {
                description = unescapeHtml(it.groupValues[1])
            }
        }
        if (title.isBlank()) {
            Regex("<h1[^>]*>(.*?)</h1>", RegexOption.DOT_MATCHES_ALL).find(html)?.let {
                title = cleanText(it.groupValues[1])
            }
        }
        if (type.isBlank()) {
            Regex(">(Manga|Manhwa|Manhua)<").find(html)?.let { type = it.groupValues[1] }
        }

        val status = when (
            Regex("(Ongoing|Completed|Hiatus)", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)?.lowercase()
        ) {
            "ongoing" -> SManga.Status.ONGOING
            "completed" -> SManga.Status.COMPLETED
            "hiatus" -> SManga.Status.ON_HIATUS
            else -> SManga.Status.UNKNOWN
        }

        val chRegex = Regex(
            "href=\"/komik/${Regex.escape(slug)}/chapter-(\\d+(?:\\.\\d+)?)/\"",
            RegexOption.IGNORE_CASE
        )
        val chapterNums = mutableSetOf<String>()
        for (m in chRegex.findAll(html)) chapterNums += m.groupValues[1]

        val chapters = chapterNums
            .sortedByDescending { it.toDoubleOrNull() ?: 0.0 }
            .map { it to 0L }

        return ComicData(
            title = title.ifBlank { slug.replace("-", " ").replaceFirstChar { it.uppercase() } },
            type = type,
            status = status,
            image = image,
            description = description,
            author = author,
            chapters = chapters,
        )
    }

    private fun normalizeCatalog(html: String): List<CatalogItem> {
        val seen = mutableSetOf<String>()
        val out = mutableListOf<CatalogItem>()

        var blocks = Regex("<article\\b.*?</article>", RegexOption.DOT_MATCHES_ALL)
            .findAll(html).map { it.value }.toList()

        if (blocks.isEmpty()) {
            blocks = Regex("<a\\s+href=\"/komik/([^/\"?#]+)/\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                .findAll(html).map { it.value }.toList()
        }

        if (blocks.isEmpty()) blocks = listOf(html)

        for (block in blocks) {
            val slugMatch = Regex("href=\"/komik/([^/\"?#]+)/\"").find(block) ?: continue
            val slug = slugMatch.groupValues[1]
            if (!seen.add(slug)) continue

            var cover = Regex("<img[^>]+src=\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
                .find(block)?.groupValues?.get(1).orEmpty()
            if (cover.isBlank()) {
                cover = Regex("srcset=\"([^\"]+)\"").find(block)?.groupValues?.get(1)?.let {
                    it.split(",").firstOrNull()?.trim()?.substringBefore(" ")
                }.orEmpty()
            }
            cover = unescapeHtml(cover)
            if (cover.isNotBlank() && !cover.startsWith("http")) cover = baseUrl + cover

            var title = Regex("<img[^>]+alt=\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
                .find(block)?.groupValues?.get(1).orEmpty()
            if (title.isBlank()) {
                title = Regex("<h[1-6][^>]*>(.*?)</h[1-6]>", RegexOption.DOT_MATCHES_ALL)
                    .find(block)?.groupValues?.get(1)?.let { cleanText(it) }.orEmpty()
            }
            title = unescapeHtml(title)
            if (title.isBlank()) title = slug.replace("-", " ").replaceFirstChar { it.uppercase() }

            val type = Regex(">(Manga|Manhwa|Manhua)<").find(block)?.groupValues?.get(1).orEmpty()

            val latest = Regex("Ch\\.\\s*(\\d+(?:\\.\\d+)?)").find(block)?.groupValues?.get(1)
                ?: Regex("Chapter\\s+(\\d+(?:\\.\\d+)?)").find(block)?.groupValues?.get(1)
                ?: ""

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

    private fun parseLdJson(html: String): List<JsonObject> {
        val out = mutableListOf<JsonObject>()
        val blocks = Regex(
            "<script type=\"application/ld\\+json\"[^>]*>(.*?)</script>",
            RegexOption.DOT_MATCHES_ALL
        )
        for (m in blocks.findAll(html)) {
            try {
                val text = unescapeHtml(m.groupValues[1])
                when (val el = json.parseToJsonElement(text)) {
                    is JsonArray -> el.forEach { if (it is JsonObject) out += it }
                    is JsonObject -> out += el
                    else -> {}
                }
            } catch (_: Exception) { }
        }
        return out
    }

    private fun findLd(list: List<JsonObject>, vararg types: String): JsonObject? {
        for (d in list) {
            val t = d["@type"] ?: continue
            when (t) {
                is JsonPrimitive -> if (t.contentOrNull in types) return d
                is JsonArray -> if (t.any { (it as? JsonPrimitive)?.contentOrNull in types }) return d
                else -> {}
            }
        }
        return null
    }

    private fun cleanText(s: String): String {
        var out = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL).replace(s, "")
        out = Regex("<[^>]+>").replace(out, "")
        out = unescapeHtml(out)
        return Regex("\\s+").replace(out, " ").trim()
    }

    private fun unescapeHtml(s: String): String = try {
        Parser.unescapeEntities(s, false)
    } catch (_: Exception) { s }

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
}