package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.components.ServiceManager

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
    @Synchronized
    fun speak(text: String) {
        currentPlayer?.stop()
        currentPlayer = GoogleTTSPlayer(text, Lang.ENGLISH) { player ->
            synchronized(this@TextToSpeech) {
                if (player === currentPlayer) {
                    currentPlayer = null
                }
            }
        }.apply { start() }
    }

    companion object {
        val INSTANCE: TextToSpeech
            get() = ServiceManager.getService(TextToSpeech::class.java)
    }
}
