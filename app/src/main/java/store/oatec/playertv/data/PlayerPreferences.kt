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

    fun duration(itemId: String): Long = preferences.getLong("duration:$itemId", 0L)

    fun saveProgress(itemId: String, positionMs: Long, durationMs: Long = 0L) {
        val position = positionMs.coerceAtLeast(0L)
        val duration = durationMs.coerceAtLeast(0L)
        val editor = preferences.edit()
        if (duration > 0L) editor.putLong("duration:$itemId", duration)
        if (duration > 0L && position >= duration - 30_000L) {
            editor.remove("progress:$itemId")
        } else if (position > 0L) {
            editor.putLong("progress:$itemId", position)
        } else {
            editor.remove("progress:$itemId")
        }
        editor.apply()
    }

    fun watchProgressFraction(itemId: String): Float {
        val position = progress(itemId)
        val duration = duration(itemId)
        if (position < 5_000L || duration <= 0L || position >= duration - 30_000L) return 0f
        return (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
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

    /** Kids Mode is device-local. The PIN is stored as a SHA-256 digest, never as plain text. */
    fun kidsModeEnabled(): Boolean = preferences.getBoolean(KIDS_MODE_ENABLED_KEY, false)

    fun setKidsModeEnabled(enabled: Boolean): Boolean {
        if (enabled && !hasKidsPin()) return false
        preferences.edit().putBoolean(KIDS_MODE_ENABLED_KEY, enabled).apply()
        return true
    }

    fun hasKidsPin(): Boolean = !preferences.getString(KIDS_PIN_HASH_KEY, null).isNullOrBlank()

    fun setKidsPin(pin: String): Boolean {
        val normalized = pin.trim()
        if (normalized.length !in 4..8 || normalized.any { !it.isDigit() }) return false
        preferences.edit()
            .putString(KIDS_PIN_HASH_KEY, pinDigest(normalized))
            .putBoolean(KIDS_MODE_ENABLED_KEY, true)
            .apply()
        return true
    }

    fun verifyKidsPin(pin: String): Boolean {
        val saved = preferences.getString(KIDS_PIN_HASH_KEY, null) ?: return false
        return java.security.MessageDigest.isEqual(
            saved.toByteArray(Charsets.UTF_8),
            pinDigest(pin.trim()).toByteArray(Charsets.UTF_8),
        )
    }

    fun changeKidsPin(currentPin: String, newPin: String): Boolean {
        if (!verifyKidsPin(currentPin)) return false
        val normalized = newPin.trim()
        if (normalized.length !in 4..8 || normalized.any { !it.isDigit() }) return false
        preferences.edit().putString(KIDS_PIN_HASH_KEY, pinDigest(normalized)).apply()
        return true
    }

    fun clearKidsPin(currentPin: String): Boolean {
        if (!verifyKidsPin(currentPin)) return false
        preferences.edit()
            .remove(KIDS_PIN_HASH_KEY)
            .putBoolean(KIDS_MODE_ENABLED_KEY, false)
            .apply()
        return true
    }

    /** Explicit parent decisions override automatic classification. */
    fun kidsAllowedItemIds(): Set<String> = readKidsIds(KIDS_ALLOW_ITEM_IDS_KEY)

    fun kidsBlockedItemIds(): Set<String> = readKidsIds(KIDS_BLOCK_ITEM_IDS_KEY)

    fun setKidsItemAllowed(itemId: String, allowed: Boolean) {
        val id = itemId.trim()
        if (id.isEmpty()) return
        val allow = kidsAllowedItemIds().toMutableSet()
        val block = kidsBlockedItemIds().toMutableSet()
        if (allowed) {
            allow.add(id)
            block.remove(id)
        } else {
            block.add(id)
            allow.remove(id)
        }
        preferences.edit()
            .putString(KIDS_ALLOW_ITEM_IDS_KEY, JSONArray(allow.toList()).toString())
            .putString(KIDS_BLOCK_ITEM_IDS_KEY, JSONArray(block.toList()).toString())
            .apply()
    }

    /** Remove a parent's explicit decision and return the item to automatic classification. */
    fun clearKidsItemRule(itemId: String): Boolean {
        val id = itemId.trim()
        if (id.isEmpty()) return false
        val allow = kidsAllowedItemIds().toMutableSet()
        val block = kidsBlockedItemIds().toMutableSet()
        val changed = allow.remove(id) or block.remove(id)
        if (changed) {
            preferences.edit()
                .putString(KIDS_ALLOW_ITEM_IDS_KEY, JSONArray(allow.toList()).toString())
                .putString(KIDS_BLOCK_ITEM_IDS_KEY, JSONArray(block.toList()).toString())
                .apply()
        }
        return changed
    }

    /** Unknown items are hidden while Kids Mode is active unless a parent allows them. */
    fun isAllowedInKidsMode(itemId: String, classifiedAsKidSafe: Boolean): Boolean {
        if (!kidsModeEnabled()) return true
        val id = itemId.trim()
        if (id.isEmpty() || id in kidsBlockedItemIds()) return false
        if (id in kidsAllowedItemIds()) return true
        return classifiedAsKidSafe
    }

    private fun readKidsIds(key: String): Set<String> = preferences.getString(key, null)
        ?.let(::JSONArray)
        ?.let { array -> (0 until array.length()).map { array.optString(it) }.filter(String::isNotBlank).toSet() }
        .orEmpty()

    private fun pinDigest(pin: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Selected playback engine: exoplayer (default) or mpv. */
    fun playbackEngine(): String = preferences.getString(PLAYBACK_ENGINE_KEY, "exoplayer")
        ?.lowercase()
        ?.takeIf { it in setOf("exoplayer", "mpv") } ?: "exoplayer"

    fun setPlaybackEngine(engine: String) {
        val normalized = engine.trim().lowercase()
        if (normalized in setOf("exoplayer", "mpv")) {
            preferences.edit().putString(PLAYBACK_ENGINE_KEY, normalized).apply()
        }
    }

    private companion object {
        const val FAVORITES_KEY = "favorite_ids"
        const val KIDS_MODE_ENABLED_KEY = "kids_mode_enabled"
        const val KIDS_PIN_HASH_KEY = "kids_pin_sha256"
        const val KIDS_ALLOW_ITEM_IDS_KEY = "kids_allow_item_ids"
        const val KIDS_BLOCK_ITEM_IDS_KEY = "kids_block_item_ids"
        const val NEXT_CHANNEL_NUMBER_KEY = "next_channel_number"
        const val CATEGORIES_KEY = "custom_categories"
        const val HIDDEN_KEY = "hidden_channels"
        const val HISTORY_KEY = "watch_history"
        const val RESUME_PLAYBACK_KEY = "resume_playback"
        const val PLAYBACK_ENGINE_KEY = "playback_engine"
        const val MAX_HISTORY_ITEMS = 30
        const val MIN_RESUME_POSITION_MS = 60_000L
    }
}