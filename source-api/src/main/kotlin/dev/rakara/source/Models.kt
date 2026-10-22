package dev.rakara.source


data class SManga(
    
    val url: String,
    val title: String = "",
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val genre: List<String> = emptyList(),
    val status: Status = Status.UNKNOWN,
    val thumbnailUrl: String? = null,
    
    val initialized: Boolean = false,
) {
    enum class Status {
        UNKNOWN,
        ONGOING,
        COMPLETED,
        LICENSED,
        PUBLISHING_FINISHED,
        CANCELLED,
        ON_HIATUS,
    }
}


data class SChapter(
    
    val url: String,
    val name: String = "",
    
    val chapterNumber: Float = -1f,
    val scanlator: String? = null,
    
    val dateUpload: Long = 0L,
    val read: Boolean = false,
    val bookmark: Boolean = false,
)


data class SPage(
    val index: Int,
    val url: String,               
    val imageUrl: String? = null,  
)


data class MangasPage(
    val mangas: List<SManga>,
    val hasNextPage: Boolean = false,
)


data class FilterList(
    val filters: List<Filter> = emptyList(),
) {
    fun text(name: String): String =
        filters.filterIsInstance<Filter.Text>().firstOrNull { it.name == name }?.state.orEmpty()

    fun select(name: String): Int =
        filters.filterIsInstance<Filter.Select>().firstOrNull { it.name == name }?.selected ?: 0

    fun triState(name: String): Int =
        filters.filterIsInstance<Filter.TriState>().firstOrNull { it.name == name }?.state ?: 0
}

sealed class Filter {
    data class Text(val name: String, val state: String = "") : Filter()

    data class Select(
        val name: String,
        val values: List<String>,
        val selected: Int = 0,
    ) : Filter()

    
    data class TriState(val name: String, val state: Int = 0) : Filter()
}