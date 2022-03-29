package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.documentation.*
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.GOOGLE
import cn.yiiguxing.plugin.translate.util.*
import com.google.gson.*
import com.intellij.openapi.diagnostic.Logger
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.reflect.Type
import java.net.HttpRetryException
import javax.swing.Icon

/**
 * GoogleTranslator
 */
object GoogleTranslator : AbstractTranslator(), DocumentationTranslator {

    private const val TRANSLATE_API_URL = "https://translate.googleapis.com/translate_a/single"
    private const val DOCUMENTATION_TRANSLATION_API_URL = "https://translate.googleapis.com/translate_a/t"


    private const val TAG_I = "i"
    private const val TAG_EM = "em"
    private const val TAG_B = "b"
    private const val TAG_STRONG = "strong"
    private const val TAG_SPAN = "span"


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

    override fun translateDocumentation(documentation: Document, srcLang: Lang, targetLang: Lang): Document {
        return checkError {
            processAndTranslateDocumentation(documentation) {
                translateDocumentation(it, srcLang, targetLang)
            }
        }
    }

    private inline fun processAndTranslateDocumentation(
        documentation: Document,
        translate: (String) -> String
    ): Document {
        val body = documentation.body()

        // 翻译内容会带有原文与译文，分号包在 `i` 标签和 `b` 标签内，因此替换掉这两个标签以免影响到翻译后的处理。
        val content = body.html()
            .replaceTag(TAG_B, TAG_STRONG)
            .replaceTag(TAG_I, TAG_EM)
        if (content.isBlank()) {
            return documentation
        }

        val translation = translate(content)

        body.html(translation)
        // 去除原文标签。
        body.select(TAG_I).remove()
        // 去除译文的粗体效果，`b` 标签替换为 `span` 标签。
        body.select(TAG_B).forEach { it.replaceWith(Element(TAG_SPAN).html(it.html())) }

        return documentation
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

    private fun String.replaceTag(targetTag: String, replacementTag: String): String {
        val regex = Regex("<(?<pre>/??)$targetTag(?<pos>( .+?)*?)>")
        return replace(regex, "<${'$'}{pre}$replacementTag${'$'}{pos}>")
    }

    private fun call(text: String, srcLang: Lang, targetLang: Lang, isDocumentation: Boolean): String {
        val baseUrl = if (isDocumentation) DOCUMENTATION_TRANSLATION_API_URL else TRANSLATE_API_URL
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

        return Http.post(url, "q" to text) {
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

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        if (throwable is HttpRetryException) {
            return ErrorInfo(message("error.service.unavailable"))
        }

        return super.createErrorInfo(throwable)
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