package store.oatec.playertv.data

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.InputSource

/** A single XMLTV programme slot. Times are stored as epoch milliseconds. */
data class EpgProgram(
    val channelId: String,
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val description: String = "",
) {
    fun isOnAir(nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis >= startTimeMillis && nowMillis < endTimeMillis
}

/**
 * Parses standard XMLTV programme entries and provides current/next lookups.
 * Fetching a guide URL and associating it with a source are handled by the source layer.
 */
object EpgRepository {
    private const val MAX_XML_CHARS = 12 * 1024 * 1024
    private const val MAX_PROGRAMMES = 100_000

    fun parseXmltv(xml: String): List<EpgProgram> {
        require(xml.length <= MAX_XML_CHARS) { "The EPG XML exceeds the 12 MB parsing limit." }
        val factory = SAXParserFactory.newInstance()
        factory.isNamespaceAware = false
        factory.isXIncludeAware = false
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        val handler = ProgrammeHandler()
        factory.newSAXParser().parse(InputSource(StringReader(xml)), handler)
        return handler.programmes.toList()
    }

    /** Returns the programme airing now on the requested XMLTV channel id, if known. */
    fun currentProgram(
        programmes: List<EpgProgram>,
        channelId: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): EpgProgram? = programmes.asSequence()
        .filter { it.channelId == channelId && it.isOnAir(nowMillis) }
        .minByOrNull { it.startTimeMillis }

    /** Returns the next programme whose start time is at or after now. */
    fun nextProgram(
        programmes: List<EpgProgram>,
        channelId: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): EpgProgram? = programmes.asSequence()
        .filter { it.channelId == channelId && it.startTimeMillis >= nowMillis && it.endTimeMillis > it.startTimeMillis }
        .minByOrNull { it.startTimeMillis }

    private class ProgrammeHandler : DefaultHandler() {
        val programmes = mutableListOf<EpgProgram>()
        private var currentChannel = ""
        private var currentTitle = ""
        private var currentDescription = ""
        private var startMillis = 0L
        private var endMillis = 0L
        private var currentField = ""
        private val text = StringBuilder()

        override fun startElement(uri: String?, localName: String?, qName: String, attrs: Attributes) {
            when (qName) {
                "programme" -> {
                    currentChannel = attrs.getValue("channel").orEmpty()
                    startMillis = parseXmltvTime(attrs.getValue("start").orEmpty()) ?: 0L
                    endMillis = parseXmltvTime(attrs.getValue("stop").orEmpty()) ?: 0L
                    currentTitle = ""
                    currentDescription = ""
                }
                "title", "desc" -> {
                    currentField = qName
                    text.setLength(0)
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (currentField.isNotEmpty()) text.append(ch, start, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String) {
            if (qName == "title" && currentField == "title") {
                if (currentTitle.isBlank()) currentTitle = text.toString().trim()
                currentField = ""
            } else if (qName == "desc" && currentField == "desc") {
                if (currentDescription.isBlank()) currentDescription = text.toString().trim()
                currentField = ""
            } else if (qName == "programme") {
                if (programmes.size < MAX_PROGRAMMES && currentChannel.isNotBlank() &&
                    currentTitle.isNotBlank() && startMillis > 0L && endMillis > startMillis
                ) {
                    programmes += EpgProgram(currentChannel, currentTitle, startMillis, endMillis, currentDescription)
                }
                currentField = ""
            }
        }
    }

    private fun parseXmltvTime(value: String): Long? {
        val formats = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmss")
        for (pattern in formats) {
            val format = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val position = ParsePosition(0)
            val normalized = value.trim()
            val date = format.parse(normalized, position)
            if (date != null && position.index == normalized.length) return date.time
        }
        return null
    }
}
