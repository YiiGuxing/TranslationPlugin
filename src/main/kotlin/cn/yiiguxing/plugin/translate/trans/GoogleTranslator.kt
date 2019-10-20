package cn.yiiguxing.plugin.translate.trans

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
 *
 * Created by Yii.Guxing on 2017/11/10
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

    private val baseUrl: String get() = GOOGLE_TRANSLATE_URL_FORMAT.format(googleHost)

    private val notSupportedLanguages = arrayListOf(Lang.CHINESE_CANTONESE, Lang.CHINESE_CLASSICAL)

    override val supportedSourceLanguages: List<Lang> = Lang.sortedValues()
        .toMutableList()
        .apply { removeAll(notSupportedLanguages) }
    override val supportedTargetLanguages: List<Lang> = Lang.sortedValues()
        .toMutableList()
        .apply {
            remove(Lang.AUTO)
            removeAll(notSupportedLanguages)
        }

    override fun buildRequest(builder: RequestBuilder) {
        builder.userAgent().googleReferer()
    }

    override fun getTranslateUrl(text: String, srcLang: Lang, targetLang: Lang): String = UrlBuilder(baseUrl)
        .addQueryParameter("client", "gtx")
        .addQueryParameters("dt", "t", /*"at",*/ "bd", "rm")
        .addQueryParameter("dj", "1")
        .addQueryParameter("ie", "UTF-8")
        .addQueryParameter("oe", "UTF-8")
        .addQueryParameter("sl", srcLang.code)
        .addQueryParameter("tl", targetLang.code)
        .addQueryParameter("hl", primaryLanguage.code) // 词性的语言
        .addQueryParameter("tk", text.tk())
        .addQueryParameter("q", text)
        .build()
        .also { logger.i("Translate url: $it") }

    override fun parserResult(original: String, srcLang: Lang, targetLang: Lang, result: String): Translation {
        logger.i("Translate result: $result")

        return gson.fromJson(result, GoogleTranslation::class.java).apply {
            this.original = original
            target = targetLang
        }.toTranslation()
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