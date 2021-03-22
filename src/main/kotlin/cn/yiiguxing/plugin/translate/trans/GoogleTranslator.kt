package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.GOOGLE_DOCUMENTATION_TRANSLATE_URL_FORMAT
import cn.yiiguxing.plugin.translate.GOOGLE_TRANSLATE_URL_FORMAT
import cn.yiiguxing.plugin.translate.util.*
import com.google.gson.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.RequestBuilder
import icons.Icons
import java.lang.reflect.Type
import javax.swing.Icon

/**
 * GoogleTranslator
 */
object GoogleTranslator : AbstractTranslator() {

    const val TRANSLATOR_ID = "translate.google"
    private const val TRANSLATOR_NAME = "Google Translate"

    private val settings = Settings.googleTranslateSettings
    private val logger: Logger = Logger.getInstance(GoogleTranslator::class.java)

    @Suppress("SpellCheckingInspection")
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Lang::class.java, LangDeserializer)
        .registerTypeAdapter(GSentence::class.java, GSentenceDeserializer)
        .create()

    override val id: String = TRANSLATOR_ID

    override val name: String = TRANSLATOR_NAME

    override val icon: Icon = Icons.Google

    override val primaryLanguage: Lang
        get() = settings.primaryLanguage

    private val notSupportedLanguages = listOf(Lang.CHINESE_CANTONESE, Lang.CHINESE_CLASSICAL)

    override val supportedSourceLanguages: List<Lang> = (Lang.sortedValues() - notSupportedLanguages).toList()
    override val supportedTargetLanguages: List<Lang> =
        (Lang.sortedValues() - notSupportedLanguages - Lang.AUTO).toList()

    override fun buildRequest(builder: RequestBuilder, orDocumentation: Boolean) {
        builder.userAgent().googleReferer()
    }

    override fun getTranslateUrl(text: String, srcLang: Lang, targetLang: Lang, forDocumentation: Boolean): String {
        val baseUrl = if (forDocumentation) {
            GOOGLE_DOCUMENTATION_TRANSLATE_URL_FORMAT
        } else {
            GOOGLE_TRANSLATE_URL_FORMAT
        }.format(googleHost)

        val urlBuilder = UrlBuilder(baseUrl)
            .addQueryParameter("sl", srcLang.code)
            .addQueryParameter("tl", targetLang.code)

        if (forDocumentation) {
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
            .addQueryParameter("q", text)
            .build()
            .also { logger.i("Translate url: $it") }
    }

    override fun parserResult(
        original: String,
        srcLang: Lang,
        targetLang: Lang,
        result: String,
        forDocumentation: Boolean
    ): BaseTranslation {
        logger.i("Translate result: $result")

        return if (forDocumentation) {
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
                jsonObject.has("orig") && jsonObject.has("trans") -> {
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