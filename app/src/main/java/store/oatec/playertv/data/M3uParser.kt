package store.oatec.playertv.data

import java.net.URI
import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.security.MessageDigest
import store.oatec.playertv.model.MediaEntry

object M3uParser {
    private val attributePattern = Regex("""([\w-]+)\s*=\s*("[^"]*"|'[^']*'|[^\s,]*)""")

    fun parse(playlist: String, sourceId: String, maxChannels: Int = Int.MAX_VALUE): List<MediaEntry> {
        val channels = mutableListOf<MediaEntry>()
        var metadata: Map<String, String> = emptyMap()
        var title = ""
        var group = ""

        for (rawLine in playlist.lineSequence()) {
            val line = rawLine.trim().removePrefix("\uFEFF")
            if (channels.size >= maxChannels) break
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val info = line.substringAfter(':')
                    val comma = info.indexOfUnquotedComma()
                    val attributeText = if (comma >= 0) info.substring(0, comma) else info
                    title = if (comma >= 0) info.substring(comma + 1).trim() else ""
                    metadata = attributePattern.findAll(attributeText).associate {
                        it.groupValues[1].lowercase() to it.groupValues[2].trim('"', '\'')
                    }
                    group = metadata["group-title"].orEmpty()
                }
                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    group = line.substringAfter(':').trim()
                }
                line.isNotEmpty() && !line.startsWith('#') -> {
                    val uri = runCatching { URI(line) }.getOrNull()
                    val scheme = uri?.scheme?.lowercase()
                    if (scheme == "http" || scheme == "https") {
                        val cleanTitle = metadata["tvg-name"].orEmpty().ifBlank { title }
                            .ifBlank { uri?.path.orEmpty().substringAfterLast('/').substringBeforeLast('.') }
                            .ifBlank { "Channel ${channels.size + 1}" }
                        val stableValue = metadata["tvg-id"].orEmpty().ifBlank { line }
                        channels += MediaEntry(
                            id = "$sourceId:${stableHash(stableValue)}",
                            title = cleanTitle,
                            category = group.ifBlank { "Other" },
                            streamUrl = line,
                            logoUrl = metadata["tvg-logo"].orEmpty(),
                            epgChannelId = metadata["tvg-id"].orEmpty(),
                        )
                    }
                    metadata = emptyMap()
                    title = ""
                    group = ""
                }
            }
        }
        return channels
    }

    /**
     * Parses a playlist as a stream and emits bounded batches. This avoids keeping
     * a complete remote M3U playlist and every parsed channel in memory at once.
     * Throws when maxChannels is exceeded instead of silently truncating content.
     */
    fun parseBatched(
        reader: Reader,
        sourceId: String,
        batchSize: Int = 500,
        maxChannels: Int = 1_000_000,
        onBatch: (List<MediaEntry>) -> Unit,
    ): Int {
        require(batchSize in 1..5_000) { "Batch size must be between 1 and 5000." }
        require(maxChannels > 0) { "Maximum channel count must be positive." }
        val input = if (reader is BufferedReader) reader else reader.buffered()
        val batch = ArrayList<MediaEntry>(batchSize)
        var metadata: Map<String, String> = emptyMap()
        var title = ""
        var group = ""
        var count = 0
        while (true) {
            val rawLine = input.readLine() ?: break
            val line = rawLine.trim().removePrefix("\uFEFF")
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val info = line.substringAfter(':')
                    val comma = info.indexOfUnquotedComma()
                    val attributeText = if (comma >= 0) info.substring(0, comma) else info
                    title = if (comma >= 0) info.substring(comma + 1).trim() else ""
                    metadata = attributePattern.findAll(attributeText).associate {
                        it.groupValues[1].lowercase() to it.groupValues[2].trim('"', '\'')
                    }
                    group = metadata["group-title"].orEmpty()
                }
                line.startsWith("#EXTGRP:", ignoreCase = true) -> group = line.substringAfter(':').trim()
                line.isNotEmpty() && !line.startsWith('#') -> {
                    val uri = runCatching { URI(line) }.getOrNull()
                    val scheme = uri?.scheme?.lowercase()
                    if (scheme == "http" || scheme == "https") {
                        if (count >= maxChannels) {
                            throw IOException("This playlist exceeds the safe limit of $maxChannels channels. The previous saved catalogue was preserved.")
                        }
                        val cleanTitle = metadata["tvg-name"].orEmpty().ifBlank { title }
                            .ifBlank { uri?.path.orEmpty().substringAfterLast('/').substringBeforeLast('.') }
                            .ifBlank { "Channel ${count + 1}" }
                        val stableValue = metadata["tvg-id"].orEmpty().ifBlank { line }
                        batch += MediaEntry(
                            id = "$sourceId:${stableHash(stableValue)}",
                            title = cleanTitle,
                            category = group.ifBlank { "Other" },
                            streamUrl = line,
                            logoUrl = metadata["tvg-logo"].orEmpty(),
                            epgChannelId = metadata["tvg-id"].orEmpty(),
                        )
                        count++
                        if (batch.size >= batchSize) { onBatch(batch.toList()); batch.clear() }
                    }
                    metadata = emptyMap()
                    title = ""
                    group = ""
                }
            }
        }
        if (batch.isNotEmpty()) onBatch(batch.toList())
        return count
    }
    private fun String.indexOfUnquotedComma(): Int {
        var quote: Char? = null
        forEachIndexed { index, character ->
            if (quote == character) quote = null
            else if (quote == null && (character == '"' || character == '\'')) quote = character
            else if (quote == null && character == ',') return index
        }
        return -1
    }

    private fun stableHash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .take(12)
        .joinToString("") { "%02x".format(it) }
}