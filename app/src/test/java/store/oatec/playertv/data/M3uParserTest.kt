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
    fun channelIdentityIsStableAcrossRefreshes() {
        val playlist = "#EXTINF:-1 tvg-id=stable-id,Channel\nhttps://one.test/live"

        assertEquals(
            M3uParser.parse(playlist, "source-1").single().id,
            M3uParser.parse(playlist, "source-1").single().id,
        )
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