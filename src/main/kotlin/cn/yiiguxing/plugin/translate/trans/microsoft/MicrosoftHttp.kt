package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.trans.microsoft.models.MicrosoftError
import cn.yiiguxing.plugin.translate.trans.microsoft.models.presentableError
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Http.send
import cn.yiiguxing.plugin.translate.util.d
import cn.yiiguxing.plugin.translate.util.md5
import com.google.gson.JsonParseException
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import java.lang.reflect.Type

internal object MicrosoftHttp {

    private val LOG: Logger = logger<MicrosoftHttp>()

    inline fun <reified T> post(
        url: String,
        token: String,
        data: Any,
        cache: Boolean = true,
        noinline builder: RequestBuilder.() -> Unit = {}
    ): T {
        return post(url, token, data, T::class.java, cache, builder)
    }

    fun <T> post(
        url: String,
        token: String,
        data: Any,
        typeOfT: Type,
        cache: Boolean = true,
        builder: RequestBuilder.() -> Unit = {}
    ): T {
        val json = Http.defaultGson.toJson(data)
        if (cache) {
            val cacheKey = getDiskCacheKey(url, json)
            CacheService.getInstance().getDiskCache(cacheKey)?.let {
                try {
                    return Http.defaultGson.fromJson(it, typeOfT)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        val resultJson = post(url, token, json, builder)
        val result: T = try {
            Http.defaultGson.fromJson(resultJson, typeOfT)
        } catch (e: JsonParseException) {
            logJsonParseError(e, url, json, resultJson)
            throw e
        }

        if (cache) {
            val cacheKey = getDiskCacheKey(url, json)
            CacheService.getInstance().putDiskCache(cacheKey, resultJson)
        }

        return result
    }

    private fun getDiskCacheKey(url: String, dataJson: String): String {
        return "$url;$dataJson".md5()
    }

    private fun post(
        url: String,
        token: String,
        dataJson: String,
        builder: RequestBuilder.() -> Unit
    ): String {
        return try {
            HttpRequests.post(url, Http.MIME_TYPE_JSON)
                .accept(Http.MIME_TYPE_JSON)
                .tuner { it.setRequestProperty("Authorization", "Bearer $token") }
                .apply(builder)
                .send(dataJson) { it.readString() }
        } catch (e: Http.StatusException) {
            throwStatusCodeException(e)
        }
    }

    private fun throwStatusCodeException(e: Http.StatusException): Nothing {
        val statusLine = "${e.statusCode} ${e.responseMessage}"
        val errorText = e.errorText
        LOG.d("Request: ${e.url} : Error $statusLine body:\n$errorText")

        val jsonError = errorText?.toJsonError()
        jsonError ?: LOG.d("Request: ${e.url} : Unable to parse JSON error")

        val message = "$statusLine - ${jsonError?.presentableError ?: errorText}"
        throw MicrosoftStatusException(message, e.statusCode, e.url, jsonError?.error)
    }

    private fun String.toJsonError(): MicrosoftError? = try {
        Http.defaultGson.fromJson(this, MicrosoftError::class.java)
    } catch (jse: JsonParseException) {
        null
    }

    private fun logJsonParseError(e: JsonParseException, url: String, requestJson: String, responseJson: String) {
        val request = Attachment(
            "request.json",
            """
            |URL: $url
            |Request JSON:
            |$requestJson
            """.trimMargin()
        ).apply { isIncluded = true }
        val response = Attachment(
            "response.json",
            """
            |Response JSON:
            |$responseJson
            """.trimMargin()
        ).apply { isIncluded = true }
        LOG.error("Failed to parse JSON", e, request, response)
    }
}