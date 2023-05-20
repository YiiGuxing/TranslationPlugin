package cn.yiiguxing.plugin.translate.trans.youdao

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.YOUDAO
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.sha256
import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import java.util.*
import javax.swing.Icon

@Suppress("SpellCheckingInspection")
object YoudaoTranslator : AbstractTranslator() {

    private const val YOUDAO_API_URL = "https://openapi.youdao.com/api"
    private const val YOUDAO_CONSOLE_URL = "https://ai.youdao.com/console"

    private val settings: YoudaoSettings by lazy { service<YoudaoSettings>() }

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

    override val supportedSourceLanguages: List<Lang> = YoudaoLanguageAdapter.supportedSourceLanguages

    override val supportedTargetLanguages: List<Lang> = YoudaoLanguageAdapter.supportedTargetLanguages

    private val errorMessageMap: Map<Int, String> by lazy {
        mapOf(
            101 to message("error.missingParameter"),
            102 to message("error.language.unsupported"),
            103 to message("error.text.too.long"),
            104 to message("error.youdao.unsupported.api"),
            105 to message("error.youdao.unsupported.signature"),
            106 to message("error.youdao.unsupported.response"),
            107 to message("error.youdao.unsupported.encryptType"),
            108 to message("error.youdao.invalidKey"),
            109 to message("error.youdao.batchLog"),
            110 to message("error.youdao.noInstance"),
            111 to message("error.invalidAccount"),
            201 to message("error.youdao.decrypt"),
            202 to message("error.invalidSignature"),
            203 to message("error.access.ip"),
            301 to message("error.youdao.dictionary"),
            302 to message("error.youdao.translation"),
            303 to message("error.youdao.serverError"),
            401 to message("error.account.has.run.out.of.balance")
        )
    }

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || Settings.youdaoTranslateSettings.let { it.appId.isEmpty() || it.getAppKey().isEmpty() }) {
            return YOUDAO.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(this, YoudaoTranslator::call, YoudaoTranslator::parseTranslation)
            .apply { updateCacheKey { it.update(settings.domain.value.toByteArray()) } }
            .execute(text, srcLang, targetLang)
    }

    private fun call(
        text: String,
        srcLang: Lang,
        targetLang: Lang
    ): String {
        val credentialSettings = Settings.youdaoTranslateSettings

        val appId = credentialSettings.appId
        val privateKey = credentialSettings.getAppKey()
        val salt = UUID.randomUUID().toString()
        val curTime = (System.currentTimeMillis() / 1000).toString()
        val qInSign = if (text.length <= 20) text else "${text.take(10)}${text.length}${text.takeLast(10)}"
        val sign = "$appId$qInSign$salt$curTime$privateKey".sha256()

        return Http.post(
            YOUDAO_API_URL,
            "appKey" to appId,
            "from" to srcLang.youdaoLanguageCode,
            "to" to targetLang.youdaoLanguageCode,
            "salt" to salt,
            "sign" to sign,
            "signType" to "v3",
            "curtime" to curTime,
            "q" to text,
            "domain" to settings.domain.value,
            "rejectFallback" to true.toString(),
        )
    }

    private fun parseTranslation(translation: String, original: String, srcLang: Lang, targetLang: Lang): Translation {
        logger.i("Translate result: $translation")

        if (translation.isBlank()) {
            return Translation(original, original, srcLang, targetLang, listOf(srcLang))
        }

        return Gson().fromJson(translation, YoudaoTranslation::class.java).apply {
            query = original
            checkError()
            if (!isSuccessful) {
                throw TranslationResultException(errorCode)
            }
        }.toTranslation()
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        if (throwable is TranslationResultException) {
            var errorMessage =
                errorMessageMap.getOrDefault(throwable.code, message("error.unknown") + "[${throwable.code}]")
            val continueAction = when (throwable.code) {
                108, 111, 202 -> ErrorInfo.continueAction(
                    message("action.check.configuration"),
                    icon = AllIcons.General.Settings
                ) {
                    YOUDAO.showConfigurationDialog()
                }

                302, 310 -> {
                    if (throwable.code == 310 || settings.domain != YoudaoDomain.GENERAL) {
                        errorMessage = message("youdao.error.310.message")
                        ErrorInfo.browseUrlAction(message("youdao.action.enable.service"), YOUDAO_CONSOLE_URL)
                    } else null
                }

                else -> null
            }

            return ErrorInfo(errorMessage, if (continueAction != null) listOf(continueAction) else emptyList())
        }

        return super.createErrorInfo(throwable)
    }
}
