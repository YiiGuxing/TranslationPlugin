package cn.yiiguxing.plugin.translate.trans.tencent

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.toExplicit
import cn.yiiguxing.plugin.translate.trans.tencent.models.TencentTranslateRequest
import cn.yiiguxing.plugin.translate.trans.tencent.models.TencentTranslateResponse
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.TENCENT
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.hmacSha256
import cn.yiiguxing.plugin.translate.util.sha256
import cn.yiiguxing.plugin.translate.util.toHexString
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Evaluator
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.Icon

/**
 * Tencent translator.
 *
 * [Product description](https://cloud.tencent.com/product/tmt)
 */
object TencentTranslator : AbstractTranslator(),DocumentationTranslator {

    private const val TENCENT_TRANSLATE_PRODUCT_URL = "https://cloud.tencent.com/product/tmt"
    private const val TENCENT_API_ENDPOINT = "tmt.tencentcloudapi.com"
    private const val TENCENT_API_VERSION = "2018-03-21"
    private const val ACTION_TEXT_TRANSLATE = "TextTranslate"
    private const val ALGORITHM = "TC3-HMAC-SHA256"
    private const val SERVICE = "tmt"
    private const val TC3_REQUEST = "tc3_request"

    private val logger: Logger = Logger.getInstance(TencentTranslator::class.java)
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd").apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override val id: String = TENCENT.id

    override val name: String = TENCENT.translatorName

    override val icon: Icon = TENCENT.icon

    @Deprecated("""Use "RateLimiter" in the "translate" implementation.""")
    override val intervalLimit: Int = TENCENT.intervalLimit
    override val contentLengthLimit: Int
        get() = 6000

    override val primaryLanguage: Lang
        get() = TENCENT.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = TencentLanguageAdapter.sourceLanguages

    override val supportedTargetLanguages: List<Lang> = TencentLanguageAdapter.targetLanguages

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force ||
            Settings.getInstance().tencentTranslateSettings.let { it.appId.isEmpty() || it.getAppKey().isEmpty() }
        ) {
            return TENCENT.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        return SimpleTranslateClient(
            this,
            { _, _, _ -> callTranslate(text, srcLang, targetLang, false) },
            TencentTranslator::parseTranslation
        ).execute(text, srcLang, targetLang)
    }

    private fun callTranslate(text: String, srcLang: Lang, targetLang: Lang, isDocumentation: Boolean): String {
        val request = TencentTranslateRequest(
            sourceText = text,
            source = srcLang.tencentLanguageCode,
            target = targetLang.tencentLanguageCode
        )

        return sendRequest(request)
    }

    private fun parseTranslation(
        translation: String,
        original: String,
        srcLang: Lang,
        targetLang: Lang
    ): Translation {
        val explicitSrcLang = srcLang.toExplicit()
        if (translation.isBlank()) {
            return Translation(original, original, explicitSrcLang, targetLang, listOf(explicitSrcLang))
        }

        val response: TencentTranslateResponse = Gson().fromJson(translation, TencentTranslateResponse::class.java)
        val result = response.response

        if (result.error != null) {
            throw TencentTranslationException(result.error.code, result.error.message)
        }

        val translatedText = result.targetText.takeIf { it.isNotEmpty() } ?: original
        val detectedLang = Lang.fromTencentLanguageCode(result.source)
        return Translation(original, translatedText, detectedLang, targetLang, listOf(detectedLang))
    }

    private fun sendRequest(request: TencentTranslateRequest): String {
        val settings = Settings.getInstance().tencentTranslateSettings
        return sendRequest(request, settings.appId, settings.getAppKey())
    }

    fun sendRequest(request: TencentTranslateRequest, secretId: String, secretKey: String): String {
        val timestamp = System.currentTimeMillis() / 1000
        val date = dateFormatter.format(Date(timestamp * 1000))
        val payload = Gson().toJson(request)

        val headers = mutableMapOf<String, String>()
        headers["Host"] = TENCENT_API_ENDPOINT
        headers["Content-Type"] = "application/json; charset=utf-8"
        headers["X-TC-Action"] = ACTION_TEXT_TRANSLATE
        headers["X-TC-Version"] = TENCENT_API_VERSION
        headers["X-TC-Timestamp"] = timestamp.toString()

        // TODO(): Make region configurable.
        headers["X-TC-Region"] = "ap-guangzhou"

        val authorization = generateAuthorization(payload, date, timestamp, secretId, secretKey)
        headers["Authorization"] = authorization

        val url = "https://$TENCENT_API_ENDPOINT/"
        return Http.post(url, "application/json; charset=utf-8", payload) {
            tuner { conn ->
                headers.forEach { (key, value) ->
                    conn.setRequestProperty(key, value)
                }
            }
        }
    }

    private fun generateAuthorization(
        payload: String,
        date: String,
        timestamp: Long,
        secretId: String,
        secretKey: String
    ): String {
        // Step 1: Create canonical request
        val httpRequestMethod = "POST"
        val canonicalUri = "/"
        val canonicalQueryString = ""
        
        // 参与签名的头部必须是小写，并且按字典序排序
        val canonicalHeaders = "content-type:application/json; charset=utf-8\nhost:$TENCENT_API_ENDPOINT\nx-tc-action:texttranslate\n"
//        val signedHeaders = "content-type;host"
//        val canonicalHeaders = "content-type:application/json\nhost:tmt.ap-beijing.tencentcloudapi.com\nx-tc-action:texttranslate\n"
        val signedHeaders = "content-type;host;x-tc-action"
//        val canonicalHeaders = "content-type:application/json; charset=utf-8\nhost:cvm.tencentcloudapi.com\nx-tc-action:describeinstances\n"
//        val signedHeaders = "content-type;host;x-tc-action"
        val hashedRequestPayload = payload.sha256().lowercase()

        val canonicalRequest = "$httpRequestMethod\n$canonicalUri\n$canonicalQueryString\n$canonicalHeaders\n$signedHeaders\n$hashedRequestPayload"

        // Step 2: Create string to sign
//        val credentialScope = "$date/cvm/tc3_request"
        val credentialScope = "$date/$SERVICE/$TC3_REQUEST"
        val hashedCanonicalRequest = canonicalRequest.sha256().lowercase()
        val stringToSign = "$ALGORITHM\n$timestamp\n$credentialScope\n$hashedCanonicalRequest"

        // Step 3: Calculate signature
        val secretDate = date.hmacSha256("TC3$secretKey".toByteArray())
        val secretService = SERVICE.hmacSha256(secretDate)
        val secretSigning = TC3_REQUEST.hmacSha256(secretService)
        val signature = stringToSign.hmacSha256(secretSigning).toHexString().lowercase()

        // Step 4: Create authorization header
        return "$ALGORITHM Credential=$secretId/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        val errorMessage = when (throwable) {
            is TencentTranslationException -> when (throwable.errorCode) {
                "FailedOperation.NoFreeAmount" -> return ErrorInfo(
                    message("error.service.is.down"),
                    ErrorInfo.browseUrlAction(message("error.service.is.down.action.name"), TENCENT_TRANSLATE_PRODUCT_URL)
                )
                "FailedOperation.ServiceIsolate" -> return ErrorInfo(
                    message("error.service.is.down"),
                    ErrorInfo.browseUrlAction(message("error.service.is.down.action.name"), TENCENT_TRANSLATE_PRODUCT_URL)
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
                ?.let { Gson().fromJson(it, TencentTranslateResponse::class.java) }
                ?.response?.error
                ?.let { "[${it.code}] ${it.message}" }
        } catch (_: Exception) {
            null
        }
    }

    override fun translateDocumentation(
        documentation: Document,
        srcLang: Lang,
        targetLang: Lang
    ): Document = checkError {
        documentation.translateBody { bodyHTML ->
            val doc = Jsoup.parse(bodyHTML)
            val codeList = doc.select("code").map { it.text() }
            val sectionList =  doc.select("td.section").select("p").map { it.text() }
            SimpleTranslateClient(
                this,
                { _, _, _ -> callTranslate(bodyHTML, srcLang, targetLang, true) },
                TencentTranslator::parseTranslation
            ).execute(bodyHTML, srcLang, targetLang).translation?.run {
                val newDoc = Jsoup.parse(this)
                newDoc.select("code").forEachIndexed { index, element ->
                    if (index < codeList.size) {
                        element.text(codeList[index])
                    }
                }
                var sectionIndex = 0
                newDoc.select("td.section").forEach { element ->
                    element.select("p").forEach { pElement ->
                        if (sectionIndex < sectionList.size) {
                            pElement.text(sectionList[sectionIndex])
                            sectionIndex++
                        }
                    }
                }
                newDoc.body().html()
            } ?: ""
        }
    }
}
