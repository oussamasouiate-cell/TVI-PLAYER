package store.oatec.playertv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class EpgRepositoryTest {
    @Test
    fun parsesProgrammeAndTimezoneOffset() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="news"><display-name>News</display-name></channel>
              <programme start="20261024090000 +0200" stop="20261024100000 +0200" channel="news">
                <title lang="en">Morning News</title>
                <desc>Daily headlines</desc>
              </programme>
            </tv>
        """.trimIndent()

        val programmes = EpgRepository.parseXmltv(xml)

        assertEquals(1, programmes.size)
        assertEquals("news", programmes[0].channelId)
        assertEquals("Morning News", programmes[0].title)
        assertEquals("Daily headlines", programmes[0].description)
        assertEquals(epochMillis(2026, 10, 24, 7, 0), programmes[0].startTimeMillis)
        assertEquals(epochMillis(2026, 10, 24, 8, 0), programmes[0].endTimeMillis)
    }

    @Test
    fun skipsInvalidProgrammeEntries() {
        val xml = """
            <tv>
              <programme start="not-a-date" stop="20261024100000 +0000" channel="x">
                <title>Bad time</title>
              </programme>
              <programme start="20261024110000 +0000" stop="20261024100000 +0000" channel="x">
                <title>Backwards interval</title>
              </programme>
              <programme start="20261024110000 +0000" stop="20261024120000 +0000" channel="x">
                <title>Valid</title>
              </programme>
            </tv>
        """.trimIndent()

        val programmes = EpgRepository.parseXmltv(xml)

        assertEquals(1, programmes.size)
        assertEquals("Valid", programmes.single().title)
    }

    @Test
    fun onAirUsesStartInclusiveEndExclusiveInterval() {
        val programme = EpgProgram("x", "Show", 1_000L, 2_000L)

        assertFalse(programme.isOnAir(999L))
        assertTrue(programme.isOnAir(1_000L))
        assertTrue(programme.isOnAir(1_999L))
        assertFalse(programme.isOnAir(2_000L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedXml() {
        EpgRepository.parseXmltv(" ".repeat(12 * 1024 * 1024 + 1))
    }

    private fun epochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis
    }
}
