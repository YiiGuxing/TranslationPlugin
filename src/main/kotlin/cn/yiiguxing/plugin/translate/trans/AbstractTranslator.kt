package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.message
import com.intellij.util.io.HttpRequests
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * Provides a skeletal implementation of the [Translator] interface.
 */
abstract class AbstractTranslator : Translator {

    final override fun translate(text: String, srcLang: Lang, targetLang: Lang): Translation = checkError {
        checkContentLength(text, contentLengthLimit)
        doTranslate(text, srcLang, targetLang)
    }

    protected abstract fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation

    protected inline fun <T> checkError(action: () -> T): T = try {
        action()
    } catch (throwable: Throwable) {
        onError(throwable)
    }

    protected open fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        val errorMessage = when (throwable) {
            is UnsupportedLanguageException -> message("error.unsupportedLanguage", throwable.lang.langName)
            is ConnectException, is UnknownHostException -> message("error.network.connection")
            is SocketException, is SSLHandshakeException -> message("error.network")
            is SocketTimeoutException -> message("error.network.timeout")
            is ContentLengthLimitException -> message("error.text.too.long")
            is HttpRequests.HttpStatusException -> when (throwable.statusCode) {
                HttpResponseStatus.TOO_MANY_REQUESTS.code() -> message("error.too.many.requests")
                HttpResponseStatus.FORBIDDEN.code() -> message("error.invalidAccount")
                HttpResponseStatus.BAD_REQUEST.code() -> message("error.bad.request")
                HttpResponseStatus.NOT_FOUND.code() -> message("error.request.not.found")
                HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE.code() -> message("error.text.too.long")
                HttpResponseStatus.REQUEST_URI_TOO_LONG.code() -> message("error.request.uri.too.long")
                HttpResponseStatus.TOO_MANY_REQUESTS.code() -> message("error.too.many.requests")
                HttpResponseStatus.SERVICE_UNAVAILABLE.code() -> message("error.deepl.service.is.down")
                HttpResponseStatus.INTERNAL_SERVER_ERROR.code() -> message("error.systemError")
                456 -> message("error.access.limited") // Quota exceeded. The character limit has been reached.
                529 -> message("error.too.many.requests") // Too many requests. Please wait and resend your request.
                else -> HttpResponseStatus.valueOf(throwable.statusCode).reasonPhrase()
            }
            is IOException -> message("error.io.exception", throwable.message ?: "")
            else -> return null
        }

        return ErrorInfo(errorMessage)
    }

    protected fun onError(throwable: Throwable): Nothing {
        val errorInfo = createErrorInfo(throwable) ?: throw throwable
        throw TranslateException(id, name, errorInfo, throwable)
    }

}