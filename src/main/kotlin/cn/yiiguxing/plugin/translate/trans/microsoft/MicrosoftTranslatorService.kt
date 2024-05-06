package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.microsoft.models.DictionaryLookup
import cn.yiiguxing.plugin.translate.trans.microsoft.models.MicrosoftTranslation
import cn.yiiguxing.plugin.translate.trans.microsoft.models.SourceText
import cn.yiiguxing.plugin.translate.trans.microsoft.models.TextType
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread


/**
 * Service for the Microsoft Translator API.
 */
internal object MicrosoftTranslatorService {

    private const val API_BASE_URL = "https://api.cognitive.microsofttranslator.com"
    private const val TRANSLATE_URL = "$API_BASE_URL/translate"
    private const val DICTIONARY_LOOKUP_URL = "$API_BASE_URL/dictionary/lookup"

    /**
     * Translates the [text] from [from] language to [to] language.
     *
     * @param text The text to be translated.
     * @param from The source language.
     * @param to The target language.
     * @param textType The type of the text, [PLAIN][TextType.PLAIN] (Default) or [HTML][TextType.HTML].
     */
    @RequiresBackgroundThread
    fun translate(text: String, from: Lang, to: Lang, textType: TextType = TextType.PLAIN): MicrosoftTranslation? {
        val translateUrl = requestUrl(TRANSLATE_URL) {
            if (from != Lang.AUTO) {
                addQueryParameter("from", from.microsoftLanguageCode)
            }
            addQueryParameter("to", to.microsoftLanguageCode)
            addQueryParameter("textType", textType.value)
        }

        val accessToken = MicrosoftEdgeAuthService.service().getAccessToken()
        return MicrosoftHttp.post<Array<out MicrosoftTranslation>>(
            translateUrl,
            accessToken,
            listOf(SourceText(text))
        )
            .firstOrNull()
            ?.apply {
                sourceText = SourceText(text)
                sourceLang = from
            }
    }

    /**
     * Looks up the dictionary for the [text] from [from] language to [to] language.
     * Provides alternative translations for a word and a few idiomatic phrases.
     */
    @RequiresBackgroundThread
    fun dictionaryLookup(text: String, from: Lang, to: Lang): DictionaryLookup {
        val lookupUrl = requestUrl(DICTIONARY_LOOKUP_URL) {
            addQueryParameter("from", from.microsoftLanguageCode)
            addQueryParameter("to", to.microsoftLanguageCode)
        }

        val accessToken = MicrosoftEdgeAuthService.service().getAccessToken()
        return MicrosoftHttp.post<Array<out DictionaryLookup>>(
            lookupUrl,
            accessToken,
            listOf(SourceText(text)),
        ).first()
    }

    private inline fun requestUrl(baseUrl: String, block: UrlBuilder.() -> Unit): String {
        return UrlBuilder(baseUrl)
            .addQueryParameter("api-version", "3.0")
            .apply(block)
            .build()
    }
}
