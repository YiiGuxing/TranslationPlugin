@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.BAIDU_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.BAIDU
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.md5
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import javax.swing.Icon

/**
 * Baidu translator
 */
object BaiduTranslator : AbstractTranslator() {

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

    override val id: String = BAIDU.id

    override val name: String = BAIDU.translatorName

    override val icon: Icon = BAIDU.icon

    override val intervalLimit: Int = BAIDU.intervalLimit

    override val contentLengthLimit: Int = BAIDU.contentLengthLimit

    override val primaryLanguage: Lang
        get() = Settings.baiduTranslateSettings.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES
        .toMutableList()
        .apply { add(0, Lang.AUTO) }
    override val supportedTargetLanguages: List<Lang> = SUPPORTED_LANGUAGES

    override fun checkConfiguration(): Boolean {
        if (Settings.baiduTranslateSettings.let { it.appId.isEmpty() || it.getAppKey().isEmpty() }) {
            return BAIDU.showConfigurationDialog()
        }

        return true
    }

    override fun getRequestUrl(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isDocumentation: Boolean
    ): String = BAIDU_TRANSLATE_URL

    override fun getRequestParams(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isDocumentation: Boolean
    ): List<Pair<String, String>> {
        val settings = Settings.baiduTranslateSettings
        val appId = settings.appId
        val privateKey = settings.getAppKey()
        val salt = System.currentTimeMillis().toString()
        val sign = (appId + text + salt + privateKey).md5().toLowerCase()

        return ArrayList<Pair<String, String>>().apply {
            add("appid" to appId)
            add("from" to srcLang.baiduCode)
            add("to" to targetLang.baiduCode)
            add("salt" to salt)
            add("sign" to sign)
            add("q" to text)
        }
    }

    override fun parserResult(
        original: String,
        srcLang: Lang,
        targetLang: Lang,
        result: String,
        isDocumentation: Boolean
    ): BaseTranslation {
        logger.i("Translate result: $result")

        return Gson().fromJson(result, BaiduTranslation::class.java).apply {
            if (!isSuccessful) {
                throw TranslateResultException(code, name)
            }
        }.toTranslation()
    }

    override fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is TranslateResultException -> when (throwable.code) {
            52001 -> message("error.request.timeout")
            52002 -> message("error.systemError")
            52003 -> message("error.invalidAccount", HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION)
            54000 -> message("error.missingParameter")
            54001 -> message("error.invalidSignature", HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION)
            54003, 54005 -> message("error.access.limited")
            54004 -> message("error.account.has.run.out.of.balance")
            58000 -> message("error.access.ip")
            58001 -> message("error.language.unsupported")
            58002 -> "服务当前已关闭，请前往管理控制台开启服务"
            90107 -> "认证未通过或未生效"
            else -> message("error.unknown") + "[${throwable.code}]"
        }
        else -> super.createErrorMessage(throwable)
    }
}