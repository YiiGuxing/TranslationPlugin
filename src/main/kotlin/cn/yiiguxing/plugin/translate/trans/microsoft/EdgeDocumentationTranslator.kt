package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.isExplicit
import cn.yiiguxing.plugin.translate.trans.UnsupportedLanguageException
import cn.yiiguxing.plugin.translate.trans.documentation.DocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.documentation.HtmlDocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.microsoft.models.MicrosoftTranslation
import cn.yiiguxing.plugin.translate.util.Http.setUserAgent
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import kotlinx.coroutines.*
import org.jsoup.nodes.Document

/**
 * Documentation translator that uses the Microsoft Edge translation service.
 *
 * Unlike translating the whole HTML body at once, the body is traversed and only
 * the text content of translatable elements is translated, keeping the document
 * structure, styles and links intact. See [HtmlDocumentationTranslator].
 *
 * @property scope the [CoroutineScope] used to run background translation requests.
 * @property keepOriginal Provides whether to keep the original text after translation.
 */
internal class EdgeDocumentationTranslator(
    private val scope: CoroutineScope,
    private val keepOriginal: () -> Boolean
) : DocumentationTranslator {

    companion object {
        private const val TRANSLATION_URL = "https://edge.microsoft.com/translate/translatetext"

        /** The maximum number of texts in a single translation request. */
        private const val MAX_TEXTS_PER_REQUEST = 100
    }

    @Suppress("unused")
    constructor(scope: CoroutineScope, keepOriginal: Boolean = false) : this(scope, { keepOriginal })

    override fun translateDocumentation(
        documentation: Document,
        srcLang: Lang,
        targetLang: Lang
    ): Document {
        checkTargetLanguage(targetLang)

        HtmlDocumentationTranslator { texts ->
            translateTextsInBatches(texts, srcLang, targetLang)
        }.translateDocument(documentation, keepOriginal())

        return documentation
    }

    private fun translateTextsInBatches(texts: List<String>, from: Lang, to: Lang): List<String> {
        val batches = texts.chunked(MAX_TEXTS_PER_REQUEST)
        if (batches.size <= 1) {
            return translateBatch(batches.firstOrNull().orEmpty(), from, to)
        }

        return runBlocking {
            batches.map { batch ->
                scope.async(Dispatchers.IO) { translateBatch(batch, from, to) }
            }.awaitAll().flatten()
        }
    }

    private fun translateBatch(batch: List<String>, from: Lang, to: Lang): List<String> {
        val translations = translateTexts(batch, from, to)
        return batch.indices.map { index ->
            translations.getOrNull(index)
                ?.translations
                ?.firstOrNull()
                ?.text
                .orEmpty()
        }
    }

    private fun translateTexts(texts: List<String>, from: Lang, to: Lang): List<MicrosoftTranslation> {
        checkTargetLanguage(to)
        return MicrosoftHttp
            .post<Array<out MicrosoftTranslation>>(
                url = getTranslationUrl(from, to),
                data = texts
            ) {
                setUserAgent()
            }
            ?.toList()
            ?: emptyList()
    }

    private fun checkTargetLanguage(lang: Lang) {
        if (!lang.isExplicit()) {
            throw UnsupportedLanguageException(
                lang,
                "Microsoft Edge Translator does not support automatic language detection for target language."
            )
        }
    }

    private fun getTranslationUrl(from: Lang, to: Lang): String {
        val urlBuilder = UrlBuilder(TRANSLATION_URL)
        if (from.isExplicit()) {
            urlBuilder.addQueryParameter("from", from.microsoftLanguageCode)
        }

        return urlBuilder
            .addQueryParameter("to", to.microsoftLanguageCode)
            .addQueryParameter("isEnterpriseClient", false.toString())
            .build()
    }
}
