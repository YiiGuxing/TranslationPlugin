package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.checkDispatchThread
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

/**
 * Text to speech.
 */
class TextToSpeech private constructor() {

    private var currentPlayer: TTSPlayer? = null

    /**
     * Text to speech.
     *
     * @param text the text.
     */
    fun speak(project: Project?, text: String): Disposable {
        checkThread()
        currentPlayer?.stop()

        return GoogleTTSPlayer(project, text, Lang.ENGLISH) { player ->
            if (player === currentPlayer) {
                currentPlayer = null
            }
        }.run {
            currentPlayer = this
            start()

            disposable
        }
    }

    @Suppress("unused")
    fun stop() {
        checkThread()
        currentPlayer?.stop()
    }

    companion object {
        val INSTANCE: TextToSpeech
            get() = ServiceManager.getService(TextToSpeech::class.java)

        private fun checkThread() = checkDispatchThread(TextToSpeech::class.java)
    }
}
