package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.isExplicit
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.toExplicit
import cn.yiiguxing.plugin.translate.trans.microsoft.models.DictionaryLookup
import cn.yiiguxing.plugin.translate.trans.microsoft.models.TextType
import cn.yiiguxing.plugin.translate.trans.microsoft.models.presentableError
import cn.yiiguxing.plugin.translate.trans.text.ExampleDocument
import cn.yiiguxing.plugin.translate.trans.text.NamedTranslationDocument
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.MICROSOFT
import cn.yiiguxing.plugin.translate.util.e
import com.intellij.openapi.diagnostic.thisLogger
import org.jsoup.nodes.Document
import javax.swing.Icon

/**
 * Microsoft translator.
 */
object MicrosoftTranslator : AbstractTranslator(), DocumentationTranslator {

    private const val ERROR_INVALID_LANGUAGE_PAIR = 400023

    override val id: String = MICROSOFT.id
    override val name: String = MICROSOFT.translatorName
    override val icon: Icon = MICROSOFT.icon
    override val intervalLimit: Int = MICROSOFT.intervalLimit
    override val contentLengthLimit: Int = MICROSOFT.contentLengthLimit
    override val primaryLanguage: Lang get() = MICROSOFT.primaryLanguage
    override val supportedSourceLanguages: List<Lang> = MicrosoftLanguageAdapter.sourceLanguages
    override val supportedTargetLanguages: List<Lang> = MicrosoftLanguageAdapter.targetLanguages

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        if (!targetLang.isExplicit()) {
            throw UnsupportedLanguageException(targetLang, "Unsupported target language: ${targetLang.langName}")
        }

        val msTranslation = MicrosoftTranslatorService.translate(text, srcLang, targetLang, TextType.PLAIN)
            ?: return Translation(text, text, srcLang.toExplicit(), targetLang)

        val translation = msTranslation.translations.first()
        val sourceLang = msTranslation.detectedLanguage?.language
            ?.let { Lang.fromMicrosoftLanguageCode(it) }
            ?: srcLang.toExplicit()

        val dictionaryLookup = getDictionaryLookup(text, sourceLang, targetLang)
        val dictDocument = dictionaryLookup?.let(MicrosoftDictionaryDocumentFactory::getDocument)
        val exampleDocument = dictionaryLookup?.let { getExampleDocument(it, sourceLang, targetLang) }
        val extraDocuments = exampleDocument?.let { listOf(it) } ?: emptyList()

        return Translation(
            text,
            translation.text,
            sourceLang,
            Lang.fromMicrosoftLanguageCode(translation.to),
            dictDocument = dictDocument,
            extraDocuments = extraDocuments
        )
    }

    private fun getDictionaryLookup(text: String, sourceLang: Lang, targetLang: Lang): DictionaryLookup? {
        if (!sourceLang.isExplicit() ||
            sourceLang == targetLang ||
            !MicrosoftTranslatorService.canLookupDictionary(text)
        ) {
            return null
        }
        return try {
            MicrosoftTranslatorService.dictionaryLookup(text, sourceLang, targetLang)
        } catch (e: MicrosoftStatusException) {
            if (e.error?.code != ERROR_INVALID_LANGUAGE_PAIR) {
                thisLogger().e("Failed to lookup dictionary", e)
            }
            null
        }
    }

    private fun getExampleDocument(
        dictionaryLookup: DictionaryLookup,
        sourceLang: Lang,
        targetLang: Lang
    ): NamedTranslationDocument<ExampleDocument>? {
        val dictionaryExamples = MicrosoftTranslatorService.dictionaryExamples(dictionaryLookup, sourceLang, targetLang)
        return MicrosoftExampleDocumentFactory.getDocument(dictionaryExamples)?.let {
            NamedTranslationDocument(message("examples.document.name"), it)
        }
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
        return MicrosoftTranslatorService.translate(documentation, srcLang, targetLang, TextType.HTML)
            ?.translations
            ?.firstOrNull()
            ?.text
            ?: documentation
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