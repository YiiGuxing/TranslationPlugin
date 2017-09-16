package cn.yiiguxing.plugin.translate

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import java.io.BufferedInputStream
import java.io.IOException
import java.net.URL
import javax.sound.sampled.*

/**
 * Text to speech util.
 */
object Speech {

    private val TTS_URL = "http://dict.youdao.com/dictvoice?audio=%s&type=%d"

    private val LOG = Logger.getInstance("#" + Speech::class.java.canonicalName)

    enum class Phonetic(val value: Int) {
        /**
         * 英式发音
         */
        UK(1),
        /**
         * 美式发音
         */
        US(2)
    }

    /**
     * 转换为语音
     *
     * @param text     目标文本
     * @param phonetic 音标
     */
    fun play(text: String, phonetic: Phonetic = Phonetic.UK) {
        ApplicationManager.getApplication().executeOnPooledThread { _play(text, phonetic) }
    }

    private fun _play(text: String, phonetic: Phonetic) {
        try {
            val url = URL(String.format(TTS_URL, text, phonetic.value))
            val inputStream = BufferedInputStream(url.openStream())

            MpegAudioFileReader().getAudioInputStream(inputStream)?.use {
                val baseFormat = it.format
                val decodedFormat = AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.sampleRate,
                        16,
                        baseFormat.channels,
                        baseFormat.channels * 2,
                        baseFormat.sampleRate,
                        false
                )
                val din = MpegFormatConversionProvider().getAudioInputStream(decodedFormat, it)

                // Play now.
                din.use {
                    rawPlay(decodedFormat, it)
                }
            }
        } catch (e: Exception) {
            LOG.error("toSpeech", e)
        }

    }

    @Throws(IOException::class, LineUnavailableException::class)
    private fun rawPlay(targetFormat: AudioFormat, din: AudioInputStream) {
        getLine(targetFormat)?.run {
            start()

            try {
                val data = ByteArray(4096)
                var bytesRead: Int
                while (true) {
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

    @Throws(LineUnavailableException::class)
    private fun getLine(audioFormat: AudioFormat): SourceDataLine? {
        val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
        val res = AudioSystem.getLine(info) as? SourceDataLine
        res?.open(audioFormat)

        return res
    }

}
