package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SETTINGS
import cn.yiiguxing.plugin.translate.YOUDAO_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.sha256
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import icons.Icons
import java.util.*
import javax.swing.Icon

@Suppress("SpellCheckingInspection")
object YoudaoTranslator : AbstractTranslator() {

    const val TRANSLATOR_ID = "ai.youdao"

    private const val TRANSLATOR_NAME = "Youdao Translate"

    private val SUPPORTED_LANGUAGES: List<Lang> = (Lang.sortedValues() - listOf(
        Lang.CHINESE_TRADITIONAL,
        Lang.CHINESE_CLASSICAL,
        Lang.AFRIKAANS,
        Lang.KYRGYZ,
        Lang.CATALAN,
        Lang.HMONG,
        Lang.SERBIAN,
        Lang.SLOVENIAN
    )).toList()

    private val logger: Logger = Logger.getInstance(YoudaoTranslator::class.java)

    override val id: String = TRANSLATOR_ID

    override val name: String = TRANSLATOR_NAME

    override val icon: Icon = Icons.Youdao

    override val defaultLangForLocale: Lang
        get() = when (Locale.getDefault()) {
            Locale.CHINA, Locale.CHINESE -> Lang.AUTO
            else -> super.defaultLangForLocale
        }

    override val primaryLanguage: Lang
        get() = Settings.youdaoTranslateSettings.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES
    override val supportedTargetLanguages: List<Lang> = SUPPORTED_LANGUAGES

    override fun getTranslateUrl(text: String, srcLang: Lang, targetLang: Lang, forDocumentation: Boolean): String {
        val settings = Settings.youdaoTranslateSettings
        val appId = settings.appId
        val privateKey = settings.getAppKey()
        val salt = UUID.randomUUID().toString()
        val curTime = (System.currentTimeMillis() / 1000).toString()
        val qInSign = if (text.length <= 20) text else "${text.take(10)}${text.length}${text.takeLast(10)}"
        val sign = "$appId$qInSign$salt$curTime$privateKey".sha256()


        return UrlBuilder(YOUDAO_TRANSLATE_URL)
            .addQueryParameter("appKey", appId)
            .addQueryParameter("from", srcLang.youdaoCode)
            .addQueryParameter("to", targetLang.youdaoCode)
            .addQueryParameter("salt", salt)
            .addQueryParameter("sign", sign)
            .addQueryParameter("signType", "v3")
            .addQueryParameter("curtime", curTime)
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

        return Gson().fromJson(result, YoudaoTranslation::class.java).apply {
            query = original
            checkError()
            if (!isSuccessful) {
                throw TranslateResultException(errorCode, name)
            }
        }.toTranslation()
    }

    @Suppress("InvalidBundleOrProperty")
    override fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is TranslateResultException -> when (throwable.code) {
            101 -> message("error.missingParameter")
            102 -> message("error.language.unsupported")
            103 -> message("error.youdao.textTooLong")
            104 -> message("error.youdao.unsupported.api")
            105 -> message("error.youdao.unsupported.signature")
            106 -> message("error.youdao.unsupported.response")
            107 -> message("error.youdao.unsupported.encryptType")
            108 -> message("error.youdao.invalidKey", HTML_DESCRIPTION_SETTINGS)
            109 -> message("error.youdao.batchLog")
            110 -> message("error.youdao.noInstance")
            111 -> message("error.invalidAccount", HTML_DESCRIPTION_SETTINGS)
            201 -> message("error.youdao.decrypt")
            202 -> message("error.invalidSignature", HTML_DESCRIPTION_SETTINGS)
            203 -> message("error.access.ip")
            301 -> message("error.youdao.dictionary")
            302 -> message("error.youdao.translation")
            303 -> message("error.youdao.serverError")
            401 -> message("error.account.arrears")
            else -> message("error.unknown") + "[${throwable.code}]"
        }
        else -> super.createErrorMessage(throwable)
    }
}
