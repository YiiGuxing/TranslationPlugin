package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SETTINGS
import cn.yiiguxing.plugin.translate.YOUDAO_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.icon.Icons
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.md5
import cn.yiiguxing.plugin.translate.util.urlEncode
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import javax.swing.Icon

object YoudaoTranslator : AbstractTranslator() {

    const val TRANSLATOR_ID = "ai.youdao"

    private const val TRANSLATOR_NAME = "Youdao Translate"

    val SUPPORTED_LANGUAGES: List<Lang> = listOf(
            Lang.AUTO,
            Lang.CHINESE,
            Lang.ENGLISH,
            Lang.JAPANESE,
            Lang.KOREAN,
            Lang.FRENCH,
            Lang.RUSSIAN,
            Lang.PORTUGUESE,
            Lang.SPANISH)

    private val logger: Logger = Logger.getInstance(YoudaoTranslator::class.java)

    override val id: String = TRANSLATOR_ID

    override val name: String = TRANSLATOR_NAME

    override val icon: Icon = Icons.Youdao

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

        return (YOUDAO_TRANSLATE_URL +
                "?appKey=${appId.urlEncode()}" +
                "&from=${getLanguageCode(srcLang)}" +
                "&to=${getLanguageCode(targetLang)}" +
                "&salt=$salt" +
                "&sign=$sign" +
                "&q=${text.urlEncode()}")
                .also {
                    logger.i("Translate url: $it")
                }
    }

    private fun getLanguageCode(lang: Lang): String = if (lang == Lang.CHINESE) "zh-CHS" else lang.code

    override fun parserResult(original: String, srcLang: Lang, targetLang: Lang, result: String): Translation {
        logger.i("Translate result: $result")

        return Gson().fromJson(result, YoudaoTranslation::class.java).apply {
            query = original
            checkError()
            if (!isSuccessful) {
                throw YoudaoTranslateException(errorCode)
            }
        }.toTranslation()
    }

    override fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is YoudaoTranslateException -> "${message("error.code", throwable.code)}: " + when (throwable.code) {
            101 -> message("error.youdao.missingParameter")
            102 -> message("error.youdao.unsupported.language")
            103 -> message("error.youdao.textTooLong")
            104 -> message("error.youdao.unsupported.api")
            105 -> message("error.youdao.unsupported.signature")
            106 -> message("error.youdao.unsupported.response")
            107 -> message("error.youdao.unsupported.encryptType")
            108 -> message("error.youdao.invalidKey", HTML_DESCRIPTION_SETTINGS)
            109 -> message("error.youdao.batchLog")
            110 -> message("error.youdao.noInstance")
            111 -> message("error.youdao.invalidAccount", HTML_DESCRIPTION_SETTINGS)
            201 -> message("error.youdao.decrypt")
            202 -> message("error.youdao.invalidSignature", HTML_DESCRIPTION_SETTINGS)
            203 -> message("error.youdao.ip")
            301 -> message("error.youdao.dictionary")
            302 -> message("error.youdao.translation")
            303 -> message("error.youdao.serverError")
            401 -> message("error.youdao.arrears")
            else -> message("error.unknown")
        }
        else -> super.createErrorMessage(throwable)
    }

    private class YoudaoTranslateException(val code: Int) : TranslateException("Translate failed: $code")

}
