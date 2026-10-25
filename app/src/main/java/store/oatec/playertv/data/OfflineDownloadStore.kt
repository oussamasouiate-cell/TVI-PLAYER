package store.oatec.playertv.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import store.oatec.playertv.model.MediaEntry
import store.oatec.playertv.model.MediaKind
import java.io.File

data class OfflineItem(
    val downloadId: Long,
    val title: String,
    val filePath: String,
    val logoUrl: String,
    val category: String,
    val url: String,
    val status: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
) {
    val isComplete: Boolean get() = status == DownloadManager.STATUS_SUCCESSFUL && File(filePath).exists()
    val progress: Int get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
    fun asMediaEntry() = MediaEntry(
        id = "offline:$downloadId",
        title = title,
        category = category,
        streamUrl = Uri.fromFile(File(filePath)).toString(),
        logoUrl = logoUrl,
        kind = MediaKind.MOVIE,
    )
}

/** App-private offline media catalogue backed by Android DownloadManager for background transfers. */
class OfflineDownloadStore(private val context: Context) {
    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("offline_downloads", Context.MODE_PRIVATE)

    @Synchronized
    fun enqueue(entry: MediaEntry): Long {
        val uri = Uri.parse(entry.streamUrl.trim())
        require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) {
            "هاد المصدر ما عندوش رابط تحميل مباشر صالح."
        }
        require(!uri.path.orEmpty().endsWith(".m3u8", true) && !uri.path.orEmpty().endsWith(".mpd", true)) {
            "هاد الفيديو بث HLS/DASH؛ التحميل Offline ديالو مازال غير مدعوم."
        }
        require(entry.kind != MediaKind.LIVE && entry.kind != MediaKind.SERIES) {
            "اختار فيلم أو حلقة، ماشي قناة مباشرة أو صفحة مسلسل."
        }
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir.resolve("offline")
        if (!dir.exists()) dir.mkdirs()
        val extension = uri.path.orEmpty().substringAfterLast('.', "mp4")
            .takeIf { it.matches(Regex("[A-Za-z0-9]{2,5}")) } ?: "mp4"
        val safeTitle = entry.title.replace(Regex("[^A-Za-z0-9._-]"), "_").take(55).ifBlank { "video" }
        val fileName = "${safeTitle}_${System.currentTimeMillis()}.$extension"
        val request = DownloadManager.Request(uri)
            .setTitle(entry.title)
            .setDescription("TVI-PLAYER • Offline download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
        val id = manager.enqueue(request)
        val item = JSONObject()
            .put("id", id)
            .put("title", entry.title)
            .put("file", fileName)
            .put("logo", entry.logoUrl)
            .put("category", entry.category)
            .put("url", entry.streamUrl)
        val items = readMetadata()
        items.put(item)
        prefs.edit().putString("items", items.toString()).apply()
        return id
    }

    @Synchronized
    fun list(): List<OfflineItem> {
        val metadata = readMetadata()
        val results = mutableListOf<OfflineItem>()
        val keep = JSONArray()
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir.resolve("offline")
        for (i in 0 until metadata.length()) {
            val item = metadata.optJSONObject(i) ?: continue
            val id = item.optLong("id", -1L)
            if (id < 0) continue
            val file = File(directory, item.optString("file"))
            val query = DownloadManager.Query().setFilterById(id)
            val cursor = runCatching { manager.query(query) }.getOrNull()
            if (cursor == null) continue
            cursor.use {
                if (!it.moveToFirst()) {
                    file.delete()
                    return@use
                }
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                results += OfflineItem(
                    downloadId = id,
                    title = item.optString("title", "Video"),
                    filePath = file.absolutePath,
                    logoUrl = item.optString("logo"),
                    category = item.optString("category"),
                    url = item.optString("url"),
                    status = status,
                    downloadedBytes = downloaded,
                    totalBytes = total,
                )
                keep.put(item)
            }
        }
        prefs.edit().putString("items", keep.toString()).apply()
        return results.sortedByDescending { it.downloadId }
    }

    @Synchronized
    fun delete(downloadId: Long) {
        val item = readMetadata().let { array ->
            (0 until array.length()).mapNotNull { array.optJSONObject(it) }
                .firstOrNull { it.optLong("id") == downloadId }
        }
        runCatching { manager.remove(downloadId) }
        if (item != null) {
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir.resolve("offline")
            File(directory, item.optString("file")).delete()
        }
        val remaining = JSONArray()
        readMetadata().let { array ->
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                if (obj.optLong("id") != downloadId) remaining.put(obj)
            }
        }
        prefs.edit().putString("items", remaining.toString()).apply()
    }

    private fun readMetadata(): JSONArray = runCatching {
        JSONArray(prefs.getString("items", "[]") ?: "[]")
    }.getOrDefault(JSONArray())
}
