package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import com.intellij.util.io.HttpRequests
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

fun IOException.getCommonMessage(): String =
    getCommonMessage(Settings.getInstance().proxySettings.toProxyOrNull() != null)

/**
 * 与 [getCommonMessage] 相同，但显式接收"是否配置了代理"。
 *
 * 配置了代理时在连接类错误后追加提示——否则用户看到"网络连接失败"根本不会想到是代理的问题。
 * 状态码错误（[HttpRequests.HttpStatusException]）是服务端返回的，与代理无关，不追加提示。
 */
internal fun IOException.getCommonMessage(proxyConfigured: Boolean): String {
    val commonMessage = when (this) {
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

        else -> message("error.io.exception", message ?: message("error.unknown"))
    }

    return if (proxyConfigured && isConnectionFailure()) {
        "$commonMessage ${message("error.network.proxy.hint")}"
    } else {
        commonMessage
    }
}

/**
 * 只有连接层面的失败才可能与代理有关。状态码错误是服务端返回的，其他 I/O 错误也与代理无关——
 * 对这些情况追加代理提示只会误导用户。
 */
private fun IOException.isConnectionFailure(): Boolean =
    this is ConnectException ||
            this is UnknownHostException ||
            this is SocketException ||
            this is SSLException ||
            this is SocketTimeoutException