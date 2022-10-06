@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans.deepl

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.DEEPL
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.i
import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import org.jsoup.nodes.Document
import javax.swing.Icon

/**
 * Deepl translator
 */
object DeeplTranslator : AbstractTranslator(), DocumentationTranslator {

    private const val DEEPL_FREE_TRANSLATE_API_URL = "https://api-free.deepl.com/v2/translate"
    private const val DEEPL_PRO_TRANSLATE_API_URL = "https://api.deepl.com/v2/translate"


    private val logger: Logger = Logger.getInstance(DeeplTranslator::class.java)

    override val id: String = DEEPL.id

    override val name: String = DEEPL.translatorName

    override val icon: Icon = DEEPL.icon

    override val intervalLimit: Int = DEEPL.intervalLimit

    override val contentLengthLimit: Int = DEEPL.contentLengthLimit

    override val primaryLanguage: Lang
        get() = DEEPL.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = DeeplLanguageAdapter.supportedSourceLanguages

    override val supportedTargetLanguages: List<Lang> = DeeplLanguageAdapter.supportedTargetLanguages

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || !DeeplCredentials.instance.isAuthKeySet) {
            return DEEPL.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(
            this,
            { _, _, _ -> call(text, srcLang, targetLang, false) },
            DeeplTranslator::parseTranslation
        ).execute(text, srcLang, targetLang)
    }

    private fun call(text: String, srcLang: Lang, targetLang: Lang, isDocument: Boolean): String {
        val authKey = DeeplCredentials.instance.authKey ?: ""
        val isFreeApi = authKey.endsWith(":fx")
        val requestURL = if (isFreeApi) DEEPL_FREE_TRANSLATE_API_URL else DEEPL_PRO_TRANSLATE_API_URL
        val postData: LinkedHashMap<String, String> = linkedMapOf(
            "target_lang" to targetLang.deeplLanguageCode,
            "text" to text
        )

        if (srcLang !== Lang.AUTO) {
            postData["source_lang"] = srcLang.deeplLanguageCode
        }
        if (isDocument) {
            postData["tag_handling"] = "html"
        }

        return Http.post(requestURL, postData) {
            userAgent(Http.PLUGIN_USER_AGENT)
            // Authentication method should be header-based authentication,
            // auth-key will leak into the log file if it is authenticated as a parameter.
            tuner { it.setRequestProperty("Authorization", "DeepL-Auth-Key $authKey") }
        }
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
            { _, _, _ -> call(documentation, srcLang, targetLang, true) },
            DeeplTranslator::parseTranslation
        )
        client.updateCacheKey { it.update("DOCUMENTATION".toByteArray()) }
        return client.execute(documentation, srcLang, targetLang).translation ?: ""
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        if (throwable is HttpRequests.HttpStatusException) {
            when (throwable.statusCode) {
                403,
                456 -> {
                    val message = if (throwable.statusCode == 403) {
                        message("error.invalidAccount")
                    } else {
                        message("error.quota.exceeded")
                    }
                    val action = ErrorInfo.continueAction(
                        message("action.check.configuration"),
                        icon = AllIcons.General.Settings
                    ) {
                        DEEPL.showConfigurationDialog()
                    }
                    return ErrorInfo(message, action)
                }

                529 -> return ErrorInfo(message("error.too.many.requests"))
            }
        }

        return super.createErrorInfo(throwable)
    }
}
