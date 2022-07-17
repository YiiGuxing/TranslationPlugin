@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans.deepl

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.DEEPL
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.i
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import io.netty.handler.codec.http.HttpResponseStatus
import org.jsoup.nodes.Document
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import javax.swing.Icon

/**
 * Deepl translator
 */
object DeeplTranslator : AbstractTranslator(), DocumentationTranslator {

    private const val DEEPL_FREE_TRANSLATE_API_URL = "https://api-free.deepl.com/v2/translate"
    private const val DEEPL_PRO_TRANSLATE_API_URL = "https://api.deepl.com/v2/translate"


    /** 通用版支持的语言列表 */
    private val SUPPORTED_LANGUAGES: List<Lang> = listOf(
        Lang.BULGARIAN,
        Lang.CHINESE,
        Lang.CZECH,
        Lang.DANISH,
        Lang.DUTCH,
        Lang.GERMAN,
        Lang.GREEK,
        Lang.ENGLISH,
        Lang.ESTONIAN,
        Lang.FINNISH,
        Lang.FRENCH,
        Lang.HUNGARIAN,
        Lang.ITALIAN,
        Lang.JAPANESE,
        Lang.LATVIAN,
        Lang.LITHUANIAN,
        Lang.POLISH,
        Lang.PORTUGUESE,
        Lang.ROMANIAN,
        Lang.RUSSIAN,
        Lang.SLOVAK,
        Lang.SLOVENIAN,
        Lang.SPANISH,
        Lang.SWEDISH,
    )

    private val SUPPORTED_TARGET_LANGUAGES: List<Lang> = listOf(
        Lang.BULGARIAN,
        Lang.CHINESE,
        Lang.CZECH,
        Lang.DANISH,
        Lang.DUTCH,
        Lang.GERMAN,
        Lang.GREEK,
        Lang.ENGLISH,
        Lang.ENGLISH_AMERICAN,
        Lang.ENGLISH_BRITISH,
        Lang.ESTONIAN,
        Lang.FINNISH,
        Lang.FRENCH,
        Lang.HUNGARIAN,
        Lang.ITALIAN,
        Lang.JAPANESE,
        Lang.LATVIAN,
        Lang.LITHUANIAN,
        Lang.POLISH,
        Lang.PORTUGUESE,
        Lang.PORTUGUESE_BRAZILIAN,
        Lang.PORTUGUESE_PORTUGUESE,
        Lang.ROMANIAN,
        Lang.RUSSIAN,
        Lang.SLOVAK,
        Lang.SLOVENIAN,
        Lang.SPANISH,
        Lang.SWEDISH,
    )

    private val logger: Logger = Logger.getInstance(DeeplTranslator::class.java)

    override val id: String = DEEPL.id

    override val name: String = DEEPL.translatorName

    override val icon: Icon = DEEPL.icon

    override val intervalLimit: Int = DEEPL.intervalLimit

    override val contentLengthLimit: Int = DEEPL.contentLengthLimit

    override val primaryLanguage: Lang
        get() = DEEPL.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES
        .toMutableList()
        .apply { add(0, Lang.AUTO) }
    override val supportedTargetLanguages: List<Lang> = SUPPORTED_TARGET_LANGUAGES

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || Settings.deeplTranslateSettings.let { it.appId.isEmpty() || it.getAppKey().isEmpty() }) {
            return DEEPL.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(
            this,
            { _, _, _ ->call(text, srcLang, targetLang, false) },
            DeeplTranslator::parseTranslation
        ).execute(text, srcLang, targetLang)
    }

    private fun call(text: String, srcLang: Lang, targetLang: Lang, isDocument: Boolean): String {
        val settings = Settings.deeplTranslateSettings
        val privateKey = settings.getAppKey()
        val isFree = privateKey.endsWith(":fx")
        val requestURL = if (isFree) DEEPL_FREE_TRANSLATE_API_URL else DEEPL_PRO_TRANSLATE_API_URL
        val postData: LinkedHashMap<String, String> = linkedMapOf(
            "auth_key" to privateKey,
            "target_lang" to targetLang.deeplLanguageCode,
            "text" to text
        )

        if (isDocument) {
            postData["tag_handling"] = "html"
        }

        if (srcLang !== Lang.AUTO) {
            postData["source_lang"] = srcLang.deeplLanguageCode
        }

        return Http.post(
            requestURL,
            postData
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun parseTranslation(translation: String, original: String, srcLang: Lang, targetLang: Lang): Translation {
        logger.i("Translate result: $translation")

        return Gson().fromJson(translation, DeeplTranslations::class.java).apply {
            this.original = original
            this.targetLang = targetLang
            if (!isSuccessful) {
                throw TranslationResultException(translations[0].code)
            }
        }.toTranslation()
    }

    override fun translateDocumentation(documentation: Document, srcLang: Lang, targetLang: Lang): Document {
        return checkError {
                processAndTranslateDocumentation(documentation) {
                translateDocumentation(it, srcLang, targetLang)
            }
        }
    }

    private fun processAndTranslateDocumentation(
        documentation: Document,
        translate: (String) -> String
    ): Document {
        val body = documentation.body()
        val content = body.html()
        if (content.isBlank()) {
            return documentation
        }

        val translation = translate(content)

        body.html(translation)

        return documentation
    }

    private fun translateDocumentation(documentation: String, srcLang: Lang, targetLang: Lang): String {
        val client = SimpleTranslateClient(
            this,
            { _, _, _ ->call(documentation, srcLang, targetLang, true) },
            DeeplTranslator::parseTranslation
        )
        client.updateCacheKey { it.update("DOCUMENTATION".toByteArray()) }
        return client.execute(documentation, srcLang, targetLang).translation ?: ""
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        val errorMessage = when (throwable) {
            is UnsupportedLanguageException -> message("error.unsupportedLanguage", throwable.lang.langName)
            is ConnectException, is UnknownHostException -> message("error.network.connection")
            is SocketException, is SSLHandshakeException -> message("error.network")
            is SocketTimeoutException -> message("error.network.timeout")
            is ContentLengthLimitException -> message("error.text.too.long")
            is HttpRequests.HttpStatusException -> when (throwable.statusCode) {
                HttpResponseStatus.TOO_MANY_REQUESTS.code() -> message("error.too.many.requests")
                HttpResponseStatus.FORBIDDEN.code() -> message("error.invalidAccount")
                HttpResponseStatus.BAD_REQUEST.code() -> message("error.bad.request")
                HttpResponseStatus.NOT_FOUND.code() -> message("error.request.not.found")
                HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE.code() -> message("error.text.too.long")
                HttpResponseStatus.REQUEST_URI_TOO_LONG.code() -> message("error.request.uri.too.long")
                HttpResponseStatus.TOO_MANY_REQUESTS.code() -> message("error.too.many.requests")
                HttpResponseStatus.SERVICE_UNAVAILABLE.code() -> message("error.service.unavailable")
                HttpResponseStatus.INTERNAL_SERVER_ERROR.code() -> message("error.systemError")
                456 -> message("error.access.limited") // Quota exceeded. The character limit has been reached.
                529 -> message("error.too.many.requests") // Too many requests. Please wait and resend your request.
                else -> HttpResponseStatus.valueOf(throwable.statusCode).reasonPhrase()
            }
            else -> return null
        }

        return ErrorInfo(errorMessage)
    }
}
