package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.ALIYUN_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.ALIYUN
import cn.yiiguxing.plugin.translate.util.*
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.swing.Icon
import javax.xml.bind.DatatypeConverter


object AliyunTranslator : AbstractTranslator() {

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

    private val logger: Logger = Logger.getInstance(AliyunTranslator::class.java)

    override val id: String = ALIYUN.id

    override val name: String = ALIYUN.translatorName

    override val icon: Icon = ALIYUN.icon

    override val intervalLimit: Int = ALIYUN.intervalLimit

    override val contentLengthLimit: Int = ALIYUN.contentLengthLimit

    override val primaryLanguage: Lang
        get() = ALIYUN.primaryLanguage

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES
        .toMutableList()
        .apply { add(0, Lang.AUTO) }
    override val supportedTargetLanguages: List<Lang> = SUPPORTED_TARGET_LANGUAGES

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || Settings.aliyunTranslateSettings.let { it.appId.isEmpty() || it.getAppKey().isEmpty() }) {
            return ALIYUN.showConfigurationDialog()
        }

        return true
    }

    override fun getRequestUrl(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isDocumentation: Boolean
    ): String = ALIYUN_TRANSLATE_URL

    /**
     * 获得请求参数-不适用
     */
    override fun getRequestParams(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isDocumentation: Boolean
    ): List<Pair<String, String>> = emptyList()

    /**
     * 重写post和签名机制
     */
    override fun doTranslate(
        `in`: String,
        srcLang: Lang,
        targetLang: Lang,
        isDocumentation: Boolean
    ): BaseTranslation {
        if (srcLang !in supportedSourceLanguages) {
            throw UnsupportedLanguageException(srcLang, name)
        }
        if (targetLang !in supportedTargetLanguages) {
            throw UnsupportedLanguageException(targetLang, name)
        }

        val cache = CacheService.getDiskCache(`in`, srcLang, targetLang, id, isDocumentation)
        if (cache != null) try {
            return parserResult(`in`, srcLang, targetLang, cache, isDocumentation)
        } catch (e: Throwable) {
            Logger.getInstance(AbstractTranslator::class.java).w(e)
        }

        val url = getRequestUrl(`in`, srcLang, targetLang, isDocumentation)
        val body = """
            {
            "FormatType": "text",
            "SourceLanguage": "${srcLang.aliyunCode}",
            "TargetLanguage": "${targetLang.aliyunCode}",
            "SourceText": "$`in`",
            "Scene": "general"
            }
        """.trimIndent()

        val realUrl = URL(url)
        val accept = "application/json"
        val contentType = "application/json;chrset=utf-8"
        val date: String = toGMTString(Date())
        val bodyMd5: String = MD5Base64(body)
        val uuid = UUID.randomUUID().toString()
        val stringToSign = """
            POST
            $accept
            $bodyMd5
            $contentType
            $date
            x-acs-signature-method:HMAC-SHA1
            x-acs-signature-nonce:$uuid
            x-acs-version:2019-01-02
            ${realUrl.file}
            """.trimIndent()

        val settings = Settings.aliyunTranslateSettings

        return HttpRequests.post(url, contentType)
            .connect {
                it.connection.setRequestProperty("Accept", accept)
                it.connection.setRequestProperty("Content-MD5", bodyMd5)
                it.connection.setRequestProperty("Date", date)
                it.connection.setRequestProperty("Host", realUrl.host)
                it.connection.setRequestProperty("Authorization", "acs ${settings.appId}:${HMACSha1(stringToSign, settings.getAppKey())}")
                it.connection.setRequestProperty("x-acs-signature-nonce", uuid)
                it.connection.setRequestProperty("x-acs-signature-method", "HMAC-SHA1")
                it.connection.setRequestProperty("x-acs-version", "2019-01-02") // 版本可选
                it.write(body)
                val result = it.readString()
                val translation = parserResult(`in`, srcLang, targetLang, result, isDocumentation)

                CacheService.putDiskCache(`in`, srcLang, targetLang, id, isDocumentation, result)

                translation
            }
    }

    override fun parserResult(
        original: String,
        srcLang: Lang,
        targetLang: Lang,
        result: String,
        isDocumentation: Boolean
    ): BaseTranslation {
        logger.i("Translate result: $result")
        return Gson().fromJson(result, AliyunTranslation::class.java).apply {
            query = original
            src = srcLang
            target = targetLang
            if (!isSuccessful) {
                println(errorMessage)
                throw TranslateResultException(code, name)
            }
        }.toTranslation()
    }

    override fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is TranslateResultException -> when (throwable.code) {
            10001 -> message("error.request.timeout")
            10002 -> message("error.systemError")
            10003 -> message("error.aliyun.decode")
            10004 -> message("error.missingParameter")
            10005 -> message("error.language.unsupported")
            10006 -> message("error.aliyun.detectingLanguage")
            10007 -> message("error.aliyun.translating")
            10008 -> message("error.text.too.long")
            10009 -> message("error.aliyun.permission")
            10010 -> message("error.service.is.down")
            10011 -> message("error.aliyun.subAccountService")
            10012 -> message("error.aliyun.translationService")
            10013 -> message("error.account.has.run.out.of.balance")//"账号服务没有开通或者欠费"
            else -> message("error.unknown") + "[${throwable.code}]"
        }
        else -> super.createErrorMessage(throwable)
    }

    private fun MD5Base64(s: String): String {
        return with(MessageDigest.getInstance("MD5")) {
            update(s.toByteArray())
            DatatypeConverter.printBase64Binary(digest())
        }
    }

    private fun HMACSha1(data: String, key: String): String {
        val mac: Mac = Mac.getInstance("HMACSha1")
        val secretKeySpec = SecretKeySpec(key.toByteArray(), mac.algorithm)
        mac.init(secretKeySpec)
        return DatatypeConverter.printBase64Binary(mac.doFinal(data.toByteArray()))
    }

    private fun toGMTString(date: Date): String {
        val df = SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.UK)
        df.timeZone = SimpleTimeZone(0, "GMT")
        return df.format(date)
    }
}
