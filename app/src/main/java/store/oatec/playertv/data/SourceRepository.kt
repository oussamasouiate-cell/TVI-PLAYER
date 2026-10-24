package store.oatec.playertv.data

import android.net.Uri
import android.util.JsonReader
import android.util.JsonToken
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import store.oatec.playertv.model.MediaEntry
import store.oatec.playertv.model.MediaKind
import store.oatec.playertv.model.CatalogSection
import store.oatec.playertv.model.PlayerSource
import store.oatec.playertv.model.SourceCatalog
import store.oatec.playertv.model.SourceType

class SourceRepository {
    suspend fun loadCatalog(source: PlayerSource, section: CatalogSection): SourceCatalog {
        return when (source.type) {
            SourceType.M3U -> {
                if (section != CatalogSection.LIVE) return SourceCatalog()
                val playlist = readText(source.serverUrl)
                SourceCatalog(liveChannels = parseChannels(playlist, source.id))
            }
            SourceType.LOCAL_M3U -> {
                if (section != CatalogSection.LIVE) return SourceCatalog()
                val file = File(source.serverUrl)
                require(file.isFile) { "The imported playlist file is missing." }
                require(file.length() <= MAX_SOURCE_BYTES) { "The playlist exceeds the 512 MB import limit." }
                SourceCatalog(liveChannels = parseChannels(file.readText(Charsets.UTF_8), source.id))
            }
            SourceType.XTREAM -> loadXtreamCatalog(source, section)
        }
    }

    /** Parse an Xtream catalogue into bounded batches; retain only the first page for initial rendering. */
    suspend fun loadCatalogBatched(
        source: PlayerSource,
        section: CatalogSection,
        onBatch: (List<MediaEntry>) -> Unit,
    ): SourceCatalog {
        if ((source.type == SourceType.M3U || source.type == SourceType.LOCAL_M3U) && section != CatalogSection.LIVE) {
            return SourceCatalog()
        }
        if (source.type == SourceType.M3U || source.type == SourceType.LOCAL_M3U) {
            val firstPage = ArrayList<MediaEntry>(DISPLAY_PAGE_SIZE)
            when (source.type) {
                SourceType.M3U -> withResponse(source.serverUrl, MAX_STREAM_RESPONSE_BYTES) { connection ->
                    InputStreamReader(
                        LimitedInputStream(connection.inputStream, MAX_STREAM_RESPONSE_BYTES),
                        Charsets.UTF_8,
                    ).use { reader ->
                        M3uParser.parseBatched(reader, source.id, DISPLAY_PAGE_SIZE, MAX_CHANNELS) { batch ->
                            if (firstPage.size < DISPLAY_PAGE_SIZE) {
                                firstPage.addAll(batch.take(DISPLAY_PAGE_SIZE - firstPage.size))
                            }
                            onBatch(batch)
                        }
                    }
                }
                SourceType.LOCAL_M3U -> {
                    val file = File(source.serverUrl)
                    require(file.isFile) { "The imported playlist file is missing." }
                    require(file.length() <= MAX_SOURCE_BYTES) { "The playlist exceeds the 512 MB import limit." }
                    file.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
                        M3uParser.parseBatched(reader, source.id, DISPLAY_PAGE_SIZE, MAX_CHANNELS) { batch ->
                            if (firstPage.size < DISPLAY_PAGE_SIZE) {
                                firstPage.addAll(batch.take(DISPLAY_PAGE_SIZE - firstPage.size))
                            }
                            onBatch(batch)
                        }
                    }
                }
                else -> 0
            }
            return when (section) {
                CatalogSection.LIVE -> SourceCatalog(liveChannels = firstPage)
                CatalogSection.MOVIES -> SourceCatalog(movies = firstPage)
                CatalogSection.SERIES -> SourceCatalog(series = firstPage)
            }
        }
        if (source.type != SourceType.XTREAM) {
            val result = loadCatalog(source, section)
            val entries = when (section) {
                CatalogSection.LIVE -> result.liveChannels
                CatalogSection.MOVIES -> result.movies
                CatalogSection.SERIES -> result.series
            }
            entries.chunked(DISPLAY_PAGE_SIZE).forEach(onBatch)
            return when (section) {
                CatalogSection.LIVE -> SourceCatalog(liveChannels = entries.take(DISPLAY_PAGE_SIZE))
                CatalogSection.MOVIES -> SourceCatalog(movies = entries.take(DISPLAY_PAGE_SIZE))
                CatalogSection.SERIES -> SourceCatalog(series = entries.take(DISPLAY_PAGE_SIZE))
            }
        }
        require(source.username.isNotBlank() && source.password.isNotBlank()) {
            "Enter the username and password for this Xtream source."
        }
        val entries = when (section) {
            CatalogSection.LIVE -> readStreamEntries(
                apiUrl(source, "get_live_streams"), source,
                loadCategoriesOrEmpty(apiUrl(source, "get_live_categories")), "live", MAX_CHANNELS, onBatch,
            )
            CatalogSection.MOVIES -> readStreamEntries(
                apiUrl(source, "get_vod_streams"), source,
                loadCategoriesOrEmpty(apiUrl(source, "get_vod_categories")), "movie", MAX_VOD_ITEMS, onBatch,
            )
            CatalogSection.SERIES -> readStreamEntries(
                apiUrl(source, "get_series"), source,
                loadCategoriesOrEmpty(apiUrl(source, "get_series_categories")), "series", MAX_VOD_ITEMS, onBatch,
            )
        }
        return when (section) {
            CatalogSection.LIVE -> SourceCatalog(liveChannels = entries)
            CatalogSection.MOVIES -> SourceCatalog(movies = entries)
            CatalogSection.SERIES -> SourceCatalog(series = entries)
        }
    }

    suspend fun loadSeriesEpisodes(source: PlayerSource, seriesId: String): List<MediaEntry> {
        require(source.type == SourceType.XTREAM) { "Series details require an Xtream source." }
        val response = readText(apiUrl(source, "get_series_info", "series_id" to seriesId), MAX_SERIES_RESPONSE_BYTES)
        val root = JSONObject(response)
        val info = root.optJSONObject("info")
        val episodes = root.optJSONObject("episodes") ?: return emptyList()
        val results = mutableListOf<MediaEntry>()
        val seasonKeys = episodes.keys()
        while (seasonKeys.hasNext()) {
            val season = seasonKeys.next()
            val seasonEpisodes = episodes.optJSONArray(season) ?: continue
            for (index in 0 until seasonEpisodes.length()) {
                val episode = seasonEpisodes.optJSONObject(index) ?: continue
                val episodeId = episode.optString("id").takeIf(String::isNotBlank) ?: continue
                val extension = safeExtension(episode.optString("container_extension"), "mp4")
                val episodeNumber = episode.optString("episode_num")
                results += MediaEntry(
                    id = "${source.id}:episode:$episodeId",
                    title = episode.optString("title").ifBlank { "Episode $episodeNumber" },
                    category = "Season $season",
                    streamUrl = streamUrl(source, "series", episodeId, extension),
                    logoUrl = episode.optJSONObject("info")?.optString("movie_image").orEmpty(),
                    kind = MediaKind.EPISODE,
                    description = info?.optString("plot").orEmpty(),
                    season = season,
                    episodeNumber = episodeNumber,
                )
            }
        }
        return results
    }

    private suspend fun loadXtreamCatalog(source: PlayerSource, section: CatalogSection): SourceCatalog {
        require(source.username.isNotBlank() && source.password.isNotBlank()) {
            "Enter the username and password for this Xtream source."
        }
        return when (section) {
            CatalogSection.LIVE -> {
                val categories = loadCategoriesOrEmpty(apiUrl(source, "get_live_categories"))
                val channels = readStreamEntries(apiUrl(source, "get_live_streams"), source, categories, "live", MAX_CHANNELS)
                SourceCatalog(liveChannels = channels)
            }
            CatalogSection.MOVIES -> {
                val categories = loadCategoriesOrEmpty(apiUrl(source, "get_vod_categories"))
                val movies = readStreamEntries(apiUrl(source, "get_vod_streams"), source, categories, "movie", MAX_VOD_ITEMS)
                SourceCatalog(movies = movies)
            }
            CatalogSection.SERIES -> {
                val categories = loadCategoriesOrEmpty(apiUrl(source, "get_series_categories"))
                val series = readStreamEntries(apiUrl(source, "get_series"), source, categories, "series", MAX_VOD_ITEMS)
                SourceCatalog(series = series)
            }
        }
    }

    private fun parseChannels(playlist: String, sourceId: String): List<MediaEntry> {
        val channels = M3uParser.parse(playlist, sourceId, MAX_CHANNELS + 1)
        if (channels.size > MAX_CHANNELS) {
            throw IOException("The playlist contains more than $MAX_CHANNELS channels. Use a smaller playlist.")
        }
        return channels
    }

    private suspend fun loadCategoriesOrEmpty(url: String): Map<String, String> =
        runCatching { categoryNames(readArray(url)) }.getOrDefault(emptyMap())

    private suspend fun readArray(url: String): JSONArray {
        val response = readText(url, MAX_CATEGORY_RESPONSE_BYTES)
        return when (val value = JSONTokener(response).nextValue()) {
            is JSONArray -> value
            is JSONObject -> throw IOException("The provider returned an unexpected response.")
            else -> throw IOException("The provider returned an invalid response.")
        }
    }

    private fun categoryNames(categories: JSONArray): Map<String, String> = buildMap {
        for (index in 0 until categories.length()) {
            val category = categories.optJSONObject(index) ?: continue
            val id = category.optString("category_id")
            val name = category.optString("category_name")
            if (id.isNotBlank() && name.isNotBlank()) put(id, name)
        }
    }

    private fun readStreamEntries(
        url: String,
        source: PlayerSource,
        categories: Map<String, String>,
        type: String,
        maxItems: Int,
        onBatch: ((List<MediaEntry>) -> Unit)? = null,
    ): List<MediaEntry> = withResponse(url, MAX_STREAM_RESPONSE_BYTES) { connection ->
        val kind = when (type) {
            "movie" -> MediaKind.MOVIE
            "series" -> MediaKind.SERIES
            else -> MediaKind.LIVE
        }
        JsonReader(InputStreamReader(LimitedInputStream(connection.inputStream, MAX_STREAM_RESPONSE_BYTES), Charsets.UTF_8)).use { reader ->
            if (reader.peek() != JsonToken.BEGIN_ARRAY) throw IOException("The provider returned an unexpected response.")
            reader.beginArray()
            val results = ArrayList<MediaEntry>(minOf(maxItems, INITIAL_STREAM_CAPACITY))
            val batch = ArrayList<MediaEntry>(DISPLAY_PAGE_SIZE)
            var totalItems = 0
            while (reader.hasNext()) {
                // Keep the import usable for very large provider catalogues.
                // Never silently report a partial catalogue as complete. If a provider
                // exceeds the safety limit, fail the atomic import and keep the last good
                // saved catalogue so the user can see and act on the error.
                if (totalItems >= maxItems) {
                    throw IOException("This catalogue exceeds the safe limit of $maxItems items. The previous saved catalogue was preserved.")
                }
                if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                    reader.skipValue()
                    continue
                }
                var id = ""
                var title = ""
                var categoryId = ""
                var categoryName = ""
                var extension = ""
                var logo = ""
                var description = ""
                var genre = ""
                var rating = ""
                var year = ""
                var releaseDate = ""
                var director = ""
                var cast = ""
                var epgChannelId = ""
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "stream_id", "series_id" -> id = readJsonValue(reader, MAX_FIELD_LENGTH)
                        "id" -> if (id.isBlank()) id = readJsonValue(reader, MAX_FIELD_LENGTH) else reader.skipValue()
                        "name" -> title = readJsonValue(reader, MAX_TITLE_LENGTH)
                        "category_id" -> categoryId = readJsonValue(reader, MAX_FIELD_LENGTH)
                        "category_name", "group-title", "group_title", "bouquet" ->
                            categoryName = readJsonValue(reader, MAX_FIELD_LENGTH)
                        "container_extension" -> extension = readJsonValue(reader, 16)
                        "stream_icon", "cover" -> {
                            val value = readJsonValue(reader, MAX_LOGO_LENGTH)
                            if (logo.isBlank()) logo = value
                        }
                        "plot" -> if (type == "live") reader.skipValue() else description = readJsonValue(reader, MAX_DESCRIPTION_LENGTH)
                        "genre" -> if (type == "live") reader.skipValue() else genre = readJsonValue(reader, MAX_FIELD_LENGTH)
                        "rating", "rating_5based" -> if (type == "live") reader.skipValue() else rating = readJsonValue(reader, 32)
                        "year" -> if (type == "live") reader.skipValue() else year = readJsonValue(reader, 8)
                        "release_date", "releaseDate" -> if (type == "live") reader.skipValue() else releaseDate = readJsonValue(reader, 32)
                        "director" -> if (type == "live") reader.skipValue() else director = readJsonValue(reader, MAX_FIELD_LENGTH)
                        "cast" -> if (type == "live") reader.skipValue() else cast = readJsonValue(reader, MAX_DESCRIPTION_LENGTH)
                        "epg_channel_id", "epg_channel_id_name", "tvg-id", "tvg_id" ->
                            epgChannelId = readJsonValue(reader, MAX_FIELD_LENGTH)
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                if (id.isBlank()) continue
                val safeExtension = safeExtension(extension, if (type == "live") "ts" else "mp4")
                val entry = MediaEntry(
                    id = "${source.id}:$type:$id",
                    title = title.ifBlank { "Untitled" },
                    category = categories[categoryId].orEmpty().ifBlank {
                        categoryName.ifBlank { categoryId.ifBlank { "Other" } }
                    },
                    streamUrl = if (kind == MediaKind.SERIES) "" else streamUrl(source, type, id, safeExtension),
                    logoUrl = logo,
                    kind = kind,
                    description = description,
                    genre = genre,
                    rating = rating,
                    year = year,
                    releaseDate = releaseDate,
                    director = director,
                    cast = cast,
                    epgChannelId = epgChannelId,
                )
                totalItems++
                if (onBatch == null) {
                    results += entry
                } else {
                    if (results.size < DISPLAY_PAGE_SIZE) results += entry
                    batch += entry
                    if (batch.size >= DISPLAY_PAGE_SIZE) {
                        onBatch(batch.toList())
                        batch.clear()
                    }
                }
            }
            reader.endArray()
            if (batch.isNotEmpty()) onBatch?.invoke(batch.toList())
            results
        }
    }

    private fun readJsonValue(reader: JsonReader, maxLength: Int): String {
        val value = when (reader.peek()) {
            JsonToken.STRING, JsonToken.NUMBER -> reader.nextString()
            JsonToken.BOOLEAN -> reader.nextBoolean().toString()
            JsonToken.NULL -> { reader.nextNull(); "" }
            else -> { reader.skipValue(); "" }
        }
        return value.take(maxLength)
    }

    private fun apiUrl(source: PlayerSource, action: String? = null, vararg parameters: Pair<String, String>): String {
        val base = normalizedBase(source.serverUrl)
        return Uri.parse(base).buildUpon()
            .appendPath("player_api.php")
            .appendQueryParameter("username", source.username)
            .appendQueryParameter("password", source.password)
            .apply {
                action?.let { appendQueryParameter("action", it) }
                parameters.forEach { (key, value) -> appendQueryParameter(key, value) }
            }
            .build()
            .toString()
    }

    private fun streamUrl(source: PlayerSource, type: String, id: String, extension: String): String =
        Uri.parse(normalizedBase(source.serverUrl)).buildUpon()
            .appendPath(type)
            .appendPath(source.username)
            .appendPath(source.password)
            .appendPath("$id.$extension")
            .build()
            .toString()

    private fun normalizedBase(value: String): String {
        val uri = Uri.parse(value.trim())
        require(uri.scheme == "http" || uri.scheme == "https") { "Use an HTTP or HTTPS server URL." }
        require(uri.host != null) { "Enter a valid server URL." }
        return value.trim().trimEnd('/').removeSuffix("/player_api.php")
    }

    private fun readText(initialUrl: String, maxBytes: Int = MAX_SOURCE_BYTES): String =
        withResponse(initialUrl, maxBytes) { connection ->
            val output = ByteArrayOutputStream()
            LimitedInputStream(connection.inputStream, maxBytes).use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
            }
            output.toString(Charsets.UTF_8.name())
        }

    private fun <T> withResponse(
        initialUrl: String,
        maxBytes: Int = MAX_SOURCE_BYTES,
        read: (HttpURLConnection) -> T,
    ): T {
        var current = URL(initialUrl)
        for (redirectCount in 0..MAX_REDIRECTS) {
            val uri = Uri.parse(current.toString())
            require(uri.scheme == "http" || uri.scheme == "https") { "Only HTTP and HTTPS sources are supported." }
            require(uri.host != null) { "Enter a valid source URL." }
            val connection = current.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "OATEC-PlayerTv/1.0 Android")
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    if (redirectCount == MAX_REDIRECTS) throw IOException("The source redirected too many times.")
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("The source returned an invalid redirect.")
                    current = URL(current, location)
                    continue
                }
                if (status !in 200..299) throw IOException("Source request failed (HTTP $status).")
                return read(connection)
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Unable to load the source.")
    }

    private fun safeExtension(value: String, fallback: String): String =
        value.takeIf { it.matches(Regex("[A-Za-z0-9]{1,8}")) } ?: fallback

    private companion object {
        const val CONNECT_TIMEOUT_MS = 20_000
        // Large IPTV catalogues can take considerably longer than ordinary API calls.
        const val READ_TIMEOUT_MS = 120_000
        const val MAX_SOURCE_BYTES = 512 * 1024 * 1024
        const val MAX_STREAM_RESPONSE_BYTES = 1024 * 1024 * 1024
        const val MAX_CATEGORY_RESPONSE_BYTES = 8 * 1024 * 1024
        const val MAX_SERIES_RESPONSE_BYTES = 4 * 1024 * 1024
        const val MAX_CHANNELS = 1_000_000
        const val MAX_VOD_ITEMS = 1_000_000
        const val MAX_REDIRECTS = 5
        const val INITIAL_STREAM_CAPACITY = 128
        const val DISPLAY_PAGE_SIZE = 500
        const val MAX_FIELD_LENGTH = 512
        const val MAX_TITLE_LENGTH = 300
        const val MAX_LOGO_LENGTH = 2_048
        const val MAX_DESCRIPTION_LENGTH = 1_000
    }
}

private class LimitedInputStream(input: InputStream, private val maxBytes: Int) : FilterInputStream(input) {
    private var bytesRead = 0

    override fun read(): Int {
        val value = super.read()
        if (value >= 0 && ++bytesRead > maxBytes) throw IOException("The response exceeds its size limit.")
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val value = super.read(buffer, offset, minOf(length, maxBytes - bytesRead + 1))
        if (value > 0) {
            bytesRead += value
            if (bytesRead > maxBytes) throw IOException("The response exceeds its size limit.")
        }
        return value
    }
}