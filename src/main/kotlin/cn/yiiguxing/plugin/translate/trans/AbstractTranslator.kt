package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.message
import com.google.gson.JsonSyntaxException
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

    protected abstract fun getTranslateUrl(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        forDocumentation: Boolean
    ): String

    protected open fun buildRequest(builder: RequestBuilder, orDocumentation: Boolean) {}

    protected abstract fun parserResult(
        original: String,
        srcLang: Lang,
        targetLang: Lang,
        result: String,
        forDocumentation: Boolean
    ): BaseTranslation

    @Suppress("InvalidBundleOrProperty")
    protected open fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is UnsupportedLanguageException -> message("error.unsupportedLanguage", throwable.lang.langName)
        is SocketException, is SSLHandshakeException -> message("error.network")
        is ConnectException, is UnknownHostException -> message("error.network.connection")
        is SocketTimeoutException -> message("error.network.timeout")
        is JsonSyntaxException -> message("error.parse")
        is HttpRequests.HttpStatusException -> HttpResponseStatus.valueOf(throwable.statusCode).reasonPhrase()
        else -> message("error.unknown")
    }

    protected open fun doTranslate(
        `in`: String,
        srcLang: Lang,
        targetLang: Lang,
        forDocumentation: Boolean
    ): BaseTranslation {
        if (srcLang !in supportedSourceLanguages) {
            throw UnsupportedLanguageException(srcLang, name)
        }
        if (targetLang !in supportedTargetLanguages) {
            throw UnsupportedLanguageException(targetLang, name)
        }

        return HttpRequests.request(getTranslateUrl(`in`, srcLang, targetLang, forDocumentation))
            .also { buildRequest(it, forDocumentation) }
            .connect {
                parserResult(`in`, srcLang, targetLang, it.readString(null), forDocumentation)
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

}