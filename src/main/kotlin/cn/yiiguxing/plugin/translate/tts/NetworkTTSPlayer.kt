package cn.yiiguxing.plugin.translate.tts

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import javax.sound.sampled.*

/**
 * NetworkTTSPlayer
 *
 * Created by Yii.Guxing on 2017-10-28 0028.
 */
open class NetworkTTSPlayer(private val url: String) : TTSPlayer {

    @Volatile private var play = false
    @Volatile private var started = false

    override fun isPlaying() = play

    override fun start() {
        if (started) throw IllegalStateException("Start with wrong state.")

        started = true
        play = true
        ApplicationManager.getApplication().executeOnPooledThread {
            play()
            play = false
            finished()
        }
    }

    override fun stop() {
        play = false
    }

    protected open fun buildRequest(builder: RequestBuilder) {}

    protected open fun finished() {}

    private fun play() {
        if (!play) return
        try {
            HttpRequests.request(url).also { buildRequest(it) }.connect { request ->
                if (!play) return@connect
                MpegAudioFileReader().getAudioInputStream(request.inputStream)?.use { ais ->
                    val baseFormat = ais.format
                    val decodedFormat = AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            baseFormat.sampleRate,
                            16,
                            baseFormat.channels,
                            baseFormat.channels * 2,
                            baseFormat.sampleRate,
                            false
                    )

                    MpegFormatConversionProvider()
                            .getAudioInputStream(decodedFormat, ais)
                            .use { rawPlay(decodedFormat, it) }
                }
            }
        } catch (e: Throwable) {
            LOGGER.error("play", e)
        }
    }

    private fun rawPlay(targetFormat: AudioFormat, din: AudioInputStream) {
        if (!play) return
        targetFormat.openLine()?.run {
            start()

            @Suppress("ConvertTryFinallyToUseCall") try {
                val data = ByteArray(4096)
                var bytesRead: Int
                while (play) {
                    bytesRead = din.read(data, 0, data.size)
                    if (bytesRead != -1) write(data, 0, bytesRead) else break
                }

                drain()
                stop()
            } finally {
                close()
            }
        }
    }

    companion object {
        private val LOGGER = Logger.getInstance("#${NetworkTTSPlayer::class.java.canonicalName}")

        private fun AudioFormat.openLine(): SourceDataLine? = try {
            val info = DataLine.Info(SourceDataLine::class.java, this)
            (AudioSystem.getLine(info) as? SourceDataLine)?.apply {
                open(this@openLine)
            }
        } catch (e: Exception) {
            LOGGER.error("openLine", e)
            null
        }
    }
}