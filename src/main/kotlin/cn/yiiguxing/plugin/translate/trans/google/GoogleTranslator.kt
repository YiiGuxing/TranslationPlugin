package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.documentation.DocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.documentation.translateBody
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.GOOGLE
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Http.setUserAgent
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import cn.yiiguxing.plugin.translate.util.i
import com.google.gson.*
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
        .registerTypeAdapter(GDocTranslation::class.java, GDocTranslationDeserializer)
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
        documentation.translateBody { bodyHTML ->
            translateDocumentation(bodyHTML, srcLang, targetLang)
        }
    }

    private fun translateDocumentation(documentation: String, srcLang: Lang, targetLang: Lang): String {
        val client = SimpleTranslateClient(
            this,
            ::callTranslateDocumentation,
            ::parseDocTranslation
        )
        client.updateCacheKey { it.update("DOCUMENTATION.v2".toByteArray()) }
        return client.execute(documentation, srcLang, targetLang).translation ?: ""
    }

    private fun callTranslateDocumentation(text: String, srcLang: Lang, targetLang: Lang): String {
        val data = arrayOf(
            arrayOf(arrayOf(text), srcLang.googleLanguageCode, targetLang.googleLanguageCode),
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

    private fun parseDocTranslation(
        translation: String,
        original: String,
        srcLang: Lang,
        targetLang: Lang,
    ): BaseTranslation {
        logger.i("Translate result: $translation")

        if (translation.isBlank()) {
            return Translation(original, original, srcLang, targetLang, listOf(srcLang))
        }

        val (translatedText, lang) = gson.fromJson(translation, GDocTranslation::class.java)
        val sLang = lang?.takeIf { srcLang == Lang.AUTO } ?: srcLang
        return BaseTranslation(original, sLang, targetLang, translatedText)
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        if (throwable is HttpRetryException) {
            return ErrorInfo(message("error.service.unavailable"))
        }

        return super.createErrorInfo(throwable)
    }

    private data class GDocTranslation(val translatedText: String, val lang: Lang?)

    private object GDocTranslationDeserializer : JsonDeserializer<GDocTranslation> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): GDocTranslation {
            var array = json.asJsonArray
            while (true) {
                val firstElement = array.first()
                array = if (firstElement.isJsonArray) firstElement.asJsonArray else break
            }

            val translatedText = array.first().asString
            val lang = if (array.size() > 1) Lang.fromGoogleLanguageCode(array[1].asString) else null
            return GDocTranslation(translatedText, lang)
        }
    }

    private object LangDeserializer : JsonDeserializer<Lang> {
        override fun deserialize(jsonElement: JsonElement, type: Type, context: JsonDeserializationContext)
                : Lang = Lang.fromGoogleLanguageCode(jsonElement.asString)
    }
}