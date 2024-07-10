package cn.yiiguxing.plugin.translate.trans.libretranslate

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.LT
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.i
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import org.jsoup.nodes.Document
import javax.swing.Icon

/**
 * LibreTranslate translator
 */
object LtTranslator : AbstractTranslator(), DocumentationTranslator {

    private val EMPTY_RESPONSE_REGEX = "\\{\\s*}".toRegex()

    private val logger: Logger = Logger.getInstance(LtTranslator::class.java)

    override val id: String = LT.id

    override val name: String = LT.translatorName

    override val icon: Icon = LT.icon

    override val intervalLimit: Int = LT.intervalLimit

    override val contentLengthLimit: Int = LT.contentLengthLimit

    override val primaryLanguage: Lang
        get() = LT.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = LtLanguageAdapter.supportedSourceLanguages

    override val supportedTargetLanguages: List<Lang> = LtLanguageAdapter.supportedTargetLanguages

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || Settings.getInstance().ltTranslateSettings.apiEndpoint.isEmpty()) {
            return LT.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(
            this,
            { _, _, _ -> call(text, srcLang, targetLang, false) },
            LtTranslator::parseTranslation
        ).execute(text, srcLang, targetLang)
    }

    override fun translateDocumentation(
        documentation: Document,
        srcLang: Lang,
        targetLang: Lang
    ): Document = checkError {
        documentation.translateBody { bodyHTML ->
            checkContentLength(bodyHTML, contentLengthLimit)

            val client = SimpleTranslateClient(
                this,
                { _, _, _ -> call(bodyHTML, srcLang, targetLang, true) },
                LtTranslator::parseTranslation
            )
            val translation = with(client) {
                updateCacheKey { it.update("DOCUMENTATION".toByteArray()) }
                execute(bodyHTML, srcLang, targetLang)
            }

            translation.translation ?: ""
        }
    }

    /**
     * 序列化json模型
     */
    @Suppress("MemberVisibilityCanBePrivate")
    data class LtTranslationRequest(
        @SerializedName("q")
        val q: String,
        @SerializedName("source")
        val source: String,
        @SerializedName("target")
        val target: String,
        @SerializedName("format")
        val format: String = "text",
        @SerializedName("api_key")
        var apiKey: String = ""
    )

    private fun call(text: String, srcLang: Lang, targetLang: Lang, isDocumentation: Boolean): String {
        val format = if (isDocumentation) "html" else "text"
        val request = LtTranslationRequest(text, srcLang.ltLanguageCode, targetLang.ltLanguageCode, format)
        val settings = Settings.getInstance().ltTranslateSettings
        if (settings.isApiKeySet) {
            request.apiKey = settings.getApiKey()
        }
        return sendHttpRequest(request)
    }

    private fun sendHttpRequest(request: Any): String {
        val settings = Settings.getInstance().ltTranslateSettings
        val url = settings.apiEndpoint
        val body = Gson().toJson(request)
        val contentType = "application/json"

        return Http.post(url, contentType, body) {}
    }

    class LtTranslationResultException(code: Int, val errorMessage: String?) :
        TranslationResultException(code) {
        override fun getLocalizedMessage(): String {
            return "$message[$errorMessage]"
        }
    }

    private fun parseTranslation(
        translation: String,
        original: String,
        srcLang: Lang,
        targetLang: Lang
    ): Translation {
        logger.i("Translate result: $translation")

        // 空JSON对象：`{}`
        if (translation.isBlank() || translation.trim().matches(EMPTY_RESPONSE_REGEX)) {
            return Translation(original, original, srcLang, targetLang, listOf(srcLang))
        }

        return Gson().fromJson(translation, LtTranslation::class.java).apply {
            query = original
            src = srcLang
            target = targetLang
            if (!isSuccessful) {
                throw LtTranslationResultException(-1, error)
            }
        }.toTranslation()
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        val errorMessage = when (throwable) {
            is LtTranslationResultException -> when (throwable.code) {
                -1 -> message("error.systemError") + "[${throwable.code}] ${throwable.errorMessage}"
                else -> message("error.unknown") + "[${throwable.code}] ${throwable.errorMessage}"
            }

            else -> return super.createErrorInfo(throwable)
        }

        return ErrorInfo(errorMessage)
    }
}
