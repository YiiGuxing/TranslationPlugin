@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans.deepl

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.DEEPL
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

    private val logger: Logger = Logger.getInstance(DeeplTranslator::class.java)

    override val id: String = DEEPL.id

    override val name: String = DEEPL.translatorName

    override val icon: Icon = DEEPL.icon

    override val intervalLimit: Int = DEEPL.intervalLimit

    override val contentLengthLimit: Int = DEEPL.contentLengthLimit

    override val primaryLanguage: Lang
        get() = DEEPL.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = DeeplSupportedLanguages.sourceLanguages

    override val supportedTargetLanguages: List<Lang> = DeeplSupportedLanguages.targetLanguages

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || !DeeplCredential.isAuthKeySet) {
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
        val authKey = DeeplCredential.authKey ?: ""
        return DeeplService(authKey).translate(text, srcLang, targetLang, isDocument)
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
            DeeplTranslator::parseTranslation
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
