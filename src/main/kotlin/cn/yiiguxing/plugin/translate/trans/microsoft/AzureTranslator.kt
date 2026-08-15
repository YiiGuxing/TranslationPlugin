package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.isExplicit
import cn.yiiguxing.plugin.translate.trans.microsoft.models.*
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import cn.yiiguxing.plugin.translate.util.type
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.jsoup.nodes.Document

/**
 * Azure Translator.
 */
@Suppress("unused")
internal class AzureTranslator(
    private val authentication: () -> AzureAuthentication
) : TextTranslator, DocumentationTranslator {

    companion object {
        private const val API_BASE_URL = "https://api.cognitive.microsofttranslator.com"
        private const val TRANSLATE_API_URL = "$API_BASE_URL/translate"
        private const val DICTIONARY_LOOKUP_API_URL = "$API_BASE_URL/dictionary/lookup"
        private const val DICTIONARY_EXAMPLES_API_URL = "$API_BASE_URL/dictionary/examples"

        /** The maximum length of the text that can be looked up in the dictionary. */
        private const val MAX_DICT_INPUT_TEXT_LENGTH = 50

        /** The maximum number of items that can be looked up in the dictionary examples. */
        private const val MAX_DICT_EXAMPLE_INPUT_ITEM_COUNT = 10
    }


    constructor(authentication: AzureAuthentication) : this({ authentication })


    /**
     * Translates the [text] from [from] language to [to] language.
     *
     * [Documentation](https://learn.microsoft.com/en-us/azure/ai-services/translator/reference/v3-0-translate)
     *
     * @param text The text to be translated.
     * @param from The source language.
     * @param to The target language.
     * @param textType The type of the text, [PLAIN][TextType.PLAIN] (Default) or [HTML][TextType.HTML].
     */
    @RequiresBackgroundThread
    fun translate(text: String, from: Lang, to: Lang, textType: TextType = TextType.PLAIN): MicrosoftTranslation? {
        checkTargetLanguage(to)

        val translateUrl = requestUrl(TRANSLATE_API_URL) {
            if (from != Lang.AUTO) {
                addQueryParameter("from", from.microsoftLanguageCode)
            }
            addQueryParameter("to", to.microsoftLanguageCode)
            addQueryParameter("textType", textType.value)
        }

        return MicrosoftHttp
            .post<Array<out MicrosoftTranslation>>(
                translateUrl,
                authentication(),
                listOf(InputText(text))
            )
            ?.firstOrNull()
            ?.apply {
                if (sourceText == null) {
                    sourceText = SourceText(text)
                }
            }
    }

    /**
     * Determines whether the [text] can be looked up in the dictionary.
     */
    fun canLookupDictionary(text: String): Boolean {
        return text.length <= MAX_DICT_INPUT_TEXT_LENGTH && text.any { !it.isWhitespace() }
    }

    /**
     * Looks up the dictionary for the [text] from [from] language to [to] language.
     * Provides alternative translations for a word and a few idiomatic phrases.
     *
     * [Documentation](https://learn.microsoft.com/en-us/azure/ai-services/translator/reference/v3-0-dictionary-lookup)
     */
    @RequiresBackgroundThread
    fun dictionaryLookup(text: String, from: Lang, to: Lang): DictionaryLookup? {
        checkTargetLanguage(to)

        val lookupUrl = requestUrl(DICTIONARY_LOOKUP_API_URL) {
            addQueryParameter("from", from.microsoftLanguageCode)
            addQueryParameter("to", to.microsoftLanguageCode)
        }

        return MicrosoftHttp.post<Array<out DictionaryLookup>>(
            lookupUrl,
            authentication(),
            arrayOf(InputText(text)),
        )?.first()
    }

    /**
     * Looks up the dictionary examples.
     * Provides examples that show how terms in the dictionary are used in context.
     * This operation is used in tandem with [Dictionary lookup][dictionaryLookup].
     *
     * [Documentation](https://learn.microsoft.com/en-us/azure/ai-services/translator/reference/v3-0-dictionary-examples)
     */
    @RequiresBackgroundThread
    fun dictionaryExamples(dictionaryLookup: DictionaryLookup, from: Lang, to: Lang): List<DictionaryExample>? {
        checkTargetLanguage(to)

        val request = dictionaryLookup.translations
            .asSequence()
            .sortedByDescending { it.confidence }
            .filter { it.normalizedTarget.length <= MAX_DICT_INPUT_TEXT_LENGTH }
            .take(MAX_DICT_EXAMPLE_INPUT_ITEM_COUNT)
            .map {
                DictionaryExampleInputText(dictionaryLookup.normalizedSource, it.normalizedTarget)
            }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?: return emptyList()

        val lookupUrl = requestUrl(DICTIONARY_EXAMPLES_API_URL) {
            addQueryParameter("from", from.microsoftLanguageCode)
            addQueryParameter("to", to.microsoftLanguageCode)
        }

        return MicrosoftHttp.post(
            lookupUrl,
            authentication(),
            request,
            typeOfT = type<List<DictionaryExample>>()
        )
    }

    private fun checkTargetLanguage(targetLang: Lang) {
        if (!targetLang.isExplicit()) {
            throw UnsupportedLanguageException(targetLang, "Unsupported target language: ${targetLang.localeName}")
        }
    }

    private inline fun requestUrl(baseUrl: String, block: UrlBuilder.() -> Unit): String {
        return UrlBuilder(baseUrl)
            .addQueryParameter("api-version", "3.0")
            .apply(block)
            .build()
    }

    override fun translate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        checkTargetLanguage(targetLang)

        val msTranslation = translate(text, srcLang, targetLang, TextType.PLAIN)

        val (dictionaryLookup, dictionaryExamples) = msTranslation?.let { mt ->
            val sourceLang = MicrosoftTranslationFactory.detectedSourceLang(mt, srcLang)
            val lookup = getDictionaryLookup(text, sourceLang, targetLang)
            lookup to lookup?.let { getDictionaryExamples(it, sourceLang, targetLang) }
        } ?: (null to null)

        return MicrosoftTranslationFactory.toTranslation(
            text,
            srcLang,
            targetLang,
            msTranslation,
            dictionaryLookup,
            dictionaryExamples
        )
    }

    private fun getDictionaryLookup(text: String, sourceLang: Lang, targetLang: Lang): DictionaryLookup? {
        if (!sourceLang.isExplicit() || sourceLang == targetLang || !canLookupDictionary(text)) {
            return null
        }

        return try {
            dictionaryLookup(text, sourceLang, targetLang)
        } catch (e: Exception) {
            thisLogger().warn("Failed to lookup dictionary", e)
            null
        }
    }

    private fun getDictionaryExamples(
        dictionaryLookup: DictionaryLookup,
        sourceLang: Lang,
        targetLang: Lang
    ): List<DictionaryExample>? {
        return try {
            dictionaryExamples(dictionaryLookup, sourceLang, targetLang)
        } catch (e: Exception) {
            thisLogger().warn("Failed to get dictionary examples", e)
            null
        }
    }

    override fun translateDocumentation(
        documentation: Document,
        srcLang: Lang,
        targetLang: Lang
    ): Document = documentation.translateBody { bodyHTML ->
        translate(bodyHTML, srcLang, targetLang, TextType.HTML)
            ?.translations
            ?.firstOrNull()
            ?.text
            ?: bodyHTML
    }
}