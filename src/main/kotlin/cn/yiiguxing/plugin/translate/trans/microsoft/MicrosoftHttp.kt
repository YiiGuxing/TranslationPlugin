package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.microsoft.data.MicrosoftError
import cn.yiiguxing.plugin.translate.trans.microsoft.data.presentableError
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Http.sendJson
import cn.yiiguxing.plugin.translate.util.d
import com.google.gson.JsonParseException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder

internal object MicrosoftHttp {

    private val LOG: Logger = logger<MicrosoftHttp>()

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

        val jsonError = errorText?.toJsonError()
        jsonError ?: LOG.d("Request: $url : Unable to parse JSON error")

        val message = "$statusLine - ${jsonError?.presentableError ?: errorText}"
        throw MicrosoftStatusException(message, response.code, url, jsonError?.error)
    }

    private fun String.toJsonError(): MicrosoftError? = try {
        Http.defaultGson.fromJson(this, MicrosoftError::class.java)
    } catch (jse: JsonParseException) {
        null
    }
}