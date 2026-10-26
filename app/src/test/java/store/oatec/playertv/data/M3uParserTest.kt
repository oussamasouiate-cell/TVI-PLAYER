package store.oatec.playertv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {
    @Test
    fun parsesQuotedAttributesGroupsAndCommaInTitle() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="news.one" tvg-name="News, One" tvg-logo="https://img.test/logo.png" group-title="News",Ignored title
            https://stream.test/live.m3u8
        """.trimIndent()

        val channel = M3uParser.parse(playlist, "source-1").single()

        assertEquals("News, One", channel.title)
        assertEquals("News", channel.category)
        assertEquals("https://img.test/logo.png", channel.logoUrl)
        assertEquals("https://stream.test/live.m3u8", channel.streamUrl)
        assertEquals("news.one", channel.epgChannelId)
        assertTrue(channel.id.startsWith("source-1:"))
    }

    @Test
    fun skipsMalformedAndNonHttpStreamLines() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Invalid
            file:///private/stream.ts
            #EXTINF:-1,Valid
            https://stream.test/good.ts
        """.trimIndent()

        assertEquals(listOf("Valid"), M3uParser.parse(playlist, "source-1").map { it.title })
    }

    @Test
    fun streamingParserEmitsBoundedBatchesAndReturnsTotalCount() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="news.one" group-title="News",One
            https://stream.test/one.ts
            #EXTINF:-1 group-title="News",Two
            https://stream.test/two.ts
            #EXTINF:-1 group-title="Movies",Three
            https://stream.test/three.ts
        """.trimIndent()
        val batches = mutableListOf<List<store.oatec.playertv.model.MediaEntry>>()

        val count = M3uParser.parseBatched(
            playlist.reader(),
            "source-1",
            batchSize = 2,
            maxChannels = 10,
            onBatch = { batch -> batches.add(batch) },
        )

        assertEquals(3, count)
        assertEquals(listOf(2, 1), batches.map { it.size })
        assertEquals(listOf("One", "Two", "Three"), batches.flatten().map { it.title })
        assertEquals("news.one", batches.flatten().first().epgChannelId)
    }

    @Test
    fun channelIdentityIsStableAcrossRefreshes() {
        val playlist = "#EXTINF:-1 tvg-id=stable-id,Channel\nhttps://one.test/live"

        assertEquals(
            M3uParser.parse(playlist, "source-1").single().id,
            M3uParser.parse(playlist, "source-1").single().id,
        )
    }

    @Test
    fun preservesStreamUrlQueryParameters() {
        val streamUrl = "https://stream.test/live?username=viewer&token=a%2Fb&quality=4k"
        val playlist = "#EXTINF:-1,Query stream\n$streamUrl"

        assertEquals(streamUrl, M3uParser.parse(playlist, "source-1").single().streamUrl)
    }

    @Test
    fun streamingParserFailsRatherThanSilentlyTruncatingAtLimit() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,First
            https://stream.test/first.ts
            #EXTINF:-1,Second
            https://stream.test/second.ts
        """.trimIndent()

        val error = org.junit.Assert.assertThrows(java.io.IOException::class.java) {
            M3uParser.parseBatched(
                playlist.reader(),
                "source-1",
                batchSize = 1,
                maxChannels = 1,
                onBatch = {},
            )
        }

        assertTrue(error.message.orEmpty().contains("safe limit"))
    }

    @Test
    fun stopsAtConfiguredChannelLimit() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,First
            https://stream.test/first.ts
            #EXTINF:-1,Second
            https://stream.test/second.ts
            #EXTINF:-1,Third
            https://stream.test/third.ts
        """.trimIndent()

        assertEquals(listOf("First", "Second"), M3uParser.parse(playlist, "source-1", maxChannels = 2).map { it.title })
    }
}