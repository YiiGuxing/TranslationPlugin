package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackController
import cn.yiiguxing.plugin.translate.tts.sound.isCompletedState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Provide text-to-speech conversion.
 */
@Service
class TextToSpeech private constructor() {

    private var currentPlaying: PlaybackController? = null

    /**
     * Text to speech.
     *
     * @param project the project.
     * @param text the text.
     * @param lang the language.
     * @param autoStart `true` to start playing immediately.
     */
    fun speak(project: Project?, text: String, lang: Lang = Lang.AUTO, autoStart: Boolean = true): PlaybackController {
        check(text.isNotBlank()) { "text is blank." }

        val player = GoogleTTSPlayer.create(project, text, lang)
        if (autoStart) {
            player.stateBinding.observe { state, _ ->
                synchronized(this@TextToSpeech) {
                    if (state.isCompletedState && currentPlaying === player) {
                        currentPlaying = null
                    }
                }
            }
            synchronized(this) {
                currentPlaying?.stop()
                currentPlaying = player
                player.start()
            }
        }

        return player
    }

    /**
     * Returns `true` if the [language][lang] is supported.
     */
    fun isSupportLanguage(lang: Lang): Boolean = GoogleTTSPlayer.SUPPORTED_LANGUAGES.contains(lang)

    fun stop() {
        synchronized(this) {
            currentPlaying?.stop()
            currentPlaying = null
        }
    }

    companion object {
        val instance: TextToSpeech get() = service()
    }
}
