package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.checkDispatchThread
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * Provide text-to-speech conversion.
 */
@Service
class TextToSpeech private constructor() {

    private var currentPlayer: TTSPlayer? = null

    /**
     * Text to speech.
     *
     * @param project the project.
     * @param text the text.
     * @param lang the language.
     */
    fun speak(project: Project?, text: String, lang: Lang): Disposable {
        checkThread()
        currentPlayer?.stop()

        return GoogleTTSPlayer(project, text, lang) { player ->
            if (player === currentPlayer) {
                currentPlayer = null
            }
        }.let {
            currentPlayer = it
            it.start()

            it.disposable
        }
    }

    /**
     * Returns `true` if the [language][lang] is supported.
     */
    fun isSupportLanguage(lang: Lang): Boolean = GoogleTTSPlayer.SUPPORTED_LANGUAGES.contains(lang)

    @Suppress("unused")
    fun stop() {
        checkThread()
        currentPlayer?.stop()
    }

    companion object {
        val instance: TextToSpeech
            get() = ApplicationManager.getApplication().getService(TextToSpeech::class.java)

        private fun checkThread() = checkDispatchThread<TextToSpeech>()
    }
}
