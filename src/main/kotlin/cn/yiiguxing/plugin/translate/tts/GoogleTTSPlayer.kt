package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.google.googleApiUrl
import cn.yiiguxing.plugin.translate.trans.google.googleReferer
import cn.yiiguxing.plugin.translate.trans.google.tk
import cn.yiiguxing.plugin.translate.tts.sound.AudioPlayer
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackController
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackStatus
import cn.yiiguxing.plugin.translate.tts.sound.source.DefaultPlaybackSource
import cn.yiiguxing.plugin.translate.tts.sound.source.PlaybackLoader
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.util.Http.setUserAgent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import java.io.IOException


/**
 * Google TTS player.
 */
class GoogleTTSPlayer private constructor(
    private val project: Project?,
    private val text: String,
    private val lang: Lang
) : PlaybackController {

    private val player: AudioPlayer = AudioPlayer(DefaultPlaybackSource(Loader())).apply {
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

        when (error) {
            is IOException -> Notifications.showErrorNotification("Google TTS", error.getCommonMessage(), project)
            else -> LOGGER.e("Google TTS Error", error)
        }
    }

    private inner class Loader : PlaybackLoader() {
        private val sentences = text.splitSentence(MAX_TEXT_LENGTH)

        private var index = 0

        private val indicator = EmptyProgressIndicator()

        override fun hasNext(): Boolean = index < sentences.size

        override fun onLoad(): ByteArray {
            val url = getTtsUrl(sentences[index], lang, index++, sentences.size)
            return HttpRequests.request(url)
                .setUserAgent()
                .googleReferer()
                .readBytes(indicator)
        }

        override fun onError(error: Throwable) {
            showErrorNotification(error)
        }

        override fun onCanceled() {
            indicator.cancel()
        }
    }

    companion object {
        private const val TTS_API_PATH = "/translate_tts"

        private val LOGGER = Logger.getInstance(GoogleTTSPlayer::class.java)

        private const val MAX_TEXT_LENGTH = 200

        private val SUPPORTED_LANGUAGES: List<Lang> = listOf(
            Lang.CHINESE_SIMPLIFIED, Lang.ENGLISH, Lang.CHINESE_TRADITIONAL, Lang.ALBANIAN, Lang.ARABIC, Lang.ESTONIAN,
            Lang.ICELANDIC, Lang.POLISH, Lang.BOSNIAN, Lang.AFRIKAANS, Lang.DANISH, Lang.GERMAN, Lang.RUSSIAN,
            Lang.FRENCH, Lang.FINNISH, Lang.KHMER, Lang.KOREAN, Lang.DUTCH, Lang.CATALAN, Lang.CZECH, Lang.CROATIAN,
            Lang.LATIN, Lang.LATVIAN, Lang.ROMANIAN, Lang.MACEDONIAN, Lang.BENGALI, Lang.NEPALI, Lang.NORWEGIAN,
            Lang.PORTUGUESE, Lang.JAPANESE, Lang.SWEDISH, Lang.SERBIAN, Lang.ESPERANTO, Lang.SLOVAK, Lang.SWAHILI,
            Lang.TAMIL, Lang.THAI, Lang.TURKISH, Lang.WELSH, Lang.UKRAINIAN, Lang.SPANISH, Lang.GREEK,
            Lang.HUNGARIAN, Lang.ARMENIAN, Lang.ITALIAN, Lang.HINDI, Lang.SUNDANESE, Lang.INDONESIAN,
            Lang.JAVANESE, Lang.VIETNAMESE
        )

        fun isSupportLanguage(lang: Lang): Boolean = SUPPORTED_LANGUAGES.contains(lang)

        fun create(project: Project?, text: String, lang: Lang): GoogleTTSPlayer {
            return GoogleTTSPlayer(project, text, lang)
        }

        private fun getTtsUrl(sentence: String, lang: Lang, index: Int, total: Int): String {
            val ttsUrl = googleApiUrl(TTS_API_PATH)
            @Suppress("SpellCheckingInspection")
            return UrlBuilder(ttsUrl)
                .addQueryParameter("client", "gtx")
                .addQueryParameter("ie", "UTF-8")
                .addQueryParameter("tl", lang.code)
                .addQueryParameter("total", total.toString())
                .addQueryParameter("idx", index.toString())
                .addQueryParameter("textlen", sentence.length.toString())
                .addQueryParameter("tk", sentence.tk())
                .addQueryParameter("q", sentence)
                .build()
        }
    }
}