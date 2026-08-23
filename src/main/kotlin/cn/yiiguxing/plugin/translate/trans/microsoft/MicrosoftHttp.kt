package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.trans.microsoft.models.AzureAuthentication
import cn.yiiguxing.plugin.translate.trans.microsoft.models.MicrosoftError
import cn.yiiguxing.plugin.translate.trans.microsoft.models.authorizationHeaderValue
import cn.yiiguxing.plugin.translate.trans.microsoft.models.presentableError
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Http.send
import cn.yiiguxing.plugin.translate.util.d
import cn.yiiguxing.plugin.translate.util.md5
import com.google.gson.JsonParseException
import com.intellij.ide.plugins.marketplace.setHeadersViaTuner
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
        authentication: AzureAuthentication,
        data: Any,
        cache: Boolean = true,
        noinline builder: RequestBuilder.() -> Unit = {}
    ): T? {
        return post(url, authentication, data, T::class.java, cache, builder)
    }

    fun <T> post(
        url: String,
        authentication: AzureAuthentication,
        data: Any,
        typeOfT: Type,
        cache: Boolean = true,
        builder: RequestBuilder.() -> Unit = {}
    ): T? {
        val json = Http.defaultGson.toJson(data)
        return post(url, Http.MIME_TYPE_JSON, json, typeOfT, cache) {
            setHeadersViaTuner()
            tuner { connection ->
                when (authentication) {
                    is AzureAuthentication.AccessToken -> connection.setRequestProperty(
                        AzureAuthentication.HEADER_AUTHORIZATION,
                        authentication.authorizationHeaderValue()
                    )

                    is AzureAuthentication.SubscriptionKey -> {
                        connection.setRequestProperty(
                            AzureAuthentication.HEADER_SUBSCRIPTION_KEY,
                            authentication.key
                        )
                        authentication.region?.let { region ->
                            connection.setRequestProperty(
                                AzureAuthentication.HEADER_SUBSCRIPTION_REGION,
                                region
                            )
                        }
                    }
                }
            }
            builder()
        }
    }

    inline fun <reified T> post(
        url: String,
        contentType: String,
        data: String,
        cache: Boolean = true,
        cacheKey: String? = null,
        noinline builder: RequestBuilder.() -> Unit = {}
    ): T? {
        return post(url, contentType, data, T::class.java, cache, cacheKey, builder)
    }

    inline fun <reified T> post(
        url: String,
        data: Any,
        cache: Boolean = true,
        cacheKey: String? = null,
        noinline builder: RequestBuilder.() -> Unit = {}
    ): T? {
        val json = Http.defaultGson.toJson(data)
        return post(url, Http.MIME_TYPE_JSON, json, T::class.java, cache, cacheKey, builder)
    }

    fun <T> post(
        url: String,
        contentType: String,
        data: String,
        typeOfT: Type,
        cache: Boolean = true,
        cacheKey: String? = null,
        builder: RequestBuilder.() -> Unit = {}
    ): T? {
        return postWithCache(url, contentType, data, cache, cacheKey, typeOfT) {
            post(url, contentType, data, builder)
        }
    }

    private fun <T> postWithCache(
        url: String,
        contentType: String,
        requestData: String,
        cache: Boolean,
        cacheKey: String?,
        typeOfT: Type,
        send: () -> String
    ): T? {
        if (cache) {
            val diskCacheKey = cacheKey ?: getDiskCacheKey(url, contentType, requestData)
            CacheService.getInstance().getDiskCache(diskCacheKey)?.takeIf { it.isNotBlank() }?.let {
                try {
                    return Http.defaultGson.fromJson(it, typeOfT)
                } catch (_: Exception) {
                    // Ignore
                }
            }
        }

        val resultJson = send()
        val result: T? = if (resultJson.isBlank()) null else try {
            Http.defaultGson.fromJson<T>(resultJson, typeOfT)
        } catch (e: JsonParseException) {
            logJsonParseError(e, requestData, resultJson)
            throw e
        }

        if (cache && resultJson.isNotBlank()) {
            val diskCacheKey = cacheKey ?: getDiskCacheKey(url, contentType, requestData)
            CacheService.getInstance().putDiskCache(diskCacheKey, resultJson)
        }

        return result
    }

    private fun getDiskCacheKey(url: String, contentType: String, data: String): String {
        return "$url;$contentType;$data".md5()
    }

    private fun post(
        url: String,
        contentType: String,
        data: String,
        builder: RequestBuilder.() -> Unit
    ): String {
        return try {
            HttpRequests.post(url, contentType)
                .accept(Http.MIME_TYPE_JSON)
                .apply(builder)
                .send(data) { it.readString() }
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
    } catch (_: JsonParseException) {
        null
    }

    private fun logJsonParseError(e: JsonParseException, requestJson: String, responseJson: String) {
        val request = Attachment("request.json", requestJson).apply { isIncluded = true }
        val response = Attachment("response.json", responseJson).apply { isIncluded = true }
        LOG.error("Failed to parse JSON. ", e, request, response)
    }
}
