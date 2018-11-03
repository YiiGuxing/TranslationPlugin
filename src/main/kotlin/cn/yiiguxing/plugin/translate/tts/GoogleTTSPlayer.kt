package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.DEFAULT_USER_AGENT
import cn.yiiguxing.plugin.translate.GOOGLE_TTS
import cn.yiiguxing.plugin.translate.GOOGLE_TTS_CN
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.tk
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.io.HttpRequests
import javazoom.jl.decoder.Bitstream
import javazoom.spi.mpeg.sampled.convert.DecodedMpegAudioInputStream
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.SequenceInputStream
import javax.sound.sampled.*


/**
 * NetworkTTSPlayer
 *
 * Created by Yii.Guxing on 2017-10-28 0028.
 */
class GoogleTTSPlayer(
        project: Project?,
        private val text: String,
        private val lang: Lang,
        private val completeListener: ((TTSPlayer) -> Unit)? = null
) : TTSPlayer {

    private val playTask: PlayTask
    private val playController: ProgressIndicator

    override val disposable: Disposable
    override val isPlaying: Boolean
        get() {
            checkThread()
            return playController.isRunning
        }

    @Volatile
    private var started = false

    private var duration = 0

    private val playlist: List<String> by lazy {
        val baseUrl = if (Settings.googleTranslateSettings.useTranslateGoogleCom) {
            GOOGLE_TTS
        } else {
            GOOGLE_TTS_CN
        }
        text.splitSentence(MAX_TEXT_LENGTH).let {
            it.mapIndexed { index, sentence ->
                UrlBuilder(baseUrl)
                        .addQueryParameter("client", "gtx")
                        .addQueryParameter("ie", "UTF-8")
                        .addQueryParameter("tl", lang.code)
                        .addQueryParameter("total", it.size.toString())
                        .addQueryParameter("idx", index.toString())
                        .addQueryParameter("textlen", sentence.length.toString())
                        .addQueryParameter("tk", sentence.tk())
                        .addQueryParameter("q", sentence)
                        .build()
            }
        }
    }

    init {
        playTask = PlayTask(project).apply {
            cancelText = "stop"
            cancelTooltipText = "stop"
        }
        playController = BackgroundableProcessIndicator(playTask).apply { isIndeterminate = true }
        disposable = playController
    }

    private inner class PlayTask(project: Project?) : Task.Backgroundable(project, "TTS") {
        override fun run(indicator: ProgressIndicator) {
            play(indicator)
        }

        override fun onThrowable(error: Throwable) {
            if (error is HttpRequests.HttpStatusException && error.statusCode == 404) {
                LOGGER.w("TTS Error: Unsupported language: ${lang.code}.")

                @Suppress("InvalidBundleOrProperty")
                NotificationGroup(NOTIFICATION_ID, NotificationDisplayType.TOOL_WINDOW, true)
                        .createNotification("TTS", message("error.unsupportedLanguage", lang.langName),
                                NotificationType.WARNING, null)
                        .show(project)
            } else {
                LOGGER.e("TTS Error", error)
            }
        }

        override fun onFinished() {
            Disposer.dispose(disposable)
            completeListener?.invoke(this@GoogleTTSPlayer)
        }
    }

    override fun start() {
        checkThread()
        if (started) throw IllegalStateException("Start with wrong state.")

        started = true
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(playTask, playController)
    }

    override fun stop() {
        checkThread()
        playController.cancel()
    }

    private fun play(indicator: ProgressIndicator) {
        with(indicator) {
            checkCanceled()
            text = "tts: downloading..."
        }
        playlist
                .map { url ->
                    indicator.checkCanceled()
                    LOGGER.i("TTS>>> $url")
                    HttpRequests.request(url)
                            .userAgent(DEFAULT_USER_AGENT)
                            .readBytes(indicator)
                            .let {
                                ByteArrayInputStream(it).apply { duration += getAudioDuration(it.size) }
                            }
                }
                .enumeration()
                .let {
                    SequenceInputStream(it).use { sis ->
                        indicator.checkCanceled()
                        sis.asAudioInputStream().rawPlay(indicator)
                    }
                }
    }

    private fun AudioInputStream.rawPlay(indicator: ProgressIndicator) {
        val decodedFormat = format.let {
            AudioFormat(AudioFormat.Encoding.PCM_SIGNED, it.sampleRate, 16, it.channels,
                    it.channels * 2, it.sampleRate, false)
        }

        MpegFormatConversionProvider()
                .getAudioInputStream(decodedFormat, this)
                .rawPlay(decodedFormat, indicator)
    }

    private fun AudioInputStream.rawPlay(format: AudioFormat, indicator: ProgressIndicator) {
        indicator.apply {
            checkCanceled()
            fraction = 0.0
            isIndeterminate = false
            text = "tts: playing..."
        }

        this as DecodedMpegAudioInputStream
        format.openLine()?.run {
            start()
            @Suppress("ConvertTryFinallyToUseCall") try {
                val data = ByteArray(2048)
                var bytesRead: Int
                while (!indicator.isCanceled) {
                    bytesRead = read(data, 0, data.size)
                    if (bytesRead != -1) {
                        write(data, 0, bytesRead)

                        val currentTime = properties()["mp3.position.microseconds"] as Long / 1000
                        indicator.fraction = currentTime.toDouble() / duration.toDouble()
                    } else {
                        indicator.fraction = 1.0
                        break
                    }
                }

                drain()
                stop()
            } finally {
                duration = 0
                close()
            }
        }
    }

    companion object {
        private val LOGGER = Logger.getInstance(GoogleTTSPlayer::class.java)

        private const val MAX_TEXT_LENGTH = 200

        private const val NOTIFICATION_ID = "TTS Error"

        val SUPPORTED_LANGUAGES: List<Lang> = listOf(
                Lang.CHINESE, Lang.ENGLISH, Lang.CHINESE_TRADITIONAL, Lang.ALBANIAN, Lang.ARABIC, Lang.ESTONIAN,
                Lang.ICELANDIC, Lang.POLISH, Lang.BOSNIAN, Lang.AFRIKAANS, Lang.DANISH, Lang.GERMAN, Lang.RUSSIAN,
                Lang.FRENCH, Lang.FINNISH, Lang.KHMER, Lang.KOREAN, Lang.DUTCH, Lang.CATALAN, Lang.CZECH, Lang.CROATIAN,
                Lang.LATIN, Lang.LATVIAN, Lang.ROMANIAN, Lang.MACEDONIAN, Lang.BENGALI, Lang.NEPALI, Lang.NORWEGIAN,
                Lang.PORTUGUESE, Lang.JAPANESE, Lang.SWEDISH, Lang.SERBIAN, Lang.ESPERANTO, Lang.SLOVAK, Lang.SWAHILI,
                Lang.TAMIL, Lang.THAI, Lang.TURKISH, Lang.WELSH, Lang.UKRAINIAN, Lang.SPANISH, Lang.GREEK,
                Lang.HUNGARIAN, Lang.ARMENIAN, Lang.ITALIAN, Lang.HINDI, Lang.SUNDANESE, Lang.INDONESIAN,
                Lang.JAVANESE, Lang.VIETNAMESE)

        private fun checkThread() = checkDispatchThread(GoogleTTSPlayer::class.java)

        private fun InputStream.asAudioInputStream(): AudioInputStream =
                MpegAudioFileReader().getAudioInputStream(this)

        private fun InputStream.getAudioDuration(dataLength: Int): Int {
            return try {
                Math.round(Bitstream(this).readFrame().total_ms(dataLength))
            } catch (e: Throwable) {
                LOGGER.error(e)
                0
            } finally {
                reset()
            }
        }

        private fun AudioFormat.openLine(): SourceDataLine? = try {
            val info = DataLine.Info(SourceDataLine::class.java, this)
            (AudioSystem.getLine(info) as? SourceDataLine)?.apply {
                open(this@openLine)
            }
        } catch (e: Exception) {
            LOGGER.w("openLine", e)
            null
        }
    }
}