package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.documentation.DocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.microsoft.models.presentableError
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.MICROSOFT
import com.intellij.openapi.components.service
import org.jsoup.nodes.Document
import javax.swing.Icon

/**
 * Microsoft translator.
 */
object MicrosoftTranslator : AbstractTranslator(), DocumentationTranslator {

    override val id: String = MICROSOFT.id
    override val name: String = MICROSOFT.translatorName
    override val icon: Icon = MICROSOFT.icon

    @Deprecated("""Use "RateLimiter" in the "translate" implementation.""")
    override val intervalLimit: Int = MICROSOFT.intervalLimit
    override val primaryLanguage: Lang get() = MICROSOFT.primaryLanguage
    override val supportedSourceLanguages: List<Lang> = MicrosoftLanguageAdapter.sourceLanguages
    override val supportedTargetLanguages: List<Lang> = MicrosoftLanguageAdapter.targetLanguages

    override val translationCacheToken: String
        get() = "keepOriginal=${service<MicrosoftTranslatorSettings>().keepOriginalDocumentation}"

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return service<MicrosoftTranslationService>().translate(text, srcLang, targetLang)
    }

    override fun translateDocumentation(
        documentation: Document,
        srcLang: Lang,
        targetLang: Lang
    ): Document = checkError {
        service<MicrosoftTranslationService>().translateDocumentation(documentation, srcLang, targetLang)
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        // https://learn.microsoft.com/zh-cn/azure/cognitive-services/translator/reference/v3-0-reference#errors
        return when (throwable) {
            is MicrosoftAuthenticationException -> ErrorInfo(throwable.message ?: "Authentication failed")
            is MicrosoftStatusException -> ErrorInfo(
                throwable.error?.presentableError ?: throwable.message ?: message("error.unknown")
            )

            else -> super.createErrorInfo(throwable)
        }
    }
}