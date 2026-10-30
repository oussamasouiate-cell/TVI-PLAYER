package store.oatec.playertv.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Small client for KinoCheck's official trailer catalogue.
 *
 * Call from Dispatchers.IO. The API is public without a key (documented limit:
 * 1,000 requests/day); pass a privately configured key only when one is available.
 * This client only retrieves trailer metadata and YouTube IDs; it does not provide
 * full movie or episode streams.
 */
class KinoCheckClient(
    private val apiKey: String = "",
) {
    fun latestTrailers(language: String = "en", page: Int = 1, limit: Int = 25): List<KinoCheckVideo> =
        getVideos("/trailers/latest", mapOf(
            "language" to language.take(2),
            "page" to page.coerceAtLeast(1).toString(),
            "limit" to limit.coerceIn(1, 100).toString(),
        ))

    fun trendingTrailers(language: String = "en", page: Int = 1, limit: Int = 25): List<KinoCheckVideo> =
        getVideos("/trailers/trending", mapOf(
            "language" to language.take(2),
            "page" to page.coerceAtLeast(1).toString(),
            "limit" to limit.coerceIn(1, 100).toString(),
        ))

    fun movieByTmdbId(tmdbId: Long, language: String = "en"): KinoCheckTitle? {
        if (tmdbId <= 0) return null
        return getTitle("/movies", mapOf("tmdb_id" to tmdbId.toString(), "language" to language.take(2)))
    }

    fun showByTmdbId(tmdbId: Long, language: String = "en"): KinoCheckTitle? {
        if (tmdbId <= 0) return null
        return getTitle("/shows", mapOf("tmdb_id" to tmdbId.toString(), "language" to language.take(2)))
    }

    fun movieByImdbId(imdbId: String, language: String = "en"): KinoCheckTitle? =
        imdbId.trim().takeIf { it.matches(Regex("tt[0-9]+")) }?.let {
            getTitle("/movies", mapOf("imdb_id" to it, "language" to language.take(2)))
        }

    fun showByImdbId(imdbId: String, language: String = "en"): KinoCheckTitle? =
        imdbId.trim().takeIf { it.matches(Regex("tt[0-9]+")) }?.let {
            getTitle("/shows", mapOf("imdb_id" to it, "language" to language.take(2)))
        }

    private fun getTitle(path: String, params: Map<String, String>): KinoCheckTitle? {
        val json = request(path, params) as? JSONObject ?: return null
        return KinoCheckTitle(
            id = json.optString("id"),
            title = json.optString("title"),
            pageUrl = json.optString("url"),
            trailer = json.optJSONObject("trailer")?.toVideo(),
            videos = json.optJSONArray("videos").toVideos(),
        )
    }

    private fun getVideos(path: String, params: Map<String, String>): List<KinoCheckVideo> {
        val response = request(path, params)
        val array = when (response) {
            is JSONArray -> response
            is JSONObject -> response.optJSONArray("videos")
                ?: response.optJSONArray("results")
                ?: response.optJSONArray("items")
                ?: response.optJSONArray("data")
            else -> null
        }
        return array.toVideos()
    }

    private fun request(path: String, params: Map<String, String>): Any? {
        val query = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val connection = (URL("https://api.kinocheck.com$path?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            if (apiKey.isNotBlank()) {
                setRequestProperty("X-Api-Key", apiKey.trim())
                setRequestProperty("X-Api-Host", "api.kinocheck.com")
            }
        }
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IOException("KinoCheck request failed (HTTP $status).")
            }
            when (body.trim().firstOrNull()) {
                '{' -> JSONObject(body)
                '[' -> JSONArray(body)
                else -> null
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun JSONObject.toVideo(): KinoCheckVideo = KinoCheckVideo(
        id = optString("id"),
        title = optString("title"),
        youtubeVideoId = optString("youtube_video_id"),
        thumbnailUrl = optString("youtube_thumbnail").ifBlank { optString("thumbnail") },
        language = optString("language"),
        categories = optJSONArray("categories").toStringList(),
        publishedAt = optString("published"),
        pageUrl = optString("url"),
        tmdbId = optJSONObject("resource")?.let { resource ->
            if (resource.isNull("tmdb_id")) null else resource.optLong("tmdb_id").takeIf { it > 0 }
        },
        imdbId = optJSONObject("resource")?.optString("imdb_id")?.takeIf { it.isNotBlank() },
    )

    private fun JSONArray?.toVideos(): List<KinoCheckVideo> {
        val array = this ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.toVideo()?.takeIf { it.youtubeVideoId.isNotBlank() }
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        val array = this ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optString(index).takeIf { it.isNotBlank() }
        }
    }
}

data class KinoCheckVideo(
    val id: String,
    val title: String,
    val youtubeVideoId: String,
    val thumbnailUrl: String,
    val language: String,
    val categories: List<String>,
    val publishedAt: String,
    val pageUrl: String,
    val tmdbId: Long?,
    val imdbId: String?,
)

data class KinoCheckTitle(
    val id: String,
    val title: String,
    val pageUrl: String,
    val trailer: KinoCheckVideo?,
    val videos: List<KinoCheckVideo>,
)
