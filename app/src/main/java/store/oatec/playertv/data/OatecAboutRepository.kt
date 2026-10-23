package store.oatec.playertv.data

import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class OatecAbout(
    val title: String,
    val description: String,
    val website: String = "https://oatec.store/",
)

class OatecAboutRepository {
    fun fetch(): OatecAbout {
        val html = readOfficialHomepage()
        val title = TITLE_PATTERN.find(html)?.groups?.get(1)?.value?.stripTags()?.trim().orEmpty()
        val description = META_PATTERN.findAll(html)
            .mapNotNull { match ->
                val tag = match.value
                if (DESCRIPTION_ATTRIBUTE.containsMatchIn(tag)) CONTENT_ATTRIBUTE.find(tag)?.groupValues?.get(1) else null
            }
            .firstOrNull()
            ?.stripTags()
            ?.trim()
            .orEmpty()
        if (title.isBlank() || description.isBlank()) {
            throw IOException("OATEC site information is temporarily unavailable.")
        }
        return OatecAbout(title = title, description = description)
    }

    private fun readOfficialHomepage(): String {
        var url = URL(OFFICIAL_URL)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val uri = Uri.parse(url.toString())
            if (uri.scheme != "https" || uri.host !in OFFICIAL_HOSTS) {
                throw IOException("The OATEC site redirected to an untrusted address.")
            }
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "OATEC-PlayerTv/1.0 Android")
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    if (redirectCount == MAX_REDIRECTS) throw IOException("The OATEC site redirected too many times.")
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("The OATEC site returned an invalid redirect.")
                    url = URL(url, location)
                    return@repeat
                }
                if (status !in 200..299) throw IOException("OATEC site request failed (HTTP $status).")
                val output = ByteArrayOutputStream()
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_RESPONSE_BYTES) throw IOException("OATEC site response is too large.")
                        output.write(buffer, 0, count)
                    }
                }
                return output.toString(Charsets.UTF_8.name())
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Unable to load OATEC site information.")
    }

    private fun String.stripTags(): String = replace(Regex("<[^>]*>"), "")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    private companion object {
        const val OFFICIAL_URL = "https://oatec.store/"
        val OFFICIAL_HOSTS = setOf("oatec.store", "www.oatec.store")
        const val MAX_REDIRECTS = 3
        const val MAX_RESPONSE_BYTES = 1024 * 1024
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        val TITLE_PATTERN = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val META_PATTERN = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
        val DESCRIPTION_ATTRIBUTE = Regex("name\\s*=\\s*[\"']description[\"']", RegexOption.IGNORE_CASE)
        val CONTENT_ATTRIBUTE = Regex("content\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
    }
}