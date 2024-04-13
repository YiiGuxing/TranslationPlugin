package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.openai.OpenAiService
import cn.yiiguxing.plugin.translate.trans.openai.OpenAiSettings
import cn.yiiguxing.plugin.translate.trans.openai.exception.OpenAIStatusException
import cn.yiiguxing.plugin.translate.tts.sound.AudioPlayer
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackController
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackState
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.getCommonMessage
import cn.yiiguxing.plugin.translate.util.invokeAndWait
import cn.yiiguxing.plugin.translate.util.splitSentence
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import java.io.IOException

class OpenAiTTSPlayer private constructor(
    project: Project?,
    private val text: String,
    private val player: AudioPlayer = AudioPlayer()
) : PlaybackController by player {

    init {
        setupSources(project)
        setupErrorHandler(project)
    }

    private fun setupSources(project: Project?) {
        val service = OpenAiService.get(service<OpenAiSettings>().getOptions())
        val modalityState = ModalityState.defaultModalityState()
        val indicator = EmptyProgressIndicator()
        player.stateBinding.observe { state, _ ->
            if (state == PlaybackState.STOPPED) {
                indicator.cancel()
            }
        }
        text.splitSentence(MAX_TEXT_LENGTH).forEach { sentence ->
            player.addSource {
                if (!TTSEngine.OPENAI.isConfigured()) {
                    invokeAndWait(modalityState) {
                        if (project?.isDisposed == false) {
                            TTSEngine.OPENAI.showConfigurationDialog()
                        }
                    }
                }

                service.speech(sentence, indicator)
            }
        }
    }

    private fun setupErrorHandler(project: Project?) {
        player.setErrorHandler { error ->
            if (project?.isDisposed != false) {
                return@setErrorHandler
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
    }

    companion object {
        private const val MAX_TEXT_LENGTH = 300

        fun create(project: Project?, text: String): OpenAiTTSPlayer {
            return OpenAiTTSPlayer(project, text)
        }
    }
}