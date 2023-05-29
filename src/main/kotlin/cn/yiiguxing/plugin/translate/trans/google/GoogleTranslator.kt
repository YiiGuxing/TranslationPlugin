package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.GOOGLE
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Http.userAgent
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

    private const val TRANSLATE_API_PATH = "/translate_a/single"
    private const val DOCUMENTATION_TRANSLATION_API_PATH = "/translate_a/t"


    private val logger: Logger = Logger.getInstance(GoogleTranslator::class.java)

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Lang::class.java, LangDeserializer)
        .registerTypeAdapter(GDocTranslation::class.java, GDocTranslationDeserializer)
        .registerTypeAdapter(GSentence::class.java, GSentenceDeserializer)
        .create()

    override val id: String = GOOGLE.id

    override val name: String = GOOGLE.translatorName

    override val icon: Icon = GOOGLE.icon

    override val intervalLimit: Int = GOOGLE.intervalLimit

    override val contentLengthLimit: Int = GOOGLE.contentLengthLimit

    override val primaryLanguage: Lang
        get() = GOOGLE.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = GoogleLanguageAdapter.supportedSourceLanguages

    override val supportedTargetLanguages: List<Lang> = GoogleLanguageAdapter.supportedTargetLanguages


    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(
            this,
            { _, _, _ -> call(text, srcLang, targetLang, false) },
            GoogleTranslator::parseTranslation
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
            { _, _, _ -> call(documentation, srcLang, targetLang, true) },
            GoogleTranslator::parseDocTranslation
        )
        client.updateCacheKey { it.update("DOCUMENTATION".toByteArray()) }
        return client.execute(documentation, srcLang, targetLang).translation ?: ""
    }

    private fun call(text: String, srcLang: Lang, targetLang: Lang, isDocumentation: Boolean): String {
        val apiPath = if (isDocumentation) DOCUMENTATION_TRANSLATION_API_PATH else TRANSLATE_API_PATH
        val urlBuilder = UrlBuilder(googleApiUrl(apiPath))
            .addQueryParameter("sl", srcLang.googleLanguageCode)
            .addQueryParameter("tl", targetLang.googleLanguageCode)

        if (isDocumentation) {
            urlBuilder
                .addQueryParameter("client", "te_lib")
                .addQueryParameter("format", "html")
        } else {
            urlBuilder
                .addQueryParameter("client", "gtx")
                .addQueryParameters("dt", "t", /*"at",*/ "bd", "rm", "qca", "ex")
                .addQueryParameter("dj", "1")
                .addQueryParameter("ie", "UTF-8")
                .addQueryParameter("oe", "UTF-8")
                .addQueryParameter("hl", primaryLanguage.googleLanguageCode) // 词性的语言
        }

        val url = urlBuilder
            .addQueryParameter("tk", text.tk())
            .build()
            .also { logger.i("Translate url: $it") }

        return Http.post(url, "q" to text) {
            userAgent().googleReferer()
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

    @Suppress("SpellCheckingInspection")
    private object GSentenceDeserializer : JsonDeserializer<GSentence> {
        override fun deserialize(jsonElement: JsonElement, type: Type, context: JsonDeserializationContext): GSentence {
            val jsonObject = jsonElement.asJsonObject
            return when {
                jsonObject.has("trans") -> {
                    context.deserialize<GTransSentence>(jsonElement, GTransSentence::class.java)
                }

                jsonObject.has("translit") || jsonObject.has("src_translit") -> {
                    context.deserialize<GTranslitSentence>(jsonElement, GTranslitSentence::class.java)
                }

                else -> throw JsonParseException("Cannot deserialize to type GSentence: $jsonElement")
            }
        }
    }
}