package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.openai.exception.OpenAIError
import cn.yiiguxing.plugin.translate.trans.openai.exception.OpenAIStatusException
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Http.sendJson
import cn.yiiguxing.plugin.translate.util.d
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder

internal object OpenAIHttp {

    private val LOG: Logger = logger<OpenAIHttp>()

    inline fun <reified T> post(url: String, data: Any, noinline init: RequestBuilder.() -> Unit): T {
        return post(url, data, init).let { Http.defaultGson.fromJson(it, T::class.java) }
    }

    fun post(url: String, data: Any, init: RequestBuilder.() -> Unit): String {
        val response = HttpRequests.post(url, Http.MIME_TYPE_JSON)
            .accept(Http.MIME_TYPE_JSON)
            .apply(init)
            .sendJson(data)
        return when (response) {
            is Http.Response.Success -> response.body
            is Http.Response.Error -> throwStatusCodeException(url, response)
        }
    }

    private fun throwStatusCodeException(url: String, response: Http.Response.Error): Nothing {
        val statusLine = "${response.code} ${response.message}"
        val errorText = response.body
        LOG.d("Request: $url : Error $statusLine body:\n$errorText")

        val error = errorText?.toError()
        error ?: LOG.d("Request: $url : Unable to parse JSON error")

        val message = "$statusLine - ${error?.error?.code ?: errorText}"
        throw OpenAIStatusException(message, response.code, url, error?.error)
    }

    private fun String.toError(): ErrorResponse? = try {
        Http.defaultGson.fromJson(this, ErrorResponse::class.java)
    } catch (jse: JsonParseException) {
        null
    }

    internal data class ErrorResponse(@SerializedName("error") val error: OpenAIError?)

}