package cn.yiiguxing.plugin.translate.trans.ali

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.toExplicit
import cn.yiiguxing.plugin.translate.trans.ali.models.AliMTRequest
import cn.yiiguxing.plugin.translate.trans.ali.models.AliMTResponse
import cn.yiiguxing.plugin.translate.trans.ali.models.AliTranslation
import cn.yiiguxing.plugin.translate.trans.ali.models.AliTranslationInput
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.ALI
import cn.yiiguxing.plugin.translate.util.*
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import org.jsoup.nodes.Document
import java.net.URLEncoder
import javax.swing.Icon


/**
 * Ali translator.
 *
 * [Product description](https://www.aliyun.com/product/ai/base_alimt)
 */
object AliTranslator : AbstractTranslator(), DocumentationTranslator {

    private const val ALI_TRANSLATE_PRODUCT_URL = "https://www.aliyun.com/product/ai/base_alimt"

    private const val SIGNATURE_ALGORITHM = "ACS3-HMAC-SHA256"
    private const val ACTION_TRANSLATE = "TranslateGeneral"

    private val EMPTY_RESPONSE_REGEX = "\\{\\s*}".toRegex()


    private val logger: Logger = Logger.getInstance(AliTranslator::class.java)

    override val id: String = ALI.id

    override val name: String = ALI.translatorName

    override val icon: Icon = ALI.icon

    override val intervalLimit: Int = ALI.intervalLimit

    override val contentLengthLimit: Int = ALI.contentLengthLimit

    override val primaryLanguage: Lang
        get() = ALI.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = AliLanguageAdapter.sourceLanguages

    override val supportedTargetLanguages: List<Lang> = AliLanguageAdapter.targetLanguages

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
        val data = AliTranslationInput(text, srcLang.aliLanguageCode, targetLang.aliLanguageCode, formatType)
        val request = AliMTRequest(
            ACTION_TRANSLATE,
            Http.MIME_TYPE_FORM,
            Http.getFormUrlEncoded(data.toDataForm())
        )
        return sendRequest(request)
    }

    private fun parseTranslation(
        translation: String,
        original: String,
        srcLang: Lang,
        targetLang: Lang
    ): Translation {
        logger.i("Translate result: $translation")

        val explicitSrcLang = srcLang.toExplicit()
        // 可能会返回一个空JSON对象：`{}`
        if (translation.isBlank() || translation.trim().matches(EMPTY_RESPONSE_REGEX)) {
            return Translation(original, original, explicitSrcLang, targetLang, listOf(explicitSrcLang))
        }

        val type = type<AliMTResponse<AliTranslation>>()
        val response: AliMTResponse<AliTranslation> = Gson().fromJson(translation, type)
        if (!response.isSuccessful) {
            throw AliTranslationResultException(response.code, response.message)
        }

        val aliTranslation = response.data
        val translatedText = aliTranslation?.translated?.takeIf { it.isNotEmpty() } ?: original
        val srcLanguage = aliTranslation?.detectedLanguage?.let { Lang.fromAliLanguageCode(it) } ?: explicitSrcLang
        return Translation(original, translatedText, srcLanguage, targetLang, listOf(srcLanguage))
    }

    private fun sendRequest(request: AliMTRequest): String {
        signRequest(request)

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

    private fun signRequest(request: AliMTRequest) {
        val requestPayloadHash = request.body.sha256().lowercase()
        request.headers["x-acs-content-sha256"] = requestPayloadHash

        val headers = request.headers
            .asSequence()
            .filter { (key) ->
                key.startsWith("x-acs-") ||
                        key.equals("Host", true) ||
                        key.equals("Content-Type", true)
            }
            .map { (key, value) -> key.lowercase() to value }
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
        val requestContentHash = requestContent.sha256().lowercase()

        val settings = Settings.getInstance().aliTranslateSettings
        val signature = settings.getAppKey()
            .takeIf { it.isNotEmpty() }
            ?.let { key -> "$SIGNATURE_ALGORITHM\n$requestContentHash".hmacSha256(key).lowercase() }
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

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        val errorMessage = when (throwable) {
            is AliTranslationResultException -> when (throwable.code) {
                10010 -> return ErrorInfo(
                    message("error.service.is.down"),
                    ErrorInfo.browseUrlAction(message("error.service.is.down.action.name"), ALI_TRANSLATE_PRODUCT_URL)
                )

                else -> "[${throwable.errorCode}] ${throwable.errorMessage}"
            }

            is Http.StatusException -> getErrorMessage(throwable)

            else -> null
        } ?: return super.createErrorInfo(throwable)

        return ErrorInfo(errorMessage)
    }

    private fun getErrorMessage(exception: Http.StatusException): String? {
        return try {
            exception.errorText
                ?.let { Gson().fromJson(it, AliMTResponse::class.java) }
                ?.let { "[${it.code}] ${it.message}" }
        } catch (e: Exception) {
            null
        }
    }
}
