package dev.rakara.source

interface Source {
    val id: Long
    val name: String
    val lang: String
    val versionId: Int
    val supportsLatest: Boolean get() = true

    suspend fun getPopular(page: Int): MangasPage
    suspend fun getLatest(page: Int): MangasPage
    suspend fun search(query: String, page: Int, filters: FilterList = FilterList()): MangasPage
    suspend fun getDetails(manga: SManga): SManga
    suspend fun getChapters(manga: SManga): List<SChapter>
    suspend fun getPages(chapter: SChapter): List<SPage>
    fun close() {}
}