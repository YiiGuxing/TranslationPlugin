package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.GOOGLE_DOCUMENTATION_TRANSLATE_URL_FORMAT
import cn.yiiguxing.plugin.translate.GOOGLE_TRANSLATE_URL_FORMAT
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.GOOGLE
import cn.yiiguxing.plugin.translate.util.*
import com.google.gson.*
import com.intellij.openapi.diagnostic.Logger
import java.lang.reflect.Type
import javax.swing.Icon

/**
 * GoogleTranslator
 */
object GoogleTranslator : AbstractTranslator(), DocumentationTranslator {
    private val settings = Settings.googleTranslateSettings
    private val logger: Logger = Logger.getInstance(GoogleTranslator::class.java)

    @Suppress("SpellCheckingInspection")
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Lang::class.java, LangDeserializer)
        .registerTypeAdapter(GSentence::class.java, GSentenceDeserializer)
        .create()

    override val id: String = GOOGLE.id

    override val name: String = GOOGLE.translatorName

    override val icon: Icon = GOOGLE.icon

    override val intervalLimit: Int = GOOGLE.intervalLimit

    override val contentLengthLimit: Int = GOOGLE.contentLengthLimit

    override val primaryLanguage: Lang
        get() = settings.primaryLanguage

    private val notSupportedLanguages = listOf(Lang.CHINESE_CANTONESE, Lang.CHINESE_CLASSICAL)

    override val supportedSourceLanguages: List<Lang> = (Lang.sortedValues() - notSupportedLanguages).toList()
    override val supportedTargetLanguages: List<Lang> =
        (Lang.sortedValues() - notSupportedLanguages - Lang.AUTO).toList()

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(
            this,
            { _, _, _ -> call(text, srcLang, targetLang, false) },
            GoogleTranslator::parseTranslation
        ).execute(text, srcLang, targetLang)
    }

    override fun translateDocumentation(documentation: String, srcLang: Lang, targetLang: Lang): BaseTranslation {
        return checkError {
            val client = SimpleTranslateClient(
                this,
                { _, _, _ -> call(documentation, srcLang, targetLang, true) },
                GoogleTranslator::parseDocTranslation
            )

            client.updateCacheKey { it.update("DOCUMENTATION".toByteArray()) }
            client.execute(documentation, srcLang, targetLang)
        }
    }

    private fun call(text: String, srcLang: Lang, targetLang: Lang, isDocumentation: Boolean): String {
        val baseUrl = if (isDocumentation) {
            GOOGLE_DOCUMENTATION_TRANSLATE_URL_FORMAT
        } else {
            GOOGLE_TRANSLATE_URL_FORMAT
        }.format(GoogleHttp.googleHost)

        val urlBuilder = UrlBuilder(baseUrl)
            .addQueryParameter("sl", srcLang.code)
            .addQueryParameter("tl", targetLang.code)

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
                .addQueryParameter("hl", primaryLanguage.code) // 词性的语言
        }

        val url = urlBuilder
            .addQueryParameter("tk", text.tk())
            .build()
            .also { logger.i("Translate url: $it") }

        return Http.postDataFrom(url, "q" to text) {
            userAgent().googleReferer()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun parseTranslation(
        translation: String,
        original: String,
        srcLang: Lang,
        targetLang: Lang,
    ): Translation {
        logger.i("Translate result: $translation")

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

        val results = gson.fromJson(translation, Array<String>::class.java)
        val sLang = if (srcLang == Lang.AUTO && results.size >= 2) Lang[results[1]] else srcLang

        return BaseTranslation(original, sLang, targetLang, results[0])
    }

    private object LangDeserializer : JsonDeserializer<Lang> {
        override fun deserialize(jsonElement: JsonElement, type: Type, context: JsonDeserializationContext)
                : Lang = Lang[jsonElement.asString]
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