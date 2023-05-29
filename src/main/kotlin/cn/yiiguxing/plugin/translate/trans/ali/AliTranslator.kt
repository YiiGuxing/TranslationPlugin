package cn.yiiguxing.plugin.translate.trans.ali

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.ALI
import cn.yiiguxing.plugin.translate.util.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import org.jsoup.nodes.Document
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.Icon

/**
 * Ali translator
 */
// Product description: https://www.aliyun.com/product/ai/base_alimt
object AliTranslator : AbstractTranslator(), DocumentationTranslator {

    private const val ALI_TRANSLATE_API_URL = "https://mt.aliyuncs.com/api/translate/web/general"
    private const val ALI_TRANSLATE_PRODUCT_URL = "https://www.aliyun.com/product/ai/base_alimt"

    private val EMPTY_RESPONSE_REGEX = "\\{\\s*}".toRegex()


    private val logger: Logger = Logger.getInstance(AliTranslator::class.java)

    override val id: String = ALI.id

    override val name: String = ALI.translatorName

    override val icon: Icon = ALI.icon

    override val intervalLimit: Int = ALI.intervalLimit

    override val contentLengthLimit: Int = ALI.contentLengthLimit

    override val primaryLanguage: Lang
        get() = ALI.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = AliLanguageAdapter.supportedSourceLanguages

    override val supportedTargetLanguages: List<Lang> = AliLanguageAdapter.supportedTargetLanguages

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || Settings.aliTranslateSettings.let { it.appId.isEmpty() || it.getAppKey().isEmpty() }) {
            return ALI.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(
            this,
            { _, _, _ -> call(text, srcLang, targetLang, false) },
            AliTranslator::parseTranslation
        ).execute(text, srcLang, targetLang)
    }

    override fun translateDocumentation(
        documentation: Document,
        srcLang: Lang,
        targetLang: Lang
    ): Document = checkError {
        documentation.translateBody { bodyHTML ->
            checkContentLength(bodyHTML, contentLengthLimit)

            val client = SimpleTranslateClient(
                this,
                { _, _, _ -> call(bodyHTML, srcLang, targetLang, true) },
                AliTranslator::parseTranslation
            )
            val translation = with(client) {
                updateCacheKey { it.update("DOCUMENTATION".toByteArray()) }
                execute(bodyHTML, srcLang, targetLang)
            }

            translation.translation ?: ""
        }
    }

    /**
     * 序列化json模型
     */
    @Suppress("MemberVisibilityCanBePrivate")
    data class AliTranslationRequest(
        @SerializedName("SourceText")
        val sourceText: String,
        @SerializedName("SourceLanguage")
        val sourceLanguage: String,
        @SerializedName("TargetLanguage")
        val targetLanguage: String,
        @SerializedName("FormatType")
        val formatType: String = "text",
        @SerializedName("Scene")
        val scene: String = "general"
    )

    private fun call(text: String, srcLang: Lang, targetLang: Lang, isDocumentation: Boolean): String {
        val formatType = if (isDocumentation) "html" else "text"
        val request = AliTranslationRequest(text, srcLang.aliLanguageCode, targetLang.aliLanguageCode, formatType)

        return sendHttpRequest(request)
    }

    private fun sendHttpRequest(request: Any): String {
        val url = ALI_TRANSLATE_API_URL
        val body = Gson().toJson(request)

        val realUrl = URL(url)
        val accept = "application/json"
        val contentType = "application/json; charset=utf-8"
        val date: String = toGMTString(Date())
        val bodyMd5: String = body.md5Base64()
        val uuid = UUID.randomUUID().toString()
        val version = "2019-01-02"
        val dataToSign = """
            POST
            $accept
            $bodyMd5
            $contentType
            $date
            x-acs-signature-method:HMAC-SHA1
            x-acs-signature-nonce:$uuid
            x-acs-version:$version
            ${realUrl.file}
        """.trimIndent()

        logger.i("Data to sign: $dataToSign")
        val settings = Settings.aliTranslateSettings

        return Http.post(url, contentType, body) {
            tuner { conn ->
                conn.setRequestProperty("Accept", accept)
                conn.setRequestProperty("Content-MD5", bodyMd5)
                conn.setRequestProperty("Date", date)
                conn.setRequestProperty("Host", realUrl.host)
                settings.getAppKey().takeIf { it.isNotEmpty() }?.let { key ->
                    conn.setRequestProperty("Authorization", "acs ${settings.appId}:${dataToSign.hmacSha1(key)}")
                }
                conn.setRequestProperty("x-acs-signature-nonce", uuid)
                conn.setRequestProperty("x-acs-signature-method", "HMAC-SHA1")
                conn.setRequestProperty("x-acs-version", version)
            }
        }
    }

    class AliTranslationResultException(code: Int, val errorMessage: String?) :
        TranslationResultException(code) {
        override fun getLocalizedMessage(): String {
            return "$message[$errorMessage]"
        }
    }

    private fun parseTranslation(
        translation: String,
        original: String,
        srcLang: Lang,
        targetLang: Lang
    ): Translation {
        logger.i("Translate result: $translation")

        // 阿里翻译会返回一个空JSON对象：`{}`
        if (translation.isBlank() || translation.trim().matches(EMPTY_RESPONSE_REGEX)) {
            return Translation(original, original, srcLang, targetLang, listOf(srcLang))
        }

        return Gson().fromJson(translation, AliTranslation::class.java).apply {
            query = original
            src = srcLang
            target = targetLang
            if (!isSuccessful) {
                throw AliTranslationResultException(intCode, errorMessage)
            }
        }.toTranslation()
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        val errorMessage = when (throwable) {
            is AliTranslationResultException -> when (throwable.code) {
                10001 -> message("error.request.timeout")
                10002 -> message("error.systemError")
                10003 -> message("error.bad.request")
                10004 -> message("error.missingParameter")
                10005 -> message("error.language.unsupported")
                10006 -> message("error.ali.language.detecting.failed")
                10007 -> message("error.systemError")
                10008 -> message("error.text.too.long")
                10009 -> message("error.ali.permission.denied")

                10010 -> return ErrorInfo(
                    message("error.service.is.down"),
                    ErrorInfo.browseUrlAction(message("error.service.is.down.action.name"), ALI_TRANSLATE_PRODUCT_URL)
                )

                10011,
                10012 -> message("error.systemError")

                10013 -> message("error.account.has.run.out.of.balance")
                else -> message("error.unknown") + "[${throwable.code}] ${throwable.errorMessage}"
            }

            else -> return super.createErrorInfo(throwable)
        }

        return ErrorInfo(errorMessage)
    }

    private fun toGMTString(date: Date): String {
        val df = SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.US)
        df.timeZone = SimpleTimeZone(0, "GMT")
        return df.format(date)
    }
}
