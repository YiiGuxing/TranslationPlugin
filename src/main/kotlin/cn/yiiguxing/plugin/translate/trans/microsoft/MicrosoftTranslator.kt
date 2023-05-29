package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.microsoft.data.MicrosoftTranslation
import cn.yiiguxing.plugin.translate.trans.microsoft.data.TextType
import cn.yiiguxing.plugin.translate.trans.microsoft.data.presentableError
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.MICROSOFT
import cn.yiiguxing.plugin.translate.util.i
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import org.jsoup.nodes.Document
import javax.swing.Icon

/**
 * Microsoft translator.
 */
object MicrosoftTranslator : AbstractTranslator(), DocumentationTranslator {

    private val LOG: Logger = logger<MicrosoftTranslator>()

    override val id: String = MICROSOFT.id
    override val name: String = MICROSOFT.translatorName
    override val icon: Icon = MICROSOFT.icon
    override val intervalLimit: Int = MICROSOFT.intervalLimit
    override val contentLengthLimit: Int = MICROSOFT.contentLengthLimit
    override val primaryLanguage: Lang get() = MICROSOFT.primaryLanguage
    override val supportedSourceLanguages: List<Lang> = MicrosoftLanguageAdapter.supportedSourceLanguages
    override val supportedTargetLanguages: List<Lang> = MicrosoftLanguageAdapter.supportedTargetLanguages

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(
            this,
            { _, _, _ -> MicrosoftTranslatorService.translate(text, srcLang, targetLang, TextType.PLAIN) },
            ::parseTranslation
        ).execute(text, srcLang, targetLang)
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
            { _, _, _ -> MicrosoftTranslatorService.translate(documentation, srcLang, targetLang, TextType.HTML) },
            ::parseTranslation
        )
        client.updateCacheKey { it.update("DOCUMENTATION".toByteArray()) }
        return client.execute(documentation, srcLang, targetLang).translation ?: ""
    }

    private fun parseTranslation(translation: String, original: String, srcLang: Lang, targetLang: Lang): Translation {
        LOG.i("Translate result: $translation")

        val type = object : TypeToken<ArrayList<MicrosoftTranslation>>() {}.type
        return Gson().fromJson<ArrayList<MicrosoftTranslation>>(translation, type)
            .firstOrNull()
            ?.apply {
                this.sourceText = original
                this.sourceLang = srcLang
            }
            ?.toTranslation()
            ?: Translation(original, original, srcLang, targetLang, emptyList())
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        // https://learn.microsoft.com/zh-cn/azure/cognitive-services/translator/reference/v3-0-reference#errors
        when (throwable) {
            is MicrosoftAuthenticationException -> return ErrorInfo(throwable.message ?: "Authentication failed")
            is MicrosoftStatusException -> return if (throwable.error?.code == 400050) {
                onError(ContentLengthLimitException())
            } else {
                ErrorInfo(throwable.error?.presentableError ?: throwable.message ?: message("error.unknown"))
            }
        }
        return super.createErrorInfo(throwable)
    }


}