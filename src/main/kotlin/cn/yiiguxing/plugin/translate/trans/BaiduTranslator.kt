@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.BAIDU_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SETTINGS
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.md5
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import icons.Icons
import javax.swing.Icon

/**
 * Baidu translator
 */
object BaiduTranslator : AbstractTranslator() {

    const val TRANSLATOR_ID = "fanyi.baidu"

    private const val TRANSLATOR_NAME = "Baidu Translate"

    private val SUPPORTED_LANGUAGES: List<Lang> = listOf(
        Lang.CHINESE,
        Lang.ENGLISH,
        Lang.CHINESE_TRADITIONAL,
        Lang.CHINESE_CANTONESE,
        Lang.CHINESE_CLASSICAL,
        Lang.JAPANESE,
        Lang.KOREAN,
        Lang.FRENCH,
        Lang.SPANISH,
        Lang.THAI,
        Lang.ARABIC,
        Lang.RUSSIAN,
        Lang.PORTUGUESE,
        Lang.GERMAN,
        Lang.ITALIAN,
        Lang.GREEK,
        Lang.DUTCH,
        Lang.POLISH,
        Lang.BULGARIAN,
        Lang.ESTONIAN,
        Lang.DANISH,
        Lang.FINNISH,
        Lang.CZECH,
        Lang.ROMANIAN,
        Lang.SLOVENIAN,
        Lang.SWEDISH,
        Lang.HUNGARIAN,
        Lang.VIETNAMESE
    )

    private val logger: Logger = Logger.getInstance(BaiduTranslator::class.java)

    override val id: String = TRANSLATOR_ID

    override val name: String = TRANSLATOR_NAME

    override val icon: Icon = Icons.Baidu

    override val primaryLanguage: Lang
        get() = Settings.baiduTranslateSettings.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES
        .toMutableList()
        .apply { add(0, Lang.AUTO) }
    override val supportedTargetLanguages: List<Lang> = SUPPORTED_LANGUAGES

    override fun getTranslateUrl(text: String, srcLang: Lang, targetLang: Lang, forDocumentation: Boolean): String {
        val settings = Settings.baiduTranslateSettings
        val appId = settings.appId
        val privateKey = settings.getAppKey()
        val salt = System.currentTimeMillis().toString()
        val sign = (appId + text + salt + privateKey).md5().toLowerCase()

        return UrlBuilder(BAIDU_TRANSLATE_URL)
            .addQueryParameter("appid", appId)
            .addQueryParameter("from", srcLang.baiduCode)
            .addQueryParameter("to", targetLang.baiduCode)
            .addQueryParameter("salt", salt)
            .addQueryParameter("sign", sign)
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

        return Gson().fromJson(result, BaiduTranslation::class.java).apply {
            if (!isSuccessful) {
                throw TranslateResultException(code, name)
            }
        }.toTranslation()
    }

    @Suppress("InvalidBundleOrProperty")
    override fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is TranslateResultException -> when (throwable.code) {
            52001 -> message("error.request.timeout")
            52002 -> message("error.systemError")
            52003 -> message("error.invalidAccount", HTML_DESCRIPTION_SETTINGS)
            54000 -> message("error.missingParameter")
            54001 -> message("error.invalidSignature", HTML_DESCRIPTION_SETTINGS)
            54003, 54005 -> message("error.access.limited")
            54004 -> message("error.account.arrears")
            58000 -> message("error.access.ip")
            58001 -> message("error.language.unsupported")
            58002 -> "服务当前已关闭，请前往管理控制台开启服务"
            90107 -> "认证未通过或未生效"
            else -> message("error.unknown") + "[${throwable.code}]"
        }
        else -> super.createErrorMessage(throwable)
    }
}