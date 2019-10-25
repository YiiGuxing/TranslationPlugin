package cn.yiiguxing.plugin.translate.trans

import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * TranslateException
 */
open class TranslateException(
    override val message: String,
    private val translatorName: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    override fun getLocalizedMessage(): String {
        return "$translatorName :: $message"
    }
}

class UnsupportedLanguageException(val lang: Lang, translatorName: String) :
    TranslateException("Unsupported language: ${lang.langName}", translatorName)

class TranslateResultException(val code: Int, translatorName: String) :
    TranslateException("Translate failed: $code", translatorName)

class NetworkException(host: String, cause: IOException) : IOException("${cause.message}. host=$host", cause) {
    companion object {
        fun wrapIfIsNetworkException(throwable: Throwable, host: String): Throwable {
            return when (throwable) {
                is SocketException,
                is SocketTimeoutException,
                is SSLHandshakeException,
                is ConnectException,
                is UnknownHostException -> NetworkException(host, throwable as IOException)
                else -> throwable
            }
        }
    }
}