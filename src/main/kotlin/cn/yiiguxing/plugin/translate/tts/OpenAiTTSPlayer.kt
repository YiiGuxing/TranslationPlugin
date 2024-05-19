package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.openai.OpenAiService
import cn.yiiguxing.plugin.translate.trans.openai.OpenAiSettings
import cn.yiiguxing.plugin.translate.trans.openai.exception.OpenAIStatusException
import cn.yiiguxing.plugin.translate.tts.sound.AudioPlayer
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackController
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackStatus
import cn.yiiguxing.plugin.translate.tts.sound.source.DefaultPlaybackSource
import cn.yiiguxing.plugin.translate.tts.sound.source.PlaybackLoader
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import java.io.IOException

/**
 * The OpenAI TTS player.
 *
 * @param project the project.
 * @param text the text to be synthesized.
 */
class OpenAiTTSPlayer private constructor(
    private val project: Project?,
    private val text: String
) : PlaybackController {

    private val player: AudioPlayer = AudioPlayer(DefaultPlaybackSource(Loader())).apply {
        setErrorHandler(::showErrorNotification)
    }

    override val statusBinding: Observable<PlaybackStatus> = player.statusBinding

    @Volatile
    private lateinit var modalityState: ModalityState

    override fun start() {
        modalityState = ModalityState.defaultModalityState()
        player.start()
    }

    override fun stop() {
        player.stop()
    }

    private fun showErrorNotification(error: Throwable) {
        if (project?.isDisposed != false) {
            return
        }

        val message = when (error) {
            is IOException -> (error as? OpenAIStatusException)?.error?.message ?: error.getCommonMessage()
            else -> {
                thisLogger().warn("OpenAi TTS Error", error)
                error.message
            }
        } ?: message("error.unknown")
        Notifications.showErrorNotification("OpenAi TTS", message, project)
    }

    private inner class Loader : PlaybackLoader.MultiSource<String>(
        text.splitSentence(MAX_TEXT_LENGTH).iterator()
    ) {
        private lateinit var service: OpenAiService
        private val indicator = EmptyProgressIndicator()

        override fun onStart() {
            service = OpenAiService.get(service<OpenAiSettings>().getOptions())
            if (TTSEngine.OPENAI.isConfigured()) {
                return
            }
            invokeAndWait(modalityState) {
                if (project?.isDisposed != false || !TTSEngine.OPENAI.showConfigurationDialog()) {
                    cancel()
                }
            }
        }

        override fun onLoad(src: String): ByteArray = service.speech(src, indicator)

        override fun onError(error: Throwable) {
            showErrorNotification(error)
        }

        override fun onCanceled() {
            indicator.cancel()
        }
    }

    companion object {
        private const val MAX_TEXT_LENGTH = 300

        /**
         * Creates a new [OpenAiTTSPlayer] instance with the specified [text].
         *
         * @param project the project.
         * @param text the text to be synthesized.
         */
        fun create(project: Project?, text: String): OpenAiTTSPlayer {
            return OpenAiTTSPlayer(project, text)
        }
    }
}