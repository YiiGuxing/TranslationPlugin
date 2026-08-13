package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.openapi.data.AsyncExpiringData
import cn.yiiguxing.plugin.translate.openapi.data.CachePolicy
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.isExplicit
import cn.yiiguxing.plugin.translate.trans.TextTranslator
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.UnsupportedLanguageException
import cn.yiiguxing.plugin.translate.trans.microsoft.BingTranslator.Companion.MAX_CHARS_PER_REQUEST
import cn.yiiguxing.plugin.translate.trans.microsoft.models.*
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Http.setUserAgent
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import cn.yiiguxing.plugin.translate.util.splitSentence
import cn.yiiguxing.plugin.translate.util.type
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Bing Translator.
 *
 * @property scope the [CoroutineScope] used to run background HTTP requests.
 */
internal class BingTranslator(
    private val scope: CoroutineScope
) : TextTranslator {

    companion object {
        private const val BING_ORIGIN = "https://www.bing.com"
        private const val BING_TRANSLATOR_URL = "$BING_ORIGIN/translator"
        private const val BING_TRANSLATOR_API_URL = "$BING_ORIGIN/ttranslatev3"
        private const val BING_SPELLCHECK_API_URL = "$BING_ORIGIN/tspellcheckv3"
        private const val BING_DICTIONARY_LOOKUP_API_URL = "$BING_ORIGIN/tlookupv3"
        private const val BING_EXAMPLE_API_URL = "$BING_ORIGIN/texamplev3"

        /** Matches runs of whitespace characters (including full-width spaces). */
        private val WHITESPACE_REGEX = Regex("[ \\u3000\\n\\r\\t\\s]+")

        // params_AbusePreventionHelper = [<key>,"<token>",<ttl>]
        private val ABUSE_PREVENTION_REGEX =
            """params_AbusePreventionHelper\s*=\s*\[\s*(\d+)\s*,\s*"([^"]+)"\s*,\s*(\d+)\s*]""".toRegex()
        private val IG_REGEX = """IG\s*:\s*"([A-Fa-f0-9]+)"""".toRegex()
        private val IID_REGEX = """data-iid\s*=\s*"([^"]+)"""".toRegex()

        /** The safety margin subtracted from the server-provided TTL before caching. */
        private const val EXPIRY_SAFETY_MARGIN_MS = 60_000L // 1 minute

        /** The maximum number of characters translated in a single request. */
        private const val MAX_CHARS_PER_REQUEST = 1000

        /** The maximum length of the text that can be looked up in the dictionary. */
        private const val MAX_DICT_INPUT_TEXT_LENGTH = 50

        /** The maximum length of the text that can be sent to the spell checker. */
        private const val MAX_SPELLCHECK_INPUT_TEXT_LENGTH = 50
    }

    /**
     * Cached authentication data used by the Bing endpoints. The token is fetched
     * from the Bing Translator page and cached until it expires (with a safety margin).
     */
    private val authService: AsyncExpiringData<BingAuthentication> =
        object : AsyncExpiringData<BingAuthentication>(scope) {
            override suspend fun load(): BingAuthentication {
                val html = withContext(Dispatchers.IO) {
                    HttpRequests.request(BING_TRANSLATOR_URL).setUserAgent().readString()
                }
                return parseBingAuthentication(html)
            }

            override fun cachePolicy(value: BingAuthentication): CachePolicy {
                return CachePolicy.ExpireAfter(value.ttl)
            }
        }


    /**
     * Parses the authentication data (AbusePreventionHelper, IG and IID) from the
     * HTML source of the Bing Translator page.
     *
     * @param html the raw HTML of the Bing Translator page.
     * @return the parsed [BingAuthentication] data.
     * @throws IllegalStateException if any of the required tokens cannot be found.
     */
    private fun parseBingAuthentication(html: String): BingAuthentication {
        val matchResult = ABUSE_PREVENTION_REGEX.find(html)
        requireNotNull(matchResult) { "Failed to parse AbusePreventionHelper from Bing Translator page" }
        val ig = IG_REGEX.find(html)?.groupValues?.getOrNull(1)
        requireNotNull(ig) { "Failed to parse IG from Bing Translator page" }
        val iid = IID_REGEX.find(html)?.groupValues?.getOrNull(1)
        requireNotNull(iid) { "Failed to parse IID from Bing Translator page" }

        val (key, token, ttlMs) = matchResult.destructured
        val ttl = ttlMs.toLong().milliseconds - EXPIRY_SAFETY_MARGIN_MS.milliseconds
        return BingAuthentication(
            ig = ig,
            iid = iid,
            key = key,
            token = token,
            ttl = maxOf(ttl, 0.milliseconds)
        )
    }

    /**
     * Verifies that the given [lang] has an explicit (non-auto) language; throws an
     * [UnsupportedLanguageException] otherwise.
     *
     * @param lang the language to check.
     */
    private fun checkExplicitLanguage(lang: Lang) {
        if (!lang.isExplicit()) {
            throw UnsupportedLanguageException(lang, "Unsupported language: ${lang.localeName}")
        }
    }

    /**
     * Builds the request URL for the given [baseUrl] by appending the required
     * `isVertical`, `IG` and `IID` query parameters.
     *
     * @param baseUrl the API endpoint URL.
     * @param auth the authentication data holding the `IG` and `IID` values.
     * @return the fully qualified request URL.
     */
    private fun requestUrl(baseUrl: String, auth: BingAuthentication): String {
        return UrlBuilder(baseUrl)
            .addQueryParameter("isVertical", "1")
            .addQueryParameter("IG", auth.ig)
            .addQueryParameter("IID", auth.iid)
            .build()
    }

    /**
     * Builds the URL-encoded form data for a request by merging the given [params]
     * with the `token` and `key` from [auth].
     *
     * @param auth the authentication data holding the `token` and `key` values.
     * @param params the request-specific form parameters.
     * @return the URL-encoded form body.
     */
    private fun requestForm(auth: BingAuthentication, vararg params: Pair<String, String>): String {
        return Http.getFormUrlEncoded(
            params.toMap() + mapOf(
                "token" to auth.token,
                "key" to auth.key
            )
        )
    }

    /**
     * Translates the given [text] from [from] to [to]. The text is split into
     * chunks that fit within [MAX_CHARS_PER_REQUEST], translated concurrently,
     * and the results are merged back together.
     *
     * @param text the text to translate.
     * @param from the source language.
     * @param to the target language.
     * @return the merged translation result, or `null` if [text] is empty.
     * @throws IllegalStateException if any of the text chunks fail to translate.
     */
    suspend fun translateText(text: String, from: Lang, to: Lang): MicrosoftTranslation? {
        val chunks = text.splitSentence(MAX_CHARS_PER_REQUEST)
        if (chunks.isEmpty()) {
            return null
        }

        val results = coroutineScope {
            chunks.map { chunk ->
                async { translateTextInner(chunk, from, to) }
            }.awaitAll()
        }

        val failedCount = results.count { it == null }
        if (failedCount > 0) {
            throw IllegalStateException("Failed to translate $failedCount of ${results.size} text chunks")
        }
        val translations = results.mapNotNull { it }
        val detectedLanguage = translations.firstNotNullOfOrNull { it.detectedLanguage }
        val boundarySpaces = computeBoundarySpaces(text, chunks)
        val mergedItems = translations
            .flatMap { it.translations }
            .groupBy { it.to }
            .map { (to, items) ->
                val mergedText = buildString {
                    items.forEachIndexed { index, item ->
                        if (index > 0 && boundarySpaces[index - 1]) {
                            append(' ')
                        }
                        append(item.text)
                    }
                }
                TranslationItem(to, mergedText)
            }

        return MicrosoftTranslation(
            sourceText = SourceText(text),
            detectedLanguage = detectedLanguage,
            translations = mergedItems
        )
    }

    /**
     * Determines, for each pair of adjacent chunks, whether they were separated
     * by a whitespace in the original [text].
     *
     * [chunks] must be the result of [splitSentence] applied to [text], which
     * collapses whitespace runs to a single space and trims each chunk, so the
     * separator is not recoverable from the chunks themselves.
     */
    private fun computeBoundarySpaces(text: String, chunks: List<String>): BooleanArray {
        if (chunks.size < 2) {
            return BooleanArray(0)
        }

        val optimized = text.replace(WHITESPACE_REGEX, " ")
        val spaces = BooleanArray(chunks.size - 1)
        var start = 0
        for (i in 1 until chunks.size) {
            val index = optimized.indexOf(chunks[i], startIndex = start)
                .takeIf { it >= 0 }
                ?: continue
            spaces[i - 1] = index > 0 && optimized[index - 1] == ' '
            start = index + chunks[i].length
        }
        return spaces
    }

    /**
     * Performs the actual HTTP translation of a single text chunk.
     *
     * @param text the chunk of text to translate.
     * @param from the source language.
     * @param to the target language; must be explicit.
     * @return the translation result, or `null` if the server returns no data.
     */
    private suspend fun translateTextInner(text: String, from: Lang, to: Lang): MicrosoftTranslation? {
        checkExplicitLanguage(to)

        val auth = authService.get()
        val translateUrl = requestUrl(BING_TRANSLATOR_API_URL, auth)
        val formData = requestForm(
            auth,
            "text" to text,
            "fromLang" to (if (from.isExplicit()) from.microsoftLanguageCode else "auto-detect"),
            "to" to to.microsoftLanguageCode,
        )

        return withContext(Dispatchers.IO) {
            MicrosoftHttp.post<Array<out MicrosoftTranslation>>(
                url = translateUrl,
                contentType = Http.MIME_TYPE_FORM,
                data = formData
            )?.firstOrNull()
        }
    }

    /**
     * Checks the spelling of the given [text] in the given [lang].
     *
     * @param text the text to spell check.
     * @param lang the language of the text; must be explicit.
     * @return the corrected text, or `null` if the text is too long, the language is
     *         not explicit, or the server returns no correction.
     */
    suspend fun checkSpelling(text: String, lang: Lang): String? {
        if (text.length > MAX_SPELLCHECK_INPUT_TEXT_LENGTH || !lang.isExplicit()) {
            return null
        }

        val auth = authService.get()
        val spellCheckUrl = requestUrl(BING_SPELLCHECK_API_URL, auth)
        val formData = requestForm(
            auth,
            "text" to text,
            "fromLang" to lang.microsoftLanguageCode,
        )

        return withContext(Dispatchers.IO) {
            MicrosoftHttp.post<SpellCheckResult>(
                url = spellCheckUrl,
                contentType = Http.MIME_TYPE_FORM,
                data = formData
            )?.correctedText
        }
    }

    /**
     * Determines whether the [text] can be looked up in the dictionary.
     */
    private fun canLookupDictionary(text: String): Boolean {
        return text.length <= MAX_DICT_INPUT_TEXT_LENGTH && text.any { !it.isWhitespace() }
    }

    /**
     * Looks up dictionary entries for the given [text] and its [translatedText].
     *
     * @param text the source word/phrase to look up.
     * @param translatedText the translation of [text].
     * @param from the source language; must be explicit.
     * @param to the target language; must be explicit.
     * @return the first matching [DictionaryLookup], or `null` if the lookup is not
     *         possible or the server returns no data.
     */
    suspend fun lookupDictionary(
        text: String,
        translatedText: String,
        from: Lang,
        to: Lang
    ): DictionaryLookup? {
        if (!canLookupDictionary(text) || !from.isExplicit() || !to.isExplicit()) {
            return null
        }

        val auth = authService.get()
        val lookupUrl = requestUrl(BING_DICTIONARY_LOOKUP_API_URL, auth)

        @Suppress("SpellCheckingInspection")
        val formData = requestForm(
            auth,
            "from" to from.microsoftLanguageCode,
            "to" to to.microsoftLanguageCode,
            "text" to text,
            "translatedtext" to translatedText
        )

        return withContext(Dispatchers.IO) {
            MicrosoftHttp.post<List<DictionaryLookup>>(
                url = lookupUrl,
                contentType = Http.MIME_TYPE_FORM,
                data = formData,
                typeOfT = type<List<DictionaryLookup>>()
            )?.firstOrNull()
        }
    }

    /**
     * Looks up example sentences for the given [text] and its [translation].
     *
     * @param text the source word/phrase to look up.
     * @param translation the translation of [text].
     * @param from the source language; must be explicit.
     * @param to the target language; must be explicit.
     * @return the list of matching [DictionaryExample]s, or `null` if the lookup is
     *         not possible or the server returns no data.
     */
    suspend fun lookupExample(
        text: String,
        translation: String,
        from: Lang,
        to: Lang
    ): List<DictionaryExample>? {
        if (!canLookupDictionary(text) || !from.isExplicit() || !to.isExplicit()) {
            return null
        }

        val auth = authService.get()
        val exampleUrl = requestUrl(BING_EXAMPLE_API_URL, auth)
        val formData = requestForm(
            auth,
            "from" to from.microsoftLanguageCode,
            "to" to to.microsoftLanguageCode,
            "text" to text,
            "translation" to translation
        )

        return withContext(Dispatchers.IO) {
            MicrosoftHttp.post<List<DictionaryExample>>(
                url = exampleUrl,
                contentType = Http.MIME_TYPE_FORM,
                data = formData,
                typeOfT = type<List<DictionaryExample>>()
            )
        }
    }

    @RequiresBackgroundThread
    override fun translate(
        text: String,
        srcLang: Lang,
        targetLang: Lang
    ): Translation {
        checkExplicitLanguage(targetLang)

        return runBlocking {
            val msTranslation = translateText(text, srcLang, targetLang)
            val sourceLang = msTranslation?.let {
                MicrosoftTranslationFactory.detectedSourceLang(it, srcLang)
            } ?: srcLang
            val translatedText = msTranslation?.translations?.firstOrNull()?.text

            // Query spelling check, dictionary lookup and example lookup in parallel.
            val spellDeferred = async {
                runCatching { checkSpelling(text, sourceLang) }.getOrNull()
            }
            val dictionaryLookupDeferred = async {
                translatedText?.let {
                    runCatching { lookupDictionary(text, it, sourceLang, targetLang) }.getOrNull()
                }
            }
            val dictionaryExamplesDeferred = async {
                translatedText?.let {
                    runCatching { lookupExample(text, it, sourceLang, targetLang) }.getOrNull()
                }
            }

            MicrosoftTranslationFactory.toTranslation(
                text,
                srcLang,
                targetLang,
                msTranslation,
                spell = spellDeferred.await(),
                dictionaryLookup = dictionaryLookupDeferred.await(),
                dictionaryExamples = dictionaryExamplesDeferred.await()
            )
        }
    }
}