package cn.yiiguxing.plugin.translate.tts.microsoft

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.tts.sound.source.PushablePlaybackSource
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.removeWhitespaces
import cn.yiiguxing.plugin.translate.util.splitSentence
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProcessCanceledException
import org.java_websocket.client.WebSocketClient
import org.java_websocket.framing.CloseFrame
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

/**
 * The source of the Microsoft Edge TTS.
 *
 * @param text The text to be synthesized.
 * @param lang The language of the text.
 */
internal class EdgeTTSSource(
    text: String,
    private val lang: Lang
) : PushablePlaybackSource() {

    private val wsClient = EdgeTTSWebSocketClient()
    private val sentences = text.splitSentence(MAX_TEXT_LENGTH).iterator()
    private var errorHandler: (Throwable) -> Unit = { thisLogger().error(it) }

    override fun prepare() {
        wsClient.connect()
    }

    fun onError(handler: (Throwable) -> Unit) {
        errorHandler = handler
    }

    override fun close() {
        wsClient.close()
        super.close()
    }

    private fun closeSource() = close()

    private inner class EdgeTTSWebSocketClient : WebSocketClient(getWSSUri()) {

        private val settings: EdgeTTSSettings by lazy { EdgeTTSSettings.instance() }

        init {
            addHeader("Pragma", "no-cache")
            addHeader("Cache-Control", "no-cache")
            addHeader("Accept-Encoding", "gzip, deflate, br")
            addHeader("Accept-Language", getLanguages())
            addHeader("User-Agent", Http.getUserAgent())
            @Suppress("SpellCheckingInspection")
            addHeader("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
        }

        override fun onOpen(handshakedata: ServerHandshake) {
            next()
        }

        override fun onMessage(message: String) {
            if (EdgeTTSMessage.parse(message)["Path"] == TURN_END) {
                next()
            }
        }

        override fun onMessage(bytes: ByteBuffer) {
            val headerLength = bytes.getShort().toInt()
            // Skip the header
            bytes.position(headerLength + 2)
            // Push the audio data
            push(bytes.slice())
        }

        private fun next() {
            if (sentences.hasNext()) {
                sendSpeechConfigRequest()
                sendSSML(sentences.next())
            } else {
                finish()
                close()
            }
        }

        private fun sendSpeechConfigRequest() {
            send(edgeTTSRequestMessage {
                contentType = "application/json; charset=utf-8"
                timestamp = getTimestamp()
                path = "speech.config"
                content = SPEECH_CONFIG
            })
        }

        private fun sendSSML(text: String) {
            send(edgeTTSRequestMessage {
                requestId = generateUuid()
                contentType = "application/ssml+xml"
                timestamp = getTimestamp() + "Z" // ???
                path = "ssml"
                content = ssml {
                    voice = settings.voice
                        ?.takeIf { it.isNotBlank() }
                        ?: EdgeTTSVoiceManager.getDefaultVoiceName(lang)
                    rate = settings.speed
                    this.text = text
                }
            })
        }

        override fun onError(ex: Exception) {
            setPrepareCanceled(ProcessCanceledException(ex))
            closeSource()
            errorHandler(ex)
            thisLogger().warn(ex)
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            if (remote && code == CODE_UNSUPPORTED_VOICE && !settings.voice.isNullOrBlank()) {
                settings.voice = null
            }

            var closed = false
            if (code != CloseFrame.NORMAL && reason.isNotEmpty()) {
                closed = true
                onError(EdgeTTSException(reason))
            }

            if (!closed && remote) {
                closeSource()
            }
        }
    }


    companion object {
        private const val MAX_TEXT_LENGTH = 200

        private const val TURN_END = "turn.end"

        private const val CODE_UNSUPPORTED_VOICE = 1007

        @Suppress("SpellCheckingInspection")
        // language=JSON
        private val SPEECH_CONFIG = """
            {
              "context": {
                "synthesis": {
                  "audio": {
                    "metadataoptions": {
                      "sentenceBoundaryEnabled": true,
                      "wordBoundaryEnabled": false
                    },
                    "outputFormat": "audio-24khz-48kbitrate-mono-mp3"
                  }
                }
              }
            }
        """.removeWhitespaces()

        private fun getWSSUri(): URI {
            return URI("$EDGE_TTS_WSS_URL?TrustedClientToken=$TRUSTED_CLIENT_TOKEN&ConnectionId=${generateUuid()}")
        }

        private fun generateUuid(): String {
            return UUID.randomUUID().toString().replace("-", "")
        }

        private fun getTimestamp(): String {
            @Suppress("SpellCheckingInspection")
            val sdfMain = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z ", Locale.ENGLISH)

            @Suppress("SpellCheckingInspection")
            val sdfZone = SimpleDateFormat("(zzzz)")

            val date = Date()
            val mainPart = sdfMain.format(date)
            val zonePart = sdfZone.format(date)

            return mainPart + zonePart
        }

        private fun getLanguages(): String {
            val locale = Locale.getDefault()
            val languageTag = locale.toLanguageTag()
            val language = locale.language

            var q = 9
            val sb = StringBuilder()
            sb.append(languageTag)
            if (languageTag != language) {
                sb.append(",").append(language).append(";q=0.").append(q--)
            }
            if (language != "en") {
                sb.append(",en;q=0.").append(q--)
                sb.append(",en-US;q=0.").append(q)
            }
            return sb.toString()
        }
    }
}