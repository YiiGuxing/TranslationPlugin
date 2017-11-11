package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.util.e
import cn.yiiguxing.plugin.translate.util.i
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import java.io.InputStream
import javax.sound.sampled.*

/**
 * NetworkTTSPlayer
 *
 * Created by Yii.Guxing on 2017-10-28 0028.
 */
open class NetworkTTSPlayer(private val url: String) : TTSPlayer {

    @Volatile private var play = false
    @Volatile private var started = false

    override fun isPlaying(): Boolean {
        checkThread()
        return play
    }

    override fun start() {
        checkThread()
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
        checkThread()
        play = false
    }

    protected open fun buildRequest(builder: RequestBuilder) {}

    protected open fun finished() {}

    private fun play() {
        if (!play) return
        try {
            LOGGER.i("TTS>>> $url")
            HttpRequests.request(url).also { buildRequest(it) }.connect { request ->
                if (play) {
                    request.inputStream.asAudioInputStream().rawPlay()
                }
            }
        } catch (e: Throwable) {
            LOGGER.e("play", e)
        }
    }

    private fun AudioInputStream.rawPlay() {
        val decodedFormat = format.let {
            AudioFormat(AudioFormat.Encoding.PCM_SIGNED, it.sampleRate, 16, it.channels,
                    it.channels * 2, it.sampleRate, false)
        }

        MpegFormatConversionProvider()
                .getAudioInputStream(decodedFormat, this)
                .rawPlay(decodedFormat)
    }

    private fun AudioInputStream.rawPlay(format: AudioFormat) {
        if (!play) return
        format.openLine()?.run {
            start()

            @Suppress("ConvertTryFinallyToUseCall") try {
                val data = ByteArray(4096)
                var bytesRead: Int
                while (play) {
                    bytesRead = read(data, 0, data.size)
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
        private val LOGGER = Logger.getInstance(NetworkTTSPlayer::class.java)

        private fun checkThread() = check(ApplicationManager.getApplication().isDispatchThread) {
            "NetworkTTSPlayer must only be used from the Event Dispatch Thread."
        }

        private fun InputStream.asAudioInputStream(): AudioInputStream =
                MpegAudioFileReader().getAudioInputStream(this)

        private fun AudioFormat.openLine(): SourceDataLine? = try {
            val info = DataLine.Info(SourceDataLine::class.java, this)
            (AudioSystem.getLine(info) as? SourceDataLine)?.apply {
                open(this@openLine)
            }
        } catch (e: Exception) {
            LOGGER.e("openLine", e)
            null
        }
    }
}