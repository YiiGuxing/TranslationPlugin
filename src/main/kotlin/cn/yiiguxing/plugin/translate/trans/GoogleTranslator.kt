package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.GOOGLE_DOCUMENTATION_TRANSLATE_URL_FORMAT
import cn.yiiguxing.plugin.translate.GOOGLE_TRANSLATE_URL_FORMAT
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.GOOGLE
import cn.yiiguxing.plugin.translate.util.*
import com.google.gson.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.RequestBuilder
import java.lang.reflect.Type
import javax.swing.Icon

/**
 * GoogleTranslator
 */
object GoogleTranslator : AbstractTranslator() {
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

    override fun buildRequest(builder: RequestBuilder, isDocumentation: Boolean) {
        builder.userAgent().googleReferer()
    }

    override fun getRequestUrl(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isDocumentation: Boolean
    ): String {
        val baseUrl = if (isDocumentation) {
            GOOGLE_DOCUMENTATION_TRANSLATE_URL_FORMAT
        } else {
            GOOGLE_TRANSLATE_URL_FORMAT
        }.format(googleHost)

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
                .addQueryParameters("dt", "t", /*"at",*/ "bd", "rm", "qca")
                .addQueryParameter("dj", "1")
                .addQueryParameter("ie", "UTF-8")
                .addQueryParameter("oe", "UTF-8")
                .addQueryParameter("hl", primaryLanguage.code) // 词性的语言
        }

        return urlBuilder
            .addQueryParameter("tk", text.tk())
            .build()
            .also { logger.i("Translate url: $it") }
    }

    override fun getRequestParams(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isDocumentation: Boolean
    ): List<Pair<String, String>> {
        return listOf("q" to text)
    }

    override fun parserResult(
        original: String,
        srcLang: Lang,
        targetLang: Lang,
        result: String,
        isDocumentation: Boolean
    ): BaseTranslation {
        logger.i("Translate result: $result")

        return if (isDocumentation) {
            val results = gson.fromJson(result, Array<String>::class.java)
            val sLang = if (srcLang == Lang.AUTO) Lang.valueOfCode(results[1]) else srcLang

            BaseTranslation(sLang, targetLang, results[0])
        } else {
            gson.fromJson(result, GoogleTranslation::class.java).apply {
                this.original = original
                target = targetLang
            }.toTranslation()
        }
    }

    override fun onError(throwable: Throwable): Throwable {
        return NetworkException.wrapIfIsNetworkException(throwable, googleHost)
    }

    private object LangDeserializer : JsonDeserializer<Lang> {
        override fun deserialize(jsonElement: JsonElement, type: Type, context: JsonDeserializationContext)
                : Lang = Lang.valueOfCode(jsonElement.asString)
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