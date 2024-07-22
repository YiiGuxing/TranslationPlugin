package cn.yiiguxing.plugin.translate.tts.microsoft

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.tts.sound.AudioPlayer
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackController
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackStatus
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.Observable
import cn.yiiguxing.plugin.translate.util.getCommonMessage
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import java.io.IOException

/**
 * The player of the Microsoft Edge TTS.
 *
 * @param text The text to be synthesized.
 * @param lang The language of the text.
 */
class EdgeTTSPlayer private constructor(
    private val project: Project?,
    text: String,
    lang: Lang
) : PlaybackController {

    private val source: EdgeTTSSource = EdgeTTSSource(text, lang).apply {
        onError(::showErrorNotification)
    }
    private val player: AudioPlayer = AudioPlayer(source).apply {
        setErrorHandler(::showErrorNotification)
    }

    override val statusBinding: Observable<PlaybackStatus> = player.statusBinding

    override fun start() {
        player.start()
    }

    override fun stop() {
        player.stop()
    }

    private fun showErrorNotification(error: Throwable) {
        if (project?.isDisposed != false) {
            return
        }

        val errorMessage = when (error) {
            is EdgeTTSException -> error.message ?: error.getCommonMessage()
            is IOException -> error.getCommonMessage()
            else -> error.message ?: message("error.unknown")
        }

        val message = message("tts.playback.failed", errorMessage)
        Notifications.showErrorNotification("Microsoft Edge TTS", message, project)
        thisLogger().w("Microsoft Edge TTS Error", error)
    }

    companion object {
        /**
         * Creates a new [EdgeTTSPlayer].
         *
         * @param project The project.
         * @param text The text to be synthesized.
         * @param lang The language of the text.
         */
        fun create(project: Project?, text: String, lang: Lang): EdgeTTSPlayer {
            return EdgeTTSPlayer(project, text, lang)
        }

        /**
         * Checks if the specified [language][lang] is supported by the Microsoft Edge TTS.
         */
        fun isSupportLanguage(lang: Lang): Boolean = EdgeTTSVoiceManager.isSupportLanguage(lang)
    }
}