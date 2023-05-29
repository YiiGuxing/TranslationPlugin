@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans.baidu

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.BAIDU
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.md5
import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import java.util.*
import javax.swing.Icon

/**
 * Baidu translator
 */
object BaiduTranslator : AbstractTranslator() {

    private const val BAIDU_TRANSLATE_API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate"
    private const val BAIDU_FANYI_PRODUCT_URL = "https://fanyi-api.baidu.com/choose"


    private val gson = Gson()

    private val logger: Logger = Logger.getInstance(BaiduTranslator::class.java)

    override val id: String = BAIDU.id

    override val name: String = BAIDU.translatorName

    override val icon: Icon = BAIDU.icon

    override val intervalLimit: Int = BAIDU.intervalLimit

    override val contentLengthLimit: Int = BAIDU.contentLengthLimit

    override val primaryLanguage: Lang
        get() = BAIDU.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = BaiduLanguageAdapter.supportedSourceLanguages

    override val supportedTargetLanguages: List<Lang> = BaiduLanguageAdapter.supportedTargetLanguages

    private val errorMessageMap: Map<Int, String> by lazy {
        mapOf(
            52001 to message("error.request.timeout"),
            52002 to message("error.systemError"),
            52003 to message("error.invalidAccount"),
            54000 to message("error.missingParameter"),
            54001 to message("error.invalidSignature"),
            54003 to message("error.access.limited"),
            54005 to message("error.access.limited"),
            54004 to message("error.account.has.run.out.of.balance"),
            58000 to message("error.access.ip"),
            58001 to message("error.language.unsupported"),
            58002 to message("error.service.is.down"),
            90107 to message("error.unauthorized"),
        )
    }

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

        return Http.post(
            BAIDU_TRANSLATE_API_URL,
            "appid" to appId,
            "from" to srcLang.baiduLanguageCode,
            "to" to targetLang.baiduLanguageCode,
            "salt" to salt,
            "sign" to sign,
            "q" to text
        )
    }

    private fun parseTranslation(translation: String, original: String, srcLang: Lang, targetLang: Lang): Translation {
        logger.i("Translate result: $translation")

        if (translation.isBlank()) {
            return Translation(original, original, srcLang, targetLang, listOf(srcLang))
        }

        return gson.fromJson(translation, BaiduTranslation::class.java).apply {
            if (!isSuccessful) {
                throw TranslationResultException(code)
            }
        }.toTranslation()
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        if (throwable is TranslationResultException) {
            val errorMessage =
                errorMessageMap.getOrDefault(throwable.code, message("error.unknown") + "[${throwable.code}]")
            val continueAction = when (throwable.code) {
                52003, 54001 -> ErrorInfo.continueAction(
                    message("action.check.configuration"),
                    icon = AllIcons.General.Settings
                ) {
                    BAIDU.showConfigurationDialog()
                }

                58002 -> ErrorInfo.browseUrlAction(
                    message("error.service.is.down.action.name"),
                    BAIDU_FANYI_PRODUCT_URL
                )

                else -> null
            }

            return ErrorInfo(errorMessage, if (continueAction != null) listOf(continueAction) else emptyList())
        }

        return super.createErrorInfo(throwable)
    }
}