package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.message
import com.intellij.util.io.HttpRequests
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

fun IOException.getCommonMessage(): String {
    return when (this) {
        is ConnectException, is UnknownHostException -> message("error.network.connection")
        is SocketException, is SSLException -> message("error.network")
        is SocketTimeoutException -> message("error.network.timeout")
        is HttpRequests.HttpStatusException -> when (statusCode) {
            HttpResponseStatus.TOO_MANY_REQUESTS.code() -> message("error.too.many.requests")
            HttpResponseStatus.BAD_REQUEST.code() -> message("error.bad.request")
            HttpResponseStatus.SERVICE_UNAVAILABLE.code() -> message("error.service.unavailable")
            HttpResponseStatus.INTERNAL_SERVER_ERROR.code() -> message("error.systemError")
            else -> HttpResponseStatus.valueOf(statusCode).reasonPhrase()
        }

        else -> message("error.io.exception", message ?: "")
    }
}