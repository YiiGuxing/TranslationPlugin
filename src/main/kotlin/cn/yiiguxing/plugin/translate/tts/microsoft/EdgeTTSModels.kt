package cn.yiiguxing.plugin.translate.tts.microsoft

import com.google.gson.annotations.SerializedName
import com.intellij.openapi.util.text.StringUtil

/**
 * SSML builder.
 *
 * @property voice The voice.
 * @property rate The rate: [[-50, 100]].
 * @property text The content text.
 */
internal class SSMLBuilder {
    lateinit var voice: String
    var rate: Int = EDGE_TTS_NORMAL_SPEED
        set(value) {
            field = value.coerceIn(EDGE_TTS_MIN_SPEED, EDGE_TTS_MAX_SPEED)
        }
    lateinit var text: String

    fun build(): String = StringBuilder().apply {
        append("<speak version='1.0' ")
        append("xmlns='http://www.w3.org/2001/10/synthesis' ")
        append("xmlns:mstts='https://www.w3.org/2001/mstts' xml:lang='en-US'>")
        append("<voice name='${voice.escape()}'>")
        append("<prosody pitch='+0Hz' rate='${rate.toRateString()}%' volume='+0%'>")
        append(text.escape())
        append("</prosody>")
        append("</voice>")
        append("</speak>")
    }.toString()
}

private fun Int.toRateString(): String = if (this >= 0) "+$this" else this.toString()

private fun String.escape(): String = StringUtil.escapeXmlEntities(this)

/**
 * Builds SSML.
 */
internal inline fun ssml(builder: SSMLBuilder.() -> Unit): String {
    return SSMLBuilder().apply(builder).build()
}

/**
 * Edge TTS request message builder.
 *
 * @property requestId The request id.
 * @property timestamp The request timestamp.
 * @property contentType The request content type.
 * @property path The request path.
 * @property content The request content.
 */
internal class EdgeTTSRequestMessageBuilder {
    var requestId: String? = null
    lateinit var timestamp: String
    lateinit var contentType: String
    lateinit var path: String
    lateinit var content: String

    /**
     * Builds the request message.
     */
    fun build(): String = StringBuilder().apply {
        requestId?.let { append("X-RequestId:$it\r\n") }
        append("Content-Type:$contentType\r\n")
        append("X-Timestamp:$timestamp\r\n")
        append("Path:$path\r\n\r\n")
        append(content)
    }.toString()
}

/**
 * Builds a request message.
 */
internal inline fun edgeTTSRequestMessage(builder: EdgeTTSRequestMessageBuilder.() -> Unit): String {
    return EdgeTTSRequestMessageBuilder().apply(builder).build()
}

/**
 * Edge TTS message.
 *
 * @property headers The headers of the message.
 * @property content The content of the message.
 */
internal data class EdgeTTSMessage(
    val headers: Map<String, String>,
    val content: String
) {

    /**
     * Gets the value of the header with the specified [key].
     */
    operator fun get(key: String): String? = headers[key]

    override fun toString(): String {
        return "EdgeTTSMessage(headers=$headers, content='$content')"
    }

    companion object {
        private const val HEADER_SEPARATOR = "\r\n"
        private const val HEADER_VALUE_SEPARATOR = ":"
        private const val HEADER_CONTENT_SEPARATOR = "\r\n\r\n"

        /**
         * Parses the specified [message] to an [EdgeTTSMessage].
         */
        fun parse(message: String): EdgeTTSMessage {
            return message.split(HEADER_CONTENT_SEPARATOR).let { parts ->
                val headers = parts[0].split(HEADER_SEPARATOR).associate {
                    val (key, value) = it.split(HEADER_VALUE_SEPARATOR)
                    key to value
                }
                val content = parts.getOrElse(1) { "" }

                EdgeTTSMessage(headers, content)
            }
        }
    }
}

internal data class EdgeTTSVoice(
    @SerializedName("Name")
    val name: String,
    @SerializedName("ShortName")
    val shortName: String,
    @SerializedName("FriendlyName")
    val friendlyName: String,
    @SerializedName("Gender")
    val gender: String,
    @SerializedName("Locale")
    val locale: String,
    @SerializedName("SuggestedCodec")
    val suggestedCodec: String,
    @SerializedName("Status")
    val status: String,
    @SerializedName("VoiceTag")
    val voiceTag: VoiceTag = VoiceTag(),
)

internal data class VoiceTag(
    @SerializedName("ContentCategories")
    val contentCategories: List<String> = emptyList(),
    @SerializedName("VoicePersonalities")
    val voicePersonalities: List<String> = emptyList(),
)