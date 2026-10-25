package store.oatec.playertv.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Client for Xtream Codes-compatible VOD APIs.
 *
 * Accepts a playlist URL shaped like:
 * http(s)://host/get.php?username=...&password=...&type=m3u_plus&output=ts
 *
 * Credentials are kept in memory only by this class and are never logged.
 */
data class XtreamVodItem(
    val streamId: String,
    val title: String,
    val categoryId: String?,
    val extension: String,
    val posterUrl: String?,
    val directUrl: String,
)

data class XtreamCredentials(
    val baseUrl: String,
    val username: String,
    val password: String,
)

class XtreamVodClient(
    private val connectTimeoutMs: Int = 12_000,
    private val readTimeoutMs: Int = 20_000,
) {
    fun credentialsFromPlaylist(playlistUrl: String): XtreamCredentials {
        val uri = Uri.parse(playlistUrl.trim())
        require(uri.scheme == "http" || uri.scheme == "https") { "رابط القائمة خاصو يكون HTTP أو HTTPS." }
        require(!uri.host.isNullOrBlank()) { "رابط القائمة غير صالح." }
        val username = uri.getQueryParameter("username")?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("ما لقيتش username فالرابط.")
        val password = uri.getQueryParameter("password")?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("ما لقيتش password فالرابط.")
        val port = if (uri.port == -1) "" else ":\u0024{uri.port}"
        val baseUrl = "\u0024{uri.scheme}://\u0024{uri.host}\u0024port\u0024{uri.encodedPath.orEmpty().substringBeforeLast('/', "").takeIf { it.isNotEmpty() }?.let { if (it.startsWith("/")) it else "/\u0024it" }.orEmpty()}"
        return XtreamCredentials(baseUrl.trimEnd('/'), username, password)
    }

    suspend fun fetchVod(credentials: XtreamCredentials): List<XtreamVodItem> = withContext(Dispatchers.IO) {
        val query = "username=\u0024{enc(credentials.username)}&password=\u0024{enc(credentials.password)}&action=get_vod_streams"
        val json = request("\u0024{credentials.baseUrl.trimEnd('/')}/player_api.php?\u0024query")
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("stream_id").takeIf { it.isNotBlank() && it != "null" } ?: continue
                val ext = item.optString("container_extension", "mp4")
                    .lowercase().takeIf { it.matches(Regex("[a-z0-9]{2,5}")) } ?: "mp4"
                add(
                    XtreamVodItem(
                        streamId = id,
                        title = item.optString("name", "Movie"),
                        categoryId = item.optString("category_id").takeIf { it.isNotBlank() && it != "null" },
                        extension = ext,
                        posterUrl = item.optString("stream_icon").takeIf { it.startsWith("http://") || it.startsWith("https://") },
                        directUrl = movieUrl(credentials, id, ext),
                    )
                )
            }
        }
    }

    fun movieUrl(credentials: XtreamCredentials, streamId: String, extension: String): String {
        require(streamId.matches(Regex("[A-Za-z0-9_-]+"))) { "معرّف الفيلم غير صالح." }
        val safeExtension = extension.lowercase().takeIf { it.matches(Regex("[a-z0-9]{2,5}")) } ?: "mp4"
        return "\u0024{credentials.baseUrl.trimEnd('/')}/movie/\u0024{encPath(credentials.username)}/\u0024{encPath(credentials.password)}/\u0024streamId.\u0024safeExtension"
    }

    suspend fun fetchVodCategories(credentials: XtreamCredentials): JSONArray = withContext(Dispatchers.IO) {
        val query = "username=\u0024{enc(credentials.username)}&password=\u0024{enc(credentials.password)}&action=get_vod_categories"
        JSONArray(request("\u0024{credentials.baseUrl.trimEnd('/')}/player_api.php?\u0024query"))
    }

    private fun request(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.setRequestProperty("Accept", "application/json")
        connection.instanceFollowRedirects = true
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("Xtream API رجعات HTTP \u0024status.")
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun encPath(value: String): String = Uri.encode(value)
}
