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