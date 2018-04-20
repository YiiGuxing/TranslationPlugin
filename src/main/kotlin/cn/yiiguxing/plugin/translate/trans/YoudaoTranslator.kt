package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SETTINGS
import cn.yiiguxing.plugin.translate.YOUDAO_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.icon.Icons
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.md5
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import java.util.*
import javax.swing.Icon

object YoudaoTranslator : AbstractTranslator() {

    const val TRANSLATOR_ID = "ai.youdao"

    private const val TRANSLATOR_NAME = "Youdao Translate"

    private val SUPPORTED_LANGUAGES: List<Lang> = listOf(
            Lang.AUTO,
            Lang.CHINESE,
            Lang.ENGLISH,
            Lang.FRENCH,
            Lang.JAPANESE,
            Lang.KOREAN,
            Lang.PORTUGUESE,
            Lang.RUSSIAN,
            Lang.SPANISH)

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

    override fun getTranslateUrl(text: String, srcLang: Lang, targetLang: Lang): String {
        val settings = Settings.youdaoTranslateSettings
        val appId = settings.appId
        val privateKey = settings.getAppKey()
        val salt = System.currentTimeMillis().toString()
        val sign = (appId + text + salt + privateKey).md5()

        return UrlBuilder(YOUDAO_TRANSLATE_URL)
                .addQueryParameter("appKey", appId)
                .addQueryParameter("from", srcLang.baiduCode)
                .addQueryParameter("to", targetLang.baiduCode)
                .addQueryParameter("salt", salt)
                .addQueryParameter("sign", sign)
                .addQueryParameter("q", text)
                .build()
                .also { logger.i("Translate url: $it") }
    }

    override fun parserResult(original: String, srcLang: Lang, targetLang: Lang, result: String): Translation {
        logger.i("Translate result: $result")

        return Gson().fromJson(result, YoudaoTranslation::class.java).apply {
            query = original
            checkError()
            if (!isSuccessful) {
                throw TranslateResultException(errorCode)
            }
        }.toTranslation()
    }

    @Suppress("InvalidBundleOrProperty")
    override fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is TranslateResultException -> "${message("error.code", throwable.code)}: " + when (throwable.code) {
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
            else -> message("error.unknown")
        }
        else -> super.createErrorMessage(throwable)
    }
}
