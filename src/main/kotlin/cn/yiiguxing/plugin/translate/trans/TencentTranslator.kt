package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION
import cn.yiiguxing.plugin.translate.TENCENT_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.TENCENT
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.w
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.swing.Icon
import javax.xml.bind.DatatypeConverter


object TencentTranslator : AbstractTranslator() {

    private val SUPPORTED_LANGUAGES: List<Lang> = listOf(
        Lang.CHINESE,
        Lang.CHINESE_TRADITIONAL,
        Lang.ENGLISH,
        Lang.JAPANESE,
        Lang.KOREAN,
        Lang.FRENCH,
        Lang.SPANISH,
        Lang.ITALIAN,
        Lang.GERMAN,
        Lang.TURKISH,
        Lang.RUSSIAN,
        Lang.PORTUGUESE,
        Lang.VIETNAMESE,
        Lang.INDONESIAN,
        Lang.THAI,
        Lang.MALAY,
        Lang.ARABIC,
        Lang.HINDI
    )

    private val SUPPORTED_TARGET_LANGUAGES: List<Lang> = listOf(
        Lang.CHINESE,
        Lang.ENGLISH,
        Lang.JAPANESE,
        Lang.KOREAN,
        Lang.FRENCH,
        Lang.SPANISH,
        Lang.ITALIAN,
        Lang.GERMAN,
        Lang.TURKISH,
        Lang.RUSSIAN,
        Lang.PORTUGUESE,
        Lang.VIETNAMESE,
        Lang.INDONESIAN,
        Lang.THAI,
        Lang.MALAY
    )

    private val logger: Logger = Logger.getInstance(TencentTranslator::class.java)

    override val id: String = TENCENT.id

    override val name: String = TENCENT.translatorName

    override val icon: Icon = TENCENT.icon

    override val intervalLimit: Int = TENCENT.intervalLimit

    override val contentLengthLimit: Int = TENCENT.contentLengthLimit

    override val primaryLanguage: Lang
        get() = TENCENT.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES
        .toMutableList()
        .apply { add(0, Lang.AUTO) }
    override val supportedTargetLanguages: List<Lang> = SUPPORTED_TARGET_LANGUAGES

    override fun checkConfiguration(): Boolean {
        if (Settings.tencentTranslateSettings.let { it.appId.isEmpty() || it.getAppKey().isEmpty() }) {
            return TENCENT.showConfigurationDialog()
        }

        return true
    }

    override fun getRequestUrl(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isDocumentation: Boolean
    ): String = TENCENT_TRANSLATE_URL

    private fun sign(s: String, key: String, method: String = "HmacSHA1"): String {
        val mac: Mac = Mac.getInstance(method)
        val secretKeySpec = SecretKeySpec(key.toByteArray(), mac.algorithm)
        mac.init(secretKeySpec)
        val hash: ByteArray = mac.doFinal(s.toByteArray())
        return DatatypeConverter.printBase64Binary(hash)
    }

    override fun getRequestParams(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isDocumentation: Boolean
    ): List<Pair<String, String>> {
        val settings = Settings.tencentTranslateSettings
        val appId = settings.appId
        val privateKey = settings.getAppKey()
        val curTimestamp = (System.currentTimeMillis() / 1000).toInt().toString()

        val postData = ArrayList<Pair<String, String>>().apply {
            add("Action" to "TextTranslate")
            // 这里如果腾讯适配是最好的
            add("Language" to "en-US")
            add("Timestamp" to curTimestamp)
            add("Region" to "ap-shanghai")
            add("Nonce" to (Random().nextInt(Int.MAX_VALUE).toString()))
            add("SecretId" to appId)
            add("Version" to "2018-03-21")
            add("ProjectId" to "0")
            add("Source" to srcLang.tencentCode)
            add("Target" to targetLang.tencentCode)
            add("SourceText" to text)
        }.sortedBy { pair -> pair.first }.toMutableList()

        @Suppress("SpellCheckingInspection")
        val dataToSign = "POSTtmt.tencentcloudapi.com/?" +
                postData.joinToString("&") { (key, value) -> "$key=${value}" }
        postData.add("Signature" to sign(dataToSign, privateKey))
        return postData
    }


    override fun parserResult(
        original: String,
        srcLang: Lang,
        targetLang: Lang,
        result: String,
        isDocumentation: Boolean
    ): BaseTranslation {
        logger.i("Translate result: $result")

        return Gson().fromJson(result, TencentTranslation::class.java).apply {
            query = original
            if (!isSuccessful) {
                logger.w(response.error!!.message)
                throw TencentTranslateResultException(response.error, name)
            }
        }.toTranslation()
    }

    override fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is TencentTranslateResultException -> when (throwable.error.code) {
            "AuthFailure.SignatureExpire" -> message("error.signature.expired")
            "AuthFailure.InvalidSecretId",
            "AuthFailure.SecretIdNotFound" -> message("error.invalidAccount", HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION)
            "AuthFailure.SignatureFailure" -> message(
                "error.invalidSignature",
                HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION
            )
            "InternalError",
            "InternalError.ErrorUnknown" -> message("error.systemError")
            "InvalidParameter" -> message("error.missingParameter")
            "RequestLimitExceeded" -> message("error.access.limited")
            "ResourceInsufficient",
            "FailedOperation.NoFreeAmount",
            "FailedOperation.ServiceIsolate" -> message("error.account.has.run.out.of.balance")
            "UnsupportedOperation.UnsupportedLanguage",
            "UnsupportedOperation.UnsupportedSourceLanguage" -> message("error.language.unsupported")
            "UnsupportedOperation.TextTooLong" -> message("error.text.too.long")
            else -> "[${throwable.error.code}]${throwable.error.message}"
        }
        else -> super.createErrorMessage(throwable)
    }
}
