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
import org.jsoup.nodes.Document
import java.util.*
import javax.swing.Icon

@Suppress("SpellCheckingInspection")
object YoudaoTranslator : AbstractTranslator(), DocumentationTranslator {

    private const val YOUDAO_API_SERVICE_URL = "https://openapi.youdao.com"
    private const val YOUDAO_TEXT_TRANSLATION_API_URL = "$YOUDAO_API_SERVICE_URL/api"
    private const val YOUDAO_HTML_TRANSLATION_API_URL = "$YOUDAO_API_SERVICE_URL/translate_html"
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
            310 to message("youdao.error.message.domain.service.not.enabled"),
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
        return SimpleTranslateClient(this, ::call, ::parseTranslation)
            .apply { updateCacheKey { it.update(settings.domain.value.toByteArray()) } }
            .execute(text, srcLang, targetLang)
    }

    private fun call(text: String, srcLang: Lang, targetLang: Lang): String {
        val params = getBaseRequestParams(text, srcLang, targetLang)
        params["domain"] = settings.domain.value
        params["rejectFallback"] = true.toString()

        return Http.post(YOUDAO_TEXT_TRANSLATION_API_URL, params)
    }

    private fun getBaseRequestParams(text: String, srcLang: Lang, targetLang: Lang): MutableMap<String, String> {
        val credentialSettings = Settings.youdaoTranslateSettings

        val appId = credentialSettings.appId
        val privateKey = credentialSettings.getAppKey()
        val salt = UUID.randomUUID().toString()
        val curTime = (System.currentTimeMillis() / 1000).toString()
        val qInSign = if (text.length <= 20) text else "${text.take(10)}${text.length}${text.takeLast(10)}"
        val sign = "$appId$qInSign$salt$curTime$privateKey".sha256()

        return mutableMapOf(
            "q" to text,
            "from" to srcLang.youdaoLanguageCode,
            "to" to targetLang.youdaoLanguageCode,
            "appKey" to appId,
            "salt" to salt,
            "sign" to sign,
            "signType" to "v3",
            "curtime" to curTime,
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

    override fun translateDocumentation(
        documentation: Document,
        srcLang: Lang,
        targetLang: Lang
    ): Document = checkError {
        documentation.translateBody { bodyHTML ->
            translateDocumentation(bodyHTML, srcLang, targetLang)
        }
    }

    private fun translateDocumentation(documentation: String, srcLang: Lang, targetLang: Lang): String {
        // Youdao does not support auto detection target language for documentation translation
        val fixedTargetLang = targetLang.takeIf { it != Lang.AUTO } ?: super.defaultLangForLocale
        val client = SimpleTranslateClient(this, ::callForDocumentation, ::parseTranslationForDocumentation)
        client.updateCacheKey { it.update("DOCUMENTATION".toByteArray()) }
        return client.execute(documentation, srcLang, fixedTargetLang).translation ?: ""
    }

    private fun callForDocumentation(text: String, srcLang: Lang, targetLang: Lang): String {
        val params = getBaseRequestParams(text, srcLang, targetLang)
        return Http.post(YOUDAO_HTML_TRANSLATION_API_URL, params)
    }

    private fun parseTranslationForDocumentation(
        translation: String,
        original: String,
        srcLang: Lang,
        targetLang: Lang
    ): BaseTranslation {
        logger.i("Documentation translation result: $translation")
        return with(Gson().fromJson(translation, YoudaoHTMLTranslation::class.java)) {
            if (errorCode != 0) {
                throw TranslationResultException(errorCode)
            }
            BaseTranslation(original, srcLang, targetLang, data?.translation)
        }
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

                110, 310 -> ErrorInfo.browseUrlAction(message("youdao.action.enable.service"), YOUDAO_CONSOLE_URL)

                302 -> {
                    if (settings.domain != YoudaoDomain.GENERAL) {
                        errorMessage = message("youdao.error.message.domain.service.not.enabled")
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
