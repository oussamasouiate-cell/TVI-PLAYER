package store.oatec.playertv.model

enum class SourceType {
    M3U,
    LOCAL_M3U,
    XTREAM,
}

enum class MediaKind {
    LIVE,
    MOVIE,
    SERIES,
    EPISODE,
}

enum class CatalogSection {
    LIVE,
    MOVIES,
    SERIES,
}

data class PlayerSource(
    val id: String,
    val name: String,
    val type: SourceType,
    val serverUrl: String,
    val username: String = "",
    val password: String = "",
    val epgUrl: String = "",
)

data class MediaEntry(
    val id: String,
    val title: String,
    val category: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val kind: MediaKind = MediaKind.LIVE,
    val description: String = "",
    val season: String = "",
    val episodeNumber: String = "",
    val genre: String = "",
    val rating: String = "",
    val year: String = "",
    val releaseDate: String = "",
    val director: String = "",
    val cast: String = "",
)

data class SourceCatalog(
    val liveChannels: List<MediaEntry> = emptyList(),
    val movies: List<MediaEntry> = emptyList(),
    val series: List<MediaEntry> = emptyList(),
)