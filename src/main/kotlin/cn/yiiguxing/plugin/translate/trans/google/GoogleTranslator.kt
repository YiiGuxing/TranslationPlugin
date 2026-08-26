package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.documentation.DocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.documentation.HtmlDocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.documentation.RawHtmlTranslationStrategy
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.GOOGLE
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.util.Http.setUserAgent
import com.google.gson.*
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import org.jsoup.nodes.Document
import java.lang.reflect.Type
import java.net.HttpRetryException
import javax.swing.Icon

/**
 * GoogleTranslator
 */
object GoogleTranslator : AbstractTranslator(), DocumentationTranslator {

    private const val TRANSLATE_API_PATH = "/v1/translate"
    private const val DOCUMENTATION_TRANSLATION_API_PATH = "/v1/translateHtml"

    private val logger: Logger = Logger.getInstance(GoogleTranslator::class.java)

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Lang::class.java, LangDeserializer)
        .registerTypeAdapter(GDocTranslations::class.java, GDocTranslationsDeserializer)
        .create()

    override val id: String = GOOGLE.id

    override val name: String = GOOGLE.translatorName

    override val icon: Icon = GOOGLE.icon

    @Deprecated("""Use "RateLimiter" in the "translate" implementation.""")
    override val intervalLimit: Int = GOOGLE.intervalLimit

    override val primaryLanguage: Lang
        get() = GOOGLE.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = GoogleLanguageAdapter.sourceLanguages

    override val supportedTargetLanguages: List<Lang> = GoogleLanguageAdapter.targetLanguages

    override fun translationCacheToken(cacheType: TranslationCacheType): String? = when (cacheType) {
        TranslationCacheType.TEXT -> null
        TranslationCacheType.DOCUMENTATION ->
            "keepOriginalDocumentation=${service<GoogleSettings>().keepOriginalDocumentation}"
    }


    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        val client = SimpleTranslateClient(
            this,
            ::callTranslate,
            ::parseTranslation
        )
        client.updateCacheKey { it.update("v2".toByteArray()) }
        return client.execute(text, srcLang, targetLang)
    }

    private fun callTranslate(text: String, srcLang: Lang, targetLang: Lang): String {
        val urlBuilder = UrlBuilder(googleTranslateApiUrl(TRANSLATE_API_PATH))
            .addQueryParameter("params.client", GoogleTranslateClient.GTX.value)
            .addQueryParameter("query.text", text)
            .addQueryParameter("query.source_language", srcLang.googleLanguageCode)
            .addQueryParameter("query.target_language", targetLang.googleLanguageCode)
            .addQueryParameter("query.display_language", primaryLanguage.googleLanguageCode)
            .addQueryParameter("key", googleTranslateApiKey(GoogleTranslateClient.GTX))
            .addQueryParameters(
                "data_types",
                GoogleTranslateDataType.TRANSLATION.value,
                GoogleTranslateDataType.BILINGUAL_DICTIONARY.value,
                GoogleTranslateDataType.ROMANIZATION_SOURCE.value,
                GoogleTranslateDataType.ROMANIZATION_TARGET.value,
                GoogleTranslateDataType.QUERY_CORRECTION.value,
                GoogleTranslateDataType.SENTENCE_SPLITS.value,
                GoogleTranslateDataType.EXAMPLE_SENTENCE.value,
                GoogleTranslateDataType.ALTERNATIVE_TRANSLATIONS.value
            )

        return Http.get(urlBuilder.build()) { setUserAgent() }
    }

    override fun translateDocumentation(
        documentation: Document,
        srcLang: Lang,
        targetLang: Lang
    ): Document = checkError {
        documentation.also { document ->
            HtmlDocumentationTranslator(RawHtmlTranslationStrategy) { texts ->
                translateDocumentation(texts, srcLang, targetLang)
            }.translateDocument(document, service<GoogleSettings>().keepOriginalDocumentation)
        }
    }

    private fun translateDocumentation(texts: List<String>, srcLang: Lang, targetLang: Lang): List<String> {
        val cacheService = service<CacheService>()
        val cacheKey = getDocumentationDiskCacheKey(texts, srcLang, targetLang)
        cacheService.getDiskCache(cacheKey)
            ?.takeIf { it.isNotEmpty() }
            ?.let { cached ->
                try {
                    return parseDocTranslations(cached, texts)
                } catch (e: Throwable) {
                    logger.w("Failed to parse from disk cache.", e)
                }
            }

        val translation = callTranslateDocumentation(texts, srcLang, targetLang)
        val result = parseDocTranslations(translation, texts)
        cacheService.putDiskCache(cacheKey, translation)
        return result
    }

    private fun getDocumentationDiskCacheKey(texts: List<String>, srcLang: Lang, targetLang: Lang): String {
        return "$id;${texts.joinToString(";")};$srcLang;$targetLang".md5()
    }

    private fun callTranslateDocumentation(texts: List<String>, srcLang: Lang, targetLang: Lang): String {
        val data = arrayOf(
            arrayOf(texts, srcLang.googleLanguageCode, targetLang.googleLanguageCode),
            GoogleTranslateClient.TE_LIB.value
        )
        return Http.post(
            googleTranslateApiUrl(DOCUMENTATION_TRANSLATION_API_PATH),
            "application/json+protobuf",
            Http.defaultGson.toJson(data)
        ) {
            setUserAgent()
            tuner {
                it.setRequestProperty("X-Goog-Api-Key", googleTranslateApiKey(GoogleTranslateClient.TE_LIB))
            }
        }
    }

    private fun parseTranslation(
        translation: String,
        original: String,
        srcLang: Lang,
        targetLang: Lang,
    ): Translation {
        logger.i("Translate result: $translation")

        if (translation.isBlank()) {
            return Translation(original, original, srcLang, targetLang, listOf(srcLang))
        }

        return gson.fromJson(translation, GoogleTranslation::class.java).apply {
            this.original = original
            target = targetLang
        }.toTranslation()
    }

    private fun parseDocTranslations(translation: String, texts: List<String>): List<String> {
        logger.i("Translate result: $translation")

        if (translation.isBlank()) {
            return texts
        }

        val translations = gson.fromJson(translation, GDocTranslations::class.java).translations
        return texts.indices.map { index -> translations.getOrNull(index).orEmpty() }
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        if (throwable is HttpRetryException) {
            return ErrorInfo(message("error.service.unavailable"))
        }

        return super.createErrorInfo(throwable)
    }

    private data class GDocTranslations(val translations: List<String>)

    private object GDocTranslationsDeserializer : JsonDeserializer<GDocTranslations> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): GDocTranslations {
            // The response format is `[[translation1, translation2, ...], [detected languages, ...]]`,
            // where the first array element is the list of translations in the input order.
            val translations = json.asJsonArray
                .firstOrNull()
                ?.asJsonArray
                ?.map { element ->
                    element.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                }
                .orEmpty()
            return GDocTranslations(translations)
        }
    }

    private object LangDeserializer : JsonDeserializer<Lang> {
        override fun deserialize(jsonElement: JsonElement, type: Type, context: JsonDeserializationContext)
                : Lang = Lang.fromGoogleLanguageCode(jsonElement.asString)
    }
}