package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION
import cn.yiiguxing.plugin.translate.YOUDAO_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.YOUDAO
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.sha256
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import java.util.*
import javax.swing.Icon

@Suppress("SpellCheckingInspection")
object YoudaoTranslator : AbstractTranslator() {

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

    override val id: String = YOUDAO.id

    override val name: String = YOUDAO.translatorName

    override val icon: Icon = YOUDAO.icon

    override val intervalLimit: Int = YOUDAO.intervalLimit

    override val contentLengthLimit: Int = YOUDAO.contentLengthLimit

    override val defaultLangForLocale: Lang
        get() = when (Locale.getDefault()) {
            Locale.CHINA, Locale.CHINESE -> Lang.AUTO
            else -> super.defaultLangForLocale
        }

    override val primaryLanguage: Lang
        get() = YOUDAO.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES
    override val supportedTargetLanguages: List<Lang> = SUPPORTED_LANGUAGES

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || Settings.youdaoTranslateSettings.let { it.appId.isEmpty() || it.getAppKey().isEmpty() }) {
            return YOUDAO.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(this, ::call, ::parseTranslation).execute(text, srcLang, targetLang)
    }

    private fun call(
        text: String,
        srcLang: Lang,
        targetLang: Lang
    ): String {
        val settings = Settings.youdaoTranslateSettings
        val appId = settings.appId
        val privateKey = settings.getAppKey()
        val salt = UUID.randomUUID().toString()
        val curTime = (System.currentTimeMillis() / 1000).toString()
        val qInSign = if (text.length <= 20) text else "${text.take(10)}${text.length}${text.takeLast(10)}"
        val sign = "$appId$qInSign$salt$curTime$privateKey".sha256()

        return Http.postDataFrom(
            YOUDAO_TRANSLATE_URL,
            "appKey" to appId,
            "from" to srcLang.youdaoCode,
            "to" to targetLang.youdaoCode,
            "salt" to salt,
            "sign" to sign,
            "signType" to "v3",
            "curtime" to curTime,
            "q" to text
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun parseTranslation(translation: String, original: String, srcLang: Lang, targetLang: Lang): Translation {
        logger.i("Translate result: $translation")

        return Gson().fromJson(translation, YoudaoTranslation::class.java).apply {
            query = original
            checkError()
            if (!isSuccessful) {
                throw TranslateResultException(errorCode, name)
            }
        }.toTranslation()
    }

    override fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is TranslateResultException -> when (throwable.code) {
            101 -> message("error.missingParameter")
            102 -> message("error.language.unsupported")
            103 -> message("error.text.too.long")
            104 -> message("error.youdao.unsupported.api")
            105 -> message("error.youdao.unsupported.signature")
            106 -> message("error.youdao.unsupported.response")
            107 -> message("error.youdao.unsupported.encryptType")
            108 -> message("error.youdao.invalidKey", HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION)
            109 -> message("error.youdao.batchLog")
            110 -> message("error.youdao.noInstance")
            111 -> message("error.invalidAccount", HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION)
            201 -> message("error.youdao.decrypt")
            202 -> message("error.invalidSignature", HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION)
            203 -> message("error.access.ip")
            301 -> message("error.youdao.dictionary")
            302 -> message("error.youdao.translation")
            303 -> message("error.youdao.serverError")
            401 -> message("error.account.has.run.out.of.balance")
            else -> message("error.unknown") + "[${throwable.code}]"
        }
        else -> super.createErrorMessage(throwable)
    }
}
