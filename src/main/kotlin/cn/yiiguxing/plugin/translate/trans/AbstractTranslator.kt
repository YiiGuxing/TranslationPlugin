package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.CacheService
import cn.yiiguxing.plugin.translate.util.urlEncode
import cn.yiiguxing.plugin.translate.util.w
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import io.netty.handler.codec.http.HttpResponseStatus
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * Provides a skeletal implementation of the [Translator] interface.
 */
abstract class AbstractTranslator : Translator {

    protected abstract fun getRequestUrl(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isDocumentation: Boolean
    ): String

    protected abstract fun getRequestParams(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isDocumentation: Boolean
    ): List<Pair<String, String>>

    protected open fun buildRequest(builder: RequestBuilder, isDocumentation: Boolean) {}

    protected abstract fun parserResult(
        original: String,
        srcLang: Lang,
        targetLang: Lang,
        result: String,
        isDocumentation: Boolean
    ): BaseTranslation

    protected open fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is UnsupportedLanguageException -> message("error.unsupportedLanguage", throwable.lang.langName)
        is SocketException, is SSLHandshakeException -> message("error.network")
        is ConnectException, is UnknownHostException -> message("error.network.connection")
        is SocketTimeoutException -> message("error.network.timeout")
        is JsonSyntaxException -> message("error.parse")
        is HttpRequests.HttpStatusException -> HttpResponseStatus.valueOf(throwable.statusCode).reasonPhrase()
        else -> when (
            throwable.message?.let { HTTP_STATUS_EXCEPTION_REGEX.matchEntire(it) }?.let { it.groupValues[1].toInt() }
        ) {
            429 -> message("error.too.many.requests")
            else -> message("error.unknown")
        }
    }

    protected open fun doTranslate(
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
            LOG.w(e)
        }

        val url = getRequestUrl(`in`, srcLang, targetLang, isDocumentation)
        val params = getRequestParams(`in`, srcLang, targetLang, isDocumentation)
        val data = params.joinToString("&") { (key, value) -> "$key=${value.urlEncode()}" }

        return HttpRequests.post(url, "application/x-www-form-urlencoded")
            .also { buildRequest(it, isDocumentation) }
            .connect {
                it.write(data)
                val result = it.readString()
                val translation = parserResult(`in`, srcLang, targetLang, result, isDocumentation)

                CacheService.putDiskCache(`in`, srcLang, targetLang, id, isDocumentation, result)

                translation
            }
    }

    override fun translate(text: String, srcLang: Lang, targetLang: Lang): Translation = run {
        doTranslate(text, srcLang, targetLang, false) as Translation
    }

    override fun translateDocumentation(documentation: String, srcLang: Lang, targetLang: Lang): BaseTranslation = run {
        doTranslate(documentation, srcLang, targetLang, true)
    }

    private inline fun <T> run(action: () -> T): T = try {
        action()
    } catch (throwable: Throwable) {
        val errorMessage = message("error.translate.failed", createErrorMessage(throwable))
        throw TranslateException(errorMessage, name, onError(throwable))
    }

    protected open fun onError(throwable: Throwable): Throwable = throwable

    companion object {
        private val HTTP_STATUS_EXCEPTION_REGEX = Regex("^Server returned HTTP response code: (\\d{3}) .+$")

        private val LOG = Logger.getInstance(AbstractTranslator::class.java)
    }

}