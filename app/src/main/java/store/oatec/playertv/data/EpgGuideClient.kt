package store.oatec.playertv.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads XMLTV guides with explicit time and size limits before handing them to the parser.
 * Call from a background coroutine/thread, never the Android main thread.
 */
class EpgGuideClient(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 15_000,
    private val maxBytes: Int = 12 * 1024 * 1024,
) {
    init {
        require(connectTimeoutMs > 0 && readTimeoutMs > 0 && maxBytes > 0)
    }

    fun fetch(urlValue: String): List<EpgProgram> {
        val url = URL(urlValue.trim())
        require(url.protocol.equals("https", ignoreCase = true) || url.protocol.equals("http", ignoreCase = true)) {
            "The EPG URL must use HTTP or HTTPS."
        }
        val connection = (url.openConnection() as? HttpURLConnection)
            ?: throw IOException("The EPG URL is not an HTTP connection.")
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/xml, text/xml, */*")
        connection.setRequestProperty("User-Agent", "TVI-PLAYER/1.0 EPG")
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("EPG server returned HTTP $status.")
            // contentLength is available on the app's minSdk (23); -1 means unknown.
            val contentLength = connection.contentLength
            val declaredLength = if (contentLength >= 0) contentLength.toLong() else -1L
            if (declaredLength > maxBytes) throw IOException("The EPG guide exceeds the size limit.")
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) throw IOException("The EPG guide exceeds the size limit.")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val xml = bytes.toString(Charsets.UTF_8)
            return EpgRepository.parseXmltv(xml)
        } finally {
            connection.disconnect()
        }
    }
}
