package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.openai.exception.OpenAIStatusException
import cn.yiiguxing.plugin.translate.trans.openai.exception.OpenAiError
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Http.sendJson
import cn.yiiguxing.plugin.translate.util.d
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder

internal object OpenAiHttp {

    private val LOG: Logger = logger<OpenAiHttp>()

    inline fun <reified T> post(url: String, data: Any, noinline init: RequestBuilder.() -> Unit): T {
        val json = post(url) {
            init()
            sendJson(data) { it.readString() }
        }
        return Http.defaultGson.fromJson(json, T::class.java)
    }

    fun <T> post(url: String, block: RequestBuilder.() -> T): T {
        return try {
            block(
                HttpRequests.post(url, Http.MIME_TYPE_JSON)
                    .accept(Http.MIME_TYPE_JSON)
            )
        } catch (e: Http.StatusException) {
            throwStatusCodeException(e)
        }
    }

    private fun throwStatusCodeException(e: Http.StatusException): Nothing {
        val statusLine = "${e.statusCode} ${e.responseMessage}"
        val errorText = e.errorText
        LOG.d("Request: ${e.url} : Error $statusLine body:\n$errorText")

        val error = errorText?.toError()
        error ?: LOG.d("Request: ${e.url} : Unable to parse JSON error")

        val message = "$statusLine - ${error?.error?.code ?: errorText}"
        throw OpenAIStatusException(message, e.statusCode, e.url, error?.error)
    }

    private fun String.toError(): ErrorResponse? = try {
        Http.defaultGson.fromJson(this, ErrorResponse::class.java)
    } catch (jse: JsonParseException) {
        null
    }

    internal data class ErrorResponse(@SerializedName("error") val error: OpenAiError?)

}