package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.DEFAULT_USER_AGENT
import cn.yiiguxing.plugin.translate.GOOGLE_TTS
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.tk
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
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

    @Volatile private var started = false

    private val playlist: List<String> by lazy {
        with(text.splitSentence(MAX_TEXT_LENGTH)) {
            mapIndexed { index, sentence ->
                "$GOOGLE_TTS?client=gtx&ie=UTF-8&tl=${lang.code}&total=$size&idx=$index&textlen=${sentence.length}" +
                        "&tk=${sentence.tk()}&q=${sentence.urlEncode()}"
            }
        }
    }

    init {
        playTask = PlayTask(project).apply {
            cancelText = "stop"
            cancelTooltipText = "stop"
        }
        playController = BackgroundableProcessIndicator(playTask).apply {
            isIndeterminate = true
        }
        disposable = playController
    }

    private inner class PlayTask(project: Project?) : Task.Backgroundable(project, "TTS") {
        override fun run(indicator: ProgressIndicator) {
            play(indicator)
        }

        override fun onFinished() {
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
        indicator.apply {
            checkCanceled()
            text = "tts: downloading..."
        }
        try {
            playlist
                    .map {
                        if (indicator.isCanceled) return@map null
                        LOGGER.i("TTS>>> $it")
                        HttpRequests.request(it)
                                .userAgent(DEFAULT_USER_AGENT)
                                .readBytes(indicator)
                                .let { ByteArrayInputStream(it) }
                    }
                    .filterNotNull()
                    .takeIf { it.isNotEmpty() }
                    ?.toEnumeration()
                    ?.let {
                        SequenceInputStream(it).use {
                            indicator.checkCanceled()
                            it.asAudioInputStream().rawPlay(indicator)
                        }
                    }
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            LOGGER.e("play", e)
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
            text = "tts: playing..."
        }

        format.openLine()?.run {
            start()

            @Suppress("ConvertTryFinallyToUseCall") try {
                val data = ByteArray(2048)
                var bytesRead: Int
                while (!indicator.isCanceled) {
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
        private val LOGGER = Logger.getInstance(GoogleTTSPlayer::class.java)

        private const val MAX_TEXT_LENGTH = 200

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