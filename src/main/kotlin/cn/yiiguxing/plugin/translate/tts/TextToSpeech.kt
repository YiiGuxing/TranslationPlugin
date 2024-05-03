package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.tts.microsoft.EdgeTTSPlayer
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackController
import cn.yiiguxing.plugin.translate.tts.sound.isCompletedState
import cn.yiiguxing.plugin.translate.util.Observable
import cn.yiiguxing.plugin.translate.util.checkDispatchThread
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Provide text-to-speech conversion.
 */
@Service
class TextToSpeech private constructor() {

    private val settings: Settings by lazy { service<Settings>() }

    private var currentPlaying: PlaybackController? = null

    /**
     * Text to speech.
     *
     * @param project the project.
     * @param text the text.
     * @param lang the language.
     * @param autoStart `true` to start playing immediately.
     */
    fun speak(
        project: Project?,
        text: String,
        lang: Lang = Lang.AUTO,
        autoStart: Boolean = true
    ): PlaybackController {
        checkThread()
        check(text.isNotBlank()) { "text is blank." }

        val player = getPlayer(project, text, lang)
        if (!autoStart) {
            return player
        }

        currentPlaying?.stop()
        currentPlaying = player
        player.statusBinding.observe(Observable.ChangeOnEDTListener { state, _ ->
            if (state.isCompletedState && currentPlaying === player) {
                currentPlaying = null
            }
        })
        player.start()

        return player
    }

    private fun getPlayer(project: Project?, text: String, lang: Lang): PlaybackController {
        return when (settings.ttsEngine) {
            TTSEngine.EDGE -> EdgeTTSPlayer.create(project, text, lang)
            TTSEngine.GOOGLE -> GoogleTTSPlayer.create(project, text, lang)
            TTSEngine.OPENAI -> OpenAiTTSPlayer.create(project, text)
        }
    }

    /**
     * Returns `true` if the [language][lang] is supported.
     */
    fun isSupportLanguage(lang: Lang): Boolean {
        checkThread()
        return when (settings.ttsEngine) {
            TTSEngine.GOOGLE -> GoogleTTSPlayer.isSupportLanguage(lang)
            TTSEngine.EDGE -> EdgeTTSPlayer.isSupportLanguage(lang)
            TTSEngine.OPENAI -> true
        }
    }

    fun stop() {
        checkThread()
        currentPlaying?.stop()
        currentPlaying = null
    }

    companion object {
        /**
         * Returns the instance of [TextToSpeech].
         */
        fun getInstance(): TextToSpeech = service()

        private fun checkThread() = checkDispatchThread<TextToSpeech>()
    }
}
