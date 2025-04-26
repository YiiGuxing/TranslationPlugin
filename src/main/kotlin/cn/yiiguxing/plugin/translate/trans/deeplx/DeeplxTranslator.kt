@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans.deeplx

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.DEEPL
import cn.yiiguxing.plugin.translate.util.i
import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import org.jsoup.nodes.Document
import javax.swing.Icon

/**
 * Deepl translator
 */
object DeeplxTranslator : AbstractTranslator(), DocumentationTranslator {

    private val logger: Logger = Logger.getInstance(DeeplxTranslator::class.java)

    override val id: String = TranslationEngine.DEEPLX.id

    override val name: String = TranslationEngine.DEEPLX.translatorName

    override val icon: Icon = TranslationEngine.DEEPLX.icon

    override val intervalLimit: Int = TranslationEngine.DEEPLX.intervalLimit

    override val contentLengthLimit: Int = TranslationEngine.DEEPLX.contentLengthLimit

    override val primaryLanguage: Lang
        get() = DEEPL.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = DeeplxSupportedLanguages.sourceLanguages

    override val supportedTargetLanguages: List<Lang> = DeeplxSupportedLanguages.targetLanguages

    private val settings = service<DeeplxSettings>()


    override fun checkConfiguration(force: Boolean): Boolean {
        if (force) {
            return DEEPL.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(
            this,
            { _, _, _ -> call(text, srcLang, targetLang, false) },
            DeeplxTranslator::parseTranslation
        ).execute(text, srcLang, targetLang)
    }

    private fun call(text: String, srcLang: Lang, targetLang: Lang, isDocument: Boolean): String {
        val apiEndpoint = settings.apiEndpoint ?: ""
        return DeeplxService(apiEndpoint).translate(text, srcLang, targetLang, isDocument)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun parseTranslation(translation: String, original: String, srcLang: Lang, targetLang: Lang): Translation {
        logger.i("Translate result: $translation")

        return Gson().fromJson(translation, DeeplxTranslations::class.java).apply {
            this.original = original
            this.targetLang = targetLang
            if (!isSuccessful) {
                throw TranslationResultException(code)
            }
        }.toTranslation()
    }

    override fun translateDocumentation(
        documentation: Document,
        srcLang: Lang,
        targetLang: Lang
    ): Document = checkError {
        documentation.translateBody { bodyHTML ->
            translateDocumentation(bodyHTML, srcLang, targetLang)
        }
    }

    private fun translateDocumentation(documentation: String, srcLang: Lang, targetLang: Lang): String {
        val client = SimpleTranslateClient(
            this,
            { _, _, _ -> call(documentation, srcLang, targetLang, true) },
            DeeplxTranslator::parseTranslation
        )
        client.updateCacheKey { it.update("DOCUMENTATION".toByteArray()) }
        return client.execute(documentation, srcLang, targetLang).translation ?: ""
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        // https://www.deepl.com/docs-api/api-access/error-handling/
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
            }
        }

        return super.createErrorInfo(throwable)
    }
}
