package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.DEFAULT_USER_AGENT
import cn.yiiguxing.plugin.translate.GOOGLE_TRANSLATE_CN_URL
import cn.yiiguxing.plugin.translate.GOOGLE_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.ui.icon.Icons
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import cn.yiiguxing.plugin.translate.util.i
import com.google.gson.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.RequestBuilder
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
    private val gson: Gson = GsonBuilder()
            .registerTypeAdapter(Lang::class.java, LangDeserializer)
            .registerTypeAdapter(GSentence::class.java, GSentenceDeserializer)
            .create()

    override val id: String = TRANSLATOR_ID

    override val name: String = TRANSLATOR_NAME

    override val icon: Icon = Icons.Google

    override val primaryLanguage: Lang
        get() = settings.primaryLanguage

    private val baseUrl: String
        get() = if (settings.useTranslateGoogleCom) GOOGLE_TRANSLATE_URL else GOOGLE_TRANSLATE_CN_URL

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
        builder.userAgent(DEFAULT_USER_AGENT)
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

    private object LangDeserializer : JsonDeserializer<Lang> {
        override fun deserialize(jsonElement: JsonElement, type: Type, context: JsonDeserializationContext)
                : Lang = Lang.valueOfCode(jsonElement.asString)
    }

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