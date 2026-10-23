package store.oatec.playertv.data

import android.content.Context
import org.json.JSONArray
import store.oatec.playertv.model.MediaEntry

class PlayerPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("player_preferences", Context.MODE_PRIVATE)

    fun favorites(): Set<String> = preferences.getString(FAVORITES_KEY, null)
        ?.let(::JSONArray)
        ?.let { array -> (0 until array.length()).mapNotNull(array::optString).toSet() }
        .orEmpty()

    fun setFavorite(id: String, favorite: Boolean) {
        val updated = favorites().toMutableSet().apply {
            if (favorite) add(id) else remove(id)
        }
        val array = JSONArray().apply { updated.forEach(::put) }
        preferences.edit().putString(FAVORITES_KEY, array.toString()).apply()
    }

    fun customCategories(): List<String> = preferences.getString(CATEGORIES_KEY, null)
        ?.let(::JSONArray)
        ?.let { array -> (0 until array.length()).map { array.optString(it) }.filter(String::isNotBlank) }
        .orEmpty()

    fun addCategory(name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isBlank() || customCategories().any { it.equals(normalized, ignoreCase = true) }) return false
        val categories = customCategories() + normalized
        preferences.edit().putString(CATEGORIES_KEY, JSONArray(categories).toString()).apply()
        return true
    }

    fun categoryFor(channelId: String, sourceCategory: String): String =
        preferences.getString("category:$channelId", null) ?: sourceCategory

    fun moveToCategory(channelId: String, category: String) {
        preferences.edit().putString("category:$channelId", category).apply()
    }

    fun channelTitle(channelId: String, sourceTitle: String): String =
        preferences.getString("title:$channelId", null)?.takeIf(String::isNotBlank) ?: sourceTitle

    fun setChannelTitle(channelId: String, title: String) {
        preferences.edit().putString("title:$channelId", title.trim()).apply()
    }

    @Synchronized
    fun moveChannelToNumber(channelId: String, number: Int, knownChannelIds: List<String>): Boolean {
        if (number < 1 || channelId !in knownChannelIds) return false
        val assignments = knownChannelIds.distinct().associateWith(::channelNumber)
        val previousNumber = assignments[channelId] ?: return false
        val occupiedChannel = assignments.entries.firstOrNull { (id, assignedNumber) ->
            id != channelId && assignedNumber == number
        }?.key
        val editor = preferences.edit().putInt("channel_number:$channelId", number)
        occupiedChannel?.let { editor.putInt("channel_number:$it", previousNumber) }
        editor.apply()
        return true
    }

    fun hiddenChannels(): Set<String> = preferences.getString(HIDDEN_KEY, null)
        ?.let(::JSONArray)
        ?.let { array -> (0 until array.length()).map { array.optString(it) }.toSet() }
        .orEmpty()

    fun setHidden(channelId: String, hidden: Boolean) {
        val ids = hiddenChannels().toMutableSet().apply {
            if (hidden) add(channelId) else remove(channelId)
        }
        preferences.edit().putString(HIDDEN_KEY, JSONArray(ids).toString()).apply()
    }

    fun progress(itemId: String): Long = preferences.getLong("progress:$itemId", 0L)

    fun saveProgress(itemId: String, positionMs: Long) {
        preferences.edit().putLong("progress:$itemId", positionMs.coerceAtLeast(0L)).apply()
    }

    fun resumePlayback(): Boolean = preferences.getBoolean(RESUME_PLAYBACK_KEY, true)

    fun setResumePlayback(enabled: Boolean) {
        val editor = preferences.edit().putBoolean(RESUME_PLAYBACK_KEY, enabled)
        if (!enabled) preferences.all.keys.filter { it.startsWith("progress:") }.forEach(editor::remove)
        editor.apply()
    }

    fun clearWatchHistory() {
        val editor = preferences.edit().remove(HISTORY_KEY)
        preferences.all.keys.filter { it.startsWith("progress:") }.forEach(editor::remove)
        editor.apply()
    }

    fun recordWatched(itemId: String) {
        val history = preferences.getString(HISTORY_KEY, null)
            ?.let(::JSONArray)
            ?.let { array -> (0 until array.length()).map { array.optString(it) } }
            .orEmpty()
        val updated = (listOf(itemId) + history.filterNot { it == itemId }).take(MAX_HISTORY_ITEMS)
        preferences.edit().putString(HISTORY_KEY, JSONArray(updated).toString()).apply()
    }

    fun recentlyWatched(entries: List<MediaEntry>): List<MediaEntry> {
        val byId = entries.associateBy { it.id }
        val history = preferences.getString(HISTORY_KEY, null)
            ?.let(::JSONArray)
            ?.let { array -> (0 until array.length()).map { array.optString(it) } }
            .orEmpty()
        return history.mapNotNull(byId::get).filter { progress(it.id) > MIN_RESUME_POSITION_MS }
    }

    @Synchronized
    fun channelNumber(channelId: String): Int {
        val key = "channel_number:$channelId"
        val existing = preferences.getInt(key, 0)
        if (existing > 0) return existing
        val number = preferences.getInt(NEXT_CHANNEL_NUMBER_KEY, 1)
        preferences.edit()
            .putInt(key, number)
            .putInt(NEXT_CHANNEL_NUMBER_KEY, number + 1)
            .apply()
        return number
    }

    private companion object {
        const val FAVORITES_KEY = "favorite_ids"
        const val NEXT_CHANNEL_NUMBER_KEY = "next_channel_number"
        const val CATEGORIES_KEY = "custom_categories"
        const val HIDDEN_KEY = "hidden_channels"
        const val HISTORY_KEY = "watch_history"
        const val RESUME_PLAYBACK_KEY = "resume_playback"
        const val MAX_HISTORY_ITEMS = 30
        const val MIN_RESUME_POSITION_MS = 60_000L
    }
}