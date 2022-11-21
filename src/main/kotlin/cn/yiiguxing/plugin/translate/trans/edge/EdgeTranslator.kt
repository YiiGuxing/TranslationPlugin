package cn.yiiguxing.plugin.translate.trans.edge

import cn.yiiguxing.plugin.translate.trans.AbstractTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.SimpleTranslateClient
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.google.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import cn.yiiguxing.plugin.translate.util.i
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import javax.swing.Icon


object EdgeTranslator : AbstractTranslator() {
    private const val TRANSLATE_API_URL = "https://api.cognitive.microsofttranslator.com/translate"


    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(
            this,
            { _, _, _ -> call(text, srcLang, targetLang, false) },
            this::parseTranslation
        ).execute(text, srcLang, targetLang)
    }

    private fun parseTranslation(
        translation: String,
        original: String,
        srcLang: Lang,
        targetLang: Lang,
    ): Translation {
        if (translation.isBlank()) {
            return Translation(original, original, srcLang, targetLang, listOf(srcLang))
        }

        val result = gson.fromJson(translation, EdgeTranslationResultList::class.java)
        val translationResult = result.getOrNull(0)?.apply {
            this.original = original
            this.srcLang = srcLang
            this.targetLang = targetLang
        }?.toTranslation()

        return translationResult ?: Translation(original, original, srcLang, targetLang, listOf(srcLang))
    }

    private val gson = Gson()
    override val id: String = TranslationEngine.EDGE.id
    override val name: String = TranslationEngine.EDGE.translatorName
    override val icon: Icon = TranslationEngine.EDGE.icon
    override val primaryLanguage: Lang
        get() = TranslationEngine.EDGE.primaryLanguage
    private val notSupportedLanguages = listOf(Lang.CHINESE_CANTONESE, Lang.CHINESE_CLASSICAL)
    override val supportedSourceLanguages: List<Lang> = (Lang.sortedValues() - notSupportedLanguages).toList()
    override val supportedTargetLanguages: List<Lang> =
        (Lang.sortedValues() - notSupportedLanguages - Lang.AUTO).toList()
    override val intervalLimit: Int = 0
    override val contentLengthLimit: Int = -1

    private val logger: Logger = Logger.getInstance(EdgeTranslator::class.java)

    private fun call(text: String, srcLang: Lang, targetLang: Lang, isDocumentation: Boolean = false): String {

        val baseUrl = TRANSLATE_API_URL
        val urlBuilder = UrlBuilder(baseUrl)
            .addQueryParameter("from", srcLang.edgeLanguageCode)
            .addQueryParameter("to", targetLang.edgeLanguageCode)
            .addQueryParameter("api-version", "3.0")
            .addQueryParameter("includeSentenceLength", "false")

        val url = urlBuilder.build().also { logger.i("Translate url: $it") }

        val data = listOf(EdgeRequestJsonItem(text))

        return Http.postJson(url, data) {
            userAgent()
            tuner { conn ->
                conn.setRequestProperty("Authorization", "Bearer ${Auth.token}")
            }
        }
    }

}

