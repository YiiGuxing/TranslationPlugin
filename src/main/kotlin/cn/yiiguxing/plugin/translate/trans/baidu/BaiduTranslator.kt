@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans.baidu

import cn.yiiguxing.plugin.translate.BAIDU_FANYI_PRODUCT_URL
import cn.yiiguxing.plugin.translate.BAIDU_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.BAIDU
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.md5
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import java.util.*
import javax.swing.Icon

/**
 * Baidu translator
 */
object BaiduTranslator : AbstractTranslator() {

    /** 通用版支持的语言列表 */
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

    /** 尊享版支持的语言列表 */
    @Suppress("unused")
    private val SUPPORTED_LANGUAGES_PRO: List<Lang> = listOf(
        Lang.CHINESE,
        Lang.ENGLISH,
        Lang.CHINESE_TRADITIONAL,
        Lang.CHINESE_CANTONESE,
        Lang.CHINESE_CLASSICAL,
        Lang.ALBANIAN,
        Lang.AMHARIC,
        Lang.ARABIC,
        Lang.AZERBAIJANI,
        Lang.IRISH,
        Lang.ESTONIAN,
        Lang.BASQUE,
        Lang.BELARUSIAN,
        Lang.BOSNIAN,
        Lang.BULGARIAN,
        Lang.PORTUGUESE,
        Lang.POLISH,
        Lang.PERSIAN,
        Lang.ICELANDIC,
        Lang.DANISH,
        Lang.GERMAN,
        Lang.GEORGIAN,
        Lang.GUJARATI,
        Lang.KHMER,
        Lang.KOREAN,
        Lang.JAPANESE,
        Lang.FILIPINO,
        Lang.FINNISH,
        Lang.FRENCH,
        Lang.DUTCH,
        Lang.GALICIAN,
        Lang.CATALAN,
        Lang.RUSSIAN,
        Lang.CZECH,
        Lang.KANNADA,
        Lang.XHOSA,
        Lang.CROATIAN,
        Lang.KURDISH,
        Lang.ROMANIAN,
        Lang.LATIN,
        Lang.LATVIAN,
        Lang.KINYARWANDA,
        Lang.LITHUANIAN,
        Lang.MALAY,
        Lang.MYANMAR,
        Lang.MALAYALAM,
        Lang.MACEDONIAN,
        Lang.BENGALI,
        Lang.MALTESE,
        Lang.NORWEGIAN,
        Lang.AFRIKAANS,
        Lang.NEPALI,
        Lang.PUNJABI,
        Lang.SWEDISH,
        Lang.SERBIAN,
        Lang.SINHALA,
        Lang.ESPERANTO,
        Lang.SLOVAK,
        Lang.SLOVENIAN,
        Lang.SWAHILI,
        Lang.SOMALI,
        Lang.THAI,
        Lang.TURKISH,
        Lang.TAJIK,
        Lang.TAMIL,
        Lang.TELUGU,
        Lang.UKRAINIAN,
        Lang.WELSH,
        Lang.URDU,
        Lang.SPANISH,
        Lang.HEBREW,
        Lang.GREEK,
        Lang.HUNGARIAN,
        Lang.HINDI,
        Lang.INDONESIAN,
        Lang.ITALIAN,
        Lang.VIETNAMESE,
        Lang.ARMENIAN,
    )

    private val logger: Logger = Logger.getInstance(BaiduTranslator::class.java)

    override val id: String = BAIDU.id

    override val name: String = BAIDU.translatorName

    override val icon: Icon = BAIDU.icon

    override val intervalLimit: Int = BAIDU.intervalLimit

    override val contentLengthLimit: Int = BAIDU.contentLengthLimit

    override val primaryLanguage: Lang
        get() = BAIDU.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES
        .toMutableList()
        .apply { add(0, Lang.AUTO) }
    override val supportedTargetLanguages: List<Lang> = SUPPORTED_LANGUAGES

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || Settings.baiduTranslateSettings.let { it.appId.isEmpty() || it.getAppKey().isEmpty() }) {
            return BAIDU.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(this, BaiduTranslator::call, BaiduTranslator::parseTranslation).execute(
            text,
            srcLang,
            targetLang
        )
    }

    private fun call(text: String, srcLang: Lang, targetLang: Lang): String {
        val settings = Settings.baiduTranslateSettings
        val appId = settings.appId
        val privateKey = settings.getAppKey()
        val salt = System.currentTimeMillis().toString()
        val sign = (appId + text + salt + privateKey).md5().lowercase(Locale.getDefault())

        return Http.postDataFrom(
            BAIDU_TRANSLATE_URL,
            "appid" to appId,
            "from" to srcLang.baiduLanguageCode,
            "to" to targetLang.baiduLanguageCode,
            "salt" to salt,
            "sign" to sign,
            "q" to text
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun parseTranslation(translation: String, original: String, srcLang: Lang, targetLang: Lang): Translation {
        logger.i("Translate result: $translation")

        return Gson().fromJson(translation, BaiduTranslation::class.java).apply {
            if (!isSuccessful) {
                throw TranslationResultException(code, name)
            }
        }.toTranslation()
    }

    override fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is TranslationResultException -> when (throwable.code) {
            52001 -> message("error.request.timeout")
            52002 -> message("error.systemError")
            52003 -> message("error.invalidAccount", HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION)
            54000 -> message("error.missingParameter")
            54001 -> message("error.invalidSignature", HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION)
            54003, 54005 -> message("error.access.limited")
            54004 -> message("error.account.has.run.out.of.balance")
            58000 -> message("error.access.ip")
            58001 -> message("error.language.unsupported")
            58002 -> message("error.service.is.down", BAIDU_FANYI_PRODUCT_URL)
            90107 -> message("error.unauthorized")
            else -> message("error.unknown") + "[${throwable.code}]"
        }
        else -> super.createErrorMessage(throwable)
    }
}