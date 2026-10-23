package store.oatec.playertv.data

import java.net.URI
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
            val line = rawLine.trim()
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