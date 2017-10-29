package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.components.ServiceManager

/**
 * Text to speech.
 */
class TextToSpeech private constructor() {

    private var currentToken: Long = 0
    private var currentPlayer: TTSPlayer? = null

    /**
     * Text to speech.
     *
     * @param text the text.
     */
    @Synchronized
    fun speak(text: String, token: Long = 0) {
        currentPlayer?.stop()
        currentPlayer = GoogleTTSPlayer(text, Lang.ENGLISH) { player ->
            synchronized(this@TextToSpeech) {
                if (player === currentPlayer) {
                    currentToken = 0
                    currentPlayer = null
                }
            }
        }.apply {
            currentToken = token
            start()
        }
    }

    @Synchronized
    fun stop(token: Long) {
        if (currentToken == 0L || currentToken == token) {
            currentPlayer?.stop()
        }
    }

    companion object {
        val INSTANCE: TextToSpeech
            get() = ServiceManager.getService(TextToSpeech::class.java)
    }
}
