package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.orUnknown
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.microsoft.models.DictionaryExample
import cn.yiiguxing.plugin.translate.trans.microsoft.models.DictionaryLookup
import cn.yiiguxing.plugin.translate.trans.microsoft.models.MicrosoftTranslation
import cn.yiiguxing.plugin.translate.trans.text.NamedTranslationDocument

/**
 * Factory for creating [Translation]s from Microsoft translator API responses.
 */
internal object MicrosoftTranslationFactory {

    /**
     * Creates a [Translation] from the given Microsoft translator API responses.
     *
     * @param text The original text.
     * @param srcLang The source language.
     * @param targetLang The target language.
     * @param msTranslation The translation response, or `null` if the translation failed.
     * @param dictionaryLookup The dictionary lookup response, or `null`.
     * @param dictionaryExamples The dictionary examples response, or `null`.
     */
    fun toTranslation(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        msTranslation: MicrosoftTranslation?,
        dictionaryLookup: DictionaryLookup? = null,
        dictionaryExamples: List<DictionaryExample>? = null
    ): Translation {
        if (msTranslation == null) {
            return Translation(text, text, srcLang.orUnknown(), targetLang)
        }

        val translation = msTranslation.translations.first()
        val sourceLang = detectedSourceLang(msTranslation, srcLang)

        val dictDocument = dictionaryLookup?.let(MicrosoftDictionaryDocumentFactory::getDocument)
        val exampleDocument = dictionaryExamples
            ?.let(MicrosoftExampleDocumentFactory::getDocument)
            ?.let { NamedTranslationDocument(message("examples.document.name"), it) }
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

    /**
     * Resolves the detected source language from the [msTranslation] response,
     * falling back to [srcLang] if no language is detected.
     */
    fun detectedSourceLang(msTranslation: MicrosoftTranslation, srcLang: Lang): Lang {
        return msTranslation.detectedLanguage?.language
            ?.let { Lang.fromMicrosoftLanguageCode(it) }
            ?: srcLang.orUnknown()
    }
}
