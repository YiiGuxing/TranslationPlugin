package cn.yiiguxing.plugin.translate.trans.ali

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.ALI
import cn.yiiguxing.plugin.translate.util.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.Icon


/**
 * Ali translator.
 *
 * [Product description](https://www.aliyun.com/product/ai/base_alimt)
 */
object AliTranslator : AbstractTranslator(), DocumentationTranslator {

    private const val ALI_TRANSLATE_PRODUCT_URL = "https://www.aliyun.com/product/ai/base_alimt"

    private const val API_ENDPOINT = "mt.aliyuncs.com"
    private const val API_VERSION = "2018-10-12"
    private const val SIGNATURE_ALGORITHM = "ACS3-HMAC-SHA256"
    private const val ACTION_TRANSLATE = "TranslateGeneral"

    private val EMPTY_RESPONSE_REGEX = "\\{\\s*}".toRegex()
    private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").apply {
        timeZone = SimpleTimeZone(0, "GMT")
    }


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
        if (force ||
            Settings.getInstance().aliTranslateSettings.let { it.appId.isEmpty() || it.getAppKey().isEmpty() }
        ) {
            return ALI.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(
            this,
            { _, _, _ -> callTranslate(text, srcLang, targetLang, false) },
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
                { _, _, _ -> callTranslate(bodyHTML, srcLang, targetLang, true) },
                AliTranslator::parseTranslation
            )
            val translation = with(client) {
                updateCacheKey { it.update("DOCUMENTATION".toByteArray()) }
                execute(bodyHTML, srcLang, targetLang)
            }

            translation.translation ?: ""
        }
    }

    private fun callTranslate(text: String, srcLang: Lang, targetLang: Lang, isDocumentation: Boolean): String {
        val formatType = if (isDocumentation) "html" else "text"
        val data = AliTranslationData(text, srcLang.aliLanguageCode, targetLang.aliLanguageCode, formatType)
        val request = Request(ACTION_TRANSLATE, Http.MIME_TYPE_FORM, Http.getFormUrlEncoded(data.toDataForm()))
        return sendHttpRequest(request)
    }

    private fun sendHttpRequest(request: Request): String {
        sign(request)

        val url = UrlBuilder("https://${request.uriHost}${request.uriPath}").apply {
            request.queries.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        return Http.post(url, request.contentType, request.body) {
            tuner { conn ->
                request.headers.forEach { (key, value) ->
                    conn.setRequestProperty(key, value)
                }
            }
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
                10010 -> return ErrorInfo(
                    message("error.service.is.down"),
                    ErrorInfo.browseUrlAction(message("error.service.is.down.action.name"), ALI_TRANSLATE_PRODUCT_URL)
                )

                else -> "[${throwable.code}] ${throwable.errorMessage}"
            }

            is Http.StatusException -> getErrorMessage(throwable)

            else -> null
        } ?: return super.createErrorInfo(throwable)

        return ErrorInfo(errorMessage)
    }

    private fun getErrorMessage(exception: Http.StatusException): String? {
        return try {
            exception.errorText
                ?.let { Gson().fromJson(it, ErrorResponse::class.java) }
                ?.let { "[${it.code}] ${it.message}" }
        } catch (e: Exception) {
            null
        }
    }

    private fun sign(request: Request) {
        val requestPayloadHash = request.body.sha256().toLowerCase()
        request.headers["x-acs-content-sha256"] = requestPayloadHash

        val headers = request.headers
            .asSequence()
            .filter { (key) ->
                key.startsWith("x-acs-") ||
                        key.equals("Host", true) ||
                        key.equals("Content-Type", true)
            }
            .map { (key, value) -> key.toLowerCase() to value }
            .sortedBy { (key) -> key }
        val headerNames = headers.joinToString(";") { (key) -> key }
        val headerContent = headers.joinToString("\n", postfix = "\n") { (key, value) -> "${key}:$value" }
        val queries = request.queries
            .asSequence()
            .map { (key, value) -> "${fixedEncode(key)}=${fixedEncode(value)}" }
            .sorted()
            .joinToString("&")
        val requestContent = """
            |POST
            |${request.uriPath}
            |$queries
            |$headerContent
            |$headerNames
            |$requestPayloadHash
        """.trimMargin("|")
        val requestContentHash = requestContent.sha256().toLowerCase()

        val settings = Settings.getInstance().aliTranslateSettings
        val signature = settings.getAppKey()
            .takeIf { it.isNotEmpty() }
            ?.let { key -> "$SIGNATURE_ALGORITHM\n$requestContentHash".hmacSha256(key).toLowerCase() }
            ?: ""
        val authorization =
            "$SIGNATURE_ALGORITHM Credential=${settings.appId},SignedHeaders=$headerNames,Signature=$signature"
        request.headers["Authorization"] = authorization
    }

    private fun fixedEncode(input: String): String {
        return URLEncoder.encode(input, Charsets.UTF_8)
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }


    private data class Request(
        val action: String,
        val contentType: String,
        val body: String,
        val version: String = API_VERSION,
        val uriHost: String = API_ENDPOINT,
        val uriPath: String = "/",
        val headers: TreeMap<String, String> = TreeMap(),
        val queries: TreeMap<String, String> = TreeMap()
    ) {
        init {
            headers["Host"] = uriHost
            headers["x-acs-action"] = action
            headers["x-acs-version"] = version
            headers["x-acs-date"] = DATE_FORMATTER.format(Date())
            headers["x-acs-signature-nonce"] = UUID.randomUUID().toString()
        }
    }

    private data class AliTranslationData(
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
    ) {
        fun toDataForm(): Map<String, String> {
            return mapOf(
                "SourceText" to sourceText,
                "SourceLanguage" to sourceLanguage,
                "TargetLanguage" to targetLanguage,
                "FormatType" to formatType,
                "Scene" to scene
            )
        }
    }

    private data class ErrorResponse(
        @SerializedName("Code")
        val code: String,
        @SerializedName("Message")
        val message: String
    )

    class AliTranslationResultException(code: Int, val errorMessage: String?) :
        TranslationResultException(code) {
        override fun getLocalizedMessage(): String {
            return "$message[$errorMessage]"
        }
    }
}
